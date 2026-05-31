package com.bookingsystem.notification.kafka;

import com.bookingsystem.notification.service.EmailNotificationService;
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
public class NotificationEventConsumer {

    private final EmailNotificationService emailService;

    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 30000),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = "payment-completed", groupId = "notification-service")
    public void onPaymentCompleted(@Payload PaymentCompletedEvent event,
                                    @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Sending booking confirmation notification: bookingId={}", event.bookingId());
        emailService.sendBookingConfirmation(event.bookingId(), event.userId(), event.amount());
    }

    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 30000),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = "booking-cancelled", groupId = "notification-service")
    public void onBookingCancelled(@Payload BookingCancelledEvent event,
                                    @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Sending cancellation notification: bookingId={}", event.bookingId());
        emailService.sendBookingCancellation(event.bookingId(), event.userId(), event.reason());
    }

    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 30000),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = "payment-failed", groupId = "notification-service")
    public void onPaymentFailed(@Payload PaymentFailedEvent event,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.warn("Sending payment failure notification: bookingId={}", event.bookingId());
        emailService.sendPaymentFailure(event.bookingId(), event.userId(), event.reason());
    }

    // Dead-letter handlers — alert on-call in production
    @KafkaListener(topics = {"payment-completed.DLT", "booking-cancelled.DLT", "payment-failed.DLT"},
            groupId = "notification-service-dlt")
    public void onDeadLetter(@Payload Object event,
                              @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("DEAD LETTER in notification service: topic={}, event={}", topic, event);
        // In production: PagerDuty / Slack alert
    }

    public record PaymentCompletedEvent(UUID bookingId, UUID userId, BigDecimal amount, String bookingType) {}
    public record BookingCancelledEvent(UUID bookingId, UUID userId, String reason) {}
    public record PaymentFailedEvent(UUID bookingId, UUID userId, String reason) {}
}
