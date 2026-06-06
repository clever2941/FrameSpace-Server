package com.framespace.utils

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtUtils(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.expiration-ms:604800000}") private val expirationMs: Long
) {
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    fun generateToken(userId: Long, username: String): String {
        val now = Date()
        val expiryDate = Date(now.time + expirationMs)

        return Jwts.builder()
            .claim("userId", userId)
            .claim("username", username)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    fun parseToken(token: String): Claims? {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (_: Exception) {
            null
        }
    }
}
