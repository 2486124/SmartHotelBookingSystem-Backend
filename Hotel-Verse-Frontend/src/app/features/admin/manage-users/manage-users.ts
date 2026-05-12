import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { Navbar } from '../../../shared/components/navbar/navbar';
import { Footer } from '../../../shared/components/footer/footer';
import { AuthService } from '../../../core/services/auth.service';
import { User } from '../../../core/models/user.model';
import { ToastService } from '../../../shared/services/toast.service';
import { catchError, of } from 'rxjs';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-manage-users',
  imports: [FormsModule, DecimalPipe, Navbar, Footer],
  templateUrl: './manage-users.html',
  styleUrl: './manage-users.css'
})
export class ManageUsers implements OnInit {
  private authSvc = inject(AuthService);
  private toast   = inject(ToastService);

  allUsers      = signal<User[]>([]);
  displayed     = signal<User[]>([]);
  loading       = signal(true);
  search        = '';
  page          = signal(1);
  totalPages    = signal(1);
  pendingDelete = signal<User | null>(null);
  deleting      = signal(false);

  ngOnInit() {
    this.authSvc.getAllUsers()
      .pipe(catchError(() => of([])))
      .subscribe(users => {
        this.allUsers.set(users);
        this.paginate(users);
        this.loading.set(false);
      });
  }

  onSearch() {
    const q = this.search.toLowerCase();
    const filtered = !q ? this.allUsers()
      : this.allUsers().filter(u =>
          u.name.toLowerCase().includes(q) ||
          u.email.toLowerCase().includes(q));
    this.page.set(1);
    this.paginate(filtered);
  }

  paginate(list: User[]) {
    this.totalPages.set(Math.ceil(list.length / PAGE_SIZE) || 1);
    const s = (this.page() - 1) * PAGE_SIZE;
    this.displayed.set(list.slice(s, s + PAGE_SIZE));
  }

  goPage(p: number) {
    if (p < 1 || p > this.totalPages()) return;
    this.page.set(p);
    const q = this.search.toLowerCase();
    const filtered = !q ? this.allUsers()
      : this.allUsers().filter(u =>
          u.name.toLowerCase().includes(q) ||
          u.email.toLowerCase().includes(q));
    this.paginate(filtered);
  }

  pageNumbers() { return Array.from({ length: this.totalPages() }, (_, i) => i + 1); }

  confirmDelete(user: User) { this.pendingDelete.set(user); }
  cancelDelete()            { this.pendingDelete.set(null); }

  executeDelete() {
    const user = this.pendingDelete();
    if (!user) return;
    this.deleting.set(true);
    this.authSvc.deleteUser(user.userId).subscribe({
      next: () => {
        this.allUsers.update(list => list.filter(u => u.userId !== user.userId));
        this.paginate(this.allUsers());
        this.pendingDelete.set(null);
        this.deleting.set(false);
        this.toast.success(`"${user.name}" has been deleted successfully.`);
      },
      error: () => this.deleting.set(false)
    });
  }

  totalUsers()   { return this.allUsers().length; }
  guestCount()   { return this.allUsers().filter(u => u.role === 'ROLE_GUEST').length; }
  managerCount() { return this.allUsers().filter(u => u.role === 'ROLE_HOTEL_MANAGER').length; }
  pageEnd()      { return Math.min(this.page() * PAGE_SIZE, this.allUsers().length); }

  roleLabel(role: string): string {
    if (role === 'ROLE_GUEST')         return 'Guest';
    if (role === 'ROLE_HOTEL_MANAGER') return 'Manager';
    if (role === 'ROLE_ADMIN')         return 'Admin';
    return role;
  }

  initials(name: string): string {
    return name.split(' ').map(p => p[0]).join('').slice(0, 2).toUpperCase();
  }
}
