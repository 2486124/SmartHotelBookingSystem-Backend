import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { Navbar } from '../../../shared/components/navbar/navbar';
import { Footer } from '../../../shared/components/footer/footer';
import { HotelService } from '../../../core/services/hotel.service';
import { HotelResponse } from '../../../core/models/hotel.model';
import { ToastService } from '../../../shared/services/toast.service';
import { catchError, of } from 'rxjs';

const PAGE_SIZE = 8;

@Component({
  selector: 'app-manage-hotels',
  imports: [FormsModule, DecimalPipe, Navbar, Footer],
  templateUrl: './manage-hotels.html',
  styleUrl: './manage-hotels.css'
})
export class ManageHotels implements OnInit {
  private hotelSvc = inject(HotelService);
  private toast    = inject(ToastService);

  allHotels     = signal<HotelResponse[]>([]);
  displayed     = signal<HotelResponse[]>([]);
  loading       = signal(true);
  search        = '';
  page          = signal(1);
  totalPages    = signal(1);
  pendingDelete = signal<HotelResponse | null>(null);
  deleting      = signal(false);

  ngOnInit() {
    this.hotelSvc.adminSearchHotels()
      .pipe(catchError(() => of([])))
      .subscribe(h => {
        const approved = h.filter(x => x.approval);
        this.allHotels.set(approved);
        this.paginate(approved);
        this.loading.set(false);
      });
  }

  onSearch() {
    const q = this.search.toLowerCase().trim();
    const filtered = !q ? this.allHotels()
      : this.allHotels().filter(h =>
          h.name.toLowerCase().includes(q) || h.location.toLowerCase().includes(q));
    this.page.set(1);
    this.paginate(filtered);
  }

  paginate(list: HotelResponse[]) {
    this.totalPages.set(Math.ceil(list.length / PAGE_SIZE) || 1);
    const s = (this.page() - 1) * PAGE_SIZE;
    this.displayed.set(list.slice(s, s + PAGE_SIZE));
  }

  goPage(p: number) {
    if (p < 1 || p > this.totalPages()) return;
    this.page.set(p);
    const q = this.search.toLowerCase().trim();
    const filtered = !q ? this.allHotels()
      : this.allHotels().filter(h =>
          h.name.toLowerCase().includes(q) || h.location.toLowerCase().includes(q));
    this.paginate(filtered);
  }

  pageNumbers() { return Array.from({ length: this.totalPages() }, (_, i) => i + 1); }

  confirmDelete(hotel: HotelResponse) {
    this.pendingDelete.set(hotel);
  }

  cancelDelete() {
    this.pendingDelete.set(null);
  }

  executeDelete() {
    const hotel = this.pendingDelete();
    if (!hotel) return;
    this.deleting.set(true);
    this.hotelSvc.adminDeleteHotel(hotel.hotelId).subscribe({
      next: () => {
        this.allHotels.update(list => list.filter(h => h.hotelId !== hotel.hotelId));
        this.paginate(this.allHotels());
        this.pendingDelete.set(null);
        this.deleting.set(false);
        this.toast.success(`"${hotel.name}" has been deleted successfully.`);
      },
      error: () => this.deleting.set(false)
    });
  }

  imgError(e: Event) {
    (e.target as HTMLImageElement).src = 'https://placehold.co/72x56/1a237e/fff?text=H';
  }
}
