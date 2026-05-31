package com.semse.mobile_server.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.semse.mobile_server.dto.LoginRequest;
import com.semse.mobile_server.dto.LoginResponse;
import com.semse.mobile_server.config.AdminPcException;
import com.semse.mobile_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${admin.pc.base-url}")
    private String adminBaseUrl;

    public LoginResponse login(LoginRequest request) {
        try {
            String url = adminBaseUrl + "/api/auth/login";

            JsonObject body = new JsonObject();
            body.addProperty("username", request.getUsername());
            body.addProperty("password", request.getPassword());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    url, entity, String.class);

            JsonObject json = JsonParser.parseString(response.getBody()).getAsJsonObject();
            JsonObject user = json.getAsJsonObject("user");

            String token = json.get("token").getAsString();
            String username = user.get("username").getAsString();
            String role = user.get("role").getAsString().toUpperCase();

            // AdminPC username으로 MobileServer DB의 userId 조회 (없으면 username 그대로 사용)
            String mobileUserId = userRepository.findByUserId(username)
                    .map(u -> u.getUserId())
                    .orElse(username);

            return new LoginResponse(token, new LoginResponse.UserInfo(mobileUserId, username, role));

        } catch (HttpClientErrorException e) {
            // AdminPC의 에러 응답에서 message 추출 후 동일 상태코드로 재전달
            String errorMsg = e.getMessage();
            try {
                JsonObject errJson = JsonParser.parseString(e.getResponseBodyAsString()).getAsJsonObject();
                if (errJson.has("error")) errorMsg = errJson.get("error").getAsString();
            } catch (Exception ignored) {}
            throw new AdminPcException(e.getStatusCode().value(), errorMsg);
        } catch (Exception e) {
            throw new AdminPcException(503, "AdminPC 서버 연결 실패: " + e.getMessage());
        }
    }
}
