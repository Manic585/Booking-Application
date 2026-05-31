package com.bookingsystem.payment.dto;

import com.bookingsystem.payment.domain.Payment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentRequest {
    @NotNull UUID bookingId;
    @NotNull @Positive BigDecimal amount;
    @NotBlank String currency = "USD";
    @NotNull Payment.PaymentMethod paymentMethod;
    @NotBlank String paymentReference;
    @NotBlank String idempotencyKey;
}
