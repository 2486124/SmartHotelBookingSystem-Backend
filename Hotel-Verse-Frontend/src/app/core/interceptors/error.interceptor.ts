import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ToastService } from '../../shared/services/toast.service';

const SILENT_STATUSES = new Set([0, 404]);

const shouldSilence = (err: HttpErrorResponse) =>
  SILENT_STATUSES.has(err.status) ||
  (err.status >= 200 && err.status < 300) || // JSON parse failures on 2xx
  err.error instanceof SyntaxError;           // explicit parse error type

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (!shouldSilence(err)) {
        const msg =
          err.error?.message ||
          err.error?.error ||
          (typeof err.error === 'string' ? err.error : null) ||
          err.message ||
          'Something went wrong. Please try again.';
        toast.error(msg);
      }
      return throwError(() => err);
    })
  );
};
