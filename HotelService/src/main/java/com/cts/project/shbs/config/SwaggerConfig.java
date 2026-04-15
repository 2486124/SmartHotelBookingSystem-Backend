package com.cts.project.shbs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI hotelBookingOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Smart Hotel Booking System API")
                .description("REST API documentation for Hotel Service")
                .version("1.0.0"));
    }
}