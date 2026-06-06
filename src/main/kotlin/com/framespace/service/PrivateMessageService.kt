package com.framespace.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.framespace.common.MessageType
import com.framespace.dto.ConversationDto
import com.framespace.dto.FriendUserDto
import com.framespace.dto.PrivateMessageDto
import com.framespace.dto.PushMovieResultDto
import com.framespace.entity.PrivateMessage
import com.framespace.entity.User
import com.framespace.mapper.MovieMapper
import com.framespace.mapper.PrivateMessageMapper
import com.framespace.mapper.UserMapper
import com.framespace.common.SocialWsEvent
import com.framespace.websocket.SocialWebSocketHub
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PrivateMessageService(
    private val privateMessageMapper: PrivateMessageMapper,
    private val userMapper: UserMapper,
    private val movieMapper: MovieMapper,
    private val friendService: FriendService,
    private val userProfileService: UserProfileService,
    private val socialWebSocketHub: SocialWebSocketHub
) {

    fun unreadCount(userId: Long): Long = privateMessageMapper.countUnread(userId)

    fun listConversations(userId: Long): List<ConversationDto> {
        val friendIds = friendService.listFriends(userId).map { it.id }
        return friendIds.mapNotNull { peerId ->
            val peer = userMapper.selectById(peerId) ?: return@mapNotNull null
            val last = privateMessageMapper.selectLastMessage(userId, peerId)
            ConversationDto(
                peer = toFriendUserDto(peer),
                lastMessage = last?.let { toDto(it) },
                unreadCount = privateMessageMapper.countUnreadFromPeer(userId, peerId),
                updatedAt = last?.createdAt
            )
        }.sortedByDescending { it.updatedAt ?: LocalDateTime.MIN }
    }

    fun getMessages(userId: Long, peerId: Long, limit: Int = 100): List<PrivateMessageDto> {
        require(friendService.isFriend(userId, peerId)) { "只能与好友私信" }
        return privateMessageMapper.selectConversation(userId, peerId, limit).map { toDto(it) }
    }

    @Transactional
    fun sendTextMessage(senderId: Long, receiverId: Long, content: String): PrivateMessageDto {
        val trimmed = content.trim()
        require(trimmed.isNotBlank()) { "消息不能为空" }
        require(trimmed.length <= 2000) { "消息不能超过 2000 字" }
        require(friendService.isFriend(senderId, receiverId)) { "只能给好友发私信" }
        userMapper.selectById(receiverId) ?: throw IllegalArgumentException("用户不存在")

        val message = PrivateMessage(
            senderId = senderId,
            receiverId = receiverId,
            messageType = MessageType.TEXT,
            content = trimmed,
            createdAt = LocalDateTime.now()
        )
        return saveAndNotify(message)
    }

    @Transactional
    fun pushMovie(
        senderId: Long,
        movieId: Long,
        friendIds: List<Long>,
        note: String?
    ): PushMovieResultDto {
        movieMapper.selectById(movieId) ?: throw IllegalArgumentException("电影不存在")
        val uniqueFriends = friendIds.distinct().filter { it != senderId }
        require(uniqueFriends.isNotEmpty()) { "请选择好友" }

        val trimmedNote = note?.trim()?.takeIf { it.isNotBlank() }
        var sent = 0
        uniqueFriends.forEach { friendId ->
            if (!friendService.isFriend(senderId, friendId)) return@forEach
            saveAndNotify(
                PrivateMessage(
                    senderId = senderId,
                    receiverId = friendId,
                    messageType = MessageType.MOVIE_PUSH,
                    content = trimmedNote,
                    movieId = movieId,
                    createdAt = LocalDateTime.now()
                )
            )
            sent++
        }
        require(sent > 0) { "未能推送给任何好友" }
        return PushMovieResultDto(sentCount = sent)
    }

    @Transactional
    fun markConversationRead(userId: Long, peerId: Long) {
        val unread = privateMessageMapper.selectList(
            QueryWrapper<PrivateMessage>()
                .eq("receiver_id", userId)
                .eq("sender_id", peerId)
                .isNull("read_at")
        )
        val now = LocalDateTime.now()
        unread.forEach {
            it.readAt = now
            privateMessageMapper.updateById(it)
        }
    }

    private fun saveAndNotify(message: PrivateMessage): PrivateMessageDto {
        privateMessageMapper.insert(message)
        val dto = toDto(message)
        socialWebSocketHub.pushEvent(
            message.receiverId!!,
            SocialWsEvent.NEW_DM,
            dto
        )
        socialWebSocketHub.pushEvent(
            message.senderId!!,
            SocialWsEvent.NEW_DM,
            dto
        )
        return dto
    }

    private fun toDto(message: PrivateMessage): PrivateMessageDto {
        val sender = userMapper.selectById(message.senderId)
        val movie = message.movieId?.let { movieMapper.selectById(it) }
        return PrivateMessageDto(
            id = message.id!!,
            senderId = message.senderId!!,
            receiverId = message.receiverId!!,
            messageType = message.messageType ?: MessageType.TEXT,
            content = message.content,
            movieId = message.movieId,
            movieTitle = movie?.title,
            moviePosterUrl = movie?.posterUrl,
            read = message.readAt != null,
            createdAt = message.createdAt ?: LocalDateTime.now(),
            senderName = userProfileService.displayName(sender),
            senderAvatarUrl = userProfileService.avatarUrl(sender)
        )
    }

    private fun toFriendUserDto(user: User): FriendUserDto = FriendUserDto(
        id = user.id!!,
        username = user.username ?: "",
        nickname = user.nickname,
        avatarUrl = userProfileService.avatarUrl(user)
    )
}
