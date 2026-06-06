package com.framespace.service

import com.fasterxml.jackson.databind.JsonNode
import com.framespace.common.GenreCatalog
import com.framespace.common.MovieCategory
import com.framespace.entity.Movie
import com.framespace.mapper.MovieMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean

@Service
class MovieSyncService(
    private val movieMapper: MovieMapper,
    private val tmdbService: TmdbService
) {

    private val log = LoggerFactory.getLogger(MovieSyncService::class.java)
    private val top500Syncing = AtomicBoolean(false)

    fun syncNowPlayingIfNeeded(force: Boolean = false) {
        val latest = movieMapper.selectLatestSyncedAt(MovieCategory.NOW_PLAYING)
        if (!force && latest != null && latest.isAfter(LocalDateTime.now().minusHours(6))) {
            return
        }

        val cnMovies = tmdbService.fetchNowPlaying("CN", "zh-CN")
            .map { toMovie(it, MovieCategory.NOW_PLAYING, "CN") }
        val usMovies = tmdbService.fetchNowPlaying("US", "en-US")
            .map { toMovie(it, MovieCategory.NOW_PLAYING, "US") }

        val cnTitleByTmdb = cnMovies
            .filter { it.tmdbId != null && containsHan(it.title.orEmpty()) }
            .associateBy({ it.tmdbId!! }, { it.title!! })
        val merged = (cnMovies + usMovies.map { us ->
            val zhTitle = us.tmdbId?.let { cnTitleByTmdb[it] }
            if (zhTitle != null) us.copy(title = zhTitle) else us
        })
            .filter { it.tmdbId != null }
            .distinctBy { it.tmdbId }

        upsertCategoryMovies(merged, MovieCategory.NOW_PLAYING)
    }

    /**
     * Top500 列表接口专用：优先返回库内已有数据，避免每次请求都同步 500 部。
     * - 7 天内已同步 → 跳过
     * - 库内有数据但过期 → 先返回旧数据，后台全量刷新
     * - 库内无数据 → 仅同步前 2 页（约 40 部）后返回，后台补全剩余
     */
    fun syncTop500IfNeeded(force: Boolean = false) {
        if (force) {
            syncTop500Full(force = true)
            return
        }

        val latest = movieMapper.selectLatestSyncedAt(MovieCategory.TOP500)
        val isFresh = latest != null && latest.isAfter(LocalDateTime.now().minusDays(7))
        if (isFresh) return

        val existingCount = movieMapper.countByCategory(MovieCategory.TOP500)
        if (existingCount > 0 && !force) {
            triggerTop500BackgroundSync(replaceAll = true)
            return
        }

        syncTop500Pages(fromPage = 1, toPage = QUICK_SYNC_PAGES, replaceAll = true)
        triggerTop500BackgroundSync(replaceAll = true, fromPage = QUICK_SYNC_PAGES + 1)
    }

    fun syncTop500Full(force: Boolean = false) {
        val latest = movieMapper.selectLatestSyncedAt(MovieCategory.TOP500)
        if (!force && latest != null && latest.isAfter(LocalDateTime.now().minusDays(7))) {
            return
        }
        syncTop500Pages(fromPage = 1, toPage = FULL_SYNC_PAGES, replaceAll = true)
    }

    private fun triggerTop500BackgroundSync(replaceAll: Boolean, fromPage: Int = 1) {
        if (!top500Syncing.compareAndSet(false, true)) return
        Thread {
            try {
                log.info("Top500 后台同步开始 (page {}-{})", fromPage, FULL_SYNC_PAGES)
                syncTop500Pages(fromPage = fromPage, toPage = FULL_SYNC_PAGES, replaceAll = replaceAll && fromPage == 1)
            } catch (e: Exception) {
                log.warn("Top500 后台同步失败: {}", e.message)
            } finally {
                top500Syncing.set(false)
            }
        }.apply {
            isDaemon = true
            name = "top500-sync"
            start()
        }
    }

    private fun syncTop500Pages(fromPage: Int, toPage: Int, replaceAll: Boolean) {
        val allMovies = mutableListOf<Movie>()
        var rank = if (replaceAll) 1 else movieMapper.countByCategory(MovieCategory.TOP500).toInt() + 1

        for (page in fromPage..toPage) {
            val pageMovies = tmdbService.fetchTopRatedPage(page)
            if (pageMovies.isEmpty()) break
            pageMovies.forEach { node ->
                if (rank <= 500) {
                    allMovies += toMovie(node, MovieCategory.TOP500, "GLOBAL", rank)
                    rank++
                }
            }
        }

        if (replaceAll) {
            upsertCategoryMovies(allMovies, MovieCategory.TOP500)
        } else {
            allMovies.forEach { upsertCategoryMovie(it) }
        }
        log.info("Top500 同步完成 {} 部 (page {}-{})", allMovies.size, fromPage, toPage)
    }

    private fun upsertCategoryMovies(movies: List<Movie>, category: String) {
        val newTmdbIds = movies.mapNotNull { it.tmdbId }.toSet()
        movies.forEach { upsertCategoryMovie(it) }
        movieMapper.selectAllByCategory(category)
            .filter { it.tmdbId != null && it.tmdbId !in newTmdbIds }
            .forEach { stale ->
                movieMapper.updateById(stale.copy(category = MovieCategory.DISCOVER))
            }
    }

    private fun upsertCategoryMovie(incoming: Movie) {
        val tmdbId = incoming.tmdbId ?: run {
            movieMapper.insert(incoming)
            return
        }
        val category = incoming.category ?: MovieCategory.DISCOVER
        val existing = movieMapper.selectByTmdbIdAndCategory(tmdbId, category)
            ?: movieMapper.selectByTmdbIdAndCategory(tmdbId, MovieCategory.DISCOVER)

        if (existing != null) {
            movieMapper.updateById(
                incoming.copy(
                    id = existing.id,
                    category = category
                )
            )
        } else {
            movieMapper.insert(incoming)
        }
    }

    fun ensureMovieByTmdbId(tmdbId: Int): Movie {
        movieMapper.selectByTmdbId(tmdbId)?.let { return it }
        val detail = tmdbService.fetchMovieDetail(tmdbId)
            ?: throw IllegalArgumentException("TMDB 电影不存在")
        val movie = toMovieFromDetail(detail)
        movieMapper.insert(movie)
        return movie
    }

    fun enrichMovieDetail(movie: Movie): Movie {
        val tmdbId = movie.tmdbId ?: return movie
        val detail = tmdbService.fetchMovieDetail(tmdbId) ?: return movie

        val director = detail.path("credits").path("crew")
            .firstOrNull { it.path("job").asText() == "Director" }
            ?.path("name")?.asText()

        val cast = detail.path("credits").path("cast")
            .take(5)
            .joinToString(",") { it.path("name").asText() }

        val genreIds = detail.path("genres").map { it.path("id").asInt() }
        val genres = GenreCatalog.namesByIds(genreIds)

        return movie.copy(
            title = detail.path("title").asText(movie.title),
            originalTitle = detail.path("original_title").asText(movie.originalTitle),
            posterUrl = tmdbService.buildPosterUrl(detail.path("poster_path").asText(null))
                ?: movie.posterUrl,
            backdropUrl = tmdbService.buildBackdropUrl(detail.path("backdrop_path").asText(null))
                ?: movie.backdropUrl,
            rating = detail.path("vote_average").asDouble(movie.rating ?: 0.0),
            summary = detail.path("overview").asText(movie.summary),
            releaseYear = detail.path("release_date").asText("").take(4).ifBlank { movie.releaseYear },
            genres = genres.ifBlank { movie.genres },
            genreIds = genreIds.joinToString(",").ifBlank { movie.genreIds },
            runtime = detail.path("runtime").asInt(0).takeIf { it > 0 } ?: movie.runtime,
            director = director ?: movie.director,
            castList = cast.ifBlank { movie.castList },
            syncedAt = LocalDateTime.now()
        )
    }

    private fun containsHan(text: String): Boolean =
        text.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }

    fun toSearchMovie(node: JsonNode): Movie = toMovie(node, MovieCategory.DISCOVER, "GLOBAL")

    fun toMovieFromDetail(detail: JsonNode, category: String = MovieCategory.DISCOVER): Movie {
        val genreIds = detail.path("genres").map { it.path("id").asInt() }
        val director = detail.path("credits").path("crew")
            .firstOrNull { it.path("job").asText() == "Director" }
            ?.path("name")?.asText()
        val cast = detail.path("credits").path("cast")
            .take(5)
            .joinToString(",") { it.path("name").asText() }

        return Movie(
            tmdbId = detail.path("id").asInt(),
            title = detail.path("title").asText(),
            originalTitle = detail.path("original_title").asText(),
            posterUrl = tmdbService.buildPosterUrl(detail.path("poster_path").asText(null)),
            backdropUrl = tmdbService.buildBackdropUrl(detail.path("backdrop_path").asText(null)),
            rating = detail.path("vote_average").asDouble(0.0),
            summary = detail.path("overview").asText(),
            releaseYear = detail.path("release_date").asText("").take(4).ifBlank { null },
            category = category,
            region = "GLOBAL",
            genres = GenreCatalog.namesByIds(genreIds),
            genreIds = genreIds.joinToString(","),
            runtime = detail.path("runtime").asInt(0).takeIf { it > 0 },
            director = director,
            castList = cast.ifBlank { null },
            syncedAt = LocalDateTime.now()
        )
    }

    private fun toMovie(
        node: JsonNode,
        category: String,
        region: String,
        rankNum: Int? = null
    ): Movie {
        val genreIds = node.path("genre_ids").map { it.asInt() }
        return Movie(
            tmdbId = node.path("id").asInt(),
            title = node.path("title").asText(),
            originalTitle = node.path("original_title").asText(),
            posterUrl = tmdbService.buildPosterUrl(node.path("poster_path").asText(null)),
            backdropUrl = tmdbService.buildBackdropUrl(node.path("backdrop_path").asText(null)),
            rating = node.path("vote_average").asDouble(),
            summary = node.path("overview").asText(),
            releaseYear = node.path("release_date").asText("").take(4).ifBlank { null },
            category = category,
            region = region,
            genres = GenreCatalog.namesByIds(genreIds),
            genreIds = genreIds.joinToString(","),
            rankNum = rankNum,
            syncedAt = LocalDateTime.now()
        )
    }

    companion object {
        private const val QUICK_SYNC_PAGES = 2
        private const val FULL_SYNC_PAGES = 25
    }
}
