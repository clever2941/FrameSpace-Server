package com.framespace.service

import com.framespace.entity.Movie
import com.framespace.mapper.MovieMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MovieTitleRefreshService(
    private val movieMapper: MovieMapper,
    private val tmdbService: TmdbService
) {

    private val log = LoggerFactory.getLogger(MovieTitleRefreshService::class.java)

    fun refreshChineseTitles(): RefreshResult {
        val movies = movieMapper.selectList(null).filter { it.tmdbId != null }
        var updated = 0
        var skipped = 0
        var failed = 0

        movies.forEach { movie ->
            val tmdbId = movie.tmdbId ?: return@forEach
            if (containsHan(movie.title.orEmpty())) {
                skipped++
                return@forEach
            }
            try {
                val detail = tmdbService.fetchMovieDetail(tmdbId) ?: run {
                    failed++
                    return@forEach
                }
                val zhTitle = detail.path("title").asText("").trim()
                if (zhTitle.isBlank() || !containsHan(zhTitle)) {
                    skipped++
                    return@forEach
                }
                movie.title = zhTitle
                movie.originalTitle = detail.path("original_title").asText(movie.originalTitle)
                movieMapper.updateById(movie)
                updated++
            } catch (e: Exception) {
                log.warn("刷新片名失败 movieId={} tmdbId={}: {}", movie.id, tmdbId, e.message)
                failed++
            }
        }
        log.info("中文片名刷新完成: updated={}, skipped={}, failed={}", updated, skipped, failed)
        return RefreshResult(updated, skipped, failed, movies.size)
    }

    private fun containsHan(text: String): Boolean =
        text.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }

    data class RefreshResult(
        val updated: Int,
        val skipped: Int,
        val failed: Int,
        val total: Int
    )
}
