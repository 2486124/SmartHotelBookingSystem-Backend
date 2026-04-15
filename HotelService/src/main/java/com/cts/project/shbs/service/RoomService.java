package com.cts.project.shbs.service;

import com.cts.project.shbs.dto.RoomFilterRequest;
import com.cts.project.shbs.dto.RoomRequest;
import com.cts.project.shbs.model.Room;
import java.util.List;

public interface RoomService {
    Room createRoom(Long hotelId, RoomRequest request);
    Room updateRoom(Long roomId, RoomRequest request);
    void deleteRoom(Long roomId);
    Room getRoomById(Long roomId);
    List<Room> getRoomsByHotelId(Long hotelId);
    List<Room> getAvailableRooms(RoomFilterRequest req);
}
