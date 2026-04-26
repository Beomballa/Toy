package com.section.admin.log.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/settings/logs")
public class AdminLogController {

    @RequestMapping({"", "/list"})
    public String logList() {
        return "views/admin-logs";
    }
}
