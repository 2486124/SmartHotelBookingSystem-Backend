package com.cts.project.shbs.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class BookedRoomsResponse {
    private LocalDate   checkIn;
    private LocalDate   checkOut;
    private Long     hotelId;
    private List<Long> bookedRoomIds;
}