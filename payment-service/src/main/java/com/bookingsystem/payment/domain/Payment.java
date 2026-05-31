package com.bookingsystem.payment.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_booking", columnList = "booking_id"),
        @Index(name = "idx_payments_idempotency", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_payments_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID bookingId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    /** Masked card last 4 digits or wallet reference */
    @Column(length = 50)
    private String paymentReference;

    /** Gateway transaction ID returned by external payment processor */
    @Column(length = 100)
    private String gatewayTransactionId;

    @Column(nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    private String failureReason;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public enum PaymentStatus { PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED }

    public enum PaymentMethod { CARD, UPI, WALLET, NET_BANKING }
}
