package com.bookingsystem.payment.kafka;

import com.bookingsystem.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPaymentCompleted(Payment payment) {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                payment.getBookingId(), payment.getId(), payment.getGatewayTransactionId());
        kafkaTemplate.send("payment-completed", payment.getBookingId().toString(), event)
                .whenComplete((r, ex) -> {
                    if (ex != null) log.error("Failed to publish payment-completed: {}", ex.getMessage());
                });
    }

    public void publishPaymentFailed(Payment payment, String reason) {
        PaymentFailedEvent event = new PaymentFailedEvent(payment.getBookingId(), payment.getId(), reason);
        kafkaTemplate.send("payment-failed", payment.getBookingId().toString(), event)
                .whenComplete((r, ex) -> {
                    if (ex != null) log.error("Failed to publish payment-failed: {}", ex.getMessage());
                });
    }

    public record PaymentCompletedEvent(UUID bookingId, UUID paymentId, String gatewayTxnId) {}
    public record PaymentFailedEvent(UUID bookingId, UUID paymentId, String reason) {}
}
