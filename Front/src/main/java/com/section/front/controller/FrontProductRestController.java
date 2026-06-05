package com.section.front.controller;

import com.section.front.product.dto.FrontProductResponse;
import com.section.front.product.service.FrontProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/front/products")
public class FrontProductRestController {

    private final FrontProductCatalogService frontProductCatalogService;

    @GetMapping
    public List<FrontProductResponse> getProducts() {
        return frontProductCatalogService.getCatalog();
    }
}
