import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { JwtResponse, LoginRequest, RegisterRequest, User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http   = inject(HttpClient);
  private router = inject(Router);
  private base   = environment.apiUrl;

  private _user = signal<JwtResponse | null>(this.loadUser());
  readonly currentUser = this._user.asReadonly();

  login(req: LoginRequest): Observable<JwtResponse> {
    return this.http.post<JwtResponse>(`${this.base}/api/auth/login`, req).pipe(
      tap(res => {
        localStorage.setItem('hv_token', res.token);
        localStorage.setItem('hv_user', JSON.stringify(res));
        this._user.set(res);
      })
    );
  }

  register(req: RegisterRequest): Observable<string> {
    return this.http.post(`${this.base}/api/auth/register`, req, { responseType: 'text' });
  }

  forgotPassword(email: string): Observable<string> {
    return this.http.post(`${this.base}/api/auth/forgot-password`, { email }, { responseType: 'text' });
  }

  logout(): void {
    localStorage.removeItem('hv_token');
    localStorage.removeItem('hv_user');
    this._user.set(null);
    this.router.navigate(['/login']);
  }

  isAuthenticated(): boolean {
    return !!this._user() && !!localStorage.getItem('hv_token');
  }

  getToken(): string | null {
    return localStorage.getItem('hv_token');
  }

  redirectByRole(): void {
    const role = this._user()?.role;
    if (role === 'ROLE_GUEST')          this.router.navigate(['/home']);
    else if (role === 'ROLE_HOTEL_MANAGER') this.router.navigate(['/manager/dashboard']);
    else if (role === 'ROLE_ADMIN')     this.router.navigate(['/admin/dashboard']);
  }

  getProfile(): Observable<User> {
    return this.http.get<User>(`${this.base}/api/auth/profile`);
  }

  updateProfile(req: { name: string; email: string; contactNumber: string; role: string; password: string }): Observable<User> {
    return this.http.put<User>(`${this.base}/api/auth/profile`, req).pipe(
      tap(updated => {
        const current = this._user();
        if (current) {
          const merged = { ...current, name: updated.name, email: updated.email };
          localStorage.setItem('hv_user', JSON.stringify(merged));
          this._user.set(merged);
        }
      })
    );
  }

  getAllUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.base}/api/auth/users`);
  }

  searchUsers(keyword: string): Observable<User[]> {
    return this.http.get<User[]>(`${this.base}/api/auth/users/search`, { params: { keyword } });
  }

  deleteUser(id: number): Observable<string> {
    return this.http.delete(`${this.base}/api/auth/users/${id}`, { responseType: 'text' });
  }

  private loadUser(): JwtResponse | null {
    try {
      const u = localStorage.getItem('hv_user');
      return u ? JSON.parse(u) : null;
    } catch { return null; }
  }
}
