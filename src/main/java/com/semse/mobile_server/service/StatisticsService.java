package com.semse.mobile_server.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.semse.mobile_server.dto.StatisticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${admin.pc.base-url}")
    private String adminBaseUrl;

    public StatisticsResponse getStatistics() {
        try {
            String url = adminBaseUrl + "/api/dashboard/summary";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonObject json = JsonParser.parseString(response.getBody()).getAsJsonObject();

            return StatisticsResponse.builder()
                    .totalDevices(json.get("total_devices").getAsInt())
                    .runningDevices(json.get("running_devices").getAsInt())
                    .errorDevices(json.get("error_devices").getAsInt())
                    .totalInspections(json.get("total_inspections").getAsInt())
                    .okCount(json.get("ok_count").getAsInt())
                    .ngCount(json.get("ng_count").getAsInt())
                    .ngRate(json.get("ng_rate").getAsDouble())
                    .errorCount(json.get("error_count").getAsInt())
                    .build();

        } catch (Exception e) {
            System.out.println("StatisticsService 호출 실패: " + e.getMessage());
            return StatisticsResponse.builder().build();
        }
    }
}