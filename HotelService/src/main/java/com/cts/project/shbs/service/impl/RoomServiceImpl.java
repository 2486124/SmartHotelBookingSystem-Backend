package com.cts.project.shbs.service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.cts.project.shbs.client.BookingServiceClient;
import com.cts.project.shbs.dto.BookedRoomsResponse;
import com.cts.project.shbs.dto.RoomFilterRequest;
import com.cts.project.shbs.dto.RoomRequest;
import com.cts.project.shbs.exception.HotelNotApprovedException;
import com.cts.project.shbs.exception.ResourceNotFoundException;
import com.cts.project.shbs.exception.ServiceUnavailableException;
import com.cts.project.shbs.model.Hotel;
import com.cts.project.shbs.model.Room;
import com.cts.project.shbs.repository.HotelRepository;
import com.cts.project.shbs.repository.RoomRepository;
import com.cts.project.shbs.service.RoomService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final BookingServiceClient bookingClient;
    
    private static final String BOOKING_CB = "bookingService";
    
    @Override
    public Room createRoom(Long hotelId, RoomRequest request) {
        log.info("Creating room of type '{}' for hotel ID: {}", request.getType(), hotelId);

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> {
                    log.error("Hotel not found with ID: {} while creating room", hotelId);
                    return new ResourceNotFoundException("Hotel not found with ID: " + hotelId);
                });

        // Block room creation if hotel is not yet approved
        if (!hotel.getApproval()) {
            log.warn("Attempted to add room to unapproved hotel ID: {}", hotelId);
            throw new HotelNotApprovedException(
                "Cannot add rooms to hotel ID: " + hotelId + " as it is not yet approved.");
        }

        Room room = new Room();
        room.setHotel(hotel);
        room.setType(request.getType());
        room.setPrice(request.getPrice());
        room.setAvailability(request.getAvailability() != null ? request.getAvailability() : true);
        room.setFeatures(request.getFeatures());
        room.setImageUrl(request.getImageUrl());

        Room saved = roomRepository.save(room);
        log.info("Room created successfully with ID: {} for hotel ID: {}", saved.getRoomId(), hotelId);
        return saved;
    }

    @Override
    public Room updateRoom(Long roomId, RoomRequest request) {
        log.info("Updating room ID: {}", roomId);
        Room room = getRoomById(roomId);

        room.setType(request.getType());
        room.setPrice(request.getPrice());
        if (request.getAvailability() != null) {
            log.debug("Updating availability for room ID: {} to {}", roomId, request.getAvailability());
            room.setAvailability(request.getAvailability());
        }
        room.setFeatures(request.getFeatures());
        room.setImageUrl(request.getImageUrl());

        Room updated = roomRepository.save(room);
        log.info("Room ID: {} updated successfully", roomId);
        return updated;
    }

    @Override
    public void deleteRoom(Long roomId) {
        log.warn("Delete request received for room ID: {}", roomId);
        Room room = getRoomById(roomId);
        roomRepository.delete(room);
        log.info("Room ID: {} deleted successfully", roomId);
    }

    @Override
    public Room getRoomById(Long roomId) {
        log.debug("Fetching room by ID: {}", roomId);
        return roomRepository.findById(roomId)
                .orElseThrow(() -> {
                    log.error("Room not found with ID: {}", roomId);
                    return new ResourceNotFoundException("Room not found with ID: " + roomId);
                });
    }

    @Override
    public List<Room> getRoomsByHotelId(Long hotelId) {
        log.info("Fetching all rooms for hotel ID: {}", hotelId);

        if (!hotelRepository.existsById(hotelId)) {
            log.error("Hotel not found with ID: {} while fetching rooms", hotelId);
            throw new ResourceNotFoundException("Hotel not found with ID: " + hotelId);
        }

        List<Room> rooms = roomRepository.findByHotelHotelId(hotelId);
        log.info("Found {} rooms for hotel ID: {}", rooms.size(), hotelId);
        return rooms;
    }

    @Override
    @CircuitBreaker(name = BOOKING_CB, fallbackMethod = "getAvailableRoomsFallback")
    @Retry(name = BOOKING_CB, fallbackMethod = "getAvailableRoomsFallback")
    public List<Room> getAvailableRooms(RoomFilterRequest req) {
        log.info("Fetching available rooms - hotel: {}, type: {}, checkIn: {}, checkOut: {}",
                req.getHotelId(), req.getRoomType(), req.getCheckIn(), req.getCheckOut());

        BookedRoomsResponse resp = bookingClient.getBookedRooms(
                req.getHotelId(), req.getCheckIn(), req.getCheckOut()
        );

        // factory throws BookingServiceException on failure
        List<Long> bookedIds = resp.getBookedRoomIds() != null
                ? resp.getBookedRoomIds()
                : Collections.emptyList();

        log.info("Hotel {} | type '{}' | booked room IDs: {}",
                req.getHotelId(), req.getRoomType(), bookedIds);

        if (bookedIds.isEmpty()) {
            return roomRepository.findByHotel_HotelIdAndTypeAndAvailabilityTrue(
                    req.getHotelId(), req.getRoomType()
            );
        }

        return roomRepository.findAvailableRooms(
                req.getHotelId(), req.getRoomType(), bookedIds
        );
    }

    public List<Room> getAvailableRoomsFallback(RoomFilterRequest req, Throwable ex) {
        log.warn("Booking service unavailable for hotel: {}, type: {}. Reason: {}. " +
                        "Returning all rooms as available.",
                req.getHotelId(), req.getRoomType(), ex.getMessage());

        return roomRepository.findByHotel_HotelIdAndTypeAndAvailabilityTrue(
                req.getHotelId(), req.getRoomType()
        );
    }
}