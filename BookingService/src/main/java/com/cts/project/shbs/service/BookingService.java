package com.cts.project.shbs.service;

import com.cts.project.shbs.dto.BookingPaymentResponse;
import com.cts.project.shbs.dto.RazorpayConfirmRequest;
import com.cts.project.shbs.dto.RazorpayOrderRequest;
import com.cts.project.shbs.dto.RazorpayOrderResponse;
import com.cts.project.shbs.model.Booking;
import com.cts.project.shbs.model.Booking.BookingStatus;
import java.time.LocalDate;
import java.util.List;

public interface BookingService {
    RazorpayOrderResponse createOrder(RazorpayOrderRequest request);
    BookingPaymentResponse confirmBooking(RazorpayConfirmRequest request);
    void cancelBooking(Long bookingId);
    void updateStatus(Long bookingId, BookingStatus status);
    void updateStatusByUserAndHotel(Long userId, Long hotelId, BookingStatus status);
    String cancelFutureBookingsByHotel(Long hotelId);
    Booking getBookingById(Long id);
    List<Booking> getAllBookings();
    List<Booking> getBookingsByUserId(Long userId);
    List<Booking> getBookingsByHotelId(Long hotelId);
    List<Long> getBookedRoomIds(Long hotelId, LocalDate checkIn, LocalDate checkOut);
}