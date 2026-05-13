package com.semse.mobile_server.controller;

import com.semse.mobile_server.dto.AlertRespondRequest;
import com.semse.mobile_server.dto.ApiResponse;
import com.semse.mobile_server.entity.Alert;
import com.semse.mobile_server.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    // 전체 알림 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<Alert>>> getAllAlerts() {
        return ResponseEntity.ok(ApiResponse.ok(alertService.getAllAlerts()));
    }

    // 미응답 알림 조회
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<Alert>>> getPendingAlerts() {
        return ResponseEntity.ok(ApiResponse.ok(alertService.getPendingAlerts()));
    }

    // 수락/거절 응답
    @PostMapping("/{alertId}/respond")
    public ResponseEntity<ApiResponse<String>> respond(
            @PathVariable String alertId,
            @RequestBody AlertRespondRequest request) {
        alertService.respond(alertId, request);
        return ResponseEntity.ok(ApiResponse.ok("응답 완료"));
    }
}