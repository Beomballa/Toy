package com.section.front.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class FrontStorefrontResourceTest {

    @Test
    void secondaryFrontPagesShareNavigationSearchAndFooterContract() throws IOException {
        String fragment = readResource("templates/fragments/storefront-shell.html");
        String stateScript = readResource("static/js/view/storefront-state.js");
        String script = readResource("static/js/view/storefront-shell.js");
        String css = readResource("static/css/storefront-shell.css");
        String[] pages = {
                "product-detail.html",
                "product-collection.html",
                "product-comparison.html",
                "brand-directory.html",
                "content-list.html",
                "content-detail.html",
                "support-center.html",
                "cart.html",
                "checkout.html",
                "order-lookup.html",
                "my-activity.html"
        };

        assertThat(fragment)
                .contains("th:fragment=\"header(activeSection, contextLabel)\"")
                .contains("data-store-shell-menu-button")
                .contains("data-store-shell-search-form")
                .contains("data-store-shell-count=\"bookmark\"")
                .contains("data-store-shell-count=\"compare\"")
                .contains("th:fragment=\"footer\"");
        assertThat(script)
                .contains("front-bookmark-products")
                .contains("front-compare-products")
                .contains("function toggleMenu()")
                .contains("function openSearch()")
                .contains("encodeURIComponent(keyword)")
                .contains("window.addEventListener(\"storage\"")
                .contains("document.addEventListener(\"storefront:storage-change\"");
        assertThat(stateScript)
                .contains("window.StorefrontState")
                .contains("front-bookmark-products")
                .contains("front-compare-products")
                .contains("front-recent-viewed-products")
                .contains("new CustomEvent(\"storefront:storage-change\"")
                .contains("Object.freeze({ keys, read, write, remove, count, notify })");
        assertThat(css)
                .contains("--store-shell-width: 1200px")
                .contains(".store-shell__primary")
                .contains(".store-shell__category")
                .contains(".store-shell__search")
                .contains(".store-footer__grid")
                .contains("@media (max-width: 767px)");

        for (String page : pages) {
            assertThat(readResource("templates/views/" + page))
                    .as(page)
                    .contains("storefront-page")
                    .contains("/css/storefront-shell.css?v=20260726.2")
                    .contains("fragments/storefront-shell :: header(")
                    .contains("fragments/storefront-shell :: footer")
                    .contains("/js/view/storefront-state.js?v=20260726.1")
                    .contains("/js/view/storefront-shell.js?v=20260726.2");
        }
    }

    @Test
    void publicContentHighlightsKeepIndependentAccessibleRenderingContract() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"contentHighlights\"")
                .contains("id=\"popularHighlightList\"")
                .contains("id=\"popularHighlightRange\"")
                .contains("id=\"noticeHighlightList\"")
                .contains("id=\"styleHighlightList\"")
                .contains("id=\"contentHighlightStatus\" aria-live=\"polite\"")
                .contains("id=\"contentHighlightRetryButton\"");
        assertThat(script)
                .contains("void loadContentHighlights()")
                .contains("/api/front/content/highlights?limit=4")
                .contains("renderPopularContentHighlights(popular)")
                .contains("markupSafeObject(rawItem)")
                .contains("renderContentHighlightState(\"ERROR\")")
                .contains("contentHighlightRetryButton?.addEventListener");
        assertThat(css)
                .contains(".content-highlights__grid")
                .contains("grid-template-columns: repeat(2, minmax(0, 1fr))")
                .contains(".content-highlight-board--popular")
                .contains("grid-template-columns: repeat(4, minmax(0, 1fr))")
                .contains(".content-popular-list.is-error")
                .contains(".content-highlight-retry[hidden]");
    }

    @Test
    void mainPageKeepsStorefrontStructureAndAccessibleDialogHooks() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String homeCss = readResource("static/css/storefront-home.css");

        assertThat(html)
                .contains("/css/storefront.css?v=20260726.1")
                .contains("/css/storefront-home.css?v=20260726.2")
                .contains("class=\"storefront-home\"")
                .contains("id=\"headerSearchPanel\"")
                .contains("id=\"headerSearchPanel\" role=\"dialog\" aria-modal=\"true\"")
                .contains("id=\"headerSearchTitle\"")
                .contains("<form role=\"search\" aria-label=\"헤더 상품 검색\">")
                .contains("href=\"/front/content\">STYLE")
                .contains("data-storefront-count=\"bookmark\"")
                .contains("data-storefront-count=\"compare\"")
                .contains("id=\"homeCategoryRail\"")
                .contains("id=\"heroNextButton\"")
                .contains("id=\"catalogMemoryTools\"")
                .contains("role=\"dialog\"")
                .contains("aria-modal=\"true\"")
                .contains("aria-labelledby=\"drawerTitle\"")
                .contains("/js/view/storefront-state.js?v=20260726.1")
                .contains("/js/view/app.js?v=20260726.2");
        assertThat(script)
                .contains("window.StorefrontState?.keys.bookmark")
                .contains("window.StorefrontState.write(\"bookmark\"")
                .contains("window.StorefrontState.write(\"compare\"")
                .contains("headerSearchPanel?.querySelector(\"form\")?.addEventListener(\"submit\"")
                .contains("document.body.classList.add(\"is-header-search-open\")")
                .contains("document.body.classList.remove(\"is-header-search-open\")");
        assertThat(homeCss)
                .contains("body.storefront-home.is-header-search-open")
                .contains("overflow: hidden");
    }

    @Test
    void homeProductRailsKeepPinnedIconSwiperAndFallbackContract() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("/vendor/swiper/12.2.0/swiper-bundle.min.css")
                .contains("/vendor/lucide/1.26.0/lucide.min.js")
                .contains("/vendor/swiper/12.2.0/swiper-bundle.min.js")
                .contains("id=\"latestDropPreviousButton\"")
                .contains("id=\"latestDropNextButton\"")
                .contains("id=\"lowStockPreviousButton\"")
                .contains("id=\"lowStockNextButton\"")
                .contains("class=\"rail-action-menu\"")
                .contains("class=\"product-rail-more\"")
                .contains("data-lucide=\"chevron-left\"")
                .contains("data-lucide=\"chevron-right\"");
        assertThat(script)
                .contains("const productRailSwipers = new Map()")
                .contains("function renderProductSwiper(")
                .contains("function destroyProductSwiper(")
                .contains("typeof window.Swiper !== \"function\"")
                .contains("slidesPerView: 1.25")
                .contains("0: { slidesPerView: 1.25, spaceBetween: 12 }")
                .contains("observer: true")
                .contains("observeParents: true")
                .contains("new window.IntersectionObserver")
                .contains("swiper.update()")
                .contains("keyboard: { enabled: true, onlyInViewport: true }")
                .contains("navigation: { prevEl: previousButton, nextEl: nextButton }")
                .contains("current?.swiper?.destroy(true, true)")
                .contains("function refreshLucideIcons()")
                .contains("window.lucide?.createIcons")
                .contains("data-lucide=\"heart\"");
        assertThat(css)
                .contains(".signal-feed.swiper")
                .contains(".signal-strip > .discovery-card")
                .contains("min-width: 0")
                .contains("max-width: 100%")
                .contains(".signal-feed.is-grid-fallback .swiper-wrapper")
                .contains(".product-rail-navigation")
                .contains(".product-rail-pagination")
                .contains(".lucide-ready .rail-product-card__wish > span");
    }

    @Test
    void detailPageKeepsCommerceVisualAndPrimaryActionHooks() throws IOException {
        String html = readResource("templates/views/product-detail.html");

        assertThat(html)
                .contains("/css/storefront.css?v=20260725.4")
                .contains("id=\"detailProductVisual\"")
                .contains("id=\"detailVisualModel\"")
                .contains("id=\"detailPrimaryAction\"")
                .contains("id=\"detailAddCartButton\"")
                .contains("id=\"detailBuyNowButton\"")
                .contains("id=\"detailGuide\"")
                .contains("id=\"detailGuideDescription\"")
                .contains("/js/view/detail.js?v=20260727.1");
    }

    @Test
    void orderLookupKeepsPrivateVerificationAndTrackingHooks() throws IOException {
        String html = readResource("templates/views/order-lookup.html");
        String script = readResource("static/js/view/order-lookup.js");
        String css = readResource("static/css/commerce.css");

        assertThat(html)
                .contains("id=\"orderLookupForm\"")
                .contains("id=\"orderProgress\"")
                .contains("id=\"orderDelivery\"")
                .contains("id=\"loadRecentOrderButton\"")
                .contains("id=\"copyOrderNumberButton\"")
                .contains("id=\"copyTrackingButton\"")
                .contains("id=\"printOrderButton\"")
                .contains("/css/commerce.css?v=20260727.1")
                .contains("/js/view/order-lookup.js?v=20260727.1");
        assertThat(script)
                .contains("fetch(\"/api/front/orders/lookup\"")
                .contains("JSON.stringify({ orderNumber, phone })")
                .contains("grade-stock-last-order")
                .contains("navigator.clipboard.writeText")
                .contains("window.print()")
                .contains("order.statusStep")
                .contains("lookupController?.abort()")
                .contains("signal: lookupController.signal")
                .contains("function clearOrderResult()")
                .contains("document.getElementById(\"orderDelivery\").replaceChildren()")
                .contains("window.history.replaceState(null, \"\", \"/front/orders\")")
                .contains("\"/images/product-placeholder.svg\"")
                .doesNotContain("placehold.co");
        assertThat(css)
                .contains(".order-result__grid")
                .contains(".order-progress li.is-current");
    }

    @Test
    void checkoutKeepsSuccessfulOrderVisibleWhenSessionStorageIsUnavailable() throws IOException {
        String html = readResource("templates/views/checkout.html");
        String script = readResource("static/js/view/commerce.js");

        assertThat(html)
                .contains("id=\"deliveryRequestPreset\"")
                .contains("data-field-counter=\"deliveryRequest\"")
                .contains("id=\"commerceTotalQuantity\"")
                .contains("/css/commerce.css?v=20260727.1")
                .contains("/js/view/commerce.js?v=20260727.2")
                .contains("role=\"dialog\"")
                .contains("aria-modal=\"true\"")
                .contains("id=\"completedOrderTitle\"");
        assertThat(script)
                .contains("window.sessionStorage.setItem(\"grade-stock-last-order\"")
                .contains("저장소 접근이 제한돼도 서버에서 완료된 주문 결과는 그대로 표시한다.")
                .contains("elements.form.reset()")
                .contains("cart = { items: [], itemCount: 0, totalQuantity: 0, totalAmount: 0 }")
                .contains("document.getElementById(\"completedOrderTitle\")?.focus()")
                .contains("formatPhoneInput")
                .contains("syncBuyerToRecipient")
                .contains("elements.form.hidden = false")
                .contains("elements.complete.hidden = false");
    }

    @Test
    void cartKeepsBulkClearAndStockWarningHooks() throws IOException {
        String html = readResource("templates/views/cart.html");
        String script = readResource("static/js/view/commerce.js");

        assertThat(html)
                .contains("id=\"clearCartButton\"")
                .contains("id=\"commerceStockSummary\"")
                .contains("id=\"commerceTotalQuantity\"")
                .contains("/css/commerce.css?v=20260727.1")
                .contains("/js/view/commerce.js?v=20260727.2");
        assertThat(script)
                .contains("request(\"/api/front/cart/items\", { method: \"DELETE\" })")
                .contains("commerce-stock-badge")
                .contains("aria-busy")
                .contains("data-cart-retry")
                .contains("if (cartMutating) return")
                .contains("syncCheckoutAvailability(false)")
                .contains("\"/images/product-placeholder.svg\"")
                .doesNotContain("placehold.co");
    }

    @Test
    void myActivityKeepsFourBoardsAndBulkManagementHooks() throws IOException {
        String html = readResource("templates/views/my-activity.html");
        String script = readResource("static/js/view/my-activity.js");
        String css = readResource("static/css/my-activity.css");

        assertThat(html)
                .contains("data-tab=\"recent\"")
                .contains("data-tab=\"wishlist\"")
                .contains("data-tab=\"compare\"")
                .contains("data-tab=\"hidden\"")
                .contains("id=\"myDeleteSelectedButton\"")
                .contains("id=\"myExportButton\"")
                .contains("/css/my-activity.css?v=20260726.2")
                .contains("/js/view/my-activity.js?v=20260727.1");
        assertThat(script)
                .contains("front-recent-viewed-products")
                .contains("front-bookmark-products")
                .contains("front-compare-products")
                .contains("front-hidden-products")
                .contains("downloadCsv")
                .contains("navigator.clipboard.writeText")
                .contains("addEventListener(\"storage\"")
                .contains("Number.isSafeInteger(Number(item?.id))")
                .contains("\"/images/product-placeholder.svg\"")
                .contains("function bindImageFallbacks()")
                .contains("function resetAllActivity()")
                .contains("window.StorefrontState.remove(KEYS[tab])")
                .doesNotContain("placehold.co");
        assertThat(css)
                .contains(".my-grid.is-list")
                .contains(".my-selection[hidden]")
                .contains("@media (max-width:780px)");
    }

    @Test
    void supportCenterKeepsSearchFaqAndPublicNoticeContracts() throws IOException {
        String home = readResource("templates/views/index.html");
        String html = readResource("templates/views/support-center.html");
        String script = readResource("static/js/view/support-center.js");
        String css = readResource("static/css/support-center.css");

        assertThat(home)
                .contains("href=\"/front/support\">고객센터")
                .contains("href=\"/front/support?view=notice\">공지사항")
                .contains("href=\"/front/support?view=faq\">자주 묻는 질문")
                .contains("href=\"/front/support#supportContact\">문의하기");
        assertThat(html)
                .contains("id=\"supportSearchForm\"")
                .contains("data-support-topic=\"SHOPPING\"")
                .contains("data-support-topic=\"ORDER\"")
                .contains("data-support-view=\"faq\"")
                .contains("data-support-view=\"notice\"")
                .contains("id=\"supportExpandAllButton\"")
                .contains("id=\"supportNoticePagination\"")
                .contains("id=\"supportCopySummaryButton\"")
                .contains("/css/support-center.css?v=20260726.1")
                .contains("/js/view/support-center.js?v=20260726.1");
        assertThat(script)
                .contains("grade-stock-support-searches")
                .contains("boardType: \"NOTICE\"")
                .contains("/api/front/content?${params}")
                .contains("noticeController?.abort()")
                .contains("compactPageIndexes(totalPages, currentPage)")
                .contains("state.expandedFaqIds")
                .contains("navigator.clipboard?.writeText")
                .contains("window.addEventListener(\"popstate\"")
                .contains("event.key === \"/\"");
        assertThat(css)
                .contains(".support-layout")
                .contains("grid-template-columns: 220px minmax(0, 1fr)")
                .contains(".support-faq h3 > button")
                .contains(".support-body [hidden]")
                .contains("@media (max-width: 620px)");
    }

    @Test
    void brandDirectoryKeepsFacetMetricsPagingAndPersonalBoardContracts() throws IOException {
        String home = readResource("templates/views/index.html");
        String app = readResource("static/js/view/app.js");
        String html = readResource("templates/views/brand-directory.html");
        String script = readResource("static/js/view/brand-directory.js");
        String css = readResource("static/css/brand-directory.css");

        assertThat(home)
                .contains("href=\"/front/brands\">브랜드")
                .doesNotContain("data-home-target=\"brandSpotlightGrid\"");
        assertThat(app)
                .contains("href=\"/front/brands?brand=${encodeURIComponent(item.brand)}\"")
                .doesNotContain("data-brand-rank=");
        assertThat(html)
                .contains("id=\"brandSearchForm\"")
                .contains("data-brand-letter=\"POPULAR\"")
                .contains("data-brand-letter=\"SAVED\"")
                .contains("id=\"brandProfile\"")
                .contains("id=\"brandCategoryBars\"")
                .contains("id=\"brandProductGrid\"")
                .contains("id=\"brandProductSize\"")
                .contains("id=\"brandPageSelect\"")
                .contains("id=\"brandSelectionBar\"")
                .contains("/css/brand-directory.css?v=20260726.1")
                .contains("/js/view/brand-directory.js?v=20260726.2");
        assertThat(script)
                .contains("/api/front/catalog/bootstrap?page=0&size=1")
                .contains("fetch(`/api/front/catalog/bootstrap?${productParams()}`")
                .contains("fetch(`/api/front/products?${productParams()}`")
                .contains("productController?.abort()")
                .contains("front-saved-brands")
                .contains("front-bookmark-products")
                .contains("front-compare-products")
                .contains("selectedProductIds.size >= 3")
                .contains("compactPageIndexes(pagination.totalPages, state.page)")
                .contains("window.addEventListener(\"popstate\"")
                .contains("clearBrandWorkspace()")
                .contains("navigator.clipboard?.writeText");
        assertThat(css)
                .contains(".brand-card-grid")
                .contains("grid-template-columns: repeat(4, minmax(0, 1fr))")
                .contains(".brand-product-toolbar")
                .contains(".brand-selection-bar")
                .contains(".brand-body [hidden]")
                .contains("@media (max-width: 680px)");
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
                .contains("class=\"detail-body storefront-page\"")
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
                .contains("if (cartSubmitting)")
                .contains("function setCartSubmitting(")
                .contains("memoryCartToken ||= createCartToken()")
                .contains("button?.toggleAttribute(\"disabled\", submitting)")
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
                .contains("rail-product-card__detail")
                .contains("상품 더보기")
                .contains("return list.slice(0, 4)");
        assertThat(css)
                .contains(".signal-feed")
                .contains(".rail-product-card")
                .contains("grid-template-columns: repeat(4, minmax(0, 1fr))");
    }

    @Test
    void productCollectionKeepsIndependentNavigationServerPagingAndDetailCta() throws IOException {
        String html = readResource("templates/views/product-collection.html");
        String shell = readResource("templates/fragments/storefront-shell.html");
        String script = readResource("static/js/view/product-collection.js");
        String css = readResource("static/css/product-collection.css");

        assertThat(html)
                .contains("data-collection-type")
                .contains("id=\"collectionSearchForm\" role=\"search\"")
                .contains("id=\"collectionSearchInput\"")
                .contains("id=\"collectionGrid\"")
                .contains("id=\"collectionPreviousButton\"")
                .contains("/css/product-collection.css?v=20260727.1")
                .contains("/js/view/product-collection.js?v=20260727.1")
                .contains("fragments/storefront-shell :: header('SHOP', '상품 컬렉션')");
        assertThat(shell)
                .contains("/front/collections/recommended")
                .contains("/front/collections/fast-delivery");
        assertThat(script)
                .contains("/api/front/products?")
                .contains("page: state.page")
                .contains("size: state.size")
                .contains("상품 더보기")
                .contains("productController?.abort()")
                .contains("signal: productController.signal")
                .contains("data-collection-retry")
                .contains("storefront:storage-change")
                .contains("function syncBookmarkButtons()")
                .contains("fast-delivery");
        assertThat(css)
                .contains(".collection-grid")
                .contains("grid-template-columns: repeat(4, minmax(0, 1fr))")
                .contains(".collection-product__detail")
                .contains(".collection-state__retry");
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
    void mobileShoppingMemoryStaysInsideCatalogBoundary() throws IOException {
        String css = readResource("static/css/storefront.css");

        assertThat(css)
                .contains(".catalog {\n        overflow-x: clip;")
                .contains("grid-template-columns: minmax(0, 1fr) auto")
                .contains("max-width: min(48vw, 176px)")
                .contains("overscroll-behavior-inline: contain")
                .contains("width: min(170px, calc(100vw - 40px))");
    }

    @Test
    void mobileCatalogActionsShareHorizontalNavigationContract() throws IOException {
        String css = readResource("static/css/storefront.css");

        assertThat(css)
                .contains(".catalog-preset-strip,\n    .section-action-bar")
                .contains(".catalog-display-panel__actions,\n    .catalog-selection__actions")
                .contains("scroll-snap-type: inline proximity")
                .contains("touch-action: pan-x")
                .contains("scroll-snap-align: start");
    }

    @Test
    void mobileCatalogMetricsShareTwoColumnContract() throws IOException {
        String css = readResource("static/css/storefront.css");

        assertThat(css)
                .contains(".catalog-insight-grid,\n    .catalog-live-metrics")
                .contains(".catalog-selection__metrics,\n    .catalog-selection__coverage")
                .contains("grid-template-columns: repeat(2, minmax(0, 1fr))")
                .contains(".catalog-page-metrics > :last-child:nth-child(odd)")
                .contains("text-overflow: ellipsis");
    }

    @Test
    void storefrontDefersOffscreenSectionsAndRespectsReducedMotion() throws IOException {
        String css = readResource("static/css/storefront.css");

        assertThat(css)
                .contains("@supports (content-visibility: auto)")
                .contains("content-visibility: auto")
                .contains("contain-intrinsic-block-size: auto 640px")
                .contains("contain: layout style")
                .contains("object-fit: cover")
                .contains("scroll-snap-type: none");
    }

    @Test
    void relatedDetailKeepsFivePrimaryActionsAndScrollableManagementMenu() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String css = readResource("static/css/storefront.css");
        String actions = html.substring(
                html.indexOf("<div class=\"detail-section-actions\">", html.indexOf("id=\"detailRelated\"")),
                html.indexOf("<details class=\"board-action-menu detail-section-menu\">", html.indexOf("id=\"detailRelated\"")));

        assertThat(actions).containsOnlyOnce("detailPreviousRelatedButton")
                .containsOnlyOnce("detailNextRelatedButton")
                .containsOnlyOnce("detailRelatedSameBrandButton")
                .containsOnlyOnce("detailRelatedSameCategoryButton")
                .containsOnlyOnce("detailRelatedAvailableOnlyButton")
                .doesNotContain("detailRelatedCheaperOnlyButton");
        assertThat(html).contains("<summary>필터·정렬·관리</summary>");
        assertThat(css)
                .contains("max-height: min(520px, calc(100vh - 160px))")
                .contains("overflow-y: auto")
                .contains("scrollbar-gutter: stable");
    }

    @Test
    void detailSectionHeadingsKeepFullWidthSingleRowActions() throws IOException {
        String css = readResource("static/css/storefront.css");

        assertThat(css)
                .contains(".detail-section > .section-heading")
                .contains("grid-template-columns: minmax(0, 1fr)")
                .contains(".detail-section > .section-heading > div:not(.detail-section-actions)")
                .contains(".detail-section > .section-heading > .detail-section-actions")
                .contains("display: flex")
                .contains("overscroll-behavior-inline: contain")
                .contains("flex: 0 0 auto")
                .contains(".board-action-menu:not([open]) > .board-action-menu__panel")
                .contains("display: none");
    }

    @Test
    void recentDetailKeepsThreePrimaryActionsAndGroupedManagementMenu() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String css = readResource("static/css/storefront.css");
        int recentSection = html.indexOf("id=\"detailRecentSection\"");
        String actions = html.substring(
                html.indexOf("<div class=\"detail-section-actions\">", recentSection),
                html.indexOf("<details class=\"board-action-menu detail-section-menu detail-recent-menu\">", recentSection));

        assertThat(actions)
                .contains("detailRecentSortPriceButton")
                .contains("detailRecentSortStockButton")
                .contains("detailRecentAvailableOnlyButton")
                .doesNotContain("detailRecentCompareAllButton")
                .doesNotContain("clearDetailRecentButton");
        assertThat(html)
                .contains("<summary>비교·이동·내보내기</summary>")
                .contains("catalog-reset-button board-action-menu__danger")
                .contains("id=\"copyDetailRecentLinksButton\"");
        assertThat(css)
                .contains(".detail-section .detail-section-menu .board-action-menu__panel")
                .contains(".detail-recent-menu .board-action-menu__danger");
    }

    @Test
    void mobileDetailKeepsFullWidthActionsAndTwoColumnOptionGuides() throws IOException {
        String css = readResource("static/css/storefront.css");

        assertThat(css)
                .contains(".page-shell--detail .detail-section")
                .contains("padding: 20px 0")
                .contains(".detail-section > .section-heading > .detail-section-actions > button")
                .contains("min-width: max-content")
                .contains(".detail-option-overview > .detail-option-stock-rate")
                .contains(".detail-option-distribution > :last-child:nth-child(odd)")
                .contains("grid-template-columns: repeat(2, minmax(0, 1fr))");
    }

    @Test
    void productDrawerKeepsStableFocusBusyAndRequestLifecycle() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");

        assertThat(html)
                .contains("aria-busy=\"false\"")
                .contains("aria-describedby=\"drawerStatus\"")
                .contains("id=\"drawerStatus\"");
        assertThat(script)
                .contains("const isNewDrawerSession")
                .contains("const requestSequence = ++drawerRequestSequence")
                .contains("requestSequence !== drawerRequestSequence")
                .contains("setDrawerBackgroundInert(true)")
                .contains("element.inert = isInert")
                .contains("drawerReturnFocus?.isConnected");
    }

    @Test
    void detailImageModalKeepsFocusIsolationAndScrollContainment() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("aria-describedby=\"detailImageModalHint\"")
                .contains("id=\"detailImageModalHint\"");
        assertThat(script)
                .contains("setDetailModalBackgroundInert(true)")
                .contains("detailMobileActions: document.getElementById(\"detailMobileActions\")")
                .contains("element.inert = isInert")
                .contains("keepFocusInsideDetailImageModal")
                .contains("detailModalReturnFocus?.isConnected")
                .contains("elements.detailImageModalCloseButton?.focus()");
        assertThat(css)
                .contains("overscroll-behavior: contain")
                .contains("touch-action: none")
                .contains("touch-action: auto");
    }

    @Test
    void mobileDetailActionsKeepSafeAreaTouchAndPriceContracts() throws IOException {
        String css = readResource("static/css/storefront.css");

        assertThat(css)
                .contains("padding-bottom: calc(88px + env(safe-area-inset-bottom))")
                .contains("min-height: calc(72px + env(safe-area-inset-bottom))")
                .contains("padding-bottom: calc(8px + env(safe-area-inset-bottom))")
                .contains("min-height: 48px")
                .contains(".detail-mobile-actions__primary > span")
                .contains("text-overflow: ellipsis")
                .contains("bottom: calc(88px + env(safe-area-inset-bottom))");
    }

    @Test
    void shortcutHelpKeepsFocusIsolationDescriptionAndScrollContainment() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("aria-describedby=\"shortcutHelpHint\"")
                .contains("id=\"shortcutHelpHint\"");
        assertThat(script)
                .contains("setShortcutHelpBackgroundInert(true)")
                .contains("keepFocusInsideShortcutHelp")
                .contains("shortcutHelpReturnFocus?.isConnected")
                .contains("elements.shortcutHelpCloseButton?.focus()");
        assertThat(css)
                .contains(".shortcut-help-modal")
                .contains("overscroll-behavior: contain")
                .contains("touch-action: none")
                .contains("touch-action: auto");
    }

    @Test
    void headerSearchKeepsSemanticSafeFocusAndMobileScrollContract() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html).contains("role=\"search\" aria-label=\"헤더 상품 검색\"");
        assertThat(script)
                .contains("let headerSearchReturnFocus = null")
                .contains("function closeHeaderSearch(restoreFocus = false)")
                .contains("headerSearchReturnFocus?.isConnected")
                .contains("closeHeaderSearch(true)")
                .contains("elements.headerSearchPanel.hidden");
        assertThat(css)
                .contains("max-height: calc(100dvh - 96px - env(safe-area-inset-bottom))")
                .contains("scrollbar-gutter: stable");
    }

    @Test
    void mobileMenuKeepsExplicitFocusLabelAndArrowNavigationContract() throws IOException {
        String script = readResource("static/js/view/app.js");

        assertThat(script)
                .contains("let mobileMenuReturnFocus = null")
                .contains("function openMobileMenu()")
                .contains("function closeMobileMenu(restoreFocus = false)")
                .contains("setAttribute(\"aria-label\", \"메뉴 닫기\")")
                .contains("handleMobileMenuKeyboard")
                .contains("[\"ArrowLeft\", \"ArrowRight\", \"Home\", \"End\"]")
                .contains("window.innerWidth > 768");
    }

    @Test
    void toastAndNetworkStatusKeepAccessibleBoundedMobileContract() throws IOException {
        String mainScript = readResource("static/js/view/app.js");
        String detailScript = readResource("static/js/view/detail.js");
        String css = readResource("static/css/storefront.css");

        assertThat(mainScript)
                .contains("toast.setAttribute(\"role\", isWarning ? \"alert\" : \"status\")")
                .contains("while (stack.childElementCount >= 3)")
                .contains("closeButton.setAttribute(\"aria-label\"")
                .contains("titleElement.textContent = String(title || \"\")")
                .contains("stack.setAttribute(\"aria-label\", \"화면 알림\")");
        assertThat(detailScript)
                .contains("while (stack.childElementCount >= 3)")
                .contains("toast.querySelector(\"button\")?.addEventListener");
        assertThat(css)
                .contains("z-index: 200")
                .contains("bottom: calc(76px + env(safe-area-inset-bottom))")
                .contains(".detail-body .toast-stack")
                .contains(".network-status");
    }

    @Test
    void storefrontLayersKeepMutualExclusionAndSharedBodyState() throws IOException {
        String script = readResource("static/js/view/app.js");

        assertThat(script)
                .contains("function syncBodyLayerState()")
                .contains("classList.toggle(\"has-open-modal\", hasOpenModal)")
                .contains("elements.productDrawer?.classList.contains(\"is-open\")")
                .contains("elements.shortcutHelpModal?.classList.contains(\"is-open\")")
                .contains("closeDrawer();\n        closeShortcutHelp();\n        closeMobileMenu();")
                .contains("closeShortcutHelp();\n        closeHeaderSearch();\n        closeMobileMenu();")
                .contains("if (!elements.productDrawer?.classList.contains(\"is-open\"))");
    }

    @Test
    void storefrontProvidesLocalProductFallbackAsset() {
        ClassPathResource resource = new ClassPathResource("static/images/product-placeholder.svg");

        assertThat(resource.exists()).isTrue();
        assertThat(resource.getFilename()).isEqualTo("product-placeholder.svg");
    }

    @Test
    void storefrontImagesKeepMissingAndBrokenSourceRecoveryContract() throws IOException {
        String script = readResource("static/js/view/app.js");

        assertThat(script)
                .contains("const PRODUCT_IMAGE_FALLBACK_URL = \"/images/product-placeholder.svg\"")
                .contains("const imageSource = thumbnail || PRODUCT_IMAGE_FALLBACK_URL")
                .contains("decoding=\"async\"")
                .contains("data-image-fallback=\"true\"")
                .contains("event.target.dataset.imageFallback === \"true\"")
                .contains("event.target.src = PRODUCT_IMAGE_FALLBACK_URL")
                .contains("visual?.classList.add(\"is-image-fallback\")");
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
                .contains("is-image-fallback")
                .contains("대체 이미지")
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
                .contains("shortcutHelpReturnFocus?.isConnected")
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
                .contains("aria-setsize=\"${details.total}\"")
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
    void catalogPaginationUsesServerPageMetadataAndPageOnlyEndpoint() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");

        assertThat(html)
                .contains("<option value=\"48\">48개</option>")
                .doesNotContain("<option value=\"ALL\">전체</option>");
        assertThat(script)
                .contains("const endpoint = includeSummary ? \"/api/front/catalog/bootstrap\" : \"/api/front/products\"")
                .contains("page: Math.max(0, paginationState.page - 1)")
                .contains("totalElements")
                .contains("await loadProducts(false)")
                .doesNotContain("return list.slice(start, start + details.effectiveSize)");
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

    @Test
    void catalogSelectionKeepsSessionTotalsAndSelectedCsvContracts() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"catalogSelectionTotalPrice\"")
                .contains("id=\"catalogSelectionTotalStock\"")
                .contains("id=\"exportSelectedProductsCsvButton\"");
        assertThat(script)
                .contains("SELECTED_PRODUCTS_SESSION_KEY")
                .contains("function persistSelectedProductIds()")
                .contains("function restoreSelectedProductIds()")
                .contains("setText(elements.catalogSelectionTotalPrice")
                .contains("setText(elements.catalogSelectionTotalStock")
                .contains("exportProductsCsv(selectedProducts()");
        assertThat(css).contains(".catalog-selection__metrics");
    }

    @Test
    void productDrawerKeepsScopedKeyboardNavigationAndActionsContracts() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");

        assertThat(html)
                .contains("aria-keyshortcuts=\"ArrowLeft ArrowRight B C L\"")
                .contains("드로어 ← / → · B / C / L");
        assertThat(script)
                .contains("activeDrawerProductId")
                .contains("previousDrawerProductId")
                .contains("nextDrawerProductId")
                .contains("function handleDrawerKeyboardShortcut(event)")
                .contains("toggleBookmarkProduct(activeDrawerProductId)")
                .contains("toggleCompareProduct(activeDrawerProductId)")
                .contains("copyTextWithFeedback(url, \"상품 링크를 복사했습니다.\"");
    }

    @Test
    void detailQuantityKeepsUnitRemainingMaxResetAndKeyboardContracts() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("aria-keyshortcuts=\"- +\"")
                .contains("id=\"detailUnitPrice\"")
                .contains("id=\"detailRemainingStock\"")
                .contains("id=\"detailQuantityMaxButton\"")
                .contains("id=\"detailQuantityResetButton\"");
        assertThat(script)
                .contains("setElementText(elements.detailUnitPrice")
                .contains("setElementText(elements.detailRemainingStock")
                .contains("setSelectedQuantity(option?.stock || 1)")
                .contains("setSelectedQuantity(1)")
                .contains("[\"-\", \"+\", \"=\"].includes(event.key)");
        assertThat(css)
                .contains(".detail-purchase-metrics")
                .contains(".detail-purchase-actions");
    }

    @Test
    void relatedProductsKeepCategoryAvailabilityStockSpreadAndResetContracts() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");

        assertThat(html)
                .contains("id=\"detailRelatedSameCategoryButton\"")
                .contains("id=\"detailRelatedAvailableOnlyButton\"")
                .contains("id=\"detailRelatedPriceSpread\"")
                .contains("id=\"detailRelatedTotalStock\"")
                .contains("id=\"detailResetRelatedFiltersButton\"");
        assertThat(script)
                .contains("sameCategoryOnly: false")
                .contains("availableOnly: false")
                .contains("item.category === product.category")
                .contains("setElementText(elements.detailRelatedPriceSpread")
                .contains("setElementText(elements.detailRelatedTotalStock")
                .contains("Object.assign(relatedSortState");
    }

    @Test
    void catalogSummaryKeepsLivePriceStockBrandAndRiskMetrics() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"catalogAveragePrice\"")
                .contains("id=\"catalogPriceRange\"")
                .contains("id=\"catalogTotalStock\"")
                .contains("id=\"catalogBrandCount\"")
                .contains("id=\"catalogLowStockCount\"");
        assertThat(script)
                .contains("const averagePrice = Number(metrics.averagePrice || 0)")
                .contains("Number(metrics.brandCount || 0)")
                .contains("setText(elements.catalogLowStockCount");
        assertThat(css).contains(".catalog-live-metrics");
    }

    @Test
    void catalogSelectionKeepsAverageBrandRiskSoldOutAndCleanupContracts() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");

        assertThat(html)
                .contains("id=\"catalogSelectionAveragePrice\"")
                .contains("id=\"catalogSelectionBrandCount\"")
                .contains("id=\"catalogSelectionLowStockCount\"")
                .contains("id=\"catalogSelectionSoldOutCount\"")
                .contains("id=\"removeSoldOutSelectionButton\"");
        assertThat(script)
                .contains("const selectedSoldOut = selected.filter")
                .contains("setText(elements.catalogSelectionAveragePrice")
                .contains("soldOutIds.forEach((productId) => selectedProductIds.delete(productId))");
    }

    @Test
    void detailPurchaseKeepsQuantityPresetsStockUsageAndWarningContracts() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"detailQuantityPresetTwoButton\"")
                .contains("id=\"detailQuantityPresetThreeButton\"")
                .contains("id=\"detailQuantityPresetFiveButton\"")
                .contains("id=\"detailStockUsageRate\"")
                .contains("id=\"detailQuantityNotice\"");
        assertThat(script)
                .contains("const stockUsageRate = Math.min(100")
                .contains("stockUsageRate >= 50")
                .contains("setSelectedQuantity(5)");
        assertThat(css).contains(".detail-stock-usage");
    }

    @Test
    void relatedComparisonKeepsRelativeMetricsFilterStatusAndBalancedRecommendation() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"detailRelatedCheaperCount\"")
                .contains("id=\"detailRelatedHigherStockCount\"")
                .contains("id=\"detailRelatedSoldOutCount\"")
                .contains("id=\"detailRelatedFilterStatus\"")
                .contains("id=\"detailBalancedRelatedButton\"");
        assertThat(script)
                .contains("function openBalancedRelatedProduct()")
                .contains("Number(left.price || 0) / Math.max(1, Number(left.stock || 0))")
                .contains("activeLabels.join(\" · \") || \"기본 추천\"");
        assertThat(css).contains(".detail-related-filter-status");
    }

    @Test
    void catalogKeepsDecisionRailForPriceStockBrandAndCopyActions() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"openCheapestCatalogButton\"")
                .contains("id=\"openHighestStockCatalogButton\"")
                .contains("id=\"openUrgentCatalogButton\"")
                .contains("id=\"focusDominantCatalogBrandButton\"")
                .contains("id=\"copyCatalogDecisionButton\"");
        assertThat(script)
                .contains("function catalogDecisionProduct(list, metric)")
                .contains("function catalogDecisionSummary(list)")
                .contains("openCatalogDecisionProduct(\"STOCK_LOW\")");
        assertThat(css).contains(".catalog-decision-rail");
    }

    @Test
    void catalogSelectionKeepsRankedPageAndSelectedProductActions() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"selectCheapestPageButton\"")
                .contains("id=\"selectHighestStockPageButton\"")
                .contains("id=\"selectSoldOutPageButton\"")
                .contains("id=\"openCheapestSelectedButton\"")
                .contains("id=\"openHighestStockSelectedButton\"");
        assertThat(script)
                .contains("function selectRankedPageProducts(metric, limit)")
                .contains("function openSelectedProductByMetric(metric)")
                .contains("selectCurrentPageProducts((product) => Number(product.stock || 0) <= 0");
        assertThat(css).contains(".catalog-selection__priority");
    }

    @Test
    void detailOptionsKeepStockRangeMedianRankAndMatrixContracts() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");

        assertThat(html)
                .contains("id=\"detailMinOptionStock\"")
                .contains("id=\"detailMaxOptionStock\"")
                .contains("id=\"detailMedianOptionStock\"")
                .contains("id=\"detailSelectedOptionRank\"")
                .contains("id=\"detailCopyOptionMatrixButton\"");
        assertThat(script)
                .contains("const medianStock = optionStocks.length")
                .contains("const selectedRank = selectedOptionName")
                .contains("async function copyOptionStockMatrix()");
    }

    @Test
    void relatedProductsKeepSavingStockGainAvailabilityBrandAndCopyContracts() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");

        assertThat(html)
                .contains("id=\"detailRelatedMaxSaving\"")
                .contains("id=\"detailRelatedMaxStockGain\"")
                .contains("id=\"detailRelatedAvailableRate\"")
                .contains("id=\"detailRelatedSameBrandCount\"")
                .contains("id=\"detailCopyValueAnalysisButton\"");
        assertThat(script)
                .contains("const maxSaving = related.reduce")
                .contains("const maxStockGain = related.reduce")
                .contains("async function copyRelatedValueAnalysis()");
    }

    @Test
    void catalogSummaryKeepsFivePriceStockAndFeaturedDistributionBars() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"catalogUnderPriceRate\"")
                .contains("id=\"catalogMiddlePriceRate\"")
                .contains("id=\"catalogHighPriceRate\"")
                .contains("id=\"catalogLowStockRate\"")
                .contains("id=\"catalogFeaturedRate\"");
        assertThat(script)
                .contains("const distributionItems = [")
                .contains("Math.round((count / totalCount) * 100)")
                .contains("bar.style.width = `${rate}%`");
        assertThat(css).contains(".catalog-distribution");
    }

    @Test
    void catalogAnalyticsVisibilityKeepsFiveControlsAndPersistentState() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"toggleCatalogMetricsButton\"")
                .contains("id=\"toggleCatalogDistributionButton\"")
                .contains("id=\"toggleCatalogDecisionButton\"")
                .contains("id=\"compactCatalogAnalyticsButton\"")
                .contains("id=\"resetCatalogAnalyticsButton\"");
        assertThat(script)
                .contains("hideMetrics: Boolean(savedDisplayPreferences.hideMetrics)")
                .contains("hideDistribution: uiState.hideDistribution")
                .contains("catalogDecisionRail?.toggleAttribute(\"hidden\"")
                .contains("const analyticsCompact = uiState.hideMetrics && uiState.hideDistribution && uiState.hideDecision");
        assertThat(css).contains(".catalog-summary > [hidden]");
    }

    @Test
    void catalogSelectionKeepsCoveragePriceStockAvailabilityAndCategoryGuide() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"catalogSelectionCoverage\"")
                .contains("id=\"catalogSelectionPriceShare\"")
                .contains("id=\"catalogSelectionStockShare\"")
                .contains("id=\"catalogSelectionAvailableCount\"")
                .contains("id=\"catalogSelectionDominantCategory\"");
        assertThat(script)
                .contains("const filteredTotalPrice = filtered.reduce")
                .contains("setText(elements.catalogSelectionCoverage")
                .contains("dominantCategory(selected) || \"-\"");
        assertThat(css).contains(".catalog-selection__coverage");
    }

    @Test
    void catalogSelectionKeepsUndoRedoHistoryAndClearControls() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"catalogSelectionHistoryCount\"")
                .contains("id=\"catalogSelectionHistoryText\"")
                .contains("id=\"undoCatalogSelectionButton\"")
                .contains("id=\"redoCatalogSelectionButton\"")
                .contains("id=\"clearCatalogSelectionHistoryButton\"");
        assertThat(script)
                .contains("const selectionHistory = []")
                .contains("function recordSelectionSnapshot(label)")
                .contains("function undoCatalogSelection()")
                .contains("function redoCatalogSelection()")
                .contains("selectionHistory.length > 20");
        assertThat(css).contains(".catalog-selection__history");
    }

    @Test
    void detailOptionsKeepNavigationRecentHistoryCopyAndClearControls() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"detailPreviousOptionButton\"")
                .contains("id=\"detailNextOptionButton\"")
                .contains("id=\"detailOptionHistoryList\"")
                .contains("id=\"detailCopyOptionHistoryButton\"")
                .contains("id=\"detailClearOptionHistoryButton\"");
        assertThat(script)
                .contains("const optionSelectionHistory = []")
                .contains("function moveDetailOption(direction)")
                .contains("function recordOptionSelection(option)")
                .contains("optionSelectionHistory.length > 5")
                .contains("async function copyOptionHistory()");
        assertThat(css).contains(".detail-option-history");
    }

    @Test
    void relatedCardsKeepCategoryPriceStockValueAndPositionComparison() throws IOException {
        String script = readResource("static/js/view/detail.js");
        String css = readResource("static/css/storefront.css");

        assertThat(script)
                .contains("item.category || \"카테고리 미정\"")
                .contains("relatedPriceDeltaRateLabel(item.price, product.price)")
                .contains("relatedStockDeltaLabel(item.stock, product.stock)")
                .contains("relatedValueScore(item, product, related)")
                .contains("${index + 1} / ${related.length}")
                .contains("const priceScore = basePrice")
                .contains("(priceScore * 0.6) + (stockScore * 0.4)");
        assertThat(css).contains(".detail-related-card__comparison");
    }

    @Test
    void compareBoardKeepsFiveDecisionMetrics() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"compareAveragePrice\"")
                .contains("id=\"comparePriceGap\"")
                .contains("id=\"compareTotalStock\"")
                .contains("id=\"compareStockGap\"")
                .contains("id=\"compareCategoryCount\"");
        assertThat(script)
                .contains("const comparePrices = comparedProducts.map")
                .contains("setText(elements.compareAveragePrice")
                .contains("setText(elements.compareStockGap")
                .contains("new Set(comparedProducts.map((product) => product.category)");
        assertThat(css).contains(".saved-board-metrics");
    }

    @Test
    void catalogSearchKeepsFiveResultQualityGuides() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"catalogSearchNameMatch\"")
                .contains("id=\"catalogSearchBrandMatch\"")
                .contains("id=\"catalogSearchModelMatch\"")
                .contains("id=\"catalogSearchAvailableMatch\"")
                .contains("id=\"catalogSearchFilterCount\"");
        assertThat(script)
                .contains("function renderCatalogSearchQuality(list)")
                .contains("elements.catalogSearchQuality?.toggleAttribute(\"hidden\", !keyword)")
                .contains("key !== \"search\" && value !== DEFAULT_STATE[key]")
                .contains("setText(elements.catalogSearchAvailableMatch");
        assertThat(css).contains(".catalog-search-quality");
    }

    @Test
    void detailPurchaseEstimateKeepsFiveQuantityDecisionGuides() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"detailOptionRemainingRate\"")
                .contains("id=\"detailProductStockUsageRate\"")
                .contains("id=\"detailSafeQuantity\"")
                .contains("id=\"detailPurchaseUrgency\"")
                .contains("id=\"detailQuantityStatus\"");
        assertThat(script)
                .contains("const remainingRate = Math.max(0, 100 - stockUsageRate)")
                .contains("const productStockUsageRate = Math.min(100")
                .contains("const safeQuantity = Math.max(1, Math.floor(maxQuantity * 0.2))")
                .contains("selectedQuantity > safeQuantity ? \"대량 선택\" : \"권장 범위\"");
        assertThat(css).contains(".detail-purchase-guide");
    }

    @Test
    void recentBoardKeepsAvailabilitySoldOutOldestExpandAndLinkControls() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");

        assertThat(html)
                .contains("id=\"filterRecentAvailableButton\"")
                .contains("id=\"filterRecentSoldOutButton\"")
                .contains("id=\"sortRecentOldestButton\"")
                .contains("id=\"toggleRecentExpandedButton\"")
                .contains("id=\"copyRecentViewedLinksButton\"");
        assertThat(script)
                .contains("recentExpanded: false")
                .contains("boardState.recentFilter = \"AVAILABLE\"")
                .contains("boardState.recentFilter = \"SOLD_OUT\"")
                .contains("boardState.recentSort = \"OLDEST\"")
                .contains("boardState.recentExpanded ? sortedProducts : sortedProducts.slice(0, 3)")
                .contains("async () => {\n            const links = readRecentProducts()");
    }

    @Test
    void bookmarkBoardKeepsFiveFilteredDecisionMetrics() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");

        assertThat(html)
                .contains("id=\"bookmarkAveragePrice\"")
                .contains("id=\"bookmarkTotalStock\"")
                .contains("id=\"bookmarkLowStockCount\"")
                .contains("id=\"bookmarkFeaturedCount\"")
                .contains("id=\"bookmarkDominantCategory\"");
        assertThat(script)
                .contains("const bookmarkTotalPrice = bookmarkedProducts.reduce")
                .contains("setText(elements.bookmarkAveragePrice")
                .contains("setText(elements.bookmarkLowStockCount")
                .contains("dominantCategory(bookmarkedProducts) || \"-\"");
    }

    @Test
    void catalogPaginationKeepsFiveCurrentPageMetrics() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"catalogPageAveragePrice\"")
                .contains("id=\"catalogPageTotalStock\"")
                .contains("id=\"catalogPageLowStockCount\"")
                .contains("id=\"catalogPageFeaturedCount\"")
                .contains("id=\"catalogPageSelectedCount\"");
        assertThat(script)
                .contains("const pageProducts = currentCatalogPageProducts(list)")
                .contains("setText(elements.catalogPageAveragePrice")
                .contains("selectedProductIds.has(Number(product.id))");
        assertThat(css).contains(".catalog-page-metrics");
    }

    @Test
    void detailRecentFlowKeepsFiveDecisionMetrics() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"detailRecentAveragePrice\"")
                .contains("id=\"detailRecentPriceRange\"")
                .contains("id=\"detailRecentTotalStock\"")
                .contains("id=\"detailRecentLowStockCount\"")
                .contains("id=\"detailRecentBrandCount\"");
        assertThat(script)
                .contains("const recentPrices = recentProducts.map")
                .contains("setElementText(elements.detailRecentAveragePrice")
                .contains("setElementText(elements.detailRecentPriceRange")
                .contains("new Set(recentProducts.map((item) => item.brand)");
        assertThat(css).contains(".detail-recent-metrics");
    }

    @Test
    void productDrawerKeepsFiveRelatedProductDecisionActions() throws IOException {
        String script = readResource("static/js/view/app.js");

        assertThat(script)
                .contains("relatedAvailableOnly: false")
                .contains("relatedSameCategoryOnly: false")
                .contains("data-drawer-related-available-toggle")
                .contains("data-drawer-related-category-toggle")
                .contains("data-drawer-related-cheapest-id")
                .contains("data-drawer-related-highest-stock-id")
                .contains("data-drawer-related-compare-all")
                .contains("addProductsToBoard(filteredRelatedProducts, \"COMPARE\")");
    }

    @Test
    void bookmarkBoardKeepsAvailabilitySoldOutCategoryOldestAndCsvActions() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");

        assertThat(html)
                .contains("id=\"filterBookmarkAvailableButton\"")
                .contains("id=\"filterBookmarkSoldOutButton\"")
                .contains("id=\"filterBookmarkCategoryButton\"")
                .contains("id=\"sortBookmarkOldestButton\"")
                .contains("id=\"exportBookmarkCsvButton\"");
        assertThat(script)
                .contains("boardState.bookmarkFilter = \"AVAILABLE\"")
                .contains("boardState.bookmarkFilter = \"SOLD_OUT\"")
                .contains("boardState.bookmarkFilter = \"DOMINANT_CATEGORY\"")
                .contains("boardState.bookmarkSort = \"OLDEST\"")
                .contains("grade-stock-bookmarks.csv");
    }

    @Test
    void emptyCatalogKeepsFiveIndependentFilterRecoveryActions() throws IOException {
        String script = readResource("static/js/view/app.js");

        assertThat(script)
                .contains("data-empty-action=\"CLEAR_SEARCH\"")
                .contains("data-empty-action=\"CLEAR_BRAND\"")
                .contains("data-empty-action=\"CLEAR_CATEGORY\"")
                .contains("data-empty-action=\"CLEAR_PRICE\"")
                .contains("data-empty-action=\"CLEAR_STOCK\"")
                .contains("function relaxEmptyCatalogState(action)")
                .contains("state[key] = DEFAULT_STATE[key]")
                .contains("paginationState.page = 1");
    }

    @Test
    void detailRecentFlowKeepsPriceStockAvailableCompareAndBookmarkActions() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");

        assertThat(html)
                .contains("id=\"detailRecentSortPriceButton\"")
                .contains("id=\"detailRecentSortStockButton\"")
                .contains("id=\"detailRecentAvailableOnlyButton\"")
                .contains("id=\"detailRecentCompareAllButton\"")
                .contains("id=\"detailRecentBookmarkAllButton\"");
        assertThat(script)
                .contains("const detailRecentState = {")
                .contains("function visibleDetailRecentProducts(currentProductId)")
                .contains("detailRecentState.sort === \"PRICE_LOW\"")
                .contains("detailRecentState.availableOnly")
                .contains("function addDetailRecentToBoard(target)");
    }

    @Test
    void productDrawerKeepsFiveOptionFilterSortAndRecommendationActions() throws IOException {
        String script = readResource("static/js/view/app.js");

        assertThat(script)
                .contains("optionAvailableOnly: false")
                .contains("optionStableOnly: false")
                .contains("optionSort: \"DEFAULT\"")
                .contains("data-drawer-option-available-toggle")
                .contains("data-drawer-option-stable-toggle")
                .contains("data-drawer-option-sort=\"STOCK_ASC\"")
                .contains("data-drawer-option-sort=\"STOCK_DESC\"")
                .contains("data-drawer-option-recommend-id")
                .contains("detailUrl.searchParams.set(\"option\", recommended.name)");
    }

    @Test
    void compareBoardKeepsStockPriceOldestAndCsvManagementActions() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");

        assertThat(html)
                .contains("id=\"openHighestStockCompareButton\"")
                .contains("id=\"openLowestStockCompareButton\"")
                .contains("id=\"openHighestPriceCompareButton\"")
                .contains("id=\"sortCompareOldestButton\"")
                .contains("id=\"exportCompareCsvButton\"");
        assertThat(script)
                .contains("function openCompareProductByMetric(metric)")
                .contains("metric === \"STOCK_HIGH\"")
                .contains("metric === \"STOCK_LOW\"")
                .contains("boardState.compareSort = \"OLDEST\"")
                .contains("grade-stock-compare-products.csv");
    }

    @Test
    void catalogSelectionKeepsAvailableStablePremiumBrandAndCleanupActions() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");

        assertThat(html)
                .contains("id=\"selectAvailablePageButton\"")
                .contains("id=\"selectStablePageButton\"")
                .contains("id=\"selectPremiumPageButton\"")
                .contains("id=\"selectDominantBrandPageButton\"")
                .contains("id=\"removeLowStockSelectionButton\"");
        assertThat(script)
                .contains("Number(product.stock || 0) > 0, \"구매 가능\"")
                .contains("Number(product.stock || 0) >= lowStockThresholdValue(), \"안정 재고\"")
                .contains("Number(product.price || 0) > 300000, \"30만원 초과\"")
                .contains("dominantBrand(currentCatalogPageProducts())")
                .contains("recordSelectionSnapshot(\"긴장 재고 선택 제외\")");
    }

    @Test
    void detailRelatedKeepsCheaperStockSoldOutBookmarkAndLinkActions() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");

        assertThat(html)
                .contains("id=\"detailRelatedCheaperOnlyButton\"")
                .contains("id=\"detailRelatedStockAdvantageOnlyButton\"")
                .contains("id=\"detailRelatedSoldOutOnlyButton\"")
                .contains("id=\"detailBookmarkAllRelatedButton\"")
                .contains("id=\"detailCopyRelatedLinksButton\"");
        assertThat(script)
                .contains("cheaperOnly: false")
                .contains("stockAdvantageOnly: false")
                .contains("soldOutOnly: false")
                .contains("function addAllRelatedToBookmark()")
                .contains("async function copyRelatedProductLinks()");
    }

    @Test
    void searchHistoryKeepsEdgeApplyRemoveAndTextExportActions() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");

        assertThat(html)
                .contains("id=\"applyLatestSearchButton\"")
                .contains("id=\"applyOldestSearchButton\"")
                .contains("id=\"removeLatestSearchButton\"")
                .contains("id=\"removeOldestSearchButton\"")
                .contains("id=\"exportSearchHistoryButton\"");
        assertThat(script)
                .contains("async function applySearchHistoryEdge(position)")
                .contains("function removeSearchHistoryEdge(position)")
                .contains("position === \"LATEST\" ? history[0]")
                .contains("grade-stock-search-history.txt")
                .contains("history.length === 0");
    }

    @Test
    void actionMenusKeepExclusiveOutsideActionEscapeAndFocusBehavior() throws IOException {
        String script = readResource("static/js/view/app.js");

        assertThat(script)
                .contains("function initActionMenuBehavior()")
                .contains("details.board-action-menu")
                .contains("otherMenu !== menu && otherMenu.open")
                .contains("const openMenuSelector = menuSelector.split")
                .contains("document.querySelectorAll(openMenuSelector)")
                .contains("if (event.key !== \"Escape\")")
                .contains("summary?.setAttribute(\"aria-expanded\", String(menu.open))")
                .contains("summary?.isConnected && summary.focus()");
    }

    @Test
    void savedViewsKeepEdgeApplyRemoveAndJsonBackupActions() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");

        assertThat(html)
                .contains("id=\"applyLatestSavedViewButton\"")
                .contains("id=\"applyOldestSavedViewButton\"")
                .contains("id=\"removeLatestSavedViewButton\"")
                .contains("id=\"removeOldestSavedViewButton\"")
                .contains("id=\"exportSavedViewsButton\"");
        assertThat(script)
                .contains("async function applySavedViewEdge(position)")
                .contains("function removeSavedViewEdge(position)")
                .contains("position === \"LATEST\" ? savedViews[0]")
                .contains("grade-stock-saved-views.json")
                .contains("JSON.stringify(savedViews, null, 2)");
    }

    @Test
    void mobileNavigationKeepsSearchToggleObserverLockCurrentAndCountStatus() throws IOException {
        String script = readResource("static/js/view/app.js");

        assertThat(script)
                .contains("elements.headerSearchPanel?.hidden === false")
                .contains("syncMobileStoreNavigation(currentMobileNavigationAction())")
                .contains("button.setAttribute(\"aria-current\", \"page\")")
                .contains("관심 상품으로 이동, ${savedCount}개 저장됨")
                .contains("function currentMobileNavigationAction()")
                .contains("!event.target.closest('[data-mobile-nav=\"SEARCH\"]')")
                .contains("announceStorefrontStatus(\"모바일 상품 검색을 열었습니다.\")");
    }

    @Test
    void keyboardNavigationKeepsFiveAccessibleSectionShortcuts() throws IOException {
        String html = readResource("templates/views/index.html");
        String script = readResource("static/js/view/app.js");

        assertThat(html).contains("Alt + 1 / 2 / 3 / 4 / 5");
        assertThat(script)
                .contains("function handleSectionKeyboardShortcut(event)")
                .contains("Digit1: [\"top\", \"홈\"]")
                .contains("Digit2: [\"featured\", \"추천 상품\"]")
                .contains("Digit3: [\"catalog\", \"상품 카탈로그\"]")
                .contains("Digit4: [\"bookmarkBoardSection\", \"관심 상품 보드\"]")
                .contains("Digit5: [\"compareBoardSection\", \"비교 상품 보드\"]")
                .contains("section.focus({ preventScroll: true })");
    }

    @Test
    void detailOptionsKeepFiveStockBandAndConcentrationDistributionBars() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"detailOptionSoldOutRate\"")
                .contains("id=\"detailOptionCriticalRate\"")
                .contains("id=\"detailOptionLowRate\"")
                .contains("id=\"detailOptionStableRate\"")
                .contains("id=\"detailOptionConcentrationRate\"");
        assertThat(script)
                .contains("const optionDistribution = [")
                .contains("const totalOptionStock = optionStocks.reduce")
                .contains("optionDistribution.forEach");
        assertThat(css).contains(".detail-option-distribution");
    }

    @Test
    void relatedProductsKeepFiveComparisonAndSavingDistributionBars() throws IOException {
        String html = readResource("templates/views/product-detail.html");
        String script = readResource("static/js/view/detail.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"detailRelatedCheaperRate\"")
                .contains("id=\"detailRelatedStockAdvantageRate\"")
                .contains("id=\"detailRelatedSameBrandRate\"")
                .contains("id=\"detailRelatedSameCategoryRate\"")
                .contains("id=\"detailRelatedAverageSavingRate\"");
        assertThat(script)
                .contains("const cheaperProducts = related.filter")
                .contains("const relatedDistribution = [")
                .contains("elements.detailRelatedAverageSavingBar.style.width");
        assertThat(css).contains(".detail-related-distribution");
    }

    @Test
    void detailImagesKeepScopedFallbackLoadingAndZoomContracts() throws IOException {
        String script = readResource("static/js/view/detail.js");
        String css = readResource("static/css/storefront.css");

        assertThat(script)
                .contains("const PRODUCT_IMAGE_FALLBACK_URL = \"/images/product-placeholder.svg\"")
                .contains("const usesFallback = !thumbnail || thumbnail === PRODUCT_IMAGE_FALLBACK_URL")
                .contains("const isDetailImage = visual === elements.detailProductVisual")
                .contains("if (image.dataset.imageFallback === \"true\")")
                .contains("function clearDetailImageModalSource()")
                .contains("elements.detailProductVisual.setAttribute(\"aria-busy\", \"true\")")
                .contains("image.addEventListener(\"load\", () => {")
                .contains("elements.detailZoomButton.hidden = false")
                .contains("image.fetchPriority = \"high\"");
        assertThat(css)
                .contains(".product-visual--has-image[aria-busy=\"true\"] .product-visual__image")
                .contains(".product-visual--has-image.is-image-fallback .product-visual__image");
    }

    @Test
    void productRailsAndDetailHeaderKeepStableAlignmentContracts() throws IOException {
        String index = readResource("templates/views/index.html");
        String detail = readResource("templates/views/product-detail.html");
        String css = readResource("static/css/storefront.css");

        assertThat(index)
                .contains("<header class=\"product-rail-header\">")
                .contains("aria-label=\"신규 드롭 관리\"")
                .contains("aria-label=\"저재고 상품 관리\"");
        assertThat(detail).contains("id=\"backToCatalogLink\"");
        assertThat(css)
                .contains(".signal-strip .product-rail-header")
                .contains("grid-template-columns: minmax(180px, 1fr) auto minmax(180px, 1fr)")
                .contains(".page-shell--detail .topbar--detail .topbar-action")
                .contains("grid-template-columns: minmax(68px, auto) minmax(0, 1fr)")
                .contains(".page-shell--detail .detail-signal-list .signal-card::before");
    }

    @Test
    void dynamicProductMarkupKeepsStoredXssProtectionContract() throws IOException {
        String mainScript = readResource("static/js/view/app.js");
        String detailScript = readResource("static/js/view/detail.js");

        assertThat(mainScript)
                .contains("function markupSafeObject(value)")
                .contains("product = markupSafeObject(product)")
                .contains("titleElement.textContent = String(title || \"\")")
                .doesNotContain("toast.innerHTML = `<strong>${title}");
        assertThat(detailScript)
                .contains("function escapeMarkup(value)")
                .contains("item = markupSafeObject(item)")
                .contains("<span>${escapeMarkup(message)}</span>")
                .doesNotContain("toast.innerHTML = `<strong>${title}");
    }

    @Test
    void publicContentDetailKeepsReadableSafeAndRecoverableContract() throws IOException {
        String html = readResource("templates/views/content-detail.html");
        String script = readResource("static/js/view/content-detail.js");
        String mainScript = readResource("static/js/view/app.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("data-document-id=${documentId}")
                .contains("id=\"contentDetailArticle\"")
                .contains("id=\"contentDetailError\"")
                .contains("id=\"contentDetailRetryButton\"")
                .contains("id=\"contentDetailRelatedGrid\"")
                .contains("id=\"contentDetailReaderProgress\"")
                .contains("id=\"contentDetailBookmarkButton\" aria-pressed=\"false\"")
                .contains("id=\"contentDetailFontScaleLabel\"")
                .contains("id=\"contentDetailResume\"")
                .contains("id=\"contentDetailNavigation\"")
                .contains("id=\"contentDetailReaction\"")
                .contains("id=\"contentDetailHelpfulButton\"")
                .contains("data-content-reaction=\"HELPFUL\"")
                .contains("data-content-reaction=\"NOT_HELPFUL\"")
                .contains("id=\"contentDetailReactionRetryButton\"")
                .contains("id=\"contentDetailSavedListLink\"")
                .contains("/front/content#contentSavedBoard")
                .contains("data-content-return-link")
                .contains("/js/view/content-detail.js?v=20260727.1")
                .contains("/css/storefront.css?v=20260724.1");
        assertThat(script)
                .contains("fetch(`/api/front/content/${documentId}`)")
                .contains("paragraph.textContent = content")
                .contains("title.textContent = item.title")
                .contains("navigator.share")
                .contains("navigator.clipboard?.writeText")
                .contains("method: \"POST\"")
                .contains("front-content-visitor-key")
                .contains("window.crypto?.randomUUID?.()")
                .contains("memoryVisitorKey ||= createVisitorKey()")
                .contains("function isValidVisitorKey(value)")
                .contains("/^[A-Za-z0-9-]{16,64}$/")
                .contains("front-recent-content")
                .contains("front-bookmarked-content")
                .contains("front-content-reading-progress")
                .contains("front-content-font-scale")
                .contains("front-content-return-url")
                .contains("renderNavigation(content.newerContent, content.olderContent)")
                .contains("loadReactionSummary()")
                .contains("submitReaction(\"HELPFUL\")")
                .contains("submitReaction(\"NOT_HELPFUL\")")
                .contains("\"X-Content-Visitor-Key\": resolveVisitorKey()")
                .contains("reactionRequestInFlight")
                .contains("aria-pressed")
                .contains("window.requestAnimationFrame")
                .contains("aria-valuenow")
                .contains("elements.retryButton.addEventListener");
        assertThat(mainScript)
                .contains("href=\"/front/content/${Number(item.id)}\"")
                .contains("aria-label=\"${item.title} 상세 보기\"");
        assertThat(css)
                .contains(".content-detail-article")
                .contains("max-width: 760px")
                .contains(".content-detail-reader-toolbar")
                .contains(".content-detail-resume")
                .contains(".content-detail-navigation")
                .contains(".content-detail-reaction")
                .contains(".content-detail-reaction__controls")
                .contains(".content-detail-reaction__meter")
                .contains("button[aria-pressed=\"true\"]")
                .contains("font-size: calc(17px * var(--content-font-scale))")
                .contains(".content-detail-related__grid")
                .contains("grid-template-columns: repeat(2, minmax(0, 1fr))");
    }

    @Test
    void publicContentArchiveKeepsSearchPagingAndSafeRenderingContract() throws IOException {
        String html = readResource("templates/views/content-list.html");
        String script = readResource("static/js/view/content-list.js");
        String css = readResource("static/css/storefront.css");

        assertThat(html)
                .contains("id=\"contentListSearchForm\"")
                .contains("data-content-board=\"NOTICE\"")
                .contains("data-content-board=\"STYLE\"")
                .contains("id=\"contentListGrid\"")
                .contains("id=\"contentListPagination\"")
                .contains("id=\"contentListSortSelect\"")
                .contains("id=\"contentListSizeSelect\"")
                .contains("id=\"contentListResetButton\"")
                .contains("id=\"contentListPageSelect\"")
                .contains("id=\"contentListInsights\"")
                .contains("id=\"contentListAppliedFilters\"")
                .contains("id=\"contentRecentBoard\"")
                .contains("id=\"contentRecentClearButton\"")
                .contains("id=\"contentSavedBoard\"")
                .contains("id=\"contentSavedGrid\"")
                .contains("id=\"contentSavedCopyButton\"")
                .contains("id=\"contentSavedClearButton\"")
                .contains("id=\"contentSavedExpandButton\"")
                .contains("id=\"contentSavedUtilityCount\"")
                .contains("/js/view/content-list.js?v=20260727.1")
                .contains("/css/storefront.css?v=20260723.4");
        assertThat(script)
                .contains("fetch(`/api/front/content?${params}`")
                .contains("sort: state.sort")
                .contains("window.history.pushState")
                .contains("window.history.replaceState")
                .contains("window.addEventListener(\"popstate\"")
                .contains("window.addEventListener(\"hashchange\"")
                .contains("correctOutOfRangePage(payload)")
                .contains("renderPageOptions(totalPages, currentPage)")
                .contains("handleTabKeydown")
                .contains("title.textContent = item.title")
                .contains("summary.textContent = item.summary")
                .contains("window.localStorage.getItem(RECENT_CONTENT_KEY)")
                .contains("window.localStorage.removeItem(RECENT_CONTENT_KEY)")
                .contains("function normalizeStoredContentItems(value, limit)")
                .contains("Number.isSafeInteger(id)")
                .contains("최근 읽은 콘텐츠를 비우지 못했습니다.")
                .contains("front-bookmarked-content")
                .contains("front-content-reading-progress")
                .contains("window.addEventListener(\"storage\"")
                .contains("document.createElement(\"article\")")
                .contains("contentBookmarkId")
                .contains("navigator.clipboard.writeText(text)")
                .contains("window.confirm(\"관심 콘텐츠를 모두 비울까요?\")")
                .contains("검색 조건에 맞는 공개 콘텐츠가 없습니다.")
                .contains("retry.addEventListener")
                .doesNotContain("innerHTML");
        assertThat(css)
                .contains(".content-list-grid")
                .contains(".content-list-card")
                .contains(".content-list-settings")
                .contains(".content-list-insights")
                .contains(".content-list-applied")
                .contains(".content-recent-board__grid")
                .contains(".content-recent-card")
                .contains(".content-list-card__link")
                .contains(".content-list-card__bookmark")
                .contains(".content-saved-board__grid")
                .contains(".content-saved-card")
                .contains("grid-template-columns: repeat(2, minmax(0, 1fr))")
                .contains(".content-list-grid.is-error");
    }

    @Test
    void productComparisonKeepsRefreshDecisionAndResponsiveTableContract() throws IOException {
        String home = readResource("templates/views/index.html");
        String html = readResource("templates/views/product-comparison.html");
        String script = readResource("static/js/view/product-comparison.js");
        String css = readResource("static/css/product-comparison.css");

        assertThat(home)
                .contains("href=\"/front/compare\">비교")
                .contains("href=\"/front/compare\">상세 비교 열기");
        assertThat(html)
                .contains("id=\"comparisonDifferenceOnly\"")
                .contains("data-compare-mode=\"BALANCE\"")
                .contains("data-compare-mode=\"PRICE\"")
                .contains("data-compare-mode=\"STOCK\"")
                .contains("id=\"comparisonCandidateList\"")
                .contains("id=\"comparisonEmptyRetryButton\"")
                .contains("id=\"comparisonRecommendation\"")
                .contains("id=\"comparisonTable\" role=\"table\"")
                .contains("id=\"comparisonOptionTable\"")
                .contains("id=\"comparisonCsvButton\"")
                .contains("id=\"comparisonPrintButton\"")
                .contains("/js/view/product-comparison.js?v=20260727.1")
                .contains("/css/product-comparison.css?v=20260726.1");
        assertThat(script)
                .contains("front-compare-products")
                .contains("front-recent-viewed-products")
                .contains("front-bookmark-products")
                .contains("Promise.allSettled")
                .contains("fetch(`/api/front/products/${Number(item.id)}`, {")
                .contains("loadController?.abort()")
                .contains("signal: loadController.signal")
                .contains("hasStoredSnapshot(stored[index])")
                .contains("Number.isSafeInteger(Number(item?.id))")
                .contains("renderRecommendation(products)")
                .contains("renderOptions(products)")
                .contains("window.history")
                .contains("window.addEventListener(\"popstate\"")
                .contains("window.addEventListener(\"storage\"")
                .contains("new Blob([csv]")
                .contains("window.print()")
                .contains("escapeHtml(product.name")
                .contains("state.products = next.slice(-3)");
        assertThat(css)
                .contains(".comparison-table-wrap")
                .contains("overflow-x: auto")
                .contains("min-width: max-content")
                .contains(".comparison-table__label")
                .contains("position: sticky")
                .contains(".comparison-table__value.is-best")
                .contains(".comparison-candidate")
                .contains("@media (max-width: 520px)")
                .contains("@media print");
    }

    private String readResource(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
