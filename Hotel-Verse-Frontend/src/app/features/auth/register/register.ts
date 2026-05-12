import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { Role } from '../../../core/models/user.model';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {
  private fb   = inject(FormBuilder);
  private auth = inject(AuthService);

  selectedRole = signal<'ROLE_GUEST' | 'ROLE_HOTEL_MANAGER'>('ROLE_GUEST');
  loading  = signal(false);
  error    = signal('');
  success  = signal('');
  showPass = signal(false);

  form = this.fb.group({
    name:          ['', Validators.required],
    email:         ['', [Validators.required, Validators.email]],
    password:      ['', [Validators.required, Validators.minLength(6)]],
    contactNumber: ['', Validators.required]
  });

  setRole(role: 'ROLE_GUEST' | 'ROLE_HOTEL_MANAGER') { this.selectedRole.set(role); }

  submit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.error.set('');
    const payload = { ...this.form.value, role: this.selectedRole() as Role };
    this.auth.register(payload as any).subscribe({
      next: () => {
        this.loading.set(false);
        this.success.set('Account created! Please sign in.');
        this.form.reset();
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(e?.error?.message || 'Registration failed. Please try again.');
      }
    });
  }

  get f() { return this.form.controls; }
}
