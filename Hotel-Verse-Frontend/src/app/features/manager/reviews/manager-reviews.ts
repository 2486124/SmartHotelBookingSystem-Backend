import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { catchError, of } from 'rxjs';
import { Navbar } from '../../../shared/components/navbar/navbar';
import { Footer } from '../../../shared/components/footer/footer';
import { StarRating } from '../../../shared/components/star-rating/star-rating';
import { HotelService } from '../../../core/services/hotel.service';
import { ReviewService } from '../../../core/services/review.service';
import { ToastService } from '../../../shared/services/toast.service';
import { ReviewResponseDTO } from '../../../core/models/review.model';

@Component({
  selector: 'app-manager-reviews',
  imports: [FormsModule, DecimalPipe, Navbar, Footer, StarRating],
  templateUrl: './manager-reviews.html',
  styleUrl: './manager-reviews.css'
})
export class ManagerReviews implements OnInit {
  private hotelSvc  = inject(HotelService);
  private reviewSvc = inject(ReviewService);
  private toast     = inject(ToastService);

  reviews      = signal<ReviewResponseDTO[]>([]);
  loading      = signal(true);
  avgRating    = signal(0);
  totalReviews = signal(0);
  pending      = signal(0);
  responses    = signal<Record<number, string>>({});
  submitting   = signal<number | null>(null);

  ngOnInit() {
    this.hotelSvc.getMyHotel()
      .pipe(catchError(() => { this.loading.set(false); return of(null); }))
      .subscribe(h => {
        if (!h) return;
        this.reviewSvc.getHotelReviews(h.hotelId)
          .pipe(catchError(() => of([])))
          .subscribe(r => {
            this.reviews.set(r);
            this.totalReviews.set(r.length);
            this.pending.set(r.filter(x => !x.managerResponse).length);
            this.avgRating.set(r.length ? r.reduce((s, x) => s + x.rating, 0) / r.length : 0);
            const init: Record<number, string> = {};
            r.forEach(x => { init[x.reviewId] = ''; });
            this.responses.set(init);
            this.loading.set(false);
          });
      });
  }

  getResponse(id: number): string { return this.responses()[id] ?? ''; }

  setResponse(id: number, val: string) {
    this.responses.update(m => ({ ...m, [id]: val }));
  }

  submit(rev: ReviewResponseDTO) {
    const text = this.getResponse(rev.reviewId).trim();
    if (!text) return;
    this.submitting.set(rev.reviewId);
    this.reviewSvc.respondToReview(rev.reviewId, text).subscribe({
      next: (updated) => {
        this.reviews.update(list => list.map(r => r.reviewId === updated.reviewId ? updated : r));
        this.pending.update(n => Math.max(0, n - 1));
        this.submitting.set(null);
        this.toast.success('Reply submitted successfully.');
      },
      error: () => {
        this.submitting.set(null);
        this.toast.error('Failed to submit reply. Please try again.');
      }
    });
  }

  timeAgo(ts: string): string {
    const d = Math.floor((Date.now() - new Date(ts).getTime()) / 86400000);
    if (d < 1) return 'today';
    if (d === 1) return '1 day ago';
    if (d < 7)  return `${d} days ago`;
    if (d < 30) return `${Math.floor(d/7)} week${Math.floor(d/7) > 1 ? 's' : ''} ago`;
    return `${Math.floor(d/30)} month${Math.floor(d/30) > 1 ? 's' : ''} ago`;
  }

  initials(name: string) {
    return (name || 'G').split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
  }
}
