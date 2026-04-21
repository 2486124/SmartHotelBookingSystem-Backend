package com.cts.project.shbs.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.cts.project.shbs.dto.HotelRequest;
import com.cts.project.shbs.exception.DuplicateResourceException;
import com.cts.project.shbs.exception.InvalidRatingException;
import com.cts.project.shbs.exception.ResourceNotFoundException;
import com.cts.project.shbs.exception.UnauthorizedAccessException;
import com.cts.project.shbs.model.Hotel;
import com.cts.project.shbs.repository.HotelRepository;
import com.cts.project.shbs.service.HotelService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;

    @Override
    public Hotel createHotel(HotelRequest request, Long managerId) {
        log.info("Creating hotel '{}' for manager ID: {}", request.getName(), managerId);

        // Prevent a manager from owning more than one hotel
        hotelRepository.findByManagerId(managerId).ifPresent(existing -> {
            log.warn("Manager ID: {} already owns hotel ID: {}", managerId, existing.getHotelId());
            throw new DuplicateResourceException(
                "Manager already owns a hotel with ID: " + existing.getHotelId());
        });

        Hotel hotel = new Hotel();
        hotel.setName(request.getName());
        hotel.setLocation(request.getLocation());
        hotel.setAmenities(request.getAmenities());
        hotel.setImageUrl(request.getImageUrl());
        hotel.setRating(5.0);
        hotel.setManagerId(managerId);
        hotel.setApproval(false);

        Hotel saved = hotelRepository.save(hotel);
        log.info("Hotel created successfully with ID: {} (pending approval)", saved.getHotelId());
        return saved;
    }

    @Override
    public Hotel updateHotel(Long hotelId, HotelRequest request) {
        log.info("Updating hotel ID: {}", hotelId);
        Hotel hotel = getHotelById(hotelId);
        hotel.setName(request.getName());
        hotel.setLocation(request.getLocation());
        hotel.setAmenities(request.getAmenities());
        hotel.setImageUrl(request.getImageUrl());

        Hotel updated = hotelRepository.save(hotel);
        log.info("Hotel ID: {} updated successfully", hotelId);
        return updated;
    }

    @Override
    public void deleteHotel(Long hotelId) {
        log.warn("Admin deleting hotel ID: {}", hotelId);
        Hotel hotel = getHotelById(hotelId);
        hotelRepository.delete(hotel);
        log.info("Hotel ID: {} deleted successfully by admin", hotelId);
    }

    @Override
    public Hotel getHotelById(Long hotelId) {
        log.debug("Fetching hotel by ID: {}", hotelId);
        return hotelRepository.findById(hotelId)
                .orElseThrow(() -> {
                    log.error("Hotel not found with ID: {}", hotelId);
                    return new ResourceNotFoundException("Hotel not found with ID: " + hotelId);
                });
    }

    @Override
    public List<Hotel> getAllApprovedHotels() {
        log.info("Fetching all approved hotels");
        List<Hotel> hotels = hotelRepository.findByApprovalTrue();
        log.info("Found {} approved hotels", hotels.size());
        return hotels;
    }

    @Override
    public Hotel approveHotel(Long hotelId) {
        log.info("Approving hotel ID: {}", hotelId);
        Hotel hotel = getHotelById(hotelId);

        // Prevent approving an already-approved hotel
        if (hotel.getApproval()) {
            log.warn("Hotel ID: {} is already approved", hotelId);
            throw new DuplicateResourceException("Hotel ID: " + hotelId + " is already approved.");
        }

        hotel.setApproval(true);
        Hotel approved = hotelRepository.save(hotel);
        log.info("Hotel ID: {} approved successfully", hotelId);
        return approved;
    }

    @Override
    public List<Hotel> searchApprovedHotels(String name, String location) {
        log.info("Searching approved hotels - name: {}, location: {}", name, location);
        List<Hotel> results = hotelRepository.searchApprovedHotels(name, location);
        log.info("Search returned {} approved hotels", results.size());
        return results;
    }

    @Override
    public List<Hotel> searchAllHotelsForAdmin(String name, String location) {
        log.info("Admin searching all hotels - name: {}, location: {}", name, location);
        List<Hotel> results = hotelRepository.searchAllHotelsForAdmin(name, location);
        log.info("Admin search returned {} hotels", results.size());
        return results;
    }

    @Override
    public void deleteOwnHotel(Long hotelId, Long managerId) {
        log.info("Manager ID: {} requesting deletion of hotel ID: {}", managerId, hotelId);

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> {
                    log.error("Hotel not found with ID: {} during manager delete", hotelId);
                    return new ResourceNotFoundException("Hotel not found with ID: " + hotelId);
                });

        if (!hotel.getManagerId().equals(managerId)) {
            log.warn("Unauthorized delete attempt - manager ID: {} tried to delete hotel ID: {} owned by manager ID: {}",
                    managerId, hotelId, hotel.getManagerId());
            throw new UnauthorizedAccessException(
                "Access denied. You can only delete your own hotel.");
        }

        hotelRepository.delete(hotel);
        log.info("Hotel ID: {} deleted successfully by manager ID: {}", hotelId, managerId);
    }

    @Override
    public Hotel getHotelByManagerId(Long managerId) {
        log.debug("Fetching hotel for manager ID: {}", managerId);

        return hotelRepository.findByManagerId(managerId)
                .orElseThrow(() -> {
                    log.error("No hotel found for manager ID: {}", managerId);
                    return new ResourceNotFoundException(
                        "No hotel found for manager ID: " + managerId);
                });
    }

    @Override
    public Hotel updateHotelRating(Long hotelId, Double rating) {
        log.info("Updating rating for hotel ID: {} to {}", hotelId, rating);

        // Validate rating range before saving
        if (rating == null || rating < 0.0 || rating > 5.0) {
            log.error("Invalid rating value: {} for hotel ID: {}", rating, hotelId);
            throw new InvalidRatingException(
                "Rating must be between 0.0 and 5.0. Provided: " + rating);
        }

        Hotel hotel = getHotelById(hotelId);
        hotel.setRating(rating);
        Hotel updated = hotelRepository.save(hotel);
        log.info("Rating updated successfully for hotel ID: {} - new rating: {}", hotelId, rating);
        return updated;
    }
}