package com.section.admin.content.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.section.common.content.entity.Document;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/content")
public class AdminContentController {

    @RequestMapping({"", "/list"})
    public String contentList(@RequestParam(value = "boardType", defaultValue = "NOTICE") String boardType, Model model) {
        model.addAttribute("boardType", boardType);
        return "views/content-list";
    }

    @GetMapping("/edit")
    public String contentEdit(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "boardType", defaultValue = "NOTICE") String boardType,
            @RequestParam(value = "returnTo", required = false) String returnTo,
            Model model
    ) {
        model.addAttribute("id", id);
        model.addAttribute("boardType", boardType);
        model.addAttribute("returnTo", returnTo);
        model.addAttribute("boardTypes", Document.BoardType.values());
        model.addAttribute("publishStatuses", Document.PublishStatus.values());
        return "views/content-edit";
    }

    @GetMapping("/get")
    public String contentGet(
            @RequestParam("id") Long id,
            @RequestParam(value = "boardType", defaultValue = "NOTICE") String boardType,
            @RequestParam(value = "returnTo", required = false) String returnTo,
            Model model
    ) {
        model.addAttribute("id", id);
        model.addAttribute("boardType", boardType);
        model.addAttribute("returnTo", returnTo);
        return "views/content-get";
    }
}
