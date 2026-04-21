package com.section.admin.product.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/settings")
public class AdminLogController {

    @GetMapping("/logs")
    public String logList() {
        return "views/admin-logs";
    }
}
