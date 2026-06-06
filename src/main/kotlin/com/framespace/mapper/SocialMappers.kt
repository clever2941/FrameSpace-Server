package com.framespace.mapper

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.framespace.entity.BrowseHistory
import com.framespace.entity.CommentLike
import com.framespace.entity.MovieFavorite
import com.framespace.entity.MovieLike
import com.framespace.entity.UserComment
import com.framespace.entity.UserNotification
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select

@Mapper
interface UserCommentMapper : BaseMapper<UserComment> {

    @Select(
        """
        SELECT * FROM user_comment
        WHERE movie_id = #{movieId}
        ORDER BY created_at DESC
        LIMIT #{limit}
        """
    )
    fun selectByMovieId(@Param("movieId") movieId: Long, @Param("limit") limit: Int): List<UserComment>

    @Select(
        """
        SELECT * FROM user_comment
        WHERE movie_id = #{movieId}
        ORDER BY created_at ASC
        LIMIT #{limit}
        """
    )
    fun selectAllByMovieId(@Param("movieId") movieId: Long, @Param("limit") limit: Int): List<UserComment>

    @Select(
        """
        SELECT * FROM user_comment
        WHERE user_id = #{userId}
        ORDER BY created_at DESC
        LIMIT #{limit}
        """
    )
    fun selectByUserId(@Param("userId") userId: Long, @Param("limit") limit: Int): List<UserComment>
}

@Mapper
interface CommentLikeMapper : BaseMapper<CommentLike>

@Mapper
interface MovieFavoriteMapper : BaseMapper<MovieFavorite> {

    @Select(
        """
        SELECT movie_id FROM movie_favorite
        WHERE user_id = #{userId}
        ORDER BY created_at DESC
        LIMIT #{limit}
        """
    )
    fun selectMovieIdsByUser(@Param("userId") userId: Long, @Param("limit") limit: Int): List<Long>
}

@Mapper
interface BrowseHistoryMapper : BaseMapper<BrowseHistory> {

    @Select(
        """
        SELECT movie_id FROM browse_history
        WHERE user_id = #{userId}
        ORDER BY browsed_at DESC
        LIMIT #{limit}
        """
    )
    fun selectMovieIdsByUser(@Param("userId") userId: Long, @Param("limit") limit: Int): List<Long>
}

@Mapper
interface MovieLikeMapper : BaseMapper<MovieLike> {

    @Select("SELECT COUNT(*) FROM movie_like WHERE movie_id = #{movieId}")
    fun countByMovieId(@Param("movieId") movieId: Long): Long
}

@Mapper
interface UserNotificationMapper : BaseMapper<UserNotification> {

    @Select(
        """
        SELECT * FROM user_notification
        WHERE user_id = #{userId}
        ORDER BY created_at DESC
        LIMIT #{limit}
        """
    )
    fun selectByUserId(@Param("userId") userId: Long, @Param("limit") limit: Int): List<UserNotification>

    @Select("SELECT COUNT(*) FROM user_notification WHERE user_id = #{userId} AND read_at IS NULL")
    fun countUnread(@Param("userId") userId: Long): Long
}
