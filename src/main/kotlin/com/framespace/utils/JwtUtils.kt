package com.framespace.utils

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import java.util.Date
import javax.crypto.SecretKey

object JwtUtils {
    // 实际生产环境中，请将此密钥配置在 application.properties 中，避免硬编码。
    // 这里使用 Keys.secretKeyFor 自动生成满足 HS256 长度要求的安全密钥
    private val SECRET_KEY: SecretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256)

    // 过期时间：7天 (毫秒)
    private const val EXPIRATION_TIME = 7L * 24 * 60 * 60 * 1000

    /**
     * 生成 JWT Token
     * @param userId 用户唯一 ID
     * @param username 用户名
     */
    fun generateToken(userId: Long, username: String): String {
        val now = Date()
        val expiryDate = Date(now.time + EXPIRATION_TIME)

        return Jwts.builder()
            .claim("userId", userId)
            .claim("username", username)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SECRET_KEY)
            .compact()
    }

    /**
     * 解析并验证 Token
     * @param token 客户端传来的 JWT 字符串
     * @return 包含用户信息的 Claims 对象。如果解析失败、被篡改或已过期，则返回 null
     */
    fun parseToken(token: String): Claims? {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: Exception) {
            // Token 不合法或已过期
            null
        }
    }
}