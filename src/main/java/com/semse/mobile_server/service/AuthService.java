package com.semse.mobile_server.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.semse.mobile_server.dto.LoginRequest;
import com.semse.mobile_server.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class AuthService {

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
            Long userId = user.get("id").getAsLong();
            String username = user.get("username").getAsString();
            String role = user.get("role").getAsString().toUpperCase();

            return new LoginResponse(token, new LoginResponse.UserInfo(userId, username, role));

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new RuntimeException("아이디 또는 비밀번호가 틀렸습니다.");
            }
            throw new RuntimeException("로그인 실패: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("재민이 서버 연결 실패: " + e.getMessage());
        }
    }
}
