package com.section.front.product.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontProductCatalogServiceTest {

    private final FrontProductCatalogService frontProductCatalogService = new FrontProductCatalogService();

    @Test
    @DisplayName("프론트 카탈로그 서비스는 화면에 필요한 더미 상품 목록을 반환한다")
    void getCatalogReturnsDummyProducts() {
        var catalog = frontProductCatalogService.getCatalog();

        assertEquals(6, catalog.size());
        assertEquals(101L, catalog.getFirst().id());
        assertEquals("New Balance", catalog.getFirst().brand());
        assertTrue(catalog.stream().filter(product -> product.featured()).count() >= 3);
        assertTrue(catalog.stream().allMatch(product -> !product.options().isEmpty()));
    }

    @Test
    @DisplayName("프론트 부트스트랩 응답은 최신 드롭 기준 메트릭을 함께 계산한다")
    void getBootstrapCalculatesMetrics() {
        var bootstrap = frontProductCatalogService.getBootstrap();

        assertEquals(6, bootstrap.metrics().totalCount());
        assertEquals(3, bootstrap.metrics().lowStockCount());
        assertEquals("2026-06-04", bootstrap.metrics().latestCreatedDate());
        assertEquals(2, bootstrap.metrics().latestDropCount());
        assertEquals(3, bootstrap.metrics().featuredCount());
        assertEquals(189, bootstrap.metrics().totalStock());
    }

    @Test
    @DisplayName("프론트 카탈로그 서비스는 상품 번호로 단건 상세를 찾는다")
    void findProductReturnsMatchingItem() {
        var product = frontProductCatalogService.findProduct(103L);

        assertTrue(product.isPresent());
        assertEquals("Gel-Kayano 14 Oyster", product.get().name());
    }

    @Test
    @DisplayName("프론트 카탈로그 서비스는 없는 상품 번호면 비어있는 결과를 반환한다")
    void findProductReturnsEmptyWhenMissing() {
        var product = frontProductCatalogService.findProduct(999L);

        assertTrue(product.isEmpty());
    }
}
