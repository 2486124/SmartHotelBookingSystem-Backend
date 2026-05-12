import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { BookingService } from './booking.service';
import {
  BookingConfirmResponse, PaymentMethod,
  RazorpayConfirmRequest, RazorpayOrderRequest, RazorpayOrderResponse
} from '../models/booking.model';

declare var Razorpay: any;

export interface PaymentParams {
  userId: number;
  roomId: number;
  hotelId: number;
  checkInDate: string;
  checkOutDate: string;
  amount: number;
  paymentMethod: PaymentMethod;
  redeemPoints: boolean;
  hotelName: string;
}

@Injectable({ providedIn: 'root' })
export class RazorpayService {
  private bookingSvc = inject(BookingService);

  initiatePayment(params: PaymentParams): Observable<BookingConfirmResponse> {
    return new Observable(observer => {
      const orderReq: RazorpayOrderRequest = {
        userId:        params.userId,
        roomId:        params.roomId,
        hotelId:       params.hotelId,
        checkInDate:   params.checkInDate,
        checkOutDate:  params.checkOutDate,
        amount:        params.amount,
        paymentMethod: params.paymentMethod
      };

      this.bookingSvc.createOrder(orderReq).subscribe({
        next: (order: RazorpayOrderResponse) => {
          const options = {
            key:         order.keyId,
            amount:      order.amount,
            currency:    order.currency || 'INR',
            name:        'HotelVerse',
            description: `Booking at ${params.hotelName}`,
            order_id:    order.razorpayOrderId,
            handler: (response: any) => {
              const confirmReq: RazorpayConfirmRequest = {
                userId:            params.userId,
                roomId:            params.roomId,
                hotelId:           params.hotelId,
                checkInDate:       params.checkInDate,
                checkOutDate:      params.checkOutDate,
                amount:            params.amount,
                paymentMethod:     params.paymentMethod,
                razorpayOrderId:   response.razorpay_order_id,
                razorpayPaymentId: response.razorpay_payment_id,
                razorpaySignature: response.razorpay_signature,
                redeemPoints:      params.redeemPoints
              };
              this.bookingSvc.confirmBooking(confirmReq).subscribe({
                next: (res) => { observer.next(res); observer.complete(); },
                error: (err) => observer.error(err)
              });
            },
            prefill: { name: '', email: '', contact: '' },
            theme:   { color: '#1a237e' },
            modal: {
              ondismiss: () => observer.error({ message: 'Payment cancelled by user.' })
            }
          };

          try {
            const rzp = new Razorpay(options);
            rzp.on('payment.failed', (resp: any) => observer.error(resp.error));
            rzp.open();
          } catch (e) {
            observer.error({ message: 'Razorpay SDK not loaded. Please refresh the page.' });
          }
        },
        error: (err) => observer.error(err)
      });
    });
  }
}
