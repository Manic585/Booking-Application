package com.bookingsystem.inventory.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class HoldRequest {
    @NotNull UUID itemId;
    @NotNull UUID bookingId;
}
