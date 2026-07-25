package com.bookingsystem.inventory.service;

import com.bookingsystem.inventory.domain.InventoryItem;
import com.bookingsystem.inventory.dto.AvailabilityResponse;
import com.bookingsystem.inventory.dto.HoldRequest;
import com.bookingsystem.inventory.dto.InventoryItemResponse;
import com.bookingsystem.inventory.exception.ConflictException;
import com.bookingsystem.inventory.exception.NotFoundException;
import com.bookingsystem.inventory.lock.RedisDistributedLock;
import com.bookingsystem.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final int HOLD_DURATION_MINUTES = 10;

    private final InventoryRepository inventoryRepository;
    private final RedisDistributedLock distributedLock;

    @Cacheable(value = "availability", key = "#referenceId + ':' + #date + ':' + #page")
    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(UUID referenceId, LocalDate date,
                                                 InventoryItem.SeatClass seatClass, int page, int size) {
        Page<InventoryItem> items = inventoryRepository.findBySeatClassAndAvailableDateAndStatus(
                seatClass, date, InventoryItem.Status.AVAILABLE, PageRequest.of(page, size));

        long available = inventoryRepository.countByReferenceIdAndAvailableDateAndStatus(
                referenceId, date, InventoryItem.Status.AVAILABLE);

        return AvailabilityResponse.builder()
                .referenceId(referenceId)
                .date(date)
                .totalAvailable(available)
                .items(items.map(this::toResponse).toList())
                .page(page)
                .totalPages(items.getTotalPages())
                .build();
    }

    /**
     * Two-phase locking strategy:
     * 1. Redis distributed lock prevents thundering herd from even hitting the DB
     * 2. Pessimistic DB lock (@Lock) ensures consistency at the storage level
     * 3. @Retryable catches OptimisticLockingFailureException for any race slip-through
     */
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    @CacheEvict(value = "availability", allEntries = true)
    @Transactional
    public InventoryItemResponse holdItem(HoldRequest request) {
        return distributedLock.withLock(request.getItemId(), () -> {
            InventoryItem item = inventoryRepository
                    .findByIdWithLock(request.getItemId())
                    .orElseThrow(() -> new NotFoundException("Item not found: " + request.getItemId()));

            if (!item.isAvailable()) {
                throw new ConflictException("Item is not available: " + request.getItemId());
            }

            item.setStatus(InventoryItem.Status.HELD);
            item.setBookingId(request.getBookingId());
            item.setHoldExpiresAt(Instant.now().plusSeconds(HOLD_DURATION_MINUTES * 60L));

            InventoryItem saved = inventoryRepository.save(item);
            log.info("Item held: itemId={}, bookingId={}", saved.getId(), saved.getBookingId());
            return toResponse(saved);
        });
    }

    @CacheEvict(value = "availability", allEntries = true)
    @Transactional
    public InventoryItemResponse confirmItem(UUID itemId, UUID bookingId) {
        InventoryItem item = inventoryRepository.findByIdWithLock(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found: " + itemId));

        if (item.getStatus() != InventoryItem.Status.HELD || !bookingId.equals(item.getBookingId())) {
            throw new ConflictException("Item is not held by booking: " + bookingId);
        }

        item.setStatus(InventoryItem.Status.BOOKED);
        item.setHoldExpiresAt(null);
        InventoryItem saved = inventoryRepository.save(item);
        log.info("Item confirmed: itemId={}, bookingId={}", saved.getId(), bookingId);
        return toResponse(saved);
    }

    @CacheEvict(value = "availability", allEntries = true)
    @Transactional
    public void releaseItem(UUID itemId, UUID bookingId) {
        inventoryRepository.findByIdWithLock(itemId).ifPresent(item -> {
            if (bookingId.equals(item.getBookingId())) {
                item.setStatus(InventoryItem.Status.AVAILABLE);
                item.setBookingId(null);
                item.setHoldExpiresAt(null);
                inventoryRepository.save(item);
                log.info("Item released: itemId={}, bookingId={}", itemId, bookingId);
            }
        });
    }

    /** Periodic cleanup of expired holds — runs every 2 minutes */
    @Scheduled(fixedDelay = 120_000)
    @Transactional
    public void releaseExpiredHolds() {
        int released = inventoryRepository.releaseExpiredHolds(Instant.now());
        if (released > 0) {
            log.info("Released {} expired holds", released);
        }
    }

    private InventoryItemResponse toResponse(InventoryItem item) {
        return InventoryItemResponse.builder()
                .id(item.getId())
                .referenceId(item.getReferenceId())
                .seatClass(item.getSeatClass().name())
                .label(item.getLabel())
                .availableDate(item.getAvailableDate())
                .status(item.getStatus().name())
                .price(item.getPrice())
                .bookingId(item.getBookingId())
                .build();
    }
}
