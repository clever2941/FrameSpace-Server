package com.framespace.utils

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class JwtAuthHelper(private val jwtUtils: JwtUtils) {

    fun resolveUserId(request: HttpServletRequest): Long? {
        val header = request.getHeader("Authorization") ?: return null
        if (!header.startsWith("Bearer ", ignoreCase = true)) return null
        val token = header.substring(7).trim()
        if (token.isEmpty()) return null
        val claims = jwtUtils.parseToken(token) ?: return null
        val userId = claims["userId"]
        return when (userId) {
            is Int -> userId.toLong()
            is Long -> userId
            is Number -> userId.toLong()
            else -> null
        }
    }

    fun requireUserId(request: HttpServletRequest): Long {
        return resolveUserId(request)
            ?: throw IllegalArgumentException("未登录或登录已过期")
    }
}
