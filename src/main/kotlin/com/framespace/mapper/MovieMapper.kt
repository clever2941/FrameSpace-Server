package com.framespace.mapper

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.framespace.entity.Movie
import org.apache.ibatis.annotations.Mapper

@Mapper
interface MovieMapper : BaseMapper<Movie>