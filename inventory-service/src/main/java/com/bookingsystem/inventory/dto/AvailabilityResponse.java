package com.bookingsystem.inventory.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AvailabilityResponse {
    private UUID referenceId;
    private LocalDate date;
    private long totalAvailable;
    private List<InventoryItemResponse> items;
    private int page;
    private int totalPages;
}
