(function () {
    const LOW_STOCK_THRESHOLD = 20;
    const state = {
        search: "",
        brand: "ALL",
        category: "ALL",
        stock: "ALL",
        sort: "LATEST"
    };
    let products = [];

    const elements = {
        brandFilter: document.getElementById("brandFilter"),
        categoryFilter: document.getElementById("categoryFilter"),
        stockFilter: document.getElementById("stockFilter"),
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
            const response = await fetch("/api/front/products");
            if (!response.ok) {
                throw new Error("상품 데이터를 불러오지 못했습니다.");
            }
            const payload = await response.json();
            products = Array.isArray(payload) ? payload.slice() : [];
        } catch (error) {
            products = [];
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
    }

    function populateFilters() {
        fillSelect(elements.brandFilter, "전체 브랜드", uniqueValues("brand"));
        fillSelect(elements.categoryFilter, "전체 카테고리", uniqueValues("category"));
    }

    function fillSelect(select, defaultLabel, values) {
        if (!select) {
            return;
        }
        select.innerHTML = [`<option value="ALL">${defaultLabel}</option>`]
            .concat(values.map((value) => `<option value="${value}">${value}</option>`))
            .join("");
    }

    function uniqueValues(key) {
        return Array.from(new Set(products.map((product) => product[key]))).sort((a, b) => a.localeCompare(b));
    }

    function bindEvents() {
        elements.searchInput?.addEventListener("input", (event) => {
            state.search = event.target.value.trim().toLowerCase();
            renderCatalog();
        });
        elements.brandFilter?.addEventListener("change", (event) => {
            state.brand = event.target.value;
            renderCatalog();
        });
        elements.categoryFilter?.addEventListener("change", (event) => {
            state.category = event.target.value;
            renderCatalog();
        });
        elements.stockFilter?.addEventListener("change", (event) => {
            state.stock = event.target.value;
            renderCatalog();
        });
        elements.sortFilter?.addEventListener("change", (event) => {
            state.sort = event.target.value;
            renderCatalog();
        });
        elements.closeDrawerButton?.addEventListener("click", closeDrawer);
        elements.productDrawer?.addEventListener("click", (event) => {
            if (event.target === elements.productDrawer) {
                closeDrawer();
            }
        });
        elements.focusLowStockButton?.addEventListener("click", () => {
            state.stock = "LOW";
            if (elements.stockFilter) {
                elements.stockFilter.value = "LOW";
            }
            renderCatalog();
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
        const todayCount = products.filter((product) => product.createdDate === "2026-06-04").length;
        const lowStockCount = products.filter((product) => product.stock < LOW_STOCK_THRESHOLD).length;

        setText(elements.metricCount, String(products.length));
        setText(elements.metricLowStock, String(lowStockCount));
        setText(elements.metricToday, String(todayCount));
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
                        <span class="spotlight-card__pill ${stockClassName(product.stock)}">${stockLabel(product.stock)}</span>
                    </div>
                    <h3 class="spotlight-card__title">${product.name}</h3>
                    <div class="spotlight-card__meta">
                        <span>${product.category}</span>
                        <span>${product.model}</span>
                    </div>
                </div>
                <div class="spotlight-card__footer">
                    <div>
                        <div class="spotlight-card__price">${formatPrice(product.price)}</div>
                        <div class="catalog-card__meta">총 재고 ${product.stock}개</div>
                    </div>
                    <button class="catalog-card__button" type="button" data-product-id="${product.id}">상세 보기</button>
                </div>
            </article>
        `).join("");

        bindProductButtons(elements.featuredGrid);
    }

    function renderSignals() {
        if (!elements.signalList) {
            return;
        }

        const todayProducts = products
            .filter((product) => product.createdDate === "2026-06-04")
            .sort((left, right) => left.stock - right.stock);
        const primarySignal = todayProducts[0] || products[0];

        if (primarySignal) {
            setText(elements.todaySignalTitle, `${primarySignal.name}이 오늘 기준 가장 빠른 반응을 보이고 있습니다.`);
            setText(elements.todaySignalText, `${primarySignal.brand} · ${primarySignal.category} · 재고 ${primarySignal.stock}개`);
        }

        const signals = [
            `${products.filter((product) => product.featured).length}개 상품이 이번 주 큐레이션에 묶여 있습니다.`,
            `${products.filter((product) => product.stock < LOW_STOCK_THRESHOLD).length}개 상품이 재고 긴장 구간에 있습니다.`,
            `${products[0] ? products.reduce((sum, product) => sum + product.stock, 0) : 0}개 재고를 첫 화면 기준으로 추적 중입니다.`
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
                        <h3 class="catalog-card__title">${product.name}</h3>
                        <div class="catalog-card__meta">
                            <span>${product.category}</span>
                            <span>${product.model}</span>
                        </div>
                    </div>
                    <span class="catalog-card__pill ${stockClassName(product.stock)}">${stockLabel(product.stock)}</span>
                </div>
                <p class="catalog-card__copy">${product.description}</p>
                <div class="catalog-card__footer">
                    <div>
                        <div class="catalog-card__price">${formatPrice(product.price)}</div>
                        <div class="catalog-card__meta">총 재고 ${product.stock}개 · ${product.createdDate}</div>
                    </div>
                    <div class="catalog-card__action">
                        <div class="catalog-card__meta">${product.mood}</div>
                        <button class="catalog-card__button" type="button" data-product-id="${product.id}">상세 보기</button>
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
            tags.push("품절 임박");
        }
        if (state.stock === "STABLE") {
            tags.push("재고 안정");
        }
        if (state.search) {
            tags.push(`검색 ${state.search}`);
        }
        if (!tags.length) {
            tags.push("전체 탐색");
        }

        elements.catalogTags.innerHTML = tags.map((tag) => `<span class="catalog-tag">${tag}</span>`).join("");
    }

    function filteredProducts() {
        return products
            .filter((product) => {
                if (state.brand !== "ALL" && product.brand !== state.brand) {
                    return false;
                }
                if (state.category !== "ALL" && product.category !== state.category) {
                    return false;
                }
                if (state.stock === "LOW" && product.stock >= LOW_STOCK_THRESHOLD) {
                    return false;
                }
                if (state.stock === "STABLE" && product.stock < LOW_STOCK_THRESHOLD) {
                    return false;
                }
                if (!state.search) {
                    return true;
                }
                const searchBase = `${product.name} ${product.model} ${product.brand}`.toLowerCase();
                return searchBase.includes(state.search);
            })
            .sort(sortComparator(state.sort));
    }

    function sortComparator(sortType) {
        if (sortType === "PRICE_HIGH") {
            return (left, right) => right.price - left.price;
        }
        if (sortType === "STOCK_ASC") {
            return (left, right) => left.stock - right.stock;
        }
        return (left, right) => right.createdDate.localeCompare(left.createdDate);
    }

    function bindProductButtons(container) {
        container.querySelectorAll("[data-product-id]").forEach((button) => {
            button.addEventListener("click", () => openDrawer(Number(button.dataset.productId)));
        });
    }

    function openDrawer(productId) {
        const product = products.find((candidate) => candidate.id === productId);
        if (!product || !elements.productDrawer || !elements.drawerBody) {
            return;
        }

        elements.drawerBody.innerHTML = `
            <p class="eyebrow">Detail</p>
            <div class="product-drawer__meta">
                <span class="product-drawer__pill ${stockClassName(product.stock)}">${stockLabel(product.stock)}</span>
                <span class="product-drawer__pill is-stable-stock">${product.brand}</span>
            </div>
            <h3>${product.name}</h3>
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
                <h3>${formatPrice(product.price)}</h3>
                <p class="product-drawer__description">현재 총 재고 ${product.stock}개 · 무드 키워드 ${product.mood}</p>
            </div>
            <div class="product-drawer__group">
                <strong>사이즈별 재고</strong>
                <div class="product-drawer__options">
                    ${product.options.map((option) => `
                        <div class="product-drawer__option">
                            <span>${option.name}</span>
                            <strong>${option.stock}개</strong>
                        </div>
                    `).join("")}
                </div>
            </div>
        `;

        elements.productDrawer.classList.add("is-open");
        elements.productDrawer.setAttribute("aria-hidden", "false");
    }

    function closeDrawer() {
        if (!elements.productDrawer) {
            return;
        }
        elements.productDrawer.classList.remove("is-open");
        elements.productDrawer.setAttribute("aria-hidden", "true");
    }

    function stockLabel(stock) {
        return stock < LOW_STOCK_THRESHOLD ? "품절 임박" : "재고 안정";
    }

    function stockClassName(stock) {
        return stock < LOW_STOCK_THRESHOLD ? "is-low-stock" : "is-stable-stock";
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
