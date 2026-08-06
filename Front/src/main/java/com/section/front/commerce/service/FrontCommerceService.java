package com.section.front.commerce.service;

import com.section.common.commerce.entity.FrontCart;
import com.section.common.commerce.entity.FrontCartItem;
import com.section.common.commerce.entity.OrderDelivery;
import com.section.common.commerce.entity.OrderItem;
import com.section.common.commerce.entity.OrderStatusHistory;
import com.section.common.commerce.entity.Orders;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.entity.ProductOption;
import com.section.common.commerce.repository.FrontCartItemRepository;
import com.section.common.commerce.repository.FrontCartRepository;
import com.section.common.commerce.repository.OrderDeliveryRepository;
import com.section.common.commerce.repository.OrderItemRepository;
import com.section.common.commerce.repository.OrderRepository;
import com.section.common.commerce.repository.OrderStatusHistoryRepository;
import com.section.common.commerce.repository.ProductOptionRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.common.system.entity.Account;
import com.section.common.system.repository.AccountRepository;
import com.section.front.commerce.dto.FrontCartItemRequest;
import com.section.front.commerce.dto.FrontCartItemResponse;
import com.section.front.commerce.dto.FrontCartResponse;
import com.section.front.commerce.dto.FrontOrderCreateRequest;
import com.section.front.commerce.dto.FrontOrderCreateResponse;
import com.section.front.commerce.dto.FrontOrderDeliveryResponse;
import com.section.front.commerce.dto.FrontOrderDetailResponse;
import com.section.front.commerce.dto.FrontOrderItemResponse;
import com.section.front.commerce.dto.FrontMemberOrderItemResponse;
import com.section.front.commerce.dto.FrontMemberOrderListResponse;
import com.section.front.commerce.dto.FrontMemberOrderCancelRequest;
import com.section.front.commerce.dto.FrontOrderStatusEventResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FrontCommerceService {

    private static final int MAX_ITEM_QUANTITY = 20;
    private static final String ACTIVE = "ACTIVE";
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_PHONE_LENGTH = 20;
    private static final int MAX_POSTAL_CODE_LENGTH = 10;
    private static final int MAX_ADDRESS_LENGTH = 200;
    private static final int MEMBER_ORDER_PAGE_SIZE = 10;

    private final FrontCartRepository cartRepository;
    private final FrontCartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderDeliveryRepository orderDeliveryRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public FrontCartResponse getCart(String cartToken) {
        validateToken(cartToken);
        return cartRepository.findByCartTokenAndStatus(cartToken, ACTIVE)
                .map(this::toCartResponse)
                .orElseGet(FrontCartResponse::empty);
    }

    @Transactional
    public FrontCartResponse addItem(String cartToken, FrontCartItemRequest request) {
        validateToken(cartToken);
        validateQuantity(request.quantity());
        Product product = requireProduct(request.productId());
        ProductOption option = requireOption(request.optionId());
        validateOption(product, option);

        FrontCart cart = cartRepository.findByCartTokenForUpdate(cartToken)
                .map(existing -> reopenCart(existing))
                .orElseGet(() -> cartRepository.save(FrontCart.create(cartToken)));
        FrontCartItem item = cartItemRepository
                .findByCartNoAndProductNoAndOptionNo(cart.getId(), product.getId(), option.getId())
                .orElseGet(() -> FrontCartItem.create(cart.getId(), product.getId(), option.getId(), 0));
        int nextQuantity = item.getQuantity() + request.quantity();
        validatePurchasableQuantity(nextQuantity, option.getStockCnt());
        item.changeQuantity(nextQuantity);
        cartItemRepository.save(item);
        return toCartResponse(cart);
    }

    @Transactional
    public FrontCartResponse changeQuantity(String cartToken, long itemId, int quantity) {
        validateToken(cartToken);
        validateQuantity(quantity);
        FrontCart cart = requireActiveCartForUpdate(cartToken);
        FrontCartItem item = cartItemRepository.findByIdAndCartNo(itemId, cart.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "장바구니 상품을 찾을 수 없습니다."));
        ProductOption option = requireOption(item.getOptionNo());
        validatePurchasableQuantity(quantity, option.getStockCnt());
        item.changeQuantity(quantity);
        return toCartResponse(cart);
    }

    @Transactional
    public FrontCartResponse removeItem(String cartToken, long itemId) {
        validateToken(cartToken);
        FrontCart cart = requireActiveCartForUpdate(cartToken);
        FrontCartItem item = cartItemRepository.findByIdAndCartNo(itemId, cart.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "장바구니 상품을 찾을 수 없습니다."));
        cartItemRepository.delete(item);
        return toCartResponse(cart);
    }

    @Transactional
    public FrontCartResponse clearCart(String cartToken) {
        validateToken(cartToken);
        FrontCart cart = requireActiveCartForUpdate(cartToken);
        cartItemRepository.deleteAllByCartNo(cart.getId());
        return FrontCartResponse.empty();
    }

    @Transactional
    public FrontOrderCreateResponse createOrder(String cartToken, FrontOrderCreateRequest request) {
        return createOrder(cartToken, request, null);
    }

    @Transactional
    public FrontOrderCreateResponse createOrder(String cartToken, FrontOrderCreateRequest request, Long memberNo) {
        validateToken(cartToken);
        validateOrderRequest(request);
        if (memberNo != null) {
            requireAvailableMember(memberNo);
        }
        FrontCart cart = requireCartForCheckout(cartToken);
        List<FrontCartItem> cartItems = cartItemRepository.findAllByCartNoOrderByIdDesc(cart.getId());
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("장바구니가 비어 있습니다.");
        }

        List<Long> productIds = cartItems.stream()
                .map(FrontCartItem::getProductNo)
                .distinct()
                .sorted()
                .toList();
        Map<Long, Product> products = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<Long> optionIds = cartItems.stream()
                .map(FrontCartItem::getOptionNo)
                .distinct()
                .sorted()
                .toList();
        Map<Long, ProductOption> options = productOptionRepository.findAllByIdForUpdate(optionIds).stream()
                .collect(Collectors.toMap(ProductOption::getId, Function.identity()));

        List<CheckoutLine> lines = cartItems.stream().map(item -> {
            Product product = products.get(item.getProductNo());
            if (product == null || !product.isActive()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "판매 중인 상품을 찾을 수 없습니다.");
            }
            ProductOption option = options.get(item.getOptionNo());
            if (option == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품 옵션을 찾을 수 없습니다.");
            }
            validateOption(product, option);
            validatePurchasableQuantity(item.getQuantity(), option.getStockCnt());
            int unitPrice = safePrice(product.getReleasePrice()) + safePrice(option.getAdditionalPrice());
            return new CheckoutLine(item, product, option, unitPrice);
        }).toList();

        int totalAmount = Math.toIntExact(lines.stream()
                .mapToLong(line -> (long) line.unitPrice() * line.item().getQuantity())
                .sum());
        String orderNumber = createOrderNumber();
        Orders order = orderRepository.save(Orders.createOrder(
                orderNumber,
                request.buyerName().trim(),
                request.buyerPhone().trim(),
                totalAmount,
                memberNo
        ));

        for (CheckoutLine line : lines) {
            line.option().removeStock(line.item().getQuantity());
            orderItemRepository.save(OrderItem.create(
                    order.getId(),
                    line.product().getId(),
                    line.option().getId(),
                    line.product().getNameKo() + " / " + line.option().getOptionName(),
                    line.unitPrice(),
                    line.item().getQuantity()
            ));
        }
        orderDeliveryRepository.save(OrderDelivery.create(
                order.getId(),
                request.recipientName().trim(),
                request.recipientPhone().trim(),
                request.postalCode().trim(),
                request.address1().trim(),
                trimToNull(request.address2()),
                trimToNull(request.deliveryRequest())
        ));
        cartItemRepository.deleteAllByCartNo(cart.getId());
        cart.complete();
        return new FrontOrderCreateResponse(order.getId(), orderNumber, totalAmount, order.getStatus());
    }

    @Transactional(readOnly = true)
    public FrontOrderDetailResponse getOrder(String orderNumber, String buyerPhone) {
        if (orderNumber == null || !orderNumber.matches("GS[A-Z0-9]{10,40}")) {
            throw new IllegalArgumentException("주문 조회 정보가 올바르지 않습니다.");
        }
        validatePhone(buyerPhone, "주문자 연락처");
        Orders order = orderRepository.findByOrderNum(orderNumber)
                .filter(candidate -> normalizePhone(candidate.getBuyerPhone()).equals(normalizePhone(buyerPhone)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문 정보를 확인할 수 없습니다."));
        return toOrderDetail(order);
    }

    @Transactional(readOnly = true)
    public FrontMemberOrderListResponse getMemberOrders(long memberNo, int page) {
        requireAvailableMember(memberNo);
        if (page < 0) {
            throw new IllegalArgumentException("페이지 정보가 올바르지 않습니다.");
        }
        Page<Orders> orders = orderRepository.findByMemberNoOrderByIdDesc(memberNo, PageRequest.of(page, MEMBER_ORDER_PAGE_SIZE));
        List<OrderItem> orderItems = orders.isEmpty()
                ? List.of()
                : orderItemRepository.findAllByOrderNoInOrderByOrderNoAscIdAsc(
                        orders.getContent().stream().map(Orders::getId).toList()
                );
        Map<Long, List<OrderItem>> itemsByOrder = orderItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderNo));
        List<FrontMemberOrderItemResponse> items = orders.getContent().stream()
                .map(order -> {
                    List<OrderItem> itemsForOrder = itemsByOrder.getOrDefault(order.getId(), List.of());
                    String productName = itemsForOrder.isEmpty() ? "주문 상품" : itemsForOrder.get(0).getProductName();
                    return new FrontMemberOrderItemResponse(
                            order.getOrderNum(), productName, itemsForOrder.size(), order.getTotalAmount(),
                            order.getStatus(), statusLabel(order.getStatus()), formatDateTime(order.getCrtDtm())
                    );
                })
                .toList();
        return new FrontMemberOrderListResponse(
                items, orders.getNumber(), orders.getSize(), orders.getTotalPages(), orders.getTotalElements(), orders.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public FrontOrderDetailResponse getMemberOrder(long memberNo, String orderNumber) {
        requireAvailableMember(memberNo);
        if (orderNumber == null || !orderNumber.matches("GS[A-Z0-9]{10,40}")) {
            throw new IllegalArgumentException("주문 조회 정보가 올바르지 않습니다.");
        }
        Orders order = orderRepository.findByOrderNumAndMemberNo(orderNumber, memberNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문 정보를 확인할 수 없습니다."));
        return toOrderDetail(order);
    }

    @Transactional
    public FrontOrderDetailResponse cancelMemberOrder(
            long memberNo,
            String orderNumber,
            FrontMemberOrderCancelRequest request
    ) {
        requireAvailableMember(memberNo);
        if (orderNumber == null || !orderNumber.matches("GS[A-Z0-9]{10,40}")) {
            throw new IllegalArgumentException("주문 조회 정보가 올바르지 않습니다.");
        }
        Orders requestedOrder = orderRepository.findByOrderNumAndMemberNo(orderNumber, memberNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문 정보를 확인할 수 없습니다."));
        Orders order = orderRepository.findByIdForUpdate(requestedOrder.getId())
                .filter(candidate -> Long.valueOf(memberNo).equals(candidate.getMemberNo()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문 정보를 확인할 수 없습니다."));
        String beforeStatus = order.getStatus();
        order.cancel();
        restoreOrderStock(order.getId());
        orderStatusHistoryRepository.save(OrderStatusHistory.create(
                order.getId(), "CUSTOMER_CANCEL", beforeStatus, order.getStatus(),
                normalizeCancelReason(request == null ? null : request.reason()), null, null, null
        ));
        return toOrderDetail(order);
    }

    private FrontOrderDetailResponse toOrderDetail(Orders order) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderNo(order.getId());
        Map<Long, Product> orderProducts = productRepository.findAllById(
                        orderItems.stream().map(OrderItem::getProductNo).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<FrontOrderItemResponse> items = orderItems.stream()
                .map(item -> new FrontOrderItemResponse(
                        item.getProductNo(),
                        item.getProductName(),
                        orderProducts.containsKey(item.getProductNo())
                                ? safeThumbnailUrl(orderProducts.get(item.getProductNo()).getThumbnailUrl())
                                : null,
                        item.getOrderPrice(),
                        item.getCount(),
                        Math.multiplyExact(item.getOrderPrice(), item.getCount())
                ))
                .toList();
        FrontOrderDeliveryResponse delivery = orderDeliveryRepository.findByOrderNo(order.getId())
                .map(value -> new FrontOrderDeliveryResponse(
                        value.getRecipientName(),
                        maskPhone(value.getRecipientPhone()),
                        value.getPostalCode(),
                        value.getAddress1(),
                        value.getAddress2(),
                        value.getDeliveryRequest()
                ))
                .orElse(null);
        List<FrontOrderStatusEventResponse> persistedHistory = orderStatusHistoryRepository
                .findTop20ByOrderNoOrderByCrtDtmDescIdDesc(order.getId()).stream()
                .map(value -> new FrontOrderStatusEventResponse(
                        value.getAfterStatus(),
                        statusLabel(value.getAfterStatus()),
                        formatDateTime(value.getCrtDtm())
                ))
                .toList();
        List<FrontOrderStatusEventResponse> history = persistedHistory.isEmpty()
                ? List.of(new FrontOrderStatusEventResponse(
                        order.getStatus(),
                        statusLabel(order.getStatus()),
                        formatDateTime(order.getCrtDtm())
                ))
                : persistedHistory;
        return new FrontOrderDetailResponse(
                order.getOrderNum(),
                maskName(order.getBuyerName()),
                order.getTotalAmount(),
                order.getStatus(),
                statusLabel(order.getStatus()),
                statusStep(order.getStatus()),
                formatDateTime(order.getCrtDtm()),
                order.getDeliveryCompany(),
                order.getTrackingNum(),
                delivery,
                items,
                history
        );
    }

    private void requireAvailableMember(long memberNo) {
        Account account = accountRepository.findById(memberNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보를 확인할 수 없습니다."));
        if (!account.isAvailableCustomer()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용할 수 없는 회원입니다.");
        }
    }

    private void restoreOrderStock(long orderNo) {
        Map<Long, Integer> quantities = orderItemRepository.findByOrderNo(orderNo).stream()
                .filter(item -> item.getOptionNo() != null)
                .collect(Collectors.toMap(
                        OrderItem::getOptionNo,
                        OrderItem::getCount,
                        Integer::sum,
                        LinkedHashMap::new
                ));
        if (quantities.isEmpty()) {
            return;
        }
        List<Long> optionIds = quantities.keySet().stream().sorted().toList();
        Map<Long, ProductOption> options = productOptionRepository.findAllByIdForUpdate(optionIds).stream()
                .collect(Collectors.toMap(ProductOption::getId, Function.identity()));
        if (options.size() != optionIds.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "주문 옵션 정보를 확인할 수 없습니다.");
        }
        quantities.forEach((optionId, quantity) -> options.get(optionId).addStock(quantity));
    }

    private String normalizeCancelReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "고객 요청 취소";
        }
        String normalized = reason.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 200) {
            throw new IllegalArgumentException("취소 사유는 200자 이하여야 합니다.");
        }
        return normalized;
    }

    private FrontCartResponse toCartResponse(FrontCart cart) {
        List<FrontCartItem> items = cartItemRepository.findAllByCartNoOrderByIdDesc(cart.getId());
        if (items.isEmpty()) {
            return FrontCartResponse.empty();
        }
        Map<Long, Product> products = productRepository.findAllById(
                        items.stream().map(FrontCartItem::getProductNo).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, ProductOption> options = productOptionRepository.findAllById(
                        items.stream().map(FrontCartItem::getOptionNo).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(ProductOption::getId, Function.identity()));

        List<FrontCartItemResponse> responses = items.stream()
                .filter(item -> products.containsKey(item.getProductNo()) && options.containsKey(item.getOptionNo()))
                .map(item -> {
                    Product product = products.get(item.getProductNo());
                    ProductOption option = options.get(item.getOptionNo());
                    int unitPrice = safePrice(product.getReleasePrice()) + safePrice(option.getAdditionalPrice());
                    return new FrontCartItemResponse(
                            item.getId(),
                            product.getId(),
                            option.getId(),
                            product.getNameKo(),
                            option.getOptionName(),
                            safeThumbnailUrl(product.getThumbnailUrl()),
                            unitPrice,
                            item.getQuantity(),
                            option.getStockCnt(),
                            unitPrice * item.getQuantity()
                    );
                })
                .toList();
        return new FrontCartResponse(
                responses,
                responses.size(),
                responses.stream().mapToInt(FrontCartItemResponse::quantity).sum(),
                responses.stream().mapToInt(FrontCartItemResponse::lineAmount).sum()
        );
    }

    private FrontCart requireCartForCheckout(String cartToken) {
        FrontCart cart = cartRepository.findByCartTokenForUpdate(cartToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "장바구니를 찾을 수 없습니다."));
        if (!ACTIVE.equals(cart.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 주문 처리된 장바구니입니다.");
        }
        return cart;
    }

    private FrontCart requireActiveCartForUpdate(String cartToken) {
        FrontCart cart = cartRepository.findByCartTokenForUpdate(cartToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "장바구니를 찾을 수 없습니다."));
        if (!ACTIVE.equals(cart.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 주문 처리된 장바구니입니다.");
        }
        return cart;
    }

    private FrontCart reopenCart(FrontCart cart) {
        if (!ACTIVE.equals(cart.getStatus())) {
            cartItemRepository.deleteAllByCartNo(cart.getId());
            cart.reopen();
        }
        return cart;
    }

    private Product requireProduct(long productId) {
        return productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "판매 중인 상품을 찾을 수 없습니다."));
    }

    private ProductOption requireOption(long optionId) {
        return productOptionRepository.findById(optionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품 옵션을 찾을 수 없습니다."));
    }

    private void validateOption(Product product, ProductOption option) {
        if (!product.getId().equals(option.getProductNo())) {
            throw new IllegalArgumentException("상품과 옵션 정보가 일치하지 않습니다.");
        }
    }

    private void validateToken(String cartToken) {
        if (cartToken == null || !cartToken.matches("[A-Za-z0-9-]{16,80}")) {
            throw new IllegalArgumentException("장바구니 토큰이 올바르지 않습니다.");
        }
    }

    private void validateQuantity(int quantity) {
        if (quantity < 1 || quantity > MAX_ITEM_QUANTITY) {
            throw new IllegalArgumentException("수량은 1개 이상 20개 이하여야 합니다.");
        }
    }

    private void validatePurchasableQuantity(int quantity, int stock) {
        validateQuantity(quantity);
        if (quantity > stock) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "선택한 옵션의 재고가 부족합니다.");
        }
    }

    private void validateOrderRequest(FrontOrderCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("주문자와 배송지 필수 정보를 입력해주세요.");
        }
        validateRequiredText(request.buyerName(), MAX_NAME_LENGTH, "주문자 이름");
        validatePhone(request.buyerPhone(), "주문자 연락처");
        validateRequiredText(request.recipientName(), MAX_NAME_LENGTH, "받는 분 이름");
        validatePhone(request.recipientPhone(), "받는 분 연락처");
        validateRequiredText(request.postalCode(), MAX_POSTAL_CODE_LENGTH, "우편번호");
        validateRequiredText(request.address1(), MAX_ADDRESS_LENGTH, "기본 주소");
        validateOptionalText(request.address2(), MAX_ADDRESS_LENGTH, "상세 주소");
        validateOptionalText(request.deliveryRequest(), MAX_ADDRESS_LENGTH, "배송 요청사항");
    }

    private void validateRequiredText(String value, int maxLength, String fieldName) {
        if (isBlank(value) || value.trim().length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " 정보가 올바르지 않습니다.");
        }
    }

    private void validateOptionalText(String value, int maxLength, String fieldName) {
        if (value != null && value.trim().length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "은 " + maxLength + "자 이하여야 합니다.");
        }
    }

    private void validatePhone(String value, String fieldName) {
        if (isBlank(value)
                || value.trim().length() > MAX_PHONE_LENGTH
                || !value.trim().matches("[0-9()\\-\\s]+")) {
            throw new IllegalArgumentException(fieldName + " 정보가 올바르지 않습니다.");
        }
        String normalized = normalizePhone(value);
        if (!normalized.matches("\\d{10,11}")) {
            throw new IllegalArgumentException(fieldName + " 정보가 올바르지 않습니다.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String normalizePhone(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }

    private String maskPhone(String value) {
        String normalized = normalizePhone(value);
        if (normalized.length() < 7) {
            return "***";
        }
        return normalized.substring(0, 3) + "-****-" + normalized.substring(normalized.length() - 4);
    }

    private String maskName(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        if (value.length() == 1) {
            return value;
        }
        return value.charAt(0) + "*".repeat(Math.max(1, value.length() - 1));
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));
    }

    private String statusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "ORDERED" -> "주문 접수";
            case "PAID" -> "결제 확인";
            case "PREPARING" -> "배송 준비";
            case "SHIPPED" -> "배송 중";
            case "DELIVERED" -> "배송 완료";
            case "CANCELLED" -> "주문 취소";
            default -> "상태 확인";
        };
    }

    private int statusStep(String status) {
        return switch (status == null ? "" : status) {
            case "ORDERED" -> 1;
            case "PAID" -> 2;
            case "PREPARING" -> 3;
            case "SHIPPED" -> 4;
            case "DELIVERED" -> 5;
            case "CANCELLED" -> 0;
            default -> 0;
        };
    }

    private int safePrice(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private String safeThumbnailUrl(String value) {
        if (value == null || value.isBlank() || value.contains("\"") || value.contains("'")) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.startsWith("/") || trimmed.startsWith("https://") || trimmed.startsWith("http://")
                ? trimmed
                : null;
    }

    private String createOrderNumber() {
        return "GS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private record CheckoutLine(
            FrontCartItem item,
            Product product,
            ProductOption option,
            int unitPrice
    ) {
    }
}
