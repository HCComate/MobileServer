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
import com.semse.mobile_server.config.RawLogWebSocketHandler;


@Service
@RequiredArgsConstructor
public class PollingService {

    private final InspectionService inspectionService;
    private final AlertWebSocketHandler alertWebSocketHandler;

    private final RestTemplate restTemplate = new RestTemplate();
    private final RawLogWebSocketHandler rawLogWebSocketHandler;

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
                rawLogWebSocketHandler.sendRawLog(buildRawLogPayload(logJson));

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
    private Object buildRawLogPayload(JsonObject logJson) {
        // header/body 구조로 변환
        java.util.Map<String, Object> header = new java.util.HashMap<>();
        header.put("device_id", logJson.has("device_id") ? logJson.get("device_id").getAsString() : "");
        header.put("batch_id", logJson.has("batch_id") ? logJson.get("batch_id").getAsString() : "");
        header.put("model_name", logJson.has("model_name") ? logJson.get("model_name").getAsString() : "");

        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("sequence", logJson.has("sequence") ? logJson.get("sequence").getAsInt() : 0);
        body.put("machine_status", logJson.has("machine_status") ? logJson.get("machine_status").getAsString() : "");
        body.put("timestamp", logJson.has("timestamp") ? logJson.get("timestamp").getAsString() : "");

        if (logJson.has("sensor_data")) {
            body.put("sensor_data", logJson.getAsJsonObject("sensor_data"));
        }
        if (logJson.has("vision_result")) {
            body.put("vision_result", logJson.getAsJsonObject("vision_result"));
        }
        if (logJson.has("status_info")) {
            body.put("status_info", logJson.getAsJsonArray("status_info"));
        }

        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("header", header);
        payload.put("body", body);
        return payload;
    }
}