package com.framespace.entity

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName

@TableName("movie")
data class Movie(
    @TableId(value = "id", type = IdType.AUTO)
    var id: Long? = null,

    @TableField("title")
    var title: String? = null,

    @TableField("original_title")
    var originalTitle: String? = null,

    @TableField("poster_url")
    var posterUrl: String? = null,

    @TableField("rating")
    var rating: Double? = null,

    @TableField("summary")
    var summary: String? = null,

    @TableField("release_year")
    var releaseYear: String? = null
)