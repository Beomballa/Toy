package com.section.common.commerce.dto;

import java.time.LocalDateTime;

public record FrontCatalogSummaryRow(
        String brandName,
        String categoryName,
        Integer releasePrice,
        Integer totalStock,
        LocalDateTime createdAt,
        boolean featured
) {
}
