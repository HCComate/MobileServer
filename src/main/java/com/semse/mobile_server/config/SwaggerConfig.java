package com.semse.mobile_server.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("VisionMate Mobile Server API")
                        .description("SEMSE 반도체 장비 모니터링 백엔드 API 문서")
                        .version("v2.5"));
    }
}