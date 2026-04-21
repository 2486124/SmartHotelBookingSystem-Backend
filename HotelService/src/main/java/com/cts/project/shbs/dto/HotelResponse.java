package com.cts.project.shbs.dto;

import lombok.Data;
import java.util.List;

@Data
public class HotelResponse {
    private Long hotelId;
    private String name;
    private String location;
    private String amenities;
    private String imageUrl;
    private Double rating;
    private Boolean approval;
}
