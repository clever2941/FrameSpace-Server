package com.framespace.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.framespace.dto.FavoriteMovieDto
import com.framespace.entity.BrowseHistory
import com.framespace.entity.Movie
import com.framespace.entity.MovieFavorite
import com.framespace.entity.MovieLike
import com.framespace.mapper.BrowseHistoryMapper
import com.framespace.mapper.MovieFavoriteMapper
import com.framespace.mapper.MovieLikeMapper
import com.framespace.mapper.MovieMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SocialService(
    private val movieFavoriteMapper: MovieFavoriteMapper,
    private val browseHistoryMapper: BrowseHistoryMapper,
    private val movieLikeMapper: MovieLikeMapper,
    private val movieMapper: MovieMapper,
    private val imageUrlService: ImageUrlService
) {

    fun recordBrowse(userId: Long, movieId: Long) {
        val existing = browseHistoryMapper.selectOne(
            QueryWrapper<BrowseHistory>()
                .eq("user_id", userId)
                .eq("movie_id", movieId)
        )
        if (existing != null) {
            existing.browsedAt = LocalDateTime.now()
            browseHistoryMapper.updateById(existing)
        } else {
            browseHistoryMapper.insert(
                BrowseHistory(userId = userId, movieId = movieId, browsedAt = LocalDateTime.now())
            )
        }
    }

    @Transactional
    fun toggleFavorite(userId: Long, movieId: Long): Boolean {
        val existing = movieFavoriteMapper.selectOne(
            QueryWrapper<MovieFavorite>()
                .eq("user_id", userId)
                .eq("movie_id", movieId)
        )
        return if (existing != null) {
            movieFavoriteMapper.deleteById(existing.id)
            false
        } else {
            movieFavoriteMapper.insert(
                MovieFavorite(userId = userId, movieId = movieId, createdAt = LocalDateTime.now())
            )
            true
        }
    }

    fun isFavorited(userId: Long?, movieId: Long): Boolean {
        if (userId == null) return false
        return movieFavoriteMapper.selectCount(
            QueryWrapper<MovieFavorite>()
                .eq("user_id", userId)
                .eq("movie_id", movieId)
        ) > 0
    }

    fun listFavorites(userId: Long, limit: Int = 50): List<FavoriteMovieDto> {
        val movieIds = movieFavoriteMapper.selectMovieIdsByUser(userId, limit)
        return movieIds.mapNotNull { id ->
            val movie = movieMapper.selectById(id) ?: return@mapNotNull null
            val fav = movieFavoriteMapper.selectOne(
                QueryWrapper<MovieFavorite>()
                    .eq("user_id", userId)
                    .eq("movie_id", id)
            )
            FavoriteMovieDto(
                movie = imageUrlService.rewriteMovie(movie),
                favoritedAt = fav?.createdAt ?: LocalDateTime.now()
            )
        }
    }

    fun listHistory(userId: Long, limit: Int = 50): List<Movie> {
        val movieIds = browseHistoryMapper.selectMovieIdsByUser(userId, limit)
        return movieIds.mapNotNull { movieMapper.selectById(it) }
            .map { imageUrlService.rewriteMovie(it) }
    }

    @Transactional
    fun toggleMovieLike(userId: Long, movieId: Long): Pair<Boolean, Long> {
        val existing = movieLikeMapper.selectOne(
            QueryWrapper<MovieLike>()
                .eq("user_id", userId)
                .eq("movie_id", movieId)
        )
        val liked = if (existing != null) {
            movieLikeMapper.deleteById(existing.id)
            false
        } else {
            movieLikeMapper.insert(
                MovieLike(userId = userId, movieId = movieId, createdAt = LocalDateTime.now())
            )
            true
        }
        return liked to movieLikeMapper.countByMovieId(movieId)
    }

    fun movieLikeCount(movieId: Long): Long = movieLikeMapper.countByMovieId(movieId)

    fun isMovieLiked(userId: Long?, movieId: Long): Boolean {
        if (userId == null) return false
        return movieLikeMapper.selectCount(
            QueryWrapper<MovieLike>()
                .eq("user_id", userId)
                .eq("movie_id", movieId)
        ) > 0
    }
}
