import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ReviewRequestDTO, ReviewResponseDTO } from '../models/review.model';

@Injectable({ providedIn: 'root' })
export class ReviewService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  submitReview(req: ReviewRequestDTO): Observable<ReviewResponseDTO> {
    return this.http.post<ReviewResponseDTO>(`${this.base}/api/reviews/`, req);
  }

  getMyReviews(): Observable<ReviewResponseDTO[]> {
    return this.http.get<ReviewResponseDTO[]>(`${this.base}/api/reviews/user`);
  }

  getHotelReviews(hotelId: number): Observable<ReviewResponseDTO[]> {
    return this.http.get<ReviewResponseDTO[]>(`${this.base}/api/reviews/hotel/${hotelId}`);
  }

  getHotelRating(hotelId: number): Observable<number> {
    return this.http.get<number>(`${this.base}/api/reviews/hotel/${hotelId}/rating`);
  }

  respondToReview(reviewId: number, managerResponse: string): Observable<ReviewResponseDTO> {
    return this.http.patch<ReviewResponseDTO>(
      `${this.base}/api/reviews/${reviewId}/respond`,
      { managerResponse }
    );
  }

  deleteReview(reviewId: number): Observable<string> {
    return this.http.delete(`${this.base}/api/reviews/${reviewId}/remove`, { responseType: 'text' });
  }
}
