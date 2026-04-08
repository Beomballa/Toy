package com.section.admin.product.controller;

import com.section.admin.product.res.OrderListResponse;
import com.section.admin.product.service.AdminOrderService;
import com.section.common.commerce.dto.OrderListReqDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
