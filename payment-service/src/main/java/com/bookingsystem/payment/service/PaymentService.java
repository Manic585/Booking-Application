package com.bookingsystem.payment.service;

import com.bookingsystem.payment.domain.Payment;
import com.bookingsystem.payment.dto.PaymentRequest;
import com.bookingsystem.payment.dto.PaymentResponse;
import com.bookingsystem.payment.exception.NotFoundException;
import com.bookingsystem.payment.gateway.PaymentGateway;
import com.bookingsystem.payment.kafka.PaymentEventPublisher;
import com.bookingsystem.payment.repository.PaymentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentEventPublisher eventPublisher;

    /**
     * Idempotent payment processing.
     * If this idempotency key was already processed, return the cached result
     * without re-charging the customer.
     */
    @Transactional
    public PaymentResponse processPayment(UUID userId, PaymentRequest request) {
        return paymentRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .map(existing -> {
                    log.info("Idempotent payment request: returning existing payment={}", existing.getId());
                    return toResponse(existing);
                })
                .orElseGet(() -> doProcessPayment(userId, request));
    }

    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "paymentGatewayFallback")
    @Retry(name = "paymentGateway")
    private PaymentResponse doProcessPayment(UUID userId, PaymentRequest request) {
        Payment payment = Payment.builder()
                .bookingId(request.getBookingId())
                .userId(userId)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .paymentReference(request.getPaymentReference())
                .idempotencyKey(request.getIdempotencyKey())
                .status(Payment.PaymentStatus.PROCESSING)
                .build();

        try {
            Payment saved = paymentRepository.save(payment);

            // Call external payment gateway (mocked in dev)
            String gatewayTxnId = paymentGateway.charge(
                    saved.getAmount(), saved.getCurrency(), saved.getPaymentReference());

            saved.setGatewayTransactionId(gatewayTxnId);
            saved.setStatus(Payment.PaymentStatus.COMPLETED);
            Payment completed = paymentRepository.save(saved);

            eventPublisher.publishPaymentCompleted(completed);
            log.info("Payment completed: paymentId={}, bookingId={}", completed.getId(), request.getBookingId());

            return toResponse(completed);

        } catch (DataIntegrityViolationException e) {
            // Concurrent duplicate — return existing
            return paymentRepository.findByIdempotencyKey(request.getIdempotencyKey())
                    .map(this::toResponse)
                    .orElseThrow(() -> new RuntimeException("Payment creation conflict"));

        } catch (Exception e) {
            log.error("Payment failed for bookingId={}: {}", request.getBookingId(), e.getMessage());
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            Payment failed = paymentRepository.save(payment);
            eventPublisher.publishPaymentFailed(failed, e.getMessage());
            return toResponse(failed);
        }
    }

    /** Circuit breaker fallback — payment gateway is down */
    private PaymentResponse paymentGatewayFallback(UUID userId, PaymentRequest request, Exception ex) {
        log.error("Payment gateway circuit breaker OPEN for bookingId={}: {}",
                request.getBookingId(), ex.getMessage());

        Payment payment = Payment.builder()
                .bookingId(request.getBookingId())
                .userId(userId)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .idempotencyKey(request.getIdempotencyKey())
                .status(Payment.PaymentStatus.FAILED)
                .failureReason("Payment gateway temporarily unavailable")
                .build();

        Payment failed = paymentRepository.save(payment);
        eventPublisher.publishPaymentFailed(failed, "Payment gateway unavailable");
        return toResponse(failed);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBooking(UUID bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Payment not found for booking: " + bookingId));
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .bookingId(p.getBookingId())
                .userId(p.getUserId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .status(p.getStatus().name())
                .paymentMethod(p.getPaymentMethod().name())
                .gatewayTransactionId(p.getGatewayTransactionId())
                .idempotencyKey(p.getIdempotencyKey())
                .failureReason(p.getFailureReason())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
