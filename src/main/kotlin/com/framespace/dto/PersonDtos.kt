package com.framespace.dto

data class PersonFilmographyDto(
    val personId: Int,
    val personName: String,
    val photoUrl: String?,
    val kind: String,
    val sort: String,
    val movies: List<PersonMovieDto>,
    val total: Long = 0,
    val page: Long = 0,
    val size: Long = 15
)

data class PersonMovieDto(
    val tmdbId: Int,
    val localId: Long?,
    val title: String,
    val originalTitle: String?,
    val posterUrl: String?,
    val rating: Double,
    val releaseYear: String?,
    val creditRole: String
)

data class EnsureMovieDto(
    val id: Long,
    val tmdbId: Int
)
