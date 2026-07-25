export interface InventoryItem {
  id: string;
  referenceId: string;
  seatClass: string;
  label: string;
  availableDate: string;
  status: string;
  price: number;
}

export interface AvailabilityResponse {
  referenceId: string;
  date: string;
  totalAvailable: number;
  items: InventoryItem[];
  page: number;
  totalPages: number;
}

export interface CreateBookingRequest {
  referenceId: string;
  bookingType: 'CINEMA';
  inventoryItemId: string;
  bookingDate: string;
  totalAmount: number;
  idempotencyKey: string;
}

export interface Booking {
  id: string;
  userId: string;
  referenceId: string;
  bookingType: string;
  inventoryItemId: string;
  bookingDate: string;
  status: 'PENDING' | 'INVENTORY_HELD' | 'PAYMENT_PROCESSING' | 'CONFIRMED' | 'CANCELLED' | 'FAILED';
  totalAmount: number;
  idempotencyKey: string;
  failureReason?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}
