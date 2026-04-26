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
            Model model
    ) {
        model.addAttribute("id", id);
        model.addAttribute("boardType", boardType);
        model.addAttribute("boardTypes", Document.BoardType.values());
        return "views/content-edit";
    }
}
