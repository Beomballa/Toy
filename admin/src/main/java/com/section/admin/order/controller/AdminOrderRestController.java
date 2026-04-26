package com.section.admin.order.controller;

import com.section.admin.order.req.OrderDeliveryStartRequest;
import com.section.admin.order.req.OrderNoRequest;
import com.section.admin.order.req.OrderStatusUpdateRequest;
import com.section.admin.order.res.OrderDetailResponse;
import com.section.admin.order.res.OrderListResponse;
import com.section.admin.order.service.AdminOrderService;
import com.section.common.commerce.dto.OrderListReqDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/orders")
public class AdminOrderRestController {
    private final AdminOrderService adminOrderService;

    @GetMapping("/list")
    public ResponseEntity<OrderListResponse> getOrderList(
            @ModelAttribute OrderListReqDto reqDto, Pageable pageable
    ) {
        return ResponseEntity.ok(adminOrderService.getOrderList(reqDto, pageable));
    }

    @GetMapping("/get")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(@RequestParam("no") Long orderNo) {
        return ResponseEntity.ok(adminOrderService.getOrderDetail(orderNo));
    }

    @PatchMapping("/status")
    public ResponseEntity<Void> updateStatus(@Valid @RequestBody OrderStatusUpdateRequest req) {
        adminOrderService.updateOrderStatus(req.orderNo(), req.status());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/delivery")
    public ResponseEntity<Void> startDelivery(@Valid @RequestBody OrderDeliveryStartRequest req) {
        adminOrderService.startDelivery(req.orderNo(), req.deliveryCompany(), req.trackingNum());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/delivery-complete")
    public ResponseEntity<Void> completeDelivery(@Valid @RequestBody OrderNoRequest req) {
        adminOrderService.completeDelivery(req.orderNo());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelOrder(@Valid @RequestBody OrderNoRequest req) {
        adminOrderService.cancelOrder(req.orderNo());
        return ResponseEntity.ok().build();
    }
}
