import { Component, inject } from '@angular/core';
import { ToastService, Toast } from '../../services/toast.service';

@Component({
  selector: 'app-toast',
  templateUrl: './toast.html',
  styleUrl: './toast.css'
})
export class ToastComponent {
  svc = inject(ToastService);

  trackId(_: number, t: Toast) { return t.id; }
}
