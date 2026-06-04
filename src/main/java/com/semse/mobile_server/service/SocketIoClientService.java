package com.semse.mobile_server.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.semse.mobile_server.config.AlertWebSocketHandler;
import com.semse.mobile_server.config.RawLogWebSocketHandler;
import com.semse.mobile_server.dto.AlertEvent;
import io.socket.client.IO;
import io.socket.client.Socket;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import com.semse.mobile_server.config.PresenceFilter;

/**
 * AdminPC-Server 의 Socket.IO 이벤트를 리스닝하는 클라이언트 서비스입니다.
 *
 * <p>구독하는 이벤트 목록:</p>
 * <ul>
 *   <li>{@code mobile_data_feed}     - 장비 실시간 검사 데이터 (MobileApp 으로 포워딩)</li>
 *   <li>{@code critical_alert}       - CRITICAL 오류 발생 알림 (LOCKED 상태 전환)</li>
 *   <li>{@code device_status_changed} - 장비 상태 변경 알림 (STANDBY → IDLE 변환 포함)</li>
 *   <li>{@code error_resolved}       - LOCKED 해제 알림 (IDLE 로 전환)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SocketIoClientService {

    private final RawLogWebSocketHandler rawLogWebSocketHandler;
    private final AlertWebSocketHandler alertWebSocketHandler;
    private final CriticalAlertService criticalAlertService;

    @Value("${admin.pc.base-url}")
    private String adminBaseUrl;

    private Socket socket;

    @PostConstruct
    public void connect() {
        try {
            IO.Options options = IO.Options.builder()
                    .setTransports(new String[]{"websocket"})
                    .build();

            socket = IO.socket(URI.create(adminBaseUrl), options);

            // ── mobile_data_feed: 장비 실시간 검사 데이터 포워딩 ────────────────────────
            // AdminPC-Server 가 직접 에미트하는 header/body 구조 JSON 을 MobileApp 으로 그대로 중계합니다.
            // (CRITICAL ERROR 시에는 critical_alert 이벤트가 별도로 발생하므로 여기서는 데이터만 중계)
            socket.on("mobile_data_feed", args -> {
                try {
                    String payload = args[0].toString();
                    System.out.println("mobile_data_feed 수신");
                    rawLogWebSocketHandler.sendRawLog(payload);
                } catch (Exception e) {
                    System.out.println("mobile_data_feed 처리 실패: " + e.getMessage());
                }
            });

            // ── critical_alert: CRITICAL 오류 발생 → LOCKED 상태 알림 ───────────────────
            // AdminPC-Server 가 장비를 LOCKED 상태로 전환할 때 발생합니다.
            socket.on("critical_alert", args -> {
                try {
                    String payload = args[0].toString();
                    System.out.println("critical_alert 수신: " + payload);
                    criticalAlertService.handleCriticalAlert(payload);
                } catch (Exception e) {
                    System.out.println("critical_alert 처리 실패: " + e.getMessage());
                }
            });

            // ── device_status_changed: 장비 상태 변경 알림 ──────────────────────────
            // AdminPC-Server 가 STANDBY, IDLE, STOP 등 상태를 알릴 때 발생합니다.
            socket.on("device_status_changed", args -> {
                try {
                    String payload = args[0].toString();
                    JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
                    String deviceId = json.has("device_id") ? json.get("device_id").getAsString() : "";
                    String rawStatus = json.has("status") ? json.get("status").getAsString() : "";

                    // AdminPC-Server 의 형식을 그대로 유지하여 포워딩
                    Map<String, Object> statusUpdate = new HashMap<>();
                    statusUpdate.put("type", "device_status_changed");
                    statusUpdate.put("device_id", deviceId);
                    statusUpdate.put("status", rawStatus);
                    if (json.has("message")) {
                        statusUpdate.put("message", json.get("message").getAsString());
                    }

                    rawLogWebSocketHandler.sendRawLog(statusUpdate);
                    System.out.println("device_status_changed 수신 - deviceId: " + deviceId
                            + ", status: " + rawStatus);
                } catch (Exception e) {
                    System.out.println("device_status_changed 처리 실패: " + e.getMessage());
                }
            });

            // ── error_resolved: LOCKED 해제 → STANDBY 전환 알림 ─────────────────────────
            // AdminPC-Server 가 LOCKED 해제 후 STANDBY 로 전환할 때 발생합니다.
            socket.on("error_resolved", args -> {
                try {
                    String payload = args[0].toString();
                    JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
                    String deviceId = json.has("device_id") ? json.get("device_id").getAsString() : "";
                    String resolvedBy = json.has("resolved_by") ? json.get("resolved_by").getAsString() : "";

                    // LOCKED 해제 후 AdminPC-Server 는 STANDBY 로 전환하므로 STANDBY 로 포워딩
                    Map<String, Object> resolveUpdate = new HashMap<>();
                    resolveUpdate.put("type", "error_resolved");
                    resolveUpdate.put("device_id", deviceId);
                    resolveUpdate.put("status", "STANDBY");
                    resolveUpdate.put("resolved_by", resolvedBy);

                    rawLogWebSocketHandler.sendRawLog(resolveUpdate);

                    // AlertWebSocketHandler 를 통해 알림도 포워딩
                    AlertEvent resolveEvent = AlertEvent.builder()
                            .alertId("resolve-" + deviceId + "-" + System.currentTimeMillis())
                            .deviceId(deviceId)
                            .errorCode("RESOLVED")
                            .errorMsg("장비 잠금 해제: " + resolvedBy)
                            .severity("LOW")
                            .timestamp(java.time.LocalDateTime.now().toString())
                            .build();
                    alertWebSocketHandler.sendAlert(resolveEvent);

                    System.out.println("error_resolved 수신 - deviceId: " + deviceId
                            + ", resolvedBy: " + resolvedBy + " → STANDBY 로 전환 포워딩");
                } catch (Exception e) {
                    System.out.println("error_resolved 처리 실패: " + e.getMessage());
                }
            });

            // ── 연결 상태 로그 ─────────────────────────────────────────────────────────
            socket.on(Socket.EVENT_CONNECT, args ->
                    System.out.println("AdminPC-Server Socket.IO 연결 성공"));
            socket.on(Socket.EVENT_DISCONNECT, args ->
                    System.out.println("AdminPC-Server Socket.IO 연결 종료"));
            socket.on(Socket.EVENT_CONNECT_ERROR, args ->
                    System.out.println("AdminPC-Server Socket.IO 연결 실패: " + args[0]));

            socket.connect();
            System.out.println("AdminPC-Server Socket.IO 연결 시도: " + adminBaseUrl);

        } catch (Exception e) {
            System.out.println("Socket.IO 초기화 실패: " + e.getMessage());
        }
    }

    @PreDestroy
    public void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket.close();
        }
    }

    // 3초마다 활동 중인 모바일 앱 유저(최근 8초 이내)를 AdminPC로 전송
    @Scheduled(fixedRate = 3000)
    public void sendMobilePresence() {
        if (socket == null || !socket.connected()) return;

        long now = System.currentTimeMillis();
        List<Map<String, Object>> activeUsersList = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : PresenceFilter.activeUsers.entrySet()) {
            String username = entry.getKey();
            Map<String, Object> userInfo = entry.getValue();
            long lastSeen = (long) userInfo.get("last_seen");

            // 8초 이내에 API 호출이 있었던 유저만 온라인으로 간주
            if (now - lastSeen < 8000) {
                activeUsersList.add(userInfo);
            } else {
                PresenceFilter.activeUsers.remove(username);
            }
        }

        try {
            String payload = new com.google.gson.Gson().toJson(activeUsersList);
            socket.emit("mobile_presence", new org.json.JSONArray(payload));
        } catch (Exception e) {
            System.out.println("mobile_presence 전송 실패: " + e.getMessage());
        }
    }
}