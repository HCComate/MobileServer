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

@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${admin.pc.base-url}")
    private String adminBaseUrl;

    public List<Map<String, Object>> getSchedules(String date) {
        try {
            String url = adminBaseUrl + "/api/schedules?date=" + date;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", "capstone2026");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonArray jsonArray = JsonParser.parseString(response.getBody()).getAsJsonArray();
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonElement el : jsonArray) {
                if (!el.isJsonObject()) continue;
                JsonObject obj = el.getAsJsonObject();
                Map<String, Object> map = new HashMap<>();
                if (obj.has("id")) map.put("id", obj.get("id").getAsLong());
                if (obj.has("username") && !obj.get("username").isJsonNull()) map.put("username", obj.get("username").getAsString());
                if (obj.has("nickname") && !obj.get("nickname").isJsonNull()) map.put("nickname", obj.get("nickname").getAsString());
                if (obj.has("emp_id") && !obj.get("emp_id").isJsonNull()) map.put("emp_id", obj.get("emp_id").getAsString());
                if (obj.has("role") && !obj.get("role").isJsonNull()) map.put("role", obj.get("role").getAsString());
                result.add(map);
            }
            return result;
        } catch (Exception e) {
            System.out.println("[ScheduleService] Failed to fetch schedules: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
