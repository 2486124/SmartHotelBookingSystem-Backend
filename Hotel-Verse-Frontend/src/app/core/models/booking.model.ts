export type BookingStatus = 'CONFIRMED' | 'CHECKED_IN' | 'CHECKED_OUT' | 'REVIEWED' | 'NOT_REVIEWED' | 'CANCELLED';
export type PaymentMethod = 'CREDIT_CARD' | 'DEBIT_CARD' | 'UPI' | 'NET_BANKING' | 'WALLET';

export interface Booking {
  bookingId: number;
  userId: number;
  roomId: number;
  hotelId: number;
  checkInDate: string;
  checkOutDate: string;
  status: BookingStatus;
  paymentId?: number;
  // enriched by backend (hotel bookings endpoint)
  userName?: string;
  totalAmount?: number;
  // enriched client-side (user bookings)
  hotelName?: string;
  hotelImageUrl?: string;
  roomType?: string;
}

export interface RazorpayOrderRequest {
  userId: number;
  roomId: number;
  hotelId: number;
  checkInDate: string;
  checkOutDate: string;
  amount: number;
  paymentMethod: PaymentMethod;
}

export interface RazorpayOrderResponse {
  keyId: string;
  razorpayOrderId: string;
  currency: string;
  amount: number;
  name?: string;
  description?: string;
}

export interface RazorpayConfirmRequest {
  userId: number;
  roomId: number;
  hotelId: number;
  checkInDate: string;
  checkOutDate: string;
  amount: number;
  paymentMethod: PaymentMethod;
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
  redeemPoints: boolean;
}

export interface BookingConfirmResponse {
  bookingId: number;
  paymentId: number;
  bookingStatus: BookingStatus;
  paymentStatus: string;
  amount: number;
  paymentMethod: PaymentMethod;
}
