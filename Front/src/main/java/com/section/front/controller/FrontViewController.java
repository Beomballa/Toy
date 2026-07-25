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

    @GetMapping("/front/collections/{collectionType}")
    public String productCollection(@PathVariable String collectionType, Model model) {
        model.addAttribute("collectionType", collectionType);
        return "views/product-collection";
    }

    @GetMapping("/front/content/{documentId}")
    public String contentDetail(@PathVariable long documentId, Model model) {
        model.addAttribute("documentId", documentId);
        return "views/content-detail";
    }

    @GetMapping("/front/content")
    public String contentList() {
        return "views/content-list";
    }
}
