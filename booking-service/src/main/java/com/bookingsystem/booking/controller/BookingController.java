package com.bookingsystem.booking.controller;

import com.bookingsystem.booking.dto.BookingResponse;
import com.bookingsystem.booking.dto.CreateBookingRequest;
import com.bookingsystem.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBooking(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(bookingService.getBooking(id, userId));
    }

    @GetMapping
    public ResponseEntity<Page<BookingResponse>> getMyBookings(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(bookingService.getUserBookings(userId, page, size));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, userId));
    }
}
