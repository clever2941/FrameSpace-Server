package com.framespace.entity

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

@TableName("user_comment")
data class UserComment(
    @TableId(value = "id", type = IdType.AUTO)
    var id: Long? = null,
    @TableField("user_id")
    var userId: Long? = null,
    @TableField("movie_id")
    var movieId: Long? = null,
    @TableField("parent_comment_id")
    var parentCommentId: Long? = null,
    @TableField("reply_to_user_id")
    var replyToUserId: Long? = null,
    @TableField("tmdb_id")
    var tmdbId: Int? = null,
    @TableField("content")
    var content: String? = null,
    @TableField("like_count")
    var likeCount: Int = 0,
    @TableField("created_at")
    var createdAt: LocalDateTime? = null
)
