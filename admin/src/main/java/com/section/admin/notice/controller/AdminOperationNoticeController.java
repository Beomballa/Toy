package com.section.admin.notice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/settings/notices")
public class AdminOperationNoticeController {

    @RequestMapping({"", "/list"})
    public String noticeList() {
        return "views/notice-list";
    }

    @RequestMapping("/history")
    public String noticeHistory() {
        return "views/notice-history";
    }
}
