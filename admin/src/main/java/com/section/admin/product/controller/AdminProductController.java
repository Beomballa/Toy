package com.section.admin.product.controller;

import com.section.admin.product.res.ProductDefaultResDto;
import com.section.admin.product.res.ProductDetailResponse;
import com.section.admin.product.service.AdminProductService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ModelAndView productList(HttpServletRequest req, Model model){
        ProductDefaultResDto defaultInfo = adminProductService.getProductDefaultInfo();

        model.addAttribute("brands", defaultInfo.getBrands());
        model.addAttribute("categories", defaultInfo.getCategories());

        return new ModelAndView("views/product-list");
    }

    /**
     * 본인 작성 문서 조회
     * */
    @RequestMapping("/set")
    public String productSet(HttpServletRequest req, Model model){
        // 브랜드, 카테고리 데이터 조회
        ProductDefaultResDto defaultInfo = adminProductService.getProductDefaultInfo();

        model.addAttribute("brands", defaultInfo.getBrands());
        model.addAttribute("categories", defaultInfo.getCategories());
        return "views/product-set";
    }

    /**
     * 본인 작성 문서 조회
     * */
    @RequestMapping("/get")
    public String productGet(@RequestParam("no") String productNo, Model model, HttpServletRequest req){
        ProductDetailResponse response = adminProductService.getProductDetail(Long.parseLong(productNo));

        ProductDefaultResDto defaultInfo = adminProductService.getProductDefaultInfo();

        // 3. 화면으로 데이터 전달
        model.addAttribute("product", response);
        model.addAttribute("brands", defaultInfo.getBrands());
        model.addAttribute("categories", defaultInfo.getCategories());

        return "views/product-get";
    }

    /**
     * 상품 수정 화면 이동
     * */
    @RequestMapping("/update")
    public String productUpdate(@RequestParam("no") String productNo, Model model, HttpServletRequest req){
        ProductDefaultResDto defaultInfo = adminProductService.getProductDefaultInfo();

        model.addAttribute("brands", defaultInfo.getBrands());
        model.addAttribute("categories", defaultInfo.getCategories());

        return "views/product-update";
    }

}
