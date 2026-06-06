package com.framespace.service

import com.fasterxml.jackson.databind.JsonNode
import com.framespace.dto.MovieDetailDto
import com.framespace.dto.PersonDto
import com.framespace.dto.ReviewDto
import org.springframework.stereotype.Service

@Service
class MovieDetailService(
    private val movieService: com.framespace.service.MovieService,
    private val tmdbService: TmdbService,
    private val imageUrlService: ImageUrlService,
    private val commentService: CommentService,
    private val socialService: SocialService
) {

    fun getDetail(movieId: Long, userId: Long?): MovieDetailDto {
        val movie = movieService.getMovieById(movieId)
            ?: throw IllegalArgumentException("电影不存在")
        userId?.let { socialService.recordBrowse(it, movieId) }

        val tmdbId = movie.tmdbId
        val tmdb = tmdbId?.let { tmdbService.fetchMovieDetail(it) }

        return MovieDetailDto(
            movie = movie,
            stills = parseStills(tmdb),
            directors = parseDirectors(tmdb),
            cast = parseCast(tmdb),
            reviews = parseReviews(tmdb),
            comments = commentService.listComments(movieId, userId),
            movieLikeCount = socialService.movieLikeCount(movieId),
            likedByMe = socialService.isMovieLiked(userId, movieId),
            favoritedByMe = socialService.isFavorited(userId, movieId)
        )
    }

    private fun parseStills(node: JsonNode?): List<String> {
        if (node == null) return emptyList()
        val backdrops = node.path("images").path("backdrops").take(8)
            .mapNotNull { imageUrlService.buildStillUrl(it.path("file_path").asText(null)) }
        val stills = node.path("images").path("stills").take(8)
            .mapNotNull { imageUrlService.buildStillUrl(it.path("file_path").asText(null)) }
        return (stills + backdrops).distinct().take(12)
    }

    private fun parseDirectors(node: JsonNode?): List<PersonDto> {
        if (node == null) return emptyList()
        return node.path("credits").path("crew")
            .filter { it.path("job").asText() == "Director" }
            .take(3)
            .map {
                PersonDto(
                    id = it.path("id").asInt(),
                    name = it.path("name").asText(),
                    role = "导演",
                    photoUrl = imageUrlService.buildProfileUrl(it.path("profile_path").asText(null))
                )
            }
    }

    private fun parseCast(node: JsonNode?): List<PersonDto> {
        if (node == null) return emptyList()
        return node.path("credits").path("cast")
            .take(12)
            .map {
                PersonDto(
                    id = it.path("id").asInt(),
                    name = it.path("name").asText(),
                    role = it.path("character").asText("演员"),
                    photoUrl = imageUrlService.buildProfileUrl(it.path("profile_path").asText(null))
                )
            }
    }

    private fun parseReviews(node: JsonNode?): List<ReviewDto> {
        if (node == null) return emptyList()
        return node.path("reviews").path("results")
            .filter { it.path("content").asText().length >= 20 }
            .sortedByDescending { it.path("author_details").path("rating").asDouble(0.0) }
            .take(8)
            .map {
                ReviewDto(
                    author = it.path("author").asText(),
                    content = it.path("content").asText(),
                    rating = it.path("author_details").path("rating").takeIf { r -> !r.isNull }?.asDouble(),
                    source = "TMDB 精选"
                )
            }
    }
}
