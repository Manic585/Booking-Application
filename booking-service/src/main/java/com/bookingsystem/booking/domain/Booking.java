package com.bookingsystem.booking.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_bookings_user", columnList = "user_id"),
        @Index(name = "idx_bookings_status", columnList = "status"),
        @Index(name = "idx_bookings_idempotency", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_bookings_reference", columnList = "reference_id, booking_date")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    /** External entity being booked (flight, hotel, movie screening) */
    @Column(nullable = false)
    private UUID referenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingType bookingType;

    /** The specific inventory item (seat/room) */
    @Column(nullable = false)
    private UUID inventoryItemId;

    @Column(nullable = false)
    private LocalDate bookingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /** Client-supplied idempotency key — prevents duplicate submissions */
    @Column(nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    private String failureReason;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public enum BookingStatus {
        PENDING,       // Created, awaiting inventory hold
        INVENTORY_HELD, // Inventory is held
        PAYMENT_PROCESSING, // Payment initiated
        CONFIRMED,     // Payment succeeded
        CANCELLED,     // Cancelled by user or system
        FAILED         // Unrecoverable failure
    }

    public enum BookingType {
        FLIGHT, HOTEL, CINEMA
    }
}
