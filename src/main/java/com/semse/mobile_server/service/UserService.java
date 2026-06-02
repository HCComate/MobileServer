package com.semse.mobile_server.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.semse.mobile_server.dto.UserResponse;
import com.semse.mobile_server.entity.User;
import com.semse.mobile_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AdminPcAuthClient adminPcAuthClient;
    private final RestTemplate restTemplate; // RestTemplateConfig 빈 주입 (타임아웃 포함)

    @Value("${admin.pc.base-url}")
    private String adminBaseUrl;

    /**
     * 전체 사용자 목록을 반환합니다.
     * 
     * <p>AdminPC-Server에서 사용자 정보를 먼저 조회하고,
     * 실패 시 로컬 DB에서 폴백으로 반환합니다.
     * 온라인 상태는 AdminPC-Server의 workers/status에서 조회합니다.</p>
     */
    public List<UserResponse> getAllUsers() {
        try {
            // AdminPC-Server에서 사용자 정보 조회
            String url = adminBaseUrl + "/api/users";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", "capstone2026");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonArray jsonArray = JsonParser.parseString(response.getBody()).getAsJsonArray();
            
            // 온라인 상태 정보 조회
            Set<String> onlineUsernames = fetchOnlineUsernames();
            
            List<UserResponse> users = new ArrayList<>();
            for (JsonElement element : jsonArray) {
                JsonObject obj = element.getAsJsonObject();
                String username = obj.get("username").getAsString();
                boolean isOnline = onlineUsernames.contains(username);
                
                users.add(UserResponse.builder()
                        .userId(username)
                        .name(obj.get("nickname").getAsString())
                        .role(obj.get("role").getAsString().toUpperCase())
                        .shiftStatus(isOnline ? "ON_DUTY" : "OFF_DUTY")
                        .workStatus("IDLE")
                        .assignedDevices(new ArrayList<>())
                        .build());
            }
            return users;
        } catch (Exception e) {
            System.out.println("[UserService] AdminPC 유저 목록 조회 실패: " + e.getMessage());
            // Fallback: 로컬 DB 반환
            return userRepository.findAll().stream()
                    .map(UserResponse::from)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 사용자 권한을 업데이트합니다.
     */
    public void updateRole(String userId, String role) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
        user.setRole(role);
        userRepository.save(user);
    }

    /**
     * AdminPC-Server에서 온라인 작업자 목록을 조회합니다.
     */
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
                    System.out.println("[UserService] AdminPC 온라인 상태 조회 실패 (재시도): " + retryEx.getMessage());
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

    /**
     * AdminPC-Server의 workers/status API를 호출합니다.
     */
    private Set<String> callWorkersStatusApi() {
        String url = adminBaseUrl + "/api/workers/status";
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
