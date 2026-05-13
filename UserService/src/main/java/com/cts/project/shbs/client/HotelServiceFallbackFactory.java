package com.cts.project.shbs.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HotelServiceFallbackFactory implements FallbackFactory<HotelServiceClient> {

    @Override
    public HotelServiceClient create(Throwable cause) {
        return (managerId, role) -> log.warn(
                "HotelService unavailable when deleting hotel for manager ID: {} — " +
                "hotel and bookings may need manual cleanup. Cause: {}",
                managerId, cause.getMessage());
    }
}
