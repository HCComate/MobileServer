package com.semse.mobile_server.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.semse.mobile_server.config.AlertWebSocketHandler;
import com.semse.mobile_server.dto.AlertEvent;


@Service
@RequiredArgsConstructor
public class PollingService {

    private final InspectionService inspectionService;
    private final AlertWebSocketHandler alertWebSocketHandler;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${admin.pc.base-url}")
    private String adminBaseUrl;

    @Value("${admin.pc.username}")
    private String username;

    @Value("${admin.pc.password}")
    private String password;

    private String token;
    private long lastId = 0;

    @Scheduled(fixedRate = 5000)
    public void pollAdminPc() {
        try {
            if (token == null) {
                login();
            }

            String url = adminBaseUrl + "/api/logs/after?last_id=" + lastId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonArray logs = JsonParser.parseString(response.getBody()).getAsJsonArray();

            for (JsonElement element : logs) {
                JsonObject logJson = element.getAsJsonObject();

                inspectionService.saveInspectionData(logJson);
                checkAndSendAlert(logJson);

                long currentId = logJson.get("id").getAsLong();
                if (currentId > lastId) {
                    lastId = currentId;
                }
            }

            if (logs.size() > 0) {
                System.out.println("Polling success. saved logs: " + logs.size() + ", lastId: " + lastId);
            }

        } catch (Exception e) {
            System.out.println("Polling failed: " + e.getMessage());
            token = null;
        }
    }
    private void checkAndSendAlert(JsonObject logJson) {
        String deviceId = logJson.has("device_id") ? logJson.get("device_id").getAsString() : "UNKNOWN";
        String timestamp = logJson.has("timestamp") ? logJson.get("timestamp").getAsString() : "";

        // 1. machine_status = ERROR
        if (logJson.has("machine_status") &&
                "ERROR".equals(logJson.get("machine_status").getAsString())) {
            alertWebSocketHandler.sendAlert(AlertEvent.builder()
                    .alertId(java.util.UUID.randomUUID().toString())
                    .deviceId(deviceId)
                    .errorCode("AM-CN-01")
                    .errorMsg("Machine status ERROR detected")
                    .severity("HIGH")
                    .timestamp(timestamp)
                    .build());
            return;
        }

        // 2. vision_result.result = NG
        if (logJson.has("vision_result")) {
            JsonObject visionResult = logJson.getAsJsonObject("vision_result");
            if (visionResult.has("result") &&
                    "NG".equals(visionResult.get("result").getAsString())) {
                alertWebSocketHandler.sendAlert(AlertEvent.builder()
                        .alertId(java.util.UUID.randomUUID().toString())
                        .deviceId(deviceId)
                        .errorCode("SV-PR-01")
                        .errorMsg("Vision result NG detected")
                        .severity("HIGH")
                        .timestamp(timestamp)
                        .build());
                return;
            }
        }

        // 3. status_info severity = HIGH 또는 CRITICAL
        if (logJson.has("status_info")) {
            JsonArray statusInfos = logJson.getAsJsonArray("status_info");
            for (JsonElement statusElement : statusInfos) {
                JsonObject statusInfo = statusElement.getAsJsonObject();
                if (statusInfo.has("severity")) {
                    String severity = statusInfo.get("severity").getAsString();
                    if ("HIGH".equals(severity) || "CRITICAL".equals(severity)) {
                        String code = statusInfo.has("code") ?
                                statusInfo.get("code").getAsString() : "UNKNOWN";
                        String msg = statusInfo.has("msg") ?
                                statusInfo.get("msg").getAsString() : "Status alert";
                        alertWebSocketHandler.sendAlert(AlertEvent.builder()
                                .alertId(java.util.UUID.randomUUID().toString())
                                .deviceId(deviceId)
                                .errorCode(code)
                                .errorMsg(msg)
                                .severity(severity)
                                .timestamp(timestamp)
                                .build());
                        return;
                    }
                }
            }
        }

        // 4. 센서 임계치 초과 (temperature > 60)
        if (logJson.has("sensor_data")) {
            JsonObject sensorData = logJson.getAsJsonObject("sensor_data");
            if (sensorData.has("temperature") &&
                    sensorData.get("temperature").getAsDouble() > 60.0) {
                alertWebSocketHandler.sendAlert(AlertEvent.builder()
                        .alertId(java.util.UUID.randomUUID().toString())
                        .deviceId(deviceId)
                        .errorCode("HM-TE-01")
                        .errorMsg("Temperature threshold exceeded: " +
                                sensorData.get("temperature").getAsDouble())
                        .severity("HIGH")
                        .timestamp(timestamp)
                        .build());
            }
        }
    }

    private void login() {
        String url = adminBaseUrl + "/api/auth/login";

        JsonObject loginBody = new JsonObject();
        loginBody.addProperty("username", username);
        loginBody.addProperty("password", password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(loginBody.toString(), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                url,
                entity,
                String.class
        );

        JsonObject json = JsonParser.parseString(response.getBody()).getAsJsonObject();
        this.token = json.get("token").getAsString();

        System.out.println("Admin PC login success");
    }
}