package com.section.admin.review.service;

import com.section.admin.review.res.AdminProductReviewListResponse;
import com.section.admin.review.res.AdminProductReviewResponse;
import com.section.common.commerce.entity.FrontProductReview;
import com.section.common.commerce.entity.FrontProductReviewReport;
import com.section.common.commerce.entity.FrontProductReviewReportStatus;
import com.section.common.commerce.entity.FrontProductReviewStatus;
import com.section.common.commerce.entity.FrontProductReviewStatusHistory;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.FrontProductReviewRepository;
import com.section.common.commerce.repository.FrontProductReviewReportRepository;
import com.section.common.commerce.repository.FrontProductReviewStatusHistoryRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductReviewService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final FrontProductReviewRepository reviewRepository;
    private final FrontProductReviewReportRepository reportRepository;
    private final FrontProductReviewStatusHistoryRepository statusHistoryRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final AdminUserRepository adminUserRepository;

    public AdminProductReviewListResponse getReviews(String rawStatus, boolean reportedOnly, boolean pendingOnly, int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "페이지 번호가 올바르지 않습니다.");
        }
        FrontProductReviewStatus status = parseStatus(rawStatus);
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(1, size), MAX_PAGE_SIZE));
        Page<FrontProductReview> reviews = pendingOnly
                ? reviewRepository.findReviewsWithReportStatus(
                status == null ? null : status.name(), FrontProductReviewReportStatus.PENDING.name(), pageable
        )
                : reportedOnly
                ? reviewRepository.findReportedReviews(status == null ? null : status.name(), pageable)
                : status == null
                ? reviewRepository.findAllByOrderByIdDesc(pageable)
                : reviewRepository.findByStatusOrderByIdDesc(status.name(), pageable);
        List<Long> reviewIds = reviews.stream().map(FrontProductReview::getId).toList();
        Map<Long, Long> reportCounts = reviewIds.isEmpty() ? Map.of() : reportRepository.countByReviewNoIn(reviewIds).stream()
                .collect(Collectors.toMap(FrontProductReviewReportRepository.ReviewReportCount::getReviewNo,
                        FrontProductReviewReportRepository.ReviewReportCount::getCount));
        Map<Long, List<FrontProductReviewReport>> reports = reviewIds.isEmpty() ? Map.of()
                : reportRepository.findAllByReviewNoInOrderByIdDesc(reviewIds).stream()
                .collect(Collectors.groupingBy(FrontProductReviewReport::getReviewNo));
        Map<Long, Long> pendingReportCounts = reviewIds.isEmpty() ? Map.of() : reportRepository
                .countByReviewNoInAndStatus(reviewIds, FrontProductReviewReportStatus.PENDING.name()).stream()
                .collect(Collectors.toMap(FrontProductReviewReportRepository.ReviewReportCount::getReviewNo,
                        FrontProductReviewReportRepository.ReviewReportCount::getCount));
        Map<Long, List<FrontProductReviewStatusHistory>> statusHistories = reviewIds.isEmpty() ? Map.of()
                : statusHistoryRepository.findAllByReviewNoInOrderByIdDesc(reviewIds).stream()
                .collect(Collectors.groupingBy(FrontProductReviewStatusHistory::getReviewNo));
        List<Long> actorIds = java.util.stream.Stream.concat(
                        statusHistories.values().stream().flatMap(List::stream).map(FrontProductReviewStatusHistory::getCrtNo),
                        reports.values().stream().flatMap(List::stream)
                                .filter(FrontProductReviewReport::isResolved)
                                .map(FrontProductReviewReport::getUptNo)
                )
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> adminNames = actorIds.isEmpty() ? Map.of() : adminUserRepository.findAllById(
                        actorIds
                ).stream()
                .collect(Collectors.toMap(AdminUser::getAdminNo, AdminUser::getName));
        Map<Long, Product> products = productRepository.findAllById(
                        reviews.stream().map(FrontProductReview::getProductNo).collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, Brand> brands = brandRepository.findAllById(
                        products.values().stream().map(Product::getBrandNo).collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(Brand::getBrandNo, Function.identity()));
        return new AdminProductReviewListResponse(
                reviews.stream().map(review -> response(
                        review, products.get(review.getProductNo()), brands,
                        reportCounts.getOrDefault(review.getId(), 0L), reports.getOrDefault(review.getId(), List.of()),
                        pendingReportCounts.getOrDefault(review.getId(), 0L), statusHistories.getOrDefault(review.getId(), List.of()), adminNames
                )).toList(),
                reviews.getTotalElements(),
                reviews.getNumber(),
                reviews.getTotalPages(),
                reviews.hasNext()
        );
    }

    @Transactional
    public void changeStatus(long reviewId, FrontProductReviewStatus status) {
        if (reviewId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "후기 번호가 올바르지 않습니다.");
        }
        // 동일 후기의 숨김·복구 요청은 한 전환씩 처리해 감사 이력 순서를 보장합니다.
        FrontProductReview review = reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "후기를 찾을 수 없습니다."));
        if (status.name().equals(review.getStatus())) {
            return;
        }
        String beforeStatus = review.getStatus();
        review.changeStatus(status);
        resolvePendingReports(reviewId);
        statusHistoryRepository.save(FrontProductReviewStatusHistory.create(reviewId, beforeStatus, status.name()));
    }

    @Transactional
    public int resolveReports(long reviewId) {
        if (reviewId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "후기 번호가 올바르지 않습니다.");
        }
        reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "후기를 찾을 수 없습니다."));
        return resolvePendingReports(reviewId);
    }

    private FrontProductReviewStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank() || "ALL".equalsIgnoreCase(rawStatus)) {
            return null;
        }
        try {
            return FrontProductReviewStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "후기 상태가 올바르지 않습니다.");
        }
    }

    private AdminProductReviewResponse response(
            FrontProductReview review,
            Product product,
        Map<Long, Brand> brands,
        long reportCount,
        List<FrontProductReviewReport> reports,
        long pendingReportCount,
        List<FrontProductReviewStatusHistory> statusHistories,
            Map<Long, String> adminNames
    ) {
        String status = review.getStatus();
        return new AdminProductReviewResponse(
                review.getId(),
                review.getProductNo(),
                product == null ? "삭제되었거나 찾을 수 없는 상품" : product.getNameKo(),
                product == null || brands.get(product.getBrandNo()) == null ? "-" : brands.get(product.getBrandNo()).getNameKo(),
                review.getReviewerName(),
                review.getRating(),
                review.getContent(),
                status,
                FrontProductReviewStatus.HIDDEN.name().equals(status) ? "숨김" : "노출",
                reportCount,
                pendingReportCount,
                reports.stream().map(report -> new AdminProductReviewResponse.ReportDetail(
                        report.getReason(), report.getDetail(), reportStatusLabel(report.getStatus()),
                        report.isResolved() ? adminNames.getOrDefault(report.getUptNo(), report.getUptNo() == null ? "-" : "관리자#" + report.getUptNo()) : "-",
                        report.isResolved() && report.getUptDtm() != null ? report.getUptDtm().format(DATE_TIME_FORMATTER) : "-",
                        report.getCrtDtm() == null ? "-" : report.getCrtDtm().format(DATE_TIME_FORMATTER)
                )).toList(),
                statusHistories.stream().map(history -> new AdminProductReviewResponse.StatusHistoryDetail(
                        "HIDE".equals(history.getActionType()) ? "숨김" : "복구",
                        statusLabel(history.getBeforeStatus()),
                        statusLabel(history.getAfterStatus()),
                        adminNames.getOrDefault(history.getCrtNo(), history.getCrtNo() == null ? "-" : "관리자#" + history.getCrtNo()),
                        history.getCrtDtm() == null ? "-" : history.getCrtDtm().format(DATE_TIME_FORMATTER)
                )).toList(),
                review.getCrtDtm() == null ? "-" : review.getCrtDtm().format(DATE_TIME_FORMATTER)
        );
    }

    private String statusLabel(String status) {
        return FrontProductReviewStatus.HIDDEN.name().equals(status) ? "숨김" : "노출";
    }

    private String reportStatusLabel(String status) {
        return FrontProductReviewReportStatus.RESOLVED.name().equals(status) ? "처리 완료" : "처리 대기";
    }

    private int resolvePendingReports(long reviewId) {
        List<FrontProductReviewReport> pendingReports = reportRepository.findAllByReviewNoAndStatus(
                reviewId,
                FrontProductReviewReportStatus.PENDING.name()
        );
        pendingReports.forEach(FrontProductReviewReport::resolve);
        return pendingReports.size();
    }
}
