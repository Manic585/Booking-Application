package com.bookingsystem.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record RecommendationRequest(
        @NotBlank String bookingType,   // FLIGHT, HOTEL, CINEMA
        String date,                     // YYYY-MM-DD, optional
        String preferences,              // free-text preferences
        String budget                    // e.g. "under $200"
) {}
