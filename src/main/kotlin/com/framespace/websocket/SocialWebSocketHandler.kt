package com.framespace.websocket

import com.framespace.utils.JwtUtils
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class SocialWebSocketHandler(
    private val socialWebSocketHub: SocialWebSocketHub,
    private val jwtUtils: JwtUtils
) : TextWebSocketHandler() {

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val userId = resolveUserId(session)
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE)
            return
        }
        session.attributes["userId"] = userId
        socialWebSocketHub.subscribe(userId, session)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        socialWebSocketHub.unsubscribe(session)
    }

    private fun resolveUserId(session: WebSocketSession): Long? {
        val headerToken = session.handshakeHeaders.getFirst("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        val queryToken = session.uri?.query
            ?.split("&")
            ?.mapNotNull { part ->
                val kv = part.split("=", limit = 2)
                if (kv.size == 2 && kv[0] == "token") kv[1] else null
            }
            ?.firstOrNull()
        val token = headerToken ?: queryToken ?: return null
        val claims = jwtUtils.parseToken(token) ?: return null
        val userId = claims["userId"]
        return when (userId) {
            is Int -> userId.toLong()
            is Long -> userId
            is Number -> userId.toLong()
            else -> null
        }
    }
}
