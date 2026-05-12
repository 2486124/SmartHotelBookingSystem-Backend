import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { Navbar } from '../../../shared/components/navbar/navbar';
import { Footer } from '../../../shared/components/footer/footer';
import { StarRating } from '../../../shared/components/star-rating/star-rating';
import { HotelService } from '../../../core/services/hotel.service';
import { ReviewService } from '../../../core/services/review.service';
import { LoyaltyService } from '../../../core/services/loyalty.service';
import { RazorpayService, PaymentParams } from '../../../core/services/razorpay.service';
import { AuthService } from '../../../core/services/auth.service';
import { HotelResponse, RoomResponse } from '../../../core/models/hotel.model';
import { ReviewResponseDTO } from '../../../core/models/review.model';
import { BookingConfirmResponse, PaymentMethod } from '../../../core/models/booking.model';

interface BookingResult extends BookingConfirmResponse {
  hotelName: string;
  roomType:  string;
  checkIn:   string;
  checkOut:  string;
  nights:    number;
  redeemed:  boolean;
}

@Component({
  selector: 'app-hotel-detail',
  imports: [RouterLink, FormsModule, DecimalPipe, Navbar, Footer, StarRating],
  templateUrl: './hotel-detail.html',
  styleUrl: './hotel-detail.css'
})
export class HotelDetail implements OnInit {
  private route      = inject(ActivatedRoute);
  private hotelSvc   = inject(HotelService);
  private reviewSvc  = inject(ReviewService);
  private loyaltySvc = inject(LoyaltyService);
  private razorSvc   = inject(RazorpayService);
  private auth       = inject(AuthService);

  hotel       = signal<HotelResponse | null>(null);
  rooms       = signal<RoomResponse[]>([]);
  allRooms    = signal<RoomResponse[]>([]); // source of truth for the type dropdown
  reviews     = signal<ReviewResponseDTO[]>([]);
  loading        = signal(true);
  payingRoomId   = signal<number | null>(null);
  payError       = signal('');
  bookingResult  = signal<BookingResult | null>(null);
  loyaltyBal     = signal(0);
  pendingRoom    = signal<RoomResponse | null>(null);

  checkIn   = '';
  checkOut  = '';
  roomType  = '';
  redeemPts = false;
  minDate   = '';

  private hotelId = 0;

  ngOnInit() {
    this.hotelId = Number(this.route.snapshot.paramMap.get('id'));
    this.hotelSvc.getHotelById(this.hotelId).subscribe(h => {
      this.hotel.set(h);
      this.loading.set(false);
    });
    this.hotelSvc.getRoomsForHotel(this.hotelId).subscribe(r => {
      this.rooms.set(r);
      this.allRooms.set(r);
    });
    this.reviewSvc.getHotelReviews(this.hotelId).pipe(catchError(() => of([])))
      .subscribe(r => this.reviews.set(r));
    this.loyaltySvc.getBalance().pipe(catchError(() => of(0)))
      .subscribe(b => this.loyaltyBal.set(b));

    const tomorrow = new Date(); tomorrow.setDate(tomorrow.getDate() + 1);
    const checkOut = new Date(tomorrow); checkOut.setDate(tomorrow.getDate() + 3);
    this.checkIn  = tomorrow.toISOString().split('T')[0];
    this.checkOut = checkOut.toISOString().split('T')[0];
    this.minDate  = tomorrow.toISOString().split('T')[0];
  }

  roomTypes(): string[] {
    return [...new Set(this.allRooms().map(r => r.type))];
  }

  updateSearch() {
    if (this.checkIn && this.checkOut) {
      this.hotelSvc.getAvailableRooms(this.hotelId, this.checkIn, this.checkOut, this.roomType || undefined)
        .subscribe(r => this.rooms.set(r));
    } else {
      this.rooms.set(
        this.roomType ? this.allRooms().filter(r => r.type === this.roomType) : this.allRooms()
      );
    }
  }

  previewNights(): number {
    if (!this.checkIn || !this.checkOut) return 1;
    return Math.max(1, Math.ceil(
      (new Date(this.checkOut).getTime() - new Date(this.checkIn).getTime()) / 86400000
    ));
  }

  previewTotal(): number {
    const room = this.pendingRoom();
    if (!room) return 0;
    const base = Number(room.price) * this.previewNights();
    return this.redeemPts && this.loyaltyBal() >= 300 ? +(base * 0.9).toFixed(2) : base;
  }

  pay(room: RoomResponse) {
    this.redeemPts = false;
    this.payError.set('');
    this.pendingRoom.set(room);
  }

  confirmPay() {
    const room = this.pendingRoom();
    const h    = this.hotel();
    const user = this.auth.currentUser();
    if (!room || !h || !user) return;

    this.pendingRoom.set(null);
    this.payingRoomId.set(room.roomId);
    this.bookingResult.set(null);

    const nights   = this.previewNights();
    const redeemed = this.redeemPts && this.loyaltyBal() >= 300;
    const amount   = Number(room.price) * nights;

    const params: PaymentParams = {
      userId:        user.id,
      roomId:        room.roomId,
      hotelId:       h.hotelId,
      checkInDate:   this.checkIn,
      checkOutDate:  this.checkOut,
      amount,
      paymentMethod: 'UPI' as PaymentMethod,
      redeemPoints:  redeemed,
      hotelName:     h.name
    };

    this.razorSvc.initiatePayment(params).subscribe({
      next: (res) => {
        this.payingRoomId.set(null);
        this.bookingResult.set({
          ...res,
          hotelName: h.name,
          roomType:  room.type,
          checkIn:   this.checkIn,
          checkOut:  this.checkOut,
          nights,
          redeemed
        });
      },
      error: (e) => {
        this.payingRoomId.set(null);
        this.payError.set(e?.message || 'Payment failed. Please try again.');
      }
    });
  }

  closeBookingResult() { this.bookingResult.set(null); }

  amenities(str: string): string[] {
    return (str || '').split(',').map(a => a.trim()).filter(Boolean);
  }

  amenityIcon(name: string): string {
    const n = name.toLowerCase();
    if (n.includes('pool'))    return '🏊';
    if (n.includes('spa'))     return '🧖';
    if (n.includes('gym'))     return '🏋️';
    if (n.includes('dining') || n.includes('restaurant')) return '🍽️';
    if (n.includes('wifi'))    return '📶';
    if (n.includes('parking')) return '🅿️';
    if (n.includes('shuttle')) return '🚌';
    if (n.includes('laundry')) return '👔';
    if (n.includes('business')) return '💼';
    if (n.includes('concierge')) return '🛎️';
    return '✨';
  }

  timeAgo(ts: string): string {
    const diff = Date.now() - new Date(ts).getTime();
    const days = Math.floor(diff / 86400000);
    if (days < 1)  return 'today';
    if (days === 1) return '1 day ago';
    if (days < 30)  return `${days} days ago`;
    const months = Math.floor(days / 30);
    return months === 1 ? '1 month ago' : `${months} months ago`;
  }

  avgRating(): number {
    const r = this.reviews();
    if (!r.length) return 0;
    return r.reduce((s, x) => s + x.rating, 0) / r.length;
  }

  initials(name: string): string {
    return (name || 'U').split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
  }

  imgError(e: Event) {
    (e.target as HTMLImageElement).src = 'https://placehold.co/200x160/1a237e/fff?text=Room';
  }
}
