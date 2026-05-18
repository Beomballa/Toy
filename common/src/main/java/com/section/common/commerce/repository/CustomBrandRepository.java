package com.section.common.commerce.repository;

import com.section.common.commerce.entity.Brand;

import java.util.List;

public interface CustomBrandRepository {
    List<Brand> getBrandList(String keyword, String isActive);
}
