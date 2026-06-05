package com.framespace.dto

/**
 * 接收 Android 客户端传来的登录/注册请求体
 */
data class AuthRequest(
    val username: String,
    val password: String
)