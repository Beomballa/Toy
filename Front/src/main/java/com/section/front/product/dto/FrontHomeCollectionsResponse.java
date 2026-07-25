package com.section.front.product.dto;

import java.util.List;

public record FrontHomeCollectionsResponse(
        List<FrontProductResponse> recommended,
        List<FrontProductResponse> ranking,
        List<FrontProductResponse> fastDelivery,
        List<FrontProductResponse> latestDrops,
        List<FrontProductResponse> lowStock
) {
}
