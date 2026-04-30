package com.semse.mobile_server.controller;

import com.semse.mobile_server.entity.InspectionLog;
import com.semse.mobile_server.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public List<InspectionLog> getDevices() {
        return deviceService.getAllDevices();
    }
    @GetMapping("/{deviceId}/detail")
    public InspectionLog getDeviceDetail(@PathVariable String deviceId) {
        return deviceService.getDeviceDetail(deviceId);
    }
}