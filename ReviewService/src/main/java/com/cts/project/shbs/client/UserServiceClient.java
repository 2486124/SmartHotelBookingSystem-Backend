package com.cts.project.shbs.client;

import com.cts.project.shbs.dto.UserNameResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", fallbackFactory = UserServiceClientFallbackFactory.class)
public interface UserServiceClient {

    @GetMapping("/api/auth/internal/users/{id}/name")
    UserNameResponse getUserName(@PathVariable("id") Long userId);
}
