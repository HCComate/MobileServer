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

    // Flask critical_alert 이벤트 수신 시 호출
    public void handleCriticalAlert(String payload) {
        try {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();

            String deviceId = json.has("device_id") ? json.get("device_id").getAsString() : "UNKNOWN";
            String timestamp = json.has("timestamp") ? json.get("timestamp").getAsString() : "";
            String batchId = json.has("batch_id") ? json.get("batch_id").getAsString() : "";

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

            alertWebSocketHandler.sendAlert(AlertEvent.builder()
                    .alertId(UUID.randomUUID().toString())
                    .deviceId(deviceId)
                    .errorCode(errorCode)
                    .errorMsg(errorMsg)
                    .severity("CRITICAL")
                    .timestamp(timestamp)
                    .build());

            System.out.println("Critical alert forwarded: " + deviceId);

        } catch (Exception e) {
            System.out.println("CriticalAlertService 처리 실패: " + e.getMessage());
        }
    }
}