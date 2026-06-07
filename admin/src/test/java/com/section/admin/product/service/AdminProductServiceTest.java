package com.section.admin.product.service;

import com.section.admin.product.req.ProductCreateRequest;
import com.section.admin.product.req.ProductBulkDeleteRequest;
import com.section.admin.product.req.ProductBulkDuplicateRequest;
import com.section.admin.product.req.ProductBulkOperateRequest;
import com.section.admin.product.req.ProductFrontDisplayListRequest;
import com.section.admin.product.req.ProductFrontDisplaySaveRequest;
import com.section.admin.product.req.ProductHistoryListRequest;
import com.section.admin.product.req.ProductListRequest;
import com.section.admin.product.req.ProductUpdateRequest;
import com.section.admin.product.res.ProductDefaultResDto;
import com.section.admin.product.res.ProductFrontDisplayDashboardResponse;
import com.section.admin.product.res.ProductFrontDisplayResponse;
import com.section.admin.product.res.ProductFrontDisplayListResponse;
import com.section.admin.product.res.ProductHistoryListResponse;
import com.section.admin.product.res.ProductHistoryResponse;
import com.section.admin.product.res.ProductListResponse;
import com.section.admin.settings.service.AdminSettingsService;
import com.section.common.base.entity.type.ProductHistoryActionType;
import com.section.common.base.entity.type.ProductStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.dto.ProductHistoryListResDto;
import com.section.common.commerce.dto.ProductListQuery;
import com.section.common.commerce.dto.ProductListResDto;
import com.section.common.commerce.dto.ProductStatsDto;
import com.section.common.commerce.dto.AdminFrontDisplayProductRow;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.entity.Category;
import com.section.common.commerce.entity.FrontProductDisplay;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.entity.ProductChangeHistory;
import com.section.common.commerce.entity.ProductOption;
import com.section.common.system.entity.AdminUser;
import com.section.common.commerce.repository.ProductChangeHistoryRepository;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.CategoryRepository;
import com.section.common.commerce.repository.FrontProductDisplayRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
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
    private FrontProductDisplayRepository frontProductDisplayRepository;
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

        when(brandRepository.findById(99L)).thenReturn(Optional.empty());

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

        when(brandRepository.findById(1L)).thenReturn(Optional.of(
                Brand.builder().brandNo(1L).nameKo("나이키").isActive("Y").build()
        ));
        when(categoryRepository.findById(2L)).thenReturn(
                Optional.of(Category.builder().categoryNo(2L).parentNo(1L).name("러닝화").depth(2).isActive("Y").build())
        );
        when(categoryRepository.findById(1L)).thenReturn(
                Optional.of(Category.builder().categoryNo(1L).name("신발").depth(1).isActive("Y").build())
        );
        when(categoryRepository.existsByParentNo(2L)).thenReturn(false);
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
    @DisplayName("상품 기본 선택 데이터는 활성 항목만 정렬해서 반환한다")
    void getProductDefaultInfoReturnsOnlyActiveAndSortedItems() {
        when(brandRepository.findByIsActiveOrderByNameKoAsc("Y")).thenReturn(List.of(
                Brand.builder().brandNo(2L).nameKo("뉴발란스").nameEn("New Balance").isActive("Y").build(),
                Brand.builder().brandNo(1L).nameKo("나이키").nameEn("Nike").isActive("Y").build()
        ));
        when(categoryRepository.findByIsActiveOrderByDepthAscNameAscCategoryNoAsc("Y")).thenReturn(List.of(
                Category.builder().categoryNo(3L).parentNo(1L).name("러닝화").depth(2).isActive("Y").build(),
                Category.builder().categoryNo(1L).name("신발").depth(1).isActive("Y").build(),
                Category.builder().categoryNo(2L).name("의류").depth(1).isActive("Y").build()
        ));

        var response = adminProductService.getProductDefaultInfo();

        assertIterableEquals(List.of("뉴발란스", "나이키"), response.brands().stream().map(ProductDefaultResDto.BrandSimpleDto::nameKo).toList());
        assertIterableEquals(List.of("신발", "의류", "러닝화"), response.categories().stream().map(ProductDefaultResDto.CategorySimpleDto::name).toList());
    }

    @Test
    @DisplayName("잘못된 상태값으로 상품 수정 시 INVALID_INPUT_VALUE 예외를 던진다")
    void updateProductInfoThrowsBusinessExceptionWhenStatusInvalid() {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setProductNo(1L);
        request.setBrandNo(1L);
        request.setCategoryNo(2L);
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
        when(brandRepository.findById(2L)).thenReturn(Optional.of(
                Brand.builder().brandNo(2L).nameKo("뉴발란스").isActive("Y").build()
        ));
        when(categoryRepository.findById(3L)).thenReturn(
                Optional.of(Category.builder().categoryNo(3L).parentNo(1L).name("러닝화").depth(2).isActive("Y").build())
        );
        when(categoryRepository.findById(1L)).thenReturn(
                Optional.of(Category.builder().categoryNo(1L).name("신발").depth(1).isActive("Y").build())
        );
        when(categoryRepository.existsByParentNo(3L)).thenReturn(false);
        when(frontProductDisplayRepository.findByProductNo(1L)).thenReturn(Optional.empty());

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
    @DisplayName("대표 노출 상품을 비활성 상태로 수정하면 featured 순번을 해제하고 이력 요약에 반영한다")
    void updateProductInfoDemotesFeaturedDisplayWhenStatusBecomesInactive() {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setProductNo(1L);
        request.setBrandNo(2L);
        request.setCategoryNo(3L);
        request.setNameKo("수정 상품");
        request.setModelNum("M992GR");
        request.setReleasePrice(259000);
        request.setStatus(ProductStatus.HIDDEN.name());

        FrontProductDisplay currentDisplay = FrontProductDisplay.builder()
                .productNo(1L)
                .headline("Grey precision")
                .description("전시 설명")
                .mood("Sharp tone")
                .featuredYn("Y")
                .featuredRank(2)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(Product.builder()
                .id(1L)
                .brandNo(2L)
                .categoryNo(3L)
                .nameKo("수정 상품")
                .modelNum("M992GR")
                .status(ProductStatus.ACTIVE.name())
                .releasePrice(259000)
                .build()));
        when(productOptionRepository.findByProductId(1L)).thenReturn(List.of());
        when(frontProductDisplayRepository.findByProductNo(1L)).thenReturn(Optional.of(currentDisplay));
        when(frontProductDisplayRepository.save(any(FrontProductDisplay.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(brandRepository.findById(2L)).thenReturn(Optional.of(
                Brand.builder().brandNo(2L).nameKo("뉴발란스").isActive("Y").build()
        ));
        when(categoryRepository.findById(3L)).thenReturn(
                Optional.of(Category.builder().categoryNo(3L).parentNo(1L).name("러닝화").depth(2).isActive("Y").build())
        );
        when(categoryRepository.findById(1L)).thenReturn(
                Optional.of(Category.builder().categoryNo(1L).name("신발").depth(1).isActive("Y").build())
        );
        when(categoryRepository.existsByParentNo(3L)).thenReturn(false);

        adminProductService.updateProductInfo(request);

        verify(frontProductDisplayRepository).save(argThat(display ->
                display.getProductNo().equals(1L)
                        && display.getFeaturedYn().equals("N")
                        && display.getFeaturedRank().equals(999)
        ));
        verify(productChangeHistoryRepository).save(argThat(history ->
                history.getProductNo().equals(1L)
                        && history.getSummary().contains("프론트 대표노출")
                        && history.getStatusSnapshot().equals(ProductStatus.HIDDEN.name())
        ));
    }

    @Test
    @DisplayName("상품 프론트 노출 정보 조회는 저장된 메타데이터를 반환한다")
    void getFrontDisplayReturnsSavedDisplay() {
        when(productRepository.findById(4L)).thenReturn(Optional.of(Product.builder()
                .id(4L)
                .brandNo(1L)
                .categoryNo(2L)
                .nameKo("상품")
                .status("ACTIVE")
                .build()));
        when(frontProductDisplayRepository.findByProductNo(4L)).thenReturn(Optional.of(
                FrontProductDisplay.builder()
                        .productNo(4L)
                        .headline("Grey precision")
                        .description("설명")
                        .mood("Mood")
                        .featuredYn("Y")
                        .featuredRank(2)
                        .build()
        ));

        ProductFrontDisplayResponse response = adminProductService.getFrontDisplay(4L);

        assertEquals(4L, response.productNo());
        assertEquals("Grey precision", response.headline());
        assertEquals(true, response.featured());
        assertEquals(2, response.featuredRank());
    }

    @Test
    @DisplayName("프론트 Featured 순번 가이드는 현재 상품을 제외한 사용중 순번과 추천 순번을 반환한다")
    void getFrontDisplayRankGuideReturnsOccupiedAndRecommendedRanks() {
        when(productRepository.findById(4L)).thenReturn(Optional.of(Product.builder()
                .id(4L)
                .brandNo(1L)
                .categoryNo(2L)
                .nameKo("상품")
                .status("ACTIVE")
                .build()));
        when(frontProductDisplayRepository.findActiveFeaturedRanks(ProductStatus.ACTIVE.name(), 4L))
                .thenReturn(List.of(1, 2, 4, 7));

        var response = adminProductService.getFrontDisplayRankGuide(4L);

        assertEquals(12, response.guideLimit());
        assertEquals(3, response.recommendedRank());
        assertIterableEquals(List.of(1, 2, 4, 7), response.occupiedRanks());
        assertIterableEquals(List.of(3, 5, 6, 8, 9, 10, 11, 12), response.availableRanks());
    }

    @Test
    @DisplayName("상품 프론트 노출 정보 저장은 신규 메타데이터를 생성하고 이력을 남긴다")
    void saveFrontDisplayCreatesMetadataAndHistory() {
        when(productRepository.findById(4L)).thenReturn(Optional.of(Product.builder()
                .id(4L)
                .brandNo(1L)
                .categoryNo(2L)
                .nameKo("상품")
                .status("ACTIVE")
                .build()));
        when(frontProductDisplayRepository.findByProductNo(4L)).thenReturn(Optional.empty());
        when(frontProductDisplayRepository.save(any(FrontProductDisplay.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductFrontDisplayResponse response = adminProductService.saveFrontDisplay(new ProductFrontDisplaySaveRequest(
                4L,
                " Grey precision ",
                " 전시 설명 ",
                " Sharp tone ",
                true,
                3
        ));

        assertEquals("Grey precision", response.headline());
        assertEquals("전시 설명", response.description());
        assertEquals("Sharp tone", response.mood());
        assertEquals(true, response.featured());
        assertEquals(3, response.featuredRank());
        verify(productChangeHistoryRepository).save(argThat(history ->
                history.getProductNo().equals(4L)
                        && history.getSummary().contains("headline=Grey precision")
                        && history.getSummary().contains("featured=Y")
                        && history.getSummary().contains("rank=3")
        ));
    }

    @Test
    @DisplayName("상품 프론트 노출 정보 저장은 일반 노출일 때 순서를 999로 고정한다")
    void saveFrontDisplayNormalizesRankWhenNotFeatured() {
        when(productRepository.findById(8L)).thenReturn(Optional.of(Product.builder()
                .id(8L)
                .brandNo(1L)
                .categoryNo(2L)
                .nameKo("상품")
                .status("ACTIVE")
                .build()));
        when(frontProductDisplayRepository.findByProductNo(8L)).thenReturn(Optional.empty());
        when(frontProductDisplayRepository.save(any(FrontProductDisplay.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductFrontDisplayResponse response = adminProductService.saveFrontDisplay(new ProductFrontDisplaySaveRequest(
                8L,
                "Quiet release",
                "일반 노출 상품",
                "Soft tone",
                false,
                7
        ));

        assertEquals(false, response.featured());
        assertEquals(999, response.featuredRank());
    }

    @Test
    @DisplayName("상품 프론트 노출 정보 초기화는 저장된 메타데이터를 제거하고 이력을 남긴다")
    void clearFrontDisplayDeletesMetadataAndRecordsHistory() {
        FrontProductDisplay display = FrontProductDisplay.builder()
                .displayNo(21L)
                .productNo(8L)
                .headline("Quiet release")
                .description("일반 노출 상품")
                .mood("Soft tone")
                .featuredYn("N")
                .featuredRank(999)
                .build();
        when(productRepository.findById(8L)).thenReturn(Optional.of(Product.builder()
                .id(8L)
                .brandNo(1L)
                .categoryNo(2L)
                .nameKo("상품")
                .status(ProductStatus.ACTIVE.name())
                .build()));
        when(frontProductDisplayRepository.findByProductNo(8L)).thenReturn(Optional.of(display));

        adminProductService.clearFrontDisplay(8L);

        verify(frontProductDisplayRepository).delete(display);
        verify(productChangeHistoryRepository).save(argThat(history ->
                history.getProductNo().equals(8L)
                        && history.getSummary().equals("프론트 노출 정보가 초기화되었습니다.")
                        && history.getStatusSnapshot().equals(ProductStatus.ACTIVE.name())
        ));
    }

    @Test
    @DisplayName("대표 노출 상품은 활성 상태 상품에만 저장할 수 있다")
    void saveFrontDisplayThrowsWhenFeaturedProductNotActive() {
        when(productRepository.findById(8L)).thenReturn(Optional.of(Product.builder()
                .id(8L)
                .brandNo(1L)
                .categoryNo(2L)
                .nameKo("상품")
                .status(ProductStatus.HIDDEN.name())
                .build()));

        BusinessException exception = assertThrows(BusinessException.class, () -> adminProductService.saveFrontDisplay(
                new ProductFrontDisplaySaveRequest(8L, "Quiet release", "일반 노출 상품", "Soft tone", true, 2)
        ));

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("대표 노출 순서는 다른 상품과 중복될 수 없다")
    void saveFrontDisplayThrowsWhenFeaturedRankAlreadyUsed() {
        when(productRepository.findById(8L)).thenReturn(Optional.of(Product.builder()
                .id(8L)
                .brandNo(1L)
                .categoryNo(2L)
                .nameKo("상품")
                .status(ProductStatus.ACTIVE.name())
                .build()));
        when(frontProductDisplayRepository.existsFeaturedRankConflict("Y", 2, 8L, ProductStatus.ACTIVE.name()))
                .thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> adminProductService.saveFrontDisplay(
                new ProductFrontDisplaySaveRequest(8L, "Quiet release", "일반 노출 상품", "Soft tone", true, 2)
        ));

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("대표 노출 상품은 순서를 반드시 함께 입력해야 한다")
    void saveFrontDisplayThrowsWhenFeaturedRankMissing() {
        when(productRepository.findById(8L)).thenReturn(Optional.of(Product.builder()
                .id(8L)
                .brandNo(1L)
                .categoryNo(2L)
                .nameKo("상품")
                .status(ProductStatus.ACTIVE.name())
                .build()));

        BusinessException exception = assertThrows(BusinessException.class, () -> adminProductService.saveFrontDisplay(
                new ProductFrontDisplaySaveRequest(8L, "Quiet release", "일반 노출 상품", "Soft tone", true, null)
        ));

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 프론트 노출 목록은 Querydsl 조회 결과를 운영 응답으로 매핑한다")
    void getFrontDisplayProductsMapsAdminRows() {
        when(productRepository.getAdminFrontDisplayProducts(argThat(query ->
                query.featuredOnly()
                        && query.status() == ProductStatus.ACTIVE
                        && query.brandNo() == 7L
                        && query.categoryNo() == 11L
                        && Boolean.TRUE.equals(query.displayConfigured())
                        && query.lowStockOnly()
                        && query.lowStockThreshold() == 15
                        && "PRICE_LOW".equals(query.sort())
                        && "Grey".equals(query.keyword())
        ))).thenReturn(List.of(
                new AdminFrontDisplayProductRow(
                        4L,
                        "990v6 Grey Day",
                        "New Balance",
                        "러닝화",
                        289000,
                        18L,
                        "ACTIVE",
                        true,
                        "Grey precision",
                        "전시 설명",
                        "Sharp tone",
                        true,
                        3
                )
        ));

        ProductFrontDisplayDashboardResponse response = adminProductService.getFrontDisplayProducts(
                new ProductFrontDisplayListRequest(" Grey ", "ACTIVE", 7L, 11L, "CONFIGURED", true, true, 15, "price_low")
        );

        assertEquals(1, response.summary().totalCount());
        assertEquals(1, response.summary().configuredCount());
        assertEquals(0, response.summary().unconfiguredCount());
        assertEquals(1, response.summary().featuredCount());
        assertEquals(0, response.summary().lowStockCount());
        assertEquals(15L, response.summary().lowStockThreshold());
        assertEquals(4L, response.items().getFirst().productNo());
        assertEquals("Grey precision", response.items().getFirst().headline());
        assertEquals(18L, response.items().getFirst().totalStock());
        assertEquals(true, response.items().getFirst().featured());
    }

    @Test
    @DisplayName("상품 프론트 노출 목록 조회는 잘못된 상태 필터면 INVALID_INPUT_VALUE 예외를 던진다")
    void getFrontDisplayProductsThrowsWhenStatusInvalid() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> adminProductService.getFrontDisplayProducts(
                        new ProductFrontDisplayListRequest(null, "not-a-status", null, null, null, false, false, null, null)
                ));

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 프론트 노출 CSV 내보내기는 동일한 Querydsl 조건으로 목록을 조회한다")
    void exportFrontDisplayProductsCsvUsesNormalizedQuery() {
        when(adminSettingsService.getLowStockDefaultThreshold()).thenReturn(20L);
        when(productRepository.getAdminFrontDisplayProducts(argThat(query ->
                !query.featuredOnly()
                        && query.status() == ProductStatus.HIDDEN
                        && query.brandNo() == 7L
                        && query.categoryNo() == 11L
                        && Boolean.FALSE.equals(query.displayConfigured())
                        && !query.lowStockOnly()
                        && query.lowStockThreshold() == 20
                        && "FEATURED".equals(query.sort())
                        && "draft".equals(query.keyword())
        ))).thenReturn(List.of(
                new AdminFrontDisplayProductRow(
                        4L,
                        "990v6 Grey Day",
                        "New Balance",
                        "러닝화",
                        289000,
                        18L,
                        "HIDDEN",
                        false,
                        null,
                        null,
                        null,
                        false,
                        999
                )
        ));
        when(brandRepository.findById(7L)).thenReturn(Optional.of(Brand.builder().brandNo(7L).nameKo("New Balance").build()));
        when(categoryRepository.findById(11L)).thenReturn(Optional.of(Category.builder().categoryNo(11L).name("러닝화").build()));

        byte[] result = adminProductService.exportFrontDisplayProductsCsv(
                new ProductFrontDisplayListRequest(" draft ", "hidden", 7L, 11L, "UNCONFIGURED", false, false, null, null)
        );

        String csv = new String(result, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csv.contains("브랜드: New Balance"));
        assertTrue(csv.contains("카테고리: 러닝화"));
        assertTrue(csv.contains("노출 설정: 미설정"));
        assertTrue(csv.contains("\"미설정\""));
        assertTrue(csv.contains("\"숨김\""));
    }

    @Test
    @DisplayName("상품 삭제 성공 시 삭제 이력을 저장한다")
    void deleteProductRecordsHistory() {
        FrontProductDisplay display = FrontProductDisplay.builder()
                .productNo(9L)
                .headline("Grey precision")
                .description("전시 설명")
                .mood("Sharp tone")
                .featuredYn("Y")
                .featuredRank(1)
                .build();
        when(productRepository.findById(9L)).thenReturn(Optional.of(Product.builder()
                .id(9L)
                .brandNo(1L)
                .categoryNo(1L)
                .nameKo("삭제 대상")
                .status("ACTIVE")
                .releasePrice(1000)
                .build()));
        when(frontProductDisplayRepository.findByProductNo(9L)).thenReturn(Optional.of(display));

        adminProductService.deleteProduct(9L);

        verify(frontProductDisplayRepository).delete(display);
        verify(productChangeHistoryRepository).save(argThat(history ->
                history.getProductNo().equals(9L)
                        && history.getSummary().equals("상품이 삭제 처리되었습니다.")
                        && history.getStatusSnapshot().equals("DELETE")
        ));
    }

    @Test
    @DisplayName("상품 일괄 상태 변경은 삭제되지 않은 상품만 반영하고 결과를 집계한다")
    void bulkOperateProductsUpdatesEligibleProductsOnly() {
        FrontProductDisplay featuredDisplay = FrontProductDisplay.builder()
                .productNo(1L)
                .headline("Grey precision")
                .description("전시 설명")
                .mood("Sharp tone")
                .featuredYn("Y")
                .featuredRank(2)
                .build();
        when(productRepository.findAllById(List.of(1L, 2L, 3L, 99L))).thenReturn(List.of(
                Product.builder().id(1L).status(ProductStatus.ACTIVE.name()).nameKo("A").brandNo(1L).categoryNo(1L).build(),
                Product.builder().id(2L).status(ProductStatus.HIDDEN.name()).nameKo("B").brandNo(1L).categoryNo(1L).build(),
                Product.builder().id(3L).status(ProductStatus.DELETE.name()).nameKo("C").brandNo(1L).categoryNo(1L).build()
        ));
        when(frontProductDisplayRepository.findByProductNo(1L)).thenReturn(Optional.of(featuredDisplay));
        when(frontProductDisplayRepository.save(any(FrontProductDisplay.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminProductService.BulkOperateResult result = adminProductService.bulkOperateProducts(
                new ProductBulkOperateRequest(List.of(1L, 2L, 3L, 99L), "HIDDEN")
        );

        assertEquals(4, result.requestedCount());
        assertEquals(1, result.updatedCount());
        assertEquals(1, result.unchangedCount());
        assertEquals(1, result.blockedCount());
        assertEquals(1, result.missingCount());
        verify(frontProductDisplayRepository).save(argThat(display ->
                display.getProductNo().equals(1L)
                        && display.getFeaturedYn().equals("N")
                        && display.getFeaturedRank().equals(999)
        ));
        verify(productChangeHistoryRepository).save(argThat(history ->
                history.getProductNo().equals(1L)
                        && history.getSummary().contains("일괄 변경")
                        && history.getStatusSnapshot().equals(ProductStatus.HIDDEN.name())
        ));
    }

    @Test
    @DisplayName("상품 일괄 삭제는 이미 삭제된 상품을 제외하고 논리 삭제한다")
    void bulkDeleteProductsDeletesEligibleProductsOnly() {
        FrontProductDisplay display = FrontProductDisplay.builder()
                .productNo(1L)
                .headline("Grey precision")
                .description("전시 설명")
                .mood("Sharp tone")
                .featuredYn("Y")
                .featuredRank(1)
                .build();
        when(productRepository.findAllById(List.of(1L, 2L, 3L))).thenReturn(List.of(
                Product.builder().id(1L).status(ProductStatus.ACTIVE.name()).nameKo("A").brandNo(1L).categoryNo(1L).build(),
                Product.builder().id(2L).status(ProductStatus.DELETE.name()).nameKo("B").brandNo(1L).categoryNo(1L).build()
        ));
        when(frontProductDisplayRepository.findByProductNo(1L)).thenReturn(Optional.of(display));

        AdminProductService.BulkDeleteResult result = adminProductService.bulkDeleteProducts(
                new ProductBulkDeleteRequest(List.of(1L, 2L, 3L))
        );

        assertEquals(3, result.requestedCount());
        assertEquals(1, result.deletedCount());
        assertEquals(1, result.alreadyDeletedCount());
        assertEquals(1, result.missingCount());
        verify(frontProductDisplayRepository).delete(display);
        verify(productChangeHistoryRepository).save(argThat(history ->
                history.getProductNo().equals(1L)
                        && history.getSummary().contains("일괄 삭제")
                        && history.getStatusSnapshot().equals(ProductStatus.DELETE.name())
        ));
    }

    @Test
    @DisplayName("상품 일괄 복제는 삭제되지 않은 상품만 숨김 상태로 복제한다")
    void bulkDuplicateProductsCreatesEligibleProductsOnly() {
        Product sourceActive = Product.builder()
                .id(1L)
                .status(ProductStatus.ACTIVE.name())
                .nameKo("A")
                .brandNo(1L)
                .categoryNo(1L)
                .releasePrice(1000)
                .build();
        Product sourceDeleted = Product.builder()
                .id(2L)
                .status(ProductStatus.DELETE.name())
                .nameKo("B")
                .brandNo(1L)
                .categoryNo(1L)
                .releasePrice(2000)
                .build();

        when(productRepository.findAllById(List.of(1L, 2L, 3L))).thenReturn(List.of(sourceActive, sourceDeleted));
        when(productRepository.save(any(Product.class))).thenReturn(Product.builder()
                .id(11L)
                .status(ProductStatus.HIDDEN.name())
                .nameKo("A (복제)")
                .brandNo(1L)
                .categoryNo(1L)
                .releasePrice(1000)
                .build());
        when(productOptionRepository.findByProductId(1L)).thenReturn(List.of(
                ProductOption.builder().productNo(1L).optionName("260").stockCnt(2).additionalPrice(0).build()
        ));

        AdminProductService.BulkDuplicateResult result = adminProductService.bulkDuplicateProducts(
                new ProductBulkDuplicateRequest(List.of(1L, 2L, 3L))
        );

        assertEquals(3, result.requestedCount());
        assertEquals(1, result.createdCount());
        assertEquals(1, result.blockedCount());
        assertEquals(1, result.missingCount());
        assertIterableEquals(List.of(11L), result.createdProductNos());
        verify(productChangeHistoryRepository).save(argThat(history ->
                history.getProductNo().equals(11L)
                        && history.getSummary().contains("원본 상품 번호: 1")
                        && history.getStatusSnapshot().equals(ProductStatus.HIDDEN.name())
        ));
    }

    @Test
    @DisplayName("상품 일괄 복제는 조회 반환 순서와 무관하게 요청 순서대로 생성 결과를 반환한다")
    void bulkDuplicateProductsPreservesRequestedOrder() {
        Product requestedThird = Product.builder()
                .id(3L)
                .status(ProductStatus.ACTIVE.name())
                .nameKo("세 번째")
                .brandNo(1L)
                .categoryNo(1L)
                .releasePrice(3000)
                .build();
        Product requestedFirst = Product.builder()
                .id(1L)
                .status(ProductStatus.ACTIVE.name())
                .nameKo("첫 번째")
                .brandNo(1L)
                .categoryNo(1L)
                .releasePrice(1000)
                .build();

        when(productRepository.findAllById(List.of(3L, 1L, 9L))).thenReturn(List.of(requestedFirst, requestedThird));
        when(productRepository.save(any(Product.class)))
                .thenReturn(
                        Product.builder().id(31L).status(ProductStatus.HIDDEN.name()).nameKo("세 번째 (복제)").brandNo(1L).categoryNo(1L).releasePrice(3000).build(),
                        Product.builder().id(11L).status(ProductStatus.HIDDEN.name()).nameKo("첫 번째 (복제)").brandNo(1L).categoryNo(1L).releasePrice(1000).build()
                );
        when(productOptionRepository.findByProductId(3L)).thenReturn(List.of());
        when(productOptionRepository.findByProductId(1L)).thenReturn(List.of());

        AdminProductService.BulkDuplicateResult result = adminProductService.bulkDuplicateProducts(
                new ProductBulkDuplicateRequest(List.of(3L, 1L, 9L))
        );

        assertEquals(3, result.requestedCount());
        assertEquals(2, result.createdCount());
        assertEquals(0, result.blockedCount());
        assertEquals(1, result.missingCount());
        assertIterableEquals(List.of(31L, 11L), result.createdProductNos());
        verify(productRepository, times(2)).save(any(Product.class));
    }

    @Test
    @DisplayName("상품 일괄 상태 변경은 DELETE 상태를 허용하지 않는다")
    void bulkOperateProductsRejectsDeleteStatus() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                adminProductService.bulkOperateProducts(new ProductBulkOperateRequest(List.of(1L), "DELETE"))
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 이력 조회는 작업자 이름을 함께 내려준다")
    void getProductHistoryIncludesActorName() {
        ProductChangeHistory history = ProductChangeHistory.of(
                4L,
                ProductHistoryActionType.CREATED,
                "상품이 기존 상품에서 복제되었습니다. 원본 상품 번호: 2",
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

        List<ProductHistoryResponse> histories = adminProductService.getProductHistory(4L);

        assertEquals(1, histories.size());
        assertEquals("관리자", histories.get(0).actorName());
        assertEquals(2L, histories.get(0).relatedProductNo());
        assertEquals("원본 상품", histories.get(0).relatedProductLabel());
        assertEquals("/admin/logs?actionType=PRODUCT_CREATE&targetId=4", histories.get(0).activityLogPath());
    }

    @Test
    @DisplayName("상품 변경 이력 목록은 필터 결과와 작업자명을 함께 반환한다")
    void getProductHistoryListReturnsPagedHistoryWithActorName() {
        ProductHistoryListRequest request = new ProductHistoryListRequest();
        request.setProductNo(4L);
        request.setActionType("UPDATED");
        request.setActorKeyword("관리자");
        request.setOrderType("oldest");

        ProductHistoryListResDto row = new ProductHistoryListResDto();
        row.setHistoryNo(7L);
        row.setProductNo(4L);
        row.setActionType("CREATED");
        row.setSummary("상품이 기존 상품에서 복제되었습니다. 원본 상품 번호: 5");
        row.setStatusSnapshot("ACTIVE");
        row.setOptionCount(2);
        row.setTotalStock(8L);
        row.setActorNo(1L);
        row.setActorName("관리자");
        row.setActionDtm(java.time.LocalDateTime.of(2026, 5, 11, 10, 0));

        when(productChangeHistoryRepository.getProductHistoryList(any(), any()))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1));

        ProductHistoryListResponse response = adminProductService.getProductHistoryList(request, PageRequest.of(0, 20));

        assertEquals(1, response.items().size());
        assertEquals("관리자", response.items().get(0).actorName());
        assertEquals(5L, response.items().get(0).relatedProductNo());
        assertEquals("원본 상품", response.items().get(0).relatedProductLabel());
        assertEquals("/admin/logs?actionType=PRODUCT_CREATE&targetId=4", response.items().get(0).activityLogPath());
        assertEquals("UPDATED", response.appliedQuery().actionType());
        assertEquals("관리자", response.appliedQuery().actorKeyword());
        assertEquals("oldest", response.appliedQuery().orderType());
        assertEquals("오래된순", response.appliedQuery().orderTypeLabel());
        assertEquals(1L, response.totalElements());
        assertEquals(0, response.currentPage());
        assertEquals("1-1 / 1건 · 1페이지", response.pageInfoLabel());
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
        when(frontProductDisplayRepository.findByProductNo(5L)).thenReturn(Optional.of(
                FrontProductDisplay.builder()
                        .productNo(5L)
                        .headline("Grey precision")
                        .description("전시 설명")
                        .mood("Sharp tone")
                        .featuredYn("Y")
                        .featuredRank(1)
                        .build()
        ));

        Long clonedProductNo = adminProductService.cloneProduct(5L);

        assertEquals(10L, clonedProductNo);
        verify(productOptionRepository).saveAll(any());
        verify(frontProductDisplayRepository).save(argThat(display ->
                display.getProductNo().equals(10L)
                        && display.getHeadline().equals("Grey precision")
                        && display.getDescription().equals("전시 설명")
                        && display.getMood().equals("Sharp tone")
                        && display.getFeaturedYn().equals("N")
                        && display.getFeaturedRank().equals(999)
        ));
        verify(productChangeHistoryRepository).save(argThat(history ->
                history.getProductNo().equals(10L)
                        && history.getSummary().contains("원본 상품 번호: 5")
                        && history.getSummary().contains("프론트 노출 초안도 함께 복제")
                        && history.getStatusSnapshot().equals("HIDDEN")
        ));
    }

    @Test
    @DisplayName("삭제된 상품은 복제할 수 없다")
    void cloneProductThrowsWhenSourceProductDeleted() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(Product.builder()
                .id(5L)
                .categoryNo(2L)
                .brandNo(3L)
                .nameKo("삭제 상품")
                .status(ProductStatus.DELETE.name())
                .releasePrice(1000)
                .build()));

        BusinessException exception = assertThrows(BusinessException.class, () -> adminProductService.cloneProduct(5L));

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, exception.getErrorCode());
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

        when(brandRepository.findById(1L)).thenReturn(Optional.of(
                Brand.builder().brandNo(1L).nameKo("나이키").isActive("Y").build()
        ));
        when(categoryRepository.findById(2L)).thenReturn(
                Optional.of(Category.builder().categoryNo(2L).parentNo(1L).name("러닝화").depth(2).isActive("Y").build())
        );
        when(categoryRepository.findById(1L)).thenReturn(
                Optional.of(Category.builder().categoryNo(1L).name("신발").depth(1).isActive("Y").build())
        );
        when(categoryRepository.existsByParentNo(2L)).thenReturn(false);

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
        when(brandRepository.findById(1L)).thenReturn(Optional.of(
                Brand.builder().brandNo(1L).nameKo("나이키").isActive("Y").build()
        ));
        when(categoryRepository.findById(2L)).thenReturn(
                Optional.of(Category.builder().categoryNo(2L).parentNo(1L).name("러닝화").depth(2).isActive("Y").build())
        );
        when(categoryRepository.findById(1L)).thenReturn(
                Optional.of(Category.builder().categoryNo(1L).name("신발").depth(1).isActive("Y").build())
        );
        when(categoryRepository.existsByParentNo(2L)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> adminProductService.updateProductInfo(request));

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("옵션 변경이 없으면 수정 이력 요약에 옵션을 포함하지 않는다")
    void updateProductInfoDoesNotRecordOptionChangeWhenOptionsUnchanged() {
        ProductUpdateRequest.ProductOptionUpdateRequest option = new ProductUpdateRequest.ProductOptionUpdateRequest();
        option.setOptionName(" 280 ");
        option.setStockCnt(3);
        option.setAdditionalPrice(5000);

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setProductNo(1L);
        request.setBrandNo(1L);
        request.setCategoryNo(2L);
        request.setNameKo("기존 상품");
        request.setModelNum("OLD");
        request.setReleasePrice(1000);
        request.setStatus("ACTIVE");
        request.setOptions(List.of(option));

        when(productRepository.findById(1L)).thenReturn(Optional.of(Product.builder()
                .id(1L)
                .brandNo(1L)
                .categoryNo(2L)
                .nameKo("기존 상품")
                .modelNum("OLD")
                .status("ACTIVE")
                .releasePrice(1000)
                .build()));
        when(productOptionRepository.findByProductId(1L)).thenReturn(List.of(
                ProductOption.builder().productNo(1L).optionName("280").stockCnt(3).additionalPrice(5000).build()
        ));
        when(brandRepository.findById(1L)).thenReturn(Optional.of(
                Brand.builder().brandNo(1L).nameKo("나이키").isActive("Y").build()
        ));
        when(categoryRepository.findById(2L)).thenReturn(
                Optional.of(Category.builder().categoryNo(2L).parentNo(1L).name("러닝화").depth(2).isActive("Y").build())
        );
        when(categoryRepository.findById(1L)).thenReturn(
                Optional.of(Category.builder().categoryNo(1L).name("신발").depth(1).isActive("Y").build())
        );
        when(categoryRepository.existsByParentNo(2L)).thenReturn(false);

        adminProductService.updateProductInfo(request);

        verify(productChangeHistoryRepository).save(argThat(history ->
                history.getProductNo().equals(1L)
                        && history.getSummary().equals("변경된 정보가 없습니다.")
        ));
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
    @DisplayName("상품 목록 통계 조회는 빠른 필터를 제외한 기준 typed query를 사용한다")
    void getProductListUsesBaseTypedQueryForStats() {
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

        assertEquals(true, listQueryCaptor.getValue().lowStockOnly());
        assertEquals(30L, listQueryCaptor.getValue().effectiveLowStockThreshold());
        assertEquals(true, listQueryCaptor.getValue().createdTodayOnly());
        assertEquals("젤 카야노", listQueryCaptor.getValue().searchKeyword());
        assertEquals(false, statsQueryCaptor.getValue().lowStockOnly());
        assertEquals(false, statsQueryCaptor.getValue().createdTodayOnly());
        assertEquals(null, statsQueryCaptor.getValue().status());
        assertEquals(30L, statsQueryCaptor.getValue().effectiveLowStockThreshold());
        assertEquals("젤 카야노", statsQueryCaptor.getValue().searchKeyword());
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
        assertEquals("기본 필터 기준", response.productStats().contextLabel());
        assertEquals("최신순", response.productStats().querySignature());
    }

    @Test
    @DisplayName("비활성 브랜드로는 상품을 등록할 수 없다")
    void createProductInfoRejectsInactiveBrand() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setBrandNo(1L);
        request.setCategoryNo(2L);
        request.setNameKo("테스트 상품");
        request.setReleasePrice(1000);

        when(brandRepository.findById(1L)).thenReturn(Optional.of(
                Brand.builder().brandNo(1L).nameKo("나이키").isActive("N").build()
        ));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(
                Category.builder().categoryNo(2L).parentNo(1L).name("러닝화").depth(2).isActive("Y").build()
        ));

        BusinessException exception = assertThrows(BusinessException.class, () -> adminProductService.createProductInfo(request));

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("상위 카테고리로는 상품을 수정할 수 없다")
    void updateProductInfoRejectsNonLeafCategory() {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setProductNo(1L);
        request.setBrandNo(1L);
        request.setCategoryNo(10L);
        request.setNameKo("테스트 상품");
        request.setReleasePrice(1000);
        request.setStatus("ACTIVE");

        when(productRepository.findById(1L)).thenReturn(Optional.of(Product.builder()
                .id(1L)
                .brandNo(1L)
                .categoryNo(2L)
                .nameKo("기존 상품")
                .status("ACTIVE")
                .releasePrice(1000)
                .build()));
        when(brandRepository.findById(1L)).thenReturn(Optional.of(
                Brand.builder().brandNo(1L).nameKo("나이키").isActive("Y").build()
        ));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(
                Category.builder().categoryNo(10L).parentNo(null).name("신발").depth(1).isActive("Y").build()
        ));

        BusinessException exception = assertThrows(BusinessException.class, () -> adminProductService.updateProductInfo(request));

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("비활성 부모 아래 카테고리로는 상품을 등록할 수 없다")
    void createProductInfoRejectsCategoryUnderInactiveParent() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setBrandNo(1L);
        request.setCategoryNo(2L);
        request.setNameKo("테스트 상품");
        request.setReleasePrice(1000);

        when(brandRepository.findById(1L)).thenReturn(Optional.of(
                Brand.builder().brandNo(1L).nameKo("나이키").isActive("Y").build()
        ));
        when(categoryRepository.findById(2L)).thenReturn(
                Optional.of(Category.builder().categoryNo(2L).parentNo(1L).name("러닝화").depth(2).isActive("Y").build())
        );
        when(categoryRepository.findById(1L)).thenReturn(
                Optional.of(Category.builder().categoryNo(1L).name("신발").depth(1).isActive("N").build())
        );
        when(categoryRepository.existsByParentNo(2L)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> adminProductService.createProductInfo(request));

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
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
        assertEquals("기본 필터 기준", response.productStats().contextLabel());
        assertEquals("발매가순 · 검색=뉴발란스 993", response.productStats().querySignature());
    }
}
