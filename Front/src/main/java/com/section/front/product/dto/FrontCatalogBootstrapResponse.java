package com.section.front.product.dto;

import java.util.List;

public record FrontCatalogBootstrapResponse(
        List<FrontProductResponse> products,
        FrontCatalogMetricsResponse metrics
) {
}
