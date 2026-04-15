package com.cts.shbsm.loyaltyClient;
//
//import com.cts.booking.dto.*; // Ensure DTOs are defined in Booking module
import com.cts.shbsm.dto.AddPointsRequestDto;
import com.cts.shbsm.dto.CancelPointsRequestDto;
import com.cts.shbsm.dto.RedemptionRequestDto;
import com.cts.shbsm.dto.RedemptionResponseDto;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "loyalty-service", url = "${loyalty.service.url:http://localhost:8085}")
public interface LoyaltyClient {

	@GetMapping("/api/loyalty/balance/{userId}")
    Integer getPointsBalance(@PathVariable("userId") Long userId);

    /**
     * 2. PREVIEW DISCOUNT AMOUNT
     * FUNCTIONALITY: Converts a specific number of points into a currency discount (e.g., 100 pts = $50).
     * WHEN TO CALL: When the user types a number into the "Redeem Points" box on the frontend.
     * DETAIL: Use this to update the "Total Price" dynamically before the user clicks 'Pay'.
     */
    @GetMapping("/api/loyalty/calculate-discount/{points}")
    Double previewDiscount(@PathVariable("points") Integer points);

    /**
     * 3. PREVIEW POTENTIAL EARNINGS
     * FUNCTIONALITY: Calculates how many points a user WILL earn based on the total booking amount.
     * WHEN TO CALL: In the search results or checkout summary to show: "Book this and earn X points!"
     */
    @GetMapping("/api/loyalty/calculate-points/{amount}")
    Integer previewPoints(@PathVariable("amount") Double amount);

    /**
     * 4. REDEEM POINTS (PRE-PAYMENT)
     * FUNCTIONALITY: Deducts points from the user's balance and creates a redemption record in the DB.
     * WHEN TO CALL: Immediately after the user clicks "Proceed to Payment" but BEFORE the actual payment gateway is triggered.
     * DETAIL: This "locks" the points so they cannot be double-spent.
     */
    @PostMapping("/api/loyalty/redeem")
    ResponseEntity<String> redeemPoints(@RequestBody RedemptionRequestDto request);

    /**
     * 5. REVERT REDEMPTION (ERROR RECOVERY)
     * FUNCTIONALITY: Cancels a previous redemption and adds the points back to the user's balance.
     * WHEN TO CALL: If the payment gateway returns a 'FAILED' status or the user cancels the payment screen.
     * DETAIL: Crucial for data integrity; ensures users don't lose points for failed transactions.
     */
    @PostMapping("/api/loyalty/revert-redemption")
    ResponseEntity<String> revertRedemption(@RequestBody RedemptionRequestDto request);

    /**
     * 6. ADD PENDING POINTS (POST-PAYMENT)
     * FUNCTIONALITY: Calculates points earned from the spend and adds them to a 'Pending' status.
     * WHEN TO CALL: Right after receiving a 'SUCCESS' notification from the payment gateway.
     * DETAIL: Points are 'Pending' so the user cannot spend them until the actual stay/service is completed.
     */
    @PostMapping("/api/loyalty/add-pending-points")
    ResponseEntity<String> addPoints(@RequestBody AddPointsRequestDto request);

    /**
     * 7. CONFIRM CHECKOUT (FINALIZE POINTS)
     * FUNCTIONALITY: Moves points from 'Pending' to 'Available' balance.
     * WHEN TO CALL: After the user successfully checks out of the hotel or completes the service.
     * DETAIL: This is the final step that makes the earned points "spendable" for the next booking.
     */
    @PostMapping("/api/loyalty/confirm-checkout")
    ResponseEntity<String> confirmPointsAfterCheckout(@RequestBody AddPointsRequestDto request);

    /**
     * 8. REDEMPTION HISTORY
     * FUNCTIONALITY: Fetches a list of all historical point redemptions for a user.
     * WHEN TO CALL: On the User Profile or "My Rewards" page to show a statement of saved money.
     */
    @GetMapping("/api/loyalty/history/{userId}")
    List<RedemptionResponseDto> getRedemptionHistory(@PathVariable("userId") Long userId);

    /**
     * 9. INITIALIZE ACCOUNT
     * FUNCTIONALITY: Creates a new entry in the loyalty table for a user with 0 points.
     * WHEN TO CALL: When a new user registers on the platform or makes their very first booking attempt.
     */
    @PostMapping("/api/loyalty/initializeLoyaltyAccount/{userId}")
    ResponseEntity<String> initializeAccount(@PathVariable("userId") Long userId);

    /**
     * 10. CANCEL EARNED POINTS
     * FUNCTIONALITY: Removes points that were previously in 'Pending' status.
     * WHEN TO CALL: If a user cancels their booking (refund scenario).
     * DETAIL: Prevents users from earning points on bookings they didn't actually complete.
     */
    @PutMapping("/api/loyalty/cancel-points")
    ResponseEntity<String> reduceCancelledPoints(@RequestBody CancelPointsRequestDto request);
}
