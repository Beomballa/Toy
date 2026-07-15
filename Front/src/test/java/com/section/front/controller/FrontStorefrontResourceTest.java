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
                .contains("/css/storefront.css?v=20260715.20")
                .contains("id=\"headerSearchPanel\"")
                .contains("id=\"homeCategoryRail\"")
                .contains("id=\"heroNextButton\"")
                .contains("id=\"catalogMemoryTools\"")
                .contains("role=\"dialog\"")
                .contains("aria-modal=\"true\"")
                .contains("aria-labelledby=\"drawerTitle\"")
                .contains("/js/view/app.js?v=20260715.20");
    }

    @Test
    void detailPageKeepsCommerceVisualAndPrimaryActionHooks() throws IOException {
        String html = readResource("templates/views/product-detail.html");

        assertThat(html)
                .contains("/css/storefront.css?v=20260715.20")
                .contains("id=\"detailProductVisual\"")
                .contains("id=\"detailVisualModel\"")
                .contains("id=\"detailPrimaryAction\"")
                .contains("/js/view/detail.js?v=20260715.20");
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
                .contains("aria-checked=\"${selectedOptionName === option.name}\"")
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

    @Test
    void detailHeroKeepsCommerceFirstActionsAndAccessibleBoardState() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"detailPrimaryAction\" aria-controls=\"detailOptions\"")
                .contains("class=\"detail-secondary-actions\"")
                .contains("class=\"board-action-menu detail-action-menu\"")
                .contains("id=\"detailBookmarkButton\" aria-pressed=\"false\"")
                .contains("id=\"detailCompareButton\" aria-pressed=\"false\"");
        assertThat(script)
                .contains("detailBookmarkButton.setAttribute(\"aria-pressed\"")
                .contains("detailCompareButton.setAttribute(\"aria-pressed\"");
        assertThat(css)
                .contains(".detail-secondary-actions")
                .contains("grid-template-columns: repeat(2, minmax(0, 1fr)) auto")
                .contains(".detail-signal-list .signal-card");
    }

    @Test
    void detailCollectionsKeepCountsCollapsedControlsAndProductCards() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"detailOptionCount\" aria-live=\"polite\"")
                .contains("id=\"detailRelatedCount\" aria-live=\"polite\"")
                .contains("class=\"board-action-menu detail-section-menu\"");
        assertThat(script)
                .contains("detailOptionCount.textContent = String(options.length)")
                .contains("detailRelatedCount.textContent = String(related.length)")
                .contains("detail-related-card saved-product-card")
                .contains("aria-label=\"연관 상품 추가 작업\"")
                .contains("setAttribute(\"aria-pressed\", String(isPressed))");
        assertThat(css)
                .contains(".detail-section-count")
                .contains("#detailRelatedGrid .saved-product-card__actions")
                .contains("#detailRecentGrid .saved-product-card");
    }

    @Test
    void mainPageKeepsReferenceHeroBeforeCategoryAndUnifiedTypography() throws IOException {
        String html = readResource("templates/views/index.html");
        String css = readResource("static/css/storefront.css");

        assertThat(html.indexOf("<section class=\"hero\""))
                .isLessThan(html.indexOf("<nav class=\"home-category-rail\""));
        assertThat(css)
                .contains("main#top > .hero")
                .contains("main#top > .home-category-rail")
                .contains("font-size: 40px")
                .contains("grid-template-columns: repeat(8, 100px)")
                .contains("font-family: \"Pretendard Variable\"");
    }

    @Test
    void mainPageKeepsStructuredServiceFooterAndSectionHierarchy() throws IOException {
        String html = readResource("templates/views/index.html");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("<footer class=\"site-footer\" aria-label=\"서비스 정보\">")
                .contains("class=\"site-footer__support\"")
                .contains("class=\"site-footer__policy\"")
                .contains("실제 거래를 제공하지 않습니다");
        assertThat(css)
                .contains(".section-heading h2")
                .contains("font-size: 18px")
                .contains(".site-footer__bottom")
                .contains("grid-column: 1 / -1");
    }

    @Test
    void detailPageAlignsProductVisualAndCommerceTypography() throws IOException {
        String css = readResource("static/css/storefront.css");

        assertThat(css)
                .contains(".page-shell--detail .detail-product-visual")
                .contains("position: relative")
                .contains("aspect-ratio: 1")
                .contains(".page-shell--detail .detail-hero h1")
                .contains("font-size: 20px")
                .contains(".page-shell--detail .detail-price-card strong");
    }

    @Test
    void storefrontPagesKeepKeyboardNavigationAndReadingProgressContracts() throws IOException {
        String mainHtml = readResource("templates/views/index.html");
        String detailHtml = readResource("templates/views/product-detail.html");
        String mainScript = readResource("static/js/view/app.js");
        String detailScript = readResource("static/js/view/detail.js");

        assertThat(mainHtml)
                .contains("class=\"skip-link\" href=\"#top\"")
                .contains("id=\"top\" tabindex=\"-1\"")
                .contains("id=\"storefrontScrollProgress\"")
                .contains("id=\"storefrontStatus\" role=\"status\"");
        assertThat(detailHtml)
                .contains("class=\"skip-link\" href=\"#detailPage\"")
                .contains("id=\"detailPage\" tabindex=\"-1\"")
                .contains("id=\"detailScrollProgress\"");
        assertThat(mainScript)
                .contains("function syncScrollState()")
                .contains("aria-current", "storefrontStatus");
        assertThat(detailScript)
                .contains("function syncDetailScrollProgress()")
                .contains("aria-current", "detailStatus");
    }

    @Test
    void catalogKeepsAccessibleSearchLoadingAndRecoveryContracts() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");

        assertThat(html)
                .contains("role=\"combobox\"")
                .contains("id=\"searchSuggestionList\" role=\"listbox\"")
                .contains("id=\"catalogGrid\"")
                .contains("aria-busy=\"false\"")
                .contains("id=\"catalogPageProgress\" role=\"status\"");
        assertThat(script)
                .contains("role=\"option\"")
                .contains("function syncSearchActiveDescendant()")
                .contains("catalogLoadError")
                .contains("data-empty-action=\"RETRY\"")
                .contains("필터 ${activeFilterCount}개 적용됨")
                .contains("setAttribute(\"aria-busy\", \"true\")")
                .contains("function announceStorefrontStatus(message)");
    }

    @Test
    void storefrontKeepsPersonalCountsResetAndCrossTabSyncContracts() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"utilityRecentCount\"")
                .contains("id=\"utilityBookmarkCount\"")
                .contains("id=\"utilityCompareCount\"")
                .contains("id=\"resetPersonalDataButton\"");
        assertThat(script)
                .contains("function syncPersonalCounts()")
                .contains("async function resetPersonalData()")
                .contains("function syncPersonalStateFromStorage(event)")
                .contains("window.addEventListener(\"storage\", syncPersonalStateFromStorage)")
                .contains("LAST_DRAWER_PRODUCT_KEY")
                .doesNotContain("DISPLAY_PREFERENCES_KEY,\n            PAGE_SIZE_KEY");
        assertThat(css).contains(".utility-count");
    }

    @Test
    void detailPageKeepsBreadcrumbOptionZoomRetryAndTopContracts() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("class=\"detail-breadcrumb\"")
                .contains("id=\"detailOptionSelection\"")
                .contains("id=\"detailZoomButton\"")
                .contains("id=\"detailImageModal\" role=\"dialog\"")
                .contains("id=\"detailRetryButton\"")
                .contains("id=\"detailScrollTopButton\"");
        assertThat(script)
                .contains("function openDetailImageModal()")
                .contains("function closeDetailImageModal()")
                .contains("detailOptionSelection.hidden = !selected")
                .contains("detailRetryButton.hidden = false")
                .contains("window.scrollTo({ top: 0, behavior: \"smooth\" })")
                .contains("detailBreadcrumbCategory.href");
        assertThat(css)
                .contains(".detail-image-modal.is-open")
                .contains(".detail-option-selection")
                .contains(".detail-top-button");
    }

    @Test
    void catalogKeepsNetworkConcurrencyAndVisibilityContracts() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");

        assertThat(html)
                .contains("id=\"networkStatus\" role=\"status\"")
                .contains("id=\"networkRetryButton\"");
        assertThat(script)
                .contains("new AbortController()")
                .contains("catalogRequestSequence")
                .contains("error?.name === \"AbortError\"")
                .contains("window.addEventListener(\"online\", handleNetworkReconnect)")
                .contains("document.addEventListener(\"visibilitychange\", handleVisibilityChange)")
                .contains("stopHeroCarousel()", "startHeroCarousel()");
    }

    @Test
    void catalogKeepsSemanticCardsImagePriorityAndKeyboardContracts() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");

        assertThat(html)
                .contains("id=\"catalogGrid\" role=\"list\"")
                .contains("id=\"copyCurrentPageLinksButton\"");
        assertThat(script)
                .contains("role=\"listitem\"")
                .contains("fetchpriority=\"high\"")
                .contains("product-visual--empty")
                .contains("이미지 없음")
                .contains("function handleCatalogCardNavigation(event)")
                .contains("currentCatalogPageProducts().map")
                .contains("elements.catalogGrid?.addEventListener(\"keydown\", handleCatalogCardNavigation)");
    }

    @Test
    void detailKeepsOptionSummaryMemoryAndRelatedNavigationContracts() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");

        assertThat(html)
                .contains("id=\"detailAvailableOptionCount\"")
                .contains("id=\"detailLowOptionCount\"")
                .contains("id=\"detailSoldOutOptionCount\"")
                .contains("id=\"detailRecommendOptionButton\"")
                .contains("id=\"detailPreviousRelatedButton\"")
                .contains("id=\"detailNextRelatedButton\"");
        assertThat(script)
                .contains("SELECTED_OPTION_KEY")
                .contains("function rememberSelectedOption")
                .contains("function restoreRememberedOption")
                .contains("function relatedPriceDeltaLabel")
                .contains("function openRelatedByDirection")
                .contains("sort((left, right) => Number(right.stock || 0) - Number(left.stock || 0))");
    }

    @Test
    void storefrontKeepsNativeShareShortcutHelpAndDismissibleStatusContracts() throws IOException {
        String html = readResource("templates/views/index.html");
        String mainScript = readResource("static/js/view/app.js");
        String detailScript = readResource("static/js/view/detail.js");

        assertThat(html)
                .contains("id=\"networkDismissButton\"")
                .contains("id=\"keyboardHelpButton\"")
                .contains("id=\"shortcutHelpModal\" role=\"dialog\"")
                .contains("id=\"shortcutHelpCloseButton\"");
        assertThat(mainScript)
                .contains("navigator.share")
                .contains("function openShortcutHelp()")
                .contains("function closeShortcutHelp()")
                .contains("shortcutHelpReturnFocus?.focus?.()")
                .contains("networkStatusDismissed = true")
                .contains("event.key === \"?\"");
        assertThat(detailScript)
                .contains("navigator.share")
                .contains("상품 요약과 상세 URL을 전달했습니다.");
    }

    @Test
    void catalogKeepsSessionRecoveryDataSaverPrefetchAndScrollContracts() throws IOException {
        String script = readResource("static/js/view/app.js");

        assertThat(script)
                .contains("CATALOG_CACHE_KEY")
                .contains("function readCatalogSessionCache()")
                .contains("네트워크 오류로 최근 세션 카탈로그를 표시합니다.")
                .contains("window.navigator.connection?.saveData")
                .contains("function warmCatalogProductDetail(event)")
                .contains("window.addEventListener(\"pagehide\", persistCatalogScrollPosition)")
                .contains("function restoreCatalogScrollPosition()");
    }

    @Test
    void catalogKeepsExpandedKeyboardNavigationContracts() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");

        assertThat(html)
                .contains("<dt>F</dt><dd>상품 필터 열기</dd>")
                .contains("<dt>Page ↑ / ↓</dt>");
        assertThat(script)
                .contains("event.key === \"Enter\"")
                .contains("event.key === \"PageUp\" || event.key === \"PageDown\"")
                .contains("function openCatalogFilterPanel()")
                .contains("function closeCatalogFilterPanel()")
                .contains("focusedProduct?.name || \"상품\"")
                .contains("카드입니다.");
    }

    @Test
    void detailOptionsKeepRadioNavigationShareAndAvailabilityContracts() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("role=\"radiogroup\" aria-label=\"구매 옵션\"")
                .contains("id=\"detailCopySelectedOptionButton\"")
                .contains("id=\"detailShareSelectedOptionButton\"")
                .contains("id=\"detailOptionStockRateBar\"");
        assertThat(script)
                .contains("role=\"radio\"")
                .contains("function handleDetailOptionNavigation(event)")
                .contains("function selectedOptionSummary()")
                .contains("async function shareSelectedOption()")
                .contains("detailOptionStockRateBar.style.width");
        assertThat(css).contains(".detail-option-stock-rate");
    }

    @Test
    void detailKeepsRecentRemovalCrossTabBreadcrumbAndRelatedSummaryContracts() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");

        assertThat(html)
                .contains("id=\"detailCopyBreadcrumbButton\"")
                .contains("id=\"detailRelatedAveragePrice\"")
                .contains("id=\"detailCompareAllRelatedButton\"");
        assertThat(script)
                .contains("data-remove-detail-recent-id")
                .contains("function removeRecentProduct(productIdValue)")
                .contains("window.addEventListener(\"storage\", syncDetailStateFromStorage)")
                .contains("async function copyDetailBreadcrumb()")
                .contains("function addAllRelatedToCompare()")
                .contains("비교 보드는 최대 3개 상품을 유지합니다.");
    }

    @Test
    void catalogCardsKeepProductivityShortcutsAndFocusRecoveryContracts() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");

        assertThat(html).contains("<dt>B / C / S / Q / L</dt>");
        assertThat(script)
                .contains("data-catalog-product-id")
                .contains("aria-keyshortcuts=\"Enter B C S Q L\"")
                .contains("[\"b\", \"c\", \"s\", \"q\", \"l\"]")
                .contains("toggleBookmarkProduct(productId)")
                .contains("toggleCompareProduct(productId)")
                .contains("toggleSelectedProduct(productId)")
                .contains("openDrawer(productId)")
                .contains("function restoreCatalogCardFocus(productId)")
                .contains("function focusCatalogCardAfterRender()");
    }

    @Test
    void catalogKeepsSearchSessionTitleAndLinkShortcutContracts() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");

        assertThat(html)
                .contains("<dt>Ctrl / ⌘ + K</dt>")
                .contains("B / C / S / Q / L");
        assertThat(script)
                .contains("FILTER_PANEL_OPEN_KEY")
                .contains("restoreCatalogFilterPanelState()")
                .contains("addEventListener(\"toggle\", persistCatalogFilterPanelState)")
                .contains("event.ctrlKey || event.metaKey")
                .contains("function syncCatalogDocumentTitle(resultCount)")
                .contains("function copyFocusedCatalogProductLink(productId)")
                .contains("elements.searchInput.blur()");
    }

    @Test
    void detailOptionsKeepDeepLinkStockFilterAndShortcutContracts() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");

        assertThat(html)
                .contains("id=\"detailOptions\" aria-keyshortcuts=\"O\"")
                .contains("id=\"detailOptionAvailableOnlyButton\"")
                .contains("id=\"detailTotalOptionStock\"");
        assertThat(script)
                .contains("new URLSearchParams(window.location.search).get(\"option\")")
                .contains("function syncSelectedOptionUrl()")
                .contains("url.searchParams.set(\"option\", selectedOptionName)")
                .contains("detailTotalOptionStock")
                .contains("optionSortState.availableOnly")
                .contains("function openDetailOptionsFromKeyboard()")
                .contains("params.delete(\"option\")");
    }

    @Test
    void detailKeepsRelatedPriceRangeAndRecentNavigationContracts() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"detailRelatedMinPrice\"")
                .contains("id=\"detailRelatedMaxPrice\"")
                .contains("id=\"detailRecentCount\"")
                .contains("id=\"detailPreviousRecentButton\"")
                .contains("id=\"detailNextRecentButton\"")
                .contains("id=\"copyDetailRecentLinksButton\"");
        assertThat(script)
                .contains("Math.min(...relatedPrices)")
                .contains("Math.max(...relatedPrices)")
                .contains("function openRecentProductByDirection(direction)")
                .contains("async function copyRecentProductLinks()")
                .contains("setElementText(elements.detailRecentCount");
        assertThat(css).contains(".detail-related-average");
    }

    @Test
    void catalogCardsKeepAccessiblePositionLabelAndShortcutHintContracts() throws IOException {
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(script)
                .contains("aria-posinset=\"${positionOffset + index + 1}\"")
                .contains("aria-setsize=\"${allList.length}\"")
                .contains("data-keyboard-hint")
                .contains("${escapeAttribute(product.name)} ${selectedProductIds")
                .contains("${escapeAttribute(product.name)} ${bookmarkedIds")
                .contains("focusedProduct?.name");
        assertThat(css)
                .contains("content: attr(data-keyboard-hint)")
                .contains(":focus-visible::after");
    }

    @Test
    void catalogKeepsCurrentPageBulkSelectionAndCsvContracts() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");

        assertThat(html)
                .contains("id=\"selectLowStockPageButton\"")
                .contains("id=\"selectFeaturedPageButton\"")
                .contains("id=\"invertPageSelectionButton\"")
                .contains("id=\"copyCurrentPageSummaryButton\"")
                .contains("id=\"exportCurrentPageCsvButton\"");
        assertThat(script)
                .contains("function selectCurrentPageProducts(predicate, label)")
                .contains("function invertCurrentPageSelection()")
                .contains("catalogSummaryClipboardText(currentCatalogPageProducts())")
                .contains("function exportCurrentPageCsv()")
                .contains("function csvCell(value)")
                .contains("function downloadTextFile(fileName, content, type)");
    }

    @Test
    void detailKeepsQuantityStockBoundEstimateAndSummaryContracts() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"detailQuantityDecreaseButton\"")
                .contains("id=\"detailQuantityInput\"")
                .contains("id=\"detailQuantityIncreaseButton\"")
                .contains("id=\"detailEstimatedTotal\"")
                .contains("id=\"detailCopyOrderSummaryButton\"");
        assertThat(script)
                .contains("DETAIL_QUANTITY_KEY")
                .contains("function setSelectedQuantity(nextQuantity, announce = true)")
                .contains("Math.min(maxQuantity")
                .contains("function syncPurchaseEstimate(option)")
                .contains("async function copyOrderSummary()");
        assertThat(css)
                .contains(".detail-purchase-estimate")
                .contains(".detail-quantity-control");
    }

    @Test
    void relatedProductsKeepSemanticKeyboardAndMetricNavigationContracts() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"detailRelatedGrid\" role=\"list\"")
                .contains("id=\"detailCheapestRelatedButton\"")
                .contains("id=\"detailHighestStockRelatedButton\"")
                .contains("id=\"detailCopyPriceComparisonButton\"");
        assertThat(script)
                .contains("role=\"listitem\" data-related-product-id")
                .contains("function handleRelatedCardNavigation(event)")
                .contains("async function copyRelatedPriceComparison()")
                .contains("function openRelatedByMetric(metric)")
                .contains("openRelatedByMetric(\"PRICE_LOW\")")
                .contains("openRelatedByMetric(\"STOCK_HIGH\")");
        assertThat(css).contains("#detailRelatedGrid [role=\"listitem\"]:focus-visible");
    }

    private String readResource(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
