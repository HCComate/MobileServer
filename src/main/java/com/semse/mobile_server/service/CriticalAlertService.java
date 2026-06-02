package com.semse.mobile_server.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.semse.mobile_server.config.AlertWebSocketHandler;
import com.semse.mobile_server.dto.AlertEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * AdminPC-Server 에서 수신한 CRITICAL 알림을 처리하는 서비스입니다.
 *
 * <p>AdminPC-Server 의 {@code critical_alert} 이벤트 페이로드 구조:</p>
 * <pre>
 * {
 *   "device_id": "DEVICE_001",
 *   "error_codes": ["E001", "E002"],
 *   "timestamp": "2024-01-01 12:00:00.000",
 *   "batch_id": "BATCH_001"
 * }
 * </pre>
 *
 * <p>MobileApp 으로는 AlertEvent 를 통해 알림을 전달합니다.
 * 장비 상태는 {@code LOCKED} 로 전환되며, 이후 수신되는 검사 데이터도
 * {@link InspectionService#saveInspectionData} 에서 LOCKED 로 저장됩니다.</p>
 */
@Service
@RequiredArgsConstructor
public class CriticalAlertService {

    private final AlertWebSocketHandler alertWebSocketHandler;
    private final AlertService alertService;

    /**
     * AdminPC-Server 에서 수신한 CRITICAL 알림을 처리합니다.
     *
     * <p>AdminPC-Server 의 {@code critical_alert} 페이로드에서 {@code error_codes} 배열을 파싱하여
     * 첫 번째 코드를 errorCode 로, 전체 코드 목록을 errorMsg 로 구성합니다.</p>
     *
     * @param payload AdminPC-Server 에서 수신한 JSON 문자열
     */
    public void handleCriticalAlert(String payload) {
        try {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();

            String deviceId = json.has("device_id") ? json.get("device_id").getAsString() : "UNKNOWN";
            String timestamp = json.has("timestamp") ? json.get("timestamp").getAsString() : "";
            String batchId = json.has("batch_id") ? json.get("batch_id").getAsString() : "";

            // error_codes 배열에서 errorCode / errorMsg 추출
            // AdminPC-Server 는 locked_devices[device_id]["error_codes"] = codes 형태로 전달
            String errorCode = "CRITICAL";
            String errorMsg = "CRITICAL 오류 발생 - 장비 잠금";
            if (json.has("error_codes")) {
                JsonArray codes = json.getAsJsonArray("error_codes");
                if (codes.size() > 0) {
                    errorCode = codes.get(0).getAsString();
                    StringBuilder sb = new StringBuilder("CRITICAL 오류 코드: ");
                    for (int i = 0; i < codes.size(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(codes.get(i).getAsString());
                    }
                    if (!batchId.isEmpty()) {
                        sb.append(" [배치: ").append(batchId).append("]");
                    }
                    errorMsg = sb.toString();
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

            // WebSocket 전송 (실시간 연결 클라이언트용)
            alertWebSocketHandler.sendAlert(event);

            // DB 저장 (REST 폴링 앱용) — 앱의 /api/alerts/pending 폴링에서 감지
            alertService.saveAlert(event);

            System.out.println("[CriticalAlertService] CRITICAL alert saved and forwarded: "
                    + deviceId + " / " + errorCode);

        } catch (Exception e) {
            System.out.println("[CriticalAlertService] 처리 실패: " + e.getMessage());
        }
    }
}
