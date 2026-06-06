package com.framespace.service

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Service
class TmdbService(
    private val restTemplate: RestTemplate,
    private val imageUrlService: ImageUrlService,
    @Value("\${tmdb.api.key}") private val apiKey: String,
    @Value("\${tmdb.api.base-url:https://api.themoviedb.org/3}") private val baseUrl: String
) {

    fun fetchNowPlaying(region: String, language: String): List<JsonNode> {
        val url = UriComponentsBuilder.fromHttpUrl("$baseUrl/movie/now_playing")
            .queryParam("api_key", apiKey)
            .queryParam("region", region)
            .queryParam("language", language)
            .queryParam("page", 1)
            .build()
            .toUriString()
        return fetchResults(url)
    }

    fun fetchTopRatedPage(page: Int): List<JsonNode> {
        val url = UriComponentsBuilder.fromHttpUrl("$baseUrl/movie/top_rated")
            .queryParam("api_key", apiKey)
            .queryParam("language", "zh-CN")
            .queryParam("page", page)
            .build()
            .toUriString()
        return fetchResults(url)
    }

    fun fetchMovieDetail(tmdbId: Int): JsonNode? {
        val url = UriComponentsBuilder.fromHttpUrl("$baseUrl/movie/$tmdbId")
            .queryParam("api_key", apiKey)
            .queryParam("language", "zh-CN")
            .queryParam("append_to_response", "credits,images,reviews")
            .build()
            .toUriString()
        return try {
            restTemplate.getForObject(url, JsonNode::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun searchMovies(query: String, page: Int): JsonNode? {
        if (query.isBlank()) return null
        val url = UriComponentsBuilder.fromHttpUrl("$baseUrl/search/movie")
            .queryParam("api_key", apiKey)
            .queryParam("language", "zh-CN")
            .queryParam("query", query.trim())
            .queryParam("page", page)
            .queryParam("include_adult", false)
            .build()
            .toUriString()
        return fetchJson(url)
    }

    fun fetchPersonDetail(personId: Int): JsonNode? {
        val url = UriComponentsBuilder.fromHttpUrl("$baseUrl/person/$personId")
            .queryParam("api_key", apiKey)
            .queryParam("language", "zh-CN")
            .build()
            .toUriString()
        return fetchJson(url)
    }

    fun fetchPersonCombinedCredits(personId: Int): JsonNode? {
        val url = UriComponentsBuilder.fromHttpUrl("$baseUrl/person/$personId/combined_credits")
            .queryParam("api_key", apiKey)
            .queryParam("language", "zh-CN")
            .build()
            .toUriString()
        return fetchJson(url)
    }

    fun buildPosterUrl(path: String?): String? = imageUrlService.buildPosterUrl(path)

    fun buildBackdropUrl(path: String?): String? = imageUrlService.buildBackdropUrl(path)

    private fun fetchJson(url: String): JsonNode? =
        try {
            restTemplate.getForObject(url, JsonNode::class.java)
        } catch (_: Exception) {
            null
        }

    private fun fetchResults(url: String): List<JsonNode> {
        val response = restTemplate.getForObject(url, JsonNode::class.java) ?: return emptyList()
        return response.path("results").map { it }
    }
}
