package com.section.front.productreview.service;

import com.section.common.commerce.entity.FrontProductReview;
import com.section.common.commerce.repository.FrontProductReviewReportRepository;
import com.section.common.commerce.repository.FrontProductReviewRepository;
import com.section.common.commerce.repository.OrderItemRepository;
import com.section.common.commerce.repository.OrderRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.common.system.entity.Account;
import com.section.common.system.repository.AccountRepository;
import com.section.front.product.service.FrontProductCatalogService;
import com.section.front.productreview.dto.FrontProductReviewReportRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FrontProductReviewReportServiceTest {
    private final FrontProductReviewRepository reviewRepository = mock(FrontProductReviewRepository.class);
    private final FrontProductReviewReportRepository reportRepository = mock(FrontProductReviewReportRepository.class);
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final FrontProductReviewService service = new FrontProductReviewService(
            reviewRepository, reportRepository, mock(ProductRepository.class), mock(OrderRepository.class),
            mock(OrderItemRepository.class), accountRepository, mock(FrontProductCatalogService.class)
    );

    @Test
    void savesOneReportForAnotherMembersVisibleReview() {
        Account member = mock(Account.class);
        FrontProductReview review = mock(FrontProductReview.class);
        given(accountRepository.findById(7L)).willReturn(Optional.of(member));
        given(member.isAvailableCustomer()).willReturn(true);
        given(member.getId()).willReturn(7L);
        given(reviewRepository.findById(11L)).willReturn(Optional.of(review));
        given(review.isVisible()).willReturn(true);
        given(review.getMemberNo()).willReturn(8L);
        given(reportRepository.existsByReviewNoAndMemberNo(11L, 7L)).willReturn(false);

        service.reportReview(7L, 11L, new FrontProductReviewReportRequest("부적절한 내용", "운영 확인 요청"));

        verify(reportRepository).save(any());
    }

    @Test
    void rejectsDuplicateReportByTheSameMember() {
        Account member = mock(Account.class);
        FrontProductReview review = mock(FrontProductReview.class);
        given(accountRepository.findById(7L)).willReturn(Optional.of(member));
        given(member.isAvailableCustomer()).willReturn(true);
        given(reviewRepository.findById(11L)).willReturn(Optional.of(review));
        given(review.isVisible()).willReturn(true);
        given(review.getMemberNo()).willReturn(8L);
        given(reportRepository.existsByReviewNoAndMemberNo(11L, 7L)).willReturn(true);

        assertThatThrownBy(() -> service.reportReview(7L, 11L, new FrontProductReviewReportRequest("광고", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 신고한 후기");
    }
}
