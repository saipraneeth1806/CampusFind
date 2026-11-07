package com.example.campusfind.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CampusFindAgentToolLoopTest {
    @Test
    void modelMayCallOnlyTheReadOnlyReportSearchToolAndThenSummarizeItsResult() throws Exception {
        ReportSearchTool searchTool = mock(ReportSearchTool.class);
        AgentSearchResult report = new AgentSearchResult(41L, "FOUND", "Blue bottle", "Water bottles",
                "Library", "2026-10-21", "OPEN", 2);
        when(searchTool.search("blue bottle", "FOUND")).thenReturn(List.of(report));

        AtomicInteger requests = new AtomicInteger();
        AtomicBoolean authorizationWasSet = new AtomicBoolean();
        ObjectMapper mapper = new ObjectMapper();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            exchange.getRequestBody().readAllBytes();
            authorizationWasSet.set("Bearer test-key".equals(exchange.getRequestHeaders().getFirst("Authorization")));
            int number = requests.incrementAndGet();
            String body = number == 1
                    ? "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":null,\"tool_calls\":[{\"id\":\"call-1\",\"type\":\"function\",\"function\":{\"name\":\"search_reports\",\"arguments\":\"{\\\"query\\\":\\\"blue bottle\\\",\\\"reportType\\\":\\\"FOUND\\\"}\"}}]}}]}"
                    : "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"I found a possible report near the library.\"}}]}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });

        try {
            server.start();
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
            CampusFindAgent agent = new CampusFindAgent(searchTool, mapper, "test-key", baseUrl, "test-model");
            AgentResponse response = agent.ask("I lost a blue bottle. Show me found reports.");

            assertTrue(response.aiPowered());
            assertEquals("I found a possible report near the library.", response.answer());
            assertEquals(List.of(report), response.reports());
            assertEquals(2, requests.get(), "The model should receive tool results before producing its final answer");
            assertTrue(authorizationWasSet.get());
            verify(searchTool).search("blue bottle", "FOUND");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void demoModeEnablesOfflineAiPoweredResponsesUsingSearchTool() {
        ReportSearchTool searchTool = mock(ReportSearchTool.class);
        AgentSearchResult report = new AgentSearchResult(52L, "FOUND", "Calculator", "Electronics",
                "Math Lab", "2026-10-22", "OPEN", 3);
        when(searchTool.search("I lost my calculator in the math lab", "FOUND")).thenReturn(List.of(report));

        CampusFindAgent agent = new CampusFindAgent(searchTool, new ObjectMapper(), "demo", "https://api.openai.com", "gpt-5.4-mini");
        AgentResponse response = agent.ask("I lost my calculator in the math lab");

        assertTrue(response.aiPowered(), "Demo mode should mark the response as aiPowered");
        assertEquals(List.of(report), response.reports(), "Demo mode should return the matching reports from the search tool");
        assertTrue(response.answer().contains("search_reports"), "Demo mode should reference the tool call");
    }

    @Test
    void missingApiKeyFallsBackToLocalSearchWithStatusMessage() {
        ReportSearchTool searchTool = mock(ReportSearchTool.class);
        AgentSearchResult report = new AgentSearchResult(10L, "LOST", "Keys", "Other",
                "Gym", "2026-10-20", "OPEN", 1);
        when(searchTool.search("lost keys", "ANY")).thenReturn(List.of(report));

        CampusFindAgent agent = new CampusFindAgent(searchTool, new ObjectMapper(), "", "https://api.openai.com/v1", "gpt-5.4-mini");
        AgentResponse response = agent.ask("lost keys");

        org.junit.jupiter.api.Assertions.assertFalse(response.aiPowered());
        assertTrue(response.providerStatus().contains("AI_API_KEY"));
        assertEquals(List.of(report), response.reports());
    }
}
