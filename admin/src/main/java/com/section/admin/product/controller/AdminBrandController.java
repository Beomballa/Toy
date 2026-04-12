package com.section.admin.product.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/brands")
public class AdminBrandController {

    @GetMapping("")
    public String brandList() {
        return "views/brand-list";
    }
}
