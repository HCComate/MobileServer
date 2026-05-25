package com.semse.mobile_server.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.semse.mobile_server.config.AlertWebSocketHandler;
import com.semse.mobile_server.dto.AlertEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CriticalAlertService {

    private final AlertWebSocketHandler alertWebSocketHandler;
    private final EscalationService escalationService;

    // Flask critical_alert 이벤트 수신 시 호출
    public void handleCriticalAlert(String payload) {
        try {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();

            String deviceId = json.has("device_id") ? json.get("device_id").getAsString() : "UNKNOWN";
            String timestamp = json.has("timestamp") ? json.get("timestamp").getAsString() : "";
            String targetUserId = json.has("target_user_id") ? json.get("target_user_id").getAsString() : null;

            // error_codes 배열에서 첫 번째 코드 사용
            String errorCode = "CRITICAL";
            String errorMsg = "Critical alert from Admin PC";
            if (json.has("error_codes")) {
                JsonArray codes = json.getAsJsonArray("error_codes");
                if (codes.size() > 0) {
                    errorCode = codes.get(0).getAsString();
                    errorMsg = "Critical error detected: " + codes.toString();
                }
            }

            String alertId = UUID.randomUUID().toString();

            AlertEvent event = AlertEvent.builder()
                    .alertId(alertId)
                    .deviceId(deviceId)
                    .errorCode(errorCode)
                    .errorMsg(errorMsg)
                    .severity("CRITICAL")
                    .timestamp(timestamp)
                    .targetUserId(targetUserId)
                    .build();

            // 알림 전송
            alertWebSocketHandler.sendAlert(event);

            // 에스컬레이션 타이머 시작 (20초 방치 시 다음 유저로 넘김)
            if (targetUserId != null) {
                escalationService.startEscalationTimer(alertId, deviceId, errorCode, errorMsg, timestamp);
            }

            System.out.println("Critical alert forwarded → deviceId: " + deviceId + ", targetUserId: " + targetUserId);

        } catch (Exception e) {
            System.out.println("CriticalAlertService 처리 실패: " + e.getMessage());
        }
    }
}
