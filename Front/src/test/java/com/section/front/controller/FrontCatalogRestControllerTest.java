package com.section.front.controller;

import com.section.front.product.dto.FrontCatalogBootstrapResponse;
import com.section.front.product.dto.FrontCatalogFacetResponse;
import com.section.front.product.dto.FrontCatalogMetricsResponse;
import com.section.front.product.dto.FrontCatalogPageResponse;
import com.section.front.product.dto.FrontProductOptionResponse;
import com.section.front.product.dto.FrontProductResponse;
import com.section.front.product.dto.FrontHomeCollectionsResponse;
import com.section.front.product.service.FrontProductCatalogService;
import com.section.common.commerce.dto.FrontCatalogQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FrontCatalogRestControllerTest {

    @Mock
    private FrontProductCatalogService frontProductCatalogService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FrontCatalogRestController(frontProductCatalogService)).build();
    }

    @Test
    @DisplayName("프론트 카탈로그 부트스트랩 API는 상품과 메트릭을 함께 반환한다")
    void getBootstrapReturnsCatalogAndMetrics() throws Exception {
        when(frontProductCatalogService.getBootstrap(any(FrontCatalogQuery.class), any(Pageable.class))).thenReturn(new FrontCatalogBootstrapResponse(
                List.of(new FrontProductResponse(
                        101L,
                        "New Balance",
                        "러닝화",
                        "990v6 Grey Day",
                        "Grey precision",
                        "M990GL6",
                        289000,
                        18,
                        "2026-06-04",
                        "설명",
                        "Grey precision",
                        true,
                        1,
                        "품절 임박",
                        "289,000원",
                        List.of(new FrontProductOptionResponse("260", 4)),
                        "/images/product/m990gl6.png"
                )),
                new FrontCatalogPageResponse(0, 12, 1, 1, true, true),
                new FrontCatalogMetricsResponse(1, 1, "2026-06-04", 1, 1, 18),
                List.of(new FrontCatalogFacetResponse("New Balance", 1)),
                List.of(new FrontCatalogFacetResponse("러닝화", 1))
        ));

        mockMvc.perform(get("/api/front/catalog/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0].id").value(101L))
                .andExpect(jsonPath("$.products[0].headline").value("Grey precision"))
                .andExpect(jsonPath("$.products[0].priceLabel").value("289,000원"))
                .andExpect(jsonPath("$.products[0].thumbnailUrl").value("/images/product/m990gl6.png"))
                .andExpect(jsonPath("$.pagination.totalElements").value(1))
                .andExpect(jsonPath("$.metrics.latestCreatedDate").value("2026-06-04"))
                .andExpect(jsonPath("$.metrics.latestDropCount").value(1))
                .andExpect(jsonPath("$.brandFacets[0].value").value("New Balance"))
                .andExpect(jsonPath("$.categoryFacets[0].count").value(1));
    }

    @Test
    @DisplayName("프론트 카탈로그 부트스트랩 API는 필터 요청을 서비스 쿼리로 전달한다")
    void getBootstrapPassesNormalizedQuery() throws Exception {
        when(frontProductCatalogService.getBootstrap(any(FrontCatalogQuery.class), any(Pageable.class))).thenReturn(new FrontCatalogBootstrapResponse(
                List.of(),
                new FrontCatalogPageResponse(2, 24, 100, 5, false, false),
                new FrontCatalogMetricsResponse(0, 0, null, 0, 0, 0),
                List.of(),
                List.of()
        ));

        mockMvc.perform(get("/api/front/catalog/bootstrap")
                        .param("keyword", " 990v6 ")
                        .param("brand", "New Balance")
                        .param("category", "러닝화")
                        .param("stock", "low")
                        .param("sort", "name_asc")
                        .param("lowStockThreshold", "30")
                        .param("featuredOnly", "true")
                        .param("priceBand", "under_200")
                        .param("page", "2")
                        .param("size", "24"))
                .andExpect(status().isOk());

        verify(frontProductCatalogService).getBootstrap(argThat(query ->
                "990v6".equals(query.keyword())
                        && "New Balance".equals(query.brand())
                        && "러닝화".equals(query.category())
                        && "LOW".equals(query.stock())
                        && "NAME_ASC".equals(query.sort())
                        && query.lowStockThreshold() == 30
                        && query.featuredOnly()
                        && "UNDER_200".equals(query.priceBand())
        ), argThat(pageable -> pageable.getPageNumber() == 2 && pageable.getPageSize() == 24));
    }

    @Test
    @DisplayName("홈 컬렉션 API는 추천 랭킹 빠른배송 신상 저재고를 분리해 반환한다")
    void getHomeCollectionsReturnsIndependentSections() throws Exception {
        FrontProductResponse product = new FrontProductResponse(
                101L, "New Balance", "러닝화", "990v6", "Grey", "M990",
                289000, 18, "2026-07-25", "설명", "Grey", true, 1,
                "품절 임박", "289,000원", List.of(), null
        );
        when(frontProductCatalogService.getHomeCollections()).thenReturn(new FrontHomeCollectionsResponse(
                List.of(product),
                List.of(product),
                List.of(product),
                List.of(product),
                List.of(product)
        ));

        mockMvc.perform(get("/api/front/catalog/home-collections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommended[0].id").value(101L))
                .andExpect(jsonPath("$.ranking[0].brand").value("New Balance"))
                .andExpect(jsonPath("$.fastDelivery[0].stock").value(18))
                .andExpect(jsonPath("$.latestDrops[0].name").value("990v6"))
                .andExpect(jsonPath("$.lowStock[0].featured").value(true));

        verify(frontProductCatalogService).getHomeCollections();
    }
}
