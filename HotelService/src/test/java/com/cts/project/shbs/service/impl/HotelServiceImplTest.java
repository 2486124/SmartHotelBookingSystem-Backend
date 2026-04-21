package com.cts.project.shbs.service.impl;

import com.cts.project.shbs.dto.HotelRequest;
import com.cts.project.shbs.exception.*;
import com.cts.project.shbs.model.Hotel;
import com.cts.project.shbs.repository.HotelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelServiceImplTest {

    @Mock
    HotelRepository hotelRepository;

    @InjectMocks
    HotelServiceImpl hotelService;

    private Hotel hotel;
    private HotelRequest hotelRequest;

    @BeforeEach
    void setUp() {
        hotel = new Hotel();
        hotel.setHotelId(1L);
        hotel.setName("Grand Palace");
        hotel.setLocation("Chennai");
        hotel.setAmenities("Pool, Gym, WiFi");
        hotel.setImageUrl("https://example.com/img.jpg");
        hotel.setRating(4.2);
        hotel.setManagerId(10L);
        hotel.setApproval(false);

        hotelRequest = new HotelRequest();
        hotelRequest.setName("Grand Palace");
        hotelRequest.setLocation("Chennai");
        hotelRequest.setAmenities("Pool, Gym, WiFi");
        hotelRequest.setImageUrl("https://example.com/img.jpg");
    }

    // createHotel

    @Nested
    @DisplayName("createHotel()")
    class CreateHotel {

        @Test
        @DisplayName("creates hotel successfully when manager has no existing hotel")
        void success() {
            when(hotelRepository.findByManagerId(10L)).thenReturn(Optional.empty());
            when(hotelRepository.save(any(Hotel.class))).thenReturn(hotel);

            Hotel result = hotelService.createHotel(hotelRequest, 10L);

            assertThat(result.getHotelId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Grand Palace");
            assertThat(result.getApproval()).isFalse(); // always starts unapproved
            verify(hotelRepository).save(any(Hotel.class));
        }

        @Test
        @DisplayName("sets approval to false by default even if not explicitly set")
        void approvalDefaultsFalse() {
            when(hotelRepository.findByManagerId(10L)).thenReturn(Optional.empty());
            when(hotelRepository.save(any(Hotel.class))).thenAnswer(inv -> {
                Hotel h = inv.getArgument(0);
                assertThat(h.getApproval()).isFalse();
                return hotel;
            });

            hotelService.createHotel(hotelRequest, 10L);
        }

        @Test
        @DisplayName("defaults rating to 5.0 on creation since manager cannot set it")
        void ratingDefaultsFiveDotZero() {
            when(hotelRepository.findByManagerId(10L)).thenReturn(Optional.empty());
            when(hotelRepository.save(any(Hotel.class))).thenAnswer(inv -> {
                Hotel h = inv.getArgument(0);
                assertThat(h.getRating()).isEqualTo(5.0);
                return hotel;
            });

            hotelService.createHotel(hotelRequest, 10L);
        }

        @Test
        @DisplayName("throws DuplicateResourceException when manager already owns a hotel")
        void managerAlreadyOwnsHotel_throws() {
            when(hotelRepository.findByManagerId(10L)).thenReturn(Optional.of(hotel));

            assertThatThrownBy(() -> hotelService.createHotel(hotelRequest, 10L))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Manager already owns a hotel");

            verify(hotelRepository, never()).save(any());
        }
    }

    // updateHotel

    @Nested
    @DisplayName("updateHotel()")
    class UpdateHotel {

        @Test
        @DisplayName("updates all fields successfully — rating unchanged since it is review-driven")
        void success() {
            HotelRequest updateReq = new HotelRequest();
            updateReq.setName("Updated Palace");
            updateReq.setLocation("Mumbai");
            updateReq.setAmenities("Spa, Pool");
            updateReq.setImageUrl("https://example.com/new.jpg");

            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
            when(hotelRepository.save(any(Hotel.class))).thenAnswer(inv -> inv.getArgument(0));

            Hotel result = hotelService.updateHotel(1L, updateReq);

            assertThat(result.getName()).isEqualTo("Updated Palace");
            assertThat(result.getLocation()).isEqualTo("Mumbai");
            assertThat(result.getAmenities()).isEqualTo("Spa, Pool");
            // Rating must not be touched by a profile update
            assertThat(result.getRating()).isEqualTo(hotel.getRating());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when hotel not found")
        void notFound() {
            when(hotelRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> hotelService.updateHotel(99L, hotelRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // deleteHotel

    @Nested
    @DisplayName("deleteHotel()")
    class DeleteHotel {

        @Test
        @DisplayName("admin deletes hotel successfully")
        void success() {
            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));

            hotelService.deleteHotel(1L);

            verify(hotelRepository).delete(hotel);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when hotel not found")
        void notFound() {
            when(hotelRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> hotelService.deleteHotel(99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(hotelRepository, never()).delete(any());
        }
    }

    // getHotelById
    @Nested
    @DisplayName("getHotelById()")
    class GetHotelById {

        @Test
        @DisplayName("returns hotel when found")
        void found() {
            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));

            Hotel result = hotelService.getHotelById(1L);

            assertThat(result.getHotelId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Grand Palace");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void notFound() {
            when(hotelRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> hotelService.getHotelById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Hotel not found with ID: 99");
        }
    }

    // getAllApprovedHotels
    @Nested
    @DisplayName("getAllApprovedHotels()")
    class GetAllApprovedHotels {

        @Test
        @DisplayName("returns only approved hotels")
        void success() {
            Hotel approved = new Hotel();
            approved.setApproval(true);
            when(hotelRepository.findByApprovalTrue()).thenReturn(List.of(approved));

            List<Hotel> result = hotelService.getAllApprovedHotels();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getApproval()).isTrue();
        }

        @Test
        @DisplayName("returns empty list when no approved hotels")
        void empty() {
            when(hotelRepository.findByApprovalTrue()).thenReturn(List.of());

            assertThat(hotelService.getAllApprovedHotels()).isEmpty();
        }
    }

    // approveHotel
    @Nested
    @DisplayName("approveHotel()")
    class ApproveHotel {

        @Test
        @DisplayName("approves a pending hotel successfully")
        void success() {
            hotel.setApproval(false);
            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
            when(hotelRepository.save(any(Hotel.class))).thenAnswer(inv -> inv.getArgument(0));

            Hotel result = hotelService.approveHotel(1L);

            assertThat(result.getApproval()).isTrue();
            verify(hotelRepository).save(hotel);
        }

        @Test
        @DisplayName("throws DuplicateResourceException when hotel is already approved")
        void alreadyApproved_throws() {
            hotel.setApproval(true);
            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));

            assertThatThrownBy(() -> hotelService.approveHotel(1L))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("already approved");

            verify(hotelRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when hotel not found")
        void notFound() {
            when(hotelRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> hotelService.approveHotel(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // searchApprovedHotels
    @Nested
    @DisplayName("searchApprovedHotels()")
    class SearchApprovedHotels {

        @Test
        @DisplayName("returns matching approved hotels")
        void success() {
            when(hotelRepository.searchApprovedHotels("Grand", "Chennai"))
                    .thenReturn(List.of(hotel));

            List<Hotel> result = hotelService.searchApprovedHotels("Grand", "Chennai");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Grand Palace");
        }

        @Test
        @DisplayName("returns empty list when no match")
        void noMatch() {
            when(hotelRepository.searchApprovedHotels("Unknown", "Mars"))
                    .thenReturn(List.of());

            assertThat(hotelService.searchApprovedHotels("Unknown", "Mars")).isEmpty();
        }
    }

    // searchAllHotelsForAdmin
    @Nested
    @DisplayName("searchAllHotelsForAdmin()")
    class SearchAllHotelsForAdmin {

        @Test
        @DisplayName("returns all hotels (approved + unapproved) for admin")
        void success() {
            Hotel unapproved = new Hotel();
            unapproved.setApproval(false);
            when(hotelRepository.searchAllHotelsForAdmin("Grand", "Chennai"))
                    .thenReturn(List.of(hotel, unapproved));

            List<Hotel> result = hotelService.searchAllHotelsForAdmin("Grand", "Chennai");

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("returns empty list when no match")
        void noMatch() {
            when(hotelRepository.searchAllHotelsForAdmin(any(), any()))
                    .thenReturn(List.of());

            assertThat(hotelService.searchAllHotelsForAdmin("X", "Y")).isEmpty();
        }
    }

    // deleteOwnHotel
    @Nested
    @DisplayName("deleteOwnHotel()")
    class DeleteOwnHotel {

        @Test
        @DisplayName("manager deletes own hotel successfully")
        void success() {
            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel)); // managerId = 10L

            hotelService.deleteOwnHotel(1L, 10L);

            verify(hotelRepository).delete(hotel);
        }

        @Test
        @DisplayName("throws UnauthorizedAccessException when manager does not own the hotel")
        void wrongManager_throws() {
            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel)); // owner = 10L

            assertThatThrownBy(() -> hotelService.deleteOwnHotel(1L, 99L)) // attacker = 99L
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessageContaining("Access denied");

            verify(hotelRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when hotel does not exist")
        void notFound() {
            when(hotelRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> hotelService.deleteOwnHotel(99L, 10L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // getHotelByManagerId
    @Nested
    @DisplayName("getHotelByManagerId()")
    class GetHotelByManagerId {

        @Test
        @DisplayName("returns hotel when manager has one")
        void found() {
            when(hotelRepository.findByManagerId(10L)).thenReturn(Optional.of(hotel));

            Hotel result = hotelService.getHotelByManagerId(10L);

            assertThat(result.getManagerId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when manager has no hotel")
        void notFound() {
            when(hotelRepository.findByManagerId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> hotelService.getHotelByManagerId(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No hotel found for manager ID: 99");
        }
    }

    // updateHotelRating
    @Nested
    @DisplayName("updateHotelRating()")
    class UpdateHotelRating {

        @Test
        @DisplayName("updates rating successfully for valid value")
        void success() {
            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
            when(hotelRepository.save(any(Hotel.class))).thenAnswer(inv -> inv.getArgument(0));

            Hotel result = hotelService.updateHotelRating(1L, 4.5);

            assertThat(result.getRating()).isEqualTo(4.5);
        }

        @Test
        @DisplayName("accepts boundary value 0.0")
        void boundaryMin() {
            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
            when(hotelRepository.save(any(Hotel.class))).thenAnswer(inv -> inv.getArgument(0));

            Hotel result = hotelService.updateHotelRating(1L, 0.0);

            assertThat(result.getRating()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("accepts boundary value 5.0")
        void boundaryMax() {
            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
            when(hotelRepository.save(any(Hotel.class))).thenAnswer(inv -> inv.getArgument(0));

            Hotel result = hotelService.updateHotelRating(1L, 5.0);

            assertThat(result.getRating()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("throws InvalidRatingException for rating above 5.0")
        void aboveMax_throws() {
            assertThatThrownBy(() -> hotelService.updateHotelRating(1L, 5.1))
                    .isInstanceOf(InvalidRatingException.class)
                    .hasMessageContaining("Rating must be between 0.0 and 5.0");

            verify(hotelRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws InvalidRatingException for negative rating")
        void negative_throws() {
            assertThatThrownBy(() -> hotelService.updateHotelRating(1L, -0.1))
                    .isInstanceOf(InvalidRatingException.class);

            verify(hotelRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws InvalidRatingException for null rating")
        void nullRating_throws() {
            assertThatThrownBy(() -> hotelService.updateHotelRating(1L, null))
                    .isInstanceOf(InvalidRatingException.class);

            verify(hotelRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when hotel not found")
        void hotelNotFound() {
            when(hotelRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> hotelService.updateHotelRating(99L, 3.0))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}