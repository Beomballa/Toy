package com.section.admin.product.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Controller
public class DashBoardController {

    @GetMapping("/")
    public String redirectMain() {
        return "redirect:/product/list";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard() {
        return "views/dashboard";
    }
}
