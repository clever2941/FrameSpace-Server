package com.framespace.entity

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

@TableName("user")
data class User(
    @TableId(value = "id", type = IdType.AUTO)
    var id: Long? = null,

    @TableField("username")
    var username: String? = null,

    @TableField("password_hash")
    var passwordHash: String? = null,

    @TableField("created_at")
    var createdAt: LocalDateTime? = null
)