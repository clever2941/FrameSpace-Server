package com.framespace.entity

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

@TableName("movie")
data class Movie(
    @TableId(value = "id", type = IdType.AUTO)
    var id: Long? = null,

    @TableField("tmdb_id")
    var tmdbId: Int? = null,

    @TableField("title")
    var title: String? = null,

    @TableField("original_title")
    var originalTitle: String? = null,

    @TableField("poster_url")
    var posterUrl: String? = null,

    @TableField("backdrop_url")
    var backdropUrl: String? = null,

    @TableField("rating")
    var rating: Double? = null,

    @TableField("summary")
    var summary: String? = null,

    @TableField("release_year")
    var releaseYear: String? = null,

    @TableField("category")
    var category: String? = null,

    @TableField("region")
    var region: String? = null,

    @TableField("genres")
    var genres: String? = null,

    @TableField("genre_ids")
    var genreIds: String? = null,

    @TableField("rank_num")
    var rankNum: Int? = null,

    @TableField("runtime")
    var runtime: Int? = null,

    @TableField("director")
    var director: String? = null,

    @TableField("cast_list")
    var castList: String? = null,

    @TableField("synced_at")
    var syncedAt: LocalDateTime? = null
)
