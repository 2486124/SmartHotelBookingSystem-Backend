import { Component, inject, signal, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DecimalPipe, DatePipe } from '@angular/common';
import { Navbar } from '../../../shared/components/navbar/navbar';
import { Footer } from '../../../shared/components/footer/footer';
import { LoyaltyService } from '../../../core/services/loyalty.service';
import { RedemptionResponseDto } from '../../../core/models/loyalty.model';

@Component({
  selector: 'app-rewards',
  imports: [RouterLink, DecimalPipe, DatePipe, Navbar, Footer],
  templateUrl: './rewards.html',
  styleUrl: './rewards.css'
})
export class Rewards implements OnInit {
  private loyaltySvc = inject(LoyaltyService);

  balance  = signal(0);
  history  = signal<RedemptionResponseDto[]>([]);
  loading  = signal(true);

  totalRedeemed = signal(0);
  totalSavings  = signal(0);

  ngOnInit() {
    this.loyaltySvc.getBalance().subscribe(b => { this.balance.set(b); this.loading.set(false); });
    this.loyaltySvc.getHistory().subscribe(h => {
      this.history.set(h);
      this.totalRedeemed.set(h.reduce((s, x) => s + (x.pointsUsed || 0), 0));
      this.totalSavings.set(h.reduce((s, x) => s + (x.discountApplied || 0), 0));
    });
  }
}
