package com.cts.project.shbs.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class HotelResponse {
    private Long hotelId;
    private String name;
    private String location;
    private String amenities;
    private String imageUrl;
    private Double rating;
    private Boolean approval;
    // We include the rooms here so when a user views a hotel, they see the rooms available!
//    private List<RoomResponse> rooms; 
}
