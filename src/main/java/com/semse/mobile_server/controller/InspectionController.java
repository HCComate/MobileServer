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
    public ResponseEntity<ApiResponse<List<DeviceDetailResponse>>> getRecentLogs() {
        return ResponseEntity.ok(ApiResponse.ok(inspectionService.getRecentLogs()));
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<DeviceDetailResponse>> getLatestByDevice(
            @RequestParam String deviceId) {
        return ResponseEntity.ok(ApiResponse.ok(inspectionService.getLatestByDevice(deviceId)));
    }
}