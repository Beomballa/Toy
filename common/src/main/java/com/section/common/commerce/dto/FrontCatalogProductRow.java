package com.section.common.commerce.dto;

import java.time.LocalDateTime;

public record FrontCatalogProductRow(
        Long productNo,
        Long brandNo,
        Long categoryNo,
        String brandName,
        String categoryName,
        String productName,
        String headline,
        String modelNum,
        Integer releasePrice,
        Integer totalStock,
        LocalDateTime createdAt,
        String description,
        String mood,
        boolean featured,
        Integer featuredRank
) {
}
