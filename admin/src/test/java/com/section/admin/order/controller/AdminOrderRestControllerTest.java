package com.section.admin.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.admin.common.controller.AdminGlobalExceptionHandler;
import com.section.admin.order.service.AdminOrderService;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminOrderRestControllerTest {

    @Mock
    private AdminOrderService adminOrderService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminOrderRestController(adminOrderService))
                .setControllerAdvice(new AdminGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("잘못된 주문 상태값은 400 INVALID_INPUT_VALUE를 반환한다")
    void updateStatusReturnsBadRequestWhenStatusInvalid() throws Exception {
        mockMvc.perform(patch("/api/admin/orders/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StatusPayload(1L, "UNKNOWN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."));
    }

    @Test
    @DisplayName("배송 시작 요청 필수값이 없으면 400 INVALID_INPUT_VALUE를 반환한다")
    void startDeliveryReturnsBadRequestWhenTrackingNumMissing() throws Exception {
        mockMvc.perform(post("/api/admin/orders/delivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeliveryPayload(1L, "CJ대한통운", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."));
    }

    @Test
    @DisplayName("허용되지 않은 주문 상태 전이는 400 ORDER_STATUS_NOT_ALLOWED를 반환한다")
    void completeDeliveryReturnsBadRequestWhenStatusTransitionInvalid() throws Exception {
        doThrow(new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED))
                .when(adminOrderService)
                .completeDelivery(1L);

        mockMvc.perform(post("/api/admin/orders/delivery-complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderNoPayload(1L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("O002"))
                .andExpect(jsonPath("$.message").value("현재 주문 상태에서는 요청한 작업을 수행할 수 없습니다."));
    }

    private record StatusPayload(Long orderNo, String status) {
    }

    private record DeliveryPayload(Long orderNo, String deliveryCompany, String trackingNum) {
    }

    private record OrderNoPayload(Long orderNo) {
    }
}
