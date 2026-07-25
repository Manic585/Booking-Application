package com.bookingsystem.payment.kafka;

import com.bookingsystem.payment.domain.Payment;
import com.bookingsystem.payment.dto.PaymentRequest;
import com.bookingsystem.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventConsumer {

    private final PaymentService paymentService;

    /**
     * Automatically initiates payment when a booking is created.
     * The saga: booking-created → payment triggered → payment-completed/failed → booking confirmed/failed
     */
    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = "booking-created", groupId = "payment-service")
    public void onBookingCreated(@Payload BookingCreatedEvent event,
                                  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Payment triggered by booking-created: bookingId={}", event.bookingId());

        PaymentRequest request = new PaymentRequest();
        request.setBookingId(event.bookingId());
        request.setAmount(event.amount());
        request.setCurrency("INR");
        request.setPaymentMethod(Payment.PaymentMethod.UPI);
        request.setPaymentReference("AUTO-" + event.bookingId());
        // Use bookingId as idempotency key for auto-payment from saga
        request.setIdempotencyKey("auto-pay-" + event.bookingId());

        paymentService.processPayment(event.userId(), request);
    }

    @KafkaListener(topics = "booking-created.DLT", groupId = "payment-service-dlt")
    public void onBookingCreatedDLT(@Payload BookingCreatedEvent event) {
        log.error("DEAD LETTER: Failed to initiate payment for bookingId={}. " +
                  "Manual investigation required.", event.bookingId());
    }

    public record BookingCreatedEvent(
            UUID bookingId, UUID itemId, UUID userId, BigDecimal amount, String bookingType) {}
}
