import { Component } from '@angular/core';

import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
    selector: 'app-register',
    imports: [ReactiveFormsModule, RouterLink],
    template: `
    <div class="auth-container">
      <div class="auth-card">
        <h1>Create Account</h1>
        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <div class="row">
            <div class="field">
              <label>First Name</label>
              <input formControlName="firstName" />
            </div>
            <div class="field">
              <label>Last Name</label>
              <input formControlName="lastName" />
            </div>
          </div>
          <div class="field">
            <label>Email</label>
            <input type="email" formControlName="email" />
          </div>
          <div class="field">
            <label>Password</label>
            <input type="password" formControlName="password" />
            <span class="hint">Minimum 8 characters</span>
          </div>
          @if (error) {
            <div class="error">{{ error }}</div>
          }
          <button type="submit" [disabled]="form.invalid || loading">
            {{ loading ? 'Creating account...' : 'Create Account' }}
          </button>
        </form>
        <p>Have an account? <a routerLink="/auth/login">Sign in</a></p>
      </div>
    </div>
    `,
    styles: [`
    .auth-container { display:flex; justify-content:center; align-items:center; min-height:100vh; background:#f5f5f5; }
    .auth-card { background:white; padding:2rem; border-radius:8px; box-shadow:0 2px 8px rgba(0,0,0,.1); width:400px; }
    .row { display:flex; gap:1rem; }
    .row .field { flex:1; }
    .field { margin-bottom:1rem; }
    label { display:block; margin-bottom:.25rem; font-weight:500; }
    input { width:100%; padding:.5rem; border:1px solid #ddd; border-radius:4px; box-sizing:border-box; }
    button { width:100%; padding:.75rem; background:#1976d2; color:white; border:none; border-radius:4px; cursor:pointer; }
    .hint { font-size:.75rem; color:#666; }
    .error { color:#d32f2f; font-size:.875rem; }
  `]
})
export class RegisterComponent {
  form: FormGroup;
  loading = false;
  error = '';

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {
    this.form = this.fb.group({
      firstName: ['', [Validators.required, Validators.maxLength(100)]],
      lastName: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.error = '';

    this.auth.register(this.form.value).subscribe({
      next: () => this.router.navigate(['/search']),
      error: err => {
        this.error = err.error?.detail || 'Registration failed.';
        this.loading = false;
      }
    });
  }
}
