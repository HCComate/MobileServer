package com.semse.mobile_server.config;

import com.semse.mobile_server.entity.User;
import com.semse.mobile_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.save(User.builder()
                    .userId("admin")
                    .password("1234")
                    .name("관리자")
                    .role("ADMIN")
                    .build());

            userRepository.save(User.builder()
                    .userId("user01")
                    .password("1234")
                    .name("김가현")
                    .role("USER")
                    .build());

            System.out.println("=== 테스트 사용자 2명 생성 완료 ===");
        }
    }
}
