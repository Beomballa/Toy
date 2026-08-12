package com.section.front.productreview.service;

import com.section.common.base.entity.type.OrderStatus;
import com.section.common.commerce.entity.FrontProductReview;
import com.section.common.commerce.entity.Orders;
import com.section.common.commerce.repository.FrontProductReviewRepository;
import com.section.common.commerce.repository.OrderItemRepository;
import com.section.common.commerce.repository.OrderRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.common.system.entity.Account;
import com.section.common.system.repository.AccountRepository;
import com.section.front.productreview.dto.FrontProductReviewCreateRequest;
import com.section.front.productreview.dto.FrontMemberProductReviewPageResponse;
import com.section.front.productreview.dto.FrontMemberProductReviewResponse;
import com.section.front.productreview.dto.FrontProductReviewPageResponse;
import com.section.front.productreview.dto.FrontProductReviewResponse;
import com.section.front.product.service.FrontProductCatalogService;
import lombok.RequiredArgsConstructor;
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
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AccountRepository accountRepository;
    private final FrontProductCatalogService productCatalogService;

    @Transactional(readOnly = true)
    public FrontProductReviewPageResponse getReviews(long productNo, int pageNumber) {
        requireProduct(productNo);
        if (pageNumber < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "페이지 번호가 올바르지 않습니다.");
        }

        Page<FrontProductReview> reviews = reviewRepository.findByProductNoOrderByIdDesc(
                productNo,
                PageRequest.of(pageNumber, REVIEW_PAGE_SIZE)
        );
        Object[] summary = reviewRepository.getSummaryByProductNo(productNo);
        long totalCount = ((Number) summary[0]).longValue();
        double averageRating = ((Number) summary[1]).doubleValue();
        return new FrontProductReviewPageResponse(
                reviews.map(FrontProductReviewResponse::from).getContent(),
                totalCount,
                Math.round(averageRating * 10) / 10.0,
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

        FrontProductReview review = FrontProductReview.create(
                memberNo,
                productNo,
                order.getId(),
                reviewerName(member),
                request.rating(),
                normalizeContent(request.content())
        );
        return FrontProductReviewResponse.from(reviewRepository.save(review));
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
        Map<Long, com.section.front.product.dto.FrontProductResponse> products = productCatalogService.findProducts(
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
            com.section.front.product.dto.FrontProductResponse product
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
}
