package com.cts.shbsm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cts.shbsm.model.LoyaltyAccount;

import java.util.Optional;

@Repository
public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {
    
    // Crucial for balance checks and updates
    Optional<LoyaltyAccount> findByUserId(Long userId);
    
    // Check if an account exists before creating a duplicate
    boolean existsByUserId(Long userId);
}