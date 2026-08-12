package com.section.admin.review.service;

import com.section.common.commerce.entity.FrontProductReview;
import com.section.common.commerce.entity.FrontProductReviewStatus;
import com.section.common.commerce.entity.FrontProductReviewStatusHistory;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.FrontProductReviewRepository;
import com.section.common.commerce.repository.FrontProductReviewReportRepository;
import com.section.common.commerce.repository.FrontProductReviewStatusHistoryRepository;
import com.section.common.commerce.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

class AdminProductReviewServiceTest {

    private final FrontProductReviewRepository reviewRepository = mock(FrontProductReviewRepository.class);
    private final FrontProductReviewReportRepository reportRepository = mock(FrontProductReviewReportRepository.class);
    private final FrontProductReviewStatusHistoryRepository statusHistoryRepository = mock(FrontProductReviewStatusHistoryRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final BrandRepository brandRepository = mock(BrandRepository.class);
    private final AdminProductReviewService service = new AdminProductReviewService(
            reviewRepository,
            reportRepository,
            statusHistoryRepository,
            productRepository,
            brandRepository
    );

    @Test
    void filtersReviewsByVisibilityStatus() {
        given(reviewRepository.findByStatusOrderByIdDesc(FrontProductReviewStatus.HIDDEN.name(), PageRequest.of(0, 20)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var response = service.getReviews("HIDDEN", false, 0, 20);

        assertThat(response.totalCount()).isZero();
        verify(reviewRepository).findByStatusOrderByIdDesc(FrontProductReviewStatus.HIDDEN.name(), PageRequest.of(0, 20));
    }

    @Test
    void changesTheRequestedReviewVisibilityStatus() {
        FrontProductReview review = mock(FrontProductReview.class);
        given(reviewRepository.findById(12L)).willReturn(Optional.of(review));
        given(review.getStatus()).willReturn(FrontProductReviewStatus.VISIBLE.name());

        service.changeStatus(12L, FrontProductReviewStatus.HIDDEN);

        verify(review).changeStatus(FrontProductReviewStatus.HIDDEN);
        ArgumentCaptor<FrontProductReviewStatusHistory> historyCaptor = ArgumentCaptor.forClass(FrontProductReviewStatusHistory.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getReviewNo()).isEqualTo(12L);
        assertThat(historyCaptor.getValue().getBeforeStatus()).isEqualTo(FrontProductReviewStatus.VISIBLE.name());
        assertThat(historyCaptor.getValue().getAfterStatus()).isEqualTo(FrontProductReviewStatus.HIDDEN.name());
        assertThat(historyCaptor.getValue().getActionType()).isEqualTo("HIDE");
    }

    @Test
    void loadsReportedReviewsWhenReportedFilterIsRequested() {
        given(reviewRepository.findReportedReviews(null, PageRequest.of(0, 20)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var response = service.getReviews("ALL", true, 0, 20);

        assertThat(response.reviews()).isEmpty();
        verify(reviewRepository).findReportedReviews(null, PageRequest.of(0, 20));
    }

    @Test
    void doesNotCreateHistoryWhenTheReviewIsAlreadyInTheRequestedStatus() {
        FrontProductReview review = mock(FrontProductReview.class);
        given(reviewRepository.findById(12L)).willReturn(Optional.of(review));
        given(review.getStatus()).willReturn(FrontProductReviewStatus.HIDDEN.name());

        service.changeStatus(12L, FrontProductReviewStatus.HIDDEN);

        verify(statusHistoryRepository, never()).save(any());
        verify(review, never()).changeStatus(any());
    }
}
