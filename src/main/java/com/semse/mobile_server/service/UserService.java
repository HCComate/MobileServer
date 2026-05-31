package com.semse.mobile_server.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.semse.mobile_server.dto.UserResponse;
import com.semse.mobile_server.entity.User;
import com.semse.mobile_server.repository.UserRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AdminPcAuthClient adminPcAuthClient;
    private final RestTemplate restTemplate = new RestTemplate();

    public UserService(UserRepository userRepository, AdminPcAuthClient adminPcAuthClient) {
        this.userRepository = userRepository;
        this.adminPcAuthClient = adminPcAuthClient;
    }

    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        Set<String> onlineUsernames = fetchOnlineUsernames();

        return users.stream()
                .map(user -> UserResponse.builder()
                        .userId(user.getUserId())
                        .name(user.getName())
                        .role(user.getRole())
                        .shiftStatus(onlineUsernames.contains(user.getUserId()) ? "ON_DUTY" : "OFF_DUTY")
                        .workStatus("IDLE")
                        .assignedDevices(user.getAssignedDevices() != null ? user.getAssignedDevices() : List.of())
                        .build())
                .collect(Collectors.toList());
    }

    public void updateRole(String userId, String role) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
        user.setRole(role);
        userRepository.save(user);
    }

    private Set<String> fetchOnlineUsernames() {
        try {
            return callWorkersStatusApi();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 401 토큰 만료 시에만 무효화 후 1회 재시도
            if (e.getStatusCode() == org.springframework.http.HttpStatus.UNAUTHORIZED) {
                adminPcAuthClient.invalidateToken();
                try {
                    return callWorkersStatusApi();
                } catch (Exception retryEx) {
                    System.out.println("[UserService] AdminPC 온라인 상태 조회 실패: " + retryEx.getMessage());
                    return Set.of();
                }
            }
            System.out.println("[UserService] AdminPC 온라인 상태 조회 실패: " + e.getMessage());
            return Set.of();
        } catch (Exception e) {
            System.out.println("[UserService] AdminPC 온라인 상태 조회 실패: " + e.getMessage());
            return Set.of();
        }
    }

    private Set<String> callWorkersStatusApi() {
        String url = adminPcAuthClient.getBaseUrl() + "/api/workers/status";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminPcAuthClient.getValidToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        JsonArray array = JsonParser.parseString(response.getBody()).getAsJsonArray();

        Set<String> onlineUsernames = new HashSet<>();
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            boolean isOnline = obj.has("is_online") && obj.get("is_online").getAsBoolean();
            if (isOnline && obj.has("username")) {
                onlineUsernames.add(obj.get("username").getAsString());
            }
        }
        return onlineUsernames;
    }
}
