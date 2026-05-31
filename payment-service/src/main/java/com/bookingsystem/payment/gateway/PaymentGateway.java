package com.bookingsystem.payment.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stub for an external payment gateway (e.g., Stripe, Razorpay).
 * In production this would call the gateway's SDK with proper retry/timeout config.
 */
@Slf4j
@Component
public class PaymentGateway {

    public String charge(BigDecimal amount, String currency, String paymentReference) {
        // Simulate network call latency
        log.info("Calling payment gateway: amount={} {}, ref={}", amount, currency, paymentReference);

        // In production: replace with Stripe/Razorpay SDK call
        // PaymentIntent intent = Stripe.paymentIntents.create(params);
        // return intent.getId();

        return "GW-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
