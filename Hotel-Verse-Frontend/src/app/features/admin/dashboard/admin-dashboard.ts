import { Component, inject, signal, OnInit } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Navbar } from '../../../shared/components/navbar/navbar';
import { Footer } from '../../../shared/components/footer/footer';
import { HotelService } from '../../../core/services/hotel.service';
import { BookingService } from '../../../core/services/booking.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../shared/services/toast.service';
import { HotelResponse } from '../../../core/models/hotel.model';

@Component({
  selector: 'app-admin-dashboard',
  imports: [DecimalPipe, Navbar, Footer],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.css'
})
export class AdminDashboard implements OnInit {
  private hotelSvc   = inject(HotelService);
  private bookingSvc = inject(BookingService);
  private authSvc    = inject(AuthService);
  private toast      = inject(ToastService);

  totalListings   = signal(0);
  bookingsMonth   = signal(0);
  totalUsers      = signal(0);
  pendingHotels   = signal<HotelResponse[]>([]);
  loading         = signal(true);
  pendingDeny     = signal<HotelResponse | null>(null);

  ngOnInit() {
    this.hotelSvc.adminSearchHotels().subscribe(hotels => {
      this.totalListings.set(hotels.filter(h => h.approval).length);
      this.pendingHotels.set(hotels.filter(h => !h.approval));
      this.loading.set(false);
    });
    this.bookingSvc.getAllBookings().subscribe(b => this.bookingsMonth.set(b.length));
    this.authSvc.getAllUsers().subscribe(u => this.totalUsers.set(u.length));
  }

  approve(hotel: HotelResponse) {
    this.hotelSvc.approveHotel(hotel.hotelId).subscribe({
      next: () => {
        this.pendingHotels.update(list => list.filter(h => h.hotelId !== hotel.hotelId));
        this.totalListings.update(n => n + 1);
        this.toast.success(`"${hotel.name}" has been approved and is now live.`);
      },
      error: () => this.toast.error('Failed to approve hotel. Please try again.')
    });
  }

  confirmDeny(hotel: HotelResponse) {
    this.pendingDeny.set(hotel);
  }

  cancelDeny() {
    this.pendingDeny.set(null);
  }

  executeDeny() {
    const hotel = this.pendingDeny();
    if (!hotel) return;
    this.pendingDeny.set(null);
    this.hotelSvc.adminDeleteHotel(hotel.hotelId).subscribe({
      next: () => {
        this.pendingHotels.update(list => list.filter(h => h.hotelId !== hotel.hotelId));
        this.toast.success(`"${hotel.name}" has been denied and removed.`);
      },
      error: () => this.toast.error('Failed to deny hotel. Please try again.')
    });
  }

  amenityList(s: string): string[] {
    return (s || '').split(',').map(a => a.trim()).filter(Boolean);
  }

  imgError(e: Event) {
    (e.target as HTMLImageElement).src = 'https://placehold.co/240x180/1a237e/fff?text=Hotel';
  }
}
