package com.bookingsystem.inventory.repository;

import com.bookingsystem.inventory.domain.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, UUID> {

    Page<InventoryItem> findBySeatClassAndAvailableDateAndStatus(
            InventoryItem.SeatClass seatClass,
            LocalDate date,
            InventoryItem.Status status,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.id = :id")
    Optional<InventoryItem> findByIdWithLock(@Param("id") UUID id);

    @Query("""
        SELECT i FROM InventoryItem i
        WHERE i.referenceId = :referenceId
          AND i.availableDate = :date
          AND (i.status = 'AVAILABLE'
               OR (i.status = 'HELD' AND i.holdExpiresAt < :now))
        ORDER BY i.label
        """)
    List<InventoryItem> findAvailableItems(
            @Param("referenceId") UUID referenceId,
            @Param("date") LocalDate date,
            @Param("now") Instant now);

    @Modifying
    @Query("""
        UPDATE InventoryItem i
        SET i.status = 'AVAILABLE', i.bookingId = NULL, i.holdExpiresAt = NULL
        WHERE i.status = 'HELD' AND i.holdExpiresAt < :now
        """)
    int releaseExpiredHolds(@Param("now") Instant now);

    long countByReferenceIdAndAvailableDateAndStatus(
            UUID referenceId, LocalDate date, InventoryItem.Status status);
}
