package com.framespace.service

import com.framespace.entity.Movie
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * 将 TMDB 图片地址转为本服务器代理地址，便于国内用户无需 VPN 加载海报。
 */
@Service
class ImageUrlService(
    @Value("\${app.public-base-url:http://localhost:8080}") private val publicBaseUrl: String
) {

    fun buildPosterUrl(tmdbPath: String?): String? =
        buildProxyUrl("poster", tmdbPath)

    fun buildBackdropUrl(tmdbPath: String?): String? =
        buildProxyUrl("backdrop", tmdbPath)

    fun buildProfileUrl(tmdbPath: String?): String? =
        buildProxyUrl("profile", tmdbPath)

    fun buildStillUrl(tmdbPath: String?): String? =
        buildProxyUrl("still", tmdbPath)

    fun buildAvatarUrl(storedPath: String?): String? {
        val path = storedPath?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val encoded = URLEncoder.encode(path, StandardCharsets.UTF_8)
        val base = publicBaseUrl.trimEnd('/')
        return "$base/api/images/avatar?file=$encoded"
    }

    fun rewriteStoredUrl(url: String?): String? {
        if (url.isNullOrBlank()) return url
        if (url.contains("/api/images/")) return url
        val path = extractTmdbPath(url) ?: return url
        val type = if (url.contains("/w780") || url.contains("backdrop")) "backdrop" else "poster"
        return buildProxyUrl(type, path)
    }

    fun rewriteMovie(movie: Movie): Movie = movie.copy(
        posterUrl = rewriteStoredUrl(movie.posterUrl),
        backdropUrl = rewriteStoredUrl(movie.backdropUrl)
    )

    fun rewriteMovies(movies: List<Movie>): List<Movie> = movies.map { rewriteMovie(it) }

    private fun buildProxyUrl(type: String, tmdbPath: String?): String? {
        val path = tmdbPath?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val normalized = if (path.startsWith("/")) path else "/$path"
        val encoded = URLEncoder.encode(normalized, StandardCharsets.UTF_8)
        val base = publicBaseUrl.trimEnd('/')
        return "$base/api/images/$type?path=$encoded"
    }

    private fun extractTmdbPath(url: String): String? {
        val marker = "/t/p/"
        if (!url.contains(marker)) return null
        val after = url.substringAfter(marker)
        val slash = after.indexOf('/')
        if (slash < 0 || slash >= after.length - 1) return null
        return after.substring(slash)
    }
}
