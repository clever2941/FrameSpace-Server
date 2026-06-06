package com.framespace.entity

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

@TableName("comment_like")
data class CommentLike(
    @TableId(value = "id", type = IdType.AUTO)
    var id: Long? = null,
    @TableField("user_id")
    var userId: Long? = null,
    @TableField("comment_id")
    var commentId: Long? = null,
    @TableField("created_at")
    var createdAt: LocalDateTime? = null
)
