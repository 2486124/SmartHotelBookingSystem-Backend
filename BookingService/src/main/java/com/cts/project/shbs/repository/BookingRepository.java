package com.cts.project.shbs.repository;

import com.cts.project.shbs.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByHotelId(Long hotelId);

    @Query("SELECT DISTINCT b.roomId FROM Booking b " +
           "WHERE b.hotelId = :hotelId " +
           "AND b.status IN ('CONFIRMED', 'CHECKED_IN') " +
           "AND b.checkInDate < :checkOut " +
           "AND b.checkOutDate > :checkIn")
    List<Long> findBookedRoomIds(@Param("hotelId") Long hotelId,
                                  @Param("checkIn") LocalDate checkIn,
                                  @Param("checkOut") LocalDate checkOut);

    @Query("SELECT b FROM Booking b " +
           "WHERE b.hotelId = :hotelId " +
           "AND b.status = com.cts.project.shbs.model.Booking.BookingStatus.CONFIRMED " +
           "AND b.checkInDate >= :today")
    List<Booking> findFutureConfirmedBookingsByHotel(@Param("hotelId") Long hotelId,
                                                      @Param("today") LocalDate today);

    @Query("SELECT b FROM Booking b " +
           "WHERE b.userId = :userId " +
           "AND b.hotelId = :hotelId " +
           "AND b.status = com.cts.project.shbs.model.Booking.BookingStatus.CHECKED_OUT")
    Optional<Booking> findCheckedOutBookingByUserAndHotel(@Param("userId") Long userId,
                                                           @Param("hotelId") Long hotelId);
}