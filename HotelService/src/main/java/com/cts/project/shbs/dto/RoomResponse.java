package com.cts.project.shbs.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RoomResponse {
    private Long roomId;
    private String type;
    private BigDecimal price;
    private Boolean availability;
    private String features;
    private String imageUrl;
}
