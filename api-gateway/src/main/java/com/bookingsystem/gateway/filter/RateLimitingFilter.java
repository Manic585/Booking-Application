package com.bookingsystem.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class RateLimitingFilter {

    /**
     * Rate limit per authenticated user; fall back to IP for anonymous requests.
     * Configured per-route in application.yml using Redis token bucket algorithm.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }
            // Fall back to IP for unauthenticated (login/register endpoints)
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + ip);
        };
    }

    @Bean
    public RedisRateLimiter apiRateLimiter() {
        // replenishRate=100 req/s, burstCapacity=200, requestedTokens=1
        return new RedisRateLimiter(100, 200, 1);
    }
}
