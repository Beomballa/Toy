package com.section.admin.product.service;

import com.section.admin.product.req.ProductCreateRequest;
import com.section.admin.product.req.ProductListRequest;
import com.section.admin.product.req.ProductUpdateRequest;
import com.section.admin.product.res.ProductListResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.dto.ProductListQuery;
import com.section.common.commerce.dto.ProductListResDto;
import com.section.common.commerce.dto.ProductStatsDto;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.entity.Category;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.CategoryRepository;
import com.section.common.commerce.repository.ProductOptionRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.common.commerce.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    @DisplayName("공백 정규화 후 중복되는 상품 옵션명으로 생성 시 INVALID_INPUT_VALUE 예외를 던진다")
    void createProductInfoThrowsBusinessExceptionWhenOptionNamesDuplicated() {
        ProductCreateRequest.ProductOptionRequest firstOption = new ProductCreateRequest.ProductOptionRequest();
        firstOption.setOptionName("270");
        firstOption.setStockCnt(1);
        firstOption.setAdditionalPrice(0);

        ProductCreateRequest.ProductOptionRequest duplicatedOption = new ProductCreateRequest.ProductOptionRequest();
        duplicatedOption.setOptionName(" 270 ");
        duplicatedOption.setStockCnt(2);
        duplicatedOption.setAdditionalPrice(0);

        ProductCreateRequest request = new ProductCreateRequest();
        request.setBrandNo(1L);
        request.setCategoryNo(2L);
        request.setNameKo("테스트 상품");
        request.setReleasePrice(1000);
        request.setOptions(List.of(firstOption, duplicatedOption));

        when(brandRepository.existsById(1L)).thenReturn(true);
        when(categoryRepository.existsById(2L)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> adminProductService.createProductInfo(request));

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("공백 정규화 후 중복되는 상품 옵션명으로 수정 시 INVALID_INPUT_VALUE 예외를 던진다")
    void updateProductInfoThrowsBusinessExceptionWhenOptionNamesDuplicated() {
        ProductUpdateRequest.ProductOptionUpdateRequest firstOption = new ProductUpdateRequest.ProductOptionUpdateRequest();
        firstOption.setOptionName("280");
        firstOption.setStockCnt(1);
        firstOption.setAdditionalPrice(0);

        ProductUpdateRequest.ProductOptionUpdateRequest duplicatedOption = new ProductUpdateRequest.ProductOptionUpdateRequest();
        duplicatedOption.setOptionName(" 280 ");
        duplicatedOption.setStockCnt(2);
        duplicatedOption.setAdditionalPrice(0);

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setProductNo(1L);
        request.setBrandNo(1L);
        request.setCategoryNo(2L);
        request.setNameKo("테스트 상품");
        request.setReleasePrice(1000);
        request.setOptions(List.of(firstOption, duplicatedOption));

        when(productRepository.findById(1L)).thenReturn(Optional.of(Product.builder()
                .id(1L)
                .brandNo(1L)
                .categoryNo(2L)
                .nameKo("기존 상품")
                .status("ACTIVE")
                .releasePrice(1000)
                .build()));
        when(brandRepository.existsById(1L)).thenReturn(true);
        when(categoryRepository.existsById(2L)).thenReturn(true);

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

        when(productService.getProductExportList(org.mockito.ArgumentMatchers.any(ProductListQuery.class), org.mockito.ArgumentMatchers.eq(1000)))
                .thenReturn(List.of(dto));

        byte[] csv = adminProductService.exportProductListCsv(request);

        assertEquals(true, csv.length > 0);
        verify(productService).getProductExportList(org.mockito.ArgumentMatchers.any(ProductListQuery.class), org.mockito.ArgumentMatchers.eq(1000));
    }

    @Test
    @DisplayName("상품 CSV 내보내기는 현재 조회 조건 요약을 함께 포함한다")
    void exportProductListCsvIncludesFilterSummary() {
        ProductListRequest request = new ProductListRequest();
        request.setBrandNo(7L);
        request.setCategoryNo(3L);
        request.setStatus("ACTIVE");
        request.setSearchKeyword("뉴발란스 993");
        request.setLowStockOnly(true);
        request.setLowStockThreshold(30L);

        ProductListResDto dto = new ProductListResDto();
        dto.setProductNo(1L);
        dto.setProductName("테스트 상품");
        dto.setStatus("ACTIVE");

        Brand brand = Brand.builder().brandNo(7L).nameKo("뉴발란스").build();
        Category category = Category.builder().categoryNo(3L).name("러닝화").build();

        when(productService.getProductExportList(org.mockito.ArgumentMatchers.any(ProductListQuery.class), org.mockito.ArgumentMatchers.eq(1000)))
                .thenReturn(List.of(dto));
        when(brandRepository.findById(7L)).thenReturn(Optional.of(brand));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));

        byte[] csv = adminProductService.exportProductListCsv(request);
        String body = new String(csv, StandardCharsets.UTF_8);

        assertEquals(true, body.contains("\"조회조건\",\"브랜드: 뉴발란스 | 카테고리: 러닝화 | 상태: 판매중 | 저재고: 30개 미만 | 검색어: 뉴발란스 993\""));
        assertEquals(true, body.contains("\"정렬\",\"최신순\""));
    }

    @Test
    @DisplayName("상품 목록 조회는 같은 typed query를 목록과 통계 조회에 재사용한다")
    void getProductListReusesSameTypedQueryForListAndStats() {
        ProductListRequest request = new ProductListRequest();
        request.setStatus("ACTIVE");
        request.setLowStockOnly(true);
        request.setLowStockThreshold(30L);
        request.setCreatedTodayOnly(true);
        request.setSearchKeyword("  젤   카야노  ");

        when(productService.getProductList(any(ProductListQuery.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(productService.getProductStats(any(ProductListQuery.class)))
                .thenReturn(new ProductStatsDto());

        adminProductService.getProductList(request, PageRequest.of(0, 10));

        ArgumentCaptor<ProductListQuery> listQueryCaptor = ArgumentCaptor.forClass(ProductListQuery.class);
        ArgumentCaptor<ProductListQuery> statsQueryCaptor = ArgumentCaptor.forClass(ProductListQuery.class);
        verify(productService).getProductList(listQueryCaptor.capture(), any(PageRequest.class));
        verify(productService).getProductStats(statsQueryCaptor.capture());

        assertSame(listQueryCaptor.getValue(), statsQueryCaptor.getValue());
        assertEquals(true, listQueryCaptor.getValue().lowStockOnly());
        assertEquals(30L, listQueryCaptor.getValue().effectiveLowStockThreshold());
        assertEquals(true, listQueryCaptor.getValue().createdTodayOnly());
        assertEquals("젤 카야노", listQueryCaptor.getValue().searchKeyword());
    }

    @Test
    @DisplayName("상품 목록 응답 통계는 현재 저재고 기준값을 함께 내려준다")
    void getProductListIncludesEffectiveLowStockThresholdInStats() {
        ProductListRequest request = new ProductListRequest();
        request.setLowStockOnly(true);
        request.setLowStockThreshold(50L);

        ProductStatsDto statsDto = new ProductStatsDto();
        statsDto.setLowStockCount(3L);

        when(productService.getProductList(any(ProductListQuery.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(productService.getProductStats(any(ProductListQuery.class)))
                .thenReturn(statsDto);

        ProductListResponse response = adminProductService.getProductList(request, PageRequest.of(0, 10));

        assertEquals(50L, response.productStats().lowStockThreshold());
        assertEquals(3L, response.productStats().lowStockCount());
        assertEquals("현재 목록 기준", response.productStats().contextLabel());
        assertEquals("최신순 · 재고<50", response.productStats().querySignature());
    }

    @Test
    @DisplayName("상품 목록 응답은 실제 조회에 사용한 typed query를 함께 내려준다")
    void getProductListIncludesAppliedQuery() {
        ProductListRequest request = new ProductListRequest();
        request.setBrandNo(7L);
        request.setCategoryNo(3L);
        request.setStatus("ACTIVE");
        request.setSearchKeyword("  뉴발란스   993 ");
        request.setOrderType("c");
        request.setLowStockOnly(true);
        request.setLowStockThreshold(30L);

        when(productService.getProductList(any(ProductListQuery.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(productService.getProductStats(any(ProductListQuery.class)))
                .thenReturn(new ProductStatsDto());

        ProductListResponse response = adminProductService.getProductList(request, PageRequest.of(0, 10));

        assertEquals(7L, response.appliedQuery().brandNo());
        assertEquals(3L, response.appliedQuery().categoryNo());
        assertEquals("ACTIVE", response.appliedQuery().statusCode());
        assertEquals("뉴발란스 993", response.appliedQuery().searchKeyword());
        assertEquals("c", response.appliedQuery().orderTypeCode());
        assertEquals(true, response.appliedQuery().lowStockOnly());
        assertEquals(30L, response.appliedQuery().lowStockThreshold());
    }

    @Test
    @DisplayName("상품 목록 응답은 현재 결과 문맥을 설명하는 메타를 함께 내려준다")
    void getProductListIncludesResultMeta() {
        ProductListRequest request = new ProductListRequest();
        request.setStatus("ACTIVE");
        request.setSearchKeyword("  뉴발란스   993 ");
        request.setOrderType("p");

        ProductListResDto first = new ProductListResDto();
        first.setProductNo(11L);
        first.setProductName("첫 번째");
        first.setStatus("ACTIVE");
        first.setReleasePrice(1000);

        ProductListResDto second = new ProductListResDto();
        second.setProductNo(12L);
        second.setProductName("두 번째");
        second.setStatus("ACTIVE");
        second.setReleasePrice(1000);

        when(productService.getProductList(any(ProductListQuery.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(1, 10), 12));
        when(productService.getProductStats(any(ProductListQuery.class)))
                .thenReturn(new ProductStatsDto());

        ProductListResponse response = adminProductService.getProductList(request, PageRequest.of(0, 10));

        assertEquals("검색 결과 12개", response.resultMeta().resultLabel());
        assertEquals("11-12 / 12개 · 2페이지", response.resultMeta().pageInfoLabel());
        assertEquals("발매가순", response.resultMeta().orderTypeLabel());
        assertEquals(10, response.resultMeta().pageSize());
        assertEquals(11L, response.resultMeta().rangeStart());
        assertEquals(12L, response.resultMeta().rangeEnd());
        assertEquals(2L, response.resultMeta().appliedFilterCount());
        assertEquals(true, response.resultMeta().hasActiveFilters());
        assertEquals("발매가순 · 검색=뉴발란스 993 · 상태=ACTIVE", response.resultMeta().querySignature());
        assertEquals(response.resultMeta().querySignature(), response.productStats().querySignature());
    }
}
