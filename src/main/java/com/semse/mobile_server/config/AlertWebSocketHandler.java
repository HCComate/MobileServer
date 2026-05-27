package com.semse.mobile_server.config;

import com.google.gson.Gson;
import com.semse.mobile_server.dto.AlertEvent;
import com.semse.mobile_server.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AlertWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Gson gson = new Gson();
    private final AlertService alertService;

    // @Lazy를 사용하여 순환 참조 연결 고리를 끊음
    public AlertWebSocketHandler(@Lazy AlertService alertService) {
        this.alertService = alertService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        System.out.println("WebSocket 연결됨: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        System.out.println("WebSocket 연결 종료: " + session.getId());
    }

    public void sendAlert(AlertEvent event) {
        if (alertService != null) {
            alertService.saveAlert(event);
        }
        String payload = gson.toJson(event);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(payload));
                } catch (IOException e) {
                    System.out.println("알림 전송 실패: " + e.getMessage());
                }
            }
        }
    }
}
