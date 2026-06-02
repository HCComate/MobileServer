package com.semse.mobile_server.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
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
    private final AlertService alertService;
    private final AlertWebSocketHandler alertWebSocketHandler;
    private final AdminPcAuthClient adminPcAuthClient;
    private final RestTemplate restTemplate; // RestTemplateConfig 빈 주입
    private final RawLogWebSocketHandler rawLogWebSocketHandler;

    private long lastId = 0;

    // -1: 미초기화, 0+: 초기화 완료 (이 ID 이후 로그만 Alert 발생)
    private long alertStartId = -1;

    /**
     * 첫 번째 pollAdminPc() 호출 시 AlertStartId를 초기화합니다.
     * @PostConstruct 대신 지연 초기화로 Spring 컨텍스트 블로킹을 방지합니다.
     */
    private void initIfNeeded() {
        if (alertStartId >= 0) return;

        try {
            // 현재 최대 로그 ID 조회 → 재시작 전 과거 CRITICAL 재발화 방지
            String url = adminPcAuthClient.getBaseUrl() + "/api/logs?limit=1";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminPcAuthClient.getValidToken());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            JsonArray logs = JsonParser.parseString(response.getBody()).getAsJsonArray();

            if (logs.size() > 0) {
                long maxId = logs.get(0).getAsJsonObject().get("id").getAsLong();
                alertStartId = maxId;
                lastId = Math.max(0, maxId - 2000);
                System.out.println("[PollingService] Init: lastId=" + lastId
                        + ", alertStartId=" + alertStartId);
            } else {
                alertStartId = 0;
            }

            // 현재 잠긴 장치 Alert DB 동기화
            // 재시작 전에 CRITICAL이 발생한 장치가 있으면 Alert DB에 등록해 앱 알람 보장
            syncLockedDeviceAlerts();

        } catch (Exception e) {
            alertStartId = 0;
            System.out.println("[PollingService] Init failed, alertStartId=0: " + e.getMessage());
        }
    }

    /**
     * AdminPC-Server의 현재 잠긴 장치 목록을 Alert DB에 동기화합니다.
     * MobileServer 재시작 후에도 잠긴 장치에 대한 알람이 앱으로 전달되도록 보장합니다.
     */
    private void syncLockedDeviceAlerts() {
        try {
            String url = adminPcAuthClient.getBaseUrl() + "/api/devices/locked";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminPcAuthClient.getValidToken());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            JsonArray lockedDevices = JsonParser.parseString(response.getBody()).getAsJsonArray();

            for (JsonElement el : lockedDevices) {
                JsonObject dev = el.getAsJsonObject();
                String deviceId = dev.has("device_id") ? dev.get("device_id").getAsString() : "";
                if (deviceId.isEmpty()) continue;

                String alertId = "locked_sync_" + deviceId;
                String errorCode = "CRITICAL";
                String errorMsg  = "CRITICAL 오류 — 장비 잠금 중";

                if (dev.has("error_codes") && dev.getAsJsonArray("error_codes").size() > 0) {
                    errorCode = dev.getAsJsonArray("error_codes").get(0).getAsString();
                    errorMsg  = "CRITICAL: " + errorCode;
                }

                String timestamp = dev.has("locked_at") ? dev.get("locked_at").getAsString()
                        : dev.has("timestamp") ? dev.get("timestamp").getAsString() : "";

                com.semse.mobile_server.dto.AlertEvent alertEvent =
                        com.semse.mobile_server.dto.AlertEvent.builder()
                                .alertId(alertId)
                                .deviceId(deviceId)
                                .errorCode(errorCode)
                                .errorMsg(errorMsg)
                                .severity("CRITICAL")
                                .timestamp(timestamp)
                                .build();

                alertService.saveAlert(alertEvent); // existsById()로 중복 방지됨
            }

            if (lockedDevices.size() > 0) {
                System.out.println("[PollingService] 잠긴 장치 Alert 동기화: "
                        + lockedDevices.size() + "건");
            }
        } catch (Exception e) {
            System.out.println("[PollingService] 잠긴 장치 동기화 실패 (무시): " + e.getMessage());
        }
    }

    @Scheduled(fixedRate = 5000)
    public void pollAdminPc() {
        initIfNeeded();

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

                if (currentId > alertStartId) {
                    detectAndSaveAlert(logJson, currentId);
                }
            }

            if (logs.size() > 0) {
                System.out.println("[PollingService] saved=" + logs.size() + " lastId=" + lastId);
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("[PollingService] Poll failed: " + e.getMessage());
            if (e.getStatusCode() == org.springframework.http.HttpStatus.UNAUTHORIZED) {
                adminPcAuthClient.invalidateToken();
            }
        } catch (Exception e) {
            System.out.println("[PollingService] Poll failed: " + e.getMessage());
        }
    }

    private void detectAndSaveAlert(JsonObject logJson, long logId) {
        try {
            String machineStatus = logJson.has("machine_status")
                    ? logJson.get("machine_status").getAsString() : "";
            if (!"ERROR".equals(machineStatus)) return;

            JsonArray statusArray = logJson.has("status_info")
                    ? logJson.getAsJsonArray("status_info") : new JsonArray();

            String topSeverity = "LOW";
            String topCode = "ERR_UNKNOWN";
            String topMsg = "Device error";

            for (int i = 0; i < statusArray.size(); i++) {
                JsonObject s = statusArray.get(i).getAsJsonObject();
                String sev = s.has("severity") ? s.get("severity").getAsString() : "LOW";
                if ("CRITICAL".equals(sev)) {
                    topSeverity = sev;
                    topCode = s.has("code") ? s.get("code").getAsString() : topCode;
                    topMsg  = s.has("msg")  ? s.get("msg").getAsString()  : topMsg;
                    break;
                }
            }

            if (!"CRITICAL".equals(topSeverity)) return;

            String deviceId = logJson.has("device_id")
                    ? logJson.get("device_id").getAsString() : "UNKNOWN";
            String timestamp = logJson.has("timestamp")
                    ? logJson.get("timestamp").getAsString() : "";
            String alertId = "alert_" + logId + "_" + deviceId;

            com.semse.mobile_server.dto.AlertEvent alertEvent =
                    com.semse.mobile_server.dto.AlertEvent.builder()
                            .alertId(alertId)
                            .deviceId(deviceId)
                            .errorCode(topCode)
                            .errorMsg(topMsg)
                            .severity(topSeverity)
                            .timestamp(timestamp)
                            .build();
            alertService.saveAlert(alertEvent);
            System.out.println("[PollingService] CRITICAL alert saved: " + alertId);

        } catch (Exception e) {
            System.out.println("[PollingService] Alert save failed: " + e.getMessage());
        }
    }

    private Object buildRawLogPayload(JsonObject logJson) {
        java.util.Map<String, Object> header = new java.util.HashMap<>();
        header.put("device_id",  logJson.has("device_id")  ? logJson.get("device_id").getAsString()  : "");
        header.put("batch_id",   logJson.has("batch_id")   ? logJson.get("batch_id").getAsString()   : "");
        header.put("model_name", logJson.has("model_name") ? logJson.get("model_name").getAsString() : "");

        String rawStatus = logJson.has("machine_status") ? logJson.get("machine_status").getAsString() : "";
        String convertedStatus = convertMachineStatusForPayload(rawStatus, logJson);

        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("sequence",       logJson.has("sequence")  ? logJson.get("sequence").getAsInt()    : 0);
        body.put("machine_status", convertedStatus);
        body.put("timestamp",      logJson.has("timestamp") ? logJson.get("timestamp").getAsString(): "");

        if (logJson.has("sensor_data"))   body.put("sensor_data",   logJson.getAsJsonObject("sensor_data"));
        if (logJson.has("vision_result")) body.put("vision_result",  logJson.getAsJsonObject("vision_result"));
        if (logJson.has("status_info"))   body.put("status_info",    logJson.getAsJsonArray("status_info"));

        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("header", header);
        payload.put("body",   body);
        return payload;
    }

    private String convertMachineStatusForPayload(String rawStatus, JsonObject logJson) {
        if ("ERROR".equals(rawStatus)) {
            JsonArray statusArray = logJson.has("status_info")
                    ? logJson.getAsJsonArray("status_info") : new JsonArray();
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
