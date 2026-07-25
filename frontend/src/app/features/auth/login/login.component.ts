import { Component } from '@angular/core';

import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
    selector: 'app-login',
    imports: [ReactiveFormsModule, RouterLink],
    template: `
    <div class="auth-container">
      <div class="auth-card">
        <h1>Sign In</h1>
        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <div class="field">
            <label>Email</label>
            <input type="email" formControlName="email" placeholder="you@example.com" />
            @if (form.get('email')?.invalid && form.get('email')?.touched) {
              <span class="error">
                Valid email required
              </span>
            }
          </div>
          <div class="field">
            <label>Password</label>
            <input type="password" formControlName="password" placeholder="••••••••" />
          </div>
          @if (error) {
            <div class="error">{{ error }}</div>
          }
          <button type="submit" [disabled]="form.invalid || loading">
            {{ loading ? 'Signing in...' : 'Sign In' }}
          </button>
        </form>
        <p>No account? <a routerLink="/auth/register">Register</a></p>
      </div>
    </div>
    `,
    styles: [`
    .auth-container { display:flex; justify-content:center; align-items:center; min-height:100vh; background:#f5f5f5; }
    .auth-card { background:white; padding:2rem; border-radius:8px; box-shadow:0 2px 8px rgba(0,0,0,.1); width:360px; }
    h1 { margin-bottom:1.5rem; font-size:1.5rem; }
    .field { margin-bottom:1rem; }
    label { display:block; margin-bottom:.25rem; font-weight:500; }
    input { width:100%; padding:.5rem; border:1px solid #ddd; border-radius:4px; box-sizing:border-box; }
    button { width:100%; padding:.75rem; background:#1976d2; color:white; border:none; border-radius:4px; cursor:pointer; }
    button:disabled { opacity:.6; }
    .error { color:#d32f2f; font-size:.875rem; margin:.5rem 0; }
  `]
})
export class LoginComponent {
  form: FormGroup;
  loading = false;
  error = '';

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.error = '';

    this.auth.login(this.form.value).subscribe({
      next: () => this.router.navigate(['/search']),
      error: err => {
        this.error = err.error?.detail || 'Login failed. Please check your credentials.';
        this.loading = false;
      }
    });
  }
}
