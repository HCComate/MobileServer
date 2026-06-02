package com.semse.mobile_server.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * AdminPC-Server 와의 인증 토큰을 단일 관리하는 클라이언트입니다.
 *
 * PollingService, DeviceService, UserService 가 이 컴포넌트를 공유하여
 * 동일 계정으로 중복 로그인하는 문제(409 CONFLICT)를 방지합니다.
 */
@Component
public class AdminPcAuthClient {

    @Value("${admin.pc.base-url}")
    private String adminBaseUrl;

    @Value("${admin.pc.username}")
    private String adminUsername;

    @Value("${admin.pc.password}")
    private String adminPassword;

    private final RestTemplate restTemplate;
    private String token;

    public AdminPcAuthClient() {
        // 타임아웃 설정 (AdminPC-Server 무응답 시 블로킹 방지)
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(5_000);
        this.restTemplate = new RestTemplate(factory);
    }

    public String getBaseUrl() {
        return adminBaseUrl;
    }

    /** 유효한 토큰 반환. 없으면 자동 로그인. */
    public synchronized String getValidToken() {
        if (token == null) {
            login();
        }
        return token;
    }

    /** 토큰 만료 시 무효화 (다음 getValidToken() 호출 시 재로그인). */
    public synchronized void invalidateToken() {
        token = null;
    }

    private void login() {
        String url = adminBaseUrl + "/api/auth/login";

        JsonObject body = new JsonObject();
        body.addProperty("username", adminUsername);
        body.addProperty("password", adminPassword);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        JsonObject json = JsonParser.parseString(response.getBody()).getAsJsonObject();
        this.token = json.get("token").getAsString();
        System.out.println("[AdminPcAuthClient] AdminPC 로그인 성공");
    }
}
