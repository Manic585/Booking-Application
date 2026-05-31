package com.bookingsystem.inventory.kafka;

import com.bookingsystem.inventory.dto.HoldRequest;
import com.bookingsystem.inventory.service.InventoryService;
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
public class BookingEventConsumer {

    private final InventoryService inventoryService;

    /**
     * Consumes booking-created events to hold inventory items.
     * @RetryableTopic handles exponential backoff + dead-letter routing automatically.
     */
    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = "booking-created", groupId = "inventory-service")
    public void onBookingCreated(@Payload BookingCreatedEvent event,
                                  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Received booking-created: bookingId={}, itemId={}, topic={}",
                event.bookingId(), event.itemId(), topic);

        HoldRequest holdRequest = new HoldRequest();
        holdRequest.setItemId(event.itemId());
        holdRequest.setBookingId(event.bookingId());

        inventoryService.holdItem(holdRequest);
    }

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = "payment-completed", groupId = "inventory-service")
    public void onPaymentCompleted(@Payload PaymentCompletedEvent event,
                                    @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Received payment-completed: bookingId={}", event.bookingId());
        inventoryService.confirmItem(event.itemId(), event.bookingId());
    }

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = "payment-failed", groupId = "inventory-service")
    public void onPaymentFailed(@Payload PaymentFailedEvent event,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.warn("Received payment-failed: bookingId={}, releasing inventory", event.bookingId());
        inventoryService.releaseItem(event.itemId(), event.bookingId());
    }

    @KafkaListener(topics = "booking-created.DLT", groupId = "inventory-service-dlt")
    public void onDeadLetter(@Payload BookingCreatedEvent event,
                              @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("DEAD LETTER: Failed to process booking-created after all retries. " +
                  "bookingId={}, itemId={}. Manual intervention required.", event.bookingId(), event.itemId());
        // Alert/metric: in production this would trigger a PagerDuty alert
    }

    public record BookingCreatedEvent(UUID bookingId, UUID itemId, UUID userId) {}
    public record PaymentCompletedEvent(UUID bookingId, UUID itemId, UUID paymentId) {}
    public record PaymentFailedEvent(UUID bookingId, UUID itemId, String reason) {}
}
