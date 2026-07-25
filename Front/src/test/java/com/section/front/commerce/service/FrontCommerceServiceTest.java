package com.section.front.commerce.service;

import com.section.common.commerce.entity.OrderDelivery;
import com.section.common.commerce.entity.OrderItem;
import com.section.common.commerce.entity.Orders;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.repository.FrontCartItemRepository;
import com.section.common.commerce.repository.FrontCartRepository;
import com.section.common.commerce.repository.OrderDeliveryRepository;
import com.section.common.commerce.repository.OrderItemRepository;
import com.section.common.commerce.repository.OrderRepository;
import com.section.common.commerce.repository.OrderStatusHistoryRepository;
import com.section.common.commerce.repository.ProductOptionRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.front.commerce.dto.FrontOrderDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class FrontCommerceServiceTest {

    private static final String ORDER_NUMBER = "GS20260725162342221ED3";

    private final FrontCartRepository cartRepository = mock(FrontCartRepository.class);
    private final FrontCartItemRepository cartItemRepository = mock(FrontCartItemRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductOptionRepository productOptionRepository = mock(ProductOptionRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
    private final OrderDeliveryRepository orderDeliveryRepository = mock(OrderDeliveryRepository.class);
    private final OrderStatusHistoryRepository orderStatusHistoryRepository = mock(OrderStatusHistoryRepository.class);

    private FrontCommerceService commerceService;

    @BeforeEach
    void setUp() {
        commerceService = new FrontCommerceService(
                cartRepository,
                cartItemRepository,
                productRepository,
                productOptionRepository,
                orderRepository,
                orderItemRepository,
                orderDeliveryRepository,
                orderStatusHistoryRepository
        );
    }

    @Test
    void returnsMaskedOrderDetailAfterPhoneVerification() {
        Orders order = mock(Orders.class);
        OrderItem item = mock(OrderItem.class);
        Product product = mock(Product.class);
        OrderDelivery delivery = mock(OrderDelivery.class);
        LocalDateTime orderedAt = LocalDateTime.of(2026, 7, 25, 16, 23);

        given(order.getId()).willReturn(515L);
        given(order.getOrderNum()).willReturn(ORDER_NUMBER);
        given(order.getBuyerName()).willReturn("테스트주문자");
        given(order.getBuyerPhone()).willReturn("010-0000-0000");
        given(order.getTotalAmount()).willReturn(120000);
        given(order.getStatus()).willReturn("ORDERED");
        given(order.getCrtDtm()).willReturn(orderedAt);
        given(orderRepository.findByOrderNum(ORDER_NUMBER)).willReturn(Optional.of(order));

        given(item.getProductNo()).willReturn(49L);
        given(item.getProductName()).willReturn("노반 메쉬 5 패널 캡 / M-L");
        given(item.getOrderPrice()).willReturn(120000);
        given(item.getCount()).willReturn(1);
        given(orderItemRepository.findByOrderNo(515L)).willReturn(List.of(item));

        given(product.getId()).willReturn(49L);
        given(product.getThumbnailUrl()).willReturn("https://example.com/product.jpg\" alt=\"broken");
        given(productRepository.findAllById(List.of(49L))).willReturn(List.of(product));

        given(delivery.getRecipientName()).willReturn("테스트수령인");
        given(delivery.getRecipientPhone()).willReturn("01012345678");
        given(delivery.getPostalCode()).willReturn("06236");
        given(delivery.getAddress1()).willReturn("서울시 강남구");
        given(orderDeliveryRepository.findByOrderNo(515L)).willReturn(Optional.of(delivery));
        given(orderStatusHistoryRepository.findTop20ByOrderNoOrderByCrtDtmDescIdDesc(515L)).willReturn(List.of());

        FrontOrderDetailResponse response = commerceService.getOrder(ORDER_NUMBER, "01000000000");

        assertThat(response.buyerName()).isEqualTo("테*****");
        assertThat(response.delivery().recipientPhone()).isEqualTo("010-****-5678");
        assertThat(response.statusLabel()).isEqualTo("주문 접수");
        assertThat(response.statusStep()).isEqualTo(1);
        assertThat(response.items()).singleElement().satisfies(orderItem -> {
            assertThat(orderItem.productId()).isEqualTo(49L);
            assertThat(orderItem.lineAmount()).isEqualTo(120000);
            assertThat(orderItem.thumbnailUrl()).isNull();
        });
        assertThat(response.statusHistory()).singleElement()
                .extracting("statusLabel")
                .isEqualTo("주문 접수");
        verify(productRepository).findAllById(List.of(49L));
    }

    @Test
    void rejectsMismatchedPhoneBeforeLoadingPrivateOrderData() {
        Orders order = mock(Orders.class);
        given(order.getBuyerPhone()).willReturn("010-0000-0000");
        given(orderRepository.findByOrderNum(ORDER_NUMBER)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> commerceService.getOrder(ORDER_NUMBER, "010-9999-9999"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");

        verify(orderItemRepository, never()).findByOrderNo(org.mockito.ArgumentMatchers.anyLong());
        verify(orderDeliveryRepository, never()).findByOrderNo(org.mockito.ArgumentMatchers.anyLong());
    }
}
