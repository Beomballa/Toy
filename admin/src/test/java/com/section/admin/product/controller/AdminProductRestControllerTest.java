package com.section.admin.product.controller;

import com.section.admin.common.controller.AdminGlobalExceptionHandler;
import com.section.admin.product.service.AdminProductService;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminProductRestControllerTest {

    @Mock
    private AdminProductService adminProductService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminProductRestController(adminProductService))
                .setControllerAdvice(new AdminGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("존재하지 않는 상품 상세 조회는 404 PRODUCT_NOT_FOUND를 반환한다")
    void getProductDetailReturnsNotFoundWhenProductMissing() throws Exception {
        when(adminProductService.getProductDetail(999L))
                .thenThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        mockMvc.perform(get("/api/admin/product/get").param("no", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("P001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 상품입니다."));
    }

    @Test
    @DisplayName("존재하지 않는 상품 삭제는 404 PRODUCT_NOT_FOUND를 반환한다")
    void deleteProductReturnsNotFoundWhenProductMissing() throws Exception {
        doThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND))
                .when(adminProductService)
                .deleteProduct(999L);

        mockMvc.perform(patch("/api/admin/product/delete/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("P001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 상품입니다."));
    }
}
