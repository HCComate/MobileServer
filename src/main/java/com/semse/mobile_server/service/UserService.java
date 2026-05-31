package com.semse.mobile_server.service;

import com.semse.mobile_server.dto.UserResponse;
import com.semse.mobile_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.semse.mobile_server.entity.User;
import java.util.ArrayList;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${admin.pc.base-url}")
    private String adminBaseUrl;

    public List<UserResponse> getAllUsers() {
        try {
            String url = adminBaseUrl + "/api/users";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", "capstone2026");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonArray jsonArray = JsonParser.parseString(response.getBody()).getAsJsonArray();
            
            List<UserResponse> users = new ArrayList<>();
            for (JsonElement element : jsonArray) {
                JsonObject obj = element.getAsJsonObject();
                boolean isOnline = obj.has("is_online") && obj.get("is_online").getAsBoolean();
                
                users.add(UserResponse.builder()
                        .userId(obj.get("username").getAsString())
                        .name(obj.get("nickname").getAsString())
                        .role(obj.get("role").getAsString().toUpperCase())
                        .shiftStatus(isOnline ? "ON_DUTY" : "OFF_DUTY")
                        .workStatus("IDLE")
                        .assignedDevices(new ArrayList<>())
                        .build());
            }
            return users;
        } catch (Exception e) {
            System.out.println("AdminPC 유저 목록 동기화 실패: " + e.getMessage());
            // 실패 시 로컬 DB 반환 (Fallback)
            return userRepository.findAll().stream()
                    .map(UserResponse::from)
                    .collect(Collectors.toList());
        }
    }
    public void updateRole(String userId, String role) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
        user.setRole(role);
        userRepository.save(user);
    }
}