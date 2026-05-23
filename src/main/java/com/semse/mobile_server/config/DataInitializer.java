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
            userRepository.save(User.builder()
                    .userId("admin")
                    .password(passwordEncoder.encode("admin1234"))
                    .name("관리자")
                    .role("MASTER")
                    .build());

            userRepository.save(User.builder()
                    .userId("tech1")
                    .password(passwordEncoder.encode("tech1234"))
                    .name("엔지니어1")
                    .role("TECHNICIAN")
                    .build());

            userRepository.save(User.builder()
                    .userId("operator1")
                    .password(passwordEncoder.encode("oper1234"))
                    .name("작업자1")
                    .role("OPERATOR")
                    .build());

            System.out.println("=== 기본 사용자 3명 생성 완료 ===");
        }
    }
}