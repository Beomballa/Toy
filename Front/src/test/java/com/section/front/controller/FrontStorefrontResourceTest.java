package com.section.front.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class FrontStorefrontResourceTest {

    @Test
    void mainPageKeepsStorefrontStructureAndAccessibleDialogHooks() throws IOException {
        String html = readResource("templates/views/index.html");

        assertThat(html)
                .contains("/css/storefront.css?v=20260713.9")
                .contains("id=\"headerSearchPanel\"")
                .contains("id=\"homeCategoryRail\"")
                .contains("id=\"heroNextButton\"")
                .contains("id=\"catalogMemoryTools\"")
                .contains("role=\"dialog\"")
                .contains("aria-modal=\"true\"")
                .contains("aria-labelledby=\"drawerTitle\"")
                .contains("/js/view/app.js?v=20260713.9");
    }

    @Test
    void detailPageKeepsCommerceVisualAndPrimaryActionHooks() throws IOException {
        String html = readResource("templates/views/product-detail.html");

        assertThat(html)
                .contains("/css/storefront.css?v=20260713.9")
                .contains("id=\"detailProductVisual\"")
                .contains("id=\"detailVisualModel\"")
                .contains("id=\"detailPrimaryAction\"")
                .contains("/js/view/detail.js?v=20260713.9");
    }

    @Test
    void storefrontKeepsKreamTypographyScale() throws IOException {
        String css = readResource("static/css/storefront.css");

        assertThat(css)
                .contains("--store-text-display: 32px")
                .contains("--store-text-search: 24px")
                .contains("--store-text-body: 16px")
                .contains("--store-text-meta: 13px")
                .contains("--store-leading-display: 1.2")
                .contains("--store-leading-body: 1.4");
    }

    @Test
    void catalogKeepsProductFirstCardContract() throws IOException {
        String script = readResource("static/js/view/app.js");

        assertThat(script)
                .contains("catalog-card__visual-link")
                .contains("catalog-card__wish")
                .contains("data-bookmark-product-id")
                .contains("catalog-card__button")
                .contains("aria-label=\"${product.name} 빠른 보기\"")
                .contains("product.thumbnailUrl")
                .contains("data-product-image")
                .contains("handleProductImageError");
    }

    @Test
    void catalogKeepsKreamSearchAndFilterStateContract() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html).contains("role=\"search\" aria-label=\"상품 검색 및 필터\"");
        assertThat(script)
                .contains("syncFilterFieldStates")
                .contains("classList.toggle(\"has-value\"");
        assertThat(css)
                .contains(".toolbar-field.has-value select")
                .contains("min-height: 30px")
                .contains("font-size: var(--store-text-search)");
    }

    @Test
    void mobileStoreNavigationKeepsSectionAndSavedStateHooks() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"mobileStoreNav\"")
                .contains("data-mobile-nav=\"HOME\"")
                .contains("data-mobile-nav=\"SAVED\"")
                .contains("id=\"mobileSavedCount\"");
        assertThat(script)
                .contains("handleMobileStoreNavigation")
                .contains("initMobileStoreNavigation")
                .contains("syncMobileStoreNavigation")
                .contains("thumbnailUrl: source.thumbnailUrl");
        assertThat(css)
                .contains(".mobile-store-nav")
                .contains("grid-template-columns: repeat(5, minmax(0, 1fr))");
    }

    @Test
    void detailMobileActionsShareDesktopStateContract() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("class=\"detail-body\"")
                .contains("id=\"detailMobileActions\"")
                .contains("id=\"detailMobileBookmarkButton\"")
                .contains("id=\"detailMobileCompareButton\"")
                .contains("id=\"detailMobilePrimaryButton\"");
        assertThat(script)
                .contains("focusDetailOptions")
                .contains("detailMobileBookmarkButton")
                .contains("detailMobileCompareButton")
                .contains("thumbnailUrl: product.thumbnailUrl");
        assertThat(css)
                .contains(".detail-mobile-actions")
                .contains("grid-template-columns: 58px 58px minmax(0, 1fr)")
                .contains("body.detail-body");
    }

    @Test
    void detailOptionsKeepSelectableSizeContractWithoutFakePrice() throws IOException {
        String script = readResource("static/js/view/detail.js");
        String css = readResource("static/css/storefront.css");

        assertThat(script)
                .contains("data-detail-option")
                .contains("selectDetailOption")
                .contains("syncSelectedOptionActions")
                .contains("aria-pressed=\"${selectedOptionName === option.name}\"")
                .doesNotContain("추가금 ${formatPrice(option.additionalPrice)}");
        assertThat(css)
                .contains(".detail-option-card.is-selected")
                .contains("grid-template-columns: repeat(3, minmax(0, 1fr))")
                .contains(".detail-mobile-actions__primary.has-option");
    }

    @Test
    void heroKeepsAccessibleAutoplayAndSwipeContract() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("aria-roledescription=\"carousel\"")
                .contains("id=\"heroDots\"");
        assertThat(script)
                .contains("initHeroCarousel")
                .contains("prefers-reduced-motion: reduce")
                .contains("visibilitychange")
                .contains("pointerdown")
                .contains("Math.abs(distance) >= 48")
                .contains("window.setInterval(() => moveHeroSlide(1), 5000)");
        assertThat(css)
                .contains(".hero-carousel-dots")
                .contains("[aria-current=\"true\"]");
    }

    @Test
    void merchandisingSectionsShareProductRailContract() throws IOException {
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(script)
                .contains("productRailCard(product, kicker, \"signal-feed-card\")")
                .contains("productRailCard(product, featuredRankLabel(product), \"spotlight-card\")")
                .contains("bindRailBookmarkButtons")
                .contains("rail-product-card__wish")
                .contains("rail-product-card__preview")
                .contains("return list.slice(0, 4)");
        assertThat(css)
                .contains(".signal-feed")
                .contains(".rail-product-card")
                .contains("grid-template-columns: repeat(4, minmax(0, 1fr))");
    }

    @Test
    void discoveryKeepsKreamBrandAndCategoryTileContract() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("인기 브랜드")
                .contains("카테고리 쇼핑");
        assertThat(script)
                .contains("brand-rank-card__visual")
                .contains("category-shortcut-card__visual")
                .doesNotContain("긴장 재고 ${item.lowStockCount}개");
        assertThat(css)
                .contains("grid-template-columns: repeat(5, minmax(0, 1fr))")
                .contains("grid-template-columns: repeat(6, minmax(0, 1fr))")
                .contains(".brand-rank-card__visual")
                .contains(".category-shortcut-card__visual");
    }

    @Test
    void personalFlowKeepsCompactShoppingActivityContract() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("<h2>쇼핑 활동</h2>")
                .contains("class=\"board-action-menu\"")
                .contains("aria-live=\"polite\"")
                .contains("aria-label=\"쇼핑 활동 바로가기\"");
        assertThat(script)
                .contains("flow-board__topline")
                .contains("flow-board__status")
                .contains("aria-label=\"${card.label} ${card.count}개 보기\"")
                .doesNotContain("<p>${card.description}</p>");
        assertThat(css)
                .contains("grid-template-columns: repeat(4, minmax(0, 1fr))")
                .contains(".flow-board__card.is-active .flow-board__status")
                .contains("grid-template-columns: repeat(4, 178px)");
    }

    @Test
    void savedBoardsKeepCompactActionsAndProductFirstCards() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("class=\"saved-board-actions\"")
                .contains("<summary>정렬·관리</summary>")
                .contains("<summary>비교 관리</summary>")
                .contains("<summary>관심 관리</summary>")
                .contains("board-action-menu__danger");
        assertThat(script)
                .contains("compare-card saved-product-card")
                .contains("saved-product-card__actions")
                .contains("class=\"saved-product-card__menu\"")
                .contains("saved-product-card__danger");
        assertThat(css)
                .contains(".saved-product-card .detail-related-card__visual")
                .contains("grid-template-columns: minmax(0, 1fr) auto auto")
                .contains(".saved-product-card__menu > div")
                .contains(".flow-board__actions > .catalog-reset-button")
                .contains(".product-drawer:not(.is-open) .product-drawer__panel")
                .contains("contain: inline-size")
                .contains("overflow-x: clip")
                .contains("overflow: hidden");
    }

    @Test
    void catalogKeepsCollapsedFilterPanelAndActiveCountContract() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"catalogFilterPanel\"")
                .contains("id=\"catalogFilterCount\" aria-live=\"polite\"")
                .contains("class=\"catalog-filter-panel__body\"")
                .contains("id=\"resetFiltersButton\"");
        assertThat(script)
                .contains("const activeFilterCount")
                .contains("catalogFilterPanel?.classList.toggle(\"has-active-filter\"")
                .contains("setText(elements.catalogFilterCount, String(activeFilterCount))");
        assertThat(css)
                .contains(".catalog-filter-panel__body")
                .contains("grid-template-columns: repeat(3, minmax(0, 1fr))")
                .contains("grid-template-columns: repeat(2, minmax(0, 1fr))");
    }

    @Test
    void catalogResultsKeepCompactActionsAndPaginationContract() throws IOException {
        String html = readResource("templates/views/index.html");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("class=\"catalog-result-actions\"")
                .contains("class=\"board-action-menu catalog-result-menu\"")
                .contains("aria-label=\"적용된 탐색 조건\"")
                .contains("class=\"catalog-pagination__menu\"")
                .contains("aria-label=\"페이지 추가 설정\"");
        assertThat(css)
                .contains(".catalog-result-actions")
                .contains(".catalog-summary > .catalog-tags")
                .contains(".catalog-pagination__menu > div")
                .contains(".catalog-pagination__controls > button");
    }

    @Test
    void shoppingMemoryKeepsCountsAndCollapsedManagementContract() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"savedViewCount\" aria-live=\"polite\"")
                .contains("id=\"searchHistoryCount\" aria-live=\"polite\"")
                .contains("id=\"hiddenProductCount\" aria-live=\"polite\"")
                .contains("class=\"board-action-menu catalog-memory-menu\"");
        assertThat(script)
                .contains("setText(elements.savedViewCount, String(savedViews.length))")
                .contains("setText(elements.searchHistoryCount, String(history.length))")
                .contains("setText(elements.hiddenProductCount, String(hiddenProducts.length))")
                .contains("catalog-memory-empty");
        assertThat(css)
                .contains(".shopping-tools .catalog-memory-strip")
                .contains("grid-template-columns: repeat(3, minmax(0, 1fr))")
                .contains(".catalog-memory-head__title > span");
    }

    @Test
    void displayAndSelectionToolsKeepCompactAccessibleContract() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("class=\"catalog-layout-switch\" role=\"group\"")
                .contains("class=\"board-action-menu catalog-display-menu\"")
                .contains("id=\"catalogSelectionCount\" aria-live=\"polite\"")
                .contains("class=\"board-action-menu catalog-selection-menu\"");
        assertThat(script)
                .contains("setText(elements.catalogSelectionCount, String(selected.length))")
                .contains("button.disabled = selected.length === 0")
                .contains("setAttribute(\"aria-pressed\", String(isPressed))");
        assertThat(css)
                .contains(".catalog-layout-switch")
                .contains("grid-template-columns: repeat(4, minmax(48px, 1fr))")
                .contains(".catalog-selection__actions > button:disabled");
    }

    private String readResource(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
