package com.bookingsystem.inventory.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Distributed lock using Redis SET NX EX.
 *
 * Why not Redlock: single-node Redis is sufficient here because the DB-level
 * optimistic locking is the true safety net. Redis just reduces contention
 * at the application layer before hitting the database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDistributedLock {

    private static final String LOCK_PREFIX = "inventory:lock:";
    private static final Duration DEFAULT_TTL = Duration.ofSeconds(10);

    private final StringRedisTemplate redisTemplate;

    /**
     * Acquires lock for the given resource, executes the supplier, then releases the lock.
     * Throws {@link LockAcquisitionException} if the lock cannot be acquired within attempts.
     */
    public <T> T withLock(UUID resourceId, Supplier<T> action) {
        return withLock(resourceId.toString(), DEFAULT_TTL, action);
    }

    public <T> T withLock(String resourceKey, Duration ttl, Supplier<T> action) {
        String lockKey = LOCK_PREFIX + resourceKey;
        String lockValue = UUID.randomUUID().toString(); // unique per caller to prevent accidental release

        boolean acquired = Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, ttl));

        if (!acquired) {
            log.warn("Failed to acquire distributed lock for resource={}", resourceKey);
            throw new LockAcquisitionException("Resource is currently locked: " + resourceKey);
        }

        try {
            log.debug("Lock acquired: key={}", lockKey);
            return action.get();
        } finally {
            // Only release if we still own the lock (Lua script for atomicity)
            releaseLock(lockKey, lockValue);
        }
    }

    private void releaseLock(String lockKey, String expectedValue) {
        String script = """
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    return redis.call('del', KEYS[1])
                else
                    return 0
                end
                """;
        try {
            redisTemplate.execute(
                    org.springframework.data.redis.core.script.RedisScript.of(script, Long.class),
                    java.util.List.of(lockKey),
                    expectedValue
            );
            log.debug("Lock released: key={}", lockKey);
        } catch (Exception e) {
            log.error("Failed to release lock: key={}", lockKey, e);
        }
    }
}
