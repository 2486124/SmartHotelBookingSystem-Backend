export interface RedemptionResponseDto {
  redemptionId?: number;
  userId: number;
  bookingId?: number;
  pointsUsed: number;        // backend field name (was: pointsRedeemed)
  discountApplied: number;
  redemptionDate?: string;   // backend field name (was: date)
  status?: string;
}
