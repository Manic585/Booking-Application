package com.bookingsystem.booking.dto;

import com.bookingsystem.booking.domain.Booking;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateBookingRequest {

    @NotNull
    private UUID referenceId;

    @NotNull
    private Booking.BookingType bookingType;

    @NotNull
    private UUID inventoryItemId;

    @NotNull
    private LocalDate bookingDate;

    @NotNull
    @Positive
    private BigDecimal totalAmount;

    /** Client must generate and send a unique key per logical booking attempt */
    @NotNull
    private String idempotencyKey;
}
