package com.cts.project.shbs.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Booking Service API")
                        .version("1.0")
                        .description(
                            "APIs for managing hotel bookings and payments. " +
                            "Authentication is handled by the API Gateway. " +
                            "Pass X-User-Id and X-User-Role headers for role-based access."
                        )
                );
    }

    @Bean
    public OperationCustomizer globalHeaderCustomizer() {
        return (operation, handlerMethod) -> {

            operation.addParametersItem(
                new Parameter()
                    .in("header")
                    .name("X-User-Id")
                    .description("ID of the requesting user (set by API Gateway)")
                    .required(false)
                    .schema(new StringSchema().example("1"))
            );

            operation.addParametersItem(
                new Parameter()
                    .in("header")
                    .name("X-User-Role")
                    .description("Role of the requesting user: ROLE_GUEST, ROLE_HOTEL_MANAGER, ROLE_ADMIN")
                    .required(false)
                    .schema(new StringSchema()
                        ._enum(java.util.List.of("ROLE_GUEST", "ROLE_HOTEL_MANAGER", "ROLE_ADMIN"))
                        .example("ROLE_GUEST"))
            );

            return operation;
        };
    }
}