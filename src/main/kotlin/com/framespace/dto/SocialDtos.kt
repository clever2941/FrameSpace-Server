package com.framespace.dto

import com.framespace.entity.Movie
import java.time.LocalDateTime

data class PersonDto(
    val id: Int,
    val name: String,
    val role: String,
    val photoUrl: String?
)

data class ReviewDto(
    val id: String,
    val author: String,
    val content: String,
    val rating: Double?,
    val source: String,
    val createdAt: String? = null
)

data class ReviewPageDto(
    val records: List<ReviewDto>,
    val nextCursor: String? = null,
    val hasMore: Boolean
)

data class CommentDto(
    val id: Long,
    val userId: Long,
    val username: String,
    val nickname: String?,
    val avatarUrl: String?,
    val content: String,
    val likeCount: Int,
    val likedByMe: Boolean,
    val createdAt: LocalDateTime,
    val parentCommentId: Long? = null,
    val replyToUserId: Long? = null,
    val replyToUsername: String? = null,
    val replies: List<CommentDto> = emptyList()
)

data class UserProfileDto(
    val id: Long,
    val username: String,
    val nickname: String?,
    val avatarUrl: String?,
    val bio: String?,
    val isSelf: Boolean = false
)

data class UpdateProfileRequest(
    val nickname: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null
)

data class UserCommentItemDto(
    val id: Long,
    val movieId: Long,
    val movieTitle: String,
    val content: String,
    val likeCount: Int,
    val createdAt: LocalDateTime
)

data class UserSpaceDto(
    val profile: UserProfileDto,
    val comments: List<UserCommentItemDto>,
    val favorites: List<FavoriteMovieDto>,
    val friendshipStatus: String? = null,
    val friendRequestId: Long? = null
)

data class FavoriteToggleDto(val favorited: Boolean)

data class AvatarUploadDto(val avatarUrl: String)

data class MovieExtrasDto(
    val stills: List<String>,
    val directors: List<PersonDto>,
    val cast: List<PersonDto>
)

data class MovieDetailDto(
    val movie: Movie,
    val stills: List<String>,
    val directors: List<PersonDto>,
    val cast: List<PersonDto>,
    val reviews: List<ReviewDto>,
    val reviewsHasMore: Boolean = false,
    val extrasAvailable: Boolean = false,
    val comments: List<CommentDto>,
    val movieLikeCount: Long,
    val likedByMe: Boolean,
    val favoritedByMe: Boolean
)

data class PostCommentRequest(
    val content: String,
    val parentCommentId: Long? = null
)

data class NotificationDto(
    val id: Long,
    val type: String,
    val actorId: Long,
    val actorName: String,
    val actorAvatarUrl: String?,
    val commentId: Long,
    val movieId: Long?,
    val movieTitle: String?,
    val preview: String?,
    val read: Boolean,
    val createdAt: LocalDateTime
)

data class UnreadCountDto(val count: Long)

data class FavoriteMovieDto(
    val movie: Movie,
    val favoritedAt: LocalDateTime
)
