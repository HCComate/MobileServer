package com.semse.mobile_server.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 주요 일정(Calendar Events) 조회 서비스.
 *
 * <p>일정은 AdminPC-Server(웹 UI)에서 작성/관리되므로 AdminPC-Server의
 * /api/events로 위임합니다. month(YYYY-MM) 또는 date(YYYY-MM-DD) 필터를 지원합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class EventService {

    private final RestTemplate restTemplate; // RestTemplateConfig 빈 (타임아웃 포함)

    @Value("${admin.pc.base-url}")
    private String adminBaseUrl;

    public List<Map<String, Object>> getEvents(String month, String date) {
        try {
            String url = adminBaseUrl + "/api/events";
            if (date != null && !date.isEmpty()) {
                url += "?date=" + date;
            } else if (month != null && !month.isEmpty()) {
                url += "?month=" + month;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", "capstone2026");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            JsonArray jsonArray = JsonParser.parseString(response.getBody()).getAsJsonArray();

            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonElement el : jsonArray) {
                if (!el.isJsonObject()) continue;
                JsonObject obj = el.getAsJsonObject();
                Map<String, Object> map = new HashMap<>();
                if (obj.has("id")) map.put("id", obj.get("id").getAsLong());
                if (obj.has("date") && !obj.get("date").isJsonNull())
                    map.put("date", obj.get("date").getAsString());
                if (obj.has("content") && !obj.get("content").isJsonNull())
                    map.put("content", obj.get("content").getAsString());
                result.add(map);
            }
            return result;
        } catch (Exception e) {
            System.out.println("[EventService] AdminPC 일정 조회 실패: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
