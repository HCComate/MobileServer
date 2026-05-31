package com.semse.mobile_server.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private UserInfo user;

    @Getter
    @AllArgsConstructor
    public static class UserInfo {
        private String userId;   // MobileServer DB의 userId (앱 currentUserId와 매칭용)
        private String username;
        private String role;
    }
}