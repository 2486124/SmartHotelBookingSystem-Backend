package com.cts.project.shbs.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.cts.project.shbs.dto.BookingNotificationDto;
import com.cts.project.shbs.dto.UserNameResponse;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/auth/internal/users/{id}/name")
    UserNameResponse getUserName(@PathVariable("id") Long userId);

    @PostMapping("/api/auth/internal/send-booking-notification")
    void sendBookingNotification(@RequestBody BookingNotificationDto request);
}
