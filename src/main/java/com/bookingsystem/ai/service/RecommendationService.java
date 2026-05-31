package com.bookingsystem.ai.service;

import com.bookingsystem.ai.dto.RecommendationRequest;
import com.bookingsystem.ai.dto.RecommendationResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String RECOMMENDATION_PROMPT = """
            You are a travel and entertainment booking expert. Provide a structured recommendation for:

            Booking type: {bookingType}
            Date: {date}
            User preferences: {preferences}
            Budget: {budget}

            Respond ONLY with valid JSON in this exact format (no markdown, no extra text):
            {
              "summary": "A 2-3 sentence overview of your recommendation",
              "tips": ["tip1", "tip2", "tip3"],
              "bestTimeToBook": "Specific advice on when to book",
              "estimatedPriceRange": "e.g. $80 - $250 per night",
              "topPicks": ["Option 1 description", "Option 2 description", "Option 3 description"]
            }
            """;

    public RecommendationResponse getRecommendations(RecommendationRequest request) {
        log.info("Recommendation request: type={}, date={}", request.bookingType(), request.date());

        String prompt = RECOMMENDATION_PROMPT
                .replace("{bookingType}", request.bookingType())
                .replace("{date}", orDefault(request.date(), "flexible"))
                .replace("{preferences}", orDefault(request.preferences(), "no specific preferences"))
                .replace("{budget}", orDefault(request.budget(), "flexible budget"));

        try {
            String rawResponse = chatClient.prompt()
                    .system("You are a booking recommendation engine. Always respond with valid JSON only.")
                    .user(prompt)
                    .call()
                    .content();

            // Strip markdown code fences if present
            String json = rawResponse
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<>() {});

            return new RecommendationResponse(
                    getString(parsed, "summary"),
                    getList(parsed, "tips"),
                    getString(parsed, "bestTimeToBook"),
                    getString(parsed, "estimatedPriceRange"),
                    getList(parsed, "topPicks")
            );

        } catch (Exception e) {
            log.error("Recommendation generation failed: {}", e.getMessage(), e);
            return new RecommendationResponse(
                    "Unable to generate recommendations at this time. Please try again.",
                    List.of("Book early for the best rates", "Compare multiple options"),
                    "As early as possible for best availability",
                    "Varies by availability",
                    List.of()
            );
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private String orDefault(String value, String defaultValue) {
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
