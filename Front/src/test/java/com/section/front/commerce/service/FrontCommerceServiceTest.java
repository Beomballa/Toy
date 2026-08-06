package com.section.front.commerce.service;

import com.section.common.commerce.entity.FrontCart;
import com.section.common.commerce.entity.FrontCartItem;
import com.section.common.commerce.entity.OrderDelivery;
import com.section.common.commerce.entity.OrderItem;
import com.section.common.commerce.entity.Orders;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.entity.ProductOption;
import com.section.common.system.entity.Account;
import com.section.common.base.exception.BusinessException;
import com.section.front.commerce.dto.FrontMemberOrderCancelRequest;
import com.section.common.commerce.repository.FrontCartItemRepository;
import com.section.common.commerce.repository.FrontCartRepository;
import com.section.common.commerce.repository.OrderDeliveryRepository;
import com.section.common.commerce.repository.OrderItemRepository;
import com.section.common.commerce.repository.OrderRepository;
import com.section.common.commerce.repository.OrderStatusHistoryRepository;
import com.section.common.commerce.repository.ProductOptionRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.common.system.repository.AccountRepository;
import com.section.front.commerce.dto.FrontCartItemRequest;
import com.section.front.commerce.dto.FrontCartResponse;
import com.section.front.commerce.dto.FrontOrderCreateRequest;
import com.section.front.commerce.dto.FrontOrderDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private final AccountRepository accountRepository = mock(AccountRepository.class);

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
                orderStatusHistoryRepository,
                accountRepository
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

    @Test
    void reopensCompletedCartBeforeAddingANewItem() {
        FrontCart cart = mock(FrontCart.class);
        Product product = mock(Product.class);
        ProductOption option = mock(ProductOption.class);

        given(cart.getId()).willReturn(17L);
        given(cart.getStatus()).willReturn("ORDERED");
        given(product.getId()).willReturn(3L);
        given(product.isActive()).willReturn(true);
        given(productRepository.findById(3L)).willReturn(Optional.of(product));
        given(option.getId()).willReturn(8L);
        given(option.getProductNo()).willReturn(3L);
        given(option.getStockCnt()).willReturn(5);
        given(productOptionRepository.findById(8L)).willReturn(Optional.of(option));
        given(cartRepository.findByCartTokenForUpdate("1234567890abcdef")).willReturn(Optional.of(cart));
        given(cartItemRepository.findByCartNoAndProductNoAndOptionNo(17L, 3L, 8L))
                .willReturn(Optional.empty());
        given(cartItemRepository.findAllByCartNoOrderByIdDesc(17L)).willReturn(List.of());

        commerceService.addItem("1234567890abcdef", new FrontCartItemRequest(3L, 8L, 1));

        verify(cartItemRepository).deleteAllByCartNo(17L);
        verify(cart).reopen();
        verify(cartItemRepository).save(any(FrontCartItem.class));
    }

    @Test
    void rejectsRepeatedCheckoutAfterCartHasAlreadyCompleted() {
        FrontCart cart = mock(FrontCart.class);
        given(cart.getStatus()).willReturn("ORDERED");
        given(cartRepository.findByCartTokenForUpdate("1234567890abcdef")).willReturn(Optional.of(cart));

        assertThatThrownBy(() -> commerceService.createOrder(
                "1234567890abcdef",
                validOrderRequest()
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void locksCheckoutOptionsInAscendingIdOrderAndClearsCompletedCart() {
        FrontCart cart = mock(FrontCart.class);
        FrontCartItem secondItem = mock(FrontCartItem.class);
        FrontCartItem firstItem = mock(FrontCartItem.class);
        Product secondProduct = mock(Product.class);
        Product firstProduct = mock(Product.class);
        ProductOption secondOption = mock(ProductOption.class);
        ProductOption firstOption = mock(ProductOption.class);
        Orders savedOrder = mock(Orders.class);

        given(cart.getId()).willReturn(21L);
        given(cart.getStatus()).willReturn("ACTIVE");
        given(cartRepository.findByCartTokenForUpdate("1234567890abcdef")).willReturn(Optional.of(cart));
        given(secondItem.getProductNo()).willReturn(2L);
        given(secondItem.getOptionNo()).willReturn(20L);
        given(secondItem.getQuantity()).willReturn(1);
        given(firstItem.getProductNo()).willReturn(1L);
        given(firstItem.getOptionNo()).willReturn(10L);
        given(firstItem.getQuantity()).willReturn(2);
        given(cartItemRepository.findAllByCartNoOrderByIdDesc(21L)).willReturn(List.of(secondItem, firstItem));

        configureCheckoutProduct(secondProduct, 2L, "두 번째 상품", 20000);
        configureCheckoutProduct(firstProduct, 1L, "첫 번째 상품", 10000);
        given(productRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(firstProduct, secondProduct));
        configureCheckoutOption(secondOption, 20L, 2L, "L", 5);
        configureCheckoutOption(firstOption, 10L, 1L, "M", 5);
        given(productOptionRepository.findAllByIdForUpdate(List.of(10L, 20L)))
                .willReturn(List.of(firstOption, secondOption));
        given(savedOrder.getId()).willReturn(99L);
        given(savedOrder.getStatus()).willReturn("ORDERED");
        given(orderRepository.save(any(Orders.class))).willReturn(savedOrder);

        commerceService.createOrder("1234567890abcdef", validOrderRequest());

        verify(productOptionRepository).findAllByIdForUpdate(List.of(10L, 20L));
        verify(firstOption).removeStock(2);
        verify(secondOption).removeStock(1);
        verify(cartItemRepository).deleteAllByCartNo(21L);
        verify(cart).complete();
    }

    @Test
    void rejectsOrderFieldsThatExceedDatabaseColumnLength() {
        FrontOrderCreateRequest request = new FrontOrderCreateRequest(
                "가".repeat(51),
                "010-1111-2222",
                "홍길동",
                "010-1111-2222",
                "06236",
                "서울시 강남구",
                null,
                null
        );

        assertThatThrownBy(() -> commerceService.createOrder("1234567890abcdef", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("주문자 이름");

        verify(cartRepository, never()).findByCartTokenForUpdate(any());
    }

    @Test
    void rejectsPhoneNumbersContainingUnexpectedCharacters() {
        assertThatThrownBy(() -> commerceService.getOrder(ORDER_NUMBER, "call-010-1111-2222"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("연락처");

        verify(orderRepository, never()).findByOrderNum(any());
    }

    @Test
    void returnsMemberOrdersWithOneBatchItemQuery() {
        Account account = mock(Account.class);
        Orders firstOrder = mock(Orders.class);
        Orders secondOrder = mock(Orders.class);
        OrderItem firstItem = mock(OrderItem.class);
        OrderItem secondItem = mock(OrderItem.class);
        given(account.isAvailableCustomer()).willReturn(true);
        given(accountRepository.findById(7L)).willReturn(Optional.of(account));
        given(firstOrder.getId()).willReturn(22L);
        given(firstOrder.getOrderNum()).willReturn("GS20260806120000001A");
        given(firstOrder.getStatus()).willReturn("PAID");
        given(firstOrder.getTotalAmount()).willReturn(120000);
        given(firstOrder.getCrtDtm()).willReturn(LocalDateTime.of(2026, 8, 6, 12, 0));
        given(secondOrder.getId()).willReturn(21L);
        given(secondOrder.getOrderNum()).willReturn("GS20260806110000001A");
        given(secondOrder.getStatus()).willReturn("ORDERED");
        given(secondOrder.getTotalAmount()).willReturn(70000);
        given(secondOrder.getCrtDtm()).willReturn(LocalDateTime.of(2026, 8, 6, 11, 0));
        given(orderRepository.findByMemberNoOrderByIdDesc(7L, PageRequest.of(0, 10)))
                .willReturn(new PageImpl<>(List.of(firstOrder, secondOrder), PageRequest.of(0, 10), 2));
        given(firstItem.getOrderNo()).willReturn(22L);
        given(firstItem.getProductName()).willReturn("첫 번째 상품");
        given(secondItem.getOrderNo()).willReturn(21L);
        given(secondItem.getProductName()).willReturn("두 번째 상품");
        given(orderItemRepository.findAllByOrderNoInOrderByOrderNoAscIdAsc(List.of(22L, 21L)))
                .willReturn(List.of(firstItem, secondItem));

        var response = commerceService.getMemberOrders(7L, 0);

        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.items()).extracting("productName").containsExactly("첫 번째 상품", "두 번째 상품");
        verify(orderItemRepository).findAllByOrderNoInOrderByOrderNoAscIdAsc(List.of(22L, 21L));
    }

    @Test
    void rejectsMemberCancellationAfterDeliveryHasStarted() {
        Account account = mock(Account.class);
        Orders order = Orders.createOrder(ORDER_NUMBER, "테스트", "01000000000", 10000, 7L);
        order.pay();
        order.startDelivery("택배사", "12345678");
        ReflectionTestUtils.setField(order, "id", 42L);
        given(account.isAvailableCustomer()).willReturn(true);
        given(accountRepository.findById(7L)).willReturn(Optional.of(account));
        given(orderRepository.findByOrderNumAndMemberNo(ORDER_NUMBER, 7L)).willReturn(Optional.of(order));
        given(orderRepository.findByIdForUpdate(42L)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> commerceService.cancelMemberOrder(
                7L, ORDER_NUMBER, new FrontMemberOrderCancelRequest("일정 변경")
        )).isInstanceOf(BusinessException.class);

        verify(productOptionRepository, never()).findAllByIdForUpdate(any());
        verify(orderStatusHistoryRepository, never()).save(any());
    }

    @Test
    void clearsCartWhileHoldingTheCartLock() {
        FrontCart cart = mock(FrontCart.class);
        given(cart.getId()).willReturn(72L);
        given(cart.getStatus()).willReturn("ACTIVE");
        given(cartRepository.findByCartTokenForUpdate("1234567890abcdef")).willReturn(Optional.of(cart));

        assertThat(commerceService.clearCart("1234567890abcdef"))
                .isEqualTo(FrontCartResponse.empty());

        verify(cartItemRepository).deleteAllByCartNo(72L);
    }

    private void configureCheckoutProduct(Product product, long id, String name, int price) {
        given(product.getId()).willReturn(id);
        given(product.getNameKo()).willReturn(name);
        given(product.getReleasePrice()).willReturn(price);
        given(product.isActive()).willReturn(true);
    }

    private void configureCheckoutOption(
            ProductOption option,
            long id,
            long productId,
            String name,
            int stock
    ) {
        given(option.getId()).willReturn(id);
        given(option.getProductNo()).willReturn(productId);
        given(option.getOptionName()).willReturn(name);
        given(option.getStockCnt()).willReturn(stock);
        given(option.getAdditionalPrice()).willReturn(0);
    }

    private FrontOrderCreateRequest validOrderRequest() {
        return new FrontOrderCreateRequest(
                "홍길동",
                "010-1111-2222",
                "홍길동",
                "010-1111-2222",
                "06236",
                "서울시 강남구",
                "101호",
                "문 앞"
        );
    }
}
