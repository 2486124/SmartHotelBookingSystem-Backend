import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
  id: number;
  type: ToastType;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private _id = 0;
  toasts = signal<Toast[]>([]);

  private add(type: ToastType, message: string, duration = 4500) {
    const id = ++this._id;
    this.toasts.update(list => [...list, { id, type, message }]);
    setTimeout(() => this.dismiss(id), duration);
  }

  dismiss(id: number) {
    this.toasts.update(list => list.filter(t => t.id !== id));
  }

  success(msg: string) { this.add('success', msg); }
  error(msg: string)   { this.add('error', msg, 6000); }
  warning(msg: string) { this.add('warning', msg); }
  info(msg: string)    { this.add('info', msg); }
}
