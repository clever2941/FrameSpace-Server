package com.framespace.entity

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

@TableName("user_friend")
data class UserFriend(
    @TableId(value = "id", type = IdType.AUTO)
    var id: Long? = null,
    @TableField("user_id")
    var userId: Long? = null,
    @TableField("friend_id")
    var friendId: Long? = null,
    @TableField("created_at")
    var createdAt: LocalDateTime? = null
)
