package com.framespace.entity

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

@TableName("friend_request")
data class FriendRequest(
    @TableId(value = "id", type = IdType.AUTO)
    var id: Long? = null,
    @TableField("from_user_id")
    var fromUserId: Long? = null,
    @TableField("to_user_id")
    var toUserId: Long? = null,
    @TableField("status")
    var status: String? = null,
    @TableField("created_at")
    var createdAt: LocalDateTime? = null,
    @TableField("updated_at")
    var updatedAt: LocalDateTime? = null
)
