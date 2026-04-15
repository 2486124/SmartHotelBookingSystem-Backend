package com.cts.project.shbs.repository;

import com.cts.project.shbs.model.Room;

import feign.Param;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByHotelHotelId(Long hotelId);
    
    @Query("""
            SELECT r FROM Room r
            WHERE r.hotel.hotelId = :hotelId
              AND r.type           = :type
              AND r.availability   = true
              AND r.roomId NOT IN  :bookedRoomIds
            """)
        List<Room> findAvailableRooms(
            @Param("hotelId")       Long hotelId,
            @Param("type")          String  type,
            @Param("bookedRoomIds") List<Long> bookedRoomIds
        );

        // Fallback — no rooms are booked at all for this hotel+type
        List<Room> findByHotel_HotelIdAndTypeAndAvailabilityTrue(
            Long hotelId, String type
        );
}
