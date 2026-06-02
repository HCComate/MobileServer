package com.semse.mobile_server.config;

import com.semse.mobile_server.entity.User;
import com.semse.mobile_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            // AdminPC-Server와 동일한 계정 (폴백 로그인용)
            save("admin",    "admin1234", "관리자",  "MASTER");
            save("hansung1", "1234",      "한성",    "TECHNICIAN");
            save("hansung2", "1234",      "홍길동",  "TECHNICIAN");
            save("hansung3", "1234",      "김철수",  "OPERATOR");
            save("hansung4", "1234",      "박한수",  "OPERATOR");
            save("hansung5", "1234",      "최서울",  "OPERATOR");
            save("hansung6", "1234",      "이영희",  "TECHNICIAN");
            save("hansung7", "1234",      "김민준",  "OPERATOR");
            save("user01",   "1234",      "김가현",  "TECHNICIAN");
            System.out.println("=== MobileServer 기본 사용자 생성 완료 ===");
        }
    }

    private void save(String userId, String password, String name, String role) {
        userRepository.save(User.builder()
                .userId(userId)
                .password(passwordEncoder.encode(password))
                .name(name)
                .role(role)
                .build());
    }
}
