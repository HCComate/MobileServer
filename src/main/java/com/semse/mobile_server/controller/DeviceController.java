package com.semse.mobile_server.controller;

import com.semse.mobile_server.entity.InspectionLog;
import com.semse.mobile_server.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.semse.mobile_server.dto.DeviceDetailResponse;
import com.semse.mobile_server.dto.DeviceListResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public List<DeviceListResponse> getDevices() {
        return deviceService.getAllDevices();
    }
    @GetMapping("/{deviceId}/detail")
    public DeviceDetailResponse getDeviceDetail(@PathVariable String deviceId) {
        return deviceService.getDeviceDetail(deviceId);
    }
}