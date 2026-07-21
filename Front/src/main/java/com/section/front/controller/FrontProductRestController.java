package com.section.front.controller;

import com.section.front.product.dto.FrontProductDetailResponse;
import com.section.front.product.dto.FrontProductResponse;
import com.section.front.product.req.FrontCatalogRequest;
import com.section.front.product.service.FrontProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/front/products")
public class FrontProductRestController {

    private final FrontProductCatalogService frontProductCatalogService;

    @GetMapping
    public List<FrontProductResponse> getProducts(@ModelAttribute FrontCatalogRequest request) {
        return frontProductCatalogService.getCatalog(request.toQuery());
    }

    @GetMapping("/{productId}")
    public FrontProductDetailResponse getProduct(@PathVariable long productId) {
        validateProductId(productId);
        return frontProductCatalogService.findProductDetail(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void validateProductId(long productId) {
        if (productId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "상품 ID는 양수여야 합니다.");
        }
    }
}
