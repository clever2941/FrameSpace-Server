package com.framespace.controller

import com.framespace.common.Result
import com.framespace.dto.*
import com.framespace.service.PrivateMessageService
import com.framespace.utils.JwtAuthHelper
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/messages")
class PrivateMessageController(
    private val privateMessageService: PrivateMessageService,
    private val jwtAuthHelper: JwtAuthHelper
) {

    @GetMapping("/conversations")
    fun conversations(request: HttpServletRequest): Result<List<ConversationDto>> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            Result.success(privateMessageService.listConversations(userId))
        } catch (e: IllegalArgumentException) {
            Result.error(401, e.message ?: "未登录")
        }
    }

    @GetMapping("/with/{peerId}")
    fun messagesWith(
        @PathVariable peerId: Long,
        request: HttpServletRequest
    ): Result<List<PrivateMessageDto>> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            Result.success(privateMessageService.getMessages(userId, peerId))
        } catch (e: IllegalArgumentException) {
            Result.error(400, e.message ?: "请求无效")
        }
    }

    @GetMapping("/unread-count")
    fun unreadCount(request: HttpServletRequest): Result<DmUnreadCountDto> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            Result.success(DmUnreadCountDto(privateMessageService.unreadCount(userId)))
        } catch (e: IllegalArgumentException) {
            Result.error(401, e.message ?: "未登录")
        }
    }

    @PostMapping("/send")
    fun sendMessage(
        @RequestBody body: SendMessageRequest,
        request: HttpServletRequest
    ): Result<PrivateMessageDto> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            Result.success(privateMessageService.sendTextMessage(userId, body.receiverId, body.content))
        } catch (e: IllegalArgumentException) {
            Result.error(400, e.message ?: "请求无效")
        }
    }

    @PostMapping("/with/{peerId}/read")
    fun markRead(
        @PathVariable peerId: Long,
        request: HttpServletRequest
    ): Result<Unit> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            privateMessageService.markConversationRead(userId, peerId)
            Result.success(Unit)
        } catch (e: IllegalArgumentException) {
            Result.error(401, e.message ?: "未登录")
        }
    }

    @PostMapping("/movies/{movieId}/push")
    fun pushMovie(
        @PathVariable movieId: Long,
        @RequestBody body: PushMovieRequest,
        request: HttpServletRequest
    ): Result<PushMovieResultDto> {
        return try {
            val userId = jwtAuthHelper.requireUserId(request)
            Result.success(
                privateMessageService.pushMovie(userId, movieId, body.friendIds, body.message)
            )
        } catch (e: IllegalArgumentException) {
            Result.error(400, e.message ?: "请求无效")
        }
    }
}
