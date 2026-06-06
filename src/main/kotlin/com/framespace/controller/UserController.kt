package com.framespace.controller

import com.framespace.common.Result
import com.framespace.dto.UpdateProfileRequest
import com.framespace.dto.UserProfileDto
import com.framespace.dto.UserSpaceDto
import com.framespace.dto.AvatarUploadDto
import com.framespace.dto.UnreadCountDto
import com.framespace.service.AvatarUploadService
import com.framespace.service.FriendService
import com.framespace.service.NotificationService
import com.framespace.service.UserProfileService
import com.framespace.utils.JwtAuthHelper
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api")
class UserController(
    private val userProfileService: UserProfileService,
    private val avatarUploadService: AvatarUploadService,
    private val notificationService: NotificationService,
    private val friendService: FriendService,
    private val jwtAuthHelper: JwtAuthHelper
) {

    @GetMapping("/user/profile")
    fun myProfile(request: HttpServletRequest): Result<UserProfileDto> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            Result.success(userProfileService.getMyProfile(userId))
        } catch (e: IllegalArgumentException) {
            Result.error(401, e.message ?: "未登录")
        }
    }

    @PostMapping("/user/avatar")
    fun uploadAvatar(
        @RequestParam("file") file: MultipartFile,
        request: HttpServletRequest
    ): Result<AvatarUploadDto> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            Result.success(avatarUploadService.uploadAvatar(userId, file))
        } catch (e: IllegalArgumentException) {
            Result.error(400, e.message ?: "上传失败")
        }
    }

    @PutMapping("/user/profile")
    fun updateProfile(
        @RequestBody body: UpdateProfileRequest,
        request: HttpServletRequest
    ): Result<UserProfileDto> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            Result.success(userProfileService.updateProfile(userId, body))
        } catch (e: IllegalArgumentException) {
            Result.error(400, e.message ?: "请求无效")
        }
    }

    @GetMapping("/users/{userId}/space")
    fun userSpace(
        @PathVariable userId: Long,
        request: HttpServletRequest
    ): Result<UserSpaceDto> {
        return try {
            val viewerId = jwtAuthHelper.resolveUserId(request)
            val space = userProfileService.getUserSpace(userId, viewerId)
            val enriched = if (viewerId != null && viewerId != userId) {
                val status = friendService.getFriendshipStatus(viewerId, userId)
                space.copy(
                    friendshipStatus = status.status,
                    friendRequestId = status.requestId
                )
            } else {
                space
            }
            Result.success(enriched)
        } catch (e: IllegalArgumentException) {
            Result.error(404, e.message ?: "用户不存在")
        }
    }

    @GetMapping("/user/notifications")
    fun notifications(request: HttpServletRequest): Result<List<com.framespace.dto.NotificationDto>> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            Result.success(notificationService.listNotifications(userId))
        } catch (e: IllegalArgumentException) {
            Result.error(401, e.message ?: "未登录")
        }
    }

    @GetMapping("/user/notifications/unread-count")
    fun unreadNotificationCount(request: HttpServletRequest): Result<UnreadCountDto> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            Result.success(UnreadCountDto(notificationService.unreadCount(userId)))
        } catch (e: IllegalArgumentException) {
            Result.error(401, e.message ?: "未登录")
        }
    }

    @PostMapping("/user/notifications/{id}/read")
    fun markNotificationRead(
        @PathVariable id: Long,
        request: HttpServletRequest
    ): Result<Unit> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            notificationService.markRead(userId, id)
            Result.success(Unit)
        } catch (e: IllegalArgumentException) {
            Result.error(401, e.message ?: "未登录")
        }
    }

    @PostMapping("/user/notifications/read-all")
    fun markAllNotificationsRead(request: HttpServletRequest): Result<Unit> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            notificationService.markAllRead(userId)
            Result.success(Unit)
        } catch (e: IllegalArgumentException) {
            Result.error(401, e.message ?: "未登录")
        }
    }
}
