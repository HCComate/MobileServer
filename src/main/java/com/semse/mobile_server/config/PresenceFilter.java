package com.semse.mobile_server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Base64;

@Component
public class PresenceFilter extends OncePerRequestFilter {

    // username -> user object
    public static final ConcurrentHashMap<String, Map<String, Object>> activeUsers = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // 단순 Base64 디코딩으로 Payload 확인 (AdminPC Server의 토큰 구조 파싱)
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> payload = mapper.readValue(payloadJson, Map.class);
                    
                    String username = (String) payload.get("username");
                    if (username != null) {
                        Map<String, Object> userInfo = new java.util.HashMap<>();
                        userInfo.put("username", username);
                        userInfo.put("user_id", payload.get("user_id"));
                        userInfo.put("role", payload.get("role"));
                        userInfo.put("last_seen", System.currentTimeMillis());
                        
                        activeUsers.put(username, userInfo);
                    }
                }
            } catch (Exception e) {
                // 토큰 파싱 실패 무시
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
