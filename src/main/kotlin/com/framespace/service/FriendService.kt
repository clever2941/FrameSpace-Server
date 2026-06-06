package com.framespace.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.framespace.common.FriendRequestStatus
import com.framespace.common.FriendshipStatus
import com.framespace.dto.FriendRequestDto
import com.framespace.dto.FriendUserDto
import com.framespace.dto.FriendshipStatusDto
import com.framespace.entity.FriendRequest
import com.framespace.entity.User
import com.framespace.entity.UserFriend
import com.framespace.mapper.FriendRequestMapper
import com.framespace.mapper.UserFriendMapper
import com.framespace.mapper.UserMapper
import com.framespace.common.SocialWsEvent
import com.framespace.websocket.SocialWebSocketHub
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class FriendService(
    private val userFriendMapper: UserFriendMapper,
    private val friendRequestMapper: FriendRequestMapper,
    private val userMapper: UserMapper,
    private val userProfileService: UserProfileService,
    private val socialWebSocketHub: SocialWebSocketHub
) {

    fun listFriends(userId: Long): List<FriendUserDto> =
        userFriendMapper.selectFriendIds(userId).mapNotNull { friendId ->
            userMapper.selectById(friendId)?.let { toFriendUserDto(it) }
        }

    fun listIncomingRequests(userId: Long): List<FriendRequestDto> =
        friendRequestMapper.selectIncomingPending(userId, 50).mapNotNull { request ->
            val fromUser = userMapper.selectById(request.fromUserId) ?: return@mapNotNull null
            FriendRequestDto(
                id = request.id!!,
                fromUser = toFriendUserDto(fromUser),
                createdAt = request.createdAt ?: LocalDateTime.now()
            )
        }

    fun searchUsers(userId: Long, query: String): List<FriendUserDto> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        return userMapper.searchByKeyword(q, userId, 20).map { toFriendUserDto(it) }
    }

    fun getFriendshipStatus(userId: Long, targetUserId: Long): FriendshipStatusDto {
        if (userId == targetUserId) {
            return FriendshipStatusDto(FriendshipStatus.SELF)
        }
        if (isFriend(userId, targetUserId)) {
            return FriendshipStatusDto(FriendshipStatus.FRIENDS)
        }
        val sent = friendRequestMapper.selectOne(
            QueryWrapper<FriendRequest>()
                .eq("from_user_id", userId)
                .eq("to_user_id", targetUserId)
                .eq("status", FriendRequestStatus.PENDING)
        )
        if (sent != null) {
            return FriendshipStatusDto(FriendshipStatus.PENDING_SENT, sent.id)
        }
        val received = friendRequestMapper.selectOne(
            QueryWrapper<FriendRequest>()
                .eq("from_user_id", targetUserId)
                .eq("to_user_id", userId)
                .eq("status", FriendRequestStatus.PENDING)
        )
        if (received != null) {
            return FriendshipStatusDto(FriendshipStatus.PENDING_RECEIVED, received.id)
        }
        return FriendshipStatusDto(FriendshipStatus.NONE)
    }

    @Transactional
    fun sendFriendRequest(fromUserId: Long, toUserId: Long) {
        require(fromUserId != toUserId) { "不能添加自己为好友" }
        userMapper.selectById(toUserId) ?: throw IllegalArgumentException("用户不存在")
        require(!isFriend(fromUserId, toUserId)) { "你们已经是好友了" }

        val reversePending = friendRequestMapper.selectOne(
            QueryWrapper<FriendRequest>()
                .eq("from_user_id", toUserId)
                .eq("to_user_id", fromUserId)
                .eq("status", FriendRequestStatus.PENDING)
        )
        if (reversePending != null) {
            acceptRequest(fromUserId, reversePending.id!!)
            return
        }

        val existing = friendRequestMapper.selectOne(
            QueryWrapper<FriendRequest>()
                .eq("from_user_id", fromUserId)
                .eq("to_user_id", toUserId)
                .eq("status", FriendRequestStatus.PENDING)
        )
        if (existing != null) {
            throw IllegalArgumentException("好友申请已发送")
        }

        val request = FriendRequest(
            fromUserId = fromUserId,
            toUserId = toUserId,
            status = FriendRequestStatus.PENDING,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        friendRequestMapper.insert(request)
        val fromUser = userMapper.selectById(fromUserId) ?: return
        socialWebSocketHub.pushEvent(
            toUserId,
            SocialWsEvent.FRIEND_REQUEST,
            FriendRequestDto(
                id = request.id!!,
                fromUser = toFriendUserDto(fromUser),
                createdAt = request.createdAt ?: LocalDateTime.now()
            )
        )
    }

    @Transactional
    fun acceptRequest(userId: Long, requestId: Long) {
        val request = friendRequestMapper.selectById(requestId)
            ?: throw IllegalArgumentException("好友申请不存在")
        require(request.toUserId == userId) { "无权处理该申请" }
        require(request.status == FriendRequestStatus.PENDING) { "申请已处理" }

        request.status = FriendRequestStatus.ACCEPTED
        request.updatedAt = LocalDateTime.now()
        friendRequestMapper.updateById(request)
        linkFriends(request.fromUserId!!, request.toUserId!!)
        notifyFriendsChanged(request.fromUserId!!, request.toUserId!!)
    }

    @Transactional
    fun rejectRequest(userId: Long, requestId: Long) {
        val request = friendRequestMapper.selectById(requestId)
            ?: throw IllegalArgumentException("好友申请不存在")
        require(request.toUserId == userId) { "无权处理该申请" }
        require(request.status == FriendRequestStatus.PENDING) { "申请已处理" }
        request.status = FriendRequestStatus.REJECTED
        request.updatedAt = LocalDateTime.now()
        friendRequestMapper.updateById(request)
    }

    @Transactional
    fun removeFriend(userId: Long, friendId: Long) {
        userFriendMapper.delete(
            QueryWrapper<UserFriend>().eq("user_id", userId).eq("friend_id", friendId)
        )
        userFriendMapper.delete(
            QueryWrapper<UserFriend>().eq("user_id", friendId).eq("friend_id", userId)
        )
        notifyFriendsChanged(userId, friendId)
    }

    fun isFriend(userId: Long, friendId: Long): Boolean =
        userFriendMapper.selectCount(
            QueryWrapper<UserFriend>().eq("user_id", userId).eq("friend_id", friendId)
        ) > 0

    private fun linkFriends(userA: Long, userB: Long) {
        if (!isFriend(userA, userB)) {
            val now = LocalDateTime.now()
            userFriendMapper.insert(UserFriend(userId = userA, friendId = userB, createdAt = now))
            userFriendMapper.insert(UserFriend(userId = userB, friendId = userA, createdAt = now))
        }
    }

    private fun notifyFriendsChanged(vararg userIds: Long) {
        socialWebSocketHub.pushEvent(userIds.toList(), SocialWsEvent.FRIENDS_CHANGED, mapOf("ok" to true))
    }

    private fun toFriendUserDto(user: User): FriendUserDto = FriendUserDto(
        id = user.id!!,
        username = user.username ?: "",
        nickname = user.nickname,
        avatarUrl = userProfileService.avatarUrl(user)
    )
}
