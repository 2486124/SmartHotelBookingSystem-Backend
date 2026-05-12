import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { Navbar } from '../../../shared/components/navbar/navbar';
import { Footer } from '../../../shared/components/footer/footer';
import { ReviewService } from '../../../core/services/review.service';
import { HotelService } from '../../../core/services/hotel.service';
import { ToastService } from '../../../shared/services/toast.service';
import { ReviewResponseDTO } from '../../../core/models/review.model';
import { HotelResponse } from '../../../core/models/hotel.model';

const PAGE_SIZE = 8;

@Component({
  selector: 'app-admin-reviews',
  imports: [FormsModule, DecimalPipe, Navbar, Footer],
  templateUrl: './admin-reviews.html',
  styleUrl: './admin-reviews.css'
})
export class AdminReviews implements OnInit {
  private reviewSvc = inject(ReviewService);
  private hotelSvc  = inject(HotelService);
  private toast     = inject(ToastService);

  allReviews       = signal<ReviewResponseDTO[]>([]);
  hotels           = signal<HotelResponse[]>([]);
  loading          = signal(true);
  search           = '';
  starFilter       = signal(0);
  page             = signal(1);
  totalPages       = signal(1);
  filteredReviews  = signal<ReviewResponseDTO[]>([]);
  displayedReviews = signal<ReviewResponseDTO[]>([]);

  // Remove modal
  pendingRemove = signal<ReviewResponseDTO | null>(null);
  removing      = signal(false);

  ngOnInit() {
    this.hotelSvc.adminSearchHotels().subscribe({
      next: (hotels) => {
        const approved = hotels.filter(h => h.approval);
        this.hotels.set(approved);
        if (approved.length === 0) { this.loading.set(false); return; }

        let loaded = 0;
        const allR: ReviewResponseDTO[] = [];
        approved.forEach(hotel => {
          this.reviewSvc.getHotelReviews(hotel.hotelId).subscribe({
            next: (reviews) => {
              allR.push(...reviews);
              loaded++;
              if (loaded === approved.length) {
                this.allReviews.set(allR);
                this.applyFilters();
                this.loading.set(false);
              }
            },
            error: () => {
              loaded++;
              if (loaded === approved.length) {
                this.allReviews.set(allR);
                this.applyFilters();
                this.loading.set(false);
              }
            }
          });
        });
      },
      error: () => this.loading.set(false)
    });
  }

  applyFilters() {
    const q    = this.search.toLowerCase();
    const star = this.starFilter();

    let reviews = this.allReviews();
    if (star > 0) reviews = reviews.filter(r => Math.floor(r.rating) === star);
    if (q) {
      reviews = reviews.filter(r =>
        this.guestName(r).toLowerCase().includes(q) ||
        this.hotelName(r.hotelId).toLowerCase().includes(q)
      );
    }

    // Sort newest first
    reviews = [...reviews].sort((a, b) =>
      new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
    );

    this.filteredReviews.set(reviews);
    this.totalPages.set(Math.ceil(reviews.length / PAGE_SIZE) || 1);
    this.page.set(1);
    this.paginateReviews(reviews);
  }

  paginateReviews(reviews: ReviewResponseDTO[]) {
    const s = (this.page() - 1) * PAGE_SIZE;
    this.displayedReviews.set(reviews.slice(s, s + PAGE_SIZE));
  }

  goPage(p: number) {
    if (p < 1 || p > this.totalPages()) return;
    this.page.set(p);
    this.paginateReviews(this.filteredReviews());
  }

  pageNumbers() { return Array.from({ length: this.totalPages() }, (_, i) => i + 1); }

  setStarFilter(star: number) {
    this.starFilter.set(star);
    this.applyFilters();
  }

  // Remove modal
  confirmRemove(review: ReviewResponseDTO) {
    this.pendingRemove.set(review);
  }

  cancelRemove() {
    this.pendingRemove.set(null);
  }

  executeRemove() {
    const review = this.pendingRemove();
    if (!review) return;
    this.removing.set(true);
    this.reviewSvc.deleteReview(review.reviewId).subscribe({
      next: () => {
        this.allReviews.update(list => list.filter(r => r.reviewId !== review.reviewId));
        this.applyFilters();
        this.pendingRemove.set(null);
        this.removing.set(false);
        this.toast.success('Review removed successfully.');
      },
      error: () => {
        this.removing.set(false);
        this.pendingRemove.set(null);
      }
    });
  }

  guestName(review: ReviewResponseDTO): string {
    return review.userName || `Guest #${review.userId}`;
  }

  hotelName(hotelId: number): string {
    return this.hotels().find(h => h.hotelId === hotelId)?.name ?? `Hotel #${hotelId}`;
  }

  hotelLocation(hotelId: number): string {
    return this.hotels().find(h => h.hotelId === hotelId)?.location ?? '';
  }

  totalReviews() { return this.allReviews().length; }

  propertiesReviewed(): number {
    return new Set(this.allReviews().map(r => r.hotelId)).size;
  }

  pageEnd(): number {
    return Math.min(this.page() * PAGE_SIZE, this.filteredReviews().length);
  }

  avgRating(): number {
    const r = this.allReviews();
    if (!r.length) return 0;
    return r.reduce((s, x) => s + x.rating, 0) / r.length;
  }

  stars(rating: number): string[] {
    return Array.from({ length: 5 }, (_, i) => {
      const v = rating - i;
      return v >= 1 ? 'full' : v >= 0.5 ? 'half' : 'empty';
    });
  }

  formatDate(ts: string): string {
    return new Date(ts).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  }
}
