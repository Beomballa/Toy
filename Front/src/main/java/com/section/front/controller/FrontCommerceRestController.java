package com.section.front.controller;

import com.section.front.commerce.dto.FrontCartItemRequest;
import com.section.front.commerce.dto.FrontCartQuantityRequest;
import com.section.front.commerce.dto.FrontCartResponse;
import com.section.front.commerce.dto.FrontOrderCreateRequest;
import com.section.front.commerce.dto.FrontOrderCreateResponse;
import com.section.front.commerce.dto.FrontOrderDetailResponse;
import com.section.front.commerce.dto.FrontOrderLookupRequest;
import com.section.front.commerce.service.FrontCommerceService;
import com.section.front.commerce.service.FrontOrderLookupRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/front")
public class FrontCommerceRestController {

    private static final String CART_TOKEN_HEADER = "X-Cart-Token";

    private final FrontCommerceService commerceService;
    private final FrontOrderLookupRateLimiter orderLookupRateLimiter;

    @GetMapping("/cart")
    public FrontCartResponse getCart(@RequestHeader(CART_TOKEN_HEADER) String cartToken) {
        return commerceService.getCart(cartToken);
    }

    @PostMapping("/cart/items")
    public FrontCartResponse addItem(
            @RequestHeader(CART_TOKEN_HEADER) String cartToken,
            @RequestBody FrontCartItemRequest request
    ) {
        return commerceService.addItem(cartToken, request);
    }

    @PatchMapping("/cart/items/{itemId}")
    public FrontCartResponse changeQuantity(
            @RequestHeader(CART_TOKEN_HEADER) String cartToken,
            @PathVariable long itemId,
            @RequestBody FrontCartQuantityRequest request
    ) {
        return commerceService.changeQuantity(cartToken, itemId, request.quantity());
    }

    @DeleteMapping("/cart/items/{itemId}")
    public FrontCartResponse removeItem(
            @RequestHeader(CART_TOKEN_HEADER) String cartToken,
            @PathVariable long itemId
    ) {
        return commerceService.removeItem(cartToken, itemId);
    }

    @DeleteMapping("/cart/items")
    public FrontCartResponse clearCart(@RequestHeader(CART_TOKEN_HEADER) String cartToken) {
        return commerceService.clearCart(cartToken);
    }

    @PostMapping("/orders")
    public FrontOrderCreateResponse createOrder(
            @RequestHeader(CART_TOKEN_HEADER) String cartToken,
            @RequestBody FrontOrderCreateRequest request
    ) {
        return commerceService.createOrder(cartToken, request);
    }

    @PostMapping("/orders/lookup")
    public FrontOrderDetailResponse getOrder(
            @RequestBody FrontOrderLookupRequest request,
            HttpServletRequest httpRequest
    ) {
        orderLookupRateLimiter.checkAndRecord(httpRequest.getRemoteAddr());
        if (request == null) {
            throw new IllegalArgumentException("주문 조회 정보가 올바르지 않습니다.");
        }
        return commerceService.getOrder(request.orderNumber(), request.phone());
    }
}
