package com.cts.shbsm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cts.shbsm.dto.RedemptionResponseDto;
import com.cts.shbsm.exception.InsufficientBalanceException;
import com.cts.shbsm.exception.ResourceNotFoundException;
import com.cts.shbsm.model.LoyaltyAccount;
import com.cts.shbsm.model.Redemption;
import com.cts.shbsm.repository.LoyaltyAccountRepository;
import com.cts.shbsm.repository.RedemptionRepository;
import com.cts.shbsm.serviceImpl.LoyaltyServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class LoyaltyServiceImplTest {

    @Mock
    private LoyaltyAccountRepository accountRepo;

    @Mock
    private RedemptionRepository redemptionRepo;

    @InjectMocks
    private LoyaltyServiceImpl loyaltyService;

    private LoyaltyAccount testAccount;
    private final Long userId = 8L;
    
    
    @BeforeEach
    void setUp() {
        testAccount = new LoyaltyAccount();
        testAccount.setUserId(userId);
        testAccount.setPointsBalance(1000);
        testAccount.setPendingPoints(200);
        testAccount.setLastUpdated(LocalDateTime.now());
        ReflectionTestUtils.setField(loyaltyService, "minRedemptionPoints", 300);
        ReflectionTestUtils.setField(loyaltyService, "discountPercentage", 0.10);
    }

    // -------------------------------------------------------------------------
    // getPointsBalance
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return correct points balance for existing user")
    void getPointsBalance_Success() {
        when(accountRepo.findByUserId(userId)).thenReturn(Optional.of(testAccount));

        Integer balance = loyaltyService.getPointsBalance(userId);

        assertEquals(1000, balance);
        verify(accountRepo, times(1)).findByUserId(userId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when account is missing")
    void getPointsBalance_NotFound() {
        when(accountRepo.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> loyaltyService.getPointsBalance(userId));
    }

    // -------------------------------------------------------------------------
    // addLoyaltyPendingPoints
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should add pending points correctly (Amount / 10)")
    void addLoyaltyPendingPoints_Success() {
        when(accountRepo.findByUserId(userId)).thenReturn(Optional.of(testAccount));

        loyaltyService.addLoyaltyPendingPoints(userId, 500.0); // 500 / 10 = 50 points

        assertEquals(250, testAccount.getPendingPoints()); // 200 initial + 50 earned
        verify(accountRepo).save(testAccount);
    }

    // -------------------------------------------------------------------------
    // adjustLoyaltyBalance
    // Service always deducts exactly 300 points.
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should deduct exactly 300 points when balance is sufficient")
    void adjustLoyaltyBalance_Success() {
        // testAccount has 1000 points → 1000 - 300 = 700
        when(accountRepo.findByUserId(userId)).thenReturn(Optional.of(testAccount));

        loyaltyService.adjustLoyaltyBalance(userId);

        assertEquals(700, testAccount.getPointsBalance());
        verify(accountRepo).save(any(LoyaltyAccount.class));
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when balance is below 300")
    void adjustLoyaltyBalance_Insufficient() {
        // Give account only 100 points — less than the required 300
        testAccount.setPointsBalance(100);
        when(accountRepo.findByUserId(userId)).thenReturn(Optional.of(testAccount));

        assertThrows(InsufficientBalanceException.class,
                () -> loyaltyService.adjustLoyaltyBalance(userId));

        verify(accountRepo, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // confirmPointsAfterCheckout
    // Service has NO date/checkout logic — it only moves pending → available
    // points when pending >= pointsToMove. BookingClient is NOT used here.
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should move pending points to available balance when pending is sufficient")
    void confirmPointsAfterCheckout_Success() {
        // 2000 / 10 = 200 points; testAccount has 200 pending — exactly enough
        when(accountRepo.findByUserId(userId)).thenReturn(Optional.of(testAccount));

        loyaltyService.confirmPointsAfterCheckout(userId, 101L, 2000.0);

        assertEquals(0, testAccount.getPendingPoints());    // 200 - 200 = 0
        assertEquals(1200, testAccount.getPointsBalance()); // 1000 + 200 = 1200
        verify(accountRepo).save(testAccount);
    }

    @Test
    @DisplayName("Should NOT update balance when pending points are insufficient")
    void confirmPointsAfterCheckout_InsufficientPending() {
        // 5000 / 10 = 500 points to move, but only 200 pending — no update expected
        when(accountRepo.findByUserId(userId)).thenReturn(Optional.of(testAccount));

        loyaltyService.confirmPointsAfterCheckout(userId, 101L, 5000.0);

        // Neither pending nor balance should change
        assertEquals(200, testAccount.getPendingPoints());
        assertEquals(1000, testAccount.getPointsBalance());
        verify(accountRepo, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when account is missing on confirmation")
    void confirmPointsAfterCheckout_AccountNotFound() {
        when(accountRepo.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> loyaltyService.confirmPointsAfterCheckout(userId, 101L, 2000.0));
    }

    // -------------------------------------------------------------------------
    // revertRedemption
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should revert redemption and refund points to balance")
    void revertRedemption_Success() {
        Long bookingId = 99L;
        Redemption redemption = new Redemption();
        redemption.setPointsUsed(300);
        redemption.setBookingId(bookingId);

        when(redemptionRepo.findByUserId(userId)).thenReturn(List.of(redemption));
        when(accountRepo.findByUserId(userId)).thenReturn(Optional.of(testAccount));

        loyaltyService.revertRedemption(userId, bookingId);

        assertEquals(1300, testAccount.getPointsBalance()); // 1000 + 300 refunded
        verify(redemptionRepo).delete(redemption);
        verify(accountRepo).save(testAccount);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when no redemption record exists for booking")
    void revertRedemption_NotFound() {
        Long bookingId = 999L;
        when(redemptionRepo.findByUserId(userId)).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> loyaltyService.revertRedemption(userId, bookingId));

        verify(accountRepo, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // initializeAccount
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should create a new loyalty account if none exists")
    void initializeAccount_NewUser() {
        when(accountRepo.findByUserId(userId)).thenReturn(Optional.empty());

        loyaltyService.initializeAccount(userId);

        verify(accountRepo, times(1)).save(any(LoyaltyAccount.class));
    }

    @Test
    @DisplayName("Should skip initialization if account already exists")
    void initializeAccount_AlreadyExists() {
        when(accountRepo.findByUserId(userId)).thenReturn(Optional.of(testAccount));

        loyaltyService.initializeAccount(userId);

        verify(accountRepo, never()).save(any(LoyaltyAccount.class));
    }

    // -------------------------------------------------------------------------
    // reduceCancelledBookingPoints
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should reduce pending points when a booking is cancelled")
    void reduceCancelledBookingPoints_Success() {
        when(accountRepo.findByUserId(userId)).thenReturn(Optional.of(testAccount));

        loyaltyService.reduceCancelledBookingPoints(userId, 1000.0); // 1000 / 10 = 100 points

        assertEquals(100, testAccount.getPendingPoints()); // 200 - 100 = 100
        verify(accountRepo).save(testAccount);
    }

    @Test
    @DisplayName("Should set pending points to zero if reduction exceeds current pending balance")
    void reduceCancelledBookingPoints_NegativeSafety() {
        when(accountRepo.findByUserId(userId)).thenReturn(Optional.of(testAccount));

        loyaltyService.reduceCancelledBookingPoints(userId, 5000.0); // 500 points > 200 pending

        assertEquals(0, testAccount.getPendingPoints());
        verify(accountRepo).save(testAccount);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when reducing points for missing account")
    void reduceCancelledBookingPoints_NotFound() {
        when(accountRepo.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> loyaltyService.reduceCancelledBookingPoints(userId, 100.0));
    }

    // -------------------------------------------------------------------------
    // calculateDiscount
    // Service computes totalAmount * 0.10 — no null/negative guard exists,
    // so tests must reflect the actual behaviour.
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should correctly calculate 10% discount on a valid amount")
    void calculateDiscount_Success() {
        Double discount = loyaltyService.calculateDiscount(500.0);
        assertEquals(50.0, discount);
    }

    @Test
    @DisplayName("Should throw NullPointerException when null is passed to calculateDiscount")
    void calculateDiscount_NullThrowsNPE() {
        // The service does not guard against null — this documents the current behaviour.
        assertThrows(NullPointerException.class,
                () -> loyaltyService.calculateDiscount(null));
    }

    @Test
    @DisplayName("Should return a negative discount for negative input (no guard in service)")
    void calculateDiscount_NegativeInput() {
        // Service applies 10% without validation; -50 * 0.10 = -5.0
        Double discount = loyaltyService.calculateDiscount(-50.0);
        assertEquals(-5.0, discount);
    }

    // -------------------------------------------------------------------------
    // calculatePoints (package-accessible helper)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return 0 points for null amount")
    void calculatePoints_NullAmount() {
        assertEquals(0, loyaltyService.calculatePoints(null));
    }

    @Test
    @DisplayName("Should return 0 points for zero or negative amount")
    void calculatePoints_ZeroOrNegative() {
        assertEquals(0, loyaltyService.calculatePoints(0.0));
        assertEquals(0, loyaltyService.calculatePoints(-100.0));
    }

    @Test
    @DisplayName("Should calculate 1 point per 10 rupees spent")
    void calculatePoints_Success() {
        assertEquals(50, loyaltyService.calculatePoints(500.0));
        assertEquals(33, loyaltyService.calculatePoints(330.0)); // floor division
    }
}