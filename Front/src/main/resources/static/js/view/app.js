(function () {
    const BOOKMARK_PRODUCTS_KEY = "front-bookmark-products";
    const COMPARE_PRODUCTS_KEY = "front-compare-products";
    const RECENT_VIEWED_KEY = "front-recent-viewed-products";
    const DEFAULT_STATE = {
        search: "",
        brand: "ALL",
        category: "ALL",
        stock: "ALL",
        sort: "LATEST",
        lowStockThreshold: "20",
        featuredOnly: "ALL",
        priceBand: "ALL"
    };
    const state = {
        ...DEFAULT_STATE
    };
    let products = [];
    let metrics = {
        totalCount: 0,
        lowStockCount: 0,
        latestCreatedDate: null,
        latestDropCount: 0,
        featuredCount: 0,
        totalStock: 0
    };
    let brandFacets = [];
    let categoryFacets = [];
    const detailCache = new Map();

    const elements = {
        brandFilter: document.getElementById("brandFilter"),
        categoryFilter: document.getElementById("categoryFilter"),
        stockFilter: document.getElementById("stockFilter"),
        featuredOnlyFilter: document.getElementById("featuredOnlyFilter"),
        priceBandFilter: document.getElementById("priceBandFilter"),
        lowStockThresholdFilter: document.getElementById("lowStockThresholdFilter"),
        sortFilter: document.getElementById("sortFilter"),
        searchInput: document.getElementById("searchInput"),
        catalogGrid: document.getElementById("catalogGrid"),
        catalogTags: document.getElementById("catalogTags"),
        catalogCountText: document.getElementById("catalogCountText"),
        catalogSummaryText: document.getElementById("catalogSummaryText"),
        brandSpotlightGrid: document.getElementById("brandSpotlightGrid"),
        categoryShortcutGrid: document.getElementById("categoryShortcutGrid"),
        latestDropGrid: document.getElementById("latestDropGrid"),
        lowStockGrid: document.getElementById("lowStockGrid"),
        featuredGrid: document.getElementById("featuredGrid"),
        recentViewedSection: document.getElementById("recentViewedSection"),
        recentViewedGrid: document.getElementById("recentViewedGrid"),
        compareBoardSection: document.getElementById("compareBoardSection"),
        compareBoardGrid: document.getElementById("compareBoardGrid"),
        compareBoardTitle: document.getElementById("compareBoardTitle"),
        compareBoardText: document.getElementById("compareBoardText"),
        clearCompareButton: document.getElementById("clearCompareButton"),
        bookmarkBoardSection: document.getElementById("bookmarkBoardSection"),
        bookmarkBoardGrid: document.getElementById("bookmarkBoardGrid"),
        bookmarkBoardTitle: document.getElementById("bookmarkBoardTitle"),
        bookmarkBoardText: document.getElementById("bookmarkBoardText"),
        clearBookmarkButton: document.getElementById("clearBookmarkButton"),
        signalList: document.getElementById("signalList"),
        todaySignalTitle: document.getElementById("todaySignalTitle"),
        todaySignalText: document.getElementById("todaySignalText"),
        metricCount: document.getElementById("metricCount"),
        metricLowStock: document.getElementById("metricLowStock"),
        metricToday: document.getElementById("metricToday"),
        productDrawer: document.getElementById("productDrawer"),
        drawerBody: document.getElementById("drawerBody"),
        closeDrawerButton: document.getElementById("closeDrawerButton"),
        focusLowStockButton: document.getElementById("focusLowStockButton"),
        openDrawerFromTop: document.getElementById("openDrawerFromTop"),
        resetFiltersButton: document.getElementById("resetFiltersButton")
    };

    async function init() {
        hydrateStateFromUrl();
        await loadProducts();
        populateFilters();
        syncControls();
        bindEvents();
        renderHeroMetrics();
        renderBrandSpotlight();
        renderCategoryShortcuts();
        renderSignalStrip();
        renderFeatured();
        renderRecentViewed();
        renderCompareBoard();
        renderBookmarkBoard();
        renderSignals();
        renderCatalog();
    }

    async function loadProducts() {
        try {
            const response = await fetch(`/api/front/catalog/bootstrap?${new URLSearchParams({
                keyword: state.search,
                brand: state.brand,
                category: state.category,
                stock: state.stock,
                sort: state.sort,
                lowStockThreshold: state.lowStockThreshold,
                featuredOnly: state.featuredOnly === "FEATURED",
                priceBand: state.priceBand
            })}`);
            if (!response.ok) {
                throw new Error("상품 데이터를 불러오지 못했습니다.");
            }
            const payload = await response.json();
            products = Array.isArray(payload?.products) ? payload.products.slice() : [];
            metrics = payload?.metrics || metrics;
            brandFacets = Array.isArray(payload?.brandFacets) ? payload.brandFacets.slice() : [];
            categoryFacets = Array.isArray(payload?.categoryFacets) ? payload.categoryFacets.slice() : [];
        } catch (error) {
            products = [];
            metrics = {
                totalCount: 0,
                lowStockCount: 0,
                latestCreatedDate: null,
                latestDropCount: 0,
                featuredCount: 0,
                totalStock: 0
            };
            brandFacets = [];
            categoryFacets = [];
            setText(elements.catalogCountText, "상품 데이터를 불러오지 못했습니다.");
            if (elements.catalogGrid) {
                elements.catalogGrid.innerHTML = `
                    <div class="catalog-empty">
                        <strong>카탈로그를 불러오지 못했습니다.</strong>
                        <p>잠시 후 다시 시도해주세요.</p>
                    </div>
                `;
            }
        }
        if (elements.lowStockThresholdFilter) {
            elements.lowStockThresholdFilter.value = state.lowStockThreshold;
        }
    }

    function populateFilters() {
        fillSelect(elements.brandFilter, "전체 브랜드", brandFacets, uniqueValues("brand"));
        fillSelect(elements.categoryFilter, "전체 카테고리", categoryFacets, uniqueValues("category"));
        if (elements.brandFilter) {
            elements.brandFilter.value = state.brand;
        }
        if (elements.categoryFilter) {
            elements.categoryFilter.value = state.category;
        }
        if (elements.stockFilter) {
            elements.stockFilter.value = state.stock;
        }
        if (elements.featuredOnlyFilter) {
            elements.featuredOnlyFilter.value = state.featuredOnly;
        }
        if (elements.priceBandFilter) {
            elements.priceBandFilter.value = state.priceBand;
        }
        if (elements.sortFilter) {
            elements.sortFilter.value = state.sort;
        }
    }

    function fillSelect(select, defaultLabel, facets, fallbackValues) {
        if (!select) {
            return;
        }
        const options = Array.isArray(facets) && facets.length
            ? facets.map((facet) => ({
                value: facet.value,
                label: `${facet.value} (${facet.count})`
            }))
            : fallbackValues.map((value) => ({
                value,
                label: value
            }));
        select.innerHTML = [`<option value="ALL">${defaultLabel}</option>`]
            .concat(options.map((option) => `<option value="${option.value}">${option.label}</option>`))
            .join("");
    }

    function uniqueValues(key) {
        return Array.from(new Set(products.map((product) => product[key]))).sort((a, b) => a.localeCompare(b));
    }

    function bindEvents() {
        elements.searchInput?.addEventListener("input", (event) => {
            state.search = event.target.value.trim().toLowerCase();
            refreshCatalog();
        });
        elements.brandFilter?.addEventListener("change", (event) => {
            state.brand = event.target.value;
            refreshCatalog();
        });
        elements.categoryFilter?.addEventListener("change", (event) => {
            state.category = event.target.value;
            refreshCatalog();
        });
        elements.stockFilter?.addEventListener("change", (event) => {
            state.stock = event.target.value;
            refreshCatalog();
        });
        elements.featuredOnlyFilter?.addEventListener("change", (event) => {
            state.featuredOnly = event.target.value;
            if (state.featuredOnly === "FEATURED" && state.sort !== "FEATURED") {
                state.sort = "FEATURED";
                if (elements.sortFilter) {
                    elements.sortFilter.value = "FEATURED";
                }
            }
            refreshCatalog();
        });
        elements.priceBandFilter?.addEventListener("change", (event) => {
            state.priceBand = event.target.value;
            refreshCatalog();
        });
        elements.sortFilter?.addEventListener("change", (event) => {
            state.sort = event.target.value;
            refreshCatalog();
        });
        elements.lowStockThresholdFilter?.addEventListener("change", (event) => {
            state.lowStockThreshold = event.target.value;
            refreshCatalog();
        });
        elements.closeDrawerButton?.addEventListener("click", closeDrawer);
        elements.productDrawer?.addEventListener("click", (event) => {
            if (event.target === elements.productDrawer) {
                closeDrawer();
            }
        });
        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && elements.productDrawer?.classList.contains("is-open")) {
                closeDrawer();
            }
        });
        elements.focusLowStockButton?.addEventListener("click", () => {
            state.stock = "LOW";
            if (elements.stockFilter) {
                elements.stockFilter.value = "LOW";
            }
            refreshCatalog();
            document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
        });
        elements.openDrawerFromTop?.addEventListener("click", () => {
            const primary = filteredProducts()[0] || products[0];
            if (primary) {
                openDrawer(primary.id);
            }
        });
        elements.catalogTags?.addEventListener("click", (event) => {
            const actionButton = event.target.closest("[data-filter-remove]");
            if (!actionButton) {
                return;
            }
            removeFilter(actionButton.dataset.filterRemove);
        });
        elements.resetFiltersButton?.addEventListener("click", async () => {
            resetState();
            syncControls();
            await refreshCatalog();
        });
        elements.clearCompareButton?.addEventListener("click", () => {
            writeCompareProducts([]);
            renderCompareBoard();
            renderCatalog();
        });
        elements.clearBookmarkButton?.addEventListener("click", () => {
            writeBookmarkProducts([]);
            renderBookmarkBoard();
            renderCatalog();
        });
        elements.catalogGrid?.addEventListener("click", (event) => {
            const compareButton = event.target.closest("[data-compare-product-id]");
            if (compareButton) {
                toggleCompareProduct(Number(compareButton.dataset.compareProductId));
                return;
            }
            const bookmarkButton = event.target.closest("[data-bookmark-product-id]");
            if (bookmarkButton) {
                toggleBookmarkProduct(Number(bookmarkButton.dataset.bookmarkProductId));
            }
        });
        window.addEventListener("popstate", async () => {
            hydrateStateFromUrl();
            syncControls();
            await refreshCatalog();
        });
    }

    function renderHeroMetrics() {
        setText(elements.metricCount, String(metrics.totalCount || products.length));
        setText(elements.metricLowStock, String(metrics.lowStockCount || 0));
        setText(elements.metricToday, String(metrics.latestDropCount || 0));
    }

    function featuredRankLabel(product) {
        if (product.featuredRank) {
            return `Featured ${product.featuredRank}`;
        }
        return product.model || "Curated";
    }

    function stockPressureLabel(stock) {
        const quantity = Number(stock || 0);
        if (quantity <= 5) {
            return "즉시 확인";
        }
        if (quantity < lowStockThresholdValue()) {
            return "품절 임박";
        }
        return "재고 안정";
    }

    function stockPressureDetail(stock) {
        const quantity = Number(stock || 0);
        if (quantity <= 5) {
            return `재고 ${quantity}개로 즉시 대응이 필요합니다.`;
        }
        if (quantity < lowStockThresholdValue()) {
            return `재고 ${quantity}개로 긴장 구간에 들어가 있습니다.`;
        }
        return `재고 ${quantity}개로 비교적 안정적인 구간입니다.`;
    }

    function relativeDropLabel(createdDate) {
        if (!createdDate) {
            return "최근 등록";
        }
        const latestCreatedDate = metrics.latestCreatedDate;
        if (latestCreatedDate && createdDate === latestCreatedDate) {
            return "오늘 등록";
        }
        return `${createdDate} 등록`;
    }

    function moodLabel(product) {
        return product.mood || product.category || "Curated";
    }

    function compactProductContext(product) {
        return [product.brand, product.category, product.priceLabel || formatPrice(product.price)]
            .filter(Boolean)
            .join(" · ");
    }

    function renderBrandSpotlight() {
        if (!elements.brandSpotlightGrid) {
            return;
        }
        const rankedBrands = brandFacets.slice(0, 5).map((facet) => {
            const brandProducts = products.filter((product) => product.brand === facet.value);
            return {
                brand: facet.value,
                count: facet.count,
                lowStockCount: brandProducts.filter((product) => product.stock < lowStockThresholdValue()).length,
                category: brandProducts[0]?.category || "Curated",
                mood: brandProducts[0]?.mood || "Core"
            };
        });

        if (!rankedBrands.length) {
            elements.brandSpotlightGrid.innerHTML = `
                <article class="catalog-empty">
                    <strong>브랜드 데이터를 준비 중입니다.</strong>
                    <p>상품을 불러오면 상단 랭킹을 보여줍니다.</p>
                </article>
            `;
            return;
        }

        elements.brandSpotlightGrid.innerHTML = rankedBrands.map((item, index) => `
            <button class="brand-rank-card" type="button" data-brand-rank="${item.brand}">
                <span class="brand-rank-card__order">${String(index + 1).padStart(2, "0")}</span>
                <div class="brand-rank-card__body">
                    <strong>${item.brand}</strong>
                    <span>${item.count}개 상품 · 긴장 재고 ${item.lowStockCount}개 · ${item.mood}</span>
                </div>
                <span class="brand-rank-card__meta">${item.category}</span>
            </button>
        `).join("");

        elements.brandSpotlightGrid.querySelectorAll("[data-brand-rank]").forEach((button) => {
            button.addEventListener("click", async () => {
                state.brand = button.dataset.brandRank;
                syncControls();
                await refreshCatalog();
                document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
            });
        });
    }

    function renderCategoryShortcuts() {
        if (!elements.categoryShortcutGrid) {
            return;
        }
        const rankedCategories = categoryFacets.slice(0, 6).map((facet) => {
            const categoryProducts = products.filter((product) => product.category === facet.value);
            const lowStockCount = categoryProducts.filter((product) => product.stock < lowStockThresholdValue()).length;
            return {
                category: facet.value,
                count: facet.count,
                lowStockCount,
                pressure: facet.count ? Math.round((lowStockCount / facet.count) * 100) : 0
            };
        });

        if (!rankedCategories.length) {
            elements.categoryShortcutGrid.innerHTML = `
                <article class="catalog-empty">
                    <strong>카테고리 데이터를 준비 중입니다.</strong>
                    <p>상품을 불러오면 바로가기 카드를 보여줍니다.</p>
                </article>
            `;
            return;
        }

        elements.categoryShortcutGrid.innerHTML = rankedCategories.map((item) => `
            <button class="category-shortcut-card" type="button" data-category-shortcut="${item.category}">
                <span class="category-shortcut-card__badge">${brandInitials(item.category)}</span>
                <strong>${item.category}</strong>
                <span>${item.count}개 상품</span>
                <em>${item.lowStockCount ? `긴장 재고 ${item.lowStockCount}개 · ${item.pressure}%` : "재고 안정"}</em>
            </button>
        `).join("");

        elements.categoryShortcutGrid.querySelectorAll("[data-category-shortcut]").forEach((button) => {
            button.addEventListener("click", async () => {
                state.category = button.dataset.categoryShortcut;
                syncControls();
                await refreshCatalog();
                document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
            });
        });
    }

    function renderSignalStrip() {
        renderLatestDrops();
        renderLowStockHighlights();
    }

    function renderLatestDrops() {
        if (!elements.latestDropGrid) {
            return;
        }
        const latestCreatedDate = metrics.latestCreatedDate;
        const latestProducts = products
            .filter((product) => !latestCreatedDate || product.createdDate === latestCreatedDate)
            .slice(0, 4);

        if (!latestProducts.length) {
            elements.latestDropGrid.innerHTML = `
                <article class="catalog-empty">
                    <strong>신규 드롭을 준비 중입니다.</strong>
                    <p>최근 등록된 상품을 불러오면 이곳에 보여줍니다.</p>
                </article>
            `;
            return;
        }

        elements.latestDropGrid.innerHTML = latestProducts.map((product) => signalFeedCard(product, relativeDropLabel(product.createdDate))).join("");
    }

    function renderLowStockHighlights() {
        if (!elements.lowStockGrid) {
            return;
        }
        const lowStockProducts = products
            .filter((product) => product.stock < lowStockThresholdValue())
            .sort((left, right) => left.stock - right.stock)
            .slice(0, 4);

        if (!lowStockProducts.length) {
            elements.lowStockGrid.innerHTML = `
                <article class="catalog-empty">
                    <strong>긴장 재고 상품이 없습니다.</strong>
                    <p>현재 기준으로는 안정 재고 상품이 더 많습니다.</p>
                </article>
            `;
            return;
        }

        elements.lowStockGrid.innerHTML = lowStockProducts.map((product) => signalFeedCard(product, stockPressureLabel(product.stock))).join("");
    }

    function signalFeedCard(product, kicker) {
        return `
            <a class="signal-feed-card" href="${detailPageUrl(product.id)}">
                ${productVisualMarkup(product, "signal-feed-card__visual")}
                <div class="signal-feed-card__body">
                    <span class="signal-feed-card__kicker">${kicker}</span>
                    <strong>${product.headline || product.name}</strong>
                    <p>${compactProductContext(product)} · ${stockPressureDetail(product.stock)}</p>
                </div>
            </a>
        `;
    }

    function renderFeatured() {
        if (!elements.featuredGrid) {
            return;
        }
        const featuredProducts = products.filter((product) => product.featured).slice(0, 3);
        elements.featuredGrid.innerHTML = featuredProducts.map((product, index) => `
            <article class="spotlight-card ${index === 0 ? "spotlight-card--accent" : ""}">
                ${productVisualMarkup(product, "spotlight-card__visual")}
                <div>
                    <div class="spotlight-card__top">
                        <span class="spotlight-card__label">${product.brand}</span>
                        <span class="spotlight-card__pill ${stockClassName(product.stock)}">${product.stockStatus || stockLabel(product.stock)}</span>
                    </div>
                    <h3 class="spotlight-card__title">${product.headline || product.name}</h3>
                    <div class="spotlight-card__meta">
                        <span>${product.name}</span>
                        <span>${product.category}</span>
                        <span>${featuredRankLabel(product)}</span>
                    </div>
                </div>
                <div class="spotlight-card__footer">
                    <div>
                        <div class="spotlight-card__price">${product.priceLabel || formatPrice(product.price)}</div>
                        <div class="catalog-card__meta">${stockPressureDetail(product.stock)}</div>
                    </div>
                    <div class="catalog-card__action">
                        <a class="catalog-card__link" href="${detailPageUrl(product.id)}">페이지 보기</a>
                        <button class="catalog-card__button" type="button" data-product-id="${product.id}">빠른 보기</button>
                    </div>
                </div>
            </article>
        `).join("");

        bindProductButtons(elements.featuredGrid);
    }

    function renderSignals() {
        if (!elements.signalList) {
            return;
        }

        const latestCreatedDate = metrics.latestCreatedDate;
        const latestProducts = products
            .filter((product) => !latestCreatedDate || product.createdDate === latestCreatedDate)
            .sort((left, right) => left.stock - right.stock);
        const lowStockProducts = products
            .filter((product) => product.stock < lowStockThresholdValue())
            .sort((left, right) => left.stock - right.stock);
        const primarySignal = lowStockProducts[0] || latestProducts[0] || products[0];

        if (primarySignal) {
            setText(elements.todaySignalTitle, `${primarySignal.headline || primarySignal.name}을 우선 확인해야 합니다.`);
            setText(elements.todaySignalText, `${compactProductContext(primarySignal)} · ${stockPressureDetail(primarySignal.stock)}`);
        }

        const signals = [
            `${metrics.featuredCount || products.filter((product) => product.featured).length}개 상품이 이번 주 큐레이션에 포함되어 우선 노출 중입니다.`,
            `${metrics.lowStockCount || products.filter((product) => product.stock < lowStockThresholdValue()).length}개 상품이 재고 긴장 구간에 있어 빠른 확인이 필요합니다.`,
            `${metrics.totalStock || (products[0] ? products.reduce((sum, product) => sum + product.stock, 0) : 0)}개 재고와 ${metrics.latestDropCount || latestProducts.length}건 최신 드롭을 첫 화면에서 함께 추적합니다.`
        ];

        elements.signalList.innerHTML = signals.map((message, index) => `
            <article class="signal-card">
                <strong>Signal 0${index + 1}</strong>
                <span>${message}</span>
            </article>
        `).join("");
    }

    function renderCatalog() {
        const list = filteredProducts();
        const comparedIds = new Set(readCompareProducts().map((product) => Number(product.id)));
        const bookmarkedIds = new Set(readBookmarkProducts().map((product) => Number(product.id)));
        renderCatalogSummary(list);

        if (!elements.catalogGrid) {
            return;
        }

        if (!list.length) {
            elements.catalogGrid.innerHTML = `
                <div class="catalog-empty">
                    <strong>조건에 맞는 상품이 없습니다.</strong>
                    <p>필터를 조금 넓히거나 검색어를 비워서 다시 확인해보세요.</p>
                </div>
            `;
            return;
        }

        elements.catalogGrid.innerHTML = list.map((product) => `
            <article class="catalog-card">
                ${productVisualMarkup(product, "catalog-card__visual")}
                <div class="catalog-card__header">
                    <div>
                        <span class="catalog-card__label">${product.brand}</span>
                        <h3 class="catalog-card__title">${product.name}</h3>
                        <div class="catalog-card__meta">
                            <span>${product.headline || product.name}</span>
                            <span>${product.category}</span>
                            <span>${featuredRankLabel(product)}</span>
                        </div>
                    </div>
                    <span class="catalog-card__pill ${stockClassName(product.stock)}">${product.stockStatus || stockLabel(product.stock)}</span>
                </div>
                <p class="catalog-card__copy">${product.description}</p>
                <div class="catalog-card__footer">
                    <div>
                        <div class="catalog-card__price">${product.priceLabel || formatPrice(product.price)}</div>
                        <div class="catalog-card__meta">${relativeDropLabel(product.createdDate)} · ${stockPressureDetail(product.stock)}</div>
                    </div>
                    <div class="catalog-card__action">
                        <div class="catalog-card__meta">${moodLabel(product)}</div>
                        <a class="catalog-card__link" href="${detailPageUrl(product.id)}">페이지 보기</a>
                        <div class="catalog-card__action-group">
                            <button class="catalog-bookmark-button ${bookmarkedIds.has(Number(product.id)) ? "is-active" : ""}" type="button" data-bookmark-product-id="${product.id}">
                                ${bookmarkedIds.has(Number(product.id)) ? "찜 해제" : "찜하기"}
                            </button>
                            <button class="catalog-compare-button ${comparedIds.has(Number(product.id)) ? "is-active" : ""}" type="button" data-compare-product-id="${product.id}">
                                ${comparedIds.has(Number(product.id)) ? "비교 해제" : "비교 담기"}
                            </button>
                        </div>
                        <button class="catalog-card__button" type="button" data-product-id="${product.id}">빠른 보기</button>
                    </div>
                </div>
            </article>
        `).join("");

        bindProductButtons(elements.catalogGrid);
    }

    function renderCatalogSummary(list) {
        setText(elements.catalogCountText, `${list.length}개 상품이 현재 조건에 맞습니다.`);
        setText(elements.catalogSummaryText, buildSummaryText(list.length));
        if (!elements.catalogTags) {
            return;
        }

        const tags = [];
        if (state.brand !== "ALL") {
            tags.push({ label: `브랜드 ${state.brand}`, key: "brand" });
        }
        if (state.category !== "ALL") {
            tags.push({ label: `카테고리 ${state.category}`, key: "category" });
        }
        if (state.stock === "LOW") {
            tags.push({ label: `품절 임박 ${state.lowStockThreshold}개 미만`, key: "stock" });
        }
        if (state.stock === "STABLE") {
            tags.push({ label: `재고 안정 ${state.lowStockThreshold}개 이상`, key: "stock" });
        }
        if (state.featuredOnly === "FEATURED") {
            tags.push({ label: "Featured만", key: "featuredOnly" });
        }
        if (state.priceBand === "UNDER_200") {
            tags.push({ label: "20만원 미만", key: "priceBand" });
        }
        if (state.priceBand === "BETWEEN_200_300") {
            tags.push({ label: "20만원-30만원", key: "priceBand" });
        }
        if (state.priceBand === "OVER_300") {
            tags.push({ label: "30만원 초과", key: "priceBand" });
        }
        if (state.search) {
            tags.push({ label: `검색 ${state.search}`, key: "search" });
        }
        if (state.sort === "FEATURED") {
            tags.push({ label: "대표 노출순", key: "sort" });
        }
        if (state.sort === "NAME_ASC") {
            tags.push({ label: "상품명 오름차순", key: "sort" });
        }
        if (state.sort === "PRICE_HIGH") {
            tags.push({ label: "발매가 높은 순", key: "sort" });
        }
        if (state.sort === "PRICE_LOW") {
            tags.push({ label: "발매가 낮은 순", key: "sort" });
        }
        if (state.sort === "STOCK_ASC") {
            tags.push({ label: "재고 낮은 순", key: "sort" });
        }
        if (state.sort === "STOCK_DESC") {
            tags.push({ label: "재고 높은 순", key: "sort" });
        }
        if (!tags.length) {
            tags.push({ label: "전체 탐색", key: "" });
        }

        elements.catalogTags.innerHTML = tags.map((tag) => tag.key
            ? `
                <button class="catalog-tag catalog-tag--interactive" type="button" data-filter-remove="${tag.key}">
                    <span>${tag.label}</span>
                    <span class="catalog-tag__remove" aria-hidden="true">×</span>
                </button>
            `
            : `<span class="catalog-tag">${tag.label}</span>`).join("");
    }

    function filteredProducts() {
        return products.slice();
    }

    async function refreshCatalog() {
        syncUrlState();
        await loadProducts();
        populateFilters();
        renderHeroMetrics();
        renderBrandSpotlight();
        renderCategoryShortcuts();
        renderSignalStrip();
        renderFeatured();
        renderRecentViewed();
        renderCompareBoard();
        renderBookmarkBoard();
        renderSignals();
        renderCatalog();
    }

    function hydrateStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        state.search = (params.get("search") || DEFAULT_STATE.search).trim().toLowerCase();
        state.brand = params.get("brand") || DEFAULT_STATE.brand;
        state.category = params.get("category") || DEFAULT_STATE.category;
        state.stock = normalizeStateValue(params.get("stock"), ["ALL", "LOW", "STABLE"], DEFAULT_STATE.stock);
        state.sort = normalizeStateValue(
            params.get("sort"),
            ["LATEST", "NAME_ASC", "PRICE_HIGH", "PRICE_LOW", "STOCK_ASC", "STOCK_DESC", "FEATURED"],
            DEFAULT_STATE.sort
        );
        state.lowStockThreshold = normalizeStateValue(
            params.get("lowStockThreshold"),
            ["10", "20", "30", "50"],
            DEFAULT_STATE.lowStockThreshold
        );
        state.featuredOnly = normalizeStateValue(params.get("featuredOnly"), ["ALL", "FEATURED"], DEFAULT_STATE.featuredOnly);
        state.priceBand = normalizeStateValue(
            params.get("priceBand"),
            ["ALL", "UNDER_200", "BETWEEN_200_300", "OVER_300"],
            DEFAULT_STATE.priceBand
        );
    }

    function syncUrlState() {
        const params = new URLSearchParams();
        Object.entries(state).forEach(([key, value]) => {
            if (value && value !== DEFAULT_STATE[key]) {
                params.set(key, value);
            }
        });
        const nextUrl = params.toString() ? `${window.location.pathname}?${params}` : window.location.pathname;
        window.history.replaceState({}, "", nextUrl);
    }

    function syncControls() {
        if (elements.searchInput) {
            elements.searchInput.value = state.search;
        }
        if (elements.brandFilter) {
            elements.brandFilter.value = state.brand;
        }
        if (elements.categoryFilter) {
            elements.categoryFilter.value = state.category;
        }
        if (elements.stockFilter) {
            elements.stockFilter.value = state.stock;
        }
        if (elements.featuredOnlyFilter) {
            elements.featuredOnlyFilter.value = state.featuredOnly;
        }
        if (elements.priceBandFilter) {
            elements.priceBandFilter.value = state.priceBand;
        }
        if (elements.sortFilter) {
            elements.sortFilter.value = state.sort;
        }
        if (elements.lowStockThresholdFilter) {
            elements.lowStockThresholdFilter.value = state.lowStockThreshold;
        }
    }

    function removeFilter(key) {
        if (!key || !(key in DEFAULT_STATE)) {
            return;
        }
        state[key] = DEFAULT_STATE[key];
        if (key === "stock") {
            state.lowStockThreshold = DEFAULT_STATE.lowStockThreshold;
        }
        if (key === "featuredOnly" && state.sort === "FEATURED") {
            state.sort = DEFAULT_STATE.sort;
        }
        syncControls();
        refreshCatalog();
    }

    function resetState() {
        Object.assign(state, DEFAULT_STATE);
    }

    function buildSummaryText(count) {
        const activeFilters = Object.entries(state)
            .filter(([key, value]) => value !== DEFAULT_STATE[key])
            .map(([key]) => key);

        if (!activeFilters.length) {
            return "브랜드, 카테고리, 가격대 조건 없이 전체 카탈로그를 보고 있습니다.";
        }
        if (!count) {
            return "현재 조건에서는 결과가 없어 일부 필터를 해제하는 편이 좋습니다.";
        }
        return `${activeFilters.length}개 조건이 적용된 상태이며, 공유 가능한 URL로 탐색 상태를 유지합니다.`;
    }

    function normalizeStateValue(value, allowedValues, fallbackValue) {
        return allowedValues.includes(value) ? value : fallbackValue;
    }

    function detailPageUrl(productId) {
        return `/front/products/${productId}${window.location.search || ""}`;
    }

    function renderRecentViewed() {
        if (!elements.recentViewedSection || !elements.recentViewedGrid) {
            return;
        }
        const recentProducts = readRecentProducts().slice(0, 3);
        if (!recentProducts.length) {
            elements.recentViewedSection.hidden = true;
            return;
        }
        elements.recentViewedSection.hidden = false;
        elements.recentViewedGrid.innerHTML = recentProducts.map((product) => `
            <a class="detail-related-card" href="${detailPageUrl(product.id)}">
                ${productVisualMarkup(product, "detail-related-card__visual")}
                <span class="detail-related-card__brand">${product.brand || "-"}</span>
                <strong>${product.headline || product.name || "-"}</strong>
                <p>${product.name || "-"} · ${product.model || "-"}</p>
                <div class="detail-related-card__meta">
                    <span>${product.priceLabel || formatPrice(product.price)}</span>
                    <span class="${stockClassName(product.stock)}">${product.stockStatus || stockLabel(product.stock)}</span>
                    <span>최근 확인</span>
                </div>
            </a>
        `).join("");
    }

    function readRecentProducts() {
        try {
            const parsed = JSON.parse(window.localStorage.getItem(RECENT_VIEWED_KEY) || "[]");
            return Array.isArray(parsed) ? parsed.filter((item) => item?.id) : [];
        } catch (error) {
            return [];
        }
    }

    function renderCompareBoard() {
        if (!elements.compareBoardSection || !elements.compareBoardGrid) {
            return;
        }
        const comparedProducts = readCompareProducts();
        if (!comparedProducts.length) {
            elements.compareBoardSection.hidden = true;
            return;
        }
        elements.compareBoardSection.hidden = false;
        setText(elements.compareBoardTitle, `${comparedProducts.length}개 상품을 비교 중입니다.`);
        setText(elements.compareBoardText, buildCompareSummary(comparedProducts));
        elements.compareBoardGrid.innerHTML = comparedProducts.map((product) => `
            <article class="detail-related-card compare-card">
                ${productVisualMarkup(product, "detail-related-card__visual")}
                <span class="detail-related-card__brand">${product.brand || "-"}</span>
                <strong>${product.headline || product.name || "-"}</strong>
                <p>${product.name || "-"} · ${product.model || "-"} · ${product.category || "-"}</p>
                <div class="detail-related-card__meta">
                    <span>${product.priceLabel || formatPrice(product.price)}</span>
                    <span class="${stockClassName(product.stock)}">${product.stockStatus || stockLabel(product.stock)}</span>
                    <span>재고 ${product.stock}개</span>
                </div>
                <div class="compare-card__actions">
                    <a class="catalog-card__link" href="${detailPageUrl(product.id)}">상세 보기</a>
                    <button class="catalog-reset-button" type="button" data-remove-compare-id="${product.id}">제거</button>
                </div>
            </article>
        `).join("");

        elements.compareBoardGrid.querySelectorAll("[data-remove-compare-id]").forEach((button) => {
            button.addEventListener("click", () => {
                removeCompareProduct(Number(button.dataset.removeCompareId));
            });
        });
    }

    function renderBookmarkBoard() {
        if (!elements.bookmarkBoardSection || !elements.bookmarkBoardGrid) {
            return;
        }
        const bookmarkedProducts = readBookmarkProducts();
        if (!bookmarkedProducts.length) {
            elements.bookmarkBoardSection.hidden = true;
            return;
        }
        elements.bookmarkBoardSection.hidden = false;
        setText(elements.bookmarkBoardTitle, `${bookmarkedProducts.length}개 관심 상품을 저장했습니다.`);
        setText(elements.bookmarkBoardText, buildBookmarkSummary(bookmarkedProducts));
        elements.bookmarkBoardGrid.innerHTML = bookmarkedProducts.map((product) => `
            <article class="detail-related-card compare-card">
                ${productVisualMarkup(product, "detail-related-card__visual")}
                <span class="detail-related-card__brand">${product.brand || "-"}</span>
                <strong>${product.headline || product.name || "-"}</strong>
                <p>${product.name || "-"} · ${product.model || "-"} · ${product.category || "-"}</p>
                <div class="detail-related-card__meta">
                    <span>${product.priceLabel || formatPrice(product.price)}</span>
                    <span class="${stockClassName(product.stock)}">${product.stockStatus || stockLabel(product.stock)}</span>
                    <span>${product.featured ? "Featured" : "Watchlist"}</span>
                </div>
                <div class="compare-card__actions">
                    <a class="catalog-card__link" href="${detailPageUrl(product.id)}">상세 보기</a>
                    <button class="catalog-reset-button" type="button" data-remove-bookmark-id="${product.id}">제거</button>
                </div>
            </article>
        `).join("");

        elements.bookmarkBoardGrid.querySelectorAll("[data-remove-bookmark-id]").forEach((button) => {
            button.addEventListener("click", () => {
                removeBookmarkProduct(Number(button.dataset.removeBookmarkId));
            });
        });
    }

    function toggleCompareProduct(productId) {
        const source = products.find((product) => Number(product.id) === Number(productId));
        if (!source) {
            return;
        }
        const current = readCompareProducts();
        const exists = current.some((product) => Number(product.id) === Number(productId));
        if (exists) {
            writeCompareProducts(current.filter((product) => Number(product.id) !== Number(productId)));
        } else {
            const summary = {
                id: source.id,
                brand: source.brand,
                name: source.name,
                headline: source.headline,
                model: source.model,
                category: source.category,
                price: source.price,
                priceLabel: source.priceLabel,
                stock: source.stock,
                stockStatus: source.stockStatus
            };
            writeCompareProducts([summary].concat(current).slice(0, 3));
        }
        renderCompareBoard();
        renderCatalog();
    }

    function removeCompareProduct(productId) {
        writeCompareProducts(readCompareProducts().filter((product) => Number(product.id) !== Number(productId)));
        renderCompareBoard();
        renderCatalog();
    }

    function toggleBookmarkProduct(productId) {
        const source = products.find((product) => Number(product.id) === Number(productId));
        if (!source) {
            return;
        }
        const current = readBookmarkProducts();
        const exists = current.some((product) => Number(product.id) === Number(productId));
        if (exists) {
            writeBookmarkProducts(current.filter((product) => Number(product.id) !== Number(productId)));
        } else {
            const summary = {
                id: source.id,
                brand: source.brand,
                name: source.name,
                headline: source.headline,
                model: source.model,
                category: source.category,
                price: source.price,
                priceLabel: source.priceLabel,
                stock: source.stock,
                stockStatus: source.stockStatus,
                featured: Boolean(source.featured)
            };
            writeBookmarkProducts([summary].concat(current).slice(0, 6));
        }
        renderBookmarkBoard();
        renderCatalog();
    }

    function removeBookmarkProduct(productId) {
        writeBookmarkProducts(readBookmarkProducts().filter((product) => Number(product.id) !== Number(productId)));
        renderBookmarkBoard();
        renderCatalog();
    }

    function readCompareProducts() {
        try {
            const parsed = JSON.parse(window.localStorage.getItem(COMPARE_PRODUCTS_KEY) || "[]");
            return Array.isArray(parsed) ? parsed.filter((item) => item?.id) : [];
        } catch (error) {
            return [];
        }
    }

    function writeCompareProducts(productsToCompare) {
        window.localStorage.setItem(COMPARE_PRODUCTS_KEY, JSON.stringify(productsToCompare));
    }

    function isComparedProduct(productId) {
        return readCompareProducts().some((product) => Number(product.id) === Number(productId));
    }

    function readBookmarkProducts() {
        try {
            const parsed = JSON.parse(window.localStorage.getItem(BOOKMARK_PRODUCTS_KEY) || "[]");
            return Array.isArray(parsed) ? parsed.filter((item) => item?.id) : [];
        } catch (error) {
            return [];
        }
    }

    function writeBookmarkProducts(bookmarkedProducts) {
        window.localStorage.setItem(BOOKMARK_PRODUCTS_KEY, JSON.stringify(bookmarkedProducts));
    }

    function buildCompareSummary(comparedProducts) {
        if (comparedProducts.length < 2) {
            return "하나를 더 담으면 가격과 재고 차이를 더 명확하게 비교할 수 있습니다.";
        }
        const prices = comparedProducts.map((product) => Number(product.price || 0));
        const stocks = comparedProducts.map((product) => Number(product.stock || 0));
        const priceGap = Math.max(...prices) - Math.min(...prices);
        const stockGap = Math.max(...stocks) - Math.min(...stocks);
        const categories = new Set(comparedProducts.map((product) => product.category).filter(Boolean));
        const categoryHint = categories.size === 1 ? "같은 카테고리 안에서 비교 중입니다." : `${categories.size}개 카테고리를 함께 보고 있습니다.`;
        return `최고가와 최저가 차이는 ${formatPrice(priceGap)}, 재고 차이는 ${stockGap}개입니다. ${categoryHint}`;
    }

    function buildBookmarkSummary(bookmarkedProducts) {
        const featuredCount = bookmarkedProducts.filter((product) => product.featured).length;
        const lowStockCount = bookmarkedProducts.filter((product) => Number(product.stock || 0) < lowStockThresholdValue()).length;
        if (!featuredCount) {
            return lowStockCount
                ? `${lowStockCount}개 상품이 긴장 재고 구간에 있어 우선 확인용 보드로 활용할 수 있습니다.`
                : "관심 상품을 모아두고 필요할 때 상세나 비교 보드로 바로 이동할 수 있습니다.";
        }
        return `${featuredCount}개 상품이 Featured 라인에 포함되어 있고, ${lowStockCount}개 상품은 긴장 재고 구간에 있습니다.`;
    }

    function productVisualMarkup(product, className) {
        return `
            <div class="${className}">
                <span class="${className}-badge">${brandInitials(product.brand)}</span>
                <div class="${className}-copy">
                    <strong>${product.category || "Curated"}</strong>
                    <span>${product.model || product.name || "-"}</span>
                </div>
            </div>
        `;
    }

    function brandInitials(brand) {
        return String(brand || "GS")
            .trim()
            .split(/\s+/)
            .map((token) => token.charAt(0))
            .join("")
            .slice(0, 2)
            .toUpperCase();
    }

    function bindProductButtons(container) {
        container.querySelectorAll("[data-product-id]").forEach((button) => {
            button.addEventListener("click", () => openDrawer(Number(button.dataset.productId)));
        });
    }

    async function openDrawer(productId) {
        if (!elements.productDrawer || !elements.drawerBody) {
            return;
        }

        elements.productDrawer.classList.add("is-open");
        elements.productDrawer.setAttribute("aria-hidden", "false");
        elements.drawerBody.innerHTML = `
            <p class="eyebrow">Detail</p>
            <h3>상품 상세를 불러오는 중입니다.</h3>
            <p class="product-drawer__description">선택한 상품 데이터를 확인하고 있습니다.</p>
        `;

        try {
            const product = await loadProductDetail(productId);

            elements.drawerBody.innerHTML = `
            <p class="eyebrow">Detail</p>
            ${productVisualMarkup(product, "product-drawer__visual")}
            <div class="product-drawer__meta">
                <span class="product-drawer__pill ${stockClassName(product.stock)}">${product.stockStatus || stockLabel(product.stock)}</span>
                <span class="product-drawer__pill is-stable-stock">${product.brand}</span>
                ${product.featured ? `<span class="product-drawer__pill">Featured${product.featuredRank ? ` #${product.featuredRank}` : ''}</span>` : ''}
            </div>
            <h3>${product.headline || product.name}</h3>
            <div class="product-drawer__meta">
                <span>${product.name}</span>
                <span>${product.model}</span>
                <span>${product.category}</span>
            </div>
            <p class="product-drawer__description">${product.description}</p>
            <div class="product-drawer__group">
                <div class="product-drawer__overview">
                    <div class="product-drawer__overview-card">
                        <span>카테고리</span>
                        <strong>${product.category}</strong>
                    </div>
                    <div class="product-drawer__overview-card">
                        <span>모델</span>
                        <strong>${product.model}</strong>
                    </div>
                    <div class="product-drawer__overview-card">
                        <span>등록일</span>
                        <strong>${product.createdDate}</strong>
                    </div>
                </div>
            </div>
            <div class="product-drawer__group">
                <strong>발매가</strong>
                <h3>${product.priceLabel || formatPrice(product.price)}</h3>
                <p class="product-drawer__description">현재 총 재고 ${product.stock}개 · 무드 키워드 ${product.mood}</p>
                <div class="product-drawer__cta">
                    <a class="catalog-card__button product-drawer__cta-link" href="${detailPageUrl(product.id)}">상세 페이지 이동</a>
                </div>
            </div>
            <div class="product-drawer__group">
                <strong>사이즈별 재고</strong>
                <div class="product-drawer__options">
                    ${Array.isArray(product.options) && product.options.length ? product.options.map((option) => `
                        <div class="product-drawer__option">
                            <div>
                                <strong>${option.name}</strong>
                                <span>${stockLabel(option.stock)}</span>
                            </div>
                            <strong>${option.stock}개</strong>
                        </div>
                    `).join("") : `
                        <div class="product-drawer__option">
                            <div>
                                <strong>등록된 옵션이 없습니다.</strong>
                                <span>세부 옵션 정보가 아직 준비되지 않았습니다.</span>
                            </div>
                            <strong>-</strong>
                        </div>
                    `}
                </div>
            </div>
            ${Array.isArray(product.relatedProducts) && product.relatedProducts.length ? `
            <div class="product-drawer__group">
                <strong>연관 상품</strong>
                <div class="product-drawer__related-list">
                    ${product.relatedProducts.map((related) => `
                        <button class="product-drawer__related-card" type="button" data-product-id="${related.id}">
                            ${productVisualMarkup(related, "product-drawer__related-visual")}
                            <span class="product-drawer__related-brand">${related.brand}</span>
                            <strong>${related.name}</strong>
                            <span class="product-drawer__related-meta">${related.reason} · ${related.model} · ${related.priceLabel || formatPrice(related.price)} · ${related.stockStatus || stockLabel(related.stock)}</span>
                        </button>
                    `).join("")}
                </div>
            </div>
            ` : ""}
        `;
            bindProductButtons(elements.drawerBody);
        } catch (error) {
            elements.drawerBody.innerHTML = `
                <p class="eyebrow">Detail</p>
                <h3>상품 상세를 불러오지 못했습니다.</h3>
                <p class="product-drawer__description">잠시 후 다시 시도해주세요.</p>
            `;
        }
    }

    async function loadProductDetail(productId) {
        if (detailCache.has(productId)) {
            return detailCache.get(productId);
        }
        const response = await fetch(`/api/front/products/${productId}`);
        if (!response.ok) {
            throw new Error("상품 상세를 불러오지 못했습니다.");
        }
        const product = await response.json();
        detailCache.set(productId, product);
        return product;
    }

    function closeDrawer() {
        if (!elements.productDrawer) {
            return;
        }
        elements.productDrawer.classList.remove("is-open");
        elements.productDrawer.setAttribute("aria-hidden", "true");
    }

    function stockLabel(stock) {
        return stock < lowStockThresholdValue() ? "품절 임박" : "재고 안정";
    }

    function stockClassName(stock) {
        return stock < lowStockThresholdValue() ? "is-low-stock" : "is-stable-stock";
    }

    function lowStockThresholdValue() {
        return Number(state.lowStockThreshold || 20);
    }

    function formatPrice(price) {
        return `${Number(price).toLocaleString("ko-KR")}원`;
    }

    function setText(element, text) {
        if (element) {
            element.textContent = text;
        }
    }

    init();
})();
