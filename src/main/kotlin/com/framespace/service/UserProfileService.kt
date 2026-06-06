package com.framespace.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.framespace.dto.FavoriteMovieDto
import com.framespace.dto.UpdateProfileRequest
import com.framespace.dto.UserCommentItemDto
import com.framespace.dto.UserProfileDto
import com.framespace.dto.UserSpaceDto
import com.framespace.entity.User
import com.framespace.mapper.MovieMapper
import com.framespace.mapper.UserCommentMapper
import com.framespace.mapper.UserMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProfileService(
    private val userMapper: UserMapper,
    private val userCommentMapper: UserCommentMapper,
    private val movieMapper: MovieMapper,
    private val socialService: SocialService,
    private val imageUrlService: ImageUrlService
) {

    fun getMyProfile(userId: Long): UserProfileDto {
        val user = userMapper.selectById(userId) ?: throw IllegalArgumentException("用户不存在")
        return toProfileDto(user, isSelf = true)
    }

    @Transactional
    fun updateProfile(userId: Long, request: UpdateProfileRequest): UserProfileDto {
        val user = userMapper.selectById(userId) ?: throw IllegalArgumentException("用户不存在")
        request.nickname?.trim()?.takeIf { it.isNotBlank() }?.let { nickname ->
            require(nickname.length <= 32) { "昵称不能超过 32 字" }
            user.nickname = nickname
        }
        request.bio?.let { bio ->
            require(bio.length <= 200) { "个性签名不能超过 200 字" }
            user.bio = bio.trim().ifBlank { null }
        }
        request.avatarUrl?.let { url ->
            val trimmed = url.trim()
            if (trimmed.isBlank()) {
                user.avatarUrl = null
            } else {
                require(trimmed.length <= 500) { "头像地址过长" }
                user.avatarUrl = trimmed
            }
        }
        userMapper.updateById(user)
        return toProfileDto(user, isSelf = true)
    }

    fun getUserSpace(targetUserId: Long, viewerId: Long?): UserSpaceDto {
        val user = userMapper.selectById(targetUserId) ?: throw IllegalArgumentException("用户不存在")
        val isSelf = viewerId == targetUserId
        val comments = userCommentMapper.selectByUserId(targetUserId, 50).map { comment ->
            val movie = resolveMovieForComment(comment)
            UserCommentItemDto(
                id = comment.id!!,
                movieId = movie?.id ?: comment.movieId!!,
                movieTitle = movie?.title ?: "未知电影",
                content = comment.content ?: "",
                likeCount = comment.likeCount,
                createdAt = comment.createdAt!!
            )
        }
        val favorites = if (isSelf) {
            socialService.listFavorites(targetUserId)
        } else {
            socialService.listFavorites(targetUserId, limit = 20)
        }
        return UserSpaceDto(
            profile = toProfileDto(user, isSelf = isSelf),
            comments = comments,
            favorites = favorites
        )
    }

    fun displayName(user: User?): String =
        user?.nickname?.takeIf { it.isNotBlank() } ?: user?.username ?: "影迷"

    fun avatarUrl(user: User?): String? =
        imageUrlService.buildAvatarUrl(user?.avatarUrl)

    private fun resolveMovieForComment(comment: com.framespace.entity.UserComment): com.framespace.entity.Movie? {
        movieMapper.selectById(comment.movieId)?.let { return it }
        val tmdbId = comment.tmdbId ?: return null
        return movieMapper.selectByTmdbId(tmdbId)
    }

    private fun toProfileDto(user: User, isSelf: Boolean): UserProfileDto = UserProfileDto(
        id = user.id!!,
        username = user.username ?: "",
        nickname = user.nickname,
        avatarUrl = avatarUrl(user),
        bio = user.bio,
        isSelf = isSelf
    )
}
