package com.cts.project.shbs.repository;

import com.cts.project.shbs.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    List<Payment> findByUserId(Long userId);

    @Modifying
    @Query(value = "UPDATE Payment SET BookingID = :bookingId WHERE PaymentID = :paymentId",
           nativeQuery = true)
    void updateBookingId(@Param("paymentId") Long paymentId,
                         @Param("bookingId") Long bookingId);
}