import { Component, input } from '@angular/core';

@Component({
  selector: 'app-footer',
  templateUrl: './footer.html',
  styleUrl: './footer.css'
})
export class Footer {
  variant = input<'user' | 'manager' | 'admin'>('user');
  year = new Date().getFullYear();
}
