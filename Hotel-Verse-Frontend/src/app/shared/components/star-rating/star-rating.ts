import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-star-rating',
  templateUrl: './star-rating.html',
  styleUrl: './star-rating.css'
})
export class StarRating {
  rating  = input<number>(0);
  max     = input<number>(5);
  size    = input<'sm' | 'md' | 'lg'>('md');
  readonly = input<boolean>(true);

  ratingChange = output<number>();

  get stars(): { filled: boolean; half: boolean }[] {
    const r = this.rating();
    return Array.from({ length: this.max() }, (_, i) => ({
      filled: i < Math.floor(r),
      half:   !Number.isInteger(r) && i === Math.floor(r)
    }));
  }

  select(i: number) {
    if (!this.readonly()) this.ratingChange.emit(i + 1);
  }
}
