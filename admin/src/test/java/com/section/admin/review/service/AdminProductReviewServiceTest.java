package com.section.admin.review.service;

import com.section.common.commerce.entity.FrontProductReview;
import com.section.common.commerce.entity.FrontProductReviewStatus;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.FrontProductReviewRepository;
import com.section.common.commerce.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AdminProductReviewServiceTest {

    private final FrontProductReviewRepository reviewRepository = mock(FrontProductReviewRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final BrandRepository brandRepository = mock(BrandRepository.class);
    private final AdminProductReviewService service = new AdminProductReviewService(
            reviewRepository,
            productRepository,
            brandRepository
    );

    @Test
    void filtersReviewsByVisibilityStatus() {
        given(reviewRepository.findByStatusOrderByIdDesc(FrontProductReviewStatus.HIDDEN.name(), PageRequest.of(0, 20)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var response = service.getReviews("HIDDEN", 0, 20);

        assertThat(response.totalCount()).isZero();
        verify(reviewRepository).findByStatusOrderByIdDesc(FrontProductReviewStatus.HIDDEN.name(), PageRequest.of(0, 20));
    }

    @Test
    void changesTheRequestedReviewVisibilityStatus() {
        FrontProductReview review = mock(FrontProductReview.class);
        given(reviewRepository.findById(12L)).willReturn(Optional.of(review));

        service.changeStatus(12L, FrontProductReviewStatus.HIDDEN);

        verify(review).changeStatus(FrontProductReviewStatus.HIDDEN);
    }
}
