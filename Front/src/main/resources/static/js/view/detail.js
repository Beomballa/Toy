(function () {
    const bootstrap = window.frontProductDetailBootstrap || {};
    const productId = Number(bootstrap.productId || 0);
    const RECENT_VIEWED_KEY = "front-recent-viewed-products";
    const RECENT_VIEWED_LIMIT = 6;

    const elements = {
        detailTitle: document.getElementById("detailTitle"),
        detailDescription: document.getElementById("detailDescription"),
        detailPrice: document.getElementById("detailPrice"),
        detailStockText: document.getElementById("detailStockText"),
        detailMetaRow: document.getElementById("detailMetaRow"),
        detailSignalList: document.getElementById("detailSignalList"),
        detailOverviewGrid: document.getElementById("detailOverviewGrid"),
        detailOptionGrid: document.getElementById("detailOptionGrid"),
        detailRelatedGrid: document.getElementById("detailRelatedGrid"),
        detailRecentSection: document.getElementById("detailRecentSection"),
        detailRecentGrid: document.getElementById("detailRecentGrid"),
        detailFocusRelated: document.getElementById("detailFocusRelated"),
        backToCatalogLink: document.getElementById("backToCatalogLink"),
        detailCatalogLink: document.getElementById("detailCatalogLink")
    };

    function formatPrice(price) {
        return `${Number(price || 0).toLocaleString("ko-KR")}원`;
    }

    function lowStockThreshold() {
        return 20;
    }

    function stockLabel(stock) {
        return Number(stock || 0) < lowStockThreshold() ? "품절 임박" : "재고 안정";
    }

    function stockClassName(stock) {
        return Number(stock || 0) < lowStockThreshold() ? "is-low-stock" : "is-stable-stock";
    }

    function renderMeta(product) {
        if (!elements.detailMetaRow) {
            return;
        }
        elements.detailMetaRow.innerHTML = `
            <span class="product-drawer__pill ${stockClassName(product.stock)}">${product.stockStatus || stockLabel(product.stock)}</span>
            <span class="product-drawer__pill is-stable-stock">${product.brand}</span>
            <span class="product-drawer__pill is-stable-stock">${product.category}</span>
            ${product.featured ? `<span class="product-drawer__pill">Featured${product.featuredRank ? ` #${product.featuredRank}` : ""}</span>` : ""}
        `;
    }

    function renderSignals(product) {
        if (!elements.detailSignalList) {
            return;
        }
        const signals = [
            `${product.stock}개 재고로 ${product.stockStatus || stockLabel(product.stock)} 상태입니다.`,
            `${product.model} 모델 기준으로 ${product.category} 라인에 포함됩니다.`,
            `${product.relatedProducts?.length || 0}개의 연관 상품이 함께 추천됩니다.`
        ];
        elements.detailSignalList.innerHTML = signals.map((message, index) => `
            <article class="signal-card">
                <strong>Signal 0${index + 1}</strong>
                <span>${message}</span>
            </article>
        `).join("");
    }

    function renderOverview(product) {
        if (!elements.detailOverviewGrid) {
            return;
        }
        const items = [
            ["브랜드", product.brand],
            ["카테고리", product.category],
            ["모델", product.model],
            ["등록일", product.createdDate],
            ["무드", product.mood],
            ["대표 노출", product.featured ? (product.featuredRank ? `Featured #${product.featuredRank}` : "Featured") : "일반"]
        ];
        elements.detailOverviewGrid.innerHTML = items.map(([label, value]) => `
            <article class="detail-info-card">
                <span>${label}</span>
                <strong>${value || "-"}</strong>
            </article>
        `).join("");
    }

    function renderOptions(product) {
        if (!elements.detailOptionGrid) {
            return;
        }
        const options = Array.isArray(product.options) ? product.options : [];
        if (!options.length) {
            elements.detailOptionGrid.innerHTML = `
                <article class="catalog-empty">
                    <strong>등록된 옵션이 없습니다.</strong>
                    <p>현재 상품에는 사이즈별 재고 정보가 없습니다.</p>
                </article>
            `;
            return;
        }
        elements.detailOptionGrid.innerHTML = options.map((option) => `
            <article class="detail-option-card">
                <span>${option.name}</span>
                <strong>${option.stock}개</strong>
                <em class="${stockClassName(option.stock)}">${stockLabel(option.stock)}</em>
            </article>
        `).join("");
    }

    function renderRelated(product) {
        if (!elements.detailRelatedGrid) {
            return;
        }
        const related = Array.isArray(product.relatedProducts) ? product.relatedProducts : [];
        if (!related.length) {
            elements.detailRelatedGrid.innerHTML = `
                <article class="catalog-empty">
                    <strong>연관 상품이 없습니다.</strong>
                    <p>현재 상품 기준으로 추천 가능한 비교 상품이 없습니다.</p>
                </article>
            `;
            return;
        }
        elements.detailRelatedGrid.innerHTML = related.map((item) => `
            <a class="detail-related-card" href="${buildProductUrl(item.id)}">
                <span class="detail-related-card__brand">${item.brand}</span>
                <strong>${item.name}</strong>
                <p>${item.reason}</p>
                <div class="detail-related-card__meta">
                    <span>${item.model}</span>
                    <span>${item.priceLabel || formatPrice(item.price)}</span>
                    <span class="${stockClassName(item.stock)}">${item.stockStatus || stockLabel(item.stock)}</span>
                </div>
            </a>
        `).join("");
    }

    function syncCatalogLinks() {
        const catalogUrl = buildCatalogUrl();
        if (elements.backToCatalogLink) {
            elements.backToCatalogLink.href = catalogUrl;
        }
        if (elements.detailCatalogLink) {
            elements.detailCatalogLink.href = `${catalogUrl}#catalog`;
        }
    }

    function buildCatalogUrl() {
        return `/front${window.location.search || ""}`;
    }

    function buildProductUrl(nextProductId) {
        return `/front/products/${nextProductId}${window.location.search || ""}`;
    }

    function saveRecentProduct(product) {
        if (!product?.id) {
            return;
        }
        const previous = readRecentProducts().filter((item) => Number(item.id) !== Number(product.id));
        const current = {
            id: product.id,
            brand: product.brand,
            name: product.name,
            headline: product.headline,
            model: product.model,
            price: product.price,
            priceLabel: product.priceLabel,
            stock: product.stock,
            stockStatus: product.stockStatus
        };
        const next = [current].concat(previous).slice(0, RECENT_VIEWED_LIMIT);
        window.localStorage.setItem(RECENT_VIEWED_KEY, JSON.stringify(next));
    }

    function readRecentProducts() {
        try {
            const parsed = JSON.parse(window.localStorage.getItem(RECENT_VIEWED_KEY) || "[]");
            return Array.isArray(parsed) ? parsed : [];
        } catch (error) {
            return [];
        }
    }

    function renderRecentProducts(currentProductId) {
        if (!elements.detailRecentSection || !elements.detailRecentGrid) {
            return;
        }
        const recentProducts = readRecentProducts().filter((item) => Number(item.id) !== Number(currentProductId)).slice(0, 3);
        if (!recentProducts.length) {
            elements.detailRecentSection.hidden = true;
            return;
        }
        elements.detailRecentSection.hidden = false;
        elements.detailRecentGrid.innerHTML = recentProducts.map((item) => `
            <a class="detail-related-card" href="${buildProductUrl(item.id)}">
                <span class="detail-related-card__brand">${item.brand || "-"}</span>
                <strong>${item.headline || item.name || "-"}</strong>
                <p>${item.name || "-"} · ${item.model || "-"}</p>
                <div class="detail-related-card__meta">
                    <span>${item.priceLabel || formatPrice(item.price)}</span>
                    <span class="${stockClassName(item.stock)}">${item.stockStatus || stockLabel(item.stock)}</span>
                    <span>다시 보기</span>
                </div>
            </a>
        `).join("");
    }

    async function init() {
        if (!productId) {
            return;
        }
        syncCatalogLinks();
        elements.detailFocusRelated?.addEventListener("click", () => {
            document.getElementById("detailRelated")?.scrollIntoView({ behavior: "smooth", block: "start" });
        });
        try {
            const response = await fetch(`/api/front/products/${productId}`);
            if (!response.ok) {
                throw new Error("상품 상세를 불러오지 못했습니다.");
            }
            const product = await response.json();
            document.title = `${product.name} | Grade Stock`;
            if (elements.detailTitle) {
                elements.detailTitle.textContent = product.headline || product.name;
            }
            if (elements.detailDescription) {
                elements.detailDescription.textContent = product.description || "상품 설명이 아직 등록되지 않았습니다.";
            }
            if (elements.detailPrice) {
                elements.detailPrice.textContent = product.priceLabel || formatPrice(product.price);
            }
            if (elements.detailStockText) {
                elements.detailStockText.textContent = `총 재고 ${product.stock}개 · 등록 ${product.createdDate || "-"}`;
            }
            saveRecentProduct(product);
            renderMeta(product);
            renderSignals(product);
            renderOverview(product);
            renderOptions(product);
            renderRelated(product);
            renderRecentProducts(product.id);
        } catch (error) {
            if (elements.detailTitle) {
                elements.detailTitle.textContent = "상품 상세를 불러오지 못했습니다.";
            }
            if (elements.detailDescription) {
                elements.detailDescription.textContent = "잠시 후 다시 시도해주세요.";
            }
        }
    }

    init();
})();
