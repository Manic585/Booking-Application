package com.bookingsystem.ai.dto;

import java.util.List;

public record ChatResponse(
        String response,
        String conversationId,
        List<String> suggestedActions
) {}
