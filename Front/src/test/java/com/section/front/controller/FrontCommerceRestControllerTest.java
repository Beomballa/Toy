package com.section.front.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.front.commerce.dto.FrontCartResponse;
import com.section.front.commerce.dto.FrontOrderCreateResponse;
import com.section.front.commerce.dto.FrontOrderDetailResponse;
import com.section.front.commerce.dto.FrontMemberOrderItemResponse;
import com.section.front.commerce.dto.FrontMemberOrderListResponse;
import com.section.front.commerce.service.FrontCommerceService;
import com.section.front.commerce.service.FrontOrderLookupRateLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FrontCommerceRestControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final FrontCommerceService commerceService = org.mockito.Mockito.mock(FrontCommerceService.class);
    private final FrontOrderLookupRateLimiter orderLookupRateLimiter =
            org.mockito.Mockito.mock(FrontOrderLookupRateLimiter.class);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FrontCommerceRestController(commerceService, orderLookupRateLimiter))
                .build();
    }

    @Test
    void returnsEmptyCart() throws Exception {
        given(commerceService.getCart("1234567890abcdef")).willReturn(FrontCartResponse.empty());

        mockMvc.perform(get("/api/front/cart")
                        .header("X-Cart-Token", "1234567890abcdef"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void createsOrderFromCart() throws Exception {
        given(commerceService.createOrder(eq("1234567890abcdef"), any(), any()))
                .willReturn(new FrontOrderCreateResponse(7L, "GS202607250001", 139000, "ORDERED"));
        Map<String, String> request = Map.of(
                "buyerName", "홍길동",
                "buyerPhone", "010-1111-2222",
                "recipientName", "홍길동",
                "recipientPhone", "010-1111-2222",
                "postalCode", "06236",
                "address1", "서울시 강남구"
        );

        mockMvc.perform(post("/api/front/orders")
                        .header("X-Cart-Token", "1234567890abcdef")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("GS202607250001"))
                .andExpect(jsonPath("$.totalAmount").value(139000));
    }

    @Test
    void clearsEveryItemFromCart() throws Exception {
        given(commerceService.clearCart("1234567890abcdef")).willReturn(FrontCartResponse.empty());

        mockMvc.perform(delete("/api/front/cart/items")
                        .header("X-Cart-Token", "1234567890abcdef"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(0))
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    @Test
    void returnsOrderWhenOrderNumberAndPhoneMatch() throws Exception {
        given(commerceService.getOrder("GS202607250001", "010-1111-2222"))
                .willReturn(new FrontOrderDetailResponse(
                        "GS202607250001", "홍**", 139000, "ORDERED", "주문 접수", 1,
                        "2026.07.25 16:30", null, null, null, java.util.List.of(), java.util.List.of()
                ));

        Map<String, String> request = Map.of(
                "orderNumber", "GS202607250001",
                "phone", "010-1111-2222"
        );

        mockMvc.perform(post("/api/front/orders/lookup")
                        .with(servletRequest -> {
                            servletRequest.setRemoteAddr("192.0.2.10");
                            return servletRequest;
                        })
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("GS202607250001"))
                .andExpect(jsonPath("$.statusLabel").value("주문 접수"));

        verify(orderLookupRateLimiter).checkAndRecord("192.0.2.10");
    }

    @Test
    void returnsOnlyTheAuthenticatedMembersOrderPage() throws Exception {
        given(commerceService.getMemberOrders(7L, 0)).willReturn(new FrontMemberOrderListResponse(
                java.util.List.of(new FrontMemberOrderItemResponse(
                        "GS20260806120000001A", "테스트 상품", 1, 99000, "PAID", "결제 확인", "2026.08.06 12:00"
                )), 0, 10, 1, 1, false
        ));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("frontMemberNo", 7L);
        session.setAttribute("frontMemberEmail", "member@example.com");

        mockMvc.perform(get("/api/front/member/orders").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].orderNumber").value("GS20260806120000001A"))
                .andExpect(jsonPath("$.items[0].statusLabel").value("결제 확인"));

        verify(commerceService).getMemberOrders(7L, 0);
    }
}
