package com.cts.project.shbs.service.impl;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock RoomRepository roomRepository;
    @Mock HotelRepository hotelRepository;
    @Mock BookingServiceClient bookingClient;

    @InjectMocks RoomServiceImpl roomService;

    private Hotel approvedHotel;
    private Hotel unapprovedHotel;
    private Room room;
    private RoomRequest roomRequest;

    @BeforeEach
    void setUp() {
        approvedHotel = new Hotel();
        approvedHotel.setHotelId(1L);
        approvedHotel.setName("Grand Palace");
        approvedHotel.setApproval(true);
        approvedHotel.setManagerId(10L);

        unapprovedHotel = new Hotel();
        unapprovedHotel.setHotelId(2L);
        unapprovedHotel.setName("New Hotel");
        unapprovedHotel.setApproval(false);
        unapprovedHotel.setManagerId(11L);

        room = new Room();
        room.setRoomId(100L);
        room.setType("DELUXE");
        room.setPrice(new BigDecimal("2500.00"));
        room.setAvailability(true);
        room.setFeatures("AC, TV, WiFi");
        room.setImageUrl("https://example.com/room.jpg");
        room.setHotel(approvedHotel);

        roomRequest = new RoomRequest();
        roomRequest.setType("DELUXE");
        roomRequest.setPrice(new BigDecimal("2500.00"));
        roomRequest.setAvailability(true);
        roomRequest.setFeatures("AC, TV, WiFi");
        roomRequest.setImageUrl("https://example.com/room.jpg");
    }

    // ── createRoom ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createRoom()")
    class CreateRoom {

        @Test
        @DisplayName("creates room successfully for an approved hotel")
        void success() {
            when(hotelRepository.findById(1L)).thenReturn(Optional.of(approvedHotel));
            when(roomRepository.save(any(Room.class))).thenReturn(room);

            Room result = roomService.createRoom(1L, roomRequest);

            assertThat(result.getRoomId()).isEqualTo(100L);
            assertThat(result.getType()).isEqualTo("DELUXE");
            assertThat(result.getHotel()).isEqualTo(approvedHotel);
            verify(roomRepository).save(any(Room.class));
        }

        @Test
        @DisplayName("defaults availability to true when request has null availability")
        void nullAvailability_defaultsToTrue() {
            roomRequest.setAvailability(null);

            when(hotelRepository.findById(1L)).thenReturn(Optional.of(approvedHotel));
            when(roomRepository.save(any(Room.class))).thenAnswer(inv -> {
                Room r = inv.getArgument(0);
                assertThat(r.getAvailability()).isTrue();
                return room;
            });

            roomService.createRoom(1L, roomRequest);
        }

        @Test
        @DisplayName("throws HotelNotApprovedException when hotel is not approved")
        void unapprovedHotel_throws() {
            when(hotelRepository.findById(2L)).thenReturn(Optional.of(unapprovedHotel));

            assertThatThrownBy(() -> roomService.createRoom(2L, roomRequest))
                    .isInstanceOf(HotelNotApprovedException.class)
                    .hasMessageContaining("not yet approved");

            verify(roomRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when hotel does not exist")
        void hotelNotFound_throws() {
            when(hotelRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roomService.createRoom(99L, roomRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Hotel not found with ID: 99");

            verify(roomRepository, never()).save(any());
        }
    }

    // ── updateRoom ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateRoom()")
    class UpdateRoom {

        @Test
        @DisplayName("updates all fields successfully")
        void success() {
            RoomRequest updateReq = new RoomRequest();
            updateReq.setType("SUITE");
            updateReq.setPrice(new BigDecimal("5000.00"));
            updateReq.setAvailability(false);
            updateReq.setFeatures("Jacuzzi, Minibar");
            updateReq.setImageUrl("https://example.com/suite.jpg");

            when(roomRepository.findById(100L)).thenReturn(Optional.of(room));
            when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

            Room result = roomService.updateRoom(100L, updateReq);

            assertThat(result.getType()).isEqualTo("SUITE");
            assertThat(result.getPrice()).isEqualByComparingTo("5000.00");
            assertThat(result.getAvailability()).isFalse();
            assertThat(result.getFeatures()).isEqualTo("Jacuzzi, Minibar");
        }

        @Test
        @DisplayName("skips availability update when request availability is null")
        void nullAvailability_skipsUpdate() {
            roomRequest.setAvailability(null);
            boolean originalAvailability = room.getAvailability(); // true

            when(roomRepository.findById(100L)).thenReturn(Optional.of(room));
            when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

            Room result = roomService.updateRoom(100L, roomRequest);

            assertThat(result.getAvailability()).isEqualTo(originalAvailability);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when room not found")
        void notFound() {
            when(roomRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roomService.updateRoom(99L, roomRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Room not found with ID: 99");
        }
    }

    // ── deleteRoom ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteRoom()")
    class DeleteRoom {

        @Test
        @DisplayName("deletes room successfully")
        void success() {
            when(roomRepository.findById(100L)).thenReturn(Optional.of(room));

            roomService.deleteRoom(100L);

            verify(roomRepository).delete(room);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when room not found")
        void notFound() {
            when(roomRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roomService.deleteRoom(99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(roomRepository, never()).delete(any());
        }
    }

    // ── getRoomById ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getRoomById()")
    class GetRoomById {

        @Test
        @DisplayName("returns room when found")
        void found() {
            when(roomRepository.findById(100L)).thenReturn(Optional.of(room));

            Room result = roomService.getRoomById(100L);

            assertThat(result.getRoomId()).isEqualTo(100L);
            assertThat(result.getType()).isEqualTo("DELUXE");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void notFound() {
            when(roomRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roomService.getRoomById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Room not found with ID: 99");
        }
    }

    // ── getRoomsByHotelId ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getRoomsByHotelId()")
    class GetRoomsByHotelId {

        @Test
        @DisplayName("returns all rooms for a valid hotel")
        void success() {
            when(hotelRepository.existsById(1L)).thenReturn(true);
            when(roomRepository.findByHotelHotelId(1L)).thenReturn(List.of(room));

            List<Room> result = roomService.getRoomsByHotelId(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRoomId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("returns empty list when hotel has no rooms")
        void noRooms() {
            when(hotelRepository.existsById(1L)).thenReturn(true);
            when(roomRepository.findByHotelHotelId(1L)).thenReturn(List.of());

            assertThat(roomService.getRoomsByHotelId(1L)).isEmpty();
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when hotel does not exist")
        void hotelNotFound() {
            when(hotelRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> roomService.getRoomsByHotelId(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Hotel not found with ID: 99");

            verify(roomRepository, never()).findByHotelHotelId(any());
        }
    }

    // ── getAvailableRooms ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAvailableRooms()")
    class GetAvailableRooms {

        private RoomFilterRequest filterRequest;

        @BeforeEach
        void setUp() {
            filterRequest = new RoomFilterRequest();
            filterRequest.setHotelId(1L);
            filterRequest.setRoomType("DELUXE");
            filterRequest.setCheckIn(LocalDate.of(2026, 5, 1));
            filterRequest.setCheckOut(LocalDate.of(2026, 5, 5));
        }

        @Test
        @DisplayName("returns available rooms using booked IDs exclusion when bookings exist")
        void withBookedRooms() {
            BookedRoomsResponse bookedResp = new BookedRoomsResponse();
            bookedResp.setBookedRoomIds(List.of(101L, 102L));

            when(bookingClient.getBookedRooms(1L, filterRequest.getCheckIn(), filterRequest.getCheckOut()))
                    .thenReturn(bookedResp);
            when(roomRepository.findAvailableRooms(1L, "DELUXE", List.of(101L, 102L)))
                    .thenReturn(List.of(room));

            List<Room> result = roomService.getAvailableRooms(filterRequest);

            assertThat(result).hasSize(1);
            verify(roomRepository).findAvailableRooms(1L, "DELUXE", List.of(101L, 102L));
            verify(roomRepository, never())
                    .findByHotel_HotelIdAndTypeAndAvailabilityTrue(any(), any());
        }

        @Test
        @DisplayName("returns all available rooms directly when no bookings exist")
        void noBookedRooms() {
            BookedRoomsResponse bookedResp = new BookedRoomsResponse();
            bookedResp.setBookedRoomIds(List.of());

            when(bookingClient.getBookedRooms(any(), any(), any())).thenReturn(bookedResp);
            when(roomRepository.findByHotel_HotelIdAndTypeAndAvailabilityTrue(1L, "DELUXE"))
                    .thenReturn(List.of(room));

            List<Room> result = roomService.getAvailableRooms(filterRequest);

            assertThat(result).hasSize(1);
            verify(roomRepository).findByHotel_HotelIdAndTypeAndAvailabilityTrue(1L, "DELUXE");
            verify(roomRepository, never()).findAvailableRooms(any(), any(), any());
        }

        @Test
        @DisplayName("returns all available rooms directly when booking client returns null response")
        void nullBookingResponse() {
            when(bookingClient.getBookedRooms(any(), any(), any())).thenReturn(null);
            when(roomRepository.findByHotel_HotelIdAndTypeAndAvailabilityTrue(1L, "DELUXE"))
                    .thenReturn(List.of(room));

            List<Room> result = roomService.getAvailableRooms(filterRequest);

            assertThat(result).hasSize(1);
            verify(roomRepository).findByHotel_HotelIdAndTypeAndAvailabilityTrue(1L, "DELUXE");
        }

        @Test
        @DisplayName("returns all available rooms when booking client returns null bookedRoomIds")
        void nullBookedRoomIds() {
            BookedRoomsResponse bookedResp = new BookedRoomsResponse();
            bookedResp.setBookedRoomIds(null);

            when(bookingClient.getBookedRooms(any(), any(), any())).thenReturn(bookedResp);
            when(roomRepository.findByHotel_HotelIdAndTypeAndAvailabilityTrue(1L, "DELUXE"))
                    .thenReturn(List.of(room));

            List<Room> result = roomService.getAvailableRooms(filterRequest);

            assertThat(result).hasSize(1);
            verify(roomRepository).findByHotel_HotelIdAndTypeAndAvailabilityTrue(1L, "DELUXE");
        }

        @Test
        @DisplayName("fallback returns all available rooms when booking client throws")
        void bookingClientDown_fallbackFires() {
            when(roomRepository.findByHotel_HotelIdAndTypeAndAvailabilityTrue(1L, "DELUXE"))
                    .thenReturn(List.of(room));

            // Call the fallback directly — no spy needed
            // This is the method @CircuitBreaker calls in production when booking-service fails
            List<Room> result = roomService.getAvailableRoomsFallback(
                    filterRequest, new RuntimeException("Connection refused"));

            assertThat(result).hasSize(1);
            verify(roomRepository).findByHotel_HotelIdAndTypeAndAvailabilityTrue(1L, "DELUXE");
        }

        @Test
        @DisplayName("fallback returns empty list when both booking client and room repo fail")
        void bookingClientDown_andNoRooms_fallbackReturnsEmpty() {
            when(roomRepository.findByHotel_HotelIdAndTypeAndAvailabilityTrue(1L, "DELUXE"))
                    .thenReturn(Collections.emptyList());

            List<Room> result = roomService.getAvailableRoomsFallback(
                    filterRequest, new RuntimeException("Service down"));

            assertThat(result).isEmpty();
            verify(roomRepository).findByHotel_HotelIdAndTypeAndAvailabilityTrue(1L, "DELUXE");
        }
    }
}