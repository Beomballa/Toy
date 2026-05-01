package com.section.admin.dashboard.service;

import com.section.admin.dashboard.res.DashboardResponse;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.OrderRepository;
import com.section.common.commerce.repository.ProductRepository;
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
        when(orderRepository.getTopBrandsBySales(anyInt())).thenReturn(List.of(
                Map.of("brandNo", 1L, "amount", 1000L),
                Map.of("brandNo", 2L, "amount", 2000L)
        ));
        when(brandRepository.findAllById(anyList())).thenReturn(List.of(
                Brand.builder().brandNo(1L).nameKo("나이키").build(),
                Brand.builder().brandNo(2L).nameKo("아디다스").build()
        ));

        DashboardResponse response = adminDashBoardService.getDashboardData();

        assertEquals(2, response.topBrands().size());
        assertEquals("나이키", response.topBrands().get(0).label());
        assertEquals("아디다스", response.topBrands().get(1).label());
        verify(brandRepository).findAllById(anyList());
    }
}
