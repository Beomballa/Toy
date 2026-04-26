package com.section.admin.dashboard.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Controller
@RequestMapping("/admin/dashboard")
public class DashBoardController {

    @RequestMapping({"", "/list"})
    public String dashboard() {
        return "views/dashboard";
    }
}
