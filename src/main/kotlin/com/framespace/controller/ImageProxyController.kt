package com.framespace.controller

import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import com.framespace.service.AvatarUploadService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/api/images")
class ImageProxyController(
    private val restTemplate: RestTemplate,
    private val avatarUploadService: AvatarUploadService
) {

    @GetMapping("/poster")
    fun proxyPoster(
        @RequestParam path: String,
        @RequestParam(required = false) size: String?,
    ): ResponseEntity<ByteArray> {
        val width = resolveWidth(size, default = "w342", allowed = POSTER_WIDTHS)
        return proxy("https://image.tmdb.org/t/p/$width${decodePath(path)}")
    }

    @GetMapping("/backdrop")
    fun proxyBackdrop(
        @RequestParam path: String,
        @RequestParam(required = false) size: String?,
    ): ResponseEntity<ByteArray> {
        val width = resolveWidth(size, default = "w780", allowed = BACKDROP_WIDTHS)
        return proxy("https://image.tmdb.org/t/p/$width${decodePath(path)}")
    }

    @GetMapping("/profile")
    fun proxyProfile(
        @RequestParam path: String,
        @RequestParam(required = false) size: String?,
    ): ResponseEntity<ByteArray> {
        val width = resolveWidth(size, default = "w185", allowed = PROFILE_WIDTHS)
        return proxy("https://image.tmdb.org/t/p/$width${decodePath(path)}")
    }

    @GetMapping("/still")
    fun proxyStill(
        @RequestParam path: String,
        @RequestParam(required = false) size: String?,
    ): ResponseEntity<ByteArray> {
        val width = resolveWidth(size, default = "w342", allowed = POSTER_WIDTHS)
        return proxy("https://image.tmdb.org/t/p/$width${decodePath(path)}")
    }

    @GetMapping("/avatar")
    fun serveAvatar(
        @RequestParam file: String,
        @RequestParam(required = false) max: Int?,
    ): ResponseEntity<ByteArray> {
        val decoded = URLDecoder.decode(file, StandardCharsets.UTF_8)
        val payload = avatarUploadService.readAvatar(decoded, max)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
            .contentType(MediaType.parseMediaType(payload.second))
            .body(payload.first)
    }

    private fun resolveWidth(size: String?, default: String, allowed: Set<String>): String {
        val normalized = size?.trim()?.lowercase()
        return if (normalized != null && normalized in allowed) normalized else default
    }

    private fun decodePath(path: String): String {
        val decoded = URLDecoder.decode(path, StandardCharsets.UTF_8)
        return if (decoded.startsWith("/")) decoded else "/$decoded"
    }

    private fun proxy(sourceUrl: String): ResponseEntity<ByteArray> {
        return try {
            val bytes = restTemplate.getForObject(sourceUrl, ByteArray::class.java)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
            ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .contentType(MediaType.IMAGE_JPEG)
                .body(bytes)
        } catch (_: Exception) {
            ResponseEntity.status(HttpStatus.BAD_GATEWAY).build()
        }
    }

    companion object {
        private val POSTER_WIDTHS = setOf("w92", "w154", "w185", "w342", "w500", "w780", "original")
        private val BACKDROP_WIDTHS = setOf("w300", "w780", "w1280", "original")
        private val PROFILE_WIDTHS = setOf("w45", "w185", "h632", "original")
    }
}
