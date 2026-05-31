package com.bookingsystem.ai.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingTools {

    private final RestTemplate restTemplate;

    @Value("${services.booking-url}")
    private String bookingUrl;

    @Tool(description = """
            Retrieve the status and details of a specific booking by its booking ID.
            Returns the booking status, amount, dates, booking type, and any failure reason.
            """)
    public String getBookingStatus(
            @ToolParam(description = "The booking UUID (e.g. '3f2a1b4c-...')") String bookingId,
            @ToolParam(description = "The user's UUID — required to authorise the lookup") String userId) {

        log.debug("getBookingStatus(bookingId={}, userId={})", bookingId, userId);
        try {
            HttpHeaders headers = new HttpHeaders();
            if (userId != null && !userId.isBlank()) {
                headers.set("X-User-Id", userId);
            }
            ResponseEntity<String> response = restTemplate.exchange(
                    bookingUrl + "/api/bookings/" + bookingId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            return response.getBody() != null ? response.getBody() : "{\"error\":\"Empty response\"}";

        } catch (HttpClientErrorException.NotFound e) {
            return "{\"error\":\"Booking not found\",\"bookingId\":\"" + bookingId + "\"}";
        } catch (Exception e) {
            log.warn("Booking lookup failed: {}", e.getMessage());
            return "{\"error\":\"Could not retrieve booking\",\"hint\":\"Provide the exact booking UUID.\"}";
        }
    }

    @Tool(description = """
            Answer frequently asked questions about the booking process, cancellation policy, or payment.
            Topic can be: 'booking', 'cancellation', 'payment', or 'general'.
            """)
    public String getBookingFAQ(
            @ToolParam(description = "FAQ topic: 'booking', 'cancellation', 'payment', or 'general'") String topic) {

        return switch (topic == null ? "general" : topic.toLowerCase()) {
            case "cancellation", "cancel" -> """
                    {
                      "topic": "Cancellation Policy",
                      "policy": "Bookings can be cancelled for a full refund before the service date.",
                      "cancellableStatuses": ["PENDING", "INVENTORY_HELD", "PAYMENT_PROCESSING"],
                      "howToCancel": "Go to My Bookings dashboard and click the Cancel button.",
                      "note": "CONFIRMED bookings may be non-refundable depending on the provider."
                    }
                    """;
            case "payment" -> """
                    {
                      "topic": "Payment",
                      "methods": ["CREDIT_CARD", "DEBIT_CARD", "NET_BANKING", "UPI"],
                      "currency": "USD",
                      "process": "Payment is processed securely after inventory is reserved.",
                      "idempotency": "Each transaction uses a unique key to prevent double-charges.",
                      "refunds": "Refunds are processed within 5-7 business days."
                    }
                    """;
            case "booking", "process" -> """
                    {
                      "topic": "Booking Process",
                      "steps": [
                        "1. Search for available flights, hotels, or cinema seats by date",
                        "2. Select your preferred item from the grid",
                        "3. Click 'Proceed to Book' and confirm your details",
                        "4. Inventory is held immediately upon submission",
                        "5. Payment is processed — booking confirmed on success",
                        "6. You receive an email notification"
                      ]
                    }
                    """;
            default -> """
                    {
                      "topic": "General FAQ",
                      "items": [
                        {"q": "What can I book?", "a": "Flight seats, hotel rooms, and cinema seats."},
                        {"q": "How do I search?", "a": "Select type, enter a reference ID and date in Search."},
                        {"q": "Can I cancel?", "a": "Yes, from My Bookings for PENDING or INVENTORY_HELD bookings."},
                        {"q": "Is payment secure?", "a": "Yes — payments use idempotent keys and circuit-breaker protection."},
                        {"q": "How long is inventory held?", "a": "A limited time; complete payment to confirm."}
                      ]
                    }
                    """;
        };
    }
}
