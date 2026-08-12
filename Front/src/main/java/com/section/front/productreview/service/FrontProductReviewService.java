package com.section.front.productreview.service;

import com.section.common.base.entity.type.OrderStatus;
import com.section.common.commerce.entity.FrontProductReview;
import com.section.common.commerce.entity.FrontProductReviewStatus;
import com.section.common.commerce.entity.FrontProductReviewReport;
import com.section.common.commerce.entity.Orders;
import com.section.common.commerce.repository.FrontProductReviewRepository;
import com.section.common.commerce.repository.FrontProductReviewReportRepository;
import com.section.common.commerce.repository.FrontProductReviewStatusHistoryRepository;
import com.section.common.commerce.repository.OrderItemRepository;
import com.section.common.commerce.repository.OrderRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.common.system.entity.Account;
import com.section.common.system.repository.AccountRepository;
import com.section.front.productreview.dto.FrontProductReviewCreateRequest;
import com.section.front.productreview.dto.FrontProductReviewReportRequest;
import com.section.front.productreview.dto.FrontReviewEligibleOrderResponse;
import com.section.front.productreview.dto.FrontMemberProductReviewPageResponse;
import com.section.front.productreview.dto.FrontMemberProductReviewResponse;
import com.section.front.productreview.dto.FrontProductReviewPageResponse;
import com.section.front.productreview.dto.FrontProductReviewResponse;
import com.section.front.product.dto.FrontProductResponse;
import com.section.front.product.service.FrontProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FrontProductReviewService {

    private static final int REVIEW_PAGE_SIZE = 10;

    private final FrontProductReviewRepository reviewRepository;
    private final FrontProductReviewReportRepository reportRepository;
    private final FrontProductReviewStatusHistoryRepository statusHistoryRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AccountRepository accountRepository;
    private final FrontProductCatalogService productCatalogService;

    @Transactional(readOnly = true)
    public FrontProductReviewPageResponse getReviews(long productNo, int pageNumber, String rawSort, Long memberNo) {
        requireProduct(productNo);
        if (pageNumber < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "페이지 번호가 올바르지 않습니다.");
        }

        FrontProductReviewSort sort = FrontProductReviewSort.from(rawSort);
        PageRequest pageable = PageRequest.of(pageNumber, REVIEW_PAGE_SIZE);
        Page<FrontProductReview> reviews = switch (sort) {
            case RATING_DESC -> reviewRepository.findByProductNoAndStatusOrderByRatingDescIdDesc(
                    productNo, FrontProductReviewStatus.VISIBLE.name(), pageable);
            case RATING_ASC -> reviewRepository.findByProductNoAndStatusOrderByRatingAscIdDesc(
                    productNo, FrontProductReviewStatus.VISIBLE.name(), pageable);
            case RECENT -> reviewRepository.findByProductNoAndStatusOrderByIdDesc(
                    productNo, FrontProductReviewStatus.VISIBLE.name(), pageable);
        };
        Object[] summary = reviewRepository.getSummaryByProductNo(productNo, FrontProductReviewStatus.VISIBLE.name());
        long totalCount = ((Number) summary[0]).longValue();
        double averageRating = ((Number) summary[1]).doubleValue();
        long[] ratingDistribution = new long[5];
        reviewRepository.countByProductNoAndStatusGroupByRating(productNo, FrontProductReviewStatus.VISIBLE.name())
                .forEach(row -> {
                    int rating = row.getRating() == null ? 0 : row.getRating();
                    if (rating >= 1 && rating <= 5) {
                        ratingDistribution[5 - rating] = row.getCount();
                    }
                });
        java.util.Set<Long> reportedReviewNos = memberNo == null || reviews.isEmpty() ? java.util.Set.of()
                : new java.util.HashSet<>(reportRepository.findReviewNosByMemberNoAndReviewNoIn(
                memberNo,
                reviews.stream().map(FrontProductReview::getId).toList()
        ));
        return new FrontProductReviewPageResponse(
                reviews.map(review -> FrontProductReviewResponse.from(review, reportedReviewNos.contains(review.getId()))).getContent(),
                totalCount,
                Math.round(averageRating * 10) / 10.0,
                java.util.Arrays.stream(ratingDistribution).boxed().toList(),
                reviews.getNumber(),
                reviews.getTotalPages(),
                reviews.hasNext()
        );
    }

    @Transactional
    public FrontProductReviewResponse createReview(
            long memberNo,
            long productNo,
            FrontProductReviewCreateRequest request
    ) {
        requireProduct(productNo);
        Account member = accountRepository.findById(memberNo)
                .filter(Account::isAvailableCustomer)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용할 수 없는 회원입니다."));
        Orders order = orderRepository.findByOrderNumAndMemberNoForUpdate(normalizeOrderNumber(request.orderNumber()), memberNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));
        if (!OrderStatus.DELIVERED.name().equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "배송 완료된 상품에만 리뷰를 작성할 수 있습니다.");
        }
        if (!orderItemRepository.existsByOrderNoAndProductNo(order.getId(), productNo)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 주문에 포함되지 않은 상품입니다.");
        }
        if (reviewRepository.existsByMemberNoAndOrderNoAndProductNo(memberNo, order.getId(), productNo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 작성한 리뷰입니다.");
        }

        String content = normalizeContent(request.content());
        if (content.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "후기 내용을 입력하세요.");
        }
        FrontProductReview review = FrontProductReview.create(
                memberNo,
                productNo,
                order.getId(),
                reviewerName(member),
                request.rating(),
                content
        );
        try {
            return FrontProductReviewResponse.from(reviewRepository.saveAndFlush(review), false);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 작성한 리뷰입니다.", exception);
        }
    }

    @Transactional
    public void reportReview(long memberNo, long reviewId, FrontProductReviewReportRequest request) {
        Account member = accountRepository.findById(memberNo)
                .filter(Account::isAvailableCustomer)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용할 수 없는 회원입니다."));
        // 동일 후기 신고는 잠금 안에서 중복 여부를 확인해 고유 제약 위반을 사용자 오류로 노출하지 않습니다.
        FrontProductReview review = reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "후기를 찾을 수 없습니다."));
        if (!review.isVisible()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "후기를 찾을 수 없습니다.");
        }
        if (memberNo == review.getMemberNo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "본인이 작성한 후기는 신고할 수 없습니다.");
        }
        if (reportRepository.existsByReviewNoAndMemberNo(reviewId, memberNo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 신고한 후기입니다.");
        }
        String reason = normalizeRequiredText(request.reason(), "신고 사유", 30);
        String detail = normalizeOptionalText(request.detail(), 500);
        reportRepository.save(FrontProductReviewReport.create(reviewId, member.getId(), reason, detail));
    }

    @Transactional(readOnly = true)
    public List<FrontReviewEligibleOrderResponse> getEligibleOrders(long memberNo, long productNo) {
        requireProduct(productNo);
        accountRepository.findById(memberNo)
                .filter(Account::isAvailableCustomer)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용할 수 없는 회원입니다."));
        return orderRepository.findReviewEligibleOrders(memberNo, productNo, OrderStatus.DELIVERED.name()).stream()
                .map(order -> new FrontReviewEligibleOrderResponse(order.getOrderNum()))
                .toList();
    }

    @Transactional(readOnly = true)
    public FrontMemberProductReviewPageResponse getMemberReviews(long memberNo, int pageNumber) {
        if (pageNumber < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "페이지 번호가 올바르지 않습니다.");
        }
        accountRepository.findById(memberNo)
                .filter(Account::isAvailableCustomer)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용할 수 없는 회원입니다."));
        Page<FrontProductReview> reviews = reviewRepository.findByMemberNoOrderByIdDesc(
                memberNo,
                PageRequest.of(pageNumber, REVIEW_PAGE_SIZE)
        );
        Map<Long, FrontProductResponse> products = productCatalogService.findProducts(
                new LinkedHashSet<>(reviews.map(FrontProductReview::getProductNo).toList())
        );
        List<FrontMemberProductReviewResponse> responses = reviews.stream()
                .map(review -> toMemberReviewResponse(review, products.get(review.getProductNo())))
                .filter(java.util.Objects::nonNull)
                .toList();
        return new FrontMemberProductReviewPageResponse(
                responses,
                reviews.getTotalElements(),
                reviews.getNumber(),
                reviews.getTotalPages(),
                reviews.hasNext()
        );
    }

    @Transactional
    public void deleteMemberReview(long memberNo, long reviewId) {
        if (reviewId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "후기 번호가 올바르지 않습니다.");
        }
        accountRepository.findById(memberNo)
                .filter(Account::isAvailableCustomer)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용할 수 없는 회원입니다."));
        // 신고와 동일한 후기 잠금을 사용해 운영 기록 확인과 삭제가 교차하지 않도록 합니다.
        FrontProductReview review = reviewRepository.findByIdAndMemberNoForUpdate(reviewId, memberNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "후기를 찾을 수 없습니다."));
        if (reportRepository.existsByReviewNo(review.getId()) || statusHistoryRepository.existsByReviewNo(review.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "신고 또는 운영 처리 이력이 있는 후기는 삭제할 수 없습니다.");
        }
        reportRepository.deleteByReviewNo(review.getId());
        statusHistoryRepository.deleteByReviewNo(review.getId());
        reviewRepository.delete(review);
    }

    private void requireProduct(long productNo) {
        if (productNo <= 0 || productRepository.getFrontCatalogProduct(productNo).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다.");
        }
    }

    private String normalizeContent(String content) {
        return content.trim().replaceAll("\\s+", " ");
    }

    private String normalizeOrderNumber(String orderNumber) {
        return orderNumber.trim().replaceAll("\\s+", " ");
    }

    private String normalizeRequiredText(String value, String fieldName, int maxLength) {
        String normalized = normalizeOptionalText(value, maxLength);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + "을 입력하세요.");
        }
        return normalized;
    }

    private String normalizeOptionalText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "입력값 길이가 올바르지 않습니다.");
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private String reviewerName(Account member) {
        String source = member.getNickname();
        if (source == null || source.isBlank()) {
            source = member.getName();
        }
        if (source == null || source.isBlank()) {
            return "회원";
        }
        return source.substring(0, 1) + "***";
    }

    private FrontMemberProductReviewResponse toMemberReviewResponse(
            FrontProductReview review,
            FrontProductResponse product
    ) {
        if (product == null) {
            return null;
        }
        return new FrontMemberProductReviewResponse(
                review.getId(),
                product.id(),
                product.name(),
                product.brand(),
                product.thumbnailUrl(),
                review.getRating(),
                review.getContent(),
                review.getCrtDtm().toLocalDate().toString()
        );
    }

    private enum FrontProductReviewSort {
        RECENT,
        RATING_DESC,
        RATING_ASC;

        private static FrontProductReviewSort from(String rawSort) {
            if (rawSort == null || rawSort.isBlank()) {
                return RECENT;
            }
            try {
                return valueOf(rawSort.trim().toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "후기 정렬 기준이 올바르지 않습니다.");
            }
        }
    }
}
