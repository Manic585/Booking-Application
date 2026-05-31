package com.bookingsystem.ai.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryTools {

    private final RestTemplate restTemplate;

    @Value("${services.inventory-url}")
    private String inventoryUrl;

    @Tool(description = """
            Search for available inventory items (flight seats, hotel rooms, or cinema seats).
            Returns a JSON list of available items with IDs, labels, prices, and availability status.
            """)
    public String searchAvailability(
            @ToolParam(description = "Item type — one of: FLIGHT_SEAT, HOTEL_ROOM, CINEMA_SEAT") String itemType,
            @ToolParam(description = "UUID reference ID of the specific route, hotel, or show") String referenceId,
            @ToolParam(description = "Date to check in YYYY-MM-DD format. Use today's date if not specified.") String date) {

        log.debug("searchAvailability(type={}, ref={}, date={})", itemType, referenceId, date);
        try {
            String effectiveDate = (date == null || date.isBlank())
                    ? LocalDate.now().toString()
                    : date;

            String uri = UriComponentsBuilder
                    .fromHttpUrl(inventoryUrl + "/api/inventory/availability")
                    .queryParam("referenceId", referenceId)
                    .queryParam("date", effectiveDate)
                    .queryParam("type", itemType)
                    .queryParam("size", 20)
                    .toUriString();

            String result = restTemplate.getForObject(uri, String.class);
            log.debug("Inventory response: {}", result);
            return result != null ? result : "{\"totalAvailable\":0,\"items\":[]}";

        } catch (Exception e) {
            log.warn("Inventory search failed: {}", e.getMessage());
            return """
                    {"error":"Could not reach inventory service",
                     "message":"%s",
                     "suggestion":"Try again in a moment or use a different date."}
                    """.formatted(e.getMessage().replace("\"", "'"));
        }
    }

    @Tool(description = """
            List all available booking options with their type names, descriptions, and sample reference IDs.
            Use this when the user asks what options are available or what they can book.
            """)
    public String listBookingOptions() {
        return """
                {
                  "availableTypes": [
                    {
                      "type": "FLIGHT_SEAT",
                      "description": "Airplane seat — economy, business, or first class",
                      "samples": [
                        {"referenceId": "00000000-0000-0000-0000-000000000001", "name": "NYC → LAX"},
                        {"referenceId": "00000000-0000-0000-0000-000000000002", "name": "NYC → LHR (London)"}
                      ]
                    },
                    {
                      "type": "HOTEL_ROOM",
                      "description": "Hotel room — standard, deluxe, or suite",
                      "samples": [
                        {"referenceId": "00000000-0000-0000-0000-000000000010", "name": "Grand Hotel NYC"},
                        {"referenceId": "00000000-0000-0000-0000-000000000011", "name": "Sea View Resort Miami"}
                      ]
                    },
                    {
                      "type": "CINEMA_SEAT",
                      "description": "Cinema seat for a movie show",
                      "samples": [
                        {"referenceId": "00000000-0000-0000-0000-000000000020", "name": "AMC — Avengers: Doomsday"},
                        {"referenceId": "00000000-0000-0000-0000-000000000021", "name": "Cineplex — The Matrix 5"}
                      ]
                    }
                  ]
                }
                """;
    }
}
