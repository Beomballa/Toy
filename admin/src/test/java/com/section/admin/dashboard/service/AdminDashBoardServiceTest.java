package com.section.admin.dashboard.service;

import com.section.admin.dashboard.res.DashboardResponse;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.OrderRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.common.system.entity.AdminOperationNotice;
import com.section.common.system.entity.AdminOperationTask;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminOperationNoticeRepository;
import com.section.common.system.repository.AdminOperationTaskRepository;
import com.section.common.system.repository.AdminUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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
    private AdminUserRepository adminUserRepository;

    @InjectMocks
    private AdminDashBoardService adminDashBoardService;

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
        when(adminUserRepository.findAllById(anyList())).thenReturn(List.of(
                AdminUser.builder().adminNo(4L).name("관리자A").loginId("adminA").password("pw").build()
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

        assertEquals(1, response.operationNotices().size());
        assertEquals("배송 지연 안내", response.operationNotices().get(0).title());
        assertEquals("/admin/settings/notices?noticeNo=10", response.operationNotices().get(0).targetPath());
        assertEquals("/admin/settings/notices/history?noticeNo=10", response.operationNotices().get(0).historyPath());
        assertEquals(1, response.operationTasks().size());
        assertEquals("배치 점검", response.operationTasks().get(0).title());
        assertEquals("/admin/settings/tasks/get?no=30&returnTo=/admin/dashboard", response.operationTasks().get(0).targetPath());
        assertEquals("관리자A", response.operationTasks().get(0).assigneeName());
        assertEquals(2, response.topBrands().size());
        assertEquals("나이키", response.topBrands().get(0).label());
        assertEquals("아디다스", response.topBrands().get(1).label());
        verify(brandRepository).findAllById(anyList());
    }
}
