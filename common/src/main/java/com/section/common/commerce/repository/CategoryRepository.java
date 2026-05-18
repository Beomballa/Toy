package com.section.common.commerce.repository;

import com.section.common.commerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category,Long>, CustomCategoryRepository {
    // depth로 조회
    List<Category> findByDepth(Integer depth);

    // 부모 카테고리로 조회
    List<Category> findByParentNo(Long parentNo);

    // 활성화된 카테고리만 조회
    List<Category> findByIsActive(String isActive);
}
