import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AvailabilityResponse, Booking, CreateBookingRequest, PageResponse } from '../models/booking.model';
import { v4 as uuidv4 } from 'uuid';

@Injectable({ providedIn: 'root' })
export class BookingService {

  constructor(private http: HttpClient) {}

  searchAvailability(referenceId: string, date: string, type: string,
                      page = 0, size = 50): Observable<AvailabilityResponse> {
    const params = new HttpParams()
      .set('referenceId', referenceId)
      .set('date', date)
      .set('type', type)
      .set('page', page)
      .set('size', size);
    return this.http.get<AvailabilityResponse>(`${environment.apiUrl}/inventory/availability`, { params });
  }

  createBooking(request: Omit<CreateBookingRequest, 'idempotencyKey'>): Observable<Booking> {
    const fullRequest: CreateBookingRequest = {
      ...request,
      idempotencyKey: uuidv4()  // Generate unique key per booking attempt
    };
    return this.http.post<Booking>(`${environment.apiUrl}/bookings`, fullRequest);
  }

  getBooking(id: string): Observable<Booking> {
    return this.http.get<Booking>(`${environment.apiUrl}/bookings/${id}`);
  }

  getMyBookings(page = 0, size = 20): Observable<PageResponse<Booking>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<Booking>>(`${environment.apiUrl}/bookings`, { params });
  }

  cancelBooking(id: string): Observable<Booking> {
    return this.http.delete<Booking>(`${environment.apiUrl}/bookings/${id}`);
  }
}
