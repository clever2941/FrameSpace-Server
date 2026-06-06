package com.framespace.service

import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.extension.service.IService
import com.framespace.entity.Movie

interface MovieService : IService<Movie> {

    fun getNowPlaying(
        page: Long,
        size: Long,
        region: String?,
        genreId: Int?,
        year: String?
    ): Page<Movie>

    fun getTop500(
        page: Long,
        size: Long,
        genreId: Int?,
        year: String?
    ): Page<Movie>

    fun getMovieById(id: Long): Movie?

    fun searchMovies(query: String, page: Long, size: Long): Page<Movie>

    fun ensureMovieByTmdbId(tmdbId: Int): Movie
}
