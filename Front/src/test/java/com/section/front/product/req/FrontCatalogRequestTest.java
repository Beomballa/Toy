package com.section.front.product.req;

import com.section.common.commerce.dto.FrontCatalogQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FrontCatalogRequestTest {

    @Test
    @DisplayName("프론트 카탈로그 요청은 필터와 재고 임계값을 정규화한다")
    void toQueryNormalizesFiltersAndThreshold() {
        FrontCatalogQuery query = new FrontCatalogRequest(
                " 990v6 ",
                " New Balance ",
                " ALL ",
                " low ",
                " stock_desc ",
                30,
                true,
                " under_200 "
        ).toQuery();

        assertEquals("990v6", query.keyword());
        assertEquals("New Balance", query.brand());
        assertNull(query.category());
        assertEquals("LOW", query.stock());
        assertEquals("STOCK_DESC", query.sort());
        assertEquals(30, query.lowStockThreshold());
        assertEquals(true, query.featuredOnly());
        assertEquals("UNDER_200", query.priceBand());
    }

    @Test
    @DisplayName("프론트 카탈로그 요청은 허용되지 않은 재고 임계값을 기본값으로 되돌린다")
    void toQueryFallsBackToDefaultThresholdWhenInvalid() {
        FrontCatalogQuery query = new FrontCatalogRequest(
                null,
                null,
                null,
                null,
                null,
                999,
                null,
                "unknown"
        ).toQuery();

        assertEquals("ALL", query.stock());
        assertEquals("LATEST", query.sort());
        assertEquals(20, query.lowStockThreshold());
        assertEquals(false, query.featuredOnly());
        assertEquals("ALL", query.priceBand());
    }

    @Test
    @DisplayName("프론트 카탈로그 요청은 추가 정렬 옵션도 그대로 허용한다")
    void toQueryAcceptsAdditionalSorts() {
        FrontCatalogQuery query = new FrontCatalogRequest(
                null,
                null,
                null,
                null,
                "name_asc",
                null,
                null,
                null
        ).toQuery();

        assertEquals("NAME_ASC", query.sort());
    }
}
