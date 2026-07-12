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
                .contains("/css/storefront.css?v=20260712.7")
                .contains("id=\"headerSearchPanel\"")
                .contains("id=\"homeCategoryRail\"")
                .contains("id=\"heroNextButton\"")
                .contains("id=\"catalogMemoryTools\"")
                .contains("role=\"dialog\"")
                .contains("aria-modal=\"true\"")
                .contains("aria-labelledby=\"drawerTitle\"")
                .contains("/js/view/app.js?v=20260712.7");
    }

    @Test
    void detailPageKeepsCommerceVisualAndPrimaryActionHooks() throws IOException {
        String html = readResource("templates/views/product-detail.html");

        assertThat(html)
                .contains("/css/storefront.css?v=20260712.7")
                .contains("id=\"detailProductVisual\"")
                .contains("id=\"detailVisualModel\"")
                .contains("id=\"detailPrimaryAction\"")
                .contains("/js/view/detail.js?v=20260712.7");
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

    private String readResource(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
