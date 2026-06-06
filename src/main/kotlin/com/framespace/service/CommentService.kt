package com.framespace.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.framespace.dto.CommentDto
import com.framespace.entity.CommentLike
import com.framespace.entity.UserComment
import com.framespace.mapper.CommentLikeMapper
import com.framespace.mapper.MovieMapper
import com.framespace.mapper.UserCommentMapper
import com.framespace.mapper.UserMapper
import com.framespace.websocket.CommentWebSocketHub
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CommentService(
    private val userCommentMapper: UserCommentMapper,
    private val commentLikeMapper: CommentLikeMapper,
    private val userMapper: UserMapper,
    private val movieMapper: MovieMapper,
    private val userProfileService: UserProfileService,
    private val notificationService: NotificationService,
    private val commentWebSocketHub: CommentWebSocketHub
) {

    fun listComments(movieId: Long, currentUserId: Long?): List<CommentDto> {
        val comments = userCommentMapper.selectAllByMovieId(movieId, 500)
        val dtoMap = comments.associate { comment ->
            comment.id!! to toDto(comment, currentUserId, includeReplies = false)
        }
        val repliesByParent = comments
            .filter { it.parentCommentId != null }
            .groupBy { it.parentCommentId!! }

        return comments
            .filter { it.parentCommentId == null }
            .sortedByDescending { it.createdAt }
            .map { root ->
                val replies = repliesByParent[root.id]
                    ?.sortedBy { it.createdAt }
                    ?.mapNotNull { dtoMap[it.id] }
                    ?: emptyList()
                dtoMap[root.id]!!.copy(replies = replies)
            }
    }

    @Transactional
    fun postComment(
        movieId: Long,
        userId: Long,
        content: String,
        parentCommentId: Long? = null
    ): CommentDto {
        val trimmed = content.trim()
        require(trimmed.isNotBlank()) { "评论内容不能为空" }
        require(trimmed.length <= 1000) { "评论不能超过 1000 字" }

        val movie = movieMapper.selectById(movieId)
            ?: throw IllegalArgumentException("电影不存在")

        var storedParentId: Long? = null
        var replyToUserId: Long? = null
        if (parentCommentId != null) {
            val parent = userCommentMapper.selectById(parentCommentId)
                ?: throw IllegalArgumentException("被回复的评论不存在")
            require(parent.movieId == movieId) { "回复评论与电影不匹配" }
            storedParentId = parent.parentCommentId ?: parent.id
            replyToUserId = parent.userId
        }

        val comment = UserComment(
            userId = userId,
            movieId = movieId,
            parentCommentId = storedParentId,
            replyToUserId = replyToUserId,
            tmdbId = movie.tmdbId,
            content = trimmed,
            likeCount = 0,
            createdAt = LocalDateTime.now()
        )
        userCommentMapper.insert(comment)
        val dto = toDto(comment, userId)

        if (storedParentId != null && replyToUserId != null) {
            notificationService.notifyCommentReply(
                actorId = userId,
                targetUserId = replyToUserId,
                commentId = comment.id!!,
                movieId = movieId,
                preview = trimmed
            )
        }

        commentWebSocketHub.broadcast(movieId, dto)
        return dto
    }

    @Transactional
    fun toggleCommentLike(commentId: Long, userId: Long): CommentDto {
        val comment = userCommentMapper.selectById(commentId)
            ?: throw IllegalArgumentException("评论不存在")
        val existing = commentLikeMapper.selectOne(
            QueryWrapper<CommentLike>()
                .eq("user_id", userId)
                .eq("comment_id", commentId)
        )
        if (existing != null) {
            commentLikeMapper.deleteById(existing.id)
            comment.likeCount = (comment.likeCount - 1).coerceAtLeast(0)
        } else {
            commentLikeMapper.insert(
                CommentLike(userId = userId, commentId = commentId, createdAt = LocalDateTime.now())
            )
            comment.likeCount = comment.likeCount + 1
            notificationService.notifyCommentLike(
                actorId = userId,
                commentOwnerId = comment.userId!!,
                commentId = commentId,
                movieId = comment.movieId!!
            )
        }
        userCommentMapper.updateById(comment)
        return toDto(comment, userId)
    }

    @Transactional
    fun deleteComment(commentId: Long, userId: Long) {
        val comment = userCommentMapper.selectById(commentId)
            ?: throw IllegalArgumentException("评论不存在")
        require(comment.userId == userId) { "只能删除自己的评论" }

        val idsToDelete = mutableListOf(commentId)
        if (comment.parentCommentId == null) {
            userCommentMapper.selectList(
                QueryWrapper<UserComment>().eq("parent_comment_id", commentId)
            ).mapNotNullTo(idsToDelete) { it.id }
        }

        idsToDelete.forEach { id ->
            commentLikeMapper.delete(
                QueryWrapper<CommentLike>().eq("comment_id", id)
            )
            userCommentMapper.deleteById(id)
        }
    }

    private fun toDto(
        comment: UserComment,
        currentUserId: Long?,
        includeReplies: Boolean = true
    ): CommentDto {
        val user = userMapper.selectById(comment.userId)
        val replyToUser = comment.replyToUserId?.let { userMapper.selectById(it) }
        val liked = currentUserId?.let { uid ->
            commentLikeMapper.selectCount(
                QueryWrapper<CommentLike>()
                    .eq("user_id", uid)
                    .eq("comment_id", comment.id)
            ) > 0
        } ?: false
        return CommentDto(
            id = comment.id!!,
            userId = comment.userId!!,
            username = user?.username ?: "影迷",
            nickname = user?.nickname,
            avatarUrl = userProfileService.avatarUrl(user),
            content = comment.content ?: "",
            likeCount = comment.likeCount,
            likedByMe = liked,
            createdAt = comment.createdAt ?: LocalDateTime.now(),
            parentCommentId = comment.parentCommentId,
            replyToUserId = comment.replyToUserId,
            replyToUsername = replyToUser?.let { userProfileService.displayName(it) },
            replies = if (includeReplies) emptyList() else emptyList()
        )
    }
}
