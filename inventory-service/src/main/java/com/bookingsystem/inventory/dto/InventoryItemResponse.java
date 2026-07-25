package com.bookingsystem.inventory.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class InventoryItemResponse {
    private UUID id;
    private UUID referenceId;
    private String seatClass;
    private String label;
    private LocalDate availableDate;
    private String status;
    private BigDecimal price;
    private UUID bookingId;
}
