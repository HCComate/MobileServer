package com.semse.mobile_server.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.semse.mobile_server.dto.ApiResponse;
import com.semse.mobile_server.dto.DeviceDetailResponse;
import com.semse.mobile_server.service.InspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inspections")
public class InspectionController {

    private final InspectionService inspectionService;

    @PostMapping
    public String saveInspection(@RequestBody String body) {
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        inspectionService.saveInspectionData(json);
        return "Inspection data saved successfully";
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<DeviceDetailResponse>>> getRecentLogs(
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        // 과도한 요청 방지를 위해 1~500으로 제한 (앱 기본 요청은 200)
        int capped = Math.min(Math.max(limit, 1), 500);
        return ResponseEntity.ok(ApiResponse.ok(inspectionService.getRecentLogs(capped)));
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<DeviceDetailResponse>> getLatestByDevice(
            @RequestParam String deviceId) {
        return ResponseEntity.ok(ApiResponse.ok(inspectionService.getLatestByDevice(deviceId)));
    }

    // 이벤트(오류) 로그만 반환 — 이벤트 로그 화면용.
    // 초당 50건 폭주 환경에서 "최근 N건 필터" 방식이 ERROR를 누락하는 문제를 서버 필터로 해결.
    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<DeviceDetailResponse>>> getRecentEvents(
            @RequestParam(value = "limit", defaultValue = "100") int limit) {
        int capped = Math.min(Math.max(limit, 1), 500);
        return ResponseEntity.ok(ApiResponse.ok(inspectionService.getRecentEvents(capped)));
    }
}