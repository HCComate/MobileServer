package com.semse.mobile_server.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.semse.mobile_server.service.InspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}