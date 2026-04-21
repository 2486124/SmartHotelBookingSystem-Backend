package com.cts.project.shbs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.cts.project.shbs.dto.HotelRequest;
import com.cts.project.shbs.dto.HotelResponse;
import com.cts.project.shbs.dto.RoomFilterRequest;
import com.cts.project.shbs.model.Hotel;
import com.cts.project.shbs.model.Room;
import com.cts.project.shbs.service.HotelService;
import com.cts.project.shbs.service.RoomService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Hotel Management", description = "APIs for managing hotels — public access, hotel manager, and admin operations")
@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
@Slf4j
public class HotelController {

	private final HotelService hotelService;
	private final RoomService roomService;

	// ─────────────────────────────────────────────────────────────────
	// PUBLIC ENDPOINTS
	// ─────────────────────────────────────────────────────────────────

	@Operation(summary = "Get all approved hotels", description = "Returns a list of all hotels that have been approved by admin. Accessible publicly at /api/hotels/approved-hotels")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "List of approved hotels returned successfully",
					content = @Content(schema = @Schema(implementation = HotelResponse.class))) })
	@GetMapping("/approved-hotels")
	public ResponseEntity<List<HotelResponse>> getAllApprovedHotels() {
		log.info("Request received to fetch all approved hotels");
		List<HotelResponse> responses = hotelService.getAllApprovedHotels().stream().map(this::mapToHotelResponse)
				.collect(Collectors.toList());
		log.info("Returning {} approved hotels", responses.size());
		return ResponseEntity.ok(responses);
	}

	// ─────────────────────────────────────────────────────────────────
	// GUEST ENDPOINTS
	// ─────────────────────────────────────────────────────────────────

	@Operation(
		summary = "Get hotel by ID",
		description = "Fetches a single approved hotel by its ID. JWT is validated at the API gateway — role is forwarded via X-User-Role header.",
		parameters = {
			@Parameter(name = "X-User-Role", in = ParameterIn.HEADER, required = true,
				description = "Role injected by API gateway after JWT validation. Must be ROLE_GUEST.",
				schema = @Schema(type = "string", example = "ROLE_GUEST"))
		}
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Hotel found and returned",
					content = @Content(schema = @Schema(implementation = HotelResponse.class))),
			@ApiResponse(responseCode = "403", description = "Access denied — Guest role required", content = @Content),
			@ApiResponse(responseCode = "404", description = "Hotel not found", content = @Content) })
	@GetMapping("/{id}")
	public ResponseEntity<?> getHotelById(
			@Parameter(description = "ID of the hotel to fetch", required = true, example = "1") @PathVariable Long id,
			@RequestHeader("X-User-Role") String userRole) {
		if (!userRole.equals("ROLE_GUEST")) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied — Guest role required.");
		}
		log.info("Request received to fetch hotel with ID: {}", id);
		Hotel hotel = hotelService.getHotelById(id);
		log.debug("Hotel found: {}", hotel.getName());
		return ResponseEntity.ok(mapToHotelResponse(hotel));
	}

	@Operation(
		summary = "Search approved hotels",
		description = "Search approved hotels by name and/or location. JWT is validated at the API gateway — role is forwarded via X-User-Role header.",
		parameters = {
			@Parameter(name = "X-User-Role", in = ParameterIn.HEADER, required = true,
				description = "Role injected by API gateway after JWT validation. Must be ROLE_GUEST.",
				schema = @Schema(type = "string", example = "ROLE_GUEST"))
		}
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Matching hotels returned",
					content = @Content(schema = @Schema(implementation = HotelResponse.class))),
			@ApiResponse(responseCode = "403", description = "Access denied — Guest role required", content = @Content) })
	@GetMapping("/search")
	public ResponseEntity<?> searchHotels(
			@Parameter(description = "Hotel name to search for", example = "Palace") @RequestParam(required = false) String name,
			@Parameter(description = "Location to filter by", example = "Chennai") @RequestParam(required = false) String location,
			@RequestHeader("X-User-Role") String userRole) {
		if (!userRole.equals("ROLE_GUEST")) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied — Guest role required.");
		}
		log.info("Public hotel search request - name: {}, location: {}", name, location);
		List<HotelResponse> responses = hotelService.searchApprovedHotels(name, location).stream()
				.map(this::mapToHotelResponse).collect(Collectors.toList());
		log.info("Search returned {} results", responses.size());
		return ResponseEntity.ok(responses);
	}

	@Operation(
		summary = "Get available rooms",
		description = "Fetch available rooms filtered by hotel, room type, check-in and check-out dates. JWT is validated at the API gateway — role is forwarded via X-User-Role header.",
		parameters = {
			@Parameter(name = "X-User-Role", in = ParameterIn.HEADER, required = true,
				description = "Role injected by API gateway after JWT validation. Must be ROLE_GUEST.",
				schema = @Schema(type = "string", example = "ROLE_GUEST"))
		}
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Available rooms returned",
					content = @Content(schema = @Schema(implementation = Room.class))),
			@ApiResponse(responseCode = "400", description = "Invalid filter parameters", content = @Content),
			@ApiResponse(responseCode = "403", description = "Access denied — Guest role required", content = @Content) })
	@GetMapping("/rooms/available")
	public ResponseEntity<?> getAvailableRooms(
			@Parameter(description = "Room filter criteria: hotelId, roomType, checkIn, checkOut") @Valid RoomFilterRequest req,
			@RequestHeader("X-User-Role") String userRole) {
		if (!userRole.equals("ROLE_GUEST")) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied — Guest role required.");
		}
		log.info("Fetching available rooms - hotelId: {}, roomType: {}, checkIn: {}, checkOut: {}", req.getHotelId(),
				req.getRoomType(), req.getCheckIn(), req.getCheckOut());
		return ResponseEntity.ok(roomService.getAvailableRooms(req));
	}

	// ─────────────────────────────────────────────────────────────────
	// HOTEL MANAGER ENDPOINTS
	// ─────────────────────────────────────────────────────────────────

	@Operation(
		summary = "Get hotel by Manager ID",
		description = "Fetches the hotel associated with the given manager ID. JWT is validated at the API gateway — user ID and role are forwarded via X-User-Id and X-User-Role headers.",
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
			@ApiResponse(responseCode = "200", description = "Hotel found and returned",
					content = @Content(schema = @Schema(implementation = HotelResponse.class))),
			@ApiResponse(responseCode = "403", description = "Access denied — Hotel Manager role required", content = @Content),
			@ApiResponse(responseCode = "404", description = "No hotel found for this manager", content = @Content) })
	@GetMapping("/manager")
	public ResponseEntity<?> getHotelByManagerId(
			@RequestHeader("X-User-Id") Long id,
			@RequestHeader("X-User-Role") String userRole) {
		if (!userRole.equals("ROLE_HOTEL_MANAGER")) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied — Hotel Manager role required.");
		}
		log.info("Request received to fetch hotel for manager ID: {}", id);
		Hotel hotel = hotelService.getHotelByManagerId(id);
		log.debug("Hotel found for manager ID: {}", id);
		return ResponseEntity.ok(mapToHotelResponse(hotel));
	}

	@Operation(
		summary = "Create a new hotel",
		description = "Allows a Hotel Manager to register a new hotel. JWT is validated at the API gateway — user ID and role are forwarded via X-User-Id and X-User-Role headers.",
		parameters = {
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true,
				description = "Authenticated manager's ID, injected by the API gateway after JWT validation.",
				schema = @Schema(type = "string", example = "10")),
			@Parameter(name = "X-User-Role", in = ParameterIn.HEADER, required = true,
				description = "Role injected by API gateway after JWT validation. Must be ROLE_HOTEL_MANAGER.",
				schema = @Schema(type = "string", example = "ROLE_HOTEL_MANAGER"))
		}
	)
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Hotel created successfully",
					content = @Content(schema = @Schema(implementation = HotelResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid hotel data", content = @Content),
			@ApiResponse(responseCode = "403", description = "Access denied — Hotel Manager role required", content = @Content) })
	@PostMapping
	public ResponseEntity<?> createHotel(@Valid @RequestBody HotelRequest request,
			@RequestHeader("X-User-Id") String userIdHeader, @RequestHeader("X-User-Role") String userRole) {
		if (!userRole.equals("ROLE_HOTEL_MANAGER")) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied — Hotel Manager role required.");
		}
		Long managerId = Long.parseLong(userIdHeader);
		log.info("Hotel creation request received from manager ID: {}", managerId);
		Hotel createdHotel = hotelService.createHotel(request, managerId);
		log.info("Hotel created successfully with ID: {}", createdHotel.getHotelId());
		return new ResponseEntity<>(mapToHotelResponse(createdHotel), HttpStatus.CREATED);
	}

	@Operation(
		summary = "Update hotel details",
		description = "Allows a Hotel Manager to update their hotel's information. JWT is validated at the API gateway — user ID and role are forwarded via X-User-Id and X-User-Role headers.",
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
			@ApiResponse(responseCode = "200", description = "Hotel updated successfully",
					content = @Content(schema = @Schema(implementation = HotelResponse.class))),
			@ApiResponse(responseCode = "404", description = "Hotel not found", content = @Content),
			@ApiResponse(responseCode = "403", description = "Access denied", content = @Content) })

    @PutMapping("/{id}")
	public ResponseEntity<?> updateHotel(
			@Parameter(description = "ID of the hotel to update", required = true, example = "1") @PathVariable Long id,
			@Valid @RequestBody HotelRequest request,
			@RequestHeader("X-User-Id") Long managerId,
			@RequestHeader("X-User-Role") String userRole) {
		if (!userRole.equals("ROLE_HOTEL_MANAGER")) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied — Hotel Manager role required.");
		}
		Hotel existing = hotelService.getHotelById(id);
	    if (!existing.getManagerId().equals(managerId)) {
	        return ResponseEntity.status(HttpStatus.FORBIDDEN)
	                .body("Access denied — you do not own this hotel");
	    }
		log.info("Update request received for hotel ID: {}", id);
		Hotel updatedHotel = hotelService.updateHotel(id, request);
		log.info("Hotel with ID: {} updated successfully", id);
		return ResponseEntity.ok(mapToHotelResponse(updatedHotel));
	}

	@Operation(
		summary = "Delete own hotel listing",
		description = "Allows a Hotel Manager to delete their own hotel listing. JWT is validated at the API gateway — user ID and role are forwarded via X-User-Id and X-User-Role headers.",
		parameters = {
			@Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true,
				description = "Authenticated manager's ID, injected by the API gateway after JWT validation.",
				schema = @Schema(type = "string", example = "10")),
			@Parameter(name = "X-User-Role", in = ParameterIn.HEADER, required = true,
				description = "Role injected by API gateway after JWT validation. Must be ROLE_HOTEL_MANAGER.",
				schema = @Schema(type = "string", example = "ROLE_HOTEL_MANAGER"))
		}
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Hotel deleted successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
			@ApiResponse(responseCode = "404", description = "No hotel found for this manager", content = @Content) })
	@DeleteMapping("/delete-hotel")
	public ResponseEntity<?> deleteOwnHotel(@RequestHeader("X-User-Id") String userIdHeader,
			@RequestHeader("X-User-Role") String userRole) {
		if (!userRole.equals("ROLE_HOTEL_MANAGER")) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied — Hotel Manager role required.");
		}
		Long managerId = Long.parseLong(userIdHeader);
		log.info("Manager ID: {} requested deletion of their own hotel", managerId);
		Hotel hotel = hotelService.getHotelByManagerId(managerId);
		hotelService.deleteOwnHotel(hotel.getHotelId(), managerId);
		log.info("Hotel ID: {} deleted by manager ID: {}", hotel.getHotelId(), managerId);
		return ResponseEntity.ok("Your hotel listing has been deleted successfully.");
	}

	@Operation(summary = "Update hotel rating", description = "Updates the rating of a hotel. Rating must be between 0.0 and 5.0.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Rating updated successfully",
					content = @Content(schema = @Schema(implementation = HotelResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid rating value", content = @Content),
			@ApiResponse(responseCode = "404", description = "Hotel not found", content = @Content) })
	@PutMapping("/{hotelId}/rating/{rating}")
	public ResponseEntity<HotelResponse> updateHotelRating(
			@Parameter(description = "ID of the hotel", required = true, example = "1") @PathVariable Long hotelId,
			@Parameter(description = "New rating value (0.0 to 5.0)", required = true, example = "4.5") @PathVariable @DecimalMin("0.0") @DecimalMax("5.0") Double rating) {
		log.info("Rating update request for hotel ID: {}, new rating: {}", hotelId, rating);
		Hotel updated = hotelService.updateHotelRating(hotelId, rating);
		log.info("Rating updated successfully for hotel ID: {}", hotelId);
		return ResponseEntity.ok(mapToHotelResponse(updated));
	}

	// ─────────────────────────────────────────────────────────────────
	// ADMIN ENDPOINTS
	// ─────────────────────────────────────────────────────────────────

	@Operation(
		summary = "Delete a hotel (Admin)",
		description = "Permanently deletes a hotel by ID. JWT is validated at the API gateway — role is forwarded via X-User-Role header.",
		parameters = {
			@Parameter(name = "X-User-Role", in = ParameterIn.HEADER, required = true,
				description = "Role injected by API gateway after JWT validation. Must be ROLE_ADMIN.",
				schema = @Schema(type = "string", example = "ROLE_ADMIN"))
		}
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Hotel deleted successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied — Admin only", content = @Content),
			@ApiResponse(responseCode = "404", description = "Hotel not found", content = @Content) })
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteHotel(
			@Parameter(description = "ID of the hotel to delete", required = true, example = "5") @PathVariable Long id,
			@RequestHeader("X-User-Role") String userRole) {
		if (!userRole.equals("ROLE_ADMIN")) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied — Admin role required.");
		}
		log.warn("Admin delete request received for hotel ID: {}", id);
		hotelService.deleteHotel(id);
		log.info("Hotel with ID: {} deleted by admin", id);
		return ResponseEntity.ok("Hotel deleted successfully.");
	}

	@Operation(
		summary = "Approve a hotel (Admin)",
		description = "Approves a newly registered hotel so it becomes publicly visible. JWT is validated at the API gateway — role is forwarded via X-User-Role header.",
		parameters = {
			@Parameter(name = "X-User-Role", in = ParameterIn.HEADER, required = true,
				description = "Role injected by API gateway after JWT validation. Must be ROLE_ADMIN.",
				schema = @Schema(type = "string", example = "ROLE_ADMIN"))
		}
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Hotel approved successfully",
					content = @Content(schema = @Schema(implementation = HotelResponse.class))),
			@ApiResponse(responseCode = "403", description = "Access denied — Admin only", content = @Content),
			@ApiResponse(responseCode = "404", description = "Hotel not found", content = @Content) })
	@PatchMapping("/{id}/approve")
	public ResponseEntity<?> approveHotel(
			@Parameter(description = "ID of the hotel to approve", required = true, example = "3") @PathVariable Long id,
			@RequestHeader("X-User-Role") String userRole) {
		if (!userRole.equals("ROLE_ADMIN")) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied — Admin role required.");
		}
		log.info("Approval request received for hotel ID: {}", id);
		Hotel approvedHotel = hotelService.approveHotel(id);
		log.info("Hotel with ID: {} approved successfully", id);
		return ResponseEntity.ok(mapToHotelResponse(approvedHotel));
	}

	@Operation(
		summary = "Admin search for all hotels",
		description = "Search all hotels (approved and unapproved) by name and/or location. JWT is validated at the API gateway — role is forwarded via X-User-Role header.",
		parameters = {
			@Parameter(name = "X-User-Role", in = ParameterIn.HEADER, required = true,
				description = "Role injected by API gateway after JWT validation. Must be ROLE_ADMIN.",
				schema = @Schema(type = "string", example = "ROLE_ADMIN"))
		}
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Matching hotels returned",
					content = @Content(schema = @Schema(implementation = HotelResponse.class))),
			@ApiResponse(responseCode = "403", description = "Access denied — Admin only", content = @Content) })
	@GetMapping("/admin/search")
	public ResponseEntity<?> searchHotelsForAdmin(
			@Parameter(description = "Hotel name to search for", example = "Grand") @RequestParam(required = false) String name,
			@Parameter(description = "Location to filter by", example = "Mumbai") @RequestParam(required = false) String location,
			@RequestHeader("X-User-Role") String userRole) {
		if (!userRole.equals("ROLE_ADMIN")) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied — Admin role required.");
		}
		log.info("Admin hotel search request - name: {}, location: {}", name, location);
		List<HotelResponse> responses = hotelService.searchAllHotelsForAdmin(name, location).stream()
				.map(this::mapToHotelResponse).collect(Collectors.toList());
		log.info("Admin search returned {} results", responses.size());
		return ResponseEntity.ok(responses);
	}

	// ─────────────────────────────────────────────────────────────────
	// HELPER
	// ─────────────────────────────────────────────────────────────────

	private HotelResponse mapToHotelResponse(Hotel hotel) {
		log.debug("Mapping hotel entity to response DTO for hotel ID: {}", hotel.getHotelId());
		HotelResponse response = new HotelResponse();
		response.setHotelId(hotel.getHotelId());
		response.setName(hotel.getName());
		response.setLocation(hotel.getLocation());
		response.setAmenities(hotel.getAmenities());
		response.setImageUrl(hotel.getImageUrl());
		response.setRating(hotel.getRating());
		response.setApproval(hotel.getApproval());
		return response;
	}
}