package com.section.front.system.controller;

import com.section.front.controller.FrontProductRestController;
import com.section.front.product.service.FrontProductCatalogService;
import com.section.common.commerce.dto.FrontCatalogQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FrontGlobalExceptionHandlerTest {

    private FrontProductCatalogService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(FrontProductCatalogService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new FrontProductRestController(service))
                .setControllerAdvice(new FrontGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("숫자가 아닌 상품 번호는 표준 400 오류를 반환한다")
    void invalidProductIdReturnsBadRequestContract() throws Exception {
        mockMvc.perform(get("/api/front/products/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("F001"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("0 이하 상품 번호는 서비스 조회 없이 표준 400 오류를 반환한다")
    void nonPositiveProductIdReturnsBadRequestContract() throws Exception {
        mockMvc.perform(get("/api/front/products/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("F001"));
    }

    @Test
    @DisplayName("길이 제한을 넘은 검색 조건은 표준 400 오류를 반환한다")
    void oversizedKeywordReturnsBadRequestContract() throws Exception {
        mockMvc.perform(get("/api/front/products").param("keyword", "a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("F001"));
    }

    @Test
    @DisplayName("없는 상품은 표준 404 오류를 반환한다")
    void missingProductReturnsNotFoundContract() throws Exception {
        when(service.findProductDetail(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/front/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("F002"))
                .andExpect(jsonPath("$.message").value("상품을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("서버 예외는 내부 정보를 노출하지 않는 500 오류를 반환한다")
    void unexpectedErrorReturnsGenericContract() throws Exception {
        when(service.getCatalog(any(FrontCatalogQuery.class), any(org.springframework.data.domain.Pageable.class)))
                .thenThrow(new IllegalStateException("database-secret"));

        mockMvc.perform(get("/api/front/products"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("F003"))
                .andExpect(jsonPath("$.message").value("상품 정보를 불러오지 못했습니다."));
    }

    @Test
    @DisplayName("주문 조회 제한 초과는 표준 429 오류를 반환한다")
    void rateLimitReturnsTooManyRequestsContract() throws Exception {
        when(service.getCatalog(any(FrontCatalogQuery.class), any(org.springframework.data.domain.Pageable.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 시도해주세요."));

        mockMvc.perform(get("/api/front/products"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("F005"))
                .andExpect(jsonPath("$.status").value(429));
    }
}
