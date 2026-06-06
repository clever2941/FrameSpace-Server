package com.framespace.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.framespace.common.NotificationType
import com.framespace.dto.NotificationDto
import com.framespace.entity.UserNotification
import com.framespace.mapper.MovieMapper
import com.framespace.mapper.UserMapper
import com.framespace.mapper.UserNotificationMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class NotificationService(
    private val userNotificationMapper: UserNotificationMapper,
    private val userMapper: UserMapper,
    private val movieMapper: MovieMapper,
    private val userProfileService: UserProfileService
) {

    fun listNotifications(userId: Long, limit: Int = 50): List<NotificationDto> =
        userNotificationMapper.selectByUserId(userId, limit).map { toDto(it) }

    fun unreadCount(userId: Long): Long =
        userNotificationMapper.countUnread(userId)

    @Transactional
    fun markRead(userId: Long, notificationId: Long) {
        val notification = userNotificationMapper.selectById(notificationId)
            ?: return
        if (notification.userId != userId) return
        if (notification.readAt != null) return
        notification.readAt = LocalDateTime.now()
        userNotificationMapper.updateById(notification)
    }

    @Transactional
    fun markAllRead(userId: Long) {
        val unread = userNotificationMapper.selectList(
            QueryWrapper<UserNotification>()
                .eq("user_id", userId)
                .isNull("read_at")
        )
        val now = LocalDateTime.now()
        unread.forEach {
            it.readAt = now
            userNotificationMapper.updateById(it)
        }
    }

    fun notifyCommentLike(actorId: Long, commentOwnerId: Long, commentId: Long, movieId: Long) {
        if (actorId == commentOwnerId) return
        insert(
            userId = commentOwnerId,
            actorId = actorId,
            type = NotificationType.COMMENT_LIKE,
            commentId = commentId,
            movieId = movieId,
            preview = null
        )
    }

    fun notifyCommentReply(
        actorId: Long,
        targetUserId: Long,
        commentId: Long,
        movieId: Long,
        preview: String
    ) {
        if (actorId == targetUserId) return
        insert(
            userId = targetUserId,
            actorId = actorId,
            type = NotificationType.COMMENT_REPLY,
            commentId = commentId,
            movieId = movieId,
            preview = preview.take(200)
        )
    }

    private fun insert(
        userId: Long,
        actorId: Long,
        type: String,
        commentId: Long,
        movieId: Long?,
        preview: String?
    ) {
        userNotificationMapper.insert(
            UserNotification(
                userId = userId,
                actorId = actorId,
                type = type,
                commentId = commentId,
                movieId = movieId,
                preview = preview,
                createdAt = LocalDateTime.now()
            )
        )
    }

    private fun toDto(notification: UserNotification): NotificationDto {
        val actor = userMapper.selectById(notification.actorId)
        val movie = notification.movieId?.let { movieMapper.selectById(it) }
        return NotificationDto(
            id = notification.id!!,
            type = notification.type ?: "",
            actorId = notification.actorId!!,
            actorName = userProfileService.displayName(actor),
            actorAvatarUrl = userProfileService.avatarUrl(actor),
            commentId = notification.commentId!!,
            movieId = notification.movieId,
            movieTitle = movie?.title,
            preview = notification.preview,
            read = notification.readAt != null,
            createdAt = notification.createdAt ?: LocalDateTime.now()
        )
    }
}
