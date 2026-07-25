package com.section.front.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.front.commerce.dto.FrontCartResponse;
import com.section.front.commerce.dto.FrontOrderCreateResponse;
import com.section.front.commerce.service.FrontCommerceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FrontCommerceRestControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final FrontCommerceService commerceService = org.mockito.Mockito.mock(FrontCommerceService.class);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FrontCommerceRestController(commerceService)).build();
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
        given(commerceService.createOrder(eq("1234567890abcdef"), any()))
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
}
