package com.semse.mobile_server.controller;

import com.semse.mobile_server.dto.ApiResponse;
import com.semse.mobile_server.dto.DeviceDetailResponse;
import com.semse.mobile_server.dto.DeviceListResponse;
import com.semse.mobile_server.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

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
    // 오류 해제 API (앱 → MobileServer → AdminPC-Server)
    @PostMapping("/{deviceId}/resolve")
    public ResponseEntity<ApiResponse<String>> resolveDevice(
            @PathVariable String deviceId) {
        deviceService.resolveDevice(deviceId);
        return ResponseEntity.ok(ApiResponse.ok("오류 해제 완료"));
    }
}