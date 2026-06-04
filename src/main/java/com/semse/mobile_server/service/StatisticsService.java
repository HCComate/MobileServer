package com.semse.mobile_server.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.semse.mobile_server.dto.StatisticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * AdminPC-Server 에서 대시보드 통계를 조회하는 서비스입니다.
 *
 * <p>AdminPC-Server 의 {@code GET /api/dashboard/summary} 응답에는 LOCKED 장비 수가 포함되지 않으므로,
 * {@code GET /api/devices/locked} 를 추가 호출하여 잠금 장비 수를 별도로 집계합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${admin.pc.base-url}")
    private String adminBaseUrl;

    /**
     * AdminPC-Server 에서 대시보드 요약 통계를 조회합니다.
     *
     * <p>AdminPC-Server 의 {@code GET /api/dashboard/summary} 에는 {@code locked_devices} 필드가
     * 없으므로, {@code GET /api/devices/locked} 를 추가로 호출하여
     * 잠금 장비 수를 별도로 집계합니다.</p>
     */
    public StatisticsResponse getStatistics() {
        try {
            String url = adminBaseUrl + "/api/dashboard/summary";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonObject json = JsonParser.parseString(response.getBody()).getAsJsonObject();

            // AdminPC-Server 의 /api/dashboard/summary 에는 locked_devices 가 없으므로
            // /api/devices/locked 를 추가 호출하여 잠금 장비 수 집계
            int lockedCount = 0;
            try {
                String lockedUrl = adminBaseUrl + "/api/devices/locked";
                ResponseEntity<String> lockedResponse = restTemplate.getForEntity(lockedUrl, String.class);
                com.google.gson.JsonArray lockedArray = JsonParser.parseString(lockedResponse.getBody()).getAsJsonArray();
                lockedCount = lockedArray.size();
            } catch (Exception le) {
                System.out.println("[StatisticsService] locked_devices 조회 실패 (무시): " + le.getMessage());
            }

            return StatisticsResponse.builder()
                    .totalDevices(json.get("total_devices").getAsInt())
                    .runningDevices(json.get("running_devices").getAsInt())
                    .lockedDevices(lockedCount)
                    .errorDevices(json.get("error_devices").getAsInt())
                    .totalInspections(json.get("total_inspections").getAsInt())
                    .okCount(json.get("ok_count").getAsInt())
                    .ngCount(json.get("ng_count").getAsInt())
                    .ngRate(json.get("ng_rate").getAsDouble())
                    .errorCount(json.get("error_count").getAsInt())
                    .build();

        } catch (Exception e) {
            System.out.println("[StatisticsService] 호출 실패: " + e.getMessage());
            return StatisticsResponse.builder().build();
        }
    }
}