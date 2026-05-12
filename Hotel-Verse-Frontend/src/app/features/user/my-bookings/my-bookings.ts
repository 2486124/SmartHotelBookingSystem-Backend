import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TitleCasePipe } from '@angular/common';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Navbar } from '../../../shared/components/navbar/navbar';
import { Footer } from '../../../shared/components/footer/footer';
import { StarRating } from '../../../shared/components/star-rating/star-rating';
import { BookingService } from '../../../core/services/booking.service';
import { ReviewService } from '../../../core/services/review.service';
import { HotelService } from '../../../core/services/hotel.service';
import { AuthService } from '../../../core/services/auth.service';
import { Booking } from '../../../core/models/booking.model';

interface BookingUI extends Booking {
  hotelName: string;
  hotelImageUrl: string;
  roomType: string;
  reviewText?: string;
  reviewRating?: number;
  reviewSubmitting?: boolean;
  reviewSubmitted?: boolean;
}

@Component({
  selector: 'app-my-bookings',
  imports: [FormsModule, RouterLink, TitleCasePipe, Navbar, Footer, StarRating],
  templateUrl: './my-bookings.html',
  styleUrl: './my-bookings.css'
})
export class MyBookings implements OnInit {
  private bookingSvc = inject(BookingService);
  private reviewSvc  = inject(ReviewService);
  private hotelSvc   = inject(HotelService);
  private auth       = inject(AuthService);

  current      = signal<BookingUI[]>([]);
  previous     = signal<BookingUI[]>([]);
  loading      = signal(true);
  cancellingId = signal<number | null>(null);
  error        = signal('');
  pendingCancel = signal<BookingUI | null>(null);

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.bookingSvc.getMyBookings().subscribe({
      next: async (bookings) => {
        const enriched = await this.enrich(bookings);
        this.current.set(enriched.filter(b =>
          ['CONFIRMED', 'CHECKED_IN'].includes(b.status)
        ));
        this.previous.set(enriched.filter(b =>
          ['CHECKED_OUT', 'REVIEWED', 'CANCELLED'].includes(b.status)
        ));
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  private enrich(bookings: Booking[]): Promise<BookingUI[]> {
    if (!bookings.length) return Promise.resolve([]);

    const uniqueIds = [...new Set(bookings.map(b => b.hotelId))];
    const calls = uniqueIds.map(id =>
      this.hotelSvc.getHotelById(id).pipe(catchError(() => of(null)))
    );

    return new Promise(resolve => {
      forkJoin(calls).subscribe(hotels => {
        const map = new Map<number, { name: string; imageUrl: string }>();
        hotels.forEach((h, i) => {
          if (h) map.set(uniqueIds[i], { name: h.name, imageUrl: h.imageUrl || '' });
        });
        resolve(bookings.map(b => ({
          ...b,
          hotelName:    map.get(b.hotelId)?.name     || `Hotel #${b.hotelId}`,
          hotelImageUrl: map.get(b.hotelId)?.imageUrl || '',
          roomType:     `Room #${b.roomId}`,
          reviewRating: 4,
          reviewText:   ''
        })));
      });
    });
  }

  cancel(b: BookingUI) {
    this.pendingCancel.set(b);
  }

  confirmCancel() {
    const b = this.pendingCancel();
    if (!b) return;
    this.pendingCancel.set(null);
    this.cancellingId.set(b.bookingId);
    this.bookingSvc.cancelBooking(b.bookingId).subscribe({
      next: () => { this.cancellingId.set(null); this.load(); },
      error: () => { this.cancellingId.set(null); }
    });
  }

  submitReview(b: BookingUI) {
    const user = this.auth.currentUser();
    if (!user || !b.reviewText?.trim()) return;
    b.reviewSubmitting = true;
    this.reviewSvc.submitReview({
      userId:    user.id,
      hotelId:   b.hotelId,
      rating:    b.reviewRating ?? 4,
      comment:   b.reviewText,
      bookingId: b.bookingId
    }).subscribe({
      next: () => { b.reviewSubmitting = false; b.reviewSubmitted = true; },
      error: () => { b.reviewSubmitting = false; }
    });
  }

  setRating(b: BookingUI, r: number) { b.reviewRating = r; }

  formatId(id: number): string {
    return `HV-${String(id).padStart(5, '0')}`;
  }

  imgError(e: Event, size = '200x160') {
    (e.target as HTMLImageElement).src =
      `https://placehold.co/${size}/1a237e/fff?text=Hotel`;
  }
}
