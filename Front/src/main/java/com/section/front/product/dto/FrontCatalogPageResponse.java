package com.section.front.product.dto;

public record FrontCatalogPageResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
