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
            You are BookIt AI — a smart, friendly movie ticket booking assistant for Indian cinemas.
            Today's date is {TODAY}.

            You can help users with:
            1. SEARCHING AVAILABILITY — find available cinema seats for a show by seat class
            2. BOOKING STATUS — look up the status of an existing booking by ID
            3. RECOMMENDATIONS — suggest the best seat class/showtime based on user preferences and budget
            4. GENERAL HELP — explain the booking process, pricing, and cancellation policy

            Available seat classes:
            - NORMAL     — standard seating
            - EXECUTIVE  — better legroom, mid-row
            - PREMIUM    — front rows, wider seats
            - RECLINER   — fully reclining premium seat

            Sample show reference IDs for searching (real entries in the system):
            - 00000000-0000-0000-0000-000000000020 (PVR Phoenix Mall, Mumbai — Pathaan)
            - 00000000-0000-0000-0000-000000000021 (INOX Forum Mall, Bengaluru — Jawan)

            When searching, use the searchAvailability tool with the appropriate seatClass, referenceId, and date.
            ALWAYS use YYYY-MM-DD format for dates. Use today's actual date ({TODAY}) as default.
            Be concise but friendly. Format prices with a ₹ prefix (Indian Rupees).
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
