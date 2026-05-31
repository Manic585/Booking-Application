package com.bookingsystem.ai.service;

import com.bookingsystem.ai.dto.ChatRequest;
import com.bookingsystem.ai.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingAssistantService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public ChatResponse chat(ChatRequest request) {
        String conversationId = (request.conversationId() != null && !request.conversationId().isBlank())
                ? request.conversationId()
                : UUID.randomUUID().toString();

        log.info("Chat [conv={}]: {}", conversationId, request.message());

        try {
            // Build a per-request memory advisor with this conversation's ID
            MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor
                    .builder(chatMemory)
                    .conversationId(conversationId)
                    .build();

            String response = chatClient.prompt()
                    .user(request.message())
                    .advisors(memoryAdvisor)
                    .call()
                    .content();

            return new ChatResponse(response, conversationId, deriveSuggestions(response));

        } catch (Exception e) {
            log.error("Chat error [conv={}]: {}", conversationId, e.getMessage(), e);
            return new ChatResponse(
                    "I'm having trouble right now. Please try again in a moment.",
                    conversationId,
                    List.of("Search availability", "View my bookings", "What can I book?")
            );
        }
    }

    private List<String> deriveSuggestions(String response) {
        if (response == null) return List.of();
        String lower = response.toLowerCase();
        if (lower.contains("flight") || lower.contains("seat")) {
            return List.of("Search flights", "Search hotels", "View my bookings");
        }
        if (lower.contains("hotel") || lower.contains("room")) {
            return List.of("Search hotels", "Search flights", "View my bookings");
        }
        if (lower.contains("cinema") || lower.contains("movie")) {
            return List.of("Search cinema seats", "Search flights", "View my bookings");
        }
        if (lower.contains("booking") || lower.contains("cancel")) {
            return List.of("View my bookings", "Search availability", "Contact support");
        }
        return List.of("Search availability", "View my bookings", "What can I book?");
    }
}
