package com.section.admin.product.controller;

import com.section.admin.product.service.AdminProductService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/product")
public class AdminProductController {

    private final AdminProductService adminProductService;

    /**
     * 본인 작성 문서 조회
     * */
    @RequestMapping("/list")
    public ModelAndView productList(HttpServletRequest req){
        return new ModelAndView("views/product-list");
    }

    /**
     * 본인 작성 문서 조회
     * */
    @RequestMapping("/set")
    public ModelAndView productSet(HttpServletRequest req){
        return new ModelAndView("views/product-set");
    }

}
