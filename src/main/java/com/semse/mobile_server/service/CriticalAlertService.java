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

    public void handleCriticalAlert(String payload) {
        try {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();

            String deviceId = json.has("device_id") ? json.get("device_id").getAsString() : "UNKNOWN";
            String timestamp = json.has("timestamp") ? json.get("timestamp").getAsString() : "";

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
                    .build();

            alertWebSocketHandler.sendAlert(event);

            System.out.println("Critical alert forwarded → deviceId: " + deviceId);

        } catch (Exception e) {
            System.out.println("CriticalAlertService 처리 실패: " + e.getMessage());
        }
    }
}
