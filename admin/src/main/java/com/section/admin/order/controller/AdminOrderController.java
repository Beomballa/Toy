package com.section.admin.order.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/orders")
public class AdminOrderController {

    @RequestMapping({"", "/list"})
    public String orderList() {
        return "views/order-list";
    }

    @RequestMapping("/get")
    public String orderDetail() {
        return "views/order-get";
    }
}
