package com.framespace.entity

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

@TableName("private_message")
data class PrivateMessage(
    @TableId(value = "id", type = IdType.AUTO)
    var id: Long? = null,
    @TableField("sender_id")
    var senderId: Long? = null,
    @TableField("receiver_id")
    var receiverId: Long? = null,
    @TableField("message_type")
    var messageType: String? = null,
    @TableField("content")
    var content: String? = null,
    @TableField("movie_id")
    var movieId: Long? = null,
    @TableField("read_at")
    var readAt: LocalDateTime? = null,
    @TableField("created_at")
    var createdAt: LocalDateTime? = null
)
