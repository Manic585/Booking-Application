package com.bookingsystem.ai.dto;

import java.util.List;

public record RecommendationResponse(
        String summary,
        List<String> tips,
        String bestTimeToBook,
        String estimatedPriceRange,
        List<String> topPicks
) {}
