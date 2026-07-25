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
            Search for available cinema seats for a specific movie show.
            Returns a JSON list of available seats with IDs, labels, prices, and availability status.
            """)
    public String searchAvailability(
            @ToolParam(description = "Seat class — one of: NORMAL, EXECUTIVE, PREMIUM, RECLINER") String seatClass,
            @ToolParam(description = "UUID reference ID of the specific movie show") String referenceId,
            @ToolParam(description = "Date to check in YYYY-MM-DD format. Use today's date if not specified.") String date) {

        log.debug("searchAvailability(seatClass={}, ref={}, date={})", seatClass, referenceId, date);
        try {
            String effectiveDate = (date == null || date.isBlank())
                    ? LocalDate.now().toString()
                    : date;

            String uri = UriComponentsBuilder
                    .fromHttpUrl(inventoryUrl + "/api/inventory/availability")
                    .queryParam("referenceId", referenceId)
                    .queryParam("date", effectiveDate)
                    .queryParam("seatClass", seatClass)
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
            List all available seat classes with their descriptions and sample show reference IDs.
            Use this when the user asks what options are available or what they can book.
            """)
    public String listBookingOptions() {
        return """
                {
                  "availableSeatClasses": [
                    {
                      "type": "NORMAL",
                      "description": "Standard seating, most affordable",
                      "samples": [
                        {"referenceId": "00000000-0000-0000-0000-000000000020", "name": "PVR Phoenix Mall, Mumbai — Pathaan"},
                        {"referenceId": "00000000-0000-0000-0000-000000000021", "name": "INOX Forum Mall, Bengaluru — Jawan"}
                      ]
                    },
                    {
                      "type": "EXECUTIVE",
                      "description": "Better legroom, mid-row seating"
                    },
                    {
                      "type": "PREMIUM",
                      "description": "Front rows, wider seats"
                    },
                    {
                      "type": "RECLINER",
                      "description": "Fully reclining premium seat"
                    }
                  ]
                }
                """;
    }
}
