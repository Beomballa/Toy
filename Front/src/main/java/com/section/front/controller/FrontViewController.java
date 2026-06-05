package com.section.front.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontViewController {

    @GetMapping({"", "/", "/front"})
    public String index() {
        return "views/index";
    }
}
