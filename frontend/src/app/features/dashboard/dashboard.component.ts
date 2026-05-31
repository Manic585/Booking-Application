import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { BookingService } from '../../core/services/booking.service';
import { AuthService } from '../../core/auth/auth.service';
import { Booking } from '../../core/models/booking.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page">
      <header class="page-header">
        <h1>My Bookings</h1>
        <a routerLink="/search" class="new-booking-btn">+ New Booking</a>
      </header>

      <div class="loading" *ngIf="loading()">Loading your bookings...</div>

      <div class="empty" *ngIf="!loading() && bookings().length === 0">
        <p>No bookings yet.</p>
        <a routerLink="/search">Search and book now →</a>
      </div>

      <div class="bookings-list" *ngIf="bookings().length > 0">
        <div class="booking-card" *ngFor="let b of bookings()">
          <div class="booking-header">
            <span class="booking-type">{{ bookingTypeIcon(b.bookingType) }} {{ b.bookingType }}</span>
            <span class="status-badge" [class]="'status-' + b.status.toLowerCase()">
              {{ b.status }}
            </span>
          </div>
          <div class="booking-body">
            <div class="detail">
              <span class="detail-label">Booking ID</span>
              <span class="detail-value mono">{{ b.id | slice:0:8 }}…</span>
            </div>
            <div class="detail">
              <span class="detail-label">Date</span>
              <span class="detail-value">{{ b.bookingDate }}</span>
            </div>
            <div class="detail">
              <span class="detail-label">Amount</span>
              <span class="detail-value">\${{ b.totalAmount | number:'1.2-2' }}</span>
            </div>
            <div class="detail">
              <span class="detail-label">Booked on</span>
              <span class="detail-value">{{ b.createdAt | date:'mediumDate' }}</span>
            </div>
          </div>
          <div class="booking-footer">
            <span class="failure-reason" *ngIf="b.failureReason">⚠️ {{ b.failureReason }}</span>
            <button class="cancel-btn"
                    *ngIf="canCancel(b.status)"
                    (click)="cancel(b.id)"
                    [disabled]="cancelling() === b.id">
              {{ cancelling() === b.id ? 'Cancelling...' : 'Cancel' }}
            </button>
          </div>
        </div>
      </div>

      <div class="pagination" *ngIf="totalPages() > 1">
        <button (click)="prevPage()" [disabled]="page() === 0">← Prev</button>
        <span>Page {{ page() + 1 }} of {{ totalPages() }}</span>
        <button (click)="nextPage()" [disabled]="page() + 1 >= totalPages()">Next →</button>
      </div>
    </div>
  `,
  styles: [`
    .page { max-width:800px; margin:2rem auto; padding:0 1rem; }
    .page-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:2rem; }
    .new-booking-btn { padding:.5rem 1rem; background:#1976d2; color:white; text-decoration:none; border-radius:4px; }
    .loading, .empty { text-align:center; color:#888; padding:3rem; }
    .booking-card { background:white; border-radius:8px; box-shadow:0 1px 4px rgba(0,0,0,.1); margin-bottom:1rem; overflow:hidden; }
    .booking-header { display:flex; justify-content:space-between; align-items:center; padding:1rem 1.5rem; background:#f5f5f5; }
    .booking-type { font-weight:600; }
    .status-badge { padding:.25rem .75rem; border-radius:20px; font-size:.75rem; font-weight:600; text-transform:uppercase; }
    .status-confirmed { background:#e8f5e9; color:#388e3c; }
    .status-pending { background:#fff8e1; color:#f57f17; }
    .status-cancelled { background:#fce4ec; color:#c62828; }
    .status-failed { background:#fce4ec; color:#c62828; }
    .status-inventory_held { background:#e3f2fd; color:#1565c0; }
    .status-payment_processing { background:#f3e5f5; color:#6a1b9a; }
    .booking-body { display:grid; grid-template-columns:repeat(auto-fit,minmax(140px,1fr)); gap:1rem; padding:1rem 1.5rem; }
    .detail-label { display:block; font-size:.75rem; color:#888; margin-bottom:.25rem; }
    .detail-value { font-weight:500; }
    .mono { font-family:monospace; font-size:.875rem; }
    .booking-footer { padding:.75rem 1.5rem; border-top:1px solid #eee; display:flex; justify-content:flex-end; align-items:center; }
    .cancel-btn { padding:.5rem 1rem; background:white; border:1px solid #d32f2f; color:#d32f2f; border-radius:4px; cursor:pointer; }
    .cancel-btn:hover { background:#ffebee; }
    .failure-reason { font-size:.875rem; color:#d32f2f; flex:1; }
    .pagination { display:flex; justify-content:center; align-items:center; gap:1rem; margin-top:2rem; }
    .pagination button { padding:.5rem 1rem; border:1px solid #ddd; border-radius:4px; cursor:pointer; }
  `]
})
export class DashboardComponent implements OnInit {
  bookings = signal<Booking[]>([]);
  loading = signal(true);
  cancelling = signal<string | null>(null);
  page = signal(0);
  totalPages = signal(0);

  constructor(private bookingService: BookingService, public auth: AuthService) {}

  ngOnInit(): void { this.loadBookings(); }

  loadBookings(): void {
    this.loading.set(true);
    this.bookingService.getMyBookings(this.page(), 20).subscribe({
      next: page => {
        this.bookings.set(page.content);
        this.totalPages.set(page.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  cancel(id: string): void {
    this.cancelling.set(id);
    this.bookingService.cancelBooking(id).subscribe({
      next: updated => {
        this.bookings.update(list => list.map(b => b.id === id ? updated : b));
        this.cancelling.set(null);
      },
      error: () => this.cancelling.set(null)
    });
  }

  canCancel(status: string): boolean {
    return ['PENDING', 'INVENTORY_HELD', 'PAYMENT_PROCESSING'].includes(status);
  }

  bookingTypeIcon(type: string): string {
    const icons: Record<string, string> = { FLIGHT: '✈️', HOTEL: '🏨', CINEMA: '🎬' };
    return icons[type] ?? '📋';
  }

  prevPage(): void { this.page.update(p => p - 1); this.loadBookings(); }
  nextPage(): void { this.page.update(p => p + 1); this.loadBookings(); }
}
