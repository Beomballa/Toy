package com.section.admin.review.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/reviews")
public class AdminProductReviewController {

    @GetMapping
    public String reviewList() {
        return "views/review-list";
    }
}
