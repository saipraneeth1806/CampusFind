package com.example.campusfind.agent;

import java.util.List;

public record AgentResponse(String answer, List<AgentSearchResult> reports, boolean aiPowered, String providerStatus) {
    public AgentResponse {
        reports = List.copyOf(reports);
        providerStatus = providerStatus == null ? "" : providerStatus;
    }
}
