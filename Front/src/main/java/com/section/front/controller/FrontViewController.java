package com.section.front.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class FrontViewController {

    @GetMapping({"", "/", "/front"})
    public String index() {
        return "views/index";
    }

    @GetMapping("/front/products/{productId}")
    public String productDetail(@PathVariable long productId, Model model) {
        model.addAttribute("productId", productId);
        return "views/product-detail";
    }
}
