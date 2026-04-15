package com.cts.project.shbs.service;

import com.cts.project.shbs.dto.HotelRequest;
import com.cts.project.shbs.model.Hotel;
import java.util.List;
import java.util.Optional;

public interface HotelService {
    Hotel createHotel(HotelRequest request, Long managerId);
    Hotel updateHotel(Long hotelId, HotelRequest request);
    void deleteHotel(Long hotelId);
    void deleteOwnHotel(Long hotelId, Long managerId);
    Hotel getHotelById(Long hotelId);
    List<Hotel> getAllApprovedHotels();
    Hotel approveHotel(Long hotelId);
    List<Hotel> searchApprovedHotels(String name, String location);
    List<Hotel> searchAllHotelsForAdmin(String name, String location);
    Hotel getHotelByManagerId(Long managerId);
    Hotel updateHotelRating(Long hotelId, Double rating);
}
