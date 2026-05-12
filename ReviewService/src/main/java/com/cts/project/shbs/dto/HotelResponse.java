package com.cts.project.shbs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
