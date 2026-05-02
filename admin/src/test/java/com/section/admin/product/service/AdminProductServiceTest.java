package com.section.admin.product.service;

import com.section.admin.product.req.ProductCreateRequest;
import com.section.admin.product.req.ProductListRequest;
import com.section.admin.product.req.ProductUpdateRequest;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.dto.ProductListReqDto;
import com.section.common.commerce.dto.ProductListResDto;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.CategoryRepository;
import com.section.common.commerce.repository.ProductOptionRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.common.commerce.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductOptionRepository productOptionRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductService productService;

    @InjectMocks
    private AdminProductService adminProductService;

    @Test
    @DisplayName("존재하지 않는 브랜드나 카테고리로 상품 생성 시 INVALID_INPUT_VALUE 예외를 던진다")
    void createProductInfoThrowsBusinessExceptionWhenBrandOrCategoryMissing() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setBrandNo(99L);
        request.setCategoryNo(88L);

        when(brandRepository.existsById(99L)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> adminProductService.createProductInfo(request));

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 생성 성공 시 저장된 상품 번호를 반환한다")
    void createProductInfoReturnsSavedProductNo() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setBrandNo(1L);
        request.setCategoryNo(2L);
        request.setNameKo("테스트 상품");
        request.setReleasePrice(1000);

        when(brandRepository.existsById(1L)).thenReturn(true);
        when(categoryRepository.existsById(2L)).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenReturn(Product.builder()
                .id(33L)
                .brandNo(1L)
                .categoryNo(2L)
                .nameKo("테스트 상품")
                .status("ACTIVE")
                .releasePrice(1000)
                .build());

        Long productNo = adminProductService.createProductInfo(request);

        assertEquals(33L, productNo);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("잘못된 상태값으로 상품 수정 시 INVALID_INPUT_VALUE 예외를 던진다")
    void updateProductInfoThrowsBusinessExceptionWhenStatusInvalid() {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setProductNo(1L);
        request.setBrandNo(1L);
        request.setCategoryNo(1L);
        request.setNameKo("테스트 상품");
        request.setReleasePrice(1000);
        request.setStatus("invalid-status");

        when(productRepository.findById(1L)).thenReturn(Optional.of(Product.builder()
                .id(1L)
                .brandNo(1L)
                .categoryNo(1L)
                .nameKo("기존 상품")
                .status("ACTIVE")
                .releasePrice(1000)
                .build()));
        when(brandRepository.existsById(1L)).thenReturn(true);
        when(categoryRepository.existsById(1L)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> adminProductService.updateProductInfo(request));

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 CSV 내보내기는 export 전용 조회 경계로 최대 건수를 제한한다")
    void exportProductListCsvUsesDedicatedExportQuery() {
        ProductListRequest request = new ProductListRequest();
        ProductListResDto dto = new ProductListResDto();
        dto.setProductNo(1L);
        dto.setProductName("테스트 상품");
        dto.setStatus("ACTIVE");

        when(productService.getProductExportList(org.mockito.ArgumentMatchers.any(ProductListReqDto.class), org.mockito.ArgumentMatchers.eq(1000)))
                .thenReturn(List.of(dto));

        byte[] csv = adminProductService.exportProductListCsv(request);

        assertEquals(true, csv.length > 0);
        verify(productService).getProductExportList(org.mockito.ArgumentMatchers.any(ProductListReqDto.class), org.mockito.ArgumentMatchers.eq(1000));
    }
}
