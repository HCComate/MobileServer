package com.semse.mobile_server.service;

import com.semse.mobile_server.dto.AlertEvent;
import com.semse.mobile_server.dto.AlertRespondRequest;
import com.semse.mobile_server.entity.Alert;
import com.semse.mobile_server.entity.Severity;
import com.semse.mobile_server.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final AdminPcAuthClient adminPcAuthClient;
    private final RestTemplate restTemplate;

    @Value("${admin.pc.base-url}")
    private String adminBaseUrl;

    public void saveAlert(AlertEvent event) {
        // 중복 저장 방지
        if (alertRepository.existsById(event.getAlertId())) return;

        LocalDateTime timestamp;
        try {
            // "yyyy-MM-dd HH:mm:ss.SSS" 또는 ISO-8601 모두 처리
            String ts = event.getTimestamp();
            if (ts != null && ts.contains("T")) {
                timestamp = LocalDateTime.parse(ts);
            } else if (ts != null && !ts.isEmpty()) {
                timestamp = LocalDateTime.parse(ts,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
            } else {
                timestamp = LocalDateTime.now();
            }
        } catch (Exception e) {
            timestamp = LocalDateTime.now();
        }

        Severity severity;
        try {
            severity = Severity.valueOf(event.getSeverity());
        } catch (Exception e) {
            severity = Severity.HIGH;
        }

        Alert alert = Alert.builder()
                .alertId(event.getAlertId())
                .deviceId(event.getDeviceId())
                .errorCode(event.getErrorCode())
                .errorMsg(event.getErrorMsg())
                .severity(severity)
                .timestamp(timestamp)
                .build();

        alertRepository.save(alert);
    }

    public void respond(String alertId, AlertRespondRequest request) {
        System.out.println("[AlertService] respond 진입: alertId=" + alertId
                + " response=" + request.getResponse()
                + " bodyDeviceId=" + request.getDeviceId());

        // 1. 로컬 DB 업데이트 + deviceId 추출
        String[] deviceIdHolder = {null};
        alertRepository.findById(alertId).ifPresent(alert -> {
            alert.setResponse(request.getResponse());
            alert.setRespondedUserId(request.getUserId());
            alert.setRespondedAt(LocalDateTime.now());
            alertRepository.save(alert);
            deviceIdHolder[0] = alert.getDeviceId();
        });

        // deviceId 우선순위: 앱이 보낸 body deviceId > DB 조회 > alertId 파싱
        String deviceId;
        if (request.getDeviceId() != null && !request.getDeviceId().isEmpty()) {
            deviceId = request.getDeviceId();
        } else if (deviceIdHolder[0] != null) {
            deviceId = deviceIdHolder[0];
        } else {
            deviceId = extractDeviceId(alertId);
        }

        // 2. AdminPC-Server 에스컬레이션 처리 포워딩 (deviceId 기준)
        forwardToAdminPc(alertId, deviceId, request);
    }

    /**
     * alertId 형식에서 deviceId를 추출합니다.
     * - "alert_{logId}_{deviceId}" → deviceId
     * - "locked_sync_{deviceId}"   → deviceId
     * - UUID 형식 등               → alertId 그대로 반환 (AdminPC에서 처리)
     */
    private String extractDeviceId(String alertId) {
        if (alertId.startsWith("alert_")) {
            // "alert_202490_RASP_PI_04" → ["alert","202490","RASP_PI_04"]
            String[] parts = alertId.split("_", 3);
            if (parts.length >= 3) return parts[2];
        }
        if (alertId.startsWith("locked_sync_")) {
            return alertId.substring("locked_sync_".length());
        }
        return alertId;
    }

    private void forwardToAdminPc(String alertId, String deviceId, AlertRespondRequest request) {
        try {
            String url = adminBaseUrl + "/api/alerts/" + deviceId + "/respond";
            HttpHeaders headers = new HttpHeaders();
            // charset 명시 — eventlet/Flask가 확실히 JSON으로 파싱하도록
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Secret", "capstone2026");

            String username = request.getUserId() != null && !request.getUserId().isEmpty()
                    ? request.getUserId() : "mobile_user";

            // body 유실 대비 — 헤더로도 응답 정보 전달 (fallback)
            headers.set("X-Response-Type", request.getResponse());
            headers.set("X-Response-User", username);

            // JSON을 문자열로 직접 직렬화 (Map 컨버터 의존성 제거 → 직렬화 오류 방지)
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            json.addProperty("response", request.getResponse());
            json.addProperty("userId", username);
            json.addProperty("username", username);

            HttpEntity<String> entity = new HttpEntity<>(json.toString(), headers);
            org.springframework.http.ResponseEntity<String> resp =
                    restTemplate.postForEntity(url, entity, String.class);
            System.out.println("[AlertService] AdminPC 포워딩 성공(" + resp.getStatusCode()
                    + "): deviceId=" + deviceId + " → " + request.getResponse() + " by " + username);
        } catch (Exception e) {
            System.out.println("[AlertService] AdminPC 포워딩 실패: " + e.getMessage());
        }
    }

    public List<Alert> getAllAlerts() {
        return alertRepository.findAllByOrderByTimestampDesc();
    }

    public List<Alert> getPendingAlerts() {
        return alertRepository.findByResponseIsNullOrderByTimestampDesc();
    }

    /**
     * 특정 사용자가 현재 에스컬레이션 대상(current_target)인 알림만 AdminPC-Server에서 조회합니다.
     *
     * <p>AdminPC-Server의 /api/alerts/pending은 current_target 필터를 적용하므로,
     * 순차 에스컬레이션에서 "지금 알림 받을 차례인 사람"에게만 알림이 반환됩니다.</p>
     *
     * @param username 앱 사용자명 (앱 JWT에서 추출)
     * @return AdminPC가 반환한 JSON 문자열 ({ success, data: [...] })
     */
    public String getPendingAlertsForUser(String username) {
        try {
            String url = adminBaseUrl + "/api/alerts/pending";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", "capstone2026");
            // AdminPC가 이 사용자 기준으로 current_target 필터링하도록 전달
            headers.set("X-Target-User", username != null ? username : "");

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            return resp.getBody();
        } catch (Exception e) {
            System.out.println("[AlertService] AdminPC pending 위임 실패: " + e.getMessage());
            return null;
        }
    }
}
