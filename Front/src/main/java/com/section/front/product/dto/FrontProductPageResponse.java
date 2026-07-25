package com.section.front.product.dto;

import java.util.List;

public record FrontProductPageResponse(
        List<FrontProductResponse> products,
        FrontCatalogPageResponse pagination
) {
}
