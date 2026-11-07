package com.example.campusfind.agent;

public record AgentSearchResult(
        Long id,
        String type,
        String itemName,
        String category,
        String location,
        String date,
        String status,
        int relevance,
        String imageUrl
) {
    public AgentSearchResult(Long id, String type, String itemName, String category, String location, String date, String status, int relevance) {
        this(id, type, itemName, category, location, date, status, relevance, null);
    }

    public String getUrl() { return "/reports/" + id; }
    public boolean hasImage() { return imageUrl != null && !imageUrl.isBlank(); }
}
