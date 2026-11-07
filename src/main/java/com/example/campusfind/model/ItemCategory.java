package com.example.campusfind.model;

public enum ItemCategory {
    ELECTRONICS("Electronics"),
    ID_CARDS("ID cards"),
    BOOKS_NOTES("Books and notes"),
    CLOTHING("Clothing"),
    BAGS("Bags"),
    KEYS("Keys"),
    ACCESSORIES("Accessories"),
    WATER_BOTTLES("Water bottles"),
    OTHER("Other");

    private final String label;

    ItemCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
