package com.semse.mobile_server.controller;

import com.semse.mobile_server.dto.ApiResponse;
import com.semse.mobile_server.dto.DeviceDetailResponse;
import com.semse.mobile_server.dto.DeviceListResponse;
import com.semse.mobile_server.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${admin.pc.base-url}")
    private String adminBaseUrl;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeviceListResponse>>> getDevices() {
        return ResponseEntity.ok(ApiResponse.ok(deviceService.getAllDevices()));
    }

    @GetMapping("/{deviceId}/detail")
    public ResponseEntity<ApiResponse<DeviceDetailResponse>> getDeviceDetail(
            @PathVariable String deviceId) {
        return ResponseEntity.ok(ApiResponse.ok(deviceService.getDeviceDetail(deviceId)));
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<DeviceDetailResponse>> getDeviceDetailAlias(
            @PathVariable String deviceId) {
        return ResponseEntity.ok(ApiResponse.ok(deviceService.getDeviceDetail(deviceId)));
    }
    @PostMapping("/{deviceId}/resolve")
    public ResponseEntity<ApiResponse<String>> resolveDevice(
            @PathVariable String deviceId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String url = adminBaseUrl + "/api/devices/" + deviceId + "/resolve";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (authHeader != null) {
                headers.set("Authorization", authHeader);
            }

            HttpEntity<String> entity = new HttpEntity<>("{}", headers);
            restTemplate.postForEntity(url, entity, String.class);

            return ResponseEntity.ok(ApiResponse.ok("장비 잠금 해제 완료"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("장비 잠금 해제 실패: " + e.getMessage()));
        }
    }
}