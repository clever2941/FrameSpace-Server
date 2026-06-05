package com.framespace.controller

import com.framespace.common.Result
import com.framespace.entity.Movie
import com.framespace.service.MovieService
import org.springframework.web.bind.annotation.*

/**
 * 电影模块前端控制器
 * 使用构造器注入的方式引入 MovieService
 */
@RestController
@RequestMapping("/api/movies")
class MovieController(private val movieService: MovieService) {

    /**
     * 1. 电影列表接口 (支持分页)
     * 请求路径：GET /api/movies?page=0&size=10
     */
    @GetMapping
    fun getMovies(
        @RequestParam(value = "page", defaultValue = "0") page: Long,
        @RequestParam(value = "size", defaultValue = "10") size: Long
    ): Result<List<Movie>> {
        // 1. 调用 Service 层获取分页数据对象
        val moviePage = movieService.getMovieList(page, size)

        // 2. 从分页结果对象中提取出当前的电影列表数据 (List<Movie>)
        val movies: List<Movie> = moviePage.records

        // 3. 将数据放入统一的包装类中返回给 Android 端
        return Result.success(movies)
    }

    /**
     * 2. 电影详情接口
     * 请求路径：GET /api/movies/{id}
     */
    @GetMapping("/{id}")
    fun getMovieDetail(@PathVariable("id") id: Long): Result<Movie> {
        // 1. 调用 Service 层根据 ID 查询电影详情
        val movie = movieService.getMovieById(id)

        // 2. 优雅的异常与空值处理
        return if (movie != null) {
            // 如果电影存在，返回 200 状态码并携带电影对象
            Result.success(movie)
        } else {
            // 如果电影不存在（如传入了非法的 ID），返回 404 状态码和明确的错误提示
            Result.error(404, "很抱歉，未找到该电影的信息或已被下架")
        }
    }
}