package com.section.common.commerce.dto;

public record AdminFrontDisplayProductRow(
        Long productNo,
        String productName,
        String brandName,
        String categoryName,
        Integer releasePrice,
        Long totalStock,
        String status,
        boolean displayConfigured,
        String headline,
        String description,
        String mood,
        boolean featured,
        Integer featuredRank
) {
}
