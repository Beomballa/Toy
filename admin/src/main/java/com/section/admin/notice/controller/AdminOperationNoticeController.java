package com.section.admin.notice.controller;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/get")
    public String noticeDetail(@RequestParam("no") Long noticeNo, @RequestParam(value = "returnTo", required = false) String returnTo, Model model) {
        model.addAttribute("noticeNo", noticeNo);
        model.addAttribute("returnTo", returnTo == null || returnTo.isBlank() ? "/admin/settings/notices" : returnTo);
        return "views/notice-get";
    }
}
