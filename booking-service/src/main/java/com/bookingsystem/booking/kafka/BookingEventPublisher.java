package com.bookingsystem.booking.kafka;

import com.bookingsystem.booking.domain.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishBookingCreated(Booking booking) {
        BookingCreatedEvent event = new BookingCreatedEvent(
                booking.getId(),
                booking.getInventoryItemId(),
                booking.getUserId(),
                booking.getTotalAmount(),
                booking.getBookingType().name()
        );
        send("booking-created", booking.getId().toString(), event);
    }

    public void publishBookingCancelled(Booking booking, String reason) {
        BookingCancelledEvent event = new BookingCancelledEvent(
                booking.getId(),
                booking.getInventoryItemId(),
                booking.getUserId(),
                reason
        );
        send("booking-cancelled", booking.getId().toString(), event);
    }

    private void send(String topic, String key, Object payload) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, payload);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event to topic={}, key={}: {}", topic, key, ex.getMessage());
            } else {
                log.debug("Published event: topic={}, key={}, offset={}",
                        topic, key, result.getRecordMetadata().offset());
            }
        });
    }

    public record BookingCreatedEvent(
            UUID bookingId, UUID itemId, UUID userId,
            java.math.BigDecimal amount, String bookingType) {}

    public record BookingCancelledEvent(
            UUID bookingId, UUID itemId, UUID userId, String reason) {}
}
