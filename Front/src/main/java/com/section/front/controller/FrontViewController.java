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

    @GetMapping("/front/cart")
    public String cart() {
        return "views/cart";
    }

    @GetMapping("/front/checkout")
    public String checkout() {
        return "views/checkout";
    }

    @GetMapping({"/front/orders", "/front/orders/{orderNumber}"})
    public String orderLookup(@PathVariable(required = false) String orderNumber, Model model) {
        model.addAttribute("orderNumber", orderNumber == null ? "" : orderNumber);
        return "views/order-lookup";
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

    @GetMapping("/front/my")
    public String myActivity() {
        return "views/my-activity";
    }

    @GetMapping("/front/support")
    public String supportCenter() {
        return "views/support-center";
    }

    @GetMapping("/front/brands")
    public String brandDirectory() {
        return "views/brand-directory";
    }

    @GetMapping("/front/compare")
    public String productComparison() {
        return "views/product-comparison";
    }
}
