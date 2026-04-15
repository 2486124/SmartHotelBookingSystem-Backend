package com.cts.project.shbs.client;
import feign.RetryableException;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

import com.cts.project.shbs.dto.HotelResponse;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class HotelServiceClientFallbackFactory implements FallbackFactory<HotelServiceClient> {

    @Override
    public HotelServiceClient create(Throwable cause) {

        return new HotelServiceClient() {

            @Override
            public ResponseEntity<Void> updateHotelRating(Long hotelId, Double rating) {
            	if (cause instanceof RetryableException) {
            	    log.error("[HotelService] Service not discoverable in Eureka " +
            	              "for hotelId={} | Reason: {}", hotelId, cause.getMessage());
            	    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            	}
                if (cause instanceof FeignException.ServiceUnavailable) {
                    log.error("[HotelService] Service is DOWN while updating rating " +
                              "for hotelId={}, rating={} | Reason: {}", hotelId, rating, cause.getMessage());
                    // 503 - let the caller know service is unavailable
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
                }

                if (cause instanceof FeignException.GatewayTimeout) {
                    log.error("[HotelService] Timeout while updating rating " +
                              "for hotelId={} | Reason: {}", hotelId, cause.getMessage());
                    // 504 - timeout specific response
                    return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).build();
                }

                // Generic fallback
                log.error("[HotelService] Failed to update rating for hotelId={} | Reason: {}", 
                           hotelId, cause.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            @Override
            public HotelResponse getHotelByManagerId(Long managerId, String role) {
            	if (cause instanceof RetryableException) {
            	    log.error("[HotelService] Service not discoverable in Eureka " +
            	              "for managerId={} | Reason: {}", managerId, cause.getMessage());
            	    return HotelResponse.builder()
            	            .hotelId(0L)
            	            .name("Unavailable")
            	            .location("Unavailable")
            	            .amenities("Unavailable")
            	            .imageUrl("Unavailable")
            	            .rating(null)
            	            .approval(null)
            	            .build();
            	}

                if (cause instanceof FeignException.ServiceUnavailable) {
                    log.error("[HotelService] Service is DOWN while fetching hotel " +
                              "for managerId={} | Reason: {}", managerId, cause.getMessage());

                    return HotelResponse.builder()
                        	.hotelId(0L)
                            .name("Unavailable")
                            .location("Un Available")
                            .amenities("Un Available")
                            .imageUrl("Un Available")
                            .rating(null)
                            .approval(null)
                            .build();
                }

                if (cause instanceof FeignException.NotFound) {
                    log.warn("[HotelService] No hotel found for managerId={}", managerId);
                    return HotelResponse.builder()
                        	.hotelId(0L)
                            .name("Not Found")
                            .location("Not Found")
                            .amenities("No Found")
                            .imageUrl("No Found")
                            .rating(null)
                            .approval(null)
                            .build();
                }

                if (cause instanceof FeignException.Unauthorized) {
                    log.warn("[HotelService] Unauthorized access for managerId={}, role={}", 
                              managerId, role);
                    return HotelResponse.builder()
                        	.hotelId(0L)
                            .name("UnAuthorized")
                            .location("UnAuthorized")
                            .amenities("UnAuthorized")
                            .imageUrl("UnAuthorized")
                            .rating(null)
                            .approval(null)
                            .build();
                }

                // Generic fallback
                log.error("[HotelService] Unexpected error fetching hotel for managerId={} | Reason: {}", 
                           managerId, cause.getMessage());
                return HotelResponse.builder()
                    	.hotelId(0L)
                        .name("Error")
                        .location("Error")
                        .amenities("Error")
                        .imageUrl("Error")
                        .rating(null)
                        .approval(null)
                        .build();
            }
        };
    }
}
