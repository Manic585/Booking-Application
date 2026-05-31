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
 * Represents an allocatable unit: a flight seat, hotel room slot, or cinema seat for a specific date.
 * Optimistic locking via @Version prevents lost-update anomalies under concurrent booking.
 */
@Entity
@Table(name = "inventory_items", indexes = {
        @Index(name = "idx_inv_type_date", columnList = "item_type, available_date"),
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

    /** External entity ID — e.g. flightId, hotelRoomId, movieScreeningId */
    @Column(nullable = false)
    private UUID referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private ItemType itemType;

    @Column(nullable = false, length = 50)
    private String label; // "Seat 12A", "Room 204", "Row E Seat 7"

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

    public enum ItemType { FLIGHT_SEAT, HOTEL_ROOM, CINEMA_SEAT }

    public enum Status { AVAILABLE, HELD, BOOKED, CANCELLED }

    public boolean isAvailable() {
        return status == Status.AVAILABLE ||
               (status == Status.HELD && holdExpiresAt != null && holdExpiresAt.isBefore(Instant.now()));
    }
}
