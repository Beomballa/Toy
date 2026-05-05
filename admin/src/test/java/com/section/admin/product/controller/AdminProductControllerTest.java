package com.section.admin.product.controller;

import com.section.admin.product.res.ProductDefaultResDto;
import com.section.admin.product.res.ProductDetailResponse;
import com.section.admin.product.service.AdminProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminProductControllerTest {

    @Mock
    private AdminProductService adminProductService;

    @InjectMocks
    private AdminProductController adminProductController;

    @Test
    @DisplayName("상품 상세 화면은 서버에서 조회한 상품 모델을 뷰에 전달한다")
    void productGetAddsProductToModel() {
        ProductDetailResponse product = new ProductDetailResponse(
                4L, 2L, "스니커즈", 3L, "뉴발란스", "993", "MR993GL",
                259000, null, null, false, "ACTIVE", "판매중", "2026.04.20 14:24", "2026.04.26 17:09", 0, 0L, List.of()
        );
        ProductDefaultResDto defaultInfo = new ProductDefaultResDto(List.of(), List.of());
        ExtendedModelMap model = new ExtendedModelMap();

        when(adminProductService.getProductDetail(4L)).thenReturn(product);
        when(adminProductService.getProductDefaultInfo()).thenReturn(defaultInfo);

        String viewName = adminProductController.productGet("4", model, null);

        assertEquals("views/product-get", viewName);
        assertSame(product, model.get("product"));
        assertSame(defaultInfo.brands(), model.get("brands"));
        assertSame(defaultInfo.categories(), model.get("categories"));
    }

    @Test
    @DisplayName("상품 수정 화면도 서버에서 조회한 상품 모델을 뷰에 전달한다")
    void productUpdateAddsProductToModel() {
        ProductDetailResponse product = new ProductDetailResponse(
                4L, 2L, "스니커즈", 3L, "뉴발란스", "993", "MR993GL",
                259000, null, null, false, "ACTIVE", "판매중", "2026.04.20 14:24", "2026.04.26 17:09", 0, 0L, List.of()
        );
        ProductDefaultResDto defaultInfo = new ProductDefaultResDto(List.of(), List.of());
        ExtendedModelMap model = new ExtendedModelMap();

        when(adminProductService.getProductDetail(4L)).thenReturn(product);
        when(adminProductService.getProductDefaultInfo()).thenReturn(defaultInfo);

        String viewName = adminProductController.productUpdate("4", model, null);

        assertEquals("views/product-update", viewName);
        assertSame(product, model.get("product"));
        assertSame(defaultInfo.brands(), model.get("brands"));
        assertSame(defaultInfo.categories(), model.get("categories"));
    }
}
