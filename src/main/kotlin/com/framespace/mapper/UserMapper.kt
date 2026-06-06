package com.framespace.mapper

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.framespace.entity.User
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select

@Mapper
interface UserMapper : BaseMapper<User> {

    @Select(
        """
        SELECT * FROM user
        WHERE id != #{excludeId}
          AND (username LIKE CONCAT('%', #{q}, '%') OR nickname LIKE CONCAT('%', #{q}, '%'))
        ORDER BY id ASC
        LIMIT #{limit}
        """
    )
    fun searchByKeyword(
        @Param("q") q: String,
        @Param("excludeId") excludeId: Long,
        @Param("limit") limit: Int
    ): List<User>
}