package com.bookingsystem.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class BookingResponse {
    private UUID id;
    private UUID userId;
    private UUID referenceId;
    private String bookingType;
    private UUID inventoryItemId;
    private LocalDate bookingDate;
    private String status;
    private BigDecimal totalAmount;
    private String idempotencyKey;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
}
