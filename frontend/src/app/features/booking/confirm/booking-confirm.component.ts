import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { BookingService } from '../../../core/services/booking.service';
import { Booking, InventoryItem } from '../../../core/models/booking.model';

@Component({
    selector: 'app-booking-confirm',
    imports: [CommonModule, RouterLink],
    template: `
    <div class="page">
      <a routerLink="/search" class="back">← Back to Search</a>
    
      @if (item) {
        <div class="confirm-card">
          <h2>Confirm Your Booking</h2>
          <div class="detail-row">
            <span class="label">Seat</span>
            <span class="value">{{ item.label }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Show Date</span>
            <span class="value">{{ date }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Seat Class</span>
            <span class="value">{{ item.seatClass }}</span>
          </div>
          <div class="detail-row total">
            <span class="label">Total</span>
            <span class="value">₹{{ item.price | number:'1.2-2' }}</span>
          </div>
          <div class="notice">
            ⏳ Seats are held for 10 minutes once you confirm. Please complete payment promptly.
          </div>
          @if (error) {
            <div class="error">{{ error }}</div>
          }
          @if (!booking()) {
            <div class="actions">
              <button class="confirm-btn" (click)="confirm()" [disabled]="loading()">
                {{ loading() ? 'Processing...' : 'Confirm & Pay' }}
              </button>
            </div>
          }
          @if (booking()) {
            <div class="success">
              <div class="checkmark">✅</div>
              <h3>Booking Submitted!</h3>
              <p>Booking ID: <strong>{{ booking()!.id }}</strong></p>
              <p>Status: <strong>{{ booking()!.status }}</strong></p>
              <p class="note">Payment is being processed. You'll receive a confirmation email shortly.</p>
              <a routerLink="/dashboard" class="dashboard-link">View My Bookings →</a>
            </div>
          }
        </div>
      }
    </div>
    `,
    styles: [`
    .page { max-width:600px; margin:2rem auto; padding:0 1rem; }
    .back { color:#1976d2; text-decoration:none; display:block; margin-bottom:1rem; }
    .confirm-card { background:white; padding:2rem; border-radius:8px; box-shadow:0 2px 8px rgba(0,0,0,.1); }
    h2 { margin-bottom:1.5rem; }
    .detail-row { display:flex; justify-content:space-between; padding:.75rem 0; border-bottom:1px solid #eee; }
    .detail-row.total { font-weight:700; font-size:1.2rem; color:#1976d2; border-bottom:none; }
    .label { color:#666; }
    .notice { background:#fff8e1; border-left:3px solid #ffc107; padding:1rem; margin:1.5rem 0; border-radius:0 4px 4px 0; font-size:.875rem; }
    .confirm-btn { width:100%; padding:1rem; background:#388e3c; color:white; border:none; border-radius:4px; font-size:1.1rem; cursor:pointer; }
    .confirm-btn:disabled { opacity:.6; }
    .error { color:#d32f2f; margin:1rem 0; }
    .success { text-align:center; padding:2rem 0; }
    .checkmark { font-size:3rem; margin-bottom:1rem; }
    .note { color:#666; font-size:.875rem; margin:1rem 0; }
    .dashboard-link { display:inline-block; margin-top:1rem; padding:.75rem 2rem; background:#1976d2; color:white; text-decoration:none; border-radius:4px; }
  `]
})
export class BookingConfirmComponent implements OnInit {
  item: InventoryItem | null = null;
  date = '';
  bookingType: 'CINEMA' = 'CINEMA';
  booking = signal<Booking | null>(null);
  loading = signal(false);
  error = '';

  constructor(private router: Router, private bookingService: BookingService) {}

  ngOnInit(): void {
    const state = this.router.getCurrentNavigation()?.extras.state as any;
    if (!state?.item) {
      this.router.navigate(['/search']);
      return;
    }
    this.item = state.item;
    this.date = state.date;
  }

  confirm(): void {
    if (!this.item) return;
    this.loading.set(true);
    this.error = '';

    this.bookingService.createBooking({
      referenceId: this.item.referenceId,
      bookingType: this.bookingType,
      inventoryItemId: this.item.id,
      bookingDate: this.date,
      totalAmount: this.item.price
    }).subscribe({
      next: b => {
        this.booking.set(b);
        this.loading.set(false);
      },
      error: err => {
        this.error = err.error?.detail || 'Booking failed. Please try again.';
        this.loading.set(false);
      }
    });
  }
}
