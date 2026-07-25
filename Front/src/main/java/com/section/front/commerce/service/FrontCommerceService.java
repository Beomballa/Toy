package com.section.front.commerce.service;

import com.section.common.commerce.entity.FrontCart;
import com.section.common.commerce.entity.FrontCartItem;
import com.section.common.commerce.entity.OrderDelivery;
import com.section.common.commerce.entity.OrderItem;
import com.section.common.commerce.entity.Orders;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.entity.ProductOption;
import com.section.common.commerce.repository.FrontCartItemRepository;
import com.section.common.commerce.repository.FrontCartRepository;
import com.section.common.commerce.repository.OrderDeliveryRepository;
import com.section.common.commerce.repository.OrderItemRepository;
import com.section.common.commerce.repository.OrderRepository;
import com.section.common.commerce.repository.ProductOptionRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.front.commerce.dto.FrontCartItemRequest;
import com.section.front.commerce.dto.FrontCartItemResponse;
import com.section.front.commerce.dto.FrontCartResponse;
import com.section.front.commerce.dto.FrontOrderCreateRequest;
import com.section.front.commerce.dto.FrontOrderCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FrontCommerceService {

    private static final int MAX_ITEM_QUANTITY = 20;
    private static final String ACTIVE = "ACTIVE";

    private final FrontCartRepository cartRepository;
    private final FrontCartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderDeliveryRepository orderDeliveryRepository;

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

        FrontCart cart = cartRepository.findByCartTokenAndStatus(cartToken, ACTIVE)
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
        FrontCart cart = requireCart(cartToken);
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
        FrontCart cart = requireCart(cartToken);
        FrontCartItem item = cartItemRepository.findByIdAndCartNo(itemId, cart.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "장바구니 상품을 찾을 수 없습니다."));
        cartItemRepository.delete(item);
        return toCartResponse(cart);
    }

    @Transactional
    public FrontOrderCreateResponse createOrder(String cartToken, FrontOrderCreateRequest request) {
        validateToken(cartToken);
        validateOrderRequest(request);
        FrontCart cart = requireCart(cartToken);
        List<FrontCartItem> cartItems = cartItemRepository.findAllByCartNoOrderByIdDesc(cart.getId());
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("장바구니가 비어 있습니다.");
        }

        List<CheckoutLine> lines = cartItems.stream().map(item -> {
            Product product = requireProduct(item.getProductNo());
            ProductOption option = productOptionRepository.findByIdForUpdate(item.getOptionNo())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품 옵션을 찾을 수 없습니다."));
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
                totalAmount
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
        cart.complete();
        return new FrontOrderCreateResponse(order.getId(), orderNumber, totalAmount, order.getStatus());
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
                            product.getThumbnailUrl(),
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

    private FrontCart requireCart(String cartToken) {
        return cartRepository.findByCartTokenAndStatus(cartToken, ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "장바구니를 찾을 수 없습니다."));
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
        if (request == null
                || isBlank(request.buyerName())
                || isBlank(request.buyerPhone())
                || isBlank(request.recipientName())
                || isBlank(request.recipientPhone())
                || isBlank(request.postalCode())
                || isBlank(request.address1())) {
            throw new IllegalArgumentException("주문자와 배송지 필수 정보를 입력해주세요.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private int safePrice(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
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
