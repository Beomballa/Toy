package com.section.front.productreview.service;

import com.section.common.base.entity.type.OrderStatus;
import com.section.common.commerce.entity.FrontProductReview;
import com.section.common.commerce.entity.Orders;
import com.section.common.commerce.dto.FrontCatalogProductRow;
import com.section.common.commerce.repository.FrontProductReviewRepository;
import com.section.common.commerce.repository.OrderItemRepository;
import com.section.common.commerce.repository.OrderRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.common.system.entity.Account;
import com.section.common.system.repository.AccountRepository;
import com.section.front.productreview.dto.FrontProductReviewCreateRequest;
import com.section.front.product.service.FrontProductCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FrontProductReviewServiceTest {

    private final FrontProductReviewRepository reviewRepository = mock(FrontProductReviewRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final FrontProductCatalogService productCatalogService = mock(FrontProductCatalogService.class);
    private final FrontProductReviewService service = new FrontProductReviewService(
            reviewRepository,
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
        given(reviewRepository.save(any(FrontProductReview.class))).willReturn(savedReview);

        var response = service.createReview(7L, 11L, request());

        assertThat(response.reviewerName()).isEqualTo("테***");
        assertThat(response.rating()).isEqualTo(5);
        verify(reviewRepository).save(any(FrontProductReview.class));
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

    private FrontProductReviewCreateRequest request() {
        return new FrontProductReviewCreateRequest("ORDER-20260812-001", 5, "배송이 빠르고 상태가 좋습니다.");
    }
}
