package com.section.admin.review.service;

import com.section.admin.review.res.AdminProductReviewListResponse;
import com.section.admin.review.res.AdminProductReviewResponse;
import com.section.common.commerce.entity.FrontProductReview;
import com.section.common.commerce.entity.FrontProductReviewReport;
import com.section.common.commerce.entity.FrontProductReviewStatus;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.FrontProductReviewRepository;
import com.section.common.commerce.repository.FrontProductReviewReportRepository;
import com.section.common.commerce.repository.ProductRepository;
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

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final FrontProductReviewRepository reviewRepository;
    private final FrontProductReviewReportRepository reportRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;

    public AdminProductReviewListResponse getReviews(String rawStatus, boolean reportedOnly, int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "페이지 번호가 올바르지 않습니다.");
        }
        FrontProductReviewStatus status = parseStatus(rawStatus);
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(1, size), MAX_PAGE_SIZE));
        Page<FrontProductReview> reviews = reportedOnly
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
                        reportCounts.getOrDefault(review.getId(), 0L), reports.getOrDefault(review.getId(), List.of())
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
        FrontProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "후기를 찾을 수 없습니다."));
        review.changeStatus(status);
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
            List<FrontProductReviewReport> reports
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
                reports.stream().map(report -> new AdminProductReviewResponse.ReportDetail(
                        report.getReason(), report.getDetail(),
                        report.getCrtDtm() == null ? "-" : report.getCrtDtm().format(DATE_TIME_FORMATTER)
                )).toList(),
                review.getCrtDtm() == null ? "-" : review.getCrtDtm().format(DATE_TIME_FORMATTER)
        );
    }
}
