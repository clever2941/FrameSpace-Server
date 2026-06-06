package com.framespace.websocket

import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class CommentWebSocketHandler(
    private val commentWebSocketHub: CommentWebSocketHub
) : TextWebSocketHandler() {

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val movieId = extractMovieId(session) ?: return
        commentWebSocketHub.subscribe(movieId, session)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        commentWebSocketHub.unsubscribe(session)
    }

    private fun extractMovieId(session: WebSocketSession): Long? {
        val path = session.uri?.path ?: return null
        // /ws/movies/{movieId}/comments
        val parts = path.split("/").filter { it.isNotBlank() }
        val index = parts.indexOf("movies")
        if (index < 0 || index + 1 >= parts.size) return null
        return parts[index + 1].toLongOrNull()
    }
}
