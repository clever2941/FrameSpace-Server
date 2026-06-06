package com.framespace.controller

import com.framespace.common.GenreCatalog
import com.framespace.common.Result
import com.framespace.dto.EnsureMovieDto
import com.framespace.dto.MovieDetailDto
import com.framespace.dto.MoviePageDto
import com.framespace.entity.Movie
import com.framespace.service.MovieDetailService
import com.framespace.service.MovieService
import com.framespace.service.MovieSyncService
import com.framespace.service.MovieTitleRefreshService
import com.framespace.utils.JwtAuthHelper
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/movies")
class MovieController(
    private val movieService: MovieService,
    private val movieDetailService: MovieDetailService,
    private val movieSyncService: MovieSyncService,
    private val movieTitleRefreshService: MovieTitleRefreshService,
    private val jwtAuthHelper: JwtAuthHelper
) {

    @GetMapping("/now-playing")
    fun getNowPlaying(
        @RequestParam(value = "page", defaultValue = "0") page: Long,
        @RequestParam(value = "size", defaultValue = "15") size: Long,
        @RequestParam(value = "region", defaultValue = "ALL") region: String,
        @RequestParam(value = "genreId", required = false) genreId: Int?,
        @RequestParam(value = "year", required = false) year: String?
    ): Result<MoviePageDto> {
        val moviePage = movieService.getNowPlaying(page, size, region, genreId, year)
        return Result.success(toPageDto(moviePage, page, size))
    }

    @GetMapping("/top500")
    fun getTop500(
        @RequestParam(value = "page", defaultValue = "0") page: Long,
        @RequestParam(value = "size", defaultValue = "15") size: Long,
        @RequestParam(value = "genreId", required = false) genreId: Int?,
        @RequestParam(value = "year", required = false) year: String?
    ): Result<MoviePageDto> {
        val moviePage = movieService.getTop500(page, size, genreId, year)
        return Result.success(toPageDto(moviePage, page, size))
    }

    @GetMapping("/genres")
    fun getGenres(): Result<List<com.framespace.dto.GenreDto>> {
        return Result.success(GenreCatalog.all)
    }

    @GetMapping("/search")
    fun searchMovies(
        @RequestParam("q") query: String,
        @RequestParam(value = "page", defaultValue = "0") page: Long,
        @RequestParam(value = "size", defaultValue = "15") size: Long
    ): Result<MoviePageDto> {
        if (query.isBlank()) {
            return Result.success(MoviePageDto(emptyList(), 0, page, size))
        }
        val moviePage = movieService.searchMovies(query, page, size)
        return Result.success(toPageDto(moviePage, page, size))
    }

    @PostMapping("/ensure/{tmdbId}")
    fun ensureMovie(@PathVariable tmdbId: Int): Result<EnsureMovieDto> {
        return try {
            val movie = movieService.ensureMovieByTmdbId(tmdbId)
            val id = movie.id ?: throw IllegalArgumentException("入库失败")
            Result.success(EnsureMovieDto(id = id, tmdbId = tmdbId))
        } catch (e: IllegalArgumentException) {
            Result.error(404, e.message ?: "电影不存在")
        }
    }

    @GetMapping("/{id}/detail")
    fun getMovieDetail(
        @PathVariable("id") id: Long,
        request: HttpServletRequest
    ): Result<MovieDetailDto> {
        return try {
            val userId = jwtAuthHelper.resolveUserId(request)
            Result.success(movieDetailService.getDetail(id, userId))
        } catch (e: IllegalArgumentException) {
            Result.error(404, e.message ?: "电影不存在")
        }
    }

    /** 强制从 TMDB 重新同步电影列表（需本机可访问 TMDB） */
    @PostMapping("/sync")
    fun syncMovies(): Result<String> {
        movieSyncService.syncNowPlayingIfNeeded(force = true)
        movieSyncService.syncTop500IfNeeded(force = true)
        return Result.success("电影列表已重新同步")
    }

    /** 为缺少中文片名的电影从 TMDB 拉取 zh-CN 标题写入 title 字段 */
    @PostMapping("/refresh-chinese-titles")
    fun refreshChineseTitles(): Result<Map<String, Int>> {
        val result = movieTitleRefreshService.refreshChineseTitles()
        return Result.success(
            mapOf(
                "total" to result.total,
                "updated" to result.updated,
                "skipped" to result.skipped,
                "failed" to result.failed
            )
        )
    }

    private fun toPageDto(
        moviePage: com.baomidou.mybatisplus.extension.plugins.pagination.Page<Movie>,
        page: Long,
        size: Long
    ): MoviePageDto = MoviePageDto(
        records = moviePage.getRecords(),
        total = moviePage.getTotal(),
        page = page,
        size = size
    )
}
