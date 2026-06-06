package com.framespace.config

import com.framespace.websocket.CommentWebSocketHandler
import com.framespace.websocket.SocialWebSocketHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val commentWebSocketHandler: CommentWebSocketHandler,
    private val socialWebSocketHandler: SocialWebSocketHandler
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(commentWebSocketHandler, "/ws/movies/{movieId}/comments")
            .setAllowedOrigins("*")
        registry.addHandler(socialWebSocketHandler, "/ws/social")
            .setAllowedOrigins("*")
    }
}
