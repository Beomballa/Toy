package com.section.admin.brand.service;

import com.section.admin.brand.req.BrandSaveRequest;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBrandService {

    private final BrandRepository brandRepository;

    public List<Brand> getBrandList() {
        return brandRepository.findAll();
    }

    public Brand getBrand(Long brandNo) {
        return brandRepository.findById(brandNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.BRAND_NOT_FOUND));
    }

    @Transactional
    public void saveBrand(BrandSaveRequest req) {
        if (req.brandNo() != null) {
            Brand brand = getBrand(req.brandNo());
            brand.update(req.nameKo(), req.nameEn(), req.logoUrl(), req.isActive());
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
        brandRepository.deleteById(brandNo);
    }
}
