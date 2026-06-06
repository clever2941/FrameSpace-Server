package com.framespace.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.framespace.dto.CommentDto
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Component
class CommentWebSocketHub(
    private val objectMapper: ObjectMapper
) {
    private val sessionsByMovie = ConcurrentHashMap<Long, MutableSet<WebSocketSession>>()

    fun subscribe(movieId: Long, session: WebSocketSession) {
        sessionsByMovie.computeIfAbsent(movieId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    fun unsubscribe(session: WebSocketSession) {
        sessionsByMovie.values.forEach { it.remove(session) }
    }

    fun broadcast(movieId: Long, comment: CommentDto) {
        val payload = objectMapper.writeValueAsString(
            mapOf("type" to "NEW_COMMENT", "data" to comment)
        )
        sessionsByMovie[movieId]?.forEach { session ->
            if (session.isOpen) {
                runCatching { session.sendMessage(TextMessage(payload)) }
            }
        }
    }
}
