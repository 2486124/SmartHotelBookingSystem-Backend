import { Component, inject, signal, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Navbar } from '../../../shared/components/navbar/navbar';
import { Footer } from '../../../shared/components/footer/footer';
import { HotelService } from '../../../core/services/hotel.service';
import { HotelResponse } from '../../../core/models/hotel.model';

const PAGE_SIZE = 9;

@Component({
  selector: 'app-home',
  imports: [RouterLink, FormsModule, DecimalPipe, Navbar, Footer],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home implements OnInit {
  private hotelSvc = inject(HotelService);

  hotels        = signal<HotelResponse[]>([]);
  displayed     = signal<HotelResponse[]>([]);
  loading       = signal(true);
  pricesLoading = signal(false);
  priceMap      = signal<Map<number, number>>(new Map());
  page          = signal(1);
  totalPages    = signal(1);

  featuredHotels = signal<HotelResponse[]>([]);
  featuredIndex  = signal(0);

  searchName = '';
  searchLoc  = '';

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.hotelSvc.getApprovedHotels().subscribe({
      next: (list) => {
        this.hotels.set(list);
        this.totalPages.set(Math.ceil(list.length / PAGE_SIZE) || 1);
        this.paginate(list);
        // Top 5 by rating as proxy for most-booked
        const featured = [...list]
          .sort((a, b) => (b.rating || 0) - (a.rating || 0))
          .slice(0, 5);
        this.featuredHotels.set(featured);
        this.fetchPrices(featured);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  search() {
    const n = this.searchName.trim();
    const l = this.searchLoc.trim();
    if (!n && !l) { this.load(); return; }
    this.loading.set(true);
    this.hotelSvc.searchHotels(n || undefined, l || undefined).subscribe({
      next: (list) => {
        this.hotels.set(list);
        this.page.set(1);
        this.totalPages.set(Math.ceil(list.length / PAGE_SIZE) || 1);
        this.paginate(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  paginate(list: HotelResponse[]) {
    const start = (this.page() - 1) * PAGE_SIZE;
    const page  = list.slice(start, start + PAGE_SIZE);
    this.displayed.set(page);
    this.fetchPrices(page);
  }

  fetchPrices(hotels: HotelResponse[]) {
    if (!hotels.length) return;
    this.pricesLoading.set(true);

    const calls = hotels.map(h =>
      this.hotelSvc.getRoomsForHotel(h.hotelId).pipe(catchError(() => of([])))
    );

    forkJoin(calls).subscribe(results => {
      const map = new Map(this.priceMap());
      hotels.forEach((h, i) => {
        const rooms = results[i];
        if (rooms.length) {
          map.set(h.hotelId, Math.min(...rooms.map(r => r.price)));
        }
      });
      this.priceMap.set(map);
      this.pricesLoading.set(false);
    });
  }

  minPrice(hotelId: number): number | null {
    return this.priceMap().get(hotelId) ?? null;
  }

  prevSlide() {
    const len = this.featuredHotels().length;
    if (!len) return;
    this.featuredIndex.update(i => (i - 1 + len) % len);
  }

  nextSlide() {
    const len = this.featuredHotels().length;
    if (!len) return;
    this.featuredIndex.update(i => (i + 1) % len);
  }

  goToSlide(i: number) {
    this.featuredIndex.set(i);
  }

  goPage(p: number) {
    if (p < 1 || p > this.totalPages()) return;
    this.page.set(p);
    this.paginate(this.hotels());
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  pageNumbers(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i + 1);
  }

  amenityList(amenities: string): string[] {
    return (amenities || '').split(',').map(a => a.trim()).filter(Boolean).slice(0, 3);
  }

  imgError(e: Event) {
    (e.target as HTMLImageElement).src = 'https://placehold.co/400x260/1a237e/fff?text=Hotel';
  }
}
