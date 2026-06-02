package com.semse.mobile_server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semse.mobile_server.dto.AlertRespondRequest;
import com.semse.mobile_server.dto.ApiResponse;
import com.semse.mobile_server.entity.Alert;
import com.semse.mobile_server.service.AlertService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 전체 알림 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<Alert>>> getAllAlerts() {
        return ResponseEntity.ok(ApiResponse.ok(alertService.getAllAlerts()));
    }

    // 미응답 알림 조회
    // ⚠️ 사용자별 current_target 필터를 위해 AdminPC-Server로 위임
    //    (자체 DB의 미필터 목록을 반환하면 모든 앱에 동시 알림이 가는 문제 발생)
    @GetMapping(value = "/pending", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getPendingAlerts(HttpServletRequest request) {
        String username = extractUsername(request);
        String body = alertService.getPendingAlertsForUser(username);

        if (body != null) {
            return ResponseEntity.ok(body);
        }
        // AdminPC 위임 실패 시 빈 목록 (자체 DB 전체 반환 금지 → 오발송 방지)
        return ResponseEntity.ok("{\"success\":true,\"data\":[]}");
    }

    // 수락/거절 응답
    @PostMapping("/{alertId}/respond")
    public ResponseEntity<ApiResponse<String>> respond(
            @PathVariable String alertId,
            @RequestBody AlertRespondRequest request) {
        alertService.respond(alertId, request);
        return ResponseEntity.ok(ApiResponse.ok("응답 완료"));
    }

    /**
     * Authorization: Bearer 토큰의 payload(2번째 세그먼트)를 base64 디코딩해 username을 추출합니다.
     * AdminPC/MobileServer 토큰 서명 방식이 달라도 payload는 읽을 수 있습니다(검증 없이 파싱).
     */
    private String extractUsername(HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) return null;
            String token = auth.substring(7);
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            Map<String, Object> payload = MAPPER.readValue(payloadJson, Map.class);
            // AdminPC 토큰: username 클레임 / MobileServer 토큰: username 또는 sub
            Object username = payload.get("username");
            if (username == null) username = payload.get("sub");
            return username != null ? username.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
