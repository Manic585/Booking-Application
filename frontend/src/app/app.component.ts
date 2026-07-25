import { Component } from '@angular/core';

import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth/auth.service';

@Component({
    selector: 'app-root',
    imports: [RouterOutlet, RouterLink],
    template: `
    @if (auth.isAuthenticated()) {
      <nav class="navbar">
        <a class="brand" routerLink="/search">🎫 BookIt</a>
        <div class="nav-links">
          <a routerLink="/search" routerLinkActive="active">Search</a>
          <a routerLink="/dashboard" routerLinkActive="active">My Bookings</a>
          <a routerLink="/ai-assistant" routerLinkActive="active" class="ai-link">🤖 AI Assistant</a>
          <button class="logout-btn" (click)="auth.logout()">Sign Out</button>
        </div>
      </nav>
    }
    <router-outlet />
    `,
    styles: [`
    .navbar { display:flex; justify-content:space-between; align-items:center;
              padding:.75rem 2rem; background:#1976d2; color:white; }
    .brand { color:white; text-decoration:none; font-size:1.25rem; font-weight:700; }
    .nav-links { display:flex; align-items:center; gap:1.5rem; }
    .nav-links a { color:rgba(255,255,255,.85); text-decoration:none; }
    .nav-links a.active, .nav-links a:hover { color:white; }
    .logout-btn { padding:.375rem .75rem; background:rgba(255,255,255,.15);
                  color:white; border:1px solid rgba(255,255,255,.4);
                  border-radius:4px; cursor:pointer; }
    .logout-btn:hover { background:rgba(255,255,255,.25); }
    .ai-link { background:rgba(255,255,255,.2); padding:.25rem .625rem; border-radius:12px; }
    .ai-link:hover { background:rgba(255,255,255,.35); }
  `]
})
export class AppComponent {
  constructor(public auth: AuthService, private router: Router) {}
}
