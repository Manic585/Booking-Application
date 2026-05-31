package com.bookingsystem.booking.service;

import com.bookingsystem.booking.domain.Booking;
import com.bookingsystem.booking.dto.BookingResponse;
import com.bookingsystem.booking.dto.CreateBookingRequest;
import com.bookingsystem.booking.exception.ConflictException;
import com.bookingsystem.booking.exception.ForbiddenException;
import com.bookingsystem.booking.exception.NotFoundException;
import com.bookingsystem.booking.kafka.BookingEventPublisher;
import com.bookingsystem.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingEventPublisher eventPublisher;

    /**
     * Idempotent create: if a booking with this idempotency key already exists,
     * return the existing booking unchanged. This lets clients safely retry
     * on network failure without creating duplicates.
     */
    @Transactional
    public BookingResponse createBooking(UUID userId, CreateBookingRequest request) {
        // Check idempotency key first (fast path for retries)
        return bookingRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .map(existing -> {
                    log.info("Idempotent request: returning existing booking={} for key={}",
                            existing.getId(), request.getIdempotencyKey());
                    return toResponse(existing);
                })
                .orElseGet(() -> createNewBooking(userId, request));
    }

    private BookingResponse createNewBooking(UUID userId, CreateBookingRequest request) {
        Booking booking = Booking.builder()
                .userId(userId)
                .referenceId(request.getReferenceId())
                .bookingType(request.getBookingType())
                .inventoryItemId(request.getInventoryItemId())
                .bookingDate(request.getBookingDate())
                .totalAmount(request.getTotalAmount())
                .idempotencyKey(request.getIdempotencyKey())
                .status(Booking.BookingStatus.PENDING)
                .build();

        try {
            Booking saved = bookingRepository.save(booking);
            log.info("Booking created: bookingId={}, userId={}", saved.getId(), userId);

            // Emit Kafka event — inventory service will pick this up and hold the item
            eventPublisher.publishBookingCreated(saved);

            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            // Race condition: another thread created with the same idempotency key
            log.warn("Duplicate idempotency key detected, returning existing: key={}", request.getIdempotencyKey());
            return bookingRepository.findByIdempotencyKey(request.getIdempotencyKey())
                    .map(this::toResponse)
                    .orElseThrow(() -> new ConflictException("Booking conflict: " + request.getIdempotencyKey()));
        }
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(UUID bookingId, UUID userId) {
        return bookingRepository.findByIdAndUserId(bookingId, userId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getUserBookings(UUID userId, int page, int size) {
        return bookingRepository.findByUserId(
                userId, PageRequest.of(page, Math.min(size, 50), Sort.by("createdAt").descending()))
                .map(this::toResponse);
    }

    @Transactional
    public BookingResponse cancelBooking(UUID bookingId, UUID userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));

        if (!booking.getUserId().equals(userId)) {
            throw new ForbiddenException("Not authorized to cancel this booking");
        }

        if (booking.getStatus() == Booking.BookingStatus.CONFIRMED) {
            throw new ConflictException("Confirmed bookings cannot be cancelled via this endpoint. Contact support.");
        }

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED ||
            booking.getStatus() == Booking.BookingStatus.FAILED) {
            return toResponse(booking); // idempotent cancel
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking.setFailureReason("Cancelled by user");
        Booking saved = bookingRepository.save(booking);

        eventPublisher.publishBookingCancelled(saved, "Cancelled by user");
        log.info("Booking cancelled: bookingId={}, userId={}", bookingId, userId);

        return toResponse(saved);
    }

    /** Called by Kafka consumer when payment completes */
    @Transactional
    public void confirmBooking(UUID bookingId) {
        bookingRepository.findById(bookingId).ifPresent(booking -> {
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            log.info("Booking confirmed: bookingId={}", bookingId);
        });
    }

    /** Called by Kafka consumer when payment fails */
    @Transactional
    public void failBooking(UUID bookingId, String reason) {
        bookingRepository.findById(bookingId).ifPresent(booking -> {
            booking.setStatus(Booking.BookingStatus.FAILED);
            booking.setFailureReason(reason);
            bookingRepository.save(booking);
            eventPublisher.publishBookingCancelled(booking, reason);
            log.warn("Booking failed: bookingId={}, reason={}", bookingId, reason);
        });
    }

    private BookingResponse toResponse(Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .userId(b.getUserId())
                .referenceId(b.getReferenceId())
                .bookingType(b.getBookingType().name())
                .inventoryItemId(b.getInventoryItemId())
                .bookingDate(b.getBookingDate())
                .status(b.getStatus().name())
                .totalAmount(b.getTotalAmount())
                .idempotencyKey(b.getIdempotencyKey())
                .failureReason(b.getFailureReason())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}
