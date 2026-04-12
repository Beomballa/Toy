package com.section.admin.product.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    @GetMapping("")
    public String categoryList() {
        return "views/category-list";
    }
}
