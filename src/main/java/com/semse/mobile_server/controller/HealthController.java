package com.semse.mobile_server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "OK",
                "server", "mobile-server",
                "message", "서버가 정상 작동 중입니다"
        );
    }
}
