package com.section.admin.order.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/order")
public class AdminOrderController {
    @RequestMapping("/list")
    public ModelAndView orderList(HttpServletRequest req, Model model){
        return new ModelAndView("views/order-list");
    }
}
