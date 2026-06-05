package com.framespace.service.impl

import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.framespace.entity.Movie
import com.framespace.mapper.MovieMapper
import com.framespace.service.MovieService
import org.springframework.stereotype.Service

/**
 * 电影业务层实现类
 * 继承 ServiceImpl 并实现 MovieService 接口
 */
@Service
class MovieServiceImpl : ServiceImpl<MovieMapper, Movie>(), MovieService {

    override fun getMovieList(page: Long, size: Long): Page<Movie> {
        // 核心逻辑：Android 客户端分页习惯从 0 开始（第 0 页表示第一页）
        // 而 MyBatis-Plus 的 Page 插件页码是从 1 开始，因此这里进行 +1 修正
        val mybatisPage = Page<Movie>(page + 1, size)

        // 调用 ServiceImpl 提供的 page 方法执行物理分页查询
        // 传入 null 表示不需要任何过滤条件，直接查询全量电影列表
        return this.page(mybatisPage)
    }

    override fun getMovieById(id: Long): Movie? {
        // 调用 ServiceImpl 提供的 getById 直接根据主键查询
        // 如果数据库中不存在该记录，会安全地返回 null
        return this.getById(id)
    }
}