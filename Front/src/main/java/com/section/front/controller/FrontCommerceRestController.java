package com.section.front.controller;

import com.section.front.commerce.dto.FrontCartItemRequest;
import com.section.front.commerce.dto.FrontCartQuantityRequest;
import com.section.front.commerce.dto.FrontCartResponse;
import com.section.front.commerce.dto.FrontOrderCreateRequest;
import com.section.front.commerce.dto.FrontOrderCreateResponse;
import com.section.front.commerce.dto.FrontOrderDetailResponse;
import com.section.front.commerce.service.FrontCommerceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/front")
public class FrontCommerceRestController {

    private static final String CART_TOKEN_HEADER = "X-Cart-Token";

    private final FrontCommerceService commerceService;

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

    @PostMapping("/orders")
    public FrontOrderCreateResponse createOrder(
            @RequestHeader(CART_TOKEN_HEADER) String cartToken,
            @RequestBody FrontOrderCreateRequest request
    ) {
        return commerceService.createOrder(cartToken, request);
    }

    @GetMapping("/orders/{orderNumber}")
    public FrontOrderDetailResponse getOrder(
            @PathVariable String orderNumber,
            @RequestParam String phone
    ) {
        return commerceService.getOrder(orderNumber, phone);
    }
}
