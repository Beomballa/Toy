package com.section.front.productreview.service;

import com.section.common.base.entity.type.OrderStatus;
import com.section.common.commerce.entity.FrontProductReview;
import com.section.common.commerce.entity.FrontProductReviewStatus;
import com.section.common.commerce.repository.FrontProductReviewRepository.ReviewRatingCount;
import com.section.common.commerce.repository.FrontProductReviewRepository.ReviewSummary;
import com.section.common.commerce.entity.Orders;
import com.section.common.commerce.dto.FrontCatalogProductRow;
import com.section.common.commerce.repository.FrontProductReviewRepository;
import com.section.common.commerce.repository.FrontProductReviewReportRepository;
import com.section.common.commerce.repository.FrontProductReviewStatusHistoryRepository;
import com.section.common.commerce.repository.OrderItemRepository;
import com.section.common.commerce.repository.OrderRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.common.system.entity.Account;
import com.section.common.system.repository.AccountRepository;
import com.section.front.productreview.dto.FrontProductReviewCreateRequest;
import com.section.front.product.service.FrontProductCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class FrontProductReviewServiceTest {

    private final FrontProductReviewRepository reviewRepository = mock(FrontProductReviewRepository.class);
    private final FrontProductReviewReportRepository reportRepository = mock(FrontProductReviewReportRepository.class);
    private final FrontProductReviewStatusHistoryRepository statusHistoryRepository = mock(FrontProductReviewStatusHistoryRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final FrontProductCatalogService productCatalogService = mock(FrontProductCatalogService.class);
    private final FrontProductReviewService service = new FrontProductReviewService(
            reviewRepository,
            reportRepository,
            statusHistoryRepository,
            productRepository,
            orderRepository,
            orderItemRepository,
            accountRepository,
            productCatalogService
    );

    @Test
    void createsReviewForDeliveredOrderItemOwnedByMember() {
        Account member = mock(Account.class);
        Orders order = mock(Orders.class);
        FrontProductReview savedReview = mock(FrontProductReview.class);
        given(productRepository.getFrontCatalogProduct(11L)).willReturn(Optional.of(mock(FrontCatalogProductRow.class)));
        given(accountRepository.findById(7L)).willReturn(Optional.of(member));
        given(member.isAvailableCustomer()).willReturn(true);
        given(member.getNickname()).willReturn("테스터");
        given(orderRepository.findByOrderNumAndMemberNoForUpdate("ORDER-20260812-001", 7L)).willReturn(Optional.of(order));
        given(order.getId()).willReturn(20L);
        given(order.getStatus()).willReturn(OrderStatus.DELIVERED.name());
        given(orderItemRepository.existsByOrderNoAndProductNo(20L, 11L)).willReturn(true);
        given(reviewRepository.existsByMemberNoAndOrderNoAndProductNo(7L, 20L, 11L)).willReturn(false);
        given(savedReview.getId()).willReturn(1L);
        given(savedReview.getReviewerName()).willReturn("테***");
        given(savedReview.getRating()).willReturn(5);
        given(savedReview.getContent()).willReturn("배송이 빠르고 상태가 좋습니다.");
        given(savedReview.getCrtDtm()).willReturn(java.time.LocalDateTime.of(2026, 8, 12, 12, 0));
        given(reviewRepository.saveAndFlush(any(FrontProductReview.class))).willReturn(savedReview);

        var response = service.createReview(7L, 11L, request());

        assertThat(response.reviewerName()).isEqualTo("테***");
        assertThat(response.rating()).isEqualTo(5);
        verify(reviewRepository).saveAndFlush(any(FrontProductReview.class));
    }

    @Test
    void translatesReviewUniqueConstraintViolationToConflict() {
        Account member = mock(Account.class);
        Orders order = mock(Orders.class);
        given(productRepository.getFrontCatalogProduct(11L)).willReturn(Optional.of(mock(FrontCatalogProductRow.class)));
        given(accountRepository.findById(7L)).willReturn(Optional.of(member));
        given(member.isAvailableCustomer()).willReturn(true);
        given(member.getNickname()).willReturn("테스터");
        given(orderRepository.findByOrderNumAndMemberNoForUpdate("ORDER-20260812-001", 7L)).willReturn(Optional.of(order));
        given(order.getId()).willReturn(20L);
        given(order.getStatus()).willReturn(OrderStatus.DELIVERED.name());
        given(orderItemRepository.existsByOrderNoAndProductNo(20L, 11L)).willReturn(true);
        given(reviewRepository.existsByMemberNoAndOrderNoAndProductNo(7L, 20L, 11L)).willReturn(false);
        given(reviewRepository.saveAndFlush(any(FrontProductReview.class))).willThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.createReview(7L, 11L, request()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 작성한 리뷰");
    }

    @Test
    void sortsVisibleReviewsByHighRatingWhenRequested() {
        given(productRepository.getFrontCatalogProduct(11L)).willReturn(Optional.of(mock(FrontCatalogProductRow.class)));
        given(reviewRepository.findByProductNoAndStatusOrderByRatingDescIdDesc(
                11L, FrontProductReviewStatus.VISIBLE.name(), PageRequest.of(0, 10)
        )).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        given(reviewRepository.getSummaryByProductNo(11L, FrontProductReviewStatus.VISIBLE.name()))
                .willReturn(reviewSummary(0L, 0D));
        given(reviewRepository.countByProductNoAndStatusGroupByRating(11L, FrontProductReviewStatus.VISIBLE.name()))
                .willReturn(List.of());

        var response = service.getReviews(11L, 0, "RATING_DESC", null);

        assertThat(response.reviews()).isEmpty();
        verify(reviewRepository).findByProductNoAndStatusOrderByRatingDescIdDesc(
                11L, FrontProductReviewStatus.VISIBLE.name(), PageRequest.of(0, 10)
        );
    }

    @Test
    void returnsFixedRatingDistributionFromVisibleReviewGroups() {
        ReviewRatingCount fiveStar = mock(ReviewRatingCount.class);
        ReviewRatingCount threeStar = mock(ReviewRatingCount.class);
        given(productRepository.getFrontCatalogProduct(11L)).willReturn(Optional.of(mock(FrontCatalogProductRow.class)));
        given(reviewRepository.findByProductNoAndStatusOrderByIdDesc(
                11L, FrontProductReviewStatus.VISIBLE.name(), PageRequest.of(0, 10)
        )).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        given(reviewRepository.getSummaryByProductNo(11L, FrontProductReviewStatus.VISIBLE.name()))
                .willReturn(reviewSummary(5L, 4.6D));
        given(reviewRepository.countByProductNoAndStatusGroupByRating(11L, FrontProductReviewStatus.VISIBLE.name()))
                .willReturn(List.of(fiveStar, threeStar));
        given(fiveStar.getRating()).willReturn(5);
        given(fiveStar.getCount()).willReturn(4L);
        given(threeStar.getRating()).willReturn(3);
        given(threeStar.getCount()).willReturn(1L);

        var response = service.getReviews(11L, 0, "RECENT", null);

        assertThat(response.ratingDistribution()).containsExactly(4L, 0L, 1L, 0L, 0L);
    }

    @Test
    void rejectsUnknownReviewSort() {
        given(productRepository.getFrontCatalogProduct(11L)).willReturn(Optional.of(mock(FrontCatalogProductRow.class)));

        assertThatThrownBy(() -> service.getReviews(11L, 0, "PRICE_ASC", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("정렬 기준");
    }

    @Test
    void marksReviewsReportedByTheCurrentMemberUsingOneBatchQuery() {
        FrontProductReview review = mock(FrontProductReview.class);
        given(productRepository.getFrontCatalogProduct(11L)).willReturn(Optional.of(mock(FrontCatalogProductRow.class)));
        given(reviewRepository.findByProductNoAndStatusOrderByIdDesc(
                11L, FrontProductReviewStatus.VISIBLE.name(), PageRequest.of(0, 10)
        )).willReturn(new PageImpl<>(List.of(review), PageRequest.of(0, 10), 1));
        given(review.getId()).willReturn(31L);
        given(review.getReviewerName()).willReturn("구매자");
        given(review.getRating()).willReturn(5);
        given(review.getContent()).willReturn("후기 내용");
        given(review.getCrtDtm()).willReturn(java.time.LocalDateTime.of(2026, 8, 12, 12, 0));
        given(reviewRepository.getSummaryByProductNo(11L, FrontProductReviewStatus.VISIBLE.name()))
                .willReturn(reviewSummary(1L, 5D));
        given(reviewRepository.countByProductNoAndStatusGroupByRating(11L, FrontProductReviewStatus.VISIBLE.name()))
                .willReturn(List.of());
        given(reportRepository.findReviewNosByMemberNoAndReviewNoIn(7L, List.of(31L))).willReturn(List.of(31L));

        var response = service.getReviews(11L, 0, "RECENT", 7L);

        assertThat(response.reviews()).singleElement().satisfies(item -> assertThat(item.reportedByMe()).isTrue());
        verify(reportRepository).findReviewNosByMemberNoAndReviewNoIn(7L, List.of(31L));
    }

    @Test
    void rejectsReviewBeforeDeliveryIsComplete() {
        Account member = mock(Account.class);
        Orders order = mock(Orders.class);
        given(productRepository.getFrontCatalogProduct(11L)).willReturn(Optional.of(mock(FrontCatalogProductRow.class)));
        given(accountRepository.findById(7L)).willReturn(Optional.of(member));
        given(member.isAvailableCustomer()).willReturn(true);
        given(orderRepository.findByOrderNumAndMemberNoForUpdate("ORDER-20260812-001", 7L)).willReturn(Optional.of(order));
        given(order.getStatus()).willReturn(OrderStatus.SHIPPED.name());

        assertThatThrownBy(() -> service.createReview(7L, 11L, request()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("배송 완료된 상품");
    }

    @Test
    void rejectsProductNotIncludedInTheOrder() {
        Account member = mock(Account.class);
        Orders order = mock(Orders.class);
        given(productRepository.getFrontCatalogProduct(11L)).willReturn(Optional.of(mock(FrontCatalogProductRow.class)));
        given(accountRepository.findById(7L)).willReturn(Optional.of(member));
        given(member.isAvailableCustomer()).willReturn(true);
        given(orderRepository.findByOrderNumAndMemberNoForUpdate("ORDER-20260812-001", 7L)).willReturn(Optional.of(order));
        given(order.getId()).willReturn(20L);
        given(order.getStatus()).willReturn(OrderStatus.DELIVERED.name());
        given(orderItemRepository.existsByOrderNoAndProductNo(20L, 11L)).willReturn(false);

        assertThatThrownBy(() -> service.createReview(7L, 11L, request()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("포함되지 않은 상품");
    }

    @Test
    void rejectsDuplicateReviewForTheSameOrderProduct() {
        Account member = mock(Account.class);
        Orders order = mock(Orders.class);
        given(productRepository.getFrontCatalogProduct(11L)).willReturn(Optional.of(mock(FrontCatalogProductRow.class)));
        given(accountRepository.findById(7L)).willReturn(Optional.of(member));
        given(member.isAvailableCustomer()).willReturn(true);
        given(orderRepository.findByOrderNumAndMemberNoForUpdate("ORDER-20260812-001", 7L)).willReturn(Optional.of(order));
        given(order.getId()).willReturn(20L);
        given(order.getStatus()).willReturn(OrderStatus.DELIVERED.name());
        given(orderItemRepository.existsByOrderNoAndProductNo(20L, 11L)).willReturn(true);
        given(reviewRepository.existsByMemberNoAndOrderNoAndProductNo(7L, 20L, 11L)).willReturn(true);

        assertThatThrownBy(() -> service.createReview(7L, 11L, request()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 작성한 리뷰");
    }

    @Test
    void rejectsContentThatBecomesEmptyAfterWhitespaceNormalization() {
        Account member = mock(Account.class);
        Orders order = mock(Orders.class);
        given(productRepository.getFrontCatalogProduct(11L)).willReturn(Optional.of(mock(FrontCatalogProductRow.class)));
        given(accountRepository.findById(7L)).willReturn(Optional.of(member));
        given(member.isAvailableCustomer()).willReturn(true);
        given(orderRepository.findByOrderNumAndMemberNoForUpdate("ORDER-20260812-001", 7L)).willReturn(Optional.of(order));
        given(order.getId()).willReturn(20L);
        given(order.getStatus()).willReturn(OrderStatus.DELIVERED.name());
        given(orderItemRepository.existsByOrderNoAndProductNo(20L, 11L)).willReturn(true);
        given(reviewRepository.existsByMemberNoAndOrderNoAndProductNo(7L, 20L, 11L)).willReturn(false);

        assertThatThrownBy(() -> service.createReview(7L, 11L, new FrontProductReviewCreateRequest(
                "ORDER-20260812-001", 5, " \n \t "
        )))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("후기 내용을 입력");
    }

    @Test
    void returnsOnlyDeliveredOrdersThatCanStillReceiveAReview() {
        Account member = mock(Account.class);
        Orders order = mock(Orders.class);
        given(productRepository.getFrontCatalogProduct(11L)).willReturn(Optional.of(mock(FrontCatalogProductRow.class)));
        given(accountRepository.findById(7L)).willReturn(Optional.of(member));
        given(member.isAvailableCustomer()).willReturn(true);
        given(order.getOrderNum()).willReturn("ORDER-20260812-001");
        given(orderRepository.findReviewEligibleOrders(7L, 11L, OrderStatus.DELIVERED.name()))
                .willReturn(List.of(order));

        var response = service.getEligibleOrders(7L, 11L);

        assertThat(response).extracting(item -> item.orderNumber()).containsExactly("ORDER-20260812-001");
    }

    @Test
    void deletesOnlyTheMembersOwnReview() {
        Account member = mock(Account.class);
        FrontProductReview review = mock(FrontProductReview.class);
        given(accountRepository.findById(7L)).willReturn(Optional.of(member));
        given(member.isAvailableCustomer()).willReturn(true);
        given(reviewRepository.findByIdAndMemberNoForUpdate(31L, 7L)).willReturn(Optional.of(review));
        given(review.getId()).willReturn(31L);

        service.deleteMemberReview(7L, 31L);

        verify(reportRepository).deleteByReviewNo(31L);
        verify(statusHistoryRepository).deleteByReviewNo(31L);
        verify(reviewRepository).delete(review);
    }

    @Test
    void preventsDeletingReviewThatHasOperationalHistory() {
        Account member = mock(Account.class);
        FrontProductReview review = mock(FrontProductReview.class);
        given(accountRepository.findById(7L)).willReturn(Optional.of(member));
        given(member.isAvailableCustomer()).willReturn(true);
        given(reviewRepository.findByIdAndMemberNoForUpdate(31L, 7L)).willReturn(Optional.of(review));
        given(review.getId()).willReturn(31L);
        given(statusHistoryRepository.existsByReviewNo(31L)).willReturn(true);

        assertThatThrownBy(() -> service.deleteMemberReview(7L, 31L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("운영 처리 이력");
        verify(reportRepository, never()).deleteByReviewNo(31L);
        verify(statusHistoryRepository, never()).deleteByReviewNo(31L);
        verify(reviewRepository, never()).delete(review);
    }

    private FrontProductReviewCreateRequest request() {
        return new FrontProductReviewCreateRequest("ORDER-20260812-001", 5, "배송이 빠르고 상태가 좋습니다.");
    }

    private ReviewSummary reviewSummary(long count, double averageRating) {
        return new ReviewSummary() {
            @Override
            public long getTotalCount() {
                return count;
            }

            @Override
            public Double getAverageRating() {
                return averageRating;
            }
        };
    }
}
