package com.section.admin.product.res;

public record ProductCreateResponse(
        String resultCode,
        String resultMsg,
        Long productNo
) {
    public static ProductCreateResponse success(Long productNo) {
        return new ProductCreateResponse("200", "success", productNo);
    }
}
