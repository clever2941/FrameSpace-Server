package com.framespace.controller

import com.framespace.common.Result
import com.framespace.dto.FriendRequestDto
import com.framespace.dto.FriendUserDto
import com.framespace.dto.FriendshipStatusDto
import com.framespace.service.FriendService
import com.framespace.utils.JwtAuthHelper
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/friends")
class FriendController(
    private val friendService: FriendService,
    private val jwtAuthHelper: JwtAuthHelper
) {

    @GetMapping
    fun listFriends(request: HttpServletRequest): Result<List<FriendUserDto>> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            Result.success(friendService.listFriends(userId))
        } catch (e: IllegalArgumentException) {
            Result.error(401, e.message ?: "未登录")
        }
    }

    @GetMapping("/requests/incoming")
    fun incomingRequests(request: HttpServletRequest): Result<List<FriendRequestDto>> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            Result.success(friendService.listIncomingRequests(userId))
        } catch (e: IllegalArgumentException) {
            Result.error(401, e.message ?: "未登录")
        }
    }

    @GetMapping("/search")
    fun searchUsers(
        @RequestParam q: String,
        request: HttpServletRequest
    ): Result<List<FriendUserDto>> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            Result.success(friendService.searchUsers(userId, q))
        } catch (e: IllegalArgumentException) {
            Result.error(401, e.message ?: "未登录")
        }
    }

    @GetMapping("/status/{targetUserId}")
    fun friendshipStatus(
        @PathVariable targetUserId: Long,
        request: HttpServletRequest
    ): Result<FriendshipStatusDto> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            Result.success(friendService.getFriendshipStatus(userId, targetUserId))
        } catch (e: IllegalArgumentException) {
            Result.error(401, e.message ?: "未登录")
        }
    }

    @PostMapping("/request/{targetUserId}")
    fun sendRequest(
        @PathVariable targetUserId: Long,
        request: HttpServletRequest
    ): Result<Unit> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            friendService.sendFriendRequest(userId, targetUserId)
            Result.success(Unit)
        } catch (e: IllegalArgumentException) {
            Result.error(400, e.message ?: "请求无效")
        }
    }

    @PostMapping("/requests/{requestId}/accept")
    fun acceptRequest(
        @PathVariable requestId: Long,
        request: HttpServletRequest
    ): Result<Unit> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            friendService.acceptRequest(userId, requestId)
            Result.success(Unit)
        } catch (e: IllegalArgumentException) {
            Result.error(400, e.message ?: "请求无效")
        }
    }

    @PostMapping("/requests/{requestId}/reject")
    fun rejectRequest(
        @PathVariable requestId: Long,
        request: HttpServletRequest
    ): Result<Unit> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            friendService.rejectRequest(userId, requestId)
            Result.success(Unit)
        } catch (e: IllegalArgumentException) {
            Result.error(400, e.message ?: "请求无效")
        }
    }

    @DeleteMapping("/{friendId}")
    fun removeFriend(
        @PathVariable friendId: Long,
        request: HttpServletRequest
    ): Result<Unit> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            friendService.removeFriend(userId, friendId)
            Result.success(Unit)
        } catch (e: IllegalArgumentException) {
            Result.error(400, e.message ?: "请求无效")
        }
    }
}
