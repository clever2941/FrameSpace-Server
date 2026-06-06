package com.framespace.mapper

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.framespace.entity.Movie
import org.apache.ibatis.annotations.Delete
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import java.time.LocalDateTime

@Mapper
interface MovieMapper : BaseMapper<Movie> {

    @Select(
        """
        SELECT synced_at FROM movie
        WHERE category = #{category}
        ORDER BY synced_at DESC LIMIT 1
        """
    )
    fun selectLatestSyncedAt(@Param("category") category: String): LocalDateTime?

    @Delete("DELETE FROM movie WHERE category = #{category}")
    fun deleteByCategory(@Param("category") category: String): Int

    @Select(
        """
        <script>
        SELECT * FROM movie
        WHERE category = #{category}
        <if test="region != null and region != '' and region != 'ALL'">
            AND region = #{region}
        </if>
        <if test="genreId != null">
            AND FIND_IN_SET(#{genreId}, genre_ids)
        </if>
        <if test="year != null and year != ''">
            AND release_year = #{year}
        </if>
        <choose>
            <when test="category == 'TOP500'">
                ORDER BY rank_num ASC
            </when>
            <otherwise>
                ORDER BY rating DESC, id DESC
            </otherwise>
        </choose>
        </script>
        """
    )
    fun selectByFilters(
        page: Page<Movie>,
        @Param("category") category: String,
        @Param("region") region: String?,
        @Param("genreId") genreId: Int?,
        @Param("year") year: String?
    ): Page<Movie>

    @Select("SELECT * FROM movie WHERE tmdb_id = #{tmdbId} ORDER BY id ASC LIMIT 1")
    fun selectByTmdbId(@Param("tmdbId") tmdbId: Int): Movie?

    @Select("SELECT * FROM movie WHERE tmdb_id = #{tmdbId} AND category = #{category} LIMIT 1")
    fun selectByTmdbIdAndCategory(
        @Param("tmdbId") tmdbId: Int,
        @Param("category") category: String
    ): Movie?

    @Select("SELECT * FROM movie WHERE category = #{category}")
    fun selectAllByCategory(@Param("category") category: String): List<Movie>

    @Select("SELECT COUNT(*) FROM movie WHERE category = #{category}")
    fun countByCategory(@Param("category") category: String): Long
}
