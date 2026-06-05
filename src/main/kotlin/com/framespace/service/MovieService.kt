package com.framespace.service

import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.extension.service.IService
import com.framespace.entity.Movie

/**
 * 电影业务层接口
 * 继承 IService<Movie> 以直接获得 MyBatis-Plus 提供的基础单表操作能力
 */
interface MovieService : IService<Movie> {

    /**
     * 分页查询电影列表
     * @param page 前端/客户端传来的当前页码
     * @param size 每页显示的条数
     */
    fun getMovieList(page: Long, size: Long): Page<Movie>

    /**
     * 根据电影 ID 查询详情
     * @param id 电影唯一标示
     */
    fun getMovieById(id: Long): Movie?
}