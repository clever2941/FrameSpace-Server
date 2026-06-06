package com.framespace.service

import com.fasterxml.jackson.databind.JsonNode
import com.framespace.dto.MovieDetailDto
import com.framespace.dto.MovieExtrasDto
import com.framespace.dto.PersonDto
import com.framespace.dto.ReviewDto
import com.framespace.dto.ReviewPageDto
import com.framespace.entity.Movie
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class MovieDetailService(
    private val movieService: com.framespace.service.MovieService,
    private val tmdbService: TmdbService,
    private val imageUrlService: ImageUrlService,
    private val socialService: SocialService
) {

    fun getDetail(movieId: Long, userId: Long?): MovieDetailDto {
        val movie = imageUrlService.rewriteMovie(
            movieService.getMovieById(movieId) ?: throw IllegalArgumentException("电影不存在")
        )
        userId?.let { uid ->
            CompletableFuture.runAsync { socialService.recordBrowse(uid, movieId) }
        }

        val likeCountFuture = CompletableFuture.supplyAsync { socialService.movieLikeCount(movieId) }
        val likedFuture = CompletableFuture.supplyAsync { socialService.isMovieLiked(userId, movieId) }
        val favoritedFuture = CompletableFuture.supplyAsync { socialService.isFavorited(userId, movieId) }

        return MovieDetailDto(
            movie = movie,
            stills = buildDbStills(movie),
            directors = parseDirectorsFromDb(movie),
            cast = parseCastFromDb(movie),
            reviews = emptyList(),
            reviewsHasMore = movie.tmdbId != null,
            extrasAvailable = movie.tmdbId != null,
            comments = emptyList(),
            movieLikeCount = likeCountFuture.join(),
            likedByMe = likedFuture.join(),
            favoritedByMe = favoritedFuture.join()
        )
    }

    fun getExtras(movieId: Long): MovieExtrasDto {
        val movie = movieService.getMovieById(movieId)
            ?: throw IllegalArgumentException("电影不存在")
        val tmdbId = movie.tmdbId
            ?: return MovieExtrasDto(buildDbStills(imageUrlService.rewriteMovie(movie)), emptyList(), emptyList())

        val creditsFuture = CompletableFuture.supplyAsync { tmdbService.fetchMovieCredits(tmdbId) }
        val imagesFuture = CompletableFuture.supplyAsync { tmdbService.fetchMovieImages(tmdbId) }
        val credits = creditsFuture.join()
        val images = imagesFuture.join()

        val stills = parseStillsFromImages(images).ifEmpty { buildDbStills(imageUrlService.rewriteMovie(movie)) }
        return MovieExtrasDto(
            stills = stills,
            directors = parseDirectors(credits),
            cast = parseCast(credits)
        )
    }

    fun listReviews(movieId: Long, cursor: String?, size: Int = REVIEW_BATCH_SIZE): ReviewPageDto {
        val movie = movieService.getMovieById(movieId)
            ?: throw IllegalArgumentException("电影不存在")
        val tmdbId = movie.tmdbId
            ?: return ReviewPageDto(emptyList(), null, false)

        val batchSize = size.coerceIn(1, 10)
        var position = ReviewCursor.decode(cursor) ?: ReviewCursor.start()
        val collected = mutableListOf<ReviewDto>()
        var exhausted = false
        var guard = 0

        while (collected.size < batchSize && !exhausted && guard++ < 40) {
            val node = tmdbService.fetchMovieReviews(tmdbId, position.tmdbPage, position.language)
            if (node == null) {
                position = advanceLanguage(position) ?: break.also { exhausted = true }
                continue
            }

            val totalPages = node.path("total_pages").asInt(1).coerceAtLeast(1)
            val results = node.path("results")
            if (results.isEmpty()) {
                position = advancePage(position, totalPages) ?: break.also { exhausted = true }
                continue
            }

            while (position.resultIndex < results.size() && collected.size < batchSize) {
                parseReviewItem(results[position.resultIndex])?.let { collected.add(it) }
                position = position.copy(resultIndex = position.resultIndex + 1)
            }

            if (position.resultIndex >= results.size()) {
                position = advancePage(position, totalPages) ?: break.also { exhausted = true }
            }
        }

        val hasMore = !exhausted
        return ReviewPageDto(
            records = collected,
            nextCursor = if (hasMore) position.encode() else null,
            hasMore = hasMore
        )
    }

    private fun buildDbStills(movie: Movie): List<String> =
        listOfNotNull(movie.backdropUrl, movie.posterUrl)
            .mapNotNull { it.trim().takeIf { url -> url.isNotBlank() } }
            .distinct()

    private fun parseDirectorsFromDb(movie: Movie): List<PersonDto> {
        val names = movie.director
            ?.split(",", "，", "、")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        return names.take(3).mapIndexed { index, name ->
            PersonDto(id = -(index + 1), name = name, role = "导演", photoUrl = null)
        }
    }

    private fun parseCastFromDb(movie: Movie): List<PersonDto> {
        val names = movie.castList
            ?.split(",", "，", "、")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        return names.take(8).mapIndexed { index, name ->
            PersonDto(id = -(index + 101), name = name, role = "演员", photoUrl = null)
        }
    }

    private fun parseStillsFromImages(node: JsonNode?): List<String> {
        if (node == null) return emptyList()
        val backdrops = node.path("backdrops").take(8)
            .mapNotNull { imageUrlService.buildBackdropUrl(it.path("file_path").asText(null)) }
        val stills = node.path("stills").take(8)
            .mapNotNull { imageUrlService.buildStillUrl(it.path("file_path").asText(null)) }
        return (stills + backdrops).distinct().take(12)
    }

    private fun advancePage(position: ReviewCursor, totalPages: Int): ReviewCursor? {
        val nextPage = position.tmdbPage + 1
        if (nextPage <= totalPages) {
            return position.copy(tmdbPage = nextPage, resultIndex = 0)
        }
        return advanceLanguage(position)
    }

    private fun advanceLanguage(position: ReviewCursor): ReviewCursor? {
        if (position.language == "zh-CN") {
            return ReviewCursor("en-US", 1, 0)
        }
        return null
    }

    private fun parseReviewItem(item: JsonNode): ReviewDto? {
        val content = item.path("content").asText("").trim()
        if (content.length < 10) return null
        val id = item.path("id").asText("").ifBlank { content.hashCode().toString() }
        return ReviewDto(
            id = id,
            author = resolveAuthorName(item),
            content = content,
            rating = item.path("author_details").path("rating")
                .takeIf { rating -> rating.isNumber }
                ?.asDouble(),
            source = "TMDB 精选",
            createdAt = item.path("created_at").asText(null)
        )
    }

    private fun resolveAuthorName(item: JsonNode): String {
        val details = item.path("author_details")
        val displayName = details.path("name").asText("").trim()
        if (displayName.isNotBlank()) return displayName

        val username = details.path("username").asText("").trim()
            .ifBlank { item.path("author").asText("").trim() }
        if (username.isBlank()) return "影迷观众"
        if (AUTO_USERNAME_REGEX.matches(username)) return "影迷观众"
        return username
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
            .take(8)
            .map {
                PersonDto(
                    id = it.path("id").asInt(),
                    name = it.path("name").asText(),
                    role = it.path("character").asText("演员"),
                    photoUrl = imageUrlService.buildProfileUrl(it.path("profile_path").asText(null))
                )
            }
    }

    private data class ReviewCursor(
        val language: String,
        val tmdbPage: Int,
        val resultIndex: Int
    ) {
        fun encode(): String = "$language|$tmdbPage|$resultIndex"

        companion object {
            fun start(): ReviewCursor = ReviewCursor("zh-CN", 1, 0)

            fun decode(raw: String?): ReviewCursor? {
                if (raw.isNullOrBlank()) return null
                val parts = raw.split("|")
                if (parts.size != 3) return null
                val page = parts[1].toIntOrNull() ?: return null
                val index = parts[2].toIntOrNull() ?: return null
                return ReviewCursor(parts[0], page, index)
            }
        }
    }

    companion object {
        const val REVIEW_BATCH_SIZE = 3
        private val AUTO_USERNAME_REGEX = Regex("""^(label|user|member|tmdb)\d*$""", RegexOption.IGNORE_CASE)
    }
}
