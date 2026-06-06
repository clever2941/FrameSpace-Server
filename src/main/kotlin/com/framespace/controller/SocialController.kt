package com.framespace.controller

import com.framespace.common.Result
import com.framespace.dto.CommentDto
import com.framespace.dto.FavoriteMovieDto
import com.framespace.dto.FavoriteToggleDto
import com.framespace.dto.PostCommentRequest
import com.framespace.entity.Movie
import com.framespace.service.CommentService
import com.framespace.service.SocialService
import com.framespace.utils.JwtAuthHelper
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class SocialController(
    private val commentService: CommentService,
    private val socialService: SocialService,
    private val jwtAuthHelper: JwtAuthHelper
) {

    @GetMapping("/movies/{movieId}/comments")
    fun listComments(
        @PathVariable movieId: Long,
        request: HttpServletRequest
    ): Result<List<CommentDto>> {
        val userId = jwtAuthHelper.resolveUserId(request)
        return Result.success(commentService.listComments(movieId, userId))
    }

    @PostMapping("/movies/{movieId}/comments")
    fun postComment(
        @PathVariable movieId: Long,
        @RequestBody body: PostCommentRequest,
        request: HttpServletRequest
    ): Result<CommentDto> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            Result.success(commentService.postComment(movieId, userId, body.content, body.parentCommentId))
        } catch (e: IllegalArgumentException) {
            Result.error(400, e.message ?: "请求无效")
        }
    }

    @PostMapping("/comments/{commentId}/like")
    fun likeComment(
        @PathVariable commentId: Long,
        request: HttpServletRequest
    ): Result<CommentDto> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            Result.success(commentService.toggleCommentLike(commentId, userId))
        } catch (e: IllegalArgumentException) {
            Result.error(400, e.message ?: "请求无效")
        }
    }

    @DeleteMapping("/comments/{commentId}")
    fun deleteComment(
        @PathVariable commentId: Long,
        request: HttpServletRequest
    ): Result<Unit> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            commentService.deleteComment(commentId, userId)
            Result.success(Unit)
        } catch (e: IllegalArgumentException) {
            Result.error(400, e.message ?: "请求无效")
        }
    }

    @PostMapping("/movies/{movieId}/like")
    fun likeMovie(
        @PathVariable movieId: Long,
        request: HttpServletRequest
    ): Result<Map<String, Any>> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            val (liked, count) = socialService.toggleMovieLike(userId, movieId)
            Result.success(mapOf("liked" to liked, "likeCount" to count))
        } catch (e: IllegalArgumentException) {
            Result.error(400, e.message ?: "请求无效")
        }
    }

    @PostMapping("/movies/{movieId}/favorite")
    fun favoriteMovie(
        @PathVariable movieId: Long,
        request: HttpServletRequest
    ): Result<FavoriteToggleDto> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            val favorited = socialService.toggleFavorite(userId, movieId)
            Result.success(FavoriteToggleDto(favorited = favorited))
        } catch (e: IllegalArgumentException) {
            Result.error(400, e.message ?: "请求无效")
        }
    }

    @GetMapping("/user/favorites")
    fun favorites(request: HttpServletRequest): Result<List<FavoriteMovieDto>> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            Result.success(socialService.listFavorites(userId))
        } catch (e: IllegalArgumentException) {
            Result.error(401, e.message ?: "未登录")
        }
    }

    @GetMapping("/user/history")
    fun history(request: HttpServletRequest): Result<List<Movie>> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            Result.success(socialService.listHistory(userId))
        } catch (e: IllegalArgumentException) {
            Result.error(401, e.message ?: "未登录")
        }
    }
}
