package com.section.admin.review.service;

import com.section.common.commerce.entity.FrontProductReview;
import com.section.common.commerce.entity.FrontProductReviewReport;
import com.section.common.commerce.entity.FrontProductReviewReportStatus;
import com.section.common.commerce.entity.FrontProductReviewStatus;
import com.section.common.commerce.entity.FrontProductReviewStatusHistory;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.FrontProductReviewRepository;
import com.section.common.commerce.repository.FrontProductReviewReportRepository;
import com.section.common.commerce.repository.FrontProductReviewStatusHistoryRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.common.system.repository.AdminUserRepository;
import com.section.common.system.entity.AdminUser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

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
    private final AdminUserRepository adminUserRepository = mock(AdminUserRepository.class);
    private final AdminProductReviewService service = new AdminProductReviewService(
            reviewRepository,
            reportRepository,
            statusHistoryRepository,
            productRepository,
            brandRepository,
            adminUserRepository
    );

    @Test
    void filtersReviewsByVisibilityStatus() {
        given(reviewRepository.findByStatusOrderByIdDesc(FrontProductReviewStatus.HIDDEN.name(), PageRequest.of(0, 20)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var response = service.getReviews("HIDDEN", false, false, 0, 20);

        assertThat(response.totalCount()).isZero();
        verify(reviewRepository).findByStatusOrderByIdDesc(FrontProductReviewStatus.HIDDEN.name(), PageRequest.of(0, 20));
    }

    @Test
    void includesStatusHistoryWithTheProcessingAdminInTheReviewList() {
        FrontProductReview review = mock(FrontProductReview.class);
        FrontProductReviewReport report = mock(FrontProductReviewReport.class);
        FrontProductReviewStatusHistory history = mock(FrontProductReviewStatusHistory.class);
        Product product = mock(Product.class);
        Brand brand = mock(Brand.class);
        AdminUser admin = mock(AdminUser.class);
        given(review.getId()).willReturn(12L);
        given(review.getProductNo()).willReturn(21L);
        given(review.getStatus()).willReturn(FrontProductReviewStatus.HIDDEN.name());
        given(review.getReviewerName()).willReturn("구매자");
        given(review.getRating()).willReturn(5);
        given(review.getContent()).willReturn("후기 내용");
        given(reviewRepository.findAllByOrderByIdDesc(PageRequest.of(0, 20)))
                .willReturn(new PageImpl<>(List.of(review), PageRequest.of(0, 20), 1));
        given(reportRepository.countByReviewNoIn(List.of(12L))).willReturn(List.of());
        given(reportRepository.countByReviewNoInAndStatus(List.of(12L), FrontProductReviewReportStatus.PENDING.name()))
                .willReturn(List.of());
        given(reportRepository.findAllByReviewNoInOrderByIdDesc(List.of(12L))).willReturn(List.of(report));
        given(report.getReviewNo()).willReturn(12L);
        given(report.getStatus()).willReturn(FrontProductReviewReportStatus.RESOLVED.name());
        given(report.isResolved()).willReturn(true);
        given(report.getUptNo()).willReturn(3L);
        given(report.getUptDtm()).willReturn(LocalDateTime.of(2026, 8, 12, 12, 30));
        given(report.getReason()).willReturn("광고");
        given(report.getDetail()).willReturn(null);
        given(report.getCrtDtm()).willReturn(LocalDateTime.of(2026, 8, 12, 12, 0));
        given(statusHistoryRepository.findAllByReviewNoInOrderByIdDesc(List.of(12L))).willReturn(List.of(history));
        given(history.getReviewNo()).willReturn(12L);
        given(history.getCrtNo()).willReturn(3L);
        given(history.getActionType()).willReturn("HIDE");
        given(history.getBeforeStatus()).willReturn(FrontProductReviewStatus.VISIBLE.name());
        given(history.getAfterStatus()).willReturn(FrontProductReviewStatus.HIDDEN.name());
        given(productRepository.findAllById(any())).willReturn(List.of(product));
        given(product.getId()).willReturn(21L);
        given(product.getBrandNo()).willReturn(4L);
        given(product.getNameKo()).willReturn("테스트 상품");
        given(brandRepository.findAllById(any())).willReturn(List.of(brand));
        given(brand.getBrandNo()).willReturn(4L);
        given(brand.getNameKo()).willReturn("테스트 브랜드");
        given(adminUserRepository.findAllById(List.of(3L))).willReturn(List.of(admin));
        given(admin.getAdminNo()).willReturn(3L);
        given(admin.getName()).willReturn("운영자");

        var response = service.getReviews("ALL", false, false, 0, 20);

        assertThat(response.reviews().get(0).statusHistories()).singleElement().satisfies(item -> {
            assertThat(item.actionLabel()).isEqualTo("숨김");
            assertThat(item.beforeStatusLabel()).isEqualTo("노출");
            assertThat(item.afterStatusLabel()).isEqualTo("숨김");
            assertThat(item.actorName()).isEqualTo("운영자");
        });
        assertThat(response.reviews().get(0).reports()).singleElement().satisfies(item -> {
            assertThat(item.statusLabel()).isEqualTo("처리 완료");
            assertThat(item.resolvedBy()).isEqualTo("운영자");
            assertThat(item.resolvedAt()).isEqualTo("2026-08-12 12:30");
        });
    }

    @Test
    void changesTheRequestedReviewVisibilityStatus() {
        FrontProductReview review = mock(FrontProductReview.class);
        FrontProductReviewReport pendingReport = mock(FrontProductReviewReport.class);
        given(reviewRepository.findByIdForUpdate(12L)).willReturn(Optional.of(review));
        given(review.getStatus()).willReturn(FrontProductReviewStatus.VISIBLE.name());
        given(reportRepository.findAllByReviewNoAndStatus(12L, FrontProductReviewReportStatus.PENDING.name()))
                .willReturn(List.of(pendingReport));

        service.changeStatus(12L, FrontProductReviewStatus.HIDDEN);

        verify(review).changeStatus(FrontProductReviewStatus.HIDDEN);
        verify(pendingReport).resolve();
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

        var response = service.getReviews("ALL", true, false, 0, 20);

        assertThat(response.reviews()).isEmpty();
        verify(reviewRepository).findReportedReviews(null, PageRequest.of(0, 20));
    }

    @Test
    void loadsOnlyPendingReportedReviewsWhenPendingFilterIsRequested() {
        given(reviewRepository.findReviewsWithReportStatus(
                null,
                FrontProductReviewReportStatus.PENDING.name(),
                PageRequest.of(0, 20)
        )).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var response = service.getReviews("ALL", false, true, 0, 20);

        assertThat(response.reviews()).isEmpty();
        verify(reviewRepository).findReviewsWithReportStatus(
                null,
                FrontProductReviewReportStatus.PENDING.name(),
                PageRequest.of(0, 20)
        );
    }

    @Test
    void pendingReportFilterKeepsRequestedVisibilityStatus() {
        given(reviewRepository.findReviewsWithReportStatus(
                FrontProductReviewStatus.HIDDEN.name(),
                FrontProductReviewReportStatus.PENDING.name(),
                PageRequest.of(0, 20)
        )).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        service.getReviews("HIDDEN", false, true, 0, 20);

        verify(reviewRepository).findReviewsWithReportStatus(
                FrontProductReviewStatus.HIDDEN.name(),
                FrontProductReviewReportStatus.PENDING.name(),
                PageRequest.of(0, 20)
        );
    }

    @Test
    void doesNotCreateHistoryWhenTheReviewIsAlreadyInTheRequestedStatus() {
        FrontProductReview review = mock(FrontProductReview.class);
        given(reviewRepository.findByIdForUpdate(12L)).willReturn(Optional.of(review));
        given(review.getStatus()).willReturn(FrontProductReviewStatus.HIDDEN.name());

        service.changeStatus(12L, FrontProductReviewStatus.HIDDEN);

        verify(statusHistoryRepository, never()).save(any());
        verify(review, never()).changeStatus(any());
        verify(reportRepository, never()).findAllByReviewNoAndStatus(any(Long.class), any(String.class));
    }

    @Test
    void resolvesPendingReportsWithoutChangingReviewVisibility() {
        FrontProductReview review = mock(FrontProductReview.class);
        FrontProductReviewReport pendingReport = mock(FrontProductReviewReport.class);
        given(reviewRepository.findByIdForUpdate(12L)).willReturn(Optional.of(review));
        given(reportRepository.findAllByReviewNoAndStatus(12L, FrontProductReviewReportStatus.PENDING.name()))
                .willReturn(List.of(pendingReport));

        int resolvedCount = service.resolveReports(12L);

        assertThat(resolvedCount).isEqualTo(1);
        verify(pendingReport).resolve();
        verify(review, never()).changeStatus(any());
        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    void resolvingAlreadyCompletedReportsDoesNotWriteAgain() {
        FrontProductReview review = mock(FrontProductReview.class);
        given(reviewRepository.findByIdForUpdate(12L)).willReturn(Optional.of(review));
        given(reportRepository.findAllByReviewNoAndStatus(12L, FrontProductReviewReportStatus.PENDING.name()))
                .willReturn(List.of());

        int resolvedCount = service.resolveReports(12L);

        assertThat(resolvedCount).isZero();
        verify(review, never()).changeStatus(any());
        verify(statusHistoryRepository, never()).save(any());
    }
}
