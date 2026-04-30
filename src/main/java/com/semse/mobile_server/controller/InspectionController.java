package com.semse.mobile_server.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.semse.mobile_server.service.InspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.semse.mobile_server.entity.InspectionLog;
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
    public List<InspectionLog> getRecentLogs() {
        return inspectionService.getRecentLogs();
    }
    @GetMapping("/latest")
    public InspectionLog getLatestByDevice(@RequestParam String deviceId) {
        return inspectionService.getLatestByDevice(deviceId);
    }
}