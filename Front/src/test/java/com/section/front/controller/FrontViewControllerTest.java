package com.section.front.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class FrontViewControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new FrontViewController()).build();

    @Test
    @DisplayName("프론트 루트 화면은 메인 뷰를 반환한다")
    void indexReturnsMainView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/index"));
    }

    @Test
    @DisplayName("프론트 별칭 경로도 메인 뷰를 반환한다")
    void frontAliasReturnsMainView() throws Exception {
        mockMvc.perform(get("/front"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/index"));
    }

    @Test
    @DisplayName("상품 상세 경로는 상세 뷰와 상품 번호를 반환한다")
    void productDetailReturnsDetailView() throws Exception {
        mockMvc.perform(get("/front/products/101"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/product-detail"))
                .andExpect(model().attribute("productId", 101L));
    }

    @Test
    @DisplayName("상품 컬렉션 경로는 독립 목록 뷰와 컬렉션 타입을 반환한다")
    void productCollectionReturnsCollectionView() throws Exception {
        mockMvc.perform(get("/front/collections/ranking"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/product-collection"))
                .andExpect(model().attribute("collectionType", "ranking"));
    }

    @Test
    @DisplayName("여름 이벤트 경로는 독립 에디토리얼 뷰를 반환한다")
    void summerEditReturnsEventView() throws Exception {
        mockMvc.perform(get("/front/events/summer-edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/summer-edit"));
    }

    @Test
    @DisplayName("장바구니와 주문서 경로는 각각의 거래 뷰를 반환한다")
    void commerceViewsReturnExpectedViews() throws Exception {
        mockMvc.perform(get("/front/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/cart"));

        mockMvc.perform(get("/front/checkout"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/checkout"));
    }

    @Test
    @DisplayName("주문 조회 경로는 주문번호를 조회 화면에 전달한다")
    void orderLookupReturnsOrderView() throws Exception {
        mockMvc.perform(get("/front/orders/GS202607250001"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/order-lookup"))
                .andExpect(model().attribute("orderNumber", "GS202607250001"));
    }

    @Test
    @DisplayName("콘텐츠 상세 경로는 상세 뷰와 문서 번호를 반환한다")
    void contentDetailReturnsDetailView() throws Exception {
        mockMvc.perform(get("/front/content/101"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/content-detail"))
                .andExpect(model().attribute("documentId", 101L));
    }

    @Test
    @DisplayName("콘텐츠 목록 경로는 공개 콘텐츠 탐색 뷰를 반환한다")
    void contentListReturnsListView() throws Exception {
        mockMvc.perform(get("/front/content"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/content-list"));
    }

    @Test
    @DisplayName("MY 쇼핑 활동 경로는 독립 관리 화면을 반환한다")
    void myActivityReturnsActivityView() throws Exception {
        mockMvc.perform(get("/front/my"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/my-activity"));
    }

    @Test
    @DisplayName("고객지원 경로는 독립 도움말 화면을 반환한다")
    void supportCenterReturnsSupportView() throws Exception {
        mockMvc.perform(get("/front/support"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/support-center"));
    }

    @Test
    @DisplayName("브랜드 탐색 경로는 독립 브랜드 디렉터리를 반환한다")
    void brandDirectoryReturnsBrandView() throws Exception {
        mockMvc.perform(get("/front/brands"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/brand-directory"));
    }

    @Test
    @DisplayName("상품 비교 경로는 독립 비교 워크스페이스를 반환한다")
    void productComparisonReturnsComparisonView() throws Exception {
        mockMvc.perform(get("/front/compare"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/product-comparison"));
    }
}
