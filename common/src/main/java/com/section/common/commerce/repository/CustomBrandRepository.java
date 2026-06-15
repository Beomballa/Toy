package com.section.common.commerce.repository;

import com.section.common.commerce.dto.BrandSummaryDto;
import com.section.common.commerce.entity.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomBrandRepository {
    Page<Brand> getBrandList(String keyword, String isActive, Pageable pageable);

    BrandSummaryDto getBrandSummary(String keyword);
}
