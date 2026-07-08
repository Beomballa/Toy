(function () {
    const state = {
        search: "",
        brand: "ALL",
        category: "ALL",
        stock: "ALL",
        sort: "LATEST",
        lowStockThreshold: "20",
        featuredOnly: "ALL",
        priceBand: "ALL"
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
        featuredGrid: document.getElementById("featuredGrid"),
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
        openDrawerFromTop: document.getElementById("openDrawerFromTop")
    };

    async function init() {
        await loadProducts();
        populateFilters();
        bindEvents();
        renderHeroMetrics();
        renderFeatured();
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
    }

    function renderHeroMetrics() {
        setText(elements.metricCount, String(metrics.totalCount || products.length));
        setText(elements.metricLowStock, String(metrics.lowStockCount || 0));
        setText(elements.metricToday, String(metrics.latestDropCount || 0));
    }

    function renderFeatured() {
        if (!elements.featuredGrid) {
            return;
        }
        const featuredProducts = products.filter((product) => product.featured).slice(0, 3);
        elements.featuredGrid.innerHTML = featuredProducts.map((product, index) => `
            <article class="spotlight-card ${index === 0 ? "spotlight-card--accent" : ""}">
                <div>
                    <div class="spotlight-card__top">
                        <span class="spotlight-card__label">${product.brand}</span>
                        <span class="spotlight-card__pill ${stockClassName(product.stock)}">${product.stockStatus || stockLabel(product.stock)}</span>
                    </div>
                    <h3 class="spotlight-card__title">${product.headline || product.name}</h3>
                    <div class="spotlight-card__meta">
                        <span>${product.name}</span>
                        <span>${product.category}</span>
                        <span>${product.featuredRank ? `Featured ${product.featuredRank}` : product.model}</span>
                    </div>
                </div>
                <div class="spotlight-card__footer">
                    <div>
                        <div class="spotlight-card__price">${product.priceLabel || formatPrice(product.price)}</div>
                        <div class="catalog-card__meta">총 재고 ${product.stock}개</div>
                    </div>
                    <div class="catalog-card__action">
                        <a class="catalog-card__link" href="/front/products/${product.id}">페이지 보기</a>
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
        const primarySignal = latestProducts[0] || products[0];

        if (primarySignal) {
            setText(elements.todaySignalTitle, `${primarySignal.name}이 최신 드롭 기준 가장 빠른 반응을 보이고 있습니다.`);
            setText(elements.todaySignalText, `${primarySignal.brand} · ${primarySignal.category} · 재고 ${primarySignal.stock}개`);
        }

        const signals = [
            `${metrics.featuredCount || products.filter((product) => product.featured).length}개 상품이 이번 주 큐레이션에 묶여 있습니다.`,
            `${metrics.lowStockCount || products.filter((product) => product.stock < lowStockThresholdValue()).length}개 상품이 재고 긴장 구간에 있습니다.`,
            `${metrics.totalStock || (products[0] ? products.reduce((sum, product) => sum + product.stock, 0) : 0)}개 재고를 첫 화면 기준으로 추적 중입니다.`
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
                <div class="catalog-card__header">
                    <div>
                        <span class="catalog-card__label">${product.brand}</span>
                        <h3 class="catalog-card__title">${product.headline || product.name}</h3>
                        <div class="catalog-card__meta">
                            <span>${product.name}</span>
                            <span>${product.category}</span>
                            <span>${product.model}</span>
                        </div>
                    </div>
                    <span class="catalog-card__pill ${stockClassName(product.stock)}">${product.stockStatus || stockLabel(product.stock)}</span>
                </div>
                <p class="catalog-card__copy">${product.description}</p>
                <div class="catalog-card__footer">
                    <div>
                        <div class="catalog-card__price">${product.priceLabel || formatPrice(product.price)}</div>
                        <div class="catalog-card__meta">총 재고 ${product.stock}개 · ${product.createdDate}</div>
                    </div>
                    <div class="catalog-card__action">
                        <div class="catalog-card__meta">${product.mood}</div>
                        <a class="catalog-card__link" href="/front/products/${product.id}">페이지 보기</a>
                        <button class="catalog-card__button" type="button" data-product-id="${product.id}">빠른 보기</button>
                    </div>
                </div>
            </article>
        `).join("");

        bindProductButtons(elements.catalogGrid);
    }

    function renderCatalogSummary(list) {
        setText(elements.catalogCountText, `${list.length}개 상품이 현재 조건에 맞습니다.`);
        if (!elements.catalogTags) {
            return;
        }

        const tags = [];
        if (state.brand !== "ALL") {
            tags.push(`브랜드 ${state.brand}`);
        }
        if (state.category !== "ALL") {
            tags.push(`카테고리 ${state.category}`);
        }
        if (state.stock === "LOW") {
            tags.push(`품절 임박 ${state.lowStockThreshold}개 미만`);
        }
        if (state.stock === "STABLE") {
            tags.push(`재고 안정 ${state.lowStockThreshold}개 이상`);
        }
        if (state.featuredOnly === "FEATURED") {
            tags.push("Featured만");
        }
        if (state.priceBand === "UNDER_200") {
            tags.push("20만원 미만");
        }
        if (state.priceBand === "BETWEEN_200_300") {
            tags.push("20만원-30만원");
        }
        if (state.priceBand === "OVER_300") {
            tags.push("30만원 초과");
        }
        if (state.search) {
            tags.push(`검색 ${state.search}`);
        }
        if (state.sort === "FEATURED") {
            tags.push("대표 노출순");
        }
        if (state.sort === "NAME_ASC") {
            tags.push("상품명 오름차순");
        }
        if (state.sort === "PRICE_HIGH") {
            tags.push("발매가 높은 순");
        }
        if (state.sort === "PRICE_LOW") {
            tags.push("발매가 낮은 순");
        }
        if (state.sort === "STOCK_ASC") {
            tags.push("재고 낮은 순");
        }
        if (state.sort === "STOCK_DESC") {
            tags.push("재고 높은 순");
        }
        if (!tags.length) {
            tags.push("전체 탐색");
        }

        elements.catalogTags.innerHTML = tags.map((tag) => `<span class="catalog-tag">${tag}</span>`).join("");
    }

    function filteredProducts() {
        return products.slice();
    }

    async function refreshCatalog() {
        await loadProducts();
        populateFilters();
        renderHeroMetrics();
        renderFeatured();
        renderSignals();
        renderCatalog();
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
            <div class="product-drawer__meta">
                <span class="product-drawer__pill ${stockClassName(product.stock)}">${product.stockStatus || stockLabel(product.stock)}</span>
                <span class="product-drawer__pill is-stable-stock">${product.brand}</span>
                ${product.featured ? `<span class="product-drawer__pill">Featured${product.featuredRank ? ` #${product.featuredRank}` : ''}</span>` : ''}
            </div>
            <h3>${product.headline || product.name}</h3>
            <div class="product-drawer__meta">
                <span>${product.name}</span>
            </div>
            <p class="product-drawer__description">${product.description}</p>
            <div class="product-drawer__group">
                <div class="product-drawer__meta">
                    <span>카테고리 ${product.category}</span>
                    <span>모델 ${product.model}</span>
                    <span>등록 ${product.createdDate}</span>
                </div>
            </div>
            <div class="product-drawer__group">
                <strong>발매가</strong>
                <h3>${product.priceLabel || formatPrice(product.price)}</h3>
                <p class="product-drawer__description">현재 총 재고 ${product.stock}개 · 무드 키워드 ${product.mood}</p>
                <div class="product-drawer__cta">
                    <a class="catalog-card__button product-drawer__cta-link" href="/front/products/${product.id}">상세 페이지 이동</a>
                </div>
            </div>
            <div class="product-drawer__group">
                <strong>사이즈별 재고</strong>
                <div class="product-drawer__options">
                    ${Array.isArray(product.options) && product.options.length ? product.options.map((option) => `
                        <div class="product-drawer__option">
                            <span>${option.name}</span>
                            <strong>${option.stock}개</strong>
                        </div>
                    `).join("") : `
                        <div class="product-drawer__option">
                            <span>등록된 옵션이 없습니다.</span>
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
