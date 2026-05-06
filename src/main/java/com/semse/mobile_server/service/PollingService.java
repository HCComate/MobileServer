package com.semse.mobile_server.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class PollingService {

    private final InspectionService inspectionService;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${admin.pc.base-url}")
    private String adminBaseUrl;

    @Value("${admin.pc.username}")
    private String username;

    @Value("${admin.pc.password}")
    private String password;

    private String token;
    private long lastId = 0;

    @Scheduled(fixedRate = 5000)
    public void pollAdminPc() {
        try {
            if (token == null) {
                login();
            }

            String url = adminBaseUrl + "/api/logs/after?last_id=" + lastId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonArray logs = JsonParser.parseString(response.getBody()).getAsJsonArray();

            for (JsonElement element : logs) {
                JsonObject logJson = element.getAsJsonObject();

                inspectionService.saveInspectionData(logJson);

                long currentId = logJson.get("id").getAsLong();
                if (currentId > lastId) {
                    lastId = currentId;
                }
            }

            if (logs.size() > 0) {
                System.out.println("Polling success. saved logs: " + logs.size() + ", lastId: " + lastId);
            }

        } catch (Exception e) {
            System.out.println("Polling failed: " + e.getMessage());
            token = null;
        }
    }

    private void login() {
        String url = adminBaseUrl + "/api/auth/login";

        JsonObject loginBody = new JsonObject();
        loginBody.addProperty("username", username);
        loginBody.addProperty("password", password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(loginBody.toString(), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                url,
                entity,
                String.class
        );

        JsonObject json = JsonParser.parseString(response.getBody()).getAsJsonObject();
        this.token = json.get("token").getAsString();

        System.out.println("Admin PC login success");
    }
}