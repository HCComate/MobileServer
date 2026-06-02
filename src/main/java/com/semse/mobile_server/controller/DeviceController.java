package com.semse.mobile_server.controller;

import com.semse.mobile_server.dto.ApiResponse;
import com.semse.mobile_server.dto.DeviceDetailResponse;
import com.semse.mobile_server.dto.DeviceListResponse;
import com.semse.mobile_server.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeviceListResponse>>> getDevices() {
        try {
            List<DeviceListResponse> devices = deviceService.getAllDevices();
            log.info("[DeviceController] 장비 목록 반환: {}건", devices.size());
            return ResponseEntity.ok(ApiResponse.ok(devices));
        } catch (Exception e) {
            log.error("[DeviceController] 장비 목록 조회 실패: {}", e.getMessage(), e);
            throw e;
        }
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
            @PathVariable String deviceId) {
        deviceService.resolveDevice(deviceId);
        return ResponseEntity.ok(ApiResponse.ok("오류 해제 완료"));
    }
}
