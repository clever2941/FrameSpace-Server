package com.framespace.mapper

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.framespace.entity.User
import org.apache.ibatis.annotations.Mapper

@Mapper
interface UserMapper : BaseMapper<User>