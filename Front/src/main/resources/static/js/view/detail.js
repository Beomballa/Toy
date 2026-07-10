(function () {
    const bootstrap = window.frontProductDetailBootstrap || {};
    const productId = Number(bootstrap.productId || 0);
    const RECENT_VIEWED_KEY = "front-recent-viewed-products";
    const RECENT_VIEWED_LIMIT = 6;
    const optionSortState = {
        mode: "STOCK_ASC"
    };
    let currentProduct = null;
    let toastTimerSeed = 0;

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
        detailShareButton: document.getElementById("detailShareButton"),
        detailCopySummaryButton: document.getElementById("detailCopySummaryButton"),
        detailSectionNav: document.getElementById("detailSectionNav"),
        detailOptionSortStockButton: document.getElementById("detailOptionSortStockButton"),
        detailOptionSortNameButton: document.getElementById("detailOptionSortNameButton"),
        clearDetailRecentButton: document.getElementById("clearDetailRecentButton"),
        copyDetailRecentSummaryButton: document.getElementById("copyDetailRecentSummaryButton"),
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

    function stockPressureDetail(stock) {
        const quantity = Number(stock || 0);
        if (quantity <= 5) {
            return `재고 ${quantity}개로 즉시 확인이 필요합니다.`;
        }
        if (quantity < lowStockThreshold()) {
            return `재고 ${quantity}개로 긴장 구간에 들어가 있습니다.`;
        }
        return `재고 ${quantity}개로 안정적으로 유지되고 있습니다.`;
    }

    function brandInitials(brand) {
        if (!brand) {
            return "GS";
        }
        return brand
            .split(/\s+/)
            .filter(Boolean)
            .slice(0, 2)
            .map((token) => token.charAt(0).toUpperCase())
            .join("");
    }

    function productVisualMarkup(product, className) {
        return `
            <div class="${className}">
                <span class="${className}__badge">${brandInitials(product.brand)}</span>
                <div class="${className}__copy">
                    <strong>${product.brand || "Grade Stock"}</strong>
                    <span>${product.category || product.model || product.reason || "Curated pick"}</span>
                </div>
            </div>
        `;
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
            stockPressureDetail(product.stock),
            `${product.model} 모델 기준으로 ${product.category} 라인에 포함되며 무드 키워드는 ${product.mood || "Curated"}입니다.`,
            `${product.relatedProducts?.length || 0}개의 연관 상품과 ${product.options?.length || 0}개의 옵션 구성을 함께 확인할 수 있습니다.`
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
        syncOptionSortButtons();
        const options = sortedOptions(product);
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
                <p>${stockPressureDetail(option.stock)}</p>
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
                ${productVisualMarkup(item, "detail-related-card__visual")}
                <span class="detail-related-card__brand">${item.brand}</span>
                <strong>${item.name}</strong>
                <p>${item.reason} · ${stockPressureDetail(item.stock)}</p>
                <div class="detail-related-card__meta">
                    <span>모델 ${item.model}</span>
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
                ${productVisualMarkup(item, "detail-related-card__visual")}
                <span class="detail-related-card__brand">${item.brand || "-"}</span>
                <strong>${item.headline || item.name || "-"}</strong>
                <p>${item.name || "-"} · ${item.model || "-"} · ${stockPressureDetail(item.stock)}</p>
                <div class="detail-related-card__meta">
                    <span>최근 본 흐름</span>
                    <span>${item.priceLabel || formatPrice(item.price)}</span>
                    <span class="${stockClassName(item.stock)}">${item.stockStatus || stockLabel(item.stock)}</span>
                    <span>다시 보기</span>
                </div>
            </a>
        `).join("");
    }

    function sortedOptions(product) {
        const options = Array.isArray(product?.options) ? product.options.slice() : [];
        if (optionSortState.mode === "NAME_ASC") {
            return options.sort((left, right) => String(left.name || "").localeCompare(String(right.name || ""), "ko"));
        }
        return options.sort((left, right) => Number(left.stock || 0) - Number(right.stock || 0));
    }

    function syncOptionSortButtons() {
        elements.detailOptionSortStockButton?.classList.toggle("is-active", optionSortState.mode === "STOCK_ASC");
        elements.detailOptionSortNameButton?.classList.toggle("is-active", optionSortState.mode === "NAME_ASC");
    }

    function summaryText(product) {
        return [
            product.headline || product.name,
            product.brand,
            product.category,
            product.model,
            product.priceLabel || formatPrice(product.price),
            stockPressureDetail(product.stock)
        ].filter(Boolean).join(" · ");
    }

    function initSectionNavigation() {
        const navLinks = Array.from(document.querySelectorAll(".detail-section-nav a[href^=\"#\"]"));
        if (!navLinks.length || typeof IntersectionObserver === "undefined") {
            return;
        }
        const sections = navLinks
            .map((link) => {
                const section = document.querySelector(link.getAttribute("href"));
                return section ? { link, section } : null;
            })
            .filter(Boolean);
        const observer = new IntersectionObserver((entries) => {
            const visible = entries
                .filter((entry) => entry.isIntersecting)
                .sort((left, right) => right.intersectionRatio - left.intersectionRatio)[0];
            if (!visible) {
                return;
            }
            sections.forEach(({ link, section }) => {
                link.classList.toggle("is-active", section === visible.target);
            });
        }, {
            rootMargin: "-25% 0px -55% 0px",
            threshold: [0.2, 0.45, 0.7]
        });
        sections.forEach(({ section }) => observer.observe(section));
    }

    function showToast(title, body, isWarning = false) {
        const stack = ensureToastStack();
        if (!stack) {
            return;
        }
        const toast = document.createElement("article");
        toast.className = `toast${isWarning ? " is-warning" : ""}`;
        toast.innerHTML = `<strong>${title}</strong><span>${body}</span>`;
        toast.dataset.toastId = String(++toastTimerSeed);
        stack.appendChild(toast);
        window.setTimeout(() => {
            toast.remove();
            if (!stack.childElementCount) {
                stack.remove();
            }
        }, 2600);
    }

    function ensureToastStack() {
        let stack = document.querySelector(".toast-stack");
        if (stack) {
            return stack;
        }
        stack = document.createElement("div");
        stack.className = "toast-stack";
        document.body.appendChild(stack);
        return stack;
    }

    async function init() {
        if (!productId) {
            return;
        }
        syncCatalogLinks();
        initSectionNavigation();
        elements.detailFocusRelated?.addEventListener("click", () => {
            document.getElementById("detailRelated")?.scrollIntoView({ behavior: "smooth", block: "start" });
        });
        elements.detailShareButton?.addEventListener("click", async () => {
            const shareUrl = `${window.location.origin}${window.location.pathname}${window.location.search}`;
            try {
                if (navigator.clipboard?.writeText) {
                    await navigator.clipboard.writeText(shareUrl);
                }
                showToast("상품 URL을 복사했습니다.", "같은 탐색 조건까지 함께 공유됩니다.");
            } catch (error) {
                window.prompt("현재 상품 URL을 복사하세요.", shareUrl);
            }
        });
        elements.detailCopySummaryButton?.addEventListener("click", async () => {
            if (!currentProduct) {
                return;
            }
            const text = summaryText(currentProduct);
            try {
                if (navigator.clipboard?.writeText) {
                    await navigator.clipboard.writeText(text);
                }
                showToast("상품 요약을 복사했습니다.", "메신저나 문서에 바로 붙여 넣을 수 있습니다.");
            } catch (error) {
                window.prompt("상품 요약을 복사하세요.", text);
            }
        });
        elements.detailOptionSortStockButton?.addEventListener("click", () => {
            optionSortState.mode = "STOCK_ASC";
            if (currentProduct) {
                renderOptions(currentProduct);
                showToast("옵션을 재고 낮은 순으로 정렬했습니다.", "품절 임박 옵션을 먼저 확인할 수 있습니다.");
            }
        });
        elements.detailOptionSortNameButton?.addEventListener("click", () => {
            optionSortState.mode = "NAME_ASC";
            if (currentProduct) {
                renderOptions(currentProduct);
                showToast("옵션을 이름순으로 정렬했습니다.", "사이즈/옵션 라인을 더 빠르게 찾을 수 있습니다.");
            }
        });
        elements.clearDetailRecentButton?.addEventListener("click", () => {
            window.localStorage.removeItem(RECENT_VIEWED_KEY);
            renderRecentProducts(productId);
            showToast("최근 본 상품을 비웠습니다.", "상세 최근 흐름 보드가 초기화되었습니다.");
        });
        elements.copyDetailRecentSummaryButton?.addEventListener("click", async () => {
            const recentProducts = readRecentProducts().filter((item) => Number(item.id) !== Number(productId)).slice(0, 3);
            const text = recentProducts.length
                ? recentProducts.map((item, index) => `${index + 1}. ${item.headline || item.name} · ${item.model || "-"} · ${item.priceLabel || formatPrice(item.price)}`).join("\n")
                : "최근 본 상품이 없습니다.";
            try {
                if (navigator.clipboard?.writeText) {
                    await navigator.clipboard.writeText(text);
                }
                showToast("최근 흐름을 복사했습니다.", "상세에서 이어 본 상품 목록을 바로 전달할 수 있습니다.");
            } catch (error) {
                window.prompt("최근 흐름을 복사하세요.", text);
            }
        });
        try {
            const response = await fetch(`/api/front/products/${productId}`);
            if (!response.ok) {
                throw new Error("상품 상세를 불러오지 못했습니다.");
            }
            const product = await response.json();
            currentProduct = product;
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
