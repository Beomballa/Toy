package com.section.admin.brand.repository;

import com.section.admin.AdminToyApplication;
import com.section.common.commerce.dto.BrandSummaryDto;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.repository.BrandRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = AdminToyApplication.class)
@ActiveProfiles("local")
@Transactional
class BrandRepositorySearchIntegrationTest {

    @Autowired
    private BrandRepository brandRepository;

    @Test
    @DisplayName("브랜드 요약 집계는 키워드 문맥에서 전체, 사용, 중지를 분리한다")
    void getBrandSummarySeparatesActiveBuckets() {
        String keyword = "브랜드요약검증";

        brandRepository.save(Brand.builder()
                .nameKo(keyword + " 나이키")
                .nameEn("SUMMARY-NIKE")
                .isActive("Y")
                .build());
        brandRepository.save(Brand.builder()
                .nameKo(keyword + " 아식스")
                .nameEn("SUMMARY-ASICS")
                .isActive("N")
                .build());

        BrandSummaryDto summary = brandRepository.getBrandSummary(keyword);

        assertEquals(2, summary.totalCount());
        assertEquals(1, summary.activeCount());
        assertEquals(1, summary.inactiveCount());
    }
}
