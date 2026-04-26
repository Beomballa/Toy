package com.section.admin.product.controller;

import com.section.admin.product.res.OrderDetailResponse;
import com.section.admin.product.res.OrderListResponse;
import com.section.admin.product.service.AdminOrderService;
import com.section.common.commerce.dto.OrderListReqDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public ResponseEntity<Void> updateStatus(@RequestBody Map<String, Object> params) {
        Long orderNo = Long.parseLong(params.get("orderNo").toString());
        String status = (String) params.get("status");
        adminOrderService.updateOrderStatus(orderNo, status);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/delivery")
    public ResponseEntity<Void> startDelivery(@RequestBody Map<String, String> params) {
        Long orderNo = Long.parseLong(params.get("orderNo"));
        String company = params.get("deliveryCompany");
        String trackingNum = params.get("trackingNum");
        adminOrderService.startDelivery(orderNo, company, trackingNum);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/delivery-complete")
    public ResponseEntity<Void> completeDelivery(@RequestBody Map<String, Long> params) {
        adminOrderService.completeDelivery(params.get("orderNo"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelOrder(@RequestBody Map<String, Long> params) {
        adminOrderService.cancelOrder(params.get("orderNo"));
        return ResponseEntity.ok().build();
    }
}
