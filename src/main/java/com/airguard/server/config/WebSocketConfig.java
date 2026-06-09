package com.airguard.server.config;

import com.airguard.server.websocket.RadarWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RadarWebSocketHandler radarWebSocketHandler;

    public WebSocketConfig(RadarWebSocketHandler radarWebSocketHandler) {
        this.radarWebSocketHandler = radarWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(radarWebSocketHandler, "/ws/radar")
                .setAllowedOrigins("*");
    }
}