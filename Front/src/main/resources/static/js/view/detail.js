(function () {
    const bootstrap = window.frontProductDetailBootstrap || {};
    const productId = Number(bootstrap.productId || 0);
    const BOOKMARK_PRODUCTS_KEY = "front-bookmark-products";
    const COMPARE_PRODUCTS_KEY = "front-compare-products";
    const RECENT_VIEWED_KEY = "front-recent-viewed-products";
    const RECENT_VIEWED_LIMIT = 6;
    const optionSortState = {
        mode: "STOCK_ASC",
        lowStockOnly: false,
        stableOnly: false
    };
    const relatedSortState = {
        mode: "DEFAULT",
        lowStockOnly: false,
        sameBrandOnly: false
    };
    let currentProduct = null;
    let toastTimerSeed = 0;

    const elements = {
        detailTitle: document.getElementById("detailTitle"),
        detailVisualBrand: document.getElementById("detailVisualBrand"),
        detailVisualModel: document.getElementById("detailVisualModel"),
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
        detailPrimaryAction: document.getElementById("detailPrimaryAction"),
        detailShareButton: document.getElementById("detailShareButton"),
        detailCopySummaryButton: document.getElementById("detailCopySummaryButton"),
        detailBookmarkButton: document.getElementById("detailBookmarkButton"),
        detailCompareButton: document.getElementById("detailCompareButton"),
        detailSectionNav: document.getElementById("detailSectionNav"),
        detailOptionSortStockButton: document.getElementById("detailOptionSortStockButton"),
        detailOptionSortNameButton: document.getElementById("detailOptionSortNameButton"),
        detailOptionLowStockOnlyButton: document.getElementById("detailOptionLowStockOnlyButton"),
        detailCopyOptionSummaryButton: document.getElementById("detailCopyOptionSummaryButton"),
        detailOptionSortStockHighButton: document.getElementById("detailOptionSortStockHighButton"),
        detailOptionSortPriceButton: document.getElementById("detailOptionSortPriceButton"),
        detailOptionStableOnlyButton: document.getElementById("detailOptionStableOnlyButton"),
        detailCopyAvailableOptionsButton: document.getElementById("detailCopyAvailableOptionsButton"),
        detailRelatedSortStockButton: document.getElementById("detailRelatedSortStockButton"),
        detailRelatedSortPriceButton: document.getElementById("detailRelatedSortPriceButton"),
        detailRelatedLowStockOnlyButton: document.getElementById("detailRelatedLowStockOnlyButton"),
        detailCopyRelatedSummaryButton: document.getElementById("detailCopyRelatedSummaryButton"),
        detailRelatedSortPriceLowButton: document.getElementById("detailRelatedSortPriceLowButton"),
        detailRelatedSameBrandButton: document.getElementById("detailRelatedSameBrandButton"),
        detailRandomRelatedButton: document.getElementById("detailRandomRelatedButton"),
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
        if (elements.detailVisualBrand) {
            elements.detailVisualBrand.textContent = brandInitials(product.brand);
        }
        if (elements.detailVisualModel) {
            elements.detailVisualModel.textContent = product.model || product.name || "Product";
        }
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
        const maximumStock = Math.max(1, ...options.map((item) => Number(item.stock || 0)));
        elements.detailOptionGrid.innerHTML = options.map((option) => `
            <article class="detail-option-card">
                <span>${option.name}</span>
                <strong>${option.stock}개</strong>
                <p>${stockPressureDetail(option.stock)}</p>
                <div class="detail-option-card__price">추가금 ${formatPrice(option.additionalPrice)}</div>
                <div class="detail-option-card__meter"><span style="width: ${Math.min(100, Math.max(4, Number(option.stock || 0) / maximumStock * 100))}%"></span></div>
                <em class="${stockClassName(option.stock)}">${stockLabel(option.stock)}</em>
            </article>
        `).join("");
    }

    function renderRelated(product) {
        if (!elements.detailRelatedGrid) {
            return;
        }
        const related = sortedRelatedProducts(product);
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
            <article class="detail-related-card">
                ${productVisualMarkup(item, "detail-related-card__visual")}
                <span class="detail-related-card__brand">${item.brand}</span>
                <strong>${item.name}</strong>
                <p>${item.reason} · ${stockPressureDetail(item.stock)}</p>
                <div class="detail-related-card__meta">
                    <span>모델 ${item.model}</span>
                    <span>${item.priceLabel || formatPrice(item.price)}</span>
                    <span class="${stockClassName(item.stock)}">${item.stockStatus || stockLabel(item.stock)}</span>
                </div>
                <div class="detail-related-card__actions">
                    <a href="${buildProductUrl(item.id)}">상세 보기</a>
                    <button class="${isComparedProduct(item.id) ? "is-active" : ""}" type="button" data-related-compare-id="${item.id}">${isComparedProduct(item.id) ? "비교 해제" : "비교 담기"}</button>
                    <button class="${isBookmarkedProduct(item.id) ? "is-active" : ""}" type="button" data-related-bookmark-id="${item.id}">${isBookmarkedProduct(item.id) ? "찜 해제" : "찜하기"}</button>
                    <button type="button" data-related-copy-id="${item.id}">요약 복사</button>
                </div>
            </article>
        `).join("");
        bindRelatedCardActions(related);
        syncRelatedButtons();
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
        const visibleOptions = options.filter((option) => {
            if (optionSortState.lowStockOnly) {
                return Number(option.stock || 0) < lowStockThreshold();
            }
            if (optionSortState.stableOnly) {
                return Number(option.stock || 0) >= lowStockThreshold();
            }
            return true;
        });
        if (optionSortState.mode === "NAME_ASC") {
            return visibleOptions.sort((left, right) => String(left.name || "").localeCompare(String(right.name || ""), "ko"));
        }
        if (optionSortState.mode === "STOCK_DESC") {
            return visibleOptions.sort((left, right) => Number(right.stock || 0) - Number(left.stock || 0));
        }
        if (optionSortState.mode === "PRICE_HIGH") {
            return visibleOptions.sort((left, right) => Number(right.additionalPrice || 0) - Number(left.additionalPrice || 0));
        }
        return visibleOptions.sort((left, right) => Number(left.stock || 0) - Number(right.stock || 0));
    }

    function syncOptionSortButtons() {
        elements.detailOptionSortStockButton?.classList.toggle("is-active", optionSortState.mode === "STOCK_ASC");
        elements.detailOptionSortNameButton?.classList.toggle("is-active", optionSortState.mode === "NAME_ASC");
        elements.detailOptionLowStockOnlyButton?.classList.toggle("is-active", optionSortState.lowStockOnly);
        elements.detailOptionSortStockHighButton?.classList.toggle("is-active", optionSortState.mode === "STOCK_DESC");
        elements.detailOptionSortPriceButton?.classList.toggle("is-active", optionSortState.mode === "PRICE_HIGH");
        elements.detailOptionStableOnlyButton?.classList.toggle("is-active", optionSortState.stableOnly);
    }

    function sortedRelatedProducts(product) {
        const related = Array.isArray(product?.relatedProducts) ? product.relatedProducts.slice() : [];
        const visibleRelated = related.filter((item) => {
            if (relatedSortState.lowStockOnly && Number(item.stock || 0) >= lowStockThreshold()) {
                return false;
            }
            return !relatedSortState.sameBrandOnly || item.brand === product.brand;
        });
        if (relatedSortState.mode === "STOCK_ASC") {
            return visibleRelated.sort((left, right) => Number(left.stock || 0) - Number(right.stock || 0));
        }
        if (relatedSortState.mode === "PRICE_HIGH") {
            return visibleRelated.sort((left, right) => Number(right.price || 0) - Number(left.price || 0));
        }
        if (relatedSortState.mode === "PRICE_LOW") {
            return visibleRelated.sort((left, right) => Number(left.price || 0) - Number(right.price || 0));
        }
        return visibleRelated;
    }

    function syncRelatedButtons() {
        elements.detailRelatedSortStockButton?.classList.toggle("is-active", relatedSortState.mode === "STOCK_ASC");
        elements.detailRelatedSortPriceButton?.classList.toggle("is-active", relatedSortState.mode === "PRICE_HIGH");
        elements.detailRelatedLowStockOnlyButton?.classList.toggle("is-active", relatedSortState.lowStockOnly);
        elements.detailRelatedSortPriceLowButton?.classList.toggle("is-active", relatedSortState.mode === "PRICE_LOW");
        elements.detailRelatedSameBrandButton?.classList.toggle("is-active", relatedSortState.sameBrandOnly);
    }

    function bindRelatedCardActions(related) {
        const findRelated = (id) => related.find((item) => Number(item.id) === Number(id));
        elements.detailRelatedGrid?.querySelectorAll("[data-related-compare-id]").forEach((button) => {
            button.addEventListener("click", () => {
                const item = findRelated(button.dataset.relatedCompareId);
                if (item) {
                    toggleCompareProduct(item);
                    renderRelated(currentProduct);
                }
            });
        });
        elements.detailRelatedGrid?.querySelectorAll("[data-related-bookmark-id]").forEach((button) => {
            button.addEventListener("click", () => {
                const item = findRelated(button.dataset.relatedBookmarkId);
                if (item) {
                    toggleBookmarkProduct(item);
                    renderRelated(currentProduct);
                }
            });
        });
        elements.detailRelatedGrid?.querySelectorAll("[data-related-copy-id]").forEach((button) => {
            button.addEventListener("click", async () => {
                const item = findRelated(button.dataset.relatedCopyId);
                if (item) {
                    await copyText(`${item.name} · ${item.brand} · ${item.priceLabel || formatPrice(item.price)} · ${stockPressureDetail(item.stock)}`, "연관 상품 요약을 복사했습니다.");
                }
            });
        });
    }

    async function copyText(text, successTitle) {
        try {
            if (!navigator.clipboard?.writeText) {
                throw new Error("Clipboard not available");
            }
            await navigator.clipboard.writeText(text);
            showToast(successTitle, "메신저나 문서에 바로 붙여 넣을 수 있습니다.");
        } catch (error) {
            window.prompt("내용을 복사하세요.", text);
        }
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

    function optionSummaryText(product) {
        const options = sortedOptions(product);
        return options.length
            ? options.map((option, index) => `${index + 1}. ${option.name} · ${option.stock}개 · ${stockLabel(option.stock)}`).join("\n")
            : "표시 가능한 옵션이 없습니다.";
    }

    function relatedSummaryText(product) {
        const related = sortedRelatedProducts(product);
        return related.length
            ? related.map((item, index) => `${index + 1}. ${item.name} · ${item.priceLabel || formatPrice(item.price)} · 재고 ${item.stock}개`).join("\n")
            : "표시 가능한 연관 상품이 없습니다.";
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

    function readCompareProducts() {
        try {
            const parsed = JSON.parse(window.localStorage.getItem(COMPARE_PRODUCTS_KEY) || "[]");
            return Array.isArray(parsed) ? parsed.filter((item) => item?.id) : [];
        } catch (error) {
            return [];
        }
    }

    function writeCompareProducts(comparedProducts) {
        window.localStorage.setItem(COMPARE_PRODUCTS_KEY, JSON.stringify(comparedProducts));
    }

    function isBookmarkedProduct(productIdValue) {
        return readBookmarkProducts().some((item) => Number(item.id) === Number(productIdValue));
    }

    function isComparedProduct(productIdValue) {
        return readCompareProducts().some((item) => Number(item.id) === Number(productIdValue));
    }

    function toggleBookmarkProduct(product) {
        const current = readBookmarkProducts();
        const exists = current.some((item) => Number(item.id) === Number(product.id));
        if (exists) {
            writeBookmarkProducts(current.filter((item) => Number(item.id) !== Number(product.id)));
            showToast("관심 상품에서 제외했습니다.", `${product.headline || product.name}을 찜 보드에서 뺐습니다.`);
        } else {
            const summary = {
                id: product.id,
                brand: product.brand,
                name: product.name,
                headline: product.headline,
                model: product.model,
                category: product.category,
                price: product.price,
                priceLabel: product.priceLabel,
                stock: product.stock,
                stockStatus: product.stockStatus,
                featured: Boolean(product.featured)
            };
            writeBookmarkProducts([summary].concat(current).slice(0, 6));
            showToast("관심 상품에 담았습니다.", `${product.headline || product.name}을 나중에 다시 볼 수 있습니다.`);
        }
        syncActionButtons();
    }

    function toggleCompareProduct(product) {
        const current = readCompareProducts();
        const exists = current.some((item) => Number(item.id) === Number(product.id));
        if (exists) {
            writeCompareProducts(current.filter((item) => Number(item.id) !== Number(product.id)));
            showToast("비교 대상에서 제외했습니다.", `${product.headline || product.name}을 비교 보드에서 뺐습니다.`);
        } else {
            const summary = {
                id: product.id,
                brand: product.brand,
                name: product.name,
                headline: product.headline,
                model: product.model,
                category: product.category,
                price: product.price,
                priceLabel: product.priceLabel,
                stock: product.stock,
                stockStatus: product.stockStatus
            };
            writeCompareProducts([summary].concat(current).slice(0, 3));
            showToast("비교 보드에 담았습니다.", `${product.headline || product.name}을 비교 목록에 추가했습니다.`);
        }
        syncActionButtons();
    }

    function syncActionButtons() {
        if (!currentProduct) {
            return;
        }
        if (elements.detailBookmarkButton) {
            const bookmarked = isBookmarkedProduct(currentProduct.id);
            elements.detailBookmarkButton.textContent = bookmarked ? "관심 상품 해제" : "관심 상품 담기";
            elements.detailBookmarkButton.classList.toggle("is-active", bookmarked);
        }
        if (elements.detailCompareButton) {
            const compared = isComparedProduct(currentProduct.id);
            elements.detailCompareButton.textContent = compared ? "비교 보드 해제" : "비교 보드 담기";
            elements.detailCompareButton.classList.toggle("is-active", compared);
        }
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
        elements.detailPrimaryAction?.addEventListener("click", () => {
            document.getElementById("detailOptions")?.scrollIntoView({ behavior: "smooth", block: "start" });
            elements.detailPrimaryAction.classList.add("is-active");
            window.setTimeout(() => elements.detailPrimaryAction?.classList.remove("is-active"), 700);
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
        elements.detailBookmarkButton?.addEventListener("click", () => {
            if (currentProduct) {
                toggleBookmarkProduct(currentProduct);
            }
        });
        elements.detailCompareButton?.addEventListener("click", () => {
            if (currentProduct) {
                toggleCompareProduct(currentProduct);
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
        elements.detailOptionLowStockOnlyButton?.addEventListener("click", () => {
            optionSortState.lowStockOnly = !optionSortState.lowStockOnly;
            optionSortState.stableOnly = false;
            if (currentProduct) {
                renderOptions(currentProduct);
                showToast(optionSortState.lowStockOnly ? "긴장 재고 옵션만 표시합니다." : "전체 옵션 표시로 복구했습니다.", "옵션 목록 밀도를 빠르게 전환할 수 있습니다.");
            }
        });
        elements.detailCopyOptionSummaryButton?.addEventListener("click", async () => {
            if (!currentProduct) {
                return;
            }
            const text = optionSummaryText(currentProduct);
            try {
                if (navigator.clipboard?.writeText) {
                    await navigator.clipboard.writeText(text);
                }
                showToast("옵션 요약을 복사했습니다.", "사이즈별 재고 정보를 바로 전달할 수 있습니다.");
            } catch (error) {
                window.prompt("옵션 요약을 복사하세요.", text);
            }
        });
        elements.detailOptionSortStockHighButton?.addEventListener("click", () => {
            optionSortState.mode = "STOCK_DESC";
            if (currentProduct) {
                renderOptions(currentProduct);
                showToast("옵션을 재고 높은 순으로 정렬했습니다.", "선택 여유가 있는 옵션부터 확인할 수 있습니다.");
            }
        });
        elements.detailOptionSortPriceButton?.addEventListener("click", () => {
            optionSortState.mode = "PRICE_HIGH";
            if (currentProduct) {
                renderOptions(currentProduct);
                showToast("옵션을 추가금 높은 순으로 정렬했습니다.", "옵션별 가격 차이를 빠르게 확인할 수 있습니다.");
            }
        });
        elements.detailOptionStableOnlyButton?.addEventListener("click", () => {
            optionSortState.stableOnly = !optionSortState.stableOnly;
            optionSortState.lowStockOnly = false;
            if (currentProduct) {
                renderOptions(currentProduct);
                showToast(optionSortState.stableOnly ? "안정 재고 옵션만 표시합니다." : "전체 옵션 표시로 복구했습니다.", "구매 가능한 옵션 밀도를 빠르게 전환할 수 있습니다.");
            }
        });
        elements.detailCopyAvailableOptionsButton?.addEventListener("click", async () => {
            if (!currentProduct) {
                return;
            }
            const available = (currentProduct.options || []).filter((option) => Number(option.stock || 0) > 0);
            const text = available.length
                ? available.map((option, index) => `${index + 1}. ${option.name} · ${option.stock}개 · 추가금 ${formatPrice(option.additionalPrice)}`).join("\n")
                : "구매 가능한 옵션이 없습니다.";
            await copyText(text, "구매 가능 옵션을 복사했습니다.");
        });
        elements.detailRelatedSortStockButton?.addEventListener("click", () => {
            relatedSortState.mode = "STOCK_ASC";
            if (currentProduct) {
                renderRelated(currentProduct);
                showToast("연관 상품을 재고 낮은 순으로 정렬했습니다.", "긴장 재고 연관 상품을 먼저 확인할 수 있습니다.");
            }
        });
        elements.detailRelatedSortPriceButton?.addEventListener("click", () => {
            relatedSortState.mode = "PRICE_HIGH";
            if (currentProduct) {
                renderRelated(currentProduct);
                showToast("연관 상품을 가격 높은 순으로 정렬했습니다.", "고가 비교 대상부터 바로 확인할 수 있습니다.");
            }
        });
        elements.detailRelatedSortPriceLowButton?.addEventListener("click", () => {
            relatedSortState.mode = "PRICE_LOW";
            if (currentProduct) {
                renderRelated(currentProduct);
                showToast("연관 상품을 가격 낮은 순으로 정렬했습니다.", "부담이 낮은 비교 후보부터 확인할 수 있습니다.");
            }
        });
        elements.detailRelatedSameBrandButton?.addEventListener("click", () => {
            relatedSortState.sameBrandOnly = !relatedSortState.sameBrandOnly;
            if (currentProduct) {
                renderRelated(currentProduct);
                showToast(relatedSortState.sameBrandOnly ? "같은 브랜드 연관 상품만 표시합니다." : "전체 브랜드로 복구했습니다.", "브랜드 내부 대안을 빠르게 비교할 수 있습니다.");
            }
        });
        elements.detailRandomRelatedButton?.addEventListener("click", () => {
            if (!currentProduct) {
                return;
            }
            const related = sortedRelatedProducts(currentProduct);
            if (!related.length) {
                showToast("이동할 연관 상품이 없습니다.", "현재 필터를 넓혀 다시 시도해주세요.", true);
                return;
            }
            const item = related[Math.floor(Math.random() * related.length)];
            window.location.href = buildProductUrl(item.id);
        });
        elements.detailRelatedLowStockOnlyButton?.addEventListener("click", () => {
            relatedSortState.lowStockOnly = !relatedSortState.lowStockOnly;
            if (currentProduct) {
                renderRelated(currentProduct);
                showToast(relatedSortState.lowStockOnly ? "긴장 재고 연관 상품만 표시합니다." : "전체 연관 상품 표시로 복구했습니다.", "비교 대상을 더 빠르게 좁힐 수 있습니다.");
            }
        });
        elements.detailCopyRelatedSummaryButton?.addEventListener("click", async () => {
            if (!currentProduct) {
                return;
            }
            const text = relatedSummaryText(currentProduct);
            try {
                if (navigator.clipboard?.writeText) {
                    await navigator.clipboard.writeText(text);
                }
                showToast("연관 상품 요약을 복사했습니다.", "비교 후보 상품 목록을 바로 전달할 수 있습니다.");
            } catch (error) {
                window.prompt("연관 상품 요약을 복사하세요.", text);
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
            syncActionButtons();
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
