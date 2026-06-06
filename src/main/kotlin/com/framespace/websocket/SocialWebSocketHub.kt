package com.framespace.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Component
class SocialWebSocketHub(
    private val objectMapper: ObjectMapper
) {
    private val sessionsByUser = ConcurrentHashMap<Long, MutableSet<WebSocketSession>>()

    fun subscribe(userId: Long, session: WebSocketSession) {
        sessionsByUser.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    fun unsubscribe(session: WebSocketSession) {
        sessionsByUser.values.forEach { it.remove(session) }
    }

    fun pushEvent(userId: Long, type: String, data: Any?) {
        val payload = objectMapper.writeValueAsString(mapOf("type" to type, "data" to data))
        sessionsByUser[userId]?.forEach { session ->
            if (session.isOpen) {
                runCatching { session.sendMessage(TextMessage(payload)) }
            }
        }
    }

    fun pushEvent(userIds: Iterable<Long>, type: String, data: Any?) {
        userIds.distinct().forEach { pushEvent(it, type, data) }
    }
}
