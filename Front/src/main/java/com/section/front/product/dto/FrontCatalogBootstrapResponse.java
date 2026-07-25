package com.section.front.product.dto;

import java.util.List;

public record FrontCatalogBootstrapResponse(
        List<FrontProductResponse> products,
        FrontCatalogPageResponse pagination,
        FrontCatalogMetricsResponse metrics,
        List<FrontCatalogFacetResponse> brandFacets,
        List<FrontCatalogFacetResponse> categoryFacets
) {
}
