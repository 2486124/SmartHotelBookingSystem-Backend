import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Booking, BookingConfirmResponse,
  RazorpayConfirmRequest, RazorpayOrderRequest, RazorpayOrderResponse
} from '../models/booking.model';

@Injectable({ providedIn: 'root' })
export class BookingService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  // ── Guest ────────────────────────────────────────────────────
  createOrder(req: RazorpayOrderRequest): Observable<RazorpayOrderResponse> {
    return this.http.post<RazorpayOrderResponse>(`${this.base}/api/bookings/create-order`, req);
  }

  confirmBooking(req: RazorpayConfirmRequest): Observable<BookingConfirmResponse> {
    return this.http.post<BookingConfirmResponse>(`${this.base}/api/bookings/confirm`, req);
  }

  getMyBookings(): Observable<Booking[]> {
    return this.http.get<Booking[]>(`${this.base}/api/bookings/user`);
  }

  getBookingById(id: number): Observable<Booking> {
    return this.http.get<Booking>(`${this.base}/api/bookings/get/${id}`);
  }

  cancelBooking(id: number): Observable<string> {
    return this.http.patch(`${this.base}/api/bookings/cancel/${id}`, {}, { responseType: 'text' });
  }

  getCheckedOutBookings(userId: number): Observable<Booking[]> {
    return this.http.get<Booking[]>(`${this.base}/api/bookings/status/${userId}`);
  }

  // ── Manager ─────────────────────────────────────────────────
  getBookingsForHotel(hotelId: number): Observable<Booking[]> {
    return this.http.get<Booking[]>(`${this.base}/api/bookings/hotel/${hotelId}`);
  }

  checkIn(id: number): Observable<string> {
    return this.http.patch(`${this.base}/api/bookings/checked-in/${id}`, {}, { responseType: 'text' });
  }

  checkOut(id: number): Observable<string> {
    return this.http.patch(`${this.base}/api/bookings/checked-out/${id}`, {}, { responseType: 'text' });
  }

  // ── Admin ────────────────────────────────────────────────────
  getAllBookings(): Observable<Booking[]> {
    return this.http.get<Booking[]>(`${this.base}/api/bookings`);
  }
}
