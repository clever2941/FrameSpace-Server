package com.framespace.dto

import java.time.LocalDateTime

data class FriendUserDto(
    val id: Long,
    val username: String,
    val nickname: String?,
    val avatarUrl: String?
) {
    val displayName: String get() = nickname?.takeIf { it.isNotBlank() } ?: username
}

data class FriendRequestDto(
    val id: Long,
    val fromUser: FriendUserDto,
    val createdAt: LocalDateTime
)

data class FriendshipStatusDto(
    val status: String,
    val requestId: Long? = null
)

data class PrivateMessageDto(
    val id: Long,
    val senderId: Long,
    val receiverId: Long,
    val messageType: String,
    val content: String?,
    val movieId: Long?,
    val movieTitle: String?,
    val moviePosterUrl: String?,
    val read: Boolean,
    val createdAt: LocalDateTime,
    val senderName: String,
    val senderAvatarUrl: String?
)

data class ConversationDto(
    val peer: FriendUserDto,
    val lastMessage: PrivateMessageDto?,
    val unreadCount: Long,
    val updatedAt: LocalDateTime?
)

data class SendMessageRequest(
    val receiverId: Long,
    val content: String
)

data class PushMovieRequest(
    val friendIds: List<Long>,
    val message: String? = null
)

data class PushMovieResultDto(
    val sentCount: Int
)

data class DmUnreadCountDto(val count: Long)
