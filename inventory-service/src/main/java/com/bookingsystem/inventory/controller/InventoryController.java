package com.bookingsystem.inventory.controller;

import com.bookingsystem.inventory.domain.InventoryItem;
import com.bookingsystem.inventory.dto.AvailabilityResponse;
import com.bookingsystem.inventory.dto.HoldRequest;
import com.bookingsystem.inventory.dto.InventoryItemResponse;
import com.bookingsystem.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/availability")
    public ResponseEntity<AvailabilityResponse> getAvailability(
            @RequestParam UUID referenceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "FLIGHT_SEAT") InventoryItem.ItemType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(inventoryService.getAvailability(referenceId, date, type, page, size));
    }

    @PostMapping("/hold")
    public ResponseEntity<InventoryItemResponse> hold(@Valid @RequestBody HoldRequest request) {
        return ResponseEntity.ok(inventoryService.holdItem(request));
    }

    @PostMapping("/{itemId}/confirm")
    public ResponseEntity<InventoryItemResponse> confirm(
            @PathVariable UUID itemId,
            @RequestParam UUID bookingId) {
        return ResponseEntity.ok(inventoryService.confirmItem(itemId, bookingId));
    }

    @PostMapping("/{itemId}/release")
    public ResponseEntity<Void> release(
            @PathVariable UUID itemId,
            @RequestParam UUID bookingId) {
        inventoryService.releaseItem(itemId, bookingId);
        return ResponseEntity.noContent().build();
    }
}
