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
                .contains("/css/storefront.css?v=20260712")
                .contains("id=\"headerSearchPanel\"")
                .contains("id=\"homeCategoryRail\"")
                .contains("id=\"heroNextButton\"")
                .contains("id=\"catalogMemoryTools\"")
                .contains("role=\"dialog\"")
                .contains("aria-modal=\"true\"")
                .contains("aria-labelledby=\"drawerTitle\"")
                .contains("/js/view/app.js?v=20260712");
    }

    @Test
    void detailPageKeepsCommerceVisualAndPrimaryActionHooks() throws IOException {
        String html = readResource("templates/views/product-detail.html");

        assertThat(html)
                .contains("/css/storefront.css?v=20260712")
                .contains("id=\"detailProductVisual\"")
                .contains("id=\"detailVisualModel\"")
                .contains("id=\"detailPrimaryAction\"")
                .contains("/js/view/detail.js?v=20260712");
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

    private String readResource(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
