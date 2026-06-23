package com.section.admin.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.admin.common.controller.AdminGlobalExceptionHandler;
import com.section.admin.order.req.OrderHistoryListRequest;
import com.section.admin.order.res.OrderHistoryListResponse;
import com.section.common.commerce.dto.OrderListReqDto;
import com.section.admin.order.service.AdminOrderService;
import com.section.admin.settings.service.AdminOperationPolicyService;
import com.section.common.base.entity.type.OrderStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.hamcrest.Matchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminOrderRestControllerTest {

    @Mock
    private AdminOrderService adminOrderService;

    @Mock
    private AdminOperationPolicyService adminOperationPolicyService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminOrderRestController(adminOrderService, adminOperationPolicyService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new AdminGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("잘못된 주문 상태값은 400 INVALID_INPUT_VALUE를 반환한다")
    void updateStatusReturnsBadRequestWhenStatusInvalid() throws Exception {
        mockMvc.perform(patch("/api/admin/orders/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StatusPayload(1L, "UNKNOWN", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."));
    }

    @Test
    @DisplayName("배송 시작 요청 필수값이 없으면 400 INVALID_INPUT_VALUE를 반환한다")
    void startDeliveryReturnsBadRequestWhenTrackingNumMissing() throws Exception {
        mockMvc.perform(post("/api/admin/orders/delivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeliveryPayload(1L, "CJ대한통운", "", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."));
    }

    @Test
    @DisplayName("허용되지 않은 주문 상태 전이는 400 ORDER_STATUS_NOT_ALLOWED를 반환한다")
    void completeDeliveryReturnsBadRequestWhenStatusTransitionInvalid() throws Exception {
        doThrow(new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED))
                .when(adminOrderService)
                .completeDelivery(1L, null);

        mockMvc.perform(post("/api/admin/orders/delivery-complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderNoPayload(1L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("O002"))
                .andExpect(jsonPath("$.message").value("현재 주문 상태에서는 요청한 작업을 수행할 수 없습니다."));
    }

    @Test
    @DisplayName("일반 주문 상태 변경에서도 허용되지 않은 전이는 400 ORDER_STATUS_NOT_ALLOWED를 반환한다")
    void updateStatusReturnsBadRequestWhenTransitionInvalid() throws Exception {
        doThrow(new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED))
                .when(adminOrderService)
                .updateOrderStatus(1L, OrderStatus.DELIVERED, null);

        mockMvc.perform(patch("/api/admin/orders/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StatusPayload(1L, "DELIVERED", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("O002"))
                .andExpect(jsonPath("$.message").value("현재 주문 상태에서는 요청한 작업을 수행할 수 없습니다."));
    }

    @Test
    @DisplayName("존재하지 않는 주문 상세 조회는 404 ORDER_NOT_FOUND를 반환한다")
    void getOrderDetailReturnsNotFoundWhenOrderMissing() throws Exception {
        when(adminOrderService.getOrderDetail(999L))
                .thenThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        mockMvc.perform(get("/api/admin/orders/get").param("no", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("O001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 주문입니다."));
    }

    @Test
    @DisplayName("주문 목록 엑셀 다운로드는 CSV 파일 응답을 반환한다")
    void exportOrderListReturnsCsvAttachment() throws Exception {
        when(adminOrderService.exportOrderListCsv(org.mockito.ArgumentMatchers.any(OrderListReqDto.class)))
                .thenReturn("주문번호\nORD-1".getBytes());

        mockMvc.perform(get("/api/admin/orders/export")
                        .param("status", "PAID")
                        .param("searchKeyword", "함장님"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", Matchers.containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment; filename=\"orders-")))
                .andExpect(content().bytes("주문번호\nORD-1".getBytes()));

        verify(adminOrderService).exportOrderListCsv(org.mockito.ArgumentMatchers.any(OrderListReqDto.class));
    }

    @Test
    @DisplayName("주문 이력 CSV 다운로드는 현재 필터 기준 파일 응답을 반환한다")
    void exportOrderHistoryListReturnsCsvAttachment() throws Exception {
        when(adminOrderService.exportOrderHistoryListCsv(org.mockito.ArgumentMatchers.any(OrderHistoryListRequest.class)))
                .thenReturn("이력번호\n11".getBytes());

        mockMvc.perform(get("/api/admin/orders/history/export")
                        .param("orderNo", "7")
                        .param("actionType", "DELIVERY_START"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", Matchers.containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment; filename=\"order-history-")))
                .andExpect(content().bytes("이력번호\n11".getBytes()));

        verify(adminOrderService).exportOrderHistoryListCsv(org.mockito.ArgumentMatchers.any(OrderHistoryListRequest.class));
    }

    @Test
    @DisplayName("관리 메모 저장 요청 필수값이 없으면 400 INVALID_INPUT_VALUE를 반환한다")
    void saveAdminMemoReturnsBadRequestWhenOrderNoMissing() throws Exception {
        mockMvc.perform(patch("/api/admin/orders/memo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MemoPayload(null, "메모"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("주문 export 설정이 비활성화되면 400 ADMIN_FEATURE_DISABLED를 반환한다")
    void exportOrderListReturnsBadRequestWhenOrderExportDisabled() throws Exception {
        doThrow(new BusinessException(ErrorCode.ADMIN_FEATURE_DISABLED))
                .when(adminOperationPolicyService)
                .assertOrderExportAllowed();

        mockMvc.perform(get("/api/admin/orders/export"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A002"));
    }

    @Test
    @DisplayName("주문 이력 export 설정이 비활성화되면 400 ADMIN_FEATURE_DISABLED를 반환한다")
    void exportOrderHistoryListReturnsBadRequestWhenOrderExportDisabled() throws Exception {
        doThrow(new BusinessException(ErrorCode.ADMIN_FEATURE_DISABLED))
                .when(adminOperationPolicyService)
                .assertOrderExportAllowed();

        mockMvc.perform(get("/api/admin/orders/history/export"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A002"));
    }

    @Test
    @DisplayName("주문 이력 목록 API는 필터 결과와 메타 정보를 함께 반환한다")
    void getOrderHistoryListReturnsPagedResult() throws Exception {
        when(adminOrderService.getOrderHistoryList(org.mockito.ArgumentMatchers.any(OrderHistoryListRequest.class), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new OrderHistoryListResponse(
                        java.util.List.of(new OrderHistoryListResponse.Item(
                                7L, 3L, "/admin/logs?actionType=ORDER_DELIVERY_START&targetId=3", "활동 로그 보기", "DELIVERY_START", "배송 시작", "배송준비", "배송중", "출고 완료", null, "CJ대한통운", "1234", 1L, "관리자", "2026.05.16 10:00"
                        )),
                        1L,
                        1,
                        0,
                        20,
                        1L,
                        1L,
                        "1-1 / 1건 · 1페이지",
                        new OrderHistoryListResponse.AppliedQuery(3L, "DELIVERY_START", null, 1L, "관리자", null, null, "oldest", "오래된순"),
                        new OrderHistoryListResponse.ResultMeta("검색 결과 1건", "1-1 / 1건 · 1페이지", 4, "1-1 · 주문=3 · 작업=DELIVERY_START · 작업자번호=1 · 작업자=관리자 · 정렬=오래된순")
                ));

        mockMvc.perform(get("/api/admin/orders/history/list?orderNo=3&actionType=DELIVERY_START&actorNo=1&actorKeyword=%EA%B4%80%EB%A6%AC%EC%9E%90&orderType=oldest&page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].historyNo").value(7L))
                .andExpect(jsonPath("$.items[0].activityLogPath").value("/admin/logs?actionType=ORDER_DELIVERY_START&targetId=3"))
                .andExpect(jsonPath("$.items[0].actorName").value("관리자"))
                .andExpect(jsonPath("$.items[0].actionLabel").value("배송 시작"))
                .andExpect(jsonPath("$.totalElements").value(1L))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageInfoLabel").value("1-1 / 1건 · 1페이지"))
                .andExpect(jsonPath("$.appliedQuery.orderNo").value(3L))
                .andExpect(jsonPath("$.appliedQuery.actionType").value("DELIVERY_START"))
                .andExpect(jsonPath("$.appliedQuery.actorNo").value(1L))
                .andExpect(jsonPath("$.appliedQuery.actorKeyword").value("관리자"))
                .andExpect(jsonPath("$.appliedQuery.orderType").value("oldest"))
                .andExpect(jsonPath("$.resultMeta.resultLabel").value("검색 결과 1건"))
                .andExpect(jsonPath("$.resultMeta.filterCount").value(4))
                .andExpect(jsonPath("$.resultMeta.querySignature").value("1-1 · 주문=3 · 작업=DELIVERY_START · 작업자번호=1 · 작업자=관리자 · 정렬=오래된순"));
    }

    private record StatusPayload(Long orderNo, String status, String reason) {
    }

    private record DeliveryPayload(Long orderNo, String deliveryCompany, String trackingNum, String reason) {
    }

    private record OrderNoPayload(Long orderNo) {
    }

    private record MemoPayload(Long orderNo, String adminMemo) {
    }
}
