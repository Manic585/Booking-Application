package com.bookingsystem.booking.repository;

import com.bookingsystem.booking.domain.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Optional<Booking> findByIdempotencyKey(String idempotencyKey);
    Page<Booking> findByUserId(UUID userId, Pageable pageable);
    Optional<Booking> findByIdAndUserId(UUID id, UUID userId);
}
