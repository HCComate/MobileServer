package com.semse.mobile_server.config;

import com.google.gson.Gson;
import com.semse.mobile_server.dto.AlertEvent;
import com.semse.mobile_server.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AlertWebSocketHandler extends TextWebSocketHandler {

    // userId → WebSocketSession 매핑
    private final Map<String, WebSocketSession> userSessionMap = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    private AlertService alertService;

    @Autowired
    public void setAlertService(AlertService alertService) {
        this.alertService = alertService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 연결 시 URL 쿼리파라미터에서 userId 추출
        // 예: ws://서버IP:8080/ws/alerts?userId=tech_001
        String userId = extractUserId(session);
        if (userId != null) {
            userSessionMap.put(userId, session);
            System.out.println("WebSocket 연결됨 - userId: " + userId + ", sessionId: " + session.getId());
        } else {
            System.out.println("WebSocket 연결됨 (userId 없음): " + session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 세션 종료 시 매핑에서 제거
        userSessionMap.entrySet().removeIf(entry -> entry.getValue().getId().equals(session.getId()));
        System.out.println("WebSocket 연결 종료: " + session.getId());
    }

    // 특정 유저에게만 알림 전송
    public void sendAlert(AlertEvent event) {
        // DB 저장
        if (alertService != null) {
            alertService.saveAlert(event);
        }

        String targetUserId = event.getTargetUserId();
        String payload = gson.toJson(event);

        if (targetUserId != null && userSessionMap.containsKey(targetUserId)) {
            // 특정 유저에게만 전송
            WebSocketSession session = userSessionMap.get(targetUserId);
            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(payload));
                    System.out.println("알림 전송 성공 → userId: " + targetUserId);
                } catch (IOException e) {
                    System.out.println("알림 전송 실패 (userId: " + targetUserId + "): " + e.getMessage());
                }
            } else {
                System.out.println("대상 유저 오프라인 - userId: " + targetUserId);
            }
        } else {
            System.out.println("대상 유저 세션 없음 - userId: " + targetUserId);
        }
    }

    // 접속 중인 유저 목록 조회 (EscalationService에서 사용)
    public boolean isUserOnline(String userId) {
        WebSocketSession session = userSessionMap.get(userId);
        return session != null && session.isOpen();
    }

    // URL 쿼리스트링에서 userId 파싱
    private String extractUserId(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri == null) return null;
            String query = uri.getQuery(); // "userId=tech_001"
            if (query == null) return null;
            for (String param : query.split("&")) {
                String[] kv = param.split("=");
                if (kv.length == 2 && "userId".equals(kv[0])) {
                    return kv[1];
                }
            }
        } catch (Exception e) {
            System.out.println("userId 파싱 실패: " + e.getMessage());
        }
        return null;
    }
}
