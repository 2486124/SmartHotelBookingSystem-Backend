package com.cts.shbsm.service;


import java.util.List;

import com.cts.shbsm.dto.RedemptionResponseDto;

public interface LoyaltyService {

	Integer getPointsBalance(Long userId);

	Double calculateDiscount(Double totalAmount);
	
	void addLoyaltyPendingPoints(Long userId, Double amountSpent);

    void adjustLoyaltyBalance(Long userId);

    List<RedemptionResponseDto> getRedemptionHistory(Long userId);
   
    void revertRedemption(Long userId, Long bookingId);

	void initializeAccount(Long userId);
	
	void confirmPointsAfterCheckout(Long userId, Long bookingId, Double amountSpent);

	void saveRedemptionRecord(Long userId, Long bookingId, Integer pointsUsed, Double autoDiscount);
	
	void reduceCancelledBookingPoints(Long userId, Double bookingAmount);

	int calculatePoints(Double amount);

}
