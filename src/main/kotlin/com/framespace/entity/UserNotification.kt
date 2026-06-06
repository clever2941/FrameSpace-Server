package com.framespace.entity

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

@TableName("user_notification")
data class UserNotification(
    @TableId(value = "id", type = IdType.AUTO)
    var id: Long? = null,
    @TableField("user_id")
    var userId: Long? = null,
    @TableField("actor_id")
    var actorId: Long? = null,
    @TableField("type")
    var type: String? = null,
    @TableField("comment_id")
    var commentId: Long? = null,
    @TableField("movie_id")
    var movieId: Long? = null,
    @TableField("preview")
    var preview: String? = null,
    @TableField("read_at")
    var readAt: LocalDateTime? = null,
    @TableField("created_at")
    var createdAt: LocalDateTime? = null
)
