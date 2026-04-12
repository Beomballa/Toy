package com.section.admin.product.service;

import com.section.admin.product.req.BrandSaveRequest;
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
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 브랜드입니다. ID: " + brandNo));
    }

    @Transactional
    public void saveBrand(BrandSaveRequest req) {
        if (req.brandNo() != null) {
            Brand brand = getBrand(req.brandNo());
            // 엔티티에 update 메서드가 사라졌으므로 직접 필드 수정이 불가능할 수 있음
            // 필요하다면 다시 메서드를 만들되 isActive만 빼야 함
        } else {
            brandRepository.save(Brand.builder()
                    .nameKo(req.nameKo())
                    .nameEn(req.nameEn())
                    .logoUrl(req.logoUrl())
                    .build());
        }
    }

    @Transactional
    public void deleteBrand(Long brandNo) {
        brandRepository.deleteById(brandNo);
    }
}
