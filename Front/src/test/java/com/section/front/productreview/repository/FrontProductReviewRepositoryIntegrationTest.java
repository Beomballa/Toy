package com.section.front.productreview.repository;

import com.section.common.commerce.entity.FrontProductReviewStatus;
import com.section.common.commerce.repository.FrontProductReviewRepository;
import com.section.front.FrontToyApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FrontToyApplication.class)
@ActiveProfiles("local")
@Transactional(readOnly = true)
class FrontProductReviewRepositoryIntegrationTest {

    @Autowired
    private FrontProductReviewRepository reviewRepository;

    @Test
    void mapsEmptyReviewSummaryToNamedProjection() {
        FrontProductReviewRepository.ReviewSummary summary = reviewRepository.getSummaryByProductNo(
                Long.MAX_VALUE,
                FrontProductReviewStatus.VISIBLE.name()
        );

        assertThat(summary.getTotalCount()).isZero();
        assertThat(summary.getAverageRating()).isZero();
    }
}
