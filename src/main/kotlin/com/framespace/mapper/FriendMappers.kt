package com.framespace.mapper

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.framespace.entity.FriendRequest
import com.framespace.entity.PrivateMessage
import com.framespace.entity.UserFriend
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select

@Mapper
interface FriendRequestMapper : BaseMapper<FriendRequest> {

    @Select(
        """
        SELECT * FROM friend_request
        WHERE to_user_id = #{userId} AND status = 'PENDING'
        ORDER BY created_at DESC
        LIMIT #{limit}
        """
    )
    fun selectIncomingPending(
        @Param("userId") userId: Long,
        @Param("limit") limit: Int
    ): List<FriendRequest>
}

@Mapper
interface UserFriendMapper : BaseMapper<UserFriend> {

    @Select(
        """
        SELECT friend_id FROM user_friend
        WHERE user_id = #{userId}
        ORDER BY created_at DESC
        """
    )
    fun selectFriendIds(@Param("userId") userId: Long): List<Long>
}

@Mapper
interface PrivateMessageMapper : BaseMapper<PrivateMessage> {

    @Select(
        """
        SELECT * FROM private_message
        WHERE (sender_id = #{userId} AND receiver_id = #{peerId})
           OR (sender_id = #{peerId} AND receiver_id = #{userId})
        ORDER BY created_at ASC
        LIMIT #{limit}
        """
    )
    fun selectConversation(
        @Param("userId") userId: Long,
        @Param("peerId") peerId: Long,
        @Param("limit") limit: Int
    ): List<PrivateMessage>

    @Select(
        """
        SELECT * FROM private_message
        WHERE (sender_id = #{userId} AND receiver_id = #{peerId})
           OR (sender_id = #{peerId} AND receiver_id = #{userId})
        ORDER BY created_at DESC
        LIMIT 1
        """
    )
    fun selectLastMessage(
        @Param("userId") userId: Long,
        @Param("peerId") peerId: Long
    ): PrivateMessage?

    @Select(
        """
        SELECT COUNT(*) FROM private_message
        WHERE receiver_id = #{userId} AND read_at IS NULL
        """
    )
    fun countUnread(@Param("userId") userId: Long): Long

    @Select(
        """
        SELECT COUNT(*) FROM private_message
        WHERE receiver_id = #{userId} AND sender_id = #{peerId} AND read_at IS NULL
        """
    )
    fun countUnreadFromPeer(
        @Param("userId") userId: Long,
        @Param("peerId") peerId: Long
    ): Long
}
