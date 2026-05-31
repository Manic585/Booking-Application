import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { BookingService } from '../../core/services/booking.service';
import { AvailabilityResponse, InventoryItem } from '../../core/models/booking.model';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="page">
      <header class="page-header">
        <h1>Search Availability</h1>
      </header>

      <div class="search-card">
        <form [formGroup]="searchForm" (ngSubmit)="search()">
          <div class="form-row">
            <div class="field">
              <label>Type</label>
              <select formControlName="type">
                <option value="FLIGHT_SEAT">✈️ Flight</option>
                <option value="HOTEL_ROOM">🏨 Hotel</option>
                <option value="CINEMA_SEAT">🎬 Cinema</option>
              </select>
            </div>
            <div class="field">
              <label>Reference ID</label>
              <input formControlName="referenceId" placeholder="Flight/Hotel/Show ID" />
            </div>
            <div class="field">
              <label>Date</label>
              <input type="date" formControlName="date" [min]="today" />
            </div>
            <button type="submit" [disabled]="searchForm.invalid || loading()">
              {{ loading() ? 'Searching...' : 'Search' }}
            </button>
          </div>
        </form>
      </div>

      <div class="results" *ngIf="results()">
        <div class="results-header">
          <span>{{ results()!.totalAvailable }} seats/rooms available</span>
          <span>for {{ results()!.date }}</span>
        </div>

        <div class="items-grid">
          <div class="item-card"
               *ngFor="let item of results()!.items"
               [class.selected]="selectedItem()?.id === item.id"
               (click)="selectItem(item)">
            <div class="item-label">{{ item.label }}</div>
            <div class="item-price">\${{ item.price | number:'1.2-2' }}</div>
            <div class="item-status" [class.available]="item.status === 'AVAILABLE'">
              {{ item.status }}
            </div>
          </div>
        </div>

        <div class="booking-summary" *ngIf="selectedItem()">
          <h3>Selected: {{ selectedItem()!.label }}</h3>
          <p>Price: \${{ selectedItem()!.price | number:'1.2-2' }}</p>
          <button class="book-btn" (click)="proceedToBook()">
            Proceed to Book →
          </button>
        </div>
      </div>

      <div class="empty" *ngIf="results() && results()!.items.length === 0">
        No availability for the selected date. Try another date.
      </div>
    </div>
  `,
  styles: [`
    .page { max-width:900px; margin:2rem auto; padding:0 1rem; }
    .page-header { margin-bottom:1.5rem; }
    .search-card { background:white; padding:1.5rem; border-radius:8px; box-shadow:0 1px 4px rgba(0,0,0,.1); margin-bottom:2rem; }
    .form-row { display:flex; gap:1rem; align-items:flex-end; flex-wrap:wrap; }
    .field { flex:1; min-width:140px; }
    label { display:block; margin-bottom:.25rem; font-weight:500; font-size:.875rem; }
    input, select { width:100%; padding:.5rem; border:1px solid #ddd; border-radius:4px; box-sizing:border-box; }
    button { padding:.5rem 1.5rem; background:#1976d2; color:white; border:none; border-radius:4px; cursor:pointer; white-space:nowrap; }
    .results-header { display:flex; justify-content:space-between; margin-bottom:1rem; color:#555; }
    .items-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(140px,1fr)); gap:.75rem; }
    .item-card { border:2px solid #eee; border-radius:6px; padding:1rem; cursor:pointer; text-align:center; transition:.2s; }
    .item-card:hover { border-color:#1976d2; }
    .item-card.selected { border-color:#1976d2; background:#e3f2fd; }
    .item-label { font-weight:600; margin-bottom:.5rem; }
    .item-price { color:#1976d2; font-size:1.1rem; }
    .item-status { font-size:.75rem; margin-top:.25rem; }
    .item-status.available { color:#388e3c; }
    .booking-summary { margin-top:2rem; padding:1.5rem; background:#e3f2fd; border-radius:8px; }
    .book-btn { margin-top:1rem; padding:.75rem 2rem; background:#388e3c; color:white; border:none; border-radius:4px; cursor:pointer; font-size:1rem; }
    .empty { text-align:center; color:#888; padding:3rem; }
  `]
})
export class SearchComponent {
  searchForm: FormGroup;
  results = signal<AvailabilityResponse | null>(null);
  selectedItem = signal<InventoryItem | null>(null);
  loading = signal(false);
  today = new Date().toISOString().split('T')[0];

  constructor(
    private fb: FormBuilder,
    private bookingService: BookingService,
    private router: Router
  ) {
    this.searchForm = this.fb.group({
      type: ['FLIGHT_SEAT', Validators.required],
      referenceId: ['', Validators.required],
      date: [this.today, Validators.required]
    });
  }

  search(): void {
    if (this.searchForm.invalid) return;
    this.loading.set(true);
    this.selectedItem.set(null);
    const { type, referenceId, date } = this.searchForm.value;

    this.bookingService.searchAvailability(referenceId, date, type).subscribe({
      next: res => {
        this.results.set(res);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  selectItem(item: InventoryItem): void {
    if (item.status === 'AVAILABLE') {
      this.selectedItem.set(item);
    }
  }

  proceedToBook(): void {
    const item = this.selectedItem();
    if (!item) return;
    this.router.navigate(['/booking/confirm'], {
      state: {
        item,
        date: this.searchForm.value.date,
        bookingType: this.searchForm.value.type.replace('_SEAT', '').replace('_ROOM', '')
      }
    });
  }
}
