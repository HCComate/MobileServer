package com.semse.mobile_server.service;

import com.semse.mobile_server.config.JwtUtil;
import com.semse.mobile_server.dto.LoginRequest;
import com.semse.mobile_server.dto.LoginResponse;
import com.semse.mobile_server.entity.User;
import com.semse.mobile_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        // 1. userId로 사용자 조회
        User user = userRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 2. 비밀번호 확인 (지금은 평문 비교, 나중에 암호화)
        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        // 3. JWT 토큰 생성
        String token = jwtUtil.generateToken(user.getUserId(), user.getRole());

        // 4. 응답 반환
        return new LoginResponse(token, user.getUserId(), user.getName(), user.getRole());
    }
}
