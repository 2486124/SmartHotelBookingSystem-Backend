package com.cts.project.shbs.client;

import com.cts.project.shbs.dto.UserNameResponse;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserServiceClientFallbackFactory implements FallbackFactory<UserServiceClient> {

    @Override
    public UserServiceClient create(Throwable cause) {
        return new UserServiceClient() {

            @Override
            public UserNameResponse getUserName(Long userId) {
                if (cause instanceof RetryableException) {
                    log.error("[UserService] Not discoverable in Eureka for userId={} | Reason: {}",
                            userId, cause.getMessage());
                } else if (cause instanceof FeignException.NotFound) {
                    log.warn("[UserService] User not found for userId={}", userId);
                } else if (cause instanceof FeignException.ServiceUnavailable) {
                    log.error("[UserService] Service is DOWN for userId={} | Reason: {}",
                            userId, cause.getMessage());
                } else {
                    log.error("[UserService] Unexpected error resolving name for userId={} | Reason: {}",
                            userId, cause.getMessage());
                }
                // Safe fallback — shows "Guest" in the review card
                return new UserNameResponse(userId, "Guest");
            }
        };
    }
}
