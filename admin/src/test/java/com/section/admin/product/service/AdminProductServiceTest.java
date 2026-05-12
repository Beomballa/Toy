package com.section.admin.product.service;

import com.section.admin.product.req.ProductCreateRequest;
import com.section.admin.product.req.ProductHistoryListRequest;
import com.section.admin.product.req.ProductListRequest;
import com.section.admin.product.req.ProductUpdateRequest;
import com.section.admin.product.res.ProductHistoryListResponse;
import com.section.admin.product.res.ProductListResponse;
import com.section.admin.settings.service.AdminSettingsService;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.dto.ProductHistoryListResDto;
import com.section.common.commerce.dto.ProductListQuery;
import com.section.common.commerce.dto.ProductListResDto;
import com.section.common.commerce.dto.ProductStatsDto;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.entity.Category;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.entity.ProductChangeHistory;
import com.section.common.commerce.entity.ProductOption;
import com.section.common.system.entity.AdminUser;
import com.section.common.commerce.repository.ProductChangeHistoryRepository;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.CategoryRepository;
import com.section.common.commerce.repository.ProductOptionRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.common.commerce.service.ProductService;
import com.section.common.system.repository.AdminUserRepository;
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
import static org.mockito.ArgumentMatchers.argThat;
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
    private ProductChangeHistoryRepository productChangeHistoryRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductService productService;
    @Mock
    private AdminUserRepository adminUserRepository;
    @Mock
    private AdminSettingsService adminSettingsService;

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
        verify(productChangeHistoryRepository).save(argThat(history ->
                history.getProductNo().equals(33L)
                        && history.getSummary().equals("상품이 새로 등록되었습니다.")
                        && history.getStatusSnapshot().equals("ACTIVE")
        ));
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

        BusinessException exception = assertThrows(BusinessException.class, () -> adminProductService.updateProductInfo(request));

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 수정 성공 시 변경 이력을 함께 저장한다")
    void updateProductInfoRecordsHistory() {
        ProductUpdateRequest.ProductOptionUpdateRequest option = new ProductUpdateRequest.ProductOptionUpdateRequest();
        option.setOptionName(" 280 ");
        option.setStockCnt(3);
        option.setAdditionalPrice(5000);

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setProductNo(1L);
        request.setBrandNo(2L);
        request.setCategoryNo(3L);
        request.setNameKo("수정 상품");
        request.setModelNum("M992GR");
        request.setReleasePrice(259000);
        request.setStatus("ACTIVE");
        request.setOptions(List.of(option));

        when(productRepository.findById(1L)).thenReturn(Optional.of(Product.builder()
                .id(1L)
                .brandNo(1L)
                .categoryNo(2L)
                .nameKo("기존 상품")
                .modelNum("OLD")
                .status("HIDDEN")
                .releasePrice(1000)
                .build()));
        when(brandRepository.existsById(2L)).thenReturn(true);
        when(categoryRepository.existsById(3L)).thenReturn(true);

        adminProductService.updateProductInfo(request);

        verify(productChangeHistoryRepository).save(argThat(history ->
                history.getProductNo().equals(1L)
                        && history.getSummary().contains("브랜드")
                        && history.getSummary().contains("카테고리")
                        && history.getSummary().contains("상품명")
                        && history.getSummary().contains("모델번호")
                        && history.getSummary().contains("발매가")
                        && history.getSummary().contains("상태")
                        && history.getSummary().contains("옵션")
                        && history.getOptionCount().equals(1)
                        && history.getTotalStock().equals(3L)
        ));
    }

    @Test
    @DisplayName("상품 삭제 성공 시 삭제 이력을 저장한다")
    void deleteProductRecordsHistory() {
        when(productRepository.findById(9L)).thenReturn(Optional.of(Product.builder()
                .id(9L)
                .brandNo(1L)
                .categoryNo(1L)
                .nameKo("삭제 대상")
                .status("ACTIVE")
                .releasePrice(1000)
                .build()));

        adminProductService.deleteProduct(9L);

        verify(productChangeHistoryRepository).save(argThat(history ->
                history.getProductNo().equals(9L)
                        && history.getSummary().equals("상품이 삭제 처리되었습니다.")
                        && history.getStatusSnapshot().equals("DELETE")
        ));
    }

    @Test
    @DisplayName("상품 이력 조회는 작업자 이름을 함께 내려준다")
    void getProductHistoryIncludesActorName() {
        ProductChangeHistory history = ProductChangeHistory.of(
                4L,
                com.section.common.base.entity.type.ProductHistoryActionType.UPDATED,
                "변경 항목: 상품명",
                "ACTIVE",
                2,
                8L
        );
        history.setCrtNo(1L);

        when(productRepository.existsById(4L)).thenReturn(true);
        when(productChangeHistoryRepository.findTop20ByProductNoOrderByHistoryNoDesc(4L)).thenReturn(List.of(history));
        when(adminUserRepository.findAllById(any())).thenReturn(List.of(
                AdminUser.builder().adminNo(1L).name("관리자").loginId("admin").password("pw").build()
        ));

        List<com.section.admin.product.res.ProductHistoryResponse> histories = adminProductService.getProductHistory(4L);

        assertEquals(1, histories.size());
        assertEquals("관리자", histories.get(0).actorName());
    }

    @Test
    @DisplayName("상품 변경 이력 목록은 필터 결과와 작업자명을 함께 반환한다")
    void getProductHistoryListReturnsPagedHistoryWithActorName() {
        ProductHistoryListRequest request = new ProductHistoryListRequest();
        request.setProductNo(4L);
        request.setActionType("UPDATED");

        ProductHistoryListResDto row = new ProductHistoryListResDto();
        row.setHistoryNo(7L);
        row.setProductNo(4L);
        row.setActionType("UPDATED");
        row.setSummary("변경 항목: 상품명");
        row.setStatusSnapshot("ACTIVE");
        row.setOptionCount(2);
        row.setTotalStock(8L);
        row.setActorNo(1L);
        row.setActionDtm(java.time.LocalDateTime.of(2026, 5, 11, 10, 0));

        when(productChangeHistoryRepository.getProductHistoryList(any(), any()))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1));
        when(adminUserRepository.findAllById(any()))
                .thenReturn(List.of(AdminUser.builder().adminNo(1L).name("관리자").loginId("admin").password("pw").build()));

        ProductHistoryListResponse response = adminProductService.getProductHistoryList(request, PageRequest.of(0, 20));

        assertEquals(1, response.items().size());
        assertEquals("관리자", response.items().get(0).actorName());
        assertEquals("UPDATED", response.appliedQuery().actionType());
        assertEquals(1L, response.totalElements());
    }

    @Test
    @DisplayName("상품 복제는 원본 상품과 옵션을 복사하고 숨김 상태로 생성한다")
    void cloneProductCopiesSourceProductAndOptions() {
        Product source = Product.builder()
                .id(5L)
                .categoryNo(2L)
                .brandNo(3L)
                .nameKo("원본 상품")
                .modelNum("M-01")
                .releasePrice(1000)
                .status("ACTIVE")
                .build();
        Product saved = Product.builder()
                .id(10L)
                .categoryNo(2L)
                .brandNo(3L)
                .nameKo("원본 상품 (복제)")
                .modelNum("M-01")
                .releasePrice(1000)
                .status("HIDDEN")
                .build();

        when(productRepository.findById(5L)).thenReturn(Optional.of(source));
        when(productRepository.save(any(Product.class))).thenReturn(saved);
        when(productOptionRepository.findByProductId(5L)).thenReturn(List.of(
                ProductOption.builder().productNo(5L).optionName("260").stockCnt(2).additionalPrice(0).build()
        ));

        Long clonedProductNo = adminProductService.cloneProduct(5L);

        assertEquals(10L, clonedProductNo);
        verify(productOptionRepository).saveAll(any());
        verify(productChangeHistoryRepository).save(argThat(history ->
                history.getProductNo().equals(10L)
                        && history.getSummary().contains("원본 상품 번호: 5")
                        && history.getStatusSnapshot().equals("HIDDEN")
        ));
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

        when(adminSettingsService.getLowStockDefaultThreshold()).thenReturn(100L);
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

        when(adminSettingsService.getLowStockDefaultThreshold()).thenReturn(100L);
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

        when(adminSettingsService.getLowStockDefaultThreshold()).thenReturn(100L);
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

        when(adminSettingsService.getLowStockDefaultThreshold()).thenReturn(100L);
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

        when(adminSettingsService.getLowStockDefaultThreshold()).thenReturn(100L);
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

        when(adminSettingsService.getLowStockDefaultThreshold()).thenReturn(100L);
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
