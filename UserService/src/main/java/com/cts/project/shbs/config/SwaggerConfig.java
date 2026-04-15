package com.cts.project.shbs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("User Service API")
                .description("Handles user registration, login, and password management. JWT validation is performed at the API gateway level — authenticated requests arrive with X-User-Id and X-User-Role headers pre-injected.")
                .version("v1.0"));
    }
}