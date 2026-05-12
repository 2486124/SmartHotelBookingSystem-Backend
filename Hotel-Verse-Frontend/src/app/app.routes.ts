import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  // ── Landing page (public, no auth required) ──────────────────
  // pathMatch:'full' is mandatory — without it path:'' is a prefix
  // match that swallows every route since all URLs start with ''.
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./features/landing/landing').then(m => m.Landing)
  },

  // ── Auth ──────────────────────────────────────────────────────
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login').then(m => m.Login)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register').then(m => m.Register)
  },

  // ── Guest / User ─────────────────────────────────────────────
  {
    path: 'home',
    canActivate: [authGuard, roleGuard(['ROLE_GUEST'])],
    loadComponent: () => import('./features/user/home/home').then(m => m.Home)
  },
  {
    path: 'hotel/:id',
    canActivate: [authGuard, roleGuard(['ROLE_GUEST'])],
    loadComponent: () => import('./features/user/hotel-detail/hotel-detail').then(m => m.HotelDetail)
  },
  {
    path: 'my-bookings',
    canActivate: [authGuard, roleGuard(['ROLE_GUEST'])],
    loadComponent: () => import('./features/user/my-bookings/my-bookings').then(m => m.MyBookings)
  },
  {
    path: 'rewards',
    canActivate: [authGuard, roleGuard(['ROLE_GUEST'])],
    loadComponent: () => import('./features/user/rewards/rewards').then(m => m.Rewards)
  },

  // ── Hotel Manager ────────────────────────────────────────────
  {
    path: 'manager',
    canActivate: [authGuard, roleGuard(['ROLE_HOTEL_MANAGER'])],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/manager/dashboard/manager-dashboard').then(m => m.ManagerDashboard)
      },
      {
        path: 'rooms',
        loadComponent: () => import('./features/manager/rooms/manager-rooms').then(m => m.ManagerRooms)
      },
      {
        path: 'bookings',
        loadComponent: () => import('./features/manager/bookings/manager-bookings').then(m => m.ManagerBookings)
      },
      {
        path: 'reviews',
        loadComponent: () => import('./features/manager/reviews/manager-reviews').then(m => m.ManagerReviews)
      }
    ]
  },

  // ── Admin ─────────────────────────────────────────────────────
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard(['ROLE_ADMIN'])],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/admin/dashboard/admin-dashboard').then(m => m.AdminDashboard)
      },
      {
        path: 'hotels',
        loadComponent: () => import('./features/admin/manage-hotels/manage-hotels').then(m => m.ManageHotels)
      },
      {
        path: 'users',
        loadComponent: () => import('./features/admin/manage-users/manage-users').then(m => m.ManageUsers)
      },
      {
        path: 'reviews',
        loadComponent: () => import('./features/admin/reviews/admin-reviews').then(m => m.AdminReviews)
      }
    ]
  },

  { path: '**', redirectTo: '/login' }
];
