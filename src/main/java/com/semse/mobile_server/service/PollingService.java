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

    private final AdminPcAuthClient adminPcAuthClient;
    private final RestTemplate restTemplate = new RestTemplate();
    private final RawLogWebSocketHandler rawLogWebSocketHandler;

    private long lastId = 0;

    @Scheduled(fixedRate = 5000)
    public void pollAdminPc() {
        try {
            String url = adminPcAuthClient.getBaseUrl() + "/api/logs/after?last_id=" + lastId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminPcAuthClient.getValidToken());

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            JsonArray logs = JsonParser.parseString(response.getBody()).getAsJsonArray();

            for (JsonElement element : logs) {
                JsonObject logJson = element.getAsJsonObject();
                inspectionService.saveInspectionData(logJson);
                rawLogWebSocketHandler.sendRawLog(buildRawLogPayload(logJson));

                long currentId = logJson.get("id").getAsLong();
                if (currentId > lastId) lastId = currentId;
            }

            if (logs.size() > 0) {
                System.out.println("Polling success. saved logs: " + logs.size() + ", lastId: " + lastId);
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("Polling failed: " + e.getMessage());
            if (e.getStatusCode() == org.springframework.http.HttpStatus.UNAUTHORIZED) {
                adminPcAuthClient.invalidateToken();
            }
        } catch (Exception e) {
            System.out.println("Polling failed: " + e.getMessage());
        }
    }
    /**
     * AdminPC-Server 의 flat JSON 로그를 MobileApp 에 전달할 header/body 구조로 변환합니다.
     *
     * <p>AdminPC-Server 의 형식을 최대한 유지하며, CRITICAL 오류 시에만 LOCKED 로 변환합니다.</p>
     */
    private Object buildRawLogPayload(JsonObject logJson) {
        java.util.Map<String, Object> header = new java.util.HashMap<>();
        header.put("device_id", logJson.has("device_id") ? logJson.get("device_id").getAsString() : "");
        header.put("batch_id", logJson.has("batch_id") ? logJson.get("batch_id").getAsString() : "");
        header.put("model_name", logJson.has("model_name") ? logJson.get("model_name").getAsString() : "");

        // machine_status 변환: ERROR+CRITICAL → LOCKED (나머지는 원본 유지)
        String rawStatus = logJson.has("machine_status") ? logJson.get("machine_status").getAsString() : "";
        String convertedStatus = convertMachineStatusForPayload(rawStatus, logJson);

        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("sequence", logJson.has("sequence") ? logJson.get("sequence").getAsInt() : 0);
        body.put("machine_status", convertedStatus);
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

    /**
     * AdminPC-Server 의 rawStatus 를 MobileApp 에 전달할 상태 문자열로 변환합니다.
     *
     * <p>AdminPC-Server 의 형식을 최대한 유지하며, CRITICAL 오류 시에만 LOCKED 로 변환합니다.</p>
     */
    private String convertMachineStatusForPayload(String rawStatus, JsonObject logJson) {
        if ("ERROR".equals(rawStatus)) {
            JsonArray statusArray = logJson.has("status_info")
                    ? logJson.getAsJsonArray("status_info")
                    : new JsonArray();
            for (int i = 0; i < statusArray.size(); i++) {
                JsonObject s = statusArray.get(i).getAsJsonObject();
                if (s.has("severity") && "CRITICAL".equals(s.get("severity").getAsString())) {
                    return "LOCKED";
                }
            }
        }
        return rawStatus;
    }
}