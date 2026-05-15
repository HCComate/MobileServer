package com.semse.mobile_server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AlertWebSocketHandler alertWebSocketHandler;
    private final RawLogWebSocketHandler rawLogWebSocketHandler;

    public WebSocketConfig(AlertWebSocketHandler alertWebSocketHandler,
                           RawLogWebSocketHandler rawLogWebSocketHandler) {
        this.alertWebSocketHandler = alertWebSocketHandler;
        this.rawLogWebSocketHandler = rawLogWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(alertWebSocketHandler, "/ws/alerts")
                .setAllowedOrigins("*");
        registry.addHandler(rawLogWebSocketHandler, "/logs")
                .setAllowedOrigins("*");
    }
}