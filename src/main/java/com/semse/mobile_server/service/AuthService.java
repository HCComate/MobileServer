package com.semse.mobile_server.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.semse.mobile_server.config.AdminPcException;
import com.semse.mobile_server.config.JwtUtil;
import com.semse.mobile_server.dto.LoginRequest;
import com.semse.mobile_server.dto.LoginResponse;
import com.semse.mobile_server.entity.User;
import com.semse.mobile_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${admin.pc.base-url}")
    private String adminBaseUrl;

    public LoginResponse login(LoginRequest request) {
        // 1차: AdminPC-Server 프록시 인증
        try {
            String url = adminBaseUrl + "/api/auth/login";

            JsonObject body = new JsonObject();
            body.addProperty("username", request.getUsername());
            body.addProperty("password", request.getPassword());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            JsonObject json = JsonParser.parseString(response.getBody()).getAsJsonObject();
            JsonObject user = json.getAsJsonObject("user");

            String token = json.get("token").getAsString();
            String username = user.get("username").getAsString();
            String role = user.get("role").getAsString().toUpperCase();

            String mobileUserId = userRepository.findByUserId(username)
                    .map(User::getUserId)
                    .orElse(username);

            System.out.println("[AuthService] AdminPC-Server 인증 성공: " + username);
            return new LoginResponse(token, new LoginResponse.UserInfo(mobileUserId, username, role));

        } catch (HttpClientErrorException e) {
            // AdminPC가 4xx 반환 (잘못된 ID/PW 등) → 그대로 전달
            String errorMsg = e.getMessage();
            try {
                JsonObject errJson = JsonParser.parseString(e.getResponseBodyAsString()).getAsJsonObject();
                if (errJson.has("error")) errorMsg = errJson.get("error").getAsString();
            } catch (Exception ignored) {}
            throw new AdminPcException(e.getStatusCode().value(), errorMsg);

        } catch (Exception adminPcDown) {
            // 2차: AdminPC-Server 연결 실패 → MobileServer 자체 DB로 폴백 인증
            System.out.println("[AuthService] AdminPC-Server 연결 실패, 로컬 인증으로 전환: "
                    + adminPcDown.getMessage());
            return loginLocal(request);
        }
    }

    private LoginResponse loginLocal(LoginRequest request) {
        User user = userRepository.findByUserId(request.getUsername())
                .orElseThrow(() -> new AdminPcException(401, "아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AdminPcException(401, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = jwtUtil.generateToken(user.getUserId(), user.getUserId(), user.getRole());
        System.out.println("[AuthService] 로컬 인증 성공: " + user.getUserId());
        return new LoginResponse(token,
                new LoginResponse.UserInfo(user.getUserId(), user.getUserId(), user.getRole()));
    }
}
