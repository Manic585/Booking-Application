package com.bookingsystem.ai.controller;

import com.bookingsystem.ai.dto.ChatRequest;
import com.bookingsystem.ai.dto.ChatResponse;
import com.bookingsystem.ai.dto.RecommendationRequest;
import com.bookingsystem.ai.dto.RecommendationResponse;
import com.bookingsystem.ai.service.BookingAssistantService;
import com.bookingsystem.ai.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AiController {

    private final BookingAssistantService assistantService;
    private final RecommendationService recommendationService;

    /**
     * Conversational booking assistant with function-calling tools.
     * Maintains conversation context via conversationId.
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(assistantService.chat(request));
    }

    /**
     * AI-generated booking recommendations based on type and preferences.
     */
    @PostMapping("/recommendations")
    public ResponseEntity<RecommendationResponse> getRecommendations(
            @Valid @RequestBody RecommendationRequest request) {
        return ResponseEntity.ok(recommendationService.getRecommendations(request));
    }

    /**
     * Quick insight — one-shot question without conversation memory.
     */
    @GetMapping("/insight")
    public ResponseEntity<ChatResponse> quickInsight(@RequestParam String question) {
        var req = new ChatRequest(question, null, null);
        return ResponseEntity.ok(assistantService.chat(req));
    }
}
