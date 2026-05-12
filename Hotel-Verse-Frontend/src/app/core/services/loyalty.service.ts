import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RedemptionResponseDto } from '../models/loyalty.model';

@Injectable({ providedIn: 'root' })
export class LoyaltyService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getBalance(): Observable<number> {
    return this.http.get<number>(`${this.base}/api/loyalty/balance`);
  }

  getHistory(): Observable<RedemptionResponseDto[]> {
    return this.http.get<RedemptionResponseDto[]>(`${this.base}/api/loyalty/history`);
  }
}
