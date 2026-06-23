package com.section.admin.product.controller;

import com.section.admin.common.controller.AdminGlobalExceptionHandler;
import com.section.admin.product.req.ProductBulkDeleteRequest;
import com.section.admin.product.req.ProductBulkDuplicateRequest;
import com.section.admin.product.req.ProductBulkOperateRequest;
import com.section.admin.product.req.ProductCreateRequest;
import com.section.admin.product.req.ProductFrontDisplayListRequest;
import com.section.admin.product.req.ProductFrontDisplaySaveRequest;
import com.section.admin.product.res.ProductFrontDisplayDashboardResponse;
import com.section.admin.product.res.ProductFrontDisplayRankGuideResponse;
import com.section.admin.product.req.ProductUpdateRequest;
import com.section.admin.product.res.ProductDetailResponse;
import com.section.admin.product.res.ProductFrontDisplayResponse;
import com.section.admin.product.res.ProductFrontDisplayListResponse;
import com.section.admin.product.res.ProductFrontDisplaySummaryResponse;
import com.section.admin.product.res.ProductHistoryListResponse;
import com.section.admin.product.res.ProductHistoryResponse;
import com.section.admin.product.res.ProductListResponse;
import com.section.admin.product.service.AdminProductService;
import com.section.admin.settings.service.AdminOperationPolicyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.base.entity.type.ProductOrderType;
import com.section.common.base.entity.type.ProductStatus;
import com.section.common.commerce.dto.AdminFrontDisplayProductQuery;
import com.section.common.commerce.dto.ProductListQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminProductRestControllerTest {
    private static final DateTimeFormatter EXPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Mock
    private AdminProductService adminProductService;
    @Mock
    private AdminOperationPolicyService adminOperationPolicyService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    private String todayExportDate() {
        return LocalDate.now().format(EXPORT_DATE_FORMAT);
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminProductRestController(adminProductService, adminOperationPolicyService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new AdminGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("상품 생성 성공 시 생성된 상품 번호를 반환한다")
    void createProductReturnsCreatedProductNo() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setCategoryNo(2L);
        request.setBrandNo(1L);
        request.setNameKo("테스트 상품");
        request.setReleasePrice(1000);

        when(adminProductService.createProductInfo(org.mockito.ArgumentMatchers.any(ProductCreateRequest.class)))
                .thenReturn(33L);

        mockMvc.perform(post("/api/admin/product/set")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.productNo").value(33L));
    }

    @Test
    @DisplayName("상품 목록 API는 appliedQuery와 resultMeta를 함께 반환한다")
    void getProductListReturnsAppliedQueryAndResultMeta() throws Exception {
        ProductListResponse response = new ProductListResponse(
                List.of(),
                0,
                1,
                2L,
                new ProductListResponse.ProductStatsItem(2L, 1L, 1L, 0L, 30L, "기본 필터 기준", "재고순 · 검색=뉴발란스 993"),
                ProductListResponse.AppliedQueryItem.from(
                        new ProductListQuery(3L, 7L, null, "뉴발란스 993", ProductOrderType.STOCK_COUNT, true, 30L, false)
                ),
                new ProductListResponse.ResultMetaItem(
                        "검색 결과 2개",
                        "1-2 / 2개 · 1페이지",
                        "재고순",
                        10,
                        1L,
                        2L,
                        3L,
                        true,
                        "재고순 · 검색=뉴발란스 993 · 재고<30"
                )
        );

        when(adminProductService.getProductList(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/admin/product/list?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedQuery.brandNo").value(7L))
                .andExpect(jsonPath("$.appliedQuery.categoryNo").value(3L))
                .andExpect(jsonPath("$.appliedQuery.orderTypeCode").value("c"))
                .andExpect(jsonPath("$.productStats.lowStockThreshold").value(30L))
                .andExpect(jsonPath("$.productStats.contextLabel").value("기본 필터 기준"))
                .andExpect(jsonPath("$.productStats.querySignature").value("재고순 · 검색=뉴발란스 993"))
                .andExpect(jsonPath("$.resultMeta.resultLabel").value("검색 결과 2개"))
                .andExpect(jsonPath("$.resultMeta.pageSize").value(10))
                .andExpect(jsonPath("$.resultMeta.rangeStart").value(1L))
                .andExpect(jsonPath("$.resultMeta.rangeEnd").value(2L))
                .andExpect(jsonPath("$.resultMeta.pageInfoLabel").value("1-2 / 2개 · 1페이지"))
                .andExpect(jsonPath("$.resultMeta.querySignature").value("재고순 · 검색=뉴발란스 993 · 재고<30"));
    }

    @Test
    @DisplayName("상품 목록 조회 중 INVALID_INPUT_VALUE 예외는 400으로 변환된다")
    void getProductListReturnsBadRequestWhenServiceThrowsInvalidInput() throws Exception {
        doThrow(new BusinessException(ErrorCode.INVALID_INPUT_VALUE))
                .when(adminProductService)
                .getProductList(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        mockMvc.perform(get("/api/admin/product/list?page=0&size=10&lowStockOnly=true&lowStockThreshold=25"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."));
    }

    @Test
    @DisplayName("상품 CSV 다운로드는 첨부 헤더와 CSV 바디를 반환한다")
    void exportProductListReturnsCsvAttachment() throws Exception {
        byte[] body = "\"내보낸시각\",\"2026.05.06 10:00\"\r\n\"정렬\",\"최신순\"".getBytes();

        when(adminProductService.exportProductListCsv(org.mockito.ArgumentMatchers.any()))
                .thenReturn(body);

        mockMvc.perform(get("/api/admin/product/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=products_" + todayExportDate() + ".csv"))
                .andExpect(content().contentType("text/csv"))
                .andExpect(content().bytes(body));
    }

    @Test
    @DisplayName("상품 CSV 다운로드 중 INVALID_INPUT_VALUE 예외는 400으로 변환된다")
    void exportProductListReturnsBadRequestWhenServiceThrowsInvalidInput() throws Exception {
        doThrow(new BusinessException(ErrorCode.INVALID_INPUT_VALUE))
                .when(adminProductService)
                .exportProductListCsv(org.mockito.ArgumentMatchers.any());

        mockMvc.perform(get("/api/admin/product/export?lowStockOnly=true&lowStockThreshold=25"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."));
    }

    @Test
    @DisplayName("상품 상세 API는 화면 요약용 필드를 함께 반환한다")
    void getProductDetailReturnsSummaryFields() throws Exception {
        ProductDetailResponse response = new ProductDetailResponse(
                4L,
                2L,
                "러닝화",
                7L,
                "뉴발란스",
                "992",
                "M992GR",
                259000,
                null,
                "https://example.com/992.jpg",
                true,
                "ACTIVE",
                "판매중",
                "2026.05.01 10:00",
                "2026.05.06 11:30",
                2,
                8L,
                List.of()
        );

        when(adminProductService.getProductDetail(4L)).thenReturn(response);

        mockMvc.perform(get("/api/admin/product/get?no=4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productNo").value(4L))
                .andExpect(jsonPath("$.hasThumbnail").value(true))
                .andExpect(jsonPath("$.optionCount").value(2))
                .andExpect(jsonPath("$.totalStock").value(8L))
                .andExpect(jsonPath("$.statusCode").value("ACTIVE"))
                .andExpect(jsonPath("$.statusDesc").value("판매중"));
    }

    @Test
    @DisplayName("상품 상세 API는 썸네일과 옵션이 없어도 기본 요약 필드를 유지한다")
    void getProductDetailReturnsSafeSummaryFieldsWhenOptionalValuesMissing() throws Exception {
        ProductDetailResponse response = new ProductDetailResponse(
                9L,
                2L,
                "러닝화",
                7L,
                "뉴발란스",
                "1080",
                "M1080",
                199000,
                null,
                "   ",
                false,
                "ACTIVE",
                "판매중",
                "",
                "",
                0,
                0L,
                List.of()
        );

        when(adminProductService.getProductDetail(9L)).thenReturn(response);

        mockMvc.perform(get("/api/admin/product/get?no=9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasThumbnail").value(false))
                .andExpect(jsonPath("$.optionCount").value(0))
                .andExpect(jsonPath("$.totalStock").value(0L))
                .andExpect(jsonPath("$.options").isArray())
                .andExpect(jsonPath("$.options").isEmpty());
    }

    @Test
    @DisplayName("상품 프론트 노출 정보 조회 API는 전시 메타데이터를 반환한다")
    void getProductFrontDisplayReturnsDisplayMetadata() throws Exception {
        when(adminProductService.getFrontDisplay(4L))
                .thenReturn(new ProductFrontDisplayResponse(4L, "Grey precision", "설명", "Mood", true, 2));

        mockMvc.perform(get("/api/admin/product/front-display?productNo=4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productNo").value(4L))
                .andExpect(jsonPath("$.headline").value("Grey precision"))
                .andExpect(jsonPath("$.featured").value(true))
                .andExpect(jsonPath("$.featuredRank").value(2));
    }

    @Test
    @DisplayName("상품 프론트 노출 순번 가이드 API는 사용중 순번과 추천값을 반환한다")
    void getProductFrontDisplayRankGuideReturnsGuide() throws Exception {
        when(adminProductService.getFrontDisplayRankGuide(4L))
                .thenReturn(new ProductFrontDisplayRankGuideResponse(
                        12,
                        3,
                        List.of(1, 2, 4),
                        List.of(3, 5, 6, 7)
                ));

        mockMvc.perform(get("/api/admin/product/front-display/rank-guide?productNo=4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guideLimit").value(12))
                .andExpect(jsonPath("$.recommendedRank").value(3))
                .andExpect(jsonPath("$.occupiedRanks[1]").value(2))
                .andExpect(jsonPath("$.availableRanks[0]").value(3));
    }

    @Test
    @DisplayName("상품 프론트 노출 목록 API는 featured 필터 결과를 반환한다")
    void getProductFrontDisplayListReturnsDisplayRows() throws Exception {
        when(adminProductService.getFrontDisplayProducts(org.mockito.ArgumentMatchers.any(ProductFrontDisplayListRequest.class)))
                .thenReturn(ProductFrontDisplayDashboardResponse.of(
                        new AdminFrontDisplayProductQuery(
                                "Grey", ProductStatus.ACTIVE, 7L, 11L, true, "READY", true, false, 20L, "PRICE_LOW"
                        ),
                        new ProductFrontDisplaySummaryResponse(1, 1, 0, 1, 0, 1, 1, 20),
                        List.of(new ProductFrontDisplayListResponse(
                                4L,
                                "990v6 Grey Day",
                                "New Balance",
                                "러닝화",
                                289000,
                                18L,
                                "ACTIVE",
                                "판매중",
                                true,
                                true,
                                "Grey precision",
                                "전시 설명",
                                "Sharp tone",
                                true,
                                3
                        ))
                ));

        mockMvc.perform(get("/api/admin/product/front-display/list?featuredOnly=true&status=ACTIVE&keyword=Grey&brandNo=7&categoryNo=11&configured=CONFIGURED&contentStatus=READY&sort=PRICE_LOW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalCount").value(1))
                .andExpect(jsonPath("$.summary.configuredCount").value(1))
                .andExpect(jsonPath("$.summary.readyContentCount").value(1))
                .andExpect(jsonPath("$.appliedQuery.configured").value("CONFIGURED"))
                .andExpect(jsonPath("$.appliedQuery.contentStatus").value("READY"))
                .andExpect(jsonPath("$.resultMeta.resultLabel").value("검색 결과 1건"))
                .andExpect(jsonPath("$.items[0].productNo").value(4L))
                .andExpect(jsonPath("$.items[0].displayConfigured").value(true))
                .andExpect(jsonPath("$.items[0].contentReady").value(true))
                .andExpect(jsonPath("$.items[0].statusDescription").value("판매중"))
                .andExpect(jsonPath("$.items[0].headline").value("Grey precision"))
                .andExpect(jsonPath("$.items[0].featuredRank").value(3));

        verify(adminProductService).getFrontDisplayProducts(org.mockito.ArgumentMatchers.argThat(request ->
                "Grey".equals(request.normalizedKeyword())
                        && request.normalizedStatus() == ProductStatus.ACTIVE
                        && request.normalizedBrandNo() == 7L
                        && request.normalizedCategoryNo() == 11L
                        && Boolean.TRUE.equals(request.normalizedConfigured())
                        && "READY".equals(request.normalizedContentStatus())
                        && "PRICE_LOW".equals(request.normalizedSort())
        ));
    }

    @Test
    @DisplayName("상품 프론트 노출 CSV 내보내기 API는 조건 기반 파일명을 내려준다")
    void exportProductFrontDisplayListReturnsCsv() throws Exception {
        when(adminProductService.exportFrontDisplayProductsCsv(org.mockito.ArgumentMatchers.any(ProductFrontDisplayListRequest.class)))
                .thenReturn("csv".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/admin/product/front-display/export?status=ACTIVE&brandNo=7&categoryNo=3&configured=UNCONFIGURED&contentStatus=INCOMPLETE&featuredOnly=true&lowStockOnly=true&keyword=Grey"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=front_display_active_brand7_category3_unconfigured_incomplete_featured_lowstock_search_" + todayExportDate() + ".csv"))
                .andExpect(content().contentType("text/csv"));
    }

    @Test
    @DisplayName("상품 프론트 노출 정보 저장 API는 저장 결과를 반환한다")
    void saveProductFrontDisplayReturnsSavedDisplay() throws Exception {
        ProductFrontDisplaySaveRequest request = new ProductFrontDisplaySaveRequest(
                4L,
                "Grey precision",
                "전시 설명",
                "Sharp tone",
                true,
                3
        );
        when(adminProductService.saveFrontDisplay(org.mockito.ArgumentMatchers.any(ProductFrontDisplaySaveRequest.class)))
                .thenReturn(new ProductFrontDisplayResponse(4L, "Grey precision", "전시 설명", "Sharp tone", true, 3));

        mockMvc.perform(post("/api/admin/product/front-display")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productNo").value(4L))
                .andExpect(jsonPath("$.headline").value("Grey precision"))
                .andExpect(jsonPath("$.featuredRank").value(3));
    }

    @Test
    @DisplayName("상품 프론트 노출 정보 초기화 API는 삭제 요청을 수행한다")
    void clearProductFrontDisplayDelegatesDelete() throws Exception {
        mockMvc.perform(delete("/api/admin/product/front-display/4"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("상품 이력 API는 최신 변경 이력을 반환한다")
    void getProductHistoryReturnsHistoryItems() throws Exception {
        when(adminProductService.getProductHistory(4L)).thenReturn(List.of(
                new ProductHistoryResponse(11L, 2L, "원본 상품", "/admin/logs?actionType=PRODUCT_CREATE&targetId=4", "활동 로그 보기", "CREATED", "등록", "상품이 기존 상품에서 복제되었습니다. 원본 상품 번호: 2", "ACTIVE", 2, 8L, 1L, "관리자", "2026.05.11 09:30")
        ));

        mockMvc.perform(get("/api/admin/product/history?no=4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].historyNo").value(11L))
                .andExpect(jsonPath("$[0].relatedProductNo").value(2L))
                .andExpect(jsonPath("$[0].relatedProductLabel").value("원본 상품"))
                .andExpect(jsonPath("$[0].activityLogPath").value("/admin/logs?actionType=PRODUCT_CREATE&targetId=4"))
                .andExpect(jsonPath("$[0].actionType").value("CREATED"))
                .andExpect(jsonPath("$[0].actionLabel").value("등록"))
                .andExpect(jsonPath("$[0].summary").value("상품이 기존 상품에서 복제되었습니다. 원본 상품 번호: 2"))
                .andExpect(jsonPath("$[0].actorNo").value(1L))
                .andExpect(jsonPath("$[0].actorName").value("관리자"))
                .andExpect(jsonPath("$[0].optionCount").value(2))
                .andExpect(jsonPath("$[0].totalStock").value(8L));
    }

    @Test
    @DisplayName("상품 이력 목록 API는 필터 결과와 메타 정보를 함께 반환한다")
    void getProductHistoryListReturnsPagedResult() throws Exception {
        when(adminProductService.getProductHistoryList(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ProductHistoryListResponse(
                        List.of(new ProductHistoryListResponse.Item(
                                7L, 4L, 5L, "원본 상품", "/admin/logs?actionType=PRODUCT_CREATE&targetId=4", "활동 로그 보기", "CREATED", "등록", "상품이 기존 상품에서 복제되었습니다. 원본 상품 번호: 5", "ACTIVE", 2, 8L, 1L, "관리자", "2026-05-11 10:00"
                        )),
                        1L,
                        1,
                        0,
                        20,
                        1L,
                        1L,
                        "1-1 / 1건 · 1페이지",
                        new ProductHistoryListResponse.AppliedQuery(4L, "UPDATED", null, 1L, "관리자", null, null, "oldest", "오래된순"),
                        new ProductHistoryListResponse.ResultMeta("검색 결과 1건", "1-1 / 1건 · 1페이지", 5, "1-1 · 상품=4 · 작업=UPDATED · 작업자번호=1 · 작업자=관리자 · 정렬=오래된순")
                ));

        mockMvc.perform(get("/api/admin/product/history/list?productNo=4&actionType=UPDATED&actorNo=1&actorKeyword=%EA%B4%80%EB%A6%AC%EC%9E%90&orderType=oldest&page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].historyNo").value(7L))
                .andExpect(jsonPath("$.items[0].relatedProductNo").value(5L))
                .andExpect(jsonPath("$.items[0].relatedProductLabel").value("원본 상품"))
                .andExpect(jsonPath("$.items[0].activityLogPath").value("/admin/logs?actionType=PRODUCT_CREATE&targetId=4"))
                .andExpect(jsonPath("$.items[0].actorName").value("관리자"))
                .andExpect(jsonPath("$.totalElements").value(1L))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageInfoLabel").value("1-1 / 1건 · 1페이지"))
                .andExpect(jsonPath("$.appliedQuery.productNo").value(4L))
                .andExpect(jsonPath("$.appliedQuery.actionType").value("UPDATED"))
                .andExpect(jsonPath("$.appliedQuery.actorNo").value(1L))
                .andExpect(jsonPath("$.appliedQuery.actorKeyword").value("관리자"))
                .andExpect(jsonPath("$.appliedQuery.orderType").value("oldest"))
                .andExpect(jsonPath("$.resultMeta.resultLabel").value("검색 결과 1건"))
                .andExpect(jsonPath("$.resultMeta.filterCount").value(5))
                .andExpect(jsonPath("$.resultMeta.querySignature").value("1-1 · 상품=4 · 작업=UPDATED · 작업자번호=1 · 작업자=관리자 · 정렬=오래된순"));
    }

    @Test
    @DisplayName("상품 변경 이력 export API는 csv 바이트를 반환한다")
    void exportProductHistoryListReturnsCsv() throws Exception {
        when(adminProductService.exportProductHistoryListCsv(org.mockito.ArgumentMatchers.any()))
                .thenReturn("history".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/admin/product/history/export?productNo=4&actorNo=1"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("product-history-")))
                .andExpect(content().contentType("text/csv"));
    }

    @Test
    @DisplayName("상품 복제 API는 새 상품 번호를 반환한다")
    void cloneProductReturnsCreatedProductNo() throws Exception {
        when(adminProductService.cloneProduct(5L)).thenReturn(10L);

        mockMvc.perform(post("/api/admin/product/clone/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.productNo").value(10L));
    }

    @Test
    @DisplayName("유지보수 모드에서는 상품 생성이 차단된다")
    void createProductReturnsServiceUnavailableWhenMaintenanceModeEnabled() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setCategoryNo(2L);
        request.setBrandNo(1L);
        request.setNameKo("테스트 상품");
        request.setReleasePrice(1000);

        doThrow(new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE))
                .when(adminOperationPolicyService)
                .assertAdminWriteAllowed();

        mockMvc.perform(post("/api/admin/product/set")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("A001"))
                .andExpect(jsonPath("$.message").value("현재 관리자 유지보수 모드입니다."));
    }

    @Test
    @DisplayName("유지보수 모드에서는 상품 수정이 차단된다")
    void updateProductReturnsServiceUnavailableWhenMaintenanceModeEnabled() throws Exception {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setProductNo(4L);
        request.setCategoryNo(2L);
        request.setBrandNo(1L);
        request.setNameKo("수정 상품");
        request.setReleasePrice(1000);

        doThrow(new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE))
                .when(adminOperationPolicyService)
                .assertAdminWriteAllowed();

        mockMvc.perform(post("/api/admin/product/update")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("A001"))
                .andExpect(jsonPath("$.message").value("현재 관리자 유지보수 모드입니다."));
    }

    @Test
    @DisplayName("유지보수 모드에서는 상품 삭제가 차단된다")
    void deleteProductReturnsServiceUnavailableWhenMaintenanceModeEnabled() throws Exception {
        doThrow(new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE))
                .when(adminOperationPolicyService)
                .assertAdminWriteAllowed();

        mockMvc.perform(patch("/api/admin/product/delete/5"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("A001"))
                .andExpect(jsonPath("$.message").value("현재 관리자 유지보수 모드입니다."));
    }

    @Test
    @DisplayName("유지보수 모드에서는 상품 일괄 상태 변경이 차단된다")
    void bulkOperateProductReturnsServiceUnavailableWhenMaintenanceModeEnabled() throws Exception {
        doThrow(new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE))
                .when(adminOperationPolicyService)
                .assertAdminWriteAllowed();

        mockMvc.perform(patch("/api/admin/product/bulk-operate")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ProductBulkOperateRequest(List.of(1L, 2L), "ACTIVE"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("A001"))
                .andExpect(jsonPath("$.message").value("현재 관리자 유지보수 모드입니다."));
    }

    @Test
    @DisplayName("상품 일괄 상태 변경 API는 집계 결과를 반환한다")
    void bulkOperateProductReturnsSummary() throws Exception {
        when(adminProductService.bulkOperateProducts(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AdminProductService.BulkOperateResult(4, 2, 1, 0, 1));

        mockMvc.perform(patch("/api/admin/product/bulk-operate")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ProductBulkOperateRequest(List.of(1L, 2L, 3L, 4L), "HIDDEN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(4))
                .andExpect(jsonPath("$.updatedCount").value(2))
                .andExpect(jsonPath("$.unchangedCount").value(1))
                .andExpect(jsonPath("$.blockedCount").value(0))
                .andExpect(jsonPath("$.missingCount").value(1));
    }

    @Test
    @DisplayName("상품 일괄 삭제 API는 집계 결과를 반환한다")
    void bulkDeleteProductReturnsSummary() throws Exception {
        when(adminProductService.bulkDeleteProducts(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AdminProductService.BulkDeleteResult(3, 2, 0, 1));

        mockMvc.perform(post("/api/admin/product/bulk-delete")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ProductBulkDeleteRequest(List.of(1L, 2L, 3L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(3))
                .andExpect(jsonPath("$.deletedCount").value(2))
                .andExpect(jsonPath("$.alreadyDeletedCount").value(0))
                .andExpect(jsonPath("$.missingCount").value(1));
    }

    @Test
    @DisplayName("유지보수 모드에서는 상품 일괄 삭제가 차단된다")
    void bulkDeleteProductReturnsServiceUnavailableWhenMaintenanceModeEnabled() throws Exception {
        doThrow(new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE))
                .when(adminOperationPolicyService)
                .assertAdminWriteAllowed();

        mockMvc.perform(post("/api/admin/product/bulk-delete")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ProductBulkDeleteRequest(List.of(1L, 2L)))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("A001"))
                .andExpect(jsonPath("$.message").value("현재 관리자 유지보수 모드입니다."));
    }

    @Test
    @DisplayName("상품 일괄 복제 API는 생성 결과를 반환한다")
    void bulkDuplicateProductReturnsSummary() throws Exception {
        when(adminProductService.bulkDuplicateProducts(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AdminProductService.BulkDuplicateResult(3, 2, 1, 0, List.of(10L, 11L)));

        mockMvc.perform(post("/api/admin/product/bulk-duplicate")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ProductBulkDuplicateRequest(List.of(1L, 2L, 3L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(3))
                .andExpect(jsonPath("$.createdCount").value(2))
                .andExpect(jsonPath("$.blockedCount").value(1))
                .andExpect(jsonPath("$.missingCount").value(0))
                .andExpect(jsonPath("$.createdProductNos[0]").value(10L))
                .andExpect(jsonPath("$.createdProductNos[1]").value(11L));
    }

    @Test
    @DisplayName("유지보수 모드에서는 상품 복제가 차단된다")
    void cloneProductReturnsServiceUnavailableWhenMaintenanceModeEnabled() throws Exception {
        doThrow(new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE))
                .when(adminOperationPolicyService)
                .assertAdminWriteAllowed();

        mockMvc.perform(post("/api/admin/product/clone/5"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("A001"))
                .andExpect(jsonPath("$.message").value("현재 관리자 유지보수 모드입니다."));
    }

    @Test
    @DisplayName("유지보수 모드에서는 상품 일괄 복제가 차단된다")
    void bulkDuplicateProductReturnsServiceUnavailableWhenMaintenanceModeEnabled() throws Exception {
        doThrow(new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE))
                .when(adminOperationPolicyService)
                .assertAdminWriteAllowed();

        mockMvc.perform(post("/api/admin/product/bulk-duplicate")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ProductBulkDuplicateRequest(List.of(1L, 2L)))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("A001"))
                .andExpect(jsonPath("$.message").value("현재 관리자 유지보수 모드입니다."));
    }

    @Test
    @DisplayName("잘못된 상품 생성 요청은 400 INVALID_INPUT_VALUE를 반환한다")
    void createProductReturnsBadRequestWhenPayloadInvalid() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setCategoryNo(2L);
        request.setBrandNo(1L);
        request.setNameKo(" ");
        request.setReleasePrice(1000);

        mockMvc.perform(post("/api/admin/product/set")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."));
    }

    @Test
    @DisplayName("잘못된 상품 옵션 생성 요청은 400 INVALID_INPUT_VALUE를 반환한다")
    void createProductReturnsBadRequestWhenOptionPayloadInvalid() throws Exception {
        ProductCreateRequest.ProductOptionRequest optionRequest = new ProductCreateRequest.ProductOptionRequest();
        optionRequest.setOptionName("  ");
        optionRequest.setStockCnt(-1);
        optionRequest.setAdditionalPrice(0);

        ProductCreateRequest request = new ProductCreateRequest();
        request.setCategoryNo(2L);
        request.setBrandNo(1L);
        request.setNameKo("테스트 상품");
        request.setReleasePrice(1000);
        request.setOptions(List.of(optionRequest));

        mockMvc.perform(post("/api/admin/product/set")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."));
    }

    @Test
    @DisplayName("상품 목록 CSV 내보내기는 attachment 헤더로 응답한다")
    void exportProductListReturnsAttachmentResponse() throws Exception {
        when(adminProductService.exportProductListCsv(org.mockito.ArgumentMatchers.any()))
                .thenReturn("csv".getBytes());

        mockMvc.perform(get("/api/admin/product/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=products_" + todayExportDate() + ".csv"));
    }

    @Test
    @DisplayName("상품 목록 CSV 파일명은 현재 필터 조건을 반영한다")
    void exportProductListUsesDynamicFilename() throws Exception {
        when(adminProductService.exportProductListCsv(org.mockito.ArgumentMatchers.any()))
                .thenReturn("csv".getBytes());

                mockMvc.perform(get("/api/admin/product/export")
                        .param("status", "ACTIVE")
                        .param("lowStockOnly", "true")
                        .param("createdTodayOnly", "true")
                        .param("brandNo", "7")
                        .param("categoryNo", "3")
                        .param("searchKeyword", "뉴발란스"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=products_active_lowstock_today_brand7_category3_search_" + todayExportDate() + ".csv"));
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

    @Test
    @DisplayName("잘못된 상품 수정 요청은 400 INVALID_INPUT_VALUE를 반환한다")
    void updateProductReturnsBadRequestWhenPayloadInvalid() throws Exception {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setProductNo(1L);
        request.setCategoryNo(2L);
        request.setBrandNo(1L);
        request.setNameKo(" ");
        request.setReleasePrice(1000);

        mockMvc.perform(post("/api/admin/product/update")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."));
    }

    @Test
    @DisplayName("잘못된 상품 옵션 수정 요청은 400 INVALID_INPUT_VALUE를 반환한다")
    void updateProductReturnsBadRequestWhenOptionPayloadInvalid() throws Exception {
        ProductUpdateRequest.ProductOptionUpdateRequest optionRequest = new ProductUpdateRequest.ProductOptionUpdateRequest();
        optionRequest.setOptionName("옵션명".repeat(30));
        optionRequest.setStockCnt(1);
        optionRequest.setAdditionalPrice(-1);

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setProductNo(1L);
        request.setCategoryNo(2L);
        request.setBrandNo(1L);
        request.setNameKo("테스트 상품");
        request.setReleasePrice(1000);
        request.setOptions(List.of(optionRequest));

        mockMvc.perform(post("/api/admin/product/update")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."));
    }
}
