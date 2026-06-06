package com.framespace.entity

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

@TableName("movie_like")
data class MovieLike(
    @TableId(value = "id", type = IdType.AUTO)
    var id: Long? = null,
    @TableField("user_id")
    var userId: Long? = null,
    @TableField("movie_id")
    var movieId: Long? = null,
    @TableField("created_at")
    var createdAt: LocalDateTime? = null
)
