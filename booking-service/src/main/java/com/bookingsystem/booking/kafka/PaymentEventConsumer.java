package com.bookingsystem.booking.kafka;

import com.bookingsystem.booking.service.BookingService;
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

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final BookingService bookingService;

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = "payment-completed", groupId = "booking-service")
    public void onPaymentCompleted(@Payload PaymentCompletedEvent event,
                                    @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Payment completed: bookingId={}, paymentId={}", event.bookingId(), event.paymentId());
        bookingService.confirmBooking(event.bookingId());
    }

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = "payment-failed", groupId = "booking-service")
    public void onPaymentFailed(@Payload PaymentFailedEvent event,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.warn("Payment failed: bookingId={}, reason={}", event.bookingId(), event.reason());
        bookingService.failBooking(event.bookingId(), event.reason());
    }

    @KafkaListener(topics = "payment-completed.DLT", groupId = "booking-service-dlt")
    public void onPaymentCompletedDLT(@Payload PaymentCompletedEvent event) {
        log.error("DEAD LETTER: payment-completed processing failed. bookingId={}. Manual check required.",
                event.bookingId());
    }

    public record PaymentCompletedEvent(UUID bookingId, UUID paymentId, UUID itemId) {}
    public record PaymentFailedEvent(UUID bookingId, UUID itemId, String reason) {}
}
