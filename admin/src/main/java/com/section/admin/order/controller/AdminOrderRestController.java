package com.section.admin.order.controller;

import com.section.admin.order.req.OrderDeliveryStartRequest;
import com.section.admin.order.req.OrderActionRequest;
import com.section.admin.order.req.OrderHistoryListRequest;
import com.section.admin.order.req.OrderMemoSaveRequest;
import com.section.admin.order.req.OrderNoRequest;
import com.section.admin.order.req.OrderStatusUpdateRequest;
import com.section.admin.order.res.OrderDetailResponse;
import com.section.admin.order.res.OrderHistoryListResponse;
import com.section.admin.order.res.OrderListResponse;
import com.section.admin.order.service.AdminOrderService;
import com.section.admin.settings.service.AdminOperationPolicyService;
import com.section.common.commerce.dto.OrderListReqDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/orders")
public class AdminOrderRestController {
    private final AdminOrderService adminOrderService;
    private final AdminOperationPolicyService adminOperationPolicyService;

    @GetMapping("/list")
    public ResponseEntity<OrderListResponse> getOrderList(
            @ModelAttribute OrderListReqDto reqDto, Pageable pageable
    ) {
        return ResponseEntity.ok(adminOrderService.getOrderList(reqDto, pageable));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportOrderList(@ModelAttribute OrderListReqDto reqDto) {
        adminOperationPolicyService.assertOrderExportAllowed();
        String fileName = "orders-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(adminOrderService.exportOrderListCsv(reqDto));
    }

    @GetMapping("/get")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(@RequestParam("no") Long orderNo) {
        return ResponseEntity.ok(adminOrderService.getOrderDetail(orderNo));
    }

    @GetMapping("/history/list")
    public ResponseEntity<OrderHistoryListResponse> getOrderHistoryList(
            @ModelAttribute OrderHistoryListRequest request,
            Pageable pageable
    ) {
        return ResponseEntity.ok(adminOrderService.getOrderHistoryList(request, pageable));
    }

    @GetMapping("/history/export")
    public ResponseEntity<byte[]> exportOrderHistoryList(@ModelAttribute OrderHistoryListRequest request) {
        adminOperationPolicyService.assertOrderExportAllowed();
        String fileName = "order-history-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(adminOrderService.exportOrderHistoryListCsv(request));
    }

    @PatchMapping("/status")
    public ResponseEntity<Void> updateStatus(@Valid @RequestBody OrderStatusUpdateRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminOrderService.updateOrderStatus(req.orderNo(), req.toOrderStatus(), req.normalizedReason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/delivery")
    public ResponseEntity<Void> startDelivery(@Valid @RequestBody OrderDeliveryStartRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminOrderService.startDelivery(
                req.orderNo(),
                req.normalizedDeliveryCompany(),
                req.normalizedTrackingNum(),
                req.normalizedReason()
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/delivery-complete")
    public ResponseEntity<Void> completeDelivery(@Valid @RequestBody OrderActionRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminOrderService.completeDelivery(req.orderNo(), req.normalizedReason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelOrder(@Valid @RequestBody OrderActionRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminOrderService.cancelOrder(req.orderNo(), req.normalizedReason());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/memo")
    public ResponseEntity<Void> saveAdminMemo(@Valid @RequestBody OrderMemoSaveRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminOrderService.saveAdminMemo(req.orderNo(), req.normalizedAdminMemo());
        return ResponseEntity.ok().build();
    }
}
