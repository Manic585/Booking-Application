package com.bookingsystem.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record RecommendationRequest(
        @NotBlank String bookingType,   // CINEMA
        String date,                     // YYYY-MM-DD, optional
        String preferences,              // free-text preferences (e.g. "recliner, near me")
        String budget                    // e.g. "under ₹500"
) {}
