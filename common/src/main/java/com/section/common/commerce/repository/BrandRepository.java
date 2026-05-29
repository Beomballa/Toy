package com.section.common.commerce.repository;

import com.section.common.commerce.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand,Long>, CustomBrandRepository {
    boolean existsByNameKoIgnoreCase(String nameKo);

    boolean existsByNameKoIgnoreCaseAndBrandNoNot(String nameKo, Long brandNo);

    boolean existsByNameEnIgnoreCase(String nameEn);

    boolean existsByNameEnIgnoreCaseAndBrandNoNot(String nameEn, Long brandNo);

    java.util.List<Brand> findByIsActiveOrderByNameKoAsc(String isActive);
}
