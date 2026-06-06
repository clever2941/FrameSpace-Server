package com.framespace.service.impl

import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.framespace.common.MovieCategory
import com.framespace.entity.Movie
import com.framespace.mapper.MovieMapper
import com.framespace.service.ImageUrlService
import com.framespace.service.MovieService
import com.framespace.service.MovieSyncService
import com.framespace.service.TmdbService
import org.springframework.stereotype.Service

@Service
class MovieServiceImpl(
    private val movieMapper: MovieMapper,
    private val movieSyncService: MovieSyncService,
    private val imageUrlService: ImageUrlService,
    private val tmdbService: TmdbService
) : ServiceImpl<MovieMapper, Movie>(), MovieService {

    override fun getNowPlaying(
        page: Long,
        size: Long,
        region: String?,
        genreId: Int?,
        year: String?
    ): Page<Movie> {
        movieSyncService.syncNowPlayingIfNeeded()
        val mybatisPage = Page<Movie>(page + 1, size)
        val pageResult = movieMapper.selectByFilters(
            mybatisPage,
            MovieCategory.NOW_PLAYING,
            region,
            genreId,
            year
        )
        pageResult.setRecords(imageUrlService.rewriteMovies(pageResult.getRecords()))
        return pageResult
    }

    override fun getTop500(
        page: Long,
        size: Long,
        genreId: Int?,
        year: String?
    ): Page<Movie> {
        movieSyncService.syncTop500IfNeeded()
        val mybatisPage = Page<Movie>(page + 1, size)
        val pageResult = movieMapper.selectByFilters(
            mybatisPage,
            MovieCategory.TOP500,
            null,
            genreId,
            year
        )
        pageResult.setRecords(imageUrlService.rewriteMovies(pageResult.getRecords()))
        return pageResult
    }

    override fun getMovieById(id: Long): Movie? {
        val movie = getById(id) ?: return null
        val enriched = movieSyncService.enrichMovieDetail(movie)
        if (enriched != movie) {
            updateById(enriched)
        }
        return imageUrlService.rewriteMovie(enriched)
    }

    override fun searchMovies(query: String, page: Long, size: Long): Page<Movie> {
        val safeSize = size.coerceIn(1, 20)
        val tmdbPage = (page + 1).toInt().coerceAtLeast(1)
        val response = tmdbService.searchMovies(query.trim(), tmdbPage)
            ?: return Page(page + 1, safeSize)

        val results = response.path("results")
            .map { node -> movieSyncService.toSearchMovie(node) }
            .take(safeSize.toInt())
        val pageResult = Page<Movie>(page + 1, safeSize)
        pageResult.setRecords(imageUrlService.rewriteMovies(results))
        pageResult.total = response.path("total_results").asLong(0)
        return pageResult
    }

    override fun ensureMovieByTmdbId(tmdbId: Int): Movie {
        val movie = movieSyncService.ensureMovieByTmdbId(tmdbId)
        return imageUrlService.rewriteMovie(movie)
    }
}
