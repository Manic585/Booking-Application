package com.bookingsystem.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    public void sendBookingConfirmation(UUID bookingId, UUID userId, BigDecimal amount) {
        log.info("Sending booking confirmation email: bookingId={}, userId={}", bookingId, userId);
        // In production: fetch user email from User Service or from the event payload
        // and use a templating engine (Thymeleaf/FreeMarker) for HTML emails
        sendEmail(
                "user@example.com", // would be resolved from user service
                "Booking Confirmed - " + bookingId,
                String.format("Your booking %s has been confirmed. Amount charged: $%s. Thank you!",
                        bookingId, amount)
        );
    }

    public void sendBookingCancellation(UUID bookingId, UUID userId, String reason) {
        log.info("Sending cancellation email: bookingId={}", bookingId);
        sendEmail(
                "user@example.com",
                "Booking Cancelled - " + bookingId,
                String.format("Your booking %s has been cancelled. Reason: %s", bookingId, reason)
        );
    }

    public void sendPaymentFailure(UUID bookingId, UUID userId, String reason) {
        log.warn("Sending payment failure email: bookingId={}", bookingId);
        sendEmail(
                "user@example.com",
                "Payment Failed - Booking " + bookingId,
                String.format("Payment for booking %s failed. Reason: %s. Please try again.", bookingId, reason)
        );
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("noreply@bookingsystem.com");
            mailSender.send(message);
            log.debug("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw e; // re-throw to trigger Kafka retry
        }
    }
}
