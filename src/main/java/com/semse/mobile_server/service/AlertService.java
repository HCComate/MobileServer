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

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final EscalationService escalationService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${admin.pc.base-url}")
    private String adminBaseUrl;

    public void saveAlert(AlertEvent event) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        LocalDateTime timestamp;
        try {
            timestamp = LocalDateTime.parse(event.getTimestamp(), formatter);
        } catch (Exception e) {
            timestamp = LocalDateTime.now();
        }

        Alert alert = Alert.builder()
                .alertId(event.getAlertId())
                .deviceId(event.getDeviceId())
                .errorCode(event.getErrorCode())
                .errorMsg(event.getErrorMsg())
                .severity(Severity.valueOf(event.getSeverity()))
                .timestamp(timestamp)
                .build();

        alertRepository.save(alert);
    }

    public void respond(String alertId, AlertRespondRequest request) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert를 찾을 수 없습니다: " + alertId));

        alert.setResponse(request.getResponse());
        alert.setRespondedUserId(request.getUserId());
        alert.setRespondedAt(LocalDateTime.now());
        alertRepository.save(alert);

        // 에스컬레이션 타이머 취소 (수락/거절 시)
        escalationService.cancelTimer(alertId);

        // 수락(ACCEPTED) 시 재민이 Flask 서버로 장비 잠금 해제 요청
        if ("ACCEPTED".equals(request.getResponse())) {
            try {
                String url = adminBaseUrl + "/api/devices/" + alert.getDeviceId() + "/resolve";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<String> entity = new HttpEntity<>("{}", headers);
                restTemplate.postForEntity(url, entity, String.class);

                System.out.println("[AlertService] 장비 잠금 해제 요청 완료 → deviceId: " + alert.getDeviceId());
            } catch (Exception e) {
                System.out.println("[AlertService] 장비 잠금 해제 요청 실패: " + e.getMessage());
            }
        }
    }

    public List<Alert> getAllAlerts() {
        return alertRepository.findAllByOrderByTimestampDesc();
    }

    public List<Alert> getPendingAlerts() {
        return alertRepository.findByResponseIsNullOrderByTimestampDesc();
    }
}
