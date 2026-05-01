package com.section.admin.dashboard.service;

import com.section.admin.dashboard.res.DashboardResponse;
import com.section.admin.order.support.OrderViewFormatter;
import com.section.common.commerce.dto.ProductListResDto;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.OrderRepository;
import com.section.common.commerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashBoardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;

    public DashboardResponse getDashboardData() {
        // 1. 오늘 요약 정보
        Map<String, Object> summaryMap = orderRepository.getTodaySummary();
        
        // Long/Integer 타입 캐스팅 오류 방지를 위해 Number 사용
        Number todayOrderCount = (Number) summaryMap.getOrDefault("todayOrderCount", 0L);
        Number todayTotalAmount = (Number) summaryMap.getOrDefault("todayTotalAmount", 0);
        Number preparingCount = (Number) summaryMap.getOrDefault("preparingCount", 0L);
        Number shippingCount = (Number) summaryMap.getOrDefault("shippingCount", 0L);
        Number cancelledCount = (Number) summaryMap.getOrDefault("cancelledCount", 0L);

        DashboardResponse.SummaryCounts summary = new DashboardResponse.SummaryCounts(
                todayOrderCount.longValue(),
                OrderViewFormatter.formatAmount(todayTotalAmount),
                preparingCount.longValue(),
                shippingCount.longValue(),
                cancelledCount.longValue()
        );

        // 2. 최근 주문 5건
        List<DashboardResponse.RecentOrder> recentOrders = orderRepository.getRecentOrders(5).stream()
                .map(DashboardResponse.RecentOrder::from)
                .toList();

        // 3. 재고 부족 상품 (10개 미만, 최대 5개)
        List<ProductListResDto> lowStockList = productRepository.getLowStockProducts(10, 5);
        List<DashboardResponse.LowStockProduct> lowStockProducts = lowStockList.stream()
                .map(p -> new DashboardResponse.LowStockProduct(
                        p.getProductNo(),
                        p.getProductName(),
                        p.getBrandName(),
                        p.getTotalStock()
                )).toList();

        // 4. 최근 7일 매출 차트
        List<DashboardResponse.ChartData> salesChart = orderRepository.getSalesLast7Days().stream()
                .map(m -> new DashboardResponse.ChartData(
                        (String) m.get("date"),
                        ((Number) m.get("amount")).longValue()
                )).toList();

        // 5. 인기 상품 Top 5
        List<DashboardResponse.ChartData> topProducts = orderRepository.getTopSellingProducts(5).stream()
                .map(m -> new DashboardResponse.ChartData(
                        (String) m.get("name"),
                        ((Number) m.get("count")).longValue()
                )).toList();

        // 6. 인기 브랜드 Top 5 (이름 매핑 포함)
        List<Map<String, Object>> brandSales = orderRepository.getTopBrandsBySales(5);
        Map<Long, String> brandNameMap = brandRepository.findAllById(
                        brandSales.stream()
                                .map(m -> (Long) m.get("brandNo"))
                                .filter(java.util.Objects::nonNull)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(Brand::getBrandNo, Brand::getNameKo));
        List<DashboardResponse.ChartData> topBrands = brandSales.stream()
                .map(m -> {
                    Long brandNo = (Long) m.get("brandNo");
                    // 집계 결과는 적지만 대시보드가 반복 조회되는 화면이라 브랜드명은 한번에 가져와 N+1을 피합니다.
                    String brandName = brandNameMap.getOrDefault(brandNo, "Unknown");
                    return new DashboardResponse.ChartData(brandName, ((Number) m.get("amount")).longValue());
                }).toList();

        return new DashboardResponse(summary, recentOrders, lowStockProducts, salesChart, topProducts, topBrands);
    }
}
