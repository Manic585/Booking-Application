package com.bookingsystem.ai.config;

import com.bookingsystem.ai.tools.BookingTools;
import com.bookingsystem.ai.tools.InventoryTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Configuration
public class AiConfig {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are BookIt AI — a smart, friendly booking assistant for a travel and entertainment platform.
            Today's date is {TODAY}.

            You can help users with:
            1. SEARCHING AVAILABILITY — find available flight seats, hotel rooms, and cinema seats
            2. BOOKING STATUS — look up the status of an existing booking by ID
            3. RECOMMENDATIONS — suggest the best options based on user preferences
            4. GENERAL HELP — explain the booking process, pricing, and policies

            Available inventory types:
            - FLIGHT_SEAT  — airplane seat bookings
            - HOTEL_ROOM   — hotel room bookings
            - CINEMA_SEAT  — cinema seat bookings

            Sample reference IDs for searching (real entries in the system):
            - Flights:  00000000-0000-0000-0000-000000000001 (NYC → LAX)
                        00000000-0000-0000-0000-000000000002 (NYC → LHR, London)
            - Hotels:   00000000-0000-0000-0000-000000000010 (Grand Hotel NYC)
                        00000000-0000-0000-0000-000000000011 (Sea View Resort Miami)
            - Cinemas:  00000000-0000-0000-0000-000000000020 (AMC — Avengers: Doomsday)
                        00000000-0000-0000-0000-000000000021 (Cineplex — The Matrix 5)

            When searching, use the searchAvailability tool with the appropriate itemType, referenceId, and date.
            ALWAYS use YYYY-MM-DD format for dates. Use today's actual date ({TODAY}) as default.
            Be concise but friendly. Format prices with a $ prefix.
            """;

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                  InventoryTools inventoryTools,
                                  BookingTools bookingTools) {
        String systemPrompt = SYSTEM_PROMPT_TEMPLATE
                .replace("{TODAY}", LocalDate.now().toString());
        return builder
                .defaultSystem(systemPrompt)
                .defaultTools(inventoryTools, bookingTools)
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
