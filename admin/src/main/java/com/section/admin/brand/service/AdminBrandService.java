package com.section.admin.brand.service;

import com.section.admin.brand.req.BrandListRequest;
import com.section.admin.brand.req.BrandSaveRequest;
import com.section.admin.brand.res.BrandResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    public List<BrandResponse> getBrandList(BrandListRequest req) {
        return brandRepository.findAll().stream()
                .filter(brand -> req.normalizedKeyword() == null
                        || brand.getNameKo().contains(req.normalizedKeyword())
                        || (brand.getNameEn() != null && brand.getNameEn().contains(req.normalizedKeyword())))
                .filter(brand -> req.normalizedIsActive() == null || req.normalizedIsActive().equalsIgnoreCase(brand.getIsActive()))
                .map(BrandResponse::from)
                .toList();
    }

    public BrandResponse getBrand(Long brandNo) {
        return BrandResponse.from(getBrandEntity(brandNo));
    }

    public Brand getBrandEntity(Long brandNo) {
        return brandRepository.findById(brandNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.BRAND_NOT_FOUND));
    }

    @Transactional
    public void saveBrand(BrandSaveRequest req) {
        if (req.brandNo() != null) {
            Brand brand = getBrandEntity(req.brandNo());
            brand.update(req.nameKo(), req.nameEn(), req.logoUrl(), req.isActive() != null ? req.isActive() : "Y");
        } else {
            brandRepository.save(Brand.builder()
                    .nameKo(req.nameKo())
                    .nameEn(req.nameEn())
                    .logoUrl(req.logoUrl())
                    .isActive(req.isActive() != null ? req.isActive() : "Y")
                    .build());
        }
    }

    @Transactional
    public void deleteBrand(Long brandNo) {
        if (productRepository.findAll().stream().anyMatch(product -> product.getBrandNo().equals(brandNo))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        brandRepository.deleteById(brandNo);
    }

    @Transactional
    public void updateActive(Long brandNo, String isActive) {
        String normalized = isActive == null ? null : isActive.trim().toUpperCase();
        if (!"Y".equals(normalized) && !"N".equals(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Brand brand = getBrandEntity(brandNo);
        brand.update(brand.getNameKo(), brand.getNameEn(), brand.getLogoUrl(), normalized);
    }
}
