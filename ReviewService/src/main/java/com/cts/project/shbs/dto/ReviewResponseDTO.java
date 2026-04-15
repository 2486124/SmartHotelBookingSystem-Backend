package com.cts.project.shbs.dto;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponseDTO {
    private Long reviewId;
    private Long userId;
    private Long hotelId;
    private Integer rating;
    private String comment;
    private LocalDateTime timestamp;
    private String managerResponse;
    private LocalDateTime responseTimestamp;
}
