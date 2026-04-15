package com.cts.shbsm.serviceImpl;


import com.cts.shbsm.dto.RedemptionResponseDto;
import com.cts.shbsm.exception.InsufficientBalanceException;
import com.cts.shbsm.exception.ResourceNotFoundException;
import com.cts.shbsm.model.LoyaltyAccount;
import com.cts.shbsm.model.Redemption;
import com.cts.shbsm.repository.LoyaltyAccountRepository;
import com.cts.shbsm.repository.RedemptionRepository;
import com.cts.shbsm.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoyaltyServiceImpl implements LoyaltyService {

    private final LoyaltyAccountRepository accountRepo;
    private final RedemptionRepository redemptionRepo;
    
    @Value("${loyalty.min.redemption.points}")
    private int minRedemptionPoints;

    @Value("${loyalty.discount.percentage}")
    private double discountPercentage;


    @Override
    public Integer getPointsBalance(Long userId) {
        return accountRepo.findByUserId(userId)
                .map(LoyaltyAccount::getPointsBalance)
                .orElseThrow(() -> {
                    log.warn("Attempted to fetch balance for non-existent User ID: {}", userId);
                    return new ResourceNotFoundException("Account not found for User: " + userId);
                });
    }

    @Override
    @Transactional
    public void initializeAccount(Long userId) {
        // Check if account already exists to avoid duplicates
        if (accountRepo.findByUserId(userId).isPresent()) {
            log.info("Initialization skipped: Account already exists for User ID: {}", userId);
            return;
        }

        LoyaltyAccount newAccount = new LoyaltyAccount();
        newAccount.setUserId(userId);
        newAccount.setPendingPoints(0);
        newAccount.setPointsBalance(0); // Start with zero
        newAccount.setLastUpdated(LocalDateTime.now());
        accountRepo.save(newAccount);
        log.info("Loyalty account initialized for User ID: {}", userId);
    }

    @Transactional
    public void addLoyaltyPendingPoints(Long userId, Double amountSpent) {
        int pointsToEarn = calculatePoints(amountSpent);

        LoyaltyAccount account = accountRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found for User: " + userId));
        account.setPendingPoints(account.getPendingPoints() + pointsToEarn);
        account.setLastUpdated(LocalDateTime.now());
        accountRepo.save(account);
    }

    @Transactional
    public void confirmPointsAfterCheckout(Long userId, Long bookingId, Double amountSpent) {
        LoyaltyAccount account = accountRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found for User: " + userId));

        int pointsToMove = calculatePoints(amountSpent);

        if (account.getPendingPoints() >= pointsToMove) {
            account.setPendingPoints(account.getPendingPoints() - pointsToMove);
            account.setPointsBalance(account.getPointsBalance() + pointsToMove);
            account.setLastUpdated(LocalDateTime.now());
            accountRepo.save(account);
            log.info("Confirmed {} points for User {}. New Balance: {}", pointsToMove, userId,
                    account.getPointsBalance());
        } else {
            log.warn("Point mismatch: User {} has only {} pending points but tried to confirm {}", userId,
                    account.getPendingPoints(), pointsToMove);
        }
    }

    @Override
    public List<RedemptionResponseDto> getRedemptionHistory(Long userId) {
        List<Redemption> redemptions = redemptionRepo.findByUserId(userId);

        if (redemptions == null || redemptions.isEmpty()) {
            return new ArrayList<>();
        }

        return redemptions.stream().map(r -> new RedemptionResponseDto(
                r.getRedemptionId(), r.getBookingId(), r.getPointsUsed(), r.getDiscountAmount(), r.getUserId(),
                "SUCCESS",
                r.getRedemptionDate()
        )).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void revertRedemption(Long userId, Long bookingId) {
        Redemption redemption = redemptionRepo.findByUserId(userId).stream()
                .filter(r -> r.getBookingId().equals(bookingId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Revert failed: No redemption record for Booking {}", bookingId);
                    return new ResourceNotFoundException("Redemption record not found for Booking ID: " + bookingId);
                });

        LoyaltyAccount account = accountRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loyalty account not found for User ID: " + userId));

        Integer pointsToRefund = redemption.getPointsUsed();
        account.setPointsBalance(account.getPointsBalance() + pointsToRefund);
        account.setLastUpdated(LocalDateTime.now());
        accountRepo.save(account);
        redemptionRepo.delete(redemption);
        log.info("Reverted {} points to User {} for cancelled Booking {}", pointsToRefund, userId, bookingId);
    }

    @Override
    @Transactional
    public void reduceCancelledBookingPoints(Long userId, Double bookingAmount) {
        // 1. Fetch the specific loyalty account for the user
        LoyaltyAccount loyalty = accountRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loyalty account not found for User: " + userId));

        // 2. Use your existing calculatePoints logic to find the exact points to remove
        int pointsToRemove = calculatePoints(bookingAmount);

        // 3. Deduct the amount from pendingPoints instead of setting to 0
        if (loyalty.getPendingPoints() != null && loyalty.getPendingPoints() >= pointsToRemove) {
            int newPendingBalance = loyalty.getPendingPoints() - pointsToRemove;
            loyalty.setPendingPoints(newPendingBalance);
        } else {
            // Safe check: If for some reason pending is less than removal, set to 0 to avoid negatives
            loyalty.setPendingPoints(0);
        }

        loyalty.setLastUpdated(LocalDateTime.now());
        accountRepo.save(loyalty);
        System.out.println("Points Adjusted: Removed " + pointsToRemove + " from pending for User " + userId);
    }

    public int calculatePoints(Double amountSpent) {
        // 1. Handle Null or Zero/Negative spending
        if (amountSpent == null || amountSpent <= 0) {
            return 0;
        }
        // 2. Strategy: 10 Rupees = 1 Point
        // Cast to int to floor-round (no fractional points)
        int earnedPoints = (int) (amountSpent / 10);
        System.out.println("Calculated Points: " + earnedPoints + " for amount: " + amountSpent);
        return earnedPoints;
    }

    public Double calculateDiscount(Double totalAmount) {
        // Flat 10% discount on total amount
        double discountAmount = totalAmount * discountPercentage;
        System.out.println("Discount: 10% applied | Discount Amount: " + discountAmount);
        return discountAmount;
    }

    @Override
    @Transactional
    public void saveRedemptionRecord(Long userId, Long bookingId, Integer pointsUsed, Double autoDiscount) {
        Redemption redemption = new Redemption();
        redemption.setUserId(userId);
        redemption.setBookingId(bookingId);
        redemption.setPointsUsed(pointsUsed);
        redemption.setDiscountAmount(autoDiscount);
        redemption.setRedemptionDate(LocalDateTime.now());
        redemptionRepo.save(redemption);
        log.info("Saved redemption record: User {}, Booking {}, Discount ₹{}", userId, bookingId, autoDiscount);
    }

    @Override
    @Transactional
    public void adjustLoyaltyBalance(Long userId) {
        LoyaltyAccount account = accountRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found for User: " + userId));

        Integer pointsToDeduct = minRedemptionPoints;

        if (account.getPointsBalance() < pointsToDeduct) {
            log.warn("Redemption failed: User {} has {} points, requested {}", userId, account.getPointsBalance(),
                    pointsToDeduct);
            throw new InsufficientBalanceException(
                    "Insufficient points: Needed " + pointsToDeduct + " but you have " + account.getPointsBalance());
        }

        account.setPointsBalance(account.getPointsBalance() - pointsToDeduct);
        account.setLastUpdated(LocalDateTime.now());
        accountRepo.save(account);
        log.info("Deducted {} points from User {}. Remaining: {}", pointsToDeduct, userId, account.getPointsBalance());
    }
}