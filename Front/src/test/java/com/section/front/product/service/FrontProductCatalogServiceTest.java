package com.section.front.product.service;

import com.section.common.commerce.dto.FrontCatalogProductRow;
import com.section.common.commerce.dto.FrontCatalogQuery;
import com.section.common.commerce.dto.FrontCatalogSummaryRow;
import com.section.common.commerce.entity.ProductOption;
import com.section.common.commerce.repository.ProductOptionRepository;
import com.section.common.commerce.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FrontProductCatalogServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductOptionRepository productOptionRepository;

    @InjectMocks
    private FrontProductCatalogService frontProductCatalogService;

    @Test
    @DisplayName("프론트 카탈로그 서비스는 DB 상품과 옵션을 화면 응답으로 조합한다")
    void getCatalogReturnsDatabaseProducts() {
        mockCatalogRows();
        mockOptions();

        var catalog = frontProductCatalogService.getCatalog(FrontCatalogQuery.defaultQuery(), PageRequest.of(0, 12));

        assertEquals(3, catalog.products().size());
        assertEquals(101L, catalog.products().getFirst().id());
        assertEquals("New Balance", catalog.products().getFirst().brand());
        assertEquals("Grey precision", catalog.products().getFirst().headline());
        assertEquals("289,000원", catalog.products().getFirst().priceLabel());
        assertEquals("품절 임박", catalog.products().getFirst().stockStatus());
        assertTrue(catalog.products().stream().allMatch(product -> !product.options().isEmpty()));
        assertEquals(3, catalog.pagination().totalElements());
    }

    @Test
    @DisplayName("프론트 카탈로그 서비스는 현재 페이지 상품의 옵션만 조회하고 전체 건수를 유지한다")
    void getCatalogLoadsOptionsForCurrentPageOnly() {
        List<FrontCatalogProductRow> pageRows = List.of(
                row(101L, 1L, 11L, "New Balance", "러닝화", "990v6", "Grey", "M990", 289000, 18, LocalDate.now(), "설명", "Grey", true, 1),
                row(102L, 2L, 12L, "Nike", "라이프스타일", "Air Max", "Air", "AM1", 219000, 10, LocalDate.now(), "설명", "Air", false, null)
        );
        PageRequest pageable = PageRequest.of(2, 2);
        when(productRepository.getFrontCatalogProducts(any(FrontCatalogQuery.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(pageRows, pageable, 31));
        when(productOptionRepository.findAllByProductNoInOrderByProductNoAscOptionNameAsc(List.of(101L, 102L)))
                .thenReturn(List.of(option(101L, "260", 18), option(102L, "270", 10)));

        var catalog = frontProductCatalogService.getCatalog(FrontCatalogQuery.defaultQuery(), pageable);

        assertEquals(2, catalog.products().size());
        assertEquals(31, catalog.pagination().totalElements());
        assertEquals(16, catalog.pagination().totalPages());
        assertEquals(2, catalog.pagination().page());
        verify(productOptionRepository).findAllByProductNoInOrderByProductNoAscOptionNameAsc(List.of(101L, 102L));
    }

    @Test
    @DisplayName("프론트 카탈로그 서비스는 범위를 벗어난 페이지를 마지막 페이지로 보정한다")
    void getCatalogMovesOutOfRangeRequestToLastPage() {
        FrontCatalogProductRow lastRow = row(
                131L, 1L, 11L, "New Balance", "러닝화", "Last Product", "Last", "LAST",
                199000, 7, LocalDate.now(), "설명", "Last", false, null
        );
        PageRequest invalidPage = PageRequest.of(99, 12);
        PageRequest lastPage = PageRequest.of(2, 12);
        when(productRepository.getFrontCatalogProducts(any(FrontCatalogQuery.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), invalidPage, 25))
                .thenReturn(new PageImpl<>(List.of(lastRow), lastPage, 25));
        when(productOptionRepository.findAllByProductNoInOrderByProductNoAscOptionNameAsc(List.of(131L)))
                .thenReturn(List.of(option(131L, "260", 7)));

        var catalog = frontProductCatalogService.getCatalog(FrontCatalogQuery.defaultQuery(), invalidPage);

        assertEquals(2, catalog.pagination().page());
        assertEquals(3, catalog.pagination().totalPages());
        assertEquals(131L, catalog.products().getFirst().id());
    }

    @Test
    @DisplayName("프론트 부트스트랩 응답은 조회 결과 기준 메트릭을 계산한다")
    void getBootstrapCalculatesMetrics() {
        mockCatalogRows();
        mockOptions();

        mockSummaryRows();

        var bootstrap = frontProductCatalogService.getBootstrap(FrontCatalogQuery.defaultQuery(), PageRequest.of(0, 12));

        assertEquals(3, bootstrap.metrics().totalCount());
        assertEquals(3, bootstrap.metrics().lowStockCount());
        assertEquals(LocalDate.now().minusDays(2).toString(), bootstrap.metrics().latestCreatedDate());
        assertEquals(2, bootstrap.metrics().latestDropCount());
        assertEquals(2, bootstrap.metrics().featuredCount());
        assertEquals(40, bootstrap.metrics().totalStock());
        assertEquals(229000, bootstrap.metrics().averagePrice());
        assertEquals(179000, bootstrap.metrics().minimumPrice());
        assertEquals(289000, bootstrap.metrics().maximumPrice());
        assertEquals(3, bootstrap.metrics().brandCount());
    }

    @Test
    @DisplayName("프론트 카탈로그 서비스는 전시 메타가 비어 있어도 기본 문구로 응답을 만든다")
    void getCatalogFallsBackWhenDisplayMetadataMissing() {
        when(productRepository.getFrontCatalogProducts(any(FrontCatalogQuery.class), any(Pageable.class))).thenReturn(pageOf(List.of(
                row(201L, 7L, 21L, "Salomon", "아웃도어", "ACS Pro", null, "L123", 199000, 21, LocalDate.now(), null, null, false, null)
        )));
        when(productOptionRepository.findAllByProductNoInOrderByProductNoAscOptionNameAsc(List.of(201L)))
                .thenReturn(List.of(option(201L, "270", 21)));

        var catalog = frontProductCatalogService.getCatalog(FrontCatalogQuery.defaultQuery(), PageRequest.of(0, 12)).products();

        assertEquals(1, catalog.size());
        assertEquals("Salomon curated", catalog.getFirst().headline());
        assertEquals("Salomon pick", catalog.getFirst().mood());
        assertEquals("199,000원", catalog.getFirst().priceLabel());
        assertTrue(catalog.getFirst().description().contains("기본 정보만 노출"));
    }

    @Test
    @DisplayName("프론트 카탈로그 서비스는 상품 생성일 기준으로 createdDate를 노출한다")
    void getCatalogUsesCreatedAtForCreatedDate() {
        when(productRepository.getFrontCatalogProducts(any(FrontCatalogQuery.class), any(Pageable.class))).thenReturn(pageOf(List.of(
                new FrontCatalogProductRow(
                        301L,
                        9L,
                        31L,
                        "Adidas",
                        "축구화",
                        "Predator Elite",
                        "Pitch control",
                        "PD-ELITE",
                        319000,
                        14,
                        LocalDateTime.of(2026, 6, 5, 16, 30),
                        "설명",
                        "Control tone",
                        true,
                        1,
                        "/images/product/pd-elite.png"
                )
        )));
        when(productOptionRepository.findAllByProductNoInOrderByProductNoAscOptionNameAsc(List.of(301L)))
                .thenReturn(List.of(option(301L, "270", 14)));

        var catalog = frontProductCatalogService.getCatalog(FrontCatalogQuery.defaultQuery(), PageRequest.of(0, 12)).products();

        assertEquals("2026-06-05", catalog.getFirst().createdDate());
        assertEquals(1, catalog.getFirst().featuredRank());
        assertEquals("/images/product/pd-elite.png", catalog.getFirst().thumbnailUrl());
    }

    @Test
    @DisplayName("프론트 카탈로그 서비스의 stockStatus는 조회 임계값 기준으로 계산된다")
    void getCatalogBuildsStockStatusFromQueryThreshold() {
        when(productRepository.getFrontCatalogProducts(any(FrontCatalogQuery.class), any(Pageable.class))).thenReturn(pageOf(List.of(
                row(401L, 1L, 11L, "New Balance", "러닝화", "990v6 Grey Day", "Grey precision", "M990GL6", 289000, 18, LocalDate.now(), "설명", "Grey precision", true, 1)
        )));
        when(productOptionRepository.findAllByProductNoInOrderByProductNoAscOptionNameAsc(List.of(401L)))
                .thenReturn(List.of(option(401L, "260", 18)));

        var stableCatalog = frontProductCatalogService.getCatalog(
                new FrontCatalogQuery(null, null, null, "ALL", "LATEST", 10, false, "ALL"),
                PageRequest.of(0, 12)
        ).products();
        var tenseCatalog = frontProductCatalogService.getCatalog(
                new FrontCatalogQuery(null, null, null, "ALL", "LATEST", 20, false, "ALL"),
                PageRequest.of(0, 12)
        ).products();

        assertEquals("재고 안정", stableCatalog.getFirst().stockStatus());
        assertEquals("품절 임박", tenseCatalog.getFirst().stockStatus());
    }

    @Test
    @DisplayName("프론트 카탈로그 서비스의 단건 조회는 상품 번호 기반 조회를 사용한다")
    void findProductUsesDedicatedProductLookup() {
        FrontCatalogProductRow target = row(101L, 1L, 11L, "New Balance", "러닝화", "990v6 Grey Day", "Grey precision", "M990GL6", 289000, 18, LocalDate.now().minusDays(2), "설명", "Grey precision", true, 1);

        when(productRepository.getFrontCatalogProduct(101L)).thenReturn(Optional.of(target));
        when(productOptionRepository.findAllByProductNoInOrderByProductNoAscOptionNameAsc(List.of(101L)))
                .thenReturn(List.of(option(101L, "260", 4)));

        var product = frontProductCatalogService.findProduct(101L);

        assertTrue(product.isPresent());
        assertEquals("Grey precision", product.get().headline());
        assertEquals("990v6 Grey Day", product.get().name());
        assertEquals("품절 임박", product.get().stockStatus());
    }

    @Test
    @DisplayName("프론트 카탈로그 서비스는 상품 번호로 단건 상세와 연관 상품을 찾는다")
    void findProductDetailIncludesRelatedProducts() {
        FrontCatalogProductRow target = row(101L, 1L, 11L, "New Balance", "러닝화", "990v6 Grey Day", "Grey precision", "M990GL6", 289000, 18, LocalDate.now().minusDays(2), "설명", "Grey precision", true, 1);
        FrontCatalogProductRow related0 = row(107L, 1L, 11L, "New Balance", "러닝화", "1906U Silver", "Silver lane", "M1906", 209000, 22, LocalDate.now().minusDays(1), "설명", "Silver lane", true, 2);
        FrontCatalogProductRow related1 = row(103L, 2L, 11L, "ASICS", "러닝화", "Gel-Kayano 14 Oyster", "Metal calm", "1201A019-200", 179000, 12, LocalDate.now().minusDays(2), "설명", "Metal calm", true, 3);
        FrontCatalogProductRow related2 = row(106L, 6L, 11L, "Hoka", "러닝화", "Mach X Voltage", "Fast cushion", "HM1123", 239000, 10, LocalDate.now().minusDays(8), "설명", "Fast cushion", false, 10);

        when(productRepository.getFrontCatalogProduct(101L)).thenReturn(Optional.of(target));
        when(productRepository.getRelatedFrontCatalogProducts(101L, 1L, 11L, 6)).thenReturn(List.of(related0, related1, related2));
        when(productOptionRepository.findAllByProductNoInOrderByProductNoAscOptionNameAsc(List.of(101L)))
                .thenReturn(List.of(option(101L, "260", 4)));
        when(productOptionRepository.findAllByProductNoInOrderByProductNoAscOptionNameAsc(List.of(107L, 103L, 106L)))
                .thenReturn(List.of(option(107L, "265", 7), option(103L, "245", 4), option(106L, "270", 10)));

        var detail = frontProductCatalogService.findProductDetail(101L);

        assertTrue(detail.isPresent());
        assertEquals("990v6 Grey Day", detail.get().name());
        assertEquals("Grey precision", detail.get().headline());
        assertEquals("289,000원", detail.get().priceLabel());
        assertEquals(3, detail.get().relatedProducts().size());
        assertEquals("브랜드·카테고리 일치", detail.get().relatedProducts().getFirst().reason());
        assertEquals(107L, detail.get().relatedProducts().getFirst().id());
        assertEquals("러닝화", detail.get().relatedProducts().getFirst().category());
        assertEquals("같은 카테고리", detail.get().relatedProducts().get(1).reason());
        assertEquals("179,000원", detail.get().relatedProducts().get(1).priceLabel());
    }

    @Test
    @DisplayName("프론트 카탈로그 서비스는 없는 상품 번호면 비어있는 결과를 반환한다")
    void findProductReturnsEmptyWhenMissing() {
        when(productRepository.getFrontCatalogProduct(999L)).thenReturn(Optional.empty());

        var product = frontProductCatalogService.findProductDetail(999L);

        assertTrue(product.isEmpty());
    }

    private void mockCatalogRows() {
        when(productRepository.getFrontCatalogProducts(any(FrontCatalogQuery.class), any(Pageable.class))).thenReturn(pageOf(List.of(
                row(101L, 1L, 11L, "New Balance", "러닝화", "990v6 Grey Day", "Grey precision", "M990GL6", 289000, 18, LocalDate.now().minusDays(2), "설명", "Grey precision", true, 1),
                row(102L, 2L, 12L, "Nike", "라이프스타일", "Air Max DN Ember", "Ember energy", "DV3337-800", 219000, 10, LocalDate.now().minusDays(3), "설명", "Ember energy", true, 2),
                row(103L, 3L, 11L, "ASICS", "러닝화", "Gel-Kayano 14 Oyster", "Metal calm", "1201A019-200", 179000, 12, LocalDate.now().minusDays(2), "설명", "Metal calm", false, 3)
        )));
    }

    private void mockSummaryRows() {
        when(productRepository.getFrontCatalogSummary(any(FrontCatalogQuery.class))).thenReturn(List.of(
                summary("New Balance", "러닝화", 289000, 18, LocalDate.now().minusDays(2), true),
                summary("Nike", "라이프스타일", 219000, 10, LocalDate.now().minusDays(3), true),
                summary("ASICS", "러닝화", 179000, 12, LocalDate.now().minusDays(2), false)
        ));
    }

    private PageImpl<FrontCatalogProductRow> pageOf(List<FrontCatalogProductRow> rows) {
        return new PageImpl<>(rows, PageRequest.of(0, 12), rows.size());
    }

    private FrontCatalogSummaryRow summary(
            String brand,
            String category,
            int price,
            int stock,
            LocalDate createdDate,
            boolean featured
    ) {
        return new FrontCatalogSummaryRow(brand, category, price, stock, createdDate.atTime(java.time.LocalTime.NOON), featured);
    }

    private void mockOptions() {
        when(productOptionRepository.findAllByProductNoInOrderByProductNoAscOptionNameAsc(List.of(101L, 102L, 103L)))
                .thenReturn(List.of(
                        option(101L, "260", 4),
                        option(102L, "265", 10),
                        option(103L, "245", 4)
                ));
    }

    private FrontCatalogProductRow row(
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
            LocalDate createdDate,
            String description,
            String mood,
            boolean featured,
            Integer featuredRank
    ) {
        return new FrontCatalogProductRow(
                productNo,
                brandNo,
                categoryNo,
                brandName,
                categoryName,
                productName,
                headline,
                modelNum,
                releasePrice,
                totalStock,
                LocalDateTime.of(createdDate, java.time.LocalTime.NOON),
                description,
                mood,
                featured,
                featuredRank,
                null
        );
    }

    private ProductOption option(Long productNo, String optionName, int stockCnt) {
        return ProductOption.builder()
                .productNo(productNo)
                .optionName(optionName)
                .stockCnt(stockCnt)
                .additionalPrice(0)
                .build();
    }
}
