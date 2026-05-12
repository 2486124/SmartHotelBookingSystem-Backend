import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Role } from '../models/user.model';

export const roleGuard = (allowed: Role[]): CanActivateFn => {
  return () => {
    const auth   = inject(AuthService);
    const router = inject(Router);
    const role   = auth.currentUser()?.role;
    if (role && allowed.includes(role)) return true;
    return router.createUrlTree(['/login']);
  };
};
