package com.framespace.dto

import com.framespace.entity.Movie

data class MoviePageDto(
    val records: List<Movie>,
    val total: Long,
    val page: Long,
    val size: Long
)
