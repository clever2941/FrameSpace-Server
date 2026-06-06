package com.framespace.common

import com.framespace.dto.GenreDto

object GenreCatalog {
    val all: List<GenreDto> = listOf(
        GenreDto(28, "动作"),
        GenreDto(12, "冒险"),
        GenreDto(16, "动画"),
        GenreDto(35, "喜剧"),
        GenreDto(80, "犯罪"),
        GenreDto(99, "纪录"),
        GenreDto(18, "剧情"),
        GenreDto(10751, "家庭"),
        GenreDto(14, "奇幻"),
        GenreDto(36, "历史"),
        GenreDto(27, "恐怖"),
        GenreDto(10402, "音乐"),
        GenreDto(9648, "悬疑"),
        GenreDto(10749, "爱情"),
        GenreDto(878, "科幻"),
        GenreDto(10770, "电视电影"),
        GenreDto(53, "惊悚"),
        GenreDto(10752, "战争"),
        GenreDto(37, "西部")
    )

    fun namesByIds(ids: List<Int>): String =
        ids.mapNotNull { id -> all.find { it.id == id }?.name }
            .joinToString(",")
}
