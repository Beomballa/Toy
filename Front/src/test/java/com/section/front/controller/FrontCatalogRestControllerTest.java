package com.section.front.controller;

import com.section.front.product.dto.FrontCatalogBootstrapResponse;
import com.section.front.product.dto.FrontCatalogMetricsResponse;
import com.section.front.product.dto.FrontProductOptionResponse;
import com.section.front.product.dto.FrontProductResponse;
import com.section.front.product.service.FrontProductCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

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
        when(frontProductCatalogService.getBootstrap()).thenReturn(new FrontCatalogBootstrapResponse(
                List.of(new FrontProductResponse(
                        101L,
                        "New Balance",
                        "러닝화",
                        "990v6 Grey Day",
                        "M990GL6",
                        289000,
                        18,
                        "2026-06-04",
                        "설명",
                        "Grey precision",
                        true,
                        List.of(new FrontProductOptionResponse("260", 4))
                )),
                new FrontCatalogMetricsResponse(1, 1, "2026-06-04", 1, 1, 18)
        ));

        mockMvc.perform(get("/api/front/catalog/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0].id").value(101L))
                .andExpect(jsonPath("$.metrics.latestCreatedDate").value("2026-06-04"))
                .andExpect(jsonPath("$.metrics.latestDropCount").value(1));
    }
}
