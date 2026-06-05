package com.section.front.controller;

import com.section.front.product.dto.FrontCatalogBootstrapResponse;
import com.section.front.product.service.FrontProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/front/catalog")
public class FrontCatalogRestController {

    private final FrontProductCatalogService frontProductCatalogService;

    @GetMapping("/bootstrap")
    public FrontCatalogBootstrapResponse getBootstrap() {
        return frontProductCatalogService.getBootstrap();
    }
}
