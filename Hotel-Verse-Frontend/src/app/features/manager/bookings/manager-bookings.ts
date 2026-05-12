import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe, TitleCasePipe } from '@angular/common';
import { Navbar } from '../../../shared/components/navbar/navbar';
import { Footer } from '../../../shared/components/footer/footer';
import { HotelService } from '../../../core/services/hotel.service';
import { BookingService } from '../../../core/services/booking.service';
import { Booking } from '../../../core/models/booking.model';
import { catchError, of } from 'rxjs';

const PAGE_SIZE = 8;

@Component({
  selector: 'app-manager-bookings',
  imports: [FormsModule, DecimalPipe, TitleCasePipe, Navbar, Footer],
  templateUrl: './manager-bookings.html',
  styleUrl: './manager-bookings.css'
})
export class ManagerBookings implements OnInit {
  private hotelSvc   = inject(HotelService);
  private bookingSvc = inject(BookingService);

  hotelId     = signal(0);
  allBookings = signal<Booking[]>([]);
  displayed   = signal<Booking[]>([]);
  loading     = signal(true);
  search      = '';
  page        = signal(1);
  totalPages  = signal(1);

  totalBookings  = signal(0);
  activeBookings = signal(0);
  todayCheckIns  = signal(0);
  todayCheckOuts = signal(0);

  actionLoadingId = signal<number | null>(null);

  private roomTypeMap  = new Map<number, string>();
  private roomPriceMap = new Map<number, number>();

  ngOnInit() {
    this.hotelSvc.getMyHotel().subscribe(h => {
      this.hotelId.set(h.hotelId);
      this.hotelSvc.getRoomsForHotel(h.hotelId)
        .pipe(catchError(() => of([])))
        .subscribe(rooms => rooms.forEach(r => {
          this.roomTypeMap.set(r.roomId, r.type);
          this.roomPriceMap.set(r.roomId, r.price);
        }));
      this.loadBookings(h.hotelId);
    });
  }

  loadBookings(hotelId: number) {
    this.loading.set(true);
    this.bookingSvc.getBookingsForHotel(hotelId)
      .pipe(catchError(() => of([])))
      .subscribe(b => {
        this.allBookings.set(b);
        this.computeStats(b);
        this.page.set(1);
        this.paginate(b);
        this.loading.set(false);
      });
  }

  private computeStats(bookings: Booking[]) {
    const today = new Date().toISOString().split('T')[0];
    this.totalBookings.set(bookings.length);
    this.activeBookings.set(bookings.filter(b =>
      b.status === 'CONFIRMED' || b.status === 'CHECKED_IN'
    ).length);
    this.todayCheckIns.set(bookings.filter(b => b.checkInDate === today).length);
    this.todayCheckOuts.set(bookings.filter(b => b.checkOutDate === today).length);
  }

  onSearch() {
    const q = this.search.toLowerCase().trim();
    const filtered = !q
      ? this.allBookings()
      : this.allBookings().filter(b =>
          String(b.bookingId).includes(q) ||
          this.roomType(b).toLowerCase().includes(q) ||
          this.guestName(b).toLowerCase().includes(q)
        );
    this.page.set(1);
    this.paginate(filtered);
  }

  paginate(list: Booking[]) {
    this.totalPages.set(Math.ceil(list.length / PAGE_SIZE) || 1);
    const s = (this.page() - 1) * PAGE_SIZE;
    this.displayed.set(list.slice(s, s + PAGE_SIZE));
  }

  goPage(p: number) {
    if (p < 1 || p > this.totalPages()) return;
    this.page.set(p);
    const q = this.search.toLowerCase().trim();
    const filtered = !q ? this.allBookings()
      : this.allBookings().filter(b =>
          String(b.bookingId).includes(q) ||
          this.roomType(b).toLowerCase().includes(q) ||
          this.guestName(b).toLowerCase().includes(q)
        );
    this.paginate(filtered);
  }

  doCheckIn(b: Booking) {
    this.actionLoadingId.set(b.bookingId);
    this.bookingSvc.checkIn(b.bookingId).subscribe({
      next: () => { this.actionLoadingId.set(null); this.loadBookings(this.hotelId()); },
      error: () => this.actionLoadingId.set(null)
    });
  }

  doCheckOut(b: Booking) {
    this.actionLoadingId.set(b.bookingId);
    this.bookingSvc.checkOut(b.bookingId).subscribe({
      next: () => { this.actionLoadingId.set(null); this.loadBookings(this.hotelId()); },
      error: () => this.actionLoadingId.set(null)
    });
  }

  roomType(b: Booking): string {
    return this.roomTypeMap.get(b.roomId) || `Room #${b.roomId}`;
  }

  guestName(b: Booking): string {
    return b.userName || `Guest #${b.userId}`;
  }

  totalPaid(b: Booking): number | null {
    if (b.totalAmount) return b.totalAmount;
    // Fallback: compute from room price × nights
    const price = this.roomPriceMap.get(b.roomId);
    if (!price) return null;
    const nights = Math.max(1, Math.round(
      (new Date(b.checkOutDate).getTime() - new Date(b.checkInDate).getTime()) / 86400000
    ));
    return price * nights;
  }

  pageNumbers() { return Array.from({ length: this.totalPages() }, (_, i) => i + 1); }

  formatId(id: number) { return `#HV-${String(id).padStart(5, '0')}`; }

  avatarColor(userId: number): string {
    const colors = ['#5c6bc0', '#26a69a', '#ef6c00', '#43a047', '#8e24aa', '#00838f', '#d81b60'];
    return colors[userId % colors.length];
  }

  initials(b: Booking): string {
    const name = b.userName;
    if (name) return name.split(' ').map((w: string) => w[0]).join('').toUpperCase().slice(0, 2);
    return `G${b.userId}`.slice(0, 2).toUpperCase();
  }

  statusClass(status: string): string {
    return status.toLowerCase().replace(/_/g, '-');
  }
}
