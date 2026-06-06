package com.framespace.service

import com.fasterxml.jackson.databind.JsonNode
import com.framespace.dto.PersonFilmographyDto
import com.framespace.dto.PersonMovieDto
import com.framespace.mapper.MovieMapper
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class PersonService(
    private val tmdbService: TmdbService,
    private val imageUrlService: ImageUrlService,
    private val movieMapper: MovieMapper
) {

    private data class PersonMeta(
        val personId: Int,
        val personName: String,
        val photoUrl: String?
    )

    private val personMetaCache = ConcurrentHashMap<Int, PersonMeta>()
    private val filmographyCache = ConcurrentHashMap<String, List<PersonMovieDto>>()

    fun getFilmography(
        personId: Int,
        kind: String,
        sort: String,
        page: Long,
        size: Long
    ): PersonFilmographyDto {
        val normalizedKind = kind.lowercase().let { if (it == "director") "director" else "cast" }
        val normalizedSort = sort.lowercase().let { if (it == "release") "release" else "rating" }
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 50)

        val meta = personMetaCache.getOrPut(personId) {
            val person = tmdbService.fetchPersonDetail(personId)
                ?: throw IllegalArgumentException("演职人员不存在")
            PersonMeta(
                personId = personId,
                personName = person.path("name").asText(),
                photoUrl = imageUrlService.buildProfileUrl(person.path("profile_path").asText(null))
            )
        }

        val cacheKey = "$personId:$normalizedKind:$normalizedSort"
        val allMovies = filmographyCache.getOrPut(cacheKey) {
            buildMovieList(personId, normalizedKind, normalizedSort)
        }

        val fromIndex = (safePage * safeSize).toInt()
        val pageMovies = allMovies.drop(fromIndex).take(safeSize.toInt())

        return PersonFilmographyDto(
            personId = meta.personId,
            personName = meta.personName,
            photoUrl = meta.photoUrl,
            kind = normalizedKind,
            sort = normalizedSort,
            movies = pageMovies,
            total = allMovies.size.toLong(),
            page = safePage,
            size = safeSize
        )
    }

    private fun buildMovieList(personId: Int, kind: String, sort: String): List<PersonMovieDto> {
        val credits = tmdbService.fetchPersonCombinedCredits(personId)
            ?: throw IllegalArgumentException("无法获取作品列表")

        val rawCredits = when (kind) {
            "director" -> credits.path("crew")
                .filter {
                    it.path("job").asText() == "Director" &&
                        it.path("media_type").asText() == "movie"
                }
            else -> credits.path("cast")
                .filter { it.path("media_type").asText() == "movie" }
        }

        return rawCredits
            .groupBy { it.path("id").asInt() }
            .values
            .map { group -> toPersonMovie(group.maxByOrNull { it.path("vote_average").asDouble(0.0) }!!, kind) }
            .let { list -> sortMovies(list, sort) }
    }

    private fun toPersonMovie(node: JsonNode, kind: String): PersonMovieDto {
        val tmdbId = node.path("id").asInt()
        val creditRole = when (kind) {
            "director" -> "导演"
            else -> node.path("character").asText("演员")
        }
        return PersonMovieDto(
            tmdbId = tmdbId,
            localId = movieMapper.selectByTmdbId(tmdbId)?.id,
            title = node.path("title").asText(),
            originalTitle = node.path("original_title").asText(null),
            posterUrl = tmdbService.buildPosterUrl(node.path("poster_path").asText(null)),
            rating = node.path("vote_average").asDouble(0.0),
            releaseYear = node.path("release_date").asText("").take(4).ifBlank { null },
            creditRole = creditRole
        )
    }

    private fun sortMovies(movies: List<PersonMovieDto>, sort: String): List<PersonMovieDto> =
        when (sort) {
            "release" -> movies.sortedByDescending { it.releaseYear.orEmpty() }
            else -> movies.sortedByDescending { it.rating }
        }
}
