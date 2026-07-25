package com.bookingsystem.inventory.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a single bookable cinema seat for a specific show (screening) and date.
 * Optimistic locking via @Version prevents lost-update anomalies under concurrent booking.
 */
@Entity
@Table(name = "inventory_items", indexes = {
        @Index(name = "idx_inv_class_date", columnList = "seat_class, available_date"),
        @Index(name = "idx_inv_ref", columnList = "reference_id"),
        @Index(name = "idx_inv_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** ID of the movie show (screening) this seat belongs to */
    @Column(nullable = false)
    private UUID referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_class", nullable = false, length = 30)
    private SeatClass seatClass;

    @Column(nullable = false, length = 50)
    private String label; // "Row E Seat 7", "Recliner R3"

    @Column(nullable = false)
    private LocalDate availableDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.AVAILABLE;

    /** UUID of the booking that holds this item; null when AVAILABLE */
    private UUID bookingId;

    /** When the hold expires if status is HELD — prevents orphaned locks */
    private Instant holdExpiresAt;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /** Optimistic locking — incremented on every update by JPA */
    @Version
    private Long version;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public enum SeatClass { NORMAL, EXECUTIVE, PREMIUM, RECLINER }

    public enum Status { AVAILABLE, HELD, BOOKED, CANCELLED }

    public boolean isAvailable() {
        return status == Status.AVAILABLE ||
               (status == Status.HELD && holdExpiresAt != null && holdExpiresAt.isBefore(Instant.now()));
    }
}
