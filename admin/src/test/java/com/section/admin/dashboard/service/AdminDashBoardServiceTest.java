package com.section.admin.dashboard.service;

import com.section.admin.content.res.ContentReactionAnalyticsResponse;
import com.section.admin.content.res.ContentReactionDataQualityResponse;
import com.section.admin.content.service.AdminContentReactionAnalyticsService;
import com.section.admin.dashboard.res.DashboardResponse;
import com.section.admin.product.req.ProductFrontDisplayListRequest;
import com.section.admin.product.res.ProductFrontDisplayDashboardResponse;
import com.section.admin.product.res.ProductFrontDisplayListResponse;
import com.section.admin.product.res.ProductFrontDisplaySummaryResponse;
import com.section.admin.product.service.AdminProductService;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.OrderRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.common.system.entity.AdminOperationNotice;
import com.section.common.system.entity.AdminOperationTask;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.dto.AdminOperationTaskCommentSummaryDto;
import com.section.common.system.dto.AdminOperationTaskWorkloadListQuery;
import com.section.common.system.dto.AdminOperationTaskWorkloadDto;
import com.section.common.system.dto.AdminOperationTaskWorkloadSummaryDto;
import com.section.common.system.repository.AdminOperationTaskCommentRepository;
import com.section.common.system.repository.AdminOperationNoticeRepository;
import com.section.common.system.repository.AdminOperationTaskRepository;
import com.section.common.system.repository.AdminUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashBoardServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private AdminOperationNoticeRepository adminOperationNoticeRepository;
    @Mock
    private AdminOperationTaskRepository adminOperationTaskRepository;
    @Mock
    private AdminOperationTaskCommentRepository adminOperationTaskCommentRepository;
    @Mock
    private AdminUserRepository adminUserRepository;
    @Mock
    private AdminProductService adminProductService;
    @Mock
    private AdminContentReactionAnalyticsService contentReactionAnalyticsService;

    @InjectMocks
    private AdminDashBoardService adminDashBoardService;

    @BeforeEach
    void setUpReactionSnapshot() {
        when(contentReactionAnalyticsService.getAnalytics(null, 7))
                .thenReturn(reactionAnalytics());
        when(contentReactionAnalyticsService.getDataQuality())
                .thenReturn(new ContentReactionDataQualityResponse(
                        4, 4, 0, "2026-07-20 10:00:00", "2026-07-24 10:00:00",
                        "HEALTHY", "2026-07-24 12:00:00"
                ));
    }

    @Test
    @DisplayName("브랜드 매출 차트는 브랜드명을 일괄 조회해 N+1 없이 매핑한다")
    void getDashboardDataMapsTopBrandsInBatch() {
        when(orderRepository.getTodaySummary()).thenReturn(Map.of(
                "todayOrderCount", 0L,
                "todayTotalAmount", 0L,
                "preparingCount", 0L,
                "shippingCount", 0L,
                "cancelledCount", 0L
        ));
        when(orderRepository.getRecentOrders(anyInt())).thenReturn(List.of());
        when(productRepository.getLowStockProducts(anyInt(), anyInt())).thenReturn(List.of());
        when(orderRepository.getSalesLast7Days()).thenReturn(List.of());
        when(orderRepository.getTopSellingProducts(anyInt())).thenReturn(List.of());
        when(adminOperationNoticeRepository.getActiveDashboardNotices(org.mockito.ArgumentMatchers.any(), anyInt()))
                .thenReturn(List.of(
                        AdminOperationNotice.builder()
                                .noticeNo(10L)
                                .title("배송 지연 안내")
                                .content("센터 점검으로 일부 출고가 늦어집니다.")
                                .isActive("Y")
                                .isPinned("Y")
                                .build()
                ));
        when(adminOperationTaskRepository.getDashboardTasks(org.mockito.ArgumentMatchers.any(), anyInt()))
                .thenReturn(List.of(
                        AdminOperationTask.builder()
                                .taskNo(30L)
                                .title("배치 점검")
                                .description("스케줄 점검")
                                .status("TODO")
                                .priority("HIGH")
                                .assigneeAdminNo(4L)
                                .isPinned("Y")
                                .build()
                ));
        when(adminOperationTaskRepository.getDashboardUnassignedTasks(org.mockito.ArgumentMatchers.any(), anyInt()))
                .thenReturn(List.of(
                        AdminOperationTask.builder()
                                .taskNo(31L)
                                .title("담당자 배정 필요")
                                .description("긴급 검토")
                                .status("TODO")
                                .priority("MEDIUM")
                                .assigneeAdminNo(null)
                                .isPinned("N")
                                .build()
                ));
        AdminOperationTaskCommentSummaryDto latestComment = new AdminOperationTaskCommentSummaryDto();
        latestComment.setTaskNo(31L);
        latestComment.setCommentNo(41L);
        latestComment.setAdminNo(5L);
        latestComment.setAdminName("관리자B");
        latestComment.setContent("담당자 확인 후 배정 필요");
        latestComment.setCrtDtm(java.time.LocalDateTime.of(2026, 5, 24, 9, 30));
        when(adminOperationTaskCommentRepository.getLatestCommentsByTaskNos(anyList()))
                .thenReturn(List.of(latestComment));
        when(adminOperationTaskRepository.getDashboardTaskWorkloads(org.mockito.ArgumentMatchers.any(), anyInt()))
                .thenReturn(List.of(
                        new AdminOperationTaskWorkloadDto(4L, "관리자A", 6L, 2L, 3L, 1L)
                ));
        when(adminOperationTaskRepository.getTaskWorkloadSummary(org.mockito.ArgumentMatchers.any(AdminOperationTaskWorkloadListQuery.class), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AdminOperationTaskWorkloadSummaryDto(1L, 6L, 1L, 2L));
        when(adminUserRepository.findAllById(anyList())).thenReturn(List.of(
                AdminUser.builder().adminNo(4L).name("관리자A").loginId("adminA").password("pw").build()
        ));
        when(adminProductService.getFrontDisplayProducts(org.mockito.ArgumentMatchers.any(ProductFrontDisplayListRequest.class)))
                .thenReturn(new ProductFrontDisplayDashboardResponse(
                        new ProductFrontDisplaySummaryResponse(3, 1, 2, 1, 2, 1, 1, 20L),
                        List.of(
                                new ProductFrontDisplayListResponse(101L, "전시 누락 상품", "브랜드A", "카테고리A", 100000, 8L, "ACTIVE", "판매중", false, false, null, null, null, true, 1),
                                new ProductFrontDisplayListResponse(102L, "문구 보완 상품", "브랜드B", "카테고리B", 90000, 45L, "ACTIVE", "판매중", true, false, "헤드라인", null, null, false, 999),
                                new ProductFrontDisplayListResponse(103L, "정상 상품", "브랜드C", "카테고리C", 85000, 50L, "ACTIVE", "판매중", true, true, "헤드라인", "설명", "무드", false, 999)
                        ),
                        null,
                        null
                ));
        when(orderRepository.getTopBrandsBySales(anyInt())).thenReturn(List.of(
                Map.of("brandNo", 1L, "amount", 1000L),
                Map.of("brandNo", 2L, "amount", 2000L)
        ));
        when(brandRepository.findAllById(anyList())).thenReturn(List.of(
                Brand.builder().brandNo(1L).nameKo("나이키").build(),
                Brand.builder().brandNo(2L).nameKo("아디다스").build()
        ));

        DashboardResponse response = adminDashBoardService.getDashboardData();

        assertEquals(3, response.frontDisplaySnapshot().summary().totalCount());
        assertEquals(2, response.frontDisplaySnapshot().summary().unconfiguredCount());
        assertEquals(2, response.frontDisplaySnapshot().actionItems().size());
        assertEquals("전시 누락 상품", response.frontDisplaySnapshot().actionItems().get(0).productName());
        assertEquals("노출 미설정 · 전시 문구 보완 · 저재고", response.frontDisplaySnapshot().actionItems().get(0).issueLabel());
        assertEquals(1, response.operationNotices().size());
        assertEquals("배송 지연 안내", response.operationNotices().get(0).title());
        assertEquals("/admin/settings/notices?noticeNo=10", response.operationNotices().get(0).targetPath());
        assertEquals("/admin/settings/notices/history?noticeNo=10", response.operationNotices().get(0).historyPath());
        assertEquals(1, response.operationTasks().size());
        assertEquals("배치 점검", response.operationTasks().get(0).title());
        assertEquals("/admin/settings/tasks?taskNo=30&openTaskNo=30&focusTaskNo=30&returnTo=%2Fadmin%2Fdashboard&source=dashboard-task", response.operationTasks().get(0).targetPath());
        assertEquals("/admin/settings/tasks/history?taskNo=30&returnTo=/admin/dashboard", response.operationTasks().get(0).historyPath());
        assertEquals("관리자A", response.operationTasks().get(0).assigneeName());
        assertEquals(1, response.unassignedTasks().size());
        assertEquals("담당자 배정 필요", response.unassignedTasks().get(0).title());
        assertEquals("담당자 확인 후 배정 필요", response.unassignedTasks().get(0).latestCommentContent());
        assertEquals("관리자B", response.unassignedTasks().get(0).latestCommentAdminName());
        assertEquals("/admin/settings/tasks?taskNo=31&openTaskNo=31&focusTaskNo=31&returnTo=%2Fadmin%2Fdashboard&source=dashboard-unassigned", response.unassignedTasks().get(0).targetPath());
        assertEquals("/admin/settings/tasks/history?taskNo=31&returnTo=/admin/dashboard", response.unassignedTasks().get(0).historyPath());
        assertEquals("/admin/settings/logs?actionType=TASK_&targetId=31", response.unassignedTasks().get(0).activityLogPath());
        assertEquals(1, response.taskWorkloads().size());
        assertEquals(2L, response.taskWorkloadSummary().unassignedTaskCount());
        assertEquals("/admin/settings/tasks?unassignedOnly=Y", response.taskWorkloadSummary().unassignedPath());
        assertEquals("관리자A", response.taskWorkloads().get(0).assigneeName());
        assertEquals(6L, response.taskWorkloads().get(0).totalCount());
        assertEquals("/admin/settings/tasks?assigneeAdminNo=4", response.taskWorkloads().get(0).targetPath());
        assertEquals("/admin/settings/tasks?assigneeAdminNo=4&overdueOnly=Y", response.taskWorkloads().get(0).overduePath());
        assertEquals(2, response.topBrands().size());
        assertEquals("나이키", response.topBrands().get(0).label());
        assertEquals("아디다스", response.topBrands().get(1).label());
        assertEquals(75, response.contentReactionSnapshot().helpfulRate());
        assertEquals(31L, response.contentReactionSnapshot().priorityAction().documentId());
        assertEquals("HEALTHY", response.contentReactionSnapshot().dataQualityStatus());
        verify(brandRepository).findAllById(anyList());
    }

    private ContentReactionAnalyticsResponse reactionAnalytics() {
        return new ContentReactionAnalyticsResponse(
                "ALL", 7, "2026-07-18", "2026-07-24", "2026-07-24 12:00:00",
                "기간 내 마지막 선택 시각 기준 현재 반응",
                new ContentReactionAnalyticsResponse.Summary(4, 3, 1, 75, 4, 1),
                List.of(),
                List.of(),
                List.of(new ContentReactionAnalyticsResponse.Content(
                        31, "NOTICE", "배송 안내", 4, 3, 1, 75
                ))
        );
    }

    @Test
    @DisplayName("매출 차트는 최근 7일을 빠짐없이 채우고 비어 있는 날짜는 0으로 보정한다")
    void getDashboardDataBackfillsMissingSalesDates() {
        when(orderRepository.getTodaySummary()).thenReturn(Map.of(
                "todayOrderCount", 0L,
                "todayTotalAmount", 0L,
                "preparingCount", 0L,
                "shippingCount", 0L,
                "cancelledCount", 0L
        ));
        when(orderRepository.getRecentOrders(anyInt())).thenReturn(List.of());
        when(productRepository.getLowStockProducts(anyInt(), anyInt())).thenReturn(List.of());
        when(orderRepository.getSalesLast7Days()).thenReturn(List.of(
                Map.of("date", LocalDate.now().minusDays(6).toString(), "amount", 1500L),
                Map.of("date", LocalDate.now().minusDays(2).toString(), "amount", 3200L),
                Map.of("date", LocalDate.now().toString(), "amount", 900L)
        ));
        when(orderRepository.getTopSellingProducts(anyInt())).thenReturn(List.of());
        when(orderRepository.getTopBrandsBySales(anyInt())).thenReturn(List.of());
        when(adminOperationNoticeRepository.getActiveDashboardNotices(org.mockito.ArgumentMatchers.any(), anyInt()))
                .thenReturn(List.of());
        when(adminOperationTaskRepository.getDashboardTasks(org.mockito.ArgumentMatchers.any(), anyInt()))
                .thenReturn(List.of());
        when(adminOperationTaskRepository.getDashboardUnassignedTasks(org.mockito.ArgumentMatchers.any(), anyInt()))
                .thenReturn(List.of());
        when(adminOperationTaskCommentRepository.getLatestCommentsByTaskNos(anyList())).thenReturn(List.of());
        when(adminOperationTaskRepository.getDashboardTaskWorkloads(org.mockito.ArgumentMatchers.any(), anyInt()))
                .thenReturn(List.of());
        when(adminOperationTaskRepository.getTaskWorkloadSummary(org.mockito.ArgumentMatchers.any(AdminOperationTaskWorkloadListQuery.class), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AdminOperationTaskWorkloadSummaryDto(0L, 0L, 0L, 0L));
        when(adminUserRepository.findAllById(anyList())).thenReturn(List.of());
        when(adminProductService.getFrontDisplayProducts(org.mockito.ArgumentMatchers.any(ProductFrontDisplayListRequest.class)))
                .thenReturn(new ProductFrontDisplayDashboardResponse(
                        new ProductFrontDisplaySummaryResponse(0, 0, 0, 0, 0, 0, 0, 20L),
                        List.of(),
                        null,
                        null
                ));
        when(brandRepository.findAllById(anyList())).thenReturn(List.of());

        DashboardResponse response = adminDashBoardService.getDashboardData();

        assertEquals(7, response.salesChart().size());
        assertEquals(LocalDate.now().minusDays(6).toString(), response.salesChart().get(0).label());
        assertEquals(1500L, response.salesChart().get(0).value());
        assertEquals(LocalDate.now().minusDays(5).toString(), response.salesChart().get(1).label());
        assertEquals(0L, response.salesChart().get(1).value());
        assertEquals(LocalDate.now().minusDays(2).toString(), response.salesChart().get(4).label());
        assertEquals(3200L, response.salesChart().get(4).value());
        assertEquals(LocalDate.now().toString(), response.salesChart().get(6).label());
        assertEquals(900L, response.salesChart().get(6).value());
    }
}
