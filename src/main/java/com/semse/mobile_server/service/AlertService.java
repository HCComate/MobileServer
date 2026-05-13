package com.semse.mobile_server.service;

import com.semse.mobile_server.dto.AlertEvent;
import com.semse.mobile_server.dto.AlertRespondRequest;
import com.semse.mobile_server.entity.Alert;
import com.semse.mobile_server.entity.Severity;
import com.semse.mobile_server.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;

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
    }

    public List<Alert> getAllAlerts() {
        return alertRepository.findAllByOrderByTimestampDesc();
    }

    public List<Alert> getPendingAlerts() {
        return alertRepository.findByResponseIsNullOrderByTimestampDesc();
    }
}