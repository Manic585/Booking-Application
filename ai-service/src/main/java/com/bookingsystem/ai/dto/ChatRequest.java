package com.bookingsystem.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "Message cannot be blank")
        @Size(max = 2000, message = "Message must be under 2000 characters")
        String message,

        /** Optional — pass back the ID returned in a previous response to continue a conversation. */
        String conversationId,

        /** Optional — used by tools that look up user-specific bookings. */
        String userId
) {}
