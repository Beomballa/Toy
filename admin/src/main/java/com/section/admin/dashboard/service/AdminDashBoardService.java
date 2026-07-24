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
import com.section.admin.order.support.OrderViewFormatter;
import com.section.admin.task.support.AdminTaskLinkSupport;
import com.section.common.commerce.dto.ProductListResDto;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.OrderRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.common.system.entity.AdminOperationTask;
import com.section.common.system.entity.AdminOperationNotice;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.dto.AdminOperationTaskCommentSummaryDto;
import com.section.common.system.dto.AdminOperationTaskWorkloadListQuery;
import com.section.common.system.dto.AdminOperationTaskWorkloadDto;
import com.section.common.system.dto.AdminOperationTaskWorkloadSummaryDto;
import com.section.common.system.repository.AdminOperationTaskCommentRepository;
import com.section.common.system.repository.AdminOperationNoticeRepository;
import com.section.common.system.repository.AdminOperationTaskRepository;
import com.section.common.system.repository.AdminUserRepository;
import com.section.common.base.entity.type.AdminOperationTaskPriority;
import com.section.common.base.entity.type.AdminOperationTaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashBoardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final AdminOperationNoticeRepository adminOperationNoticeRepository;
    private final AdminOperationTaskRepository adminOperationTaskRepository;
    private final AdminOperationTaskCommentRepository adminOperationTaskCommentRepository;
    private final AdminUserRepository adminUserRepository;
    private final AdminProductService adminProductService;
    private final AdminContentReactionAnalyticsService contentReactionAnalyticsService;

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
        DashboardResponse.FrontDisplaySnapshot frontDisplaySnapshot = buildFrontDisplaySnapshot();

        List<DashboardResponse.OperationNotice> operationNotices = adminOperationNoticeRepository
                .getActiveDashboardNotices(LocalDateTime.now(), 3)
                .stream()
                .map(this::toOperationNotice)
                .toList();

        List<AdminOperationTask> dashboardTasks = adminOperationTaskRepository.getDashboardTasks(LocalDate.now(), 5);
        Map<Long, String> adminNameMap = adminUserRepository.findAllById(
                        dashboardTasks.stream()
                                .map(AdminOperationTask::getAssigneeAdminNo)
                                .filter(java.util.Objects::nonNull)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(AdminUser::getAdminNo, AdminUser::getName));
        List<DashboardResponse.OperationTask> operationTasks = dashboardTasks.stream()
                .map(task -> toOperationTask(task, adminNameMap))
                .toList();
        List<AdminOperationTask> unassignedTaskEntities = adminOperationTaskRepository
                .getDashboardUnassignedTasks(LocalDate.now(), 5);
        Map<Long, AdminOperationTaskCommentSummaryDto> latestCommentMap = adminOperationTaskCommentRepository
                .getLatestCommentsByTaskNos(
                        unassignedTaskEntities.stream()
                                .map(AdminOperationTask::getTaskNo)
                                .filter(Objects::nonNull)
                                .toList()
                ).stream()
                .collect(Collectors.toMap(AdminOperationTaskCommentSummaryDto::getTaskNo, item -> item));
        List<DashboardResponse.UnassignedTask> unassignedTaskItems = unassignedTaskEntities.stream()
                .map(task -> toUnassignedTask(task, latestCommentMap.get(task.getTaskNo())))
                .toList();
        List<DashboardResponse.TaskWorkload> taskWorkloads = adminOperationTaskRepository
                .getDashboardTaskWorkloads(LocalDate.now(), 5)
                .stream()
                .map(this::toTaskWorkload)
                .toList();
        AdminOperationTaskWorkloadSummaryDto workloadSummary = adminOperationTaskRepository.getTaskWorkloadSummary(
                new AdminOperationTaskWorkloadListQuery(null, null, null, null),
                LocalDate.now()
        );
        DashboardResponse.TaskWorkloadSummary taskWorkloadSummary = new DashboardResponse.TaskWorkloadSummary(
                workloadSummary.assigneeCount(),
                workloadSummary.assignedTaskCount(),
                workloadSummary.overdueTaskCount(),
                workloadSummary.unassignedTaskCount(),
                "/admin/settings/tasks/workloads",
                "/admin/settings/tasks?unassignedOnly=Y"
        );
        DashboardResponse.ContentReactionSnapshot contentReactionSnapshot = buildContentReactionSnapshot();

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
        List<DashboardResponse.ChartData> salesChart = normalizeSalesChart(orderRepository.getSalesLast7Days());

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

        return new DashboardResponse(
                summary, frontDisplaySnapshot, operationNotices, operationTasks, unassignedTaskItems,
                taskWorkloadSummary, taskWorkloads, contentReactionSnapshot, recentOrders,
                lowStockProducts, salesChart, topProducts, topBrands
        );
    }

    private DashboardResponse.ContentReactionSnapshot buildContentReactionSnapshot() {
        ContentReactionAnalyticsResponse analytics = contentReactionAnalyticsService.getAnalytics(null, 7);
        ContentReactionDataQualityResponse quality = contentReactionAnalyticsService.getDataQuality();
        DashboardResponse.ReactionActionItem action = analytics.improvementContents().stream()
                .findFirst()
                .map(item -> new DashboardResponse.ReactionActionItem(
                        item.documentId(),
                        item.boardType(),
                        item.title(),
                        item.notHelpfulCount(),
                        item.helpfulRate(),
                        "/admin/content/get?id=" + item.documentId()
                                + "&boardType=" + item.boardType()
                                + "&source=dashboard-content-reaction"
                                + "&returnTo=/admin/dashboard"
                ))
                .orElse(null);
        return new DashboardResponse.ContentReactionSnapshot(
                analytics.summary().totalCount(),
                analytics.summary().helpfulRate(),
                analytics.summary().evaluatedContentCount(),
                quality.orphanCount(),
                quality.status(),
                action,
                "/admin/content/list?source=dashboard-content-reaction&returnTo=/admin/dashboard"
        );
    }

    private DashboardResponse.FrontDisplaySnapshot buildFrontDisplaySnapshot() {
        ProductFrontDisplayDashboardResponse frontDisplayProducts = adminProductService.getFrontDisplayProducts(
                new ProductFrontDisplayListRequest(null, null, null, null, null, null, false, false, null, "FEATURED")
        );
        ProductFrontDisplaySummaryResponse summary = frontDisplayProducts.summary();
        List<DashboardResponse.FrontDisplayActionItem> actionItems = frontDisplayProducts.items().stream()
                .filter(item -> requiresFrontDisplayAction(item, summary.lowStockThreshold()))
                .sorted((left, right) -> Integer.compare(frontDisplayIssueScore(right, summary.lowStockThreshold()), frontDisplayIssueScore(left, summary.lowStockThreshold())))
                .limit(5)
                .map(item -> toFrontDisplayActionItem(item, summary.lowStockThreshold()))
                .toList();

        return new DashboardResponse.FrontDisplaySnapshot(
                new DashboardResponse.FrontDisplaySummary(
                        summary.totalCount(),
                        summary.configuredCount(),
                        summary.unconfiguredCount(),
                        summary.readyContentCount(),
                        summary.incompleteContentCount(),
                        summary.featuredCount(),
                        summary.lowStockCount(),
                        summary.lowStockThreshold()
                ),
                actionItems,
                "/admin/products/front-display",
                "/admin/products/front-display?configured=UNCONFIGURED",
                "/admin/products/front-display?contentStatus=INCOMPLETE",
                "/admin/products/front-display?lowStockOnly=true"
        );
    }

    private List<DashboardResponse.ChartData> normalizeSalesChart(List<Map<String, Object>> rawChart) {
        Map<String, Long> salesByDate = rawChart.stream()
                .filter(item -> item.get("date") instanceof String)
                .collect(Collectors.toMap(
                        item -> (String) item.get("date"),
                        item -> ((Number) item.getOrDefault("amount", 0L)).longValue(),
                        Long::sum,
                        LinkedHashMap::new
                ));

        return LocalDate.now().minusDays(6).datesUntil(LocalDate.now().plusDays(1))
                .map(date -> {
                    String label = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
                    return new DashboardResponse.ChartData(label, salesByDate.getOrDefault(label, 0L));
                })
                .toList();
    }

    private boolean requiresFrontDisplayAction(ProductFrontDisplayListResponse item, long lowStockThreshold) {
        return !item.displayConfigured() || !item.contentReady() || isLowStock(item.totalStock(), lowStockThreshold);
    }

    private int frontDisplayIssueScore(ProductFrontDisplayListResponse item, long lowStockThreshold) {
        int score = 0;
        if (!item.displayConfigured()) {
            score += 3;
        }
        if (!item.contentReady()) {
            score += 2;
        }
        if (isLowStock(item.totalStock(), lowStockThreshold)) {
            score += 1;
        }
        if (item.featured()) {
            score += 1;
        }
        return score;
    }

    private DashboardResponse.FrontDisplayActionItem toFrontDisplayActionItem(ProductFrontDisplayListResponse item, long lowStockThreshold) {
        StringJoiner issueJoiner = new StringJoiner(" · ");
        if (!item.displayConfigured()) {
            issueJoiner.add("노출 미설정");
        }
        if (!item.contentReady()) {
            issueJoiner.add("전시 문구 보완");
        }
        if (isLowStock(item.totalStock(), lowStockThreshold)) {
            issueJoiner.add("저재고");
        }

        String issueDetail = buildFrontDisplayIssueDetail(item, lowStockThreshold);
        return new DashboardResponse.FrontDisplayActionItem(
                item.productNo(),
                item.productName(),
                item.brandName(),
                item.totalStock(),
                item.displayConfigured(),
                item.contentReady(),
                item.featured(),
                issueJoiner.length() == 0 ? "정상" : issueJoiner.toString(),
                issueDetail
        );
    }

    private String buildFrontDisplayIssueDetail(ProductFrontDisplayListResponse item, long lowStockThreshold) {
        StringJoiner detailJoiner = new StringJoiner(" · ");
        detailJoiner.add(item.displayConfigured() ? "노출 설정 완료" : "노출 설정 필요");
        detailJoiner.add(item.contentReady() ? "전시 문구 준비 완료" : "헤드라인/설명/무드 보완 필요");
        if (isLowStock(item.totalStock(), lowStockThreshold)) {
            detailJoiner.add("재고 " + item.totalStock() + "개");
        }
        if (item.featured()) {
            detailJoiner.add("Featured");
        }
        return detailJoiner.toString();
    }

    private boolean isLowStock(Long totalStock, long lowStockThreshold) {
        return totalStock != null && totalStock < lowStockThreshold;
    }


    private DashboardResponse.OperationNotice toOperationNotice(AdminOperationNotice notice) {
        return new DashboardResponse.OperationNotice(
                notice.getNoticeNo(),
                notice.getTitle(),
                notice.getContent(),
                "Y".equalsIgnoreCase(notice.getIsPinned()),
                buildPeriodLabel(notice),
                "/admin/settings/notices?noticeNo=" + notice.getNoticeNo(),
                "/admin/settings/notices/history?noticeNo=" + notice.getNoticeNo()
        );
    }

    private String buildPeriodLabel(AdminOperationNotice notice) {
        if (notice.getStartDtm() == null && notice.getEndDtm() == null) {
            return "상시 노출";
        }
        String start = notice.getStartDtm() == null ? "-" : OrderViewFormatter.formatDateTime(notice.getStartDtm());
        String end = notice.getEndDtm() == null ? "-" : OrderViewFormatter.formatDateTime(notice.getEndDtm());
        return start + " ~ " + end;
    }

    private DashboardResponse.OperationTask toOperationTask(AdminOperationTask task, Map<Long, String> adminNameMap) {
        return new DashboardResponse.OperationTask(
                task.getTaskNo(),
                task.getTitle(),
                AdminOperationTaskStatus.fromCode(task.getStatus()).getLabel(),
                AdminOperationTaskPriority.fromCode(task.getPriority()).getLabel(),
                adminNameMap.getOrDefault(task.getAssigneeAdminNo(), "미지정"),
                buildTaskDueDateLabel(task),
                "Y".equalsIgnoreCase(task.getIsPinned()),
                AdminTaskLinkSupport.buildListOpenPath(task.getTaskNo(), "/admin/dashboard", "dashboard-task"),
                "/admin/settings/tasks/history?taskNo=" + task.getTaskNo() + "&returnTo=/admin/dashboard",
                "/admin/settings/logs?actionType=TASK_&targetId=" + task.getTaskNo()
        );
    }

    private DashboardResponse.TaskWorkload toTaskWorkload(AdminOperationTaskWorkloadDto item) {
        String assigneeName = item.assigneeAdminName() == null || item.assigneeAdminName().isBlank()
                ? "미지정"
                : item.assigneeAdminName();
        String targetPath = item.assigneeAdminNo() == null
                ? "/admin/settings/tasks"
                : "/admin/settings/tasks?assigneeAdminNo=" + item.assigneeAdminNo();
        String overduePath = item.assigneeAdminNo() == null
                ? "/admin/settings/tasks?overdueOnly=Y"
                : "/admin/settings/tasks?assigneeAdminNo=" + item.assigneeAdminNo() + "&overdueOnly=Y";
        return new DashboardResponse.TaskWorkload(
                item.assigneeAdminNo(),
                assigneeName,
                item.totalCount(),
                item.todoCount(),
                item.inProgressCount(),
                item.overdueCount(),
                targetPath,
                overduePath
        );
    }

    private DashboardResponse.UnassignedTask toUnassignedTask(AdminOperationTask task, AdminOperationTaskCommentSummaryDto latestComment) {
        return new DashboardResponse.UnassignedTask(
                task.getTaskNo(),
                task.getTitle(),
                AdminOperationTaskStatus.fromCode(task.getStatus()).getLabel(),
                AdminOperationTaskPriority.fromCode(task.getPriority()).getLabel(),
                buildTaskDueDateLabel(task),
                "Y".equalsIgnoreCase(task.getIsPinned()),
                latestComment == null ? null : latestComment.getContent(),
                latestComment == null ? null : resolveCommentAdminName(latestComment),
                latestComment == null ? null : formatDateTime(latestComment.getCrtDtm()),
                AdminTaskLinkSupport.buildListOpenPath(task.getTaskNo(), "/admin/dashboard", "dashboard-unassigned"),
                "/admin/settings/tasks/history?taskNo=" + task.getTaskNo() + "&returnTo=/admin/dashboard",
                "/admin/settings/logs?actionType=TASK_&targetId=" + task.getTaskNo()
        );
    }

    private String resolveCommentAdminName(AdminOperationTaskCommentSummaryDto comment) {
        if (comment.getAdminName() != null && !comment.getAdminName().isBlank()) {
            return comment.getAdminName();
        }
        return comment.getAdminNo() == null ? "관리자" : "관리자#" + comment.getAdminNo();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : OrderViewFormatter.formatDateTime(value);
    }

    private String buildTaskDueDateLabel(AdminOperationTask task) {
        if (task.getDueDate() == null) {
            return "기한 없음";
        }
        if ("DONE".equalsIgnoreCase(task.getStatus())) {
            return "완료";
        }
        if (task.getDueDate().isBefore(LocalDate.now())) {
            return task.getDueDate() + " · 기한 초과";
        }
        if (task.getDueDate().isEqual(LocalDate.now())) {
            return task.getDueDate() + " · 오늘 마감";
        }
        return task.getDueDate().toString();
    }
}
