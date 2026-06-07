package com.section.front.controller;

import com.section.front.product.dto.FrontProductDetailResponse;
import com.section.front.product.dto.FrontProductOptionResponse;
import com.section.front.product.dto.FrontRelatedProductResponse;
import com.section.front.product.dto.FrontProductResponse;
import com.section.front.product.service.FrontProductCatalogService;
import com.section.common.commerce.dto.FrontCatalogQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
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
class FrontProductRestControllerTest {

    @Mock
    private FrontProductCatalogService frontProductCatalogService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FrontProductRestController(frontProductCatalogService)).build();
    }

    @Test
    @DisplayName("프론트 상품 API는 카탈로그 목록을 반환한다")
    void getProductsReturnsCatalog() throws Exception {
        when(frontProductCatalogService.getCatalog(any(FrontCatalogQuery.class))).thenReturn(List.of(
                new FrontProductResponse(
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
                        List.of(new FrontProductOptionResponse("260", 4))
                )
        ));

        mockMvc.perform(get("/api/front/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(101L))
                .andExpect(jsonPath("$[0].brand").value("New Balance"))
                .andExpect(jsonPath("$[0].headline").value("Grey precision"))
                .andExpect(jsonPath("$[0].stockStatus").value("품절 임박"))
                .andExpect(jsonPath("$[0].options[0].name").value("260"));
    }

    @Test
    @DisplayName("프론트 상품 API는 필터 요청을 서비스 쿼리로 전달한다")
    void getProductsPassesNormalizedQuery() throws Exception {
        when(frontProductCatalogService.getCatalog(any(FrontCatalogQuery.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/front/products")
                        .param("stock", "stable")
                        .param("sort", "name_asc")
                        .param("lowStockThreshold", "50")
                        .param("featuredOnly", "true")
                        .param("priceBand", "between_200_300"))
                .andExpect(status().isOk());

        verify(frontProductCatalogService).getCatalog(argThat(query ->
                "STABLE".equals(query.stock())
                        && "NAME_ASC".equals(query.sort())
                        && query.lowStockThreshold() == 50
                        && query.featuredOnly()
                        && "BETWEEN_200_300".equals(query.priceBand())
        ));
    }

    @Test
    @DisplayName("프론트 상품 상세 API는 단건 응답을 반환한다")
    void getProductReturnsDetail() throws Exception {
        when(frontProductCatalogService.findProductDetail(101L)).thenReturn(java.util.Optional.of(
                new FrontProductDetailResponse(
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
                        List.of(new FrontRelatedProductResponse(103L, "ASICS", "Gel-Kayano 14 Oyster", "같은 카테고리", "1201A019-200", 179000, 12, "품절 임박", "179,000원"))
                )
        ));

        mockMvc.perform(get("/api/front/products/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101L))
                .andExpect(jsonPath("$.name").value("990v6 Grey Day"))
                .andExpect(jsonPath("$.headline").value("Grey precision"))
                .andExpect(jsonPath("$.priceLabel").value("289,000원"))
                .andExpect(jsonPath("$.relatedProducts[0].reason").value("같은 카테고리"))
                .andExpect(jsonPath("$.relatedProducts[0].id").value(103L));
    }

    @Test
    @DisplayName("프론트 상품 상세 API는 없는 상품 번호면 404를 반환한다")
    void getProductReturnsNotFoundWhenMissing() throws Exception {
        when(frontProductCatalogService.findProductDetail(999L)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/front/products/999"))
                .andExpect(status().isNotFound());
    }
}
