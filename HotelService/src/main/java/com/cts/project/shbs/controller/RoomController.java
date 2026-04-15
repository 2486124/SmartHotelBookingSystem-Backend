package com.cts.project.shbs.controller;

import com.cts.project.shbs.dto.RoomRequest;
import com.cts.project.shbs.dto.RoomResponse;
import com.cts.project.shbs.model.Hotel;
import com.cts.project.shbs.model.Room;
import com.cts.project.shbs.service.HotelService;
import com.cts.project.shbs.service.RoomService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Room Management", description = "APIs for managing rooms within a hotel — public access and hotel manager operations")
@RestController
@RequestMapping("/api/hotels/{hotelId}/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomController {

    private final RoomService roomService;
    private final HotelService hotelService;

    // ─────────────────────────────────────────────────────────────────
    // GUEST, HOTEL_MANAGER ENDPOINTS
    // ─────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Get all rooms for a hotel",
        description = "Returns a list of all rooms belonging to the specified hotel. JWT is validated at the API gateway — role is forwarded via X-User-Role header.",
        parameters = {
            @Parameter(name = "X-User-Role", in = ParameterIn.HEADER, required = true,
                description = "Role injected by API gateway after JWT validation. Must be ROLE_GUEST or ROLE_HOTEL_MANAGER.",
                schema = @Schema(type = "string", example = "ROLE_GUEST"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of rooms returned successfully",
            content = @Content(schema = @Schema(implementation = RoomResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied — Guest or Hotel Manager role required", content = @Content),
        @ApiResponse(responseCode = "404", description = "Hotel not found", content = @Content)
    })
    @GetMapping
    public ResponseEntity<?> getRoomsByHotelId(
            @Parameter(description = "ID of the hotel", required = true, example = "1")
            @PathVariable Long hotelId,
            @RequestHeader("X-User-Role") String userRole) {
        if (!userRole.equals("ROLE_GUEST") && !userRole.equals("ROLE_HOTEL_MANAGER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied — Guest or Hotel Manager role required.");
        }
        log.info("Request received to fetch all rooms for hotel ID: {}", hotelId);
        List<RoomResponse> responses = roomService.getRoomsByHotelId(hotelId)
                .stream()
                .map(this::mapToRoomResponse)
                .collect(Collectors.toList());
        log.info("Returning {} rooms for hotel ID: {}", responses.size(), hotelId);
        return ResponseEntity.ok(responses);
    }

    @Operation(
        summary = "Get a room by ID",
        description = "Fetches details of a specific room by its ID within a hotel. JWT is validated at the API gateway — role is forwarded via X-User-Role header.",
        parameters = {
            @Parameter(name = "X-User-Role", in = ParameterIn.HEADER, required = true,
                description = "Role injected by API gateway after JWT validation. Must be ROLE_GUEST or ROLE_HOTEL_MANAGER.",
                schema = @Schema(type = "string", example = "ROLE_GUEST"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Room found and returned",
            content = @Content(schema = @Schema(implementation = RoomResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied — Guest or Hotel Manager role required", content = @Content),
        @ApiResponse(responseCode = "404", description = "Room or Hotel not found", content = @Content)
    })
    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoomById(
            @Parameter(description = "ID of the hotel", required = true, example = "1")
            @PathVariable Long hotelId,
            @Parameter(description = "ID of the room to fetch", required = true, example = "101")
            @PathVariable Long roomId,
            @RequestHeader("X-User-Role") String userRole) {
        if (!userRole.equals("ROLE_GUEST") && !userRole.equals("ROLE_HOTEL_MANAGER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied — Guest or Hotel Manager role required.");
        }
        log.info("Request received to fetch room ID: {} for hotel ID: {}", roomId, hotelId);
        Room room = roomService.getRoomById(roomId);
        if (!room.getHotel().getHotelId().equals(hotelId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Room not found in this hotel.");
        }
        log.debug("Room found - type: {}, price: {}", room.getType(), room.getPrice());
        return ResponseEntity.ok(mapToRoomResponse(room));
    }

    // ─────────────────────────────────────────────────────────────────
    // HOTEL MANAGER ENDPOINTS
    // ─────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Add a new room to a hotel",
        description = "Allows a Hotel Manager to add a new room to their hotel. JWT is validated at the API gateway — user ID and role are forwarded via X-User-Id and X-User-Role headers.",
        parameters = {
            @Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true,
                description = "Authenticated manager's ID, injected by the API gateway after JWT validation.",
                schema = @Schema(type = "integer", example = "10")),
            @Parameter(name = "X-User-Role", in = ParameterIn.HEADER, required = true,
                description = "Role injected by API gateway after JWT validation. Must be ROLE_HOTEL_MANAGER.",
                schema = @Schema(type = "string", example = "ROLE_HOTEL_MANAGER"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Room created successfully",
            content = @Content(schema = @Schema(implementation = RoomResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid room data", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied — not a Hotel Manager or not your hotel", content = @Content),
        @ApiResponse(responseCode = "404", description = "Hotel not found", content = @Content)
    })
    @PostMapping
    public ResponseEntity<?> createRoom(
            @Parameter(description = "ID of the hotel to add the room to", required = true, example = "1")
            @PathVariable Long hotelId,
            @Valid @RequestBody RoomRequest request,
            @RequestHeader("X-User-Id") Long managerId,
            @RequestHeader("X-User-Role") String userRole) {
        if (!userRole.equals("ROLE_HOTEL_MANAGER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied — Hotel Manager role required.");
        }
        Hotel hotel = hotelService.getHotelById(hotelId);
        if (!hotel.getManagerId().equals(managerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied — you do not own this hotel.");
        }
        log.info("Room creation request received for hotel ID: {} by manager ID: {}", hotelId, managerId);
        Room createdRoom = roomService.createRoom(hotelId, request);
        log.info("Room created successfully with ID: {} for hotel ID: {}", createdRoom.getRoomId(), hotelId);
        return new ResponseEntity<>(mapToRoomResponse(createdRoom), HttpStatus.CREATED);
    }

    @Operation(
        summary = "Update a room",
        description = "Allows a Hotel Manager to update details of an existing room. JWT is validated at the API gateway — user ID and role are forwarded via X-User-Id and X-User-Role headers.",
        parameters = {
            @Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true,
                description = "Authenticated manager's ID, injected by the API gateway after JWT validation.",
                schema = @Schema(type = "integer", example = "10")),
            @Parameter(name = "X-User-Role", in = ParameterIn.HEADER, required = true,
                description = "Role injected by API gateway after JWT validation. Must be ROLE_HOTEL_MANAGER.",
                schema = @Schema(type = "string", example = "ROLE_HOTEL_MANAGER"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Room updated successfully",
            content = @Content(schema = @Schema(implementation = RoomResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid room data", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied — not a Hotel Manager or not your hotel", content = @Content),
        @ApiResponse(responseCode = "404", description = "Room or Hotel not found", content = @Content)
    })
    @PutMapping("/{roomId}")
    public ResponseEntity<?> updateRoom(
            @Parameter(description = "ID of the hotel", required = true, example = "1")
            @PathVariable Long hotelId,
            @Parameter(description = "ID of the room to update", required = true, example = "101")
            @PathVariable Long roomId,
            @Valid @RequestBody RoomRequest request,
            @RequestHeader("X-User-Id") Long managerId,
            @RequestHeader("X-User-Role") String userRole) {
        if (!userRole.equals("ROLE_HOTEL_MANAGER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied — Hotel Manager role required.");
        }
        Room room = roomService.getRoomById(roomId);
        if (!room.getHotel().getHotelId().equals(hotelId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Room not found in this hotel.");
        }
        if (!room.getHotel().getManagerId().equals(managerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied — you do not own this hotel.");
        }
        log.info("Update request received for room ID: {} in hotel ID: {} by manager ID: {}", roomId, hotelId, managerId);
        Room updatedRoom = roomService.updateRoom(roomId, request);
        log.info("Room ID: {} updated successfully for hotel ID: {}", roomId, hotelId);
        return ResponseEntity.ok(mapToRoomResponse(updatedRoom));
    }

    @Operation(
        summary = "Delete a room",
        description = "Allows a Hotel Manager to delete a room from their hotel. JWT is validated at the API gateway — user ID and role are forwarded via X-User-Id and X-User-Role headers.",
        parameters = {
            @Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true,
                description = "Authenticated manager's ID, injected by the API gateway after JWT validation.",
                schema = @Schema(type = "integer", example = "10")),
            @Parameter(name = "X-User-Role", in = ParameterIn.HEADER, required = true,
                description = "Role injected by API gateway after JWT validation. Must be ROLE_HOTEL_MANAGER.",
                schema = @Schema(type = "string", example = "ROLE_HOTEL_MANAGER"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Room deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied — not a Hotel Manager or not your hotel", content = @Content),
        @ApiResponse(responseCode = "404", description = "Room or Hotel not found", content = @Content)
    })
    @DeleteMapping("/{roomId}")
    public ResponseEntity<?> deleteRoom(
            @Parameter(description = "ID of the hotel", required = true, example = "1")
            @PathVariable Long hotelId,
            @Parameter(description = "ID of the room to delete", required = true, example = "101")
            @PathVariable Long roomId,
            @RequestHeader("X-User-Id") Long managerId,
            @RequestHeader("X-User-Role") String userRole) {
        if (!userRole.equals("ROLE_HOTEL_MANAGER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied — Hotel Manager role required.");
        }
        Room room = roomService.getRoomById(roomId);
        if (!room.getHotel().getHotelId().equals(hotelId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Room not found in this hotel.");
        }
        if (!room.getHotel().getManagerId().equals(managerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied — you do not own this hotel.");
        }
        log.warn("Delete request received for room ID: {} in hotel ID: {} by manager ID: {}", roomId, hotelId, managerId);
        roomService.deleteRoom(roomId);
        log.info("Room ID: {} deleted successfully from hotel ID: {}", roomId, hotelId);
        return ResponseEntity.ok("Room deleted successfully.");
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────

    private RoomResponse mapToRoomResponse(Room room) {
        log.debug("Mapping room entity to response DTO for room ID: {}", room.getRoomId());
        RoomResponse response = new RoomResponse();
        response.setRoomId(room.getRoomId());
        response.setType(room.getType());
        response.setPrice(room.getPrice());
        response.setAvailability(room.getAvailability());
        response.setFeatures(room.getFeatures());
        response.setImageUrl(room.getImageUrl());
        return response;
    }
}