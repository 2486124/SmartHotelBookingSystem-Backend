package com.cts.shbsm.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cts.shbsm.model.Redemption;

import java.util.List;

@Repository
public interface RedemptionRepository extends JpaRepository<Redemption, Long> {
    
    // Used for the history API
    List<Redemption> findByUserId(Long userId);

    // Useful if you need to find redemptions for a specific booking
    List<Redemption> findByBookingId(Long bookingId);
}
