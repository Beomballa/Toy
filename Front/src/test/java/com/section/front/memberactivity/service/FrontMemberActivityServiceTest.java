package com.section.front.memberactivity.service;

import com.section.common.commerce.entity.FrontMemberActivityType;
import com.section.common.commerce.entity.FrontMemberProductActivity;
import com.section.common.commerce.repository.FrontMemberProductActivityRepository;
import com.section.common.system.entity.Account;
import com.section.common.system.repository.AccountRepository;
import com.section.front.product.dto.FrontProductResponse;
import com.section.front.product.service.FrontProductCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class FrontMemberActivityServiceTest {

    private final FrontMemberProductActivityRepository activityRepository = mock(FrontMemberProductActivityRepository.class);
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final FrontProductCatalogService productCatalogService = mock(FrontProductCatalogService.class);
    private final FrontMemberActivityService service = new FrontMemberActivityService(
            activityRepository,
            accountRepository,
            productCatalogService
    );

    @BeforeEach
    void setUp() {
        Account account = Account.createCustomer("member@example.com", "encoded", "회원", "노렌");
        given(accountRepository.findByIdForUpdate(7L)).willReturn(Optional.of(account));
        given(productCatalogService.findProducts(anySet())).willAnswer(invocation -> invocation.<Set<Long>>getArgument(0).stream()
                .collect(java.util.stream.Collectors.toMap(id -> id, this::product)));
        given(activityRepository.findAllByMemberNoOrderByLastInteractedAtDescIdDesc(7L)).willReturn(List.of());
    }

    @Test
    void replaceDeduplicatesProductsAndAppliesTypeLimit() {
        service.replace(7L, FrontMemberActivityType.COMPARE, List.of(3L, 2L, 3L, 1L, 4L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FrontMemberProductActivity>> captor = ArgumentCaptor.forClass(List.class);
        verify(activityRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(FrontMemberProductActivity::getProductNo)
                .containsExactly(3L, 2L, 1L);
        verify(activityRepository).deleteAllByMemberNoAndActivityType(7L, FrontMemberActivityType.COMPARE);
        verify(activityRepository).flush();
    }

    @Test
    void replaceRejectsMissingActiveProductWithoutDeletingCurrentState() {
        given(productCatalogService.findProducts(Set.of(99L))).willReturn(Map.of());

        assertThatThrownBy(() -> service.replace(7L, FrontMemberActivityType.BOOKMARK, List.of(99L)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");

        verify(activityRepository, never()).deleteAllByMemberNoAndActivityType(7L, FrontMemberActivityType.BOOKMARK);
    }

    private FrontProductResponse product(long id) {
        return new FrontProductResponse(
                id,
                "브랜드",
                "스니커즈",
                "상품 " + id,
                "상품 " + id,
                "MODEL-" + id,
                100000,
                10,
                "2026-08-05",
                "설명",
                "daily",
                false,
                null,
                "STABLE",
                "100,000원",
                List.of(),
                "/images/product-placeholder.svg"
        );
    }
}
