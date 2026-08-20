(function () {
    const BOOKMARK_PRODUCTS_KEY = "front-bookmark-products";
    const PLACEHOLDER_IMAGE = "/images/product-placeholder.svg";
    const COLLECTIONS = {
        recommended: {
            eyebrow: "Recommended",
            title: "추천 상품",
            description: "전시 담당자가 선별한 Featured 상품을 한 화면에서 확인합니다.",
            query: { featuredOnly: true, sort: "FEATURED" }
        },
        ranking: {
            eyebrow: "Stock Ranking",
            title: "상품 랭킹",
            description: "현재 확보된 재고가 많은 상품부터 비교할 수 있는 운영형 랭킹입니다.",
            query: { sort: "STOCK_DESC" }
        },
        "fast-delivery": {
            eyebrow: "Ready Stock",
            title: "빠른배송 상품",
            description: "안정 재고가 확보된 상품입니다. 실제 배송 예정일은 주문 단계에서 별도 확인이 필요합니다.",
            query: { stock: "STABLE", sort: "STOCK_DESC" }
        },
        new: {
            eyebrow: "New Drops",
            title: "신규 드롭",
            description: "최근 등록된 상품부터 새로운 입고 흐름을 확인합니다.",
            query: { sort: "LATEST" }
        },
        luxury: {
            eyebrow: "Premium Edit",
            title: "럭셔리 셀렉션",
            description: "30만원을 초과하는 프리미엄 가격대 상품을 모았습니다.",
            query: { priceBand: "OVER_300", sort: "PRICE_HIGH" }
        }
    };
    const requestedCollectionType = document.body.dataset.collectionType;
    const collectionType = Object.prototype.hasOwnProperty.call(COLLECTIONS, requestedCollectionType)
        ? requestedCollectionType
        : "recommended";
    const collection = COLLECTIONS[collectionType];
    const state = {
        page: 0,
        size: 20,
        keyword: "",
        sort: "DEFAULT",
        filters: {
            brand: "",
            category: "",
            stock: "ALL",
            priceBand: "ALL",
            lowStockThreshold: 20,
            featuredOnly: false
        }
    };
    let pagination = {
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true
    };
    let currentProducts = [];
    let productController = null;
    let quickViewController = null;
    let quickViewRequestSequence = 0;
    let quickViewTrigger = null;
    let requestSequence = 0;
    const elements = {
        eyebrow: document.getElementById("collectionEyebrow"),
        title: document.getElementById("collectionTitle"),
        description: document.getElementById("collectionDescription"),
        searchForm: document.getElementById("collectionSearchForm"),
        totalCount: document.getElementById("collectionTotalCount"),
        pageCount: document.getElementById("collectionPageCount"),
        searchInput: document.getElementById("collectionSearchInput"),
        sortSelect: document.getElementById("collectionSortSelect"),
        searchButton: document.getElementById("collectionSearchButton"),
        filterButton: document.getElementById("collectionFilterButton"),
        filterCount: document.getElementById("collectionFilterCount"),
        resetButton: document.getElementById("collectionResetButton"),
        filterDialog: document.getElementById("collectionFilterDialog"),
        filterForm: document.getElementById("collectionFilterForm"),
        filterClose: document.getElementById("collectionFilterCloseButton"),
        brandInput: document.getElementById("collectionBrandInput"),
        categoryInput: document.getElementById("collectionCategoryInput"),
        stockSelect: document.getElementById("collectionStockSelect"),
        priceBandSelect: document.getElementById("collectionPriceBandSelect"),
        lowStockThreshold: document.getElementById("collectionLowStockThreshold"),
        featuredOnly: document.getElementById("collectionFeaturedOnly"),
        filterLocked: document.getElementById("collectionFilterLocked"),
        filterReset: document.getElementById("collectionFilterResetButton"),
        filterSummary: document.getElementById("collectionFilterSummary"),
        resultText: document.getElementById("collectionResultText"),
        rangeText: document.getElementById("collectionRangeText"),
        grid: document.getElementById("collectionGrid"),
        quickView: document.getElementById("collectionQuickView"),
        quickViewContent: document.getElementById("collectionQuickViewContent"),
        quickViewClose: document.getElementById("collectionQuickViewClose"),
        previousButton: document.getElementById("collectionPreviousButton"),
        nextButton: document.getElementById("collectionNextButton"),
        firstButton: document.getElementById("collectionFirstButton"),
        lastButton: document.getElementById("collectionLastButton"),
        pageSelect: document.getElementById("collectionPageSelect"),
        paginationText: document.getElementById("collectionPaginationText")
    };

    function init() {
        hydrateState();
        elements.eyebrow.textContent = collection.eyebrow;
        elements.title.textContent = collection.title;
        elements.description.textContent = collection.description;
        document.title = `${collection.title} | NOREN`;
        document.querySelector(`.store-shell__category a[href$="/${collectionType}"]`)?.classList.add("is-current");
        elements.searchInput.value = state.keyword;
        elements.sortSelect.value = state.sort;
        syncFilterControls();
        renderFilterSummary();
        bindEvents();
        loadProducts();
    }

    function bindEvents() {
        elements.searchForm.addEventListener("submit", (event) => {
            event.preventDefault();
            applySearch();
        });
        elements.sortSelect.addEventListener("change", () => {
            state.sort = elements.sortSelect.value;
            state.page = 0;
            loadProducts();
        });
        elements.resetButton.addEventListener("click", () => {
            state.keyword = "";
            state.sort = "DEFAULT";
            state.page = 0;
            elements.searchInput.value = "";
            elements.sortSelect.value = "DEFAULT";
            resetFilters();
            loadProducts();
        });
        elements.filterButton.addEventListener("click", openFilterDialog);
        elements.filterForm.addEventListener("submit", (event) => {
            event.preventDefault();
            applyFilters();
        });
        elements.filterReset.addEventListener("click", () => {
            resetFilters();
            syncFilterControls();
        });
        elements.filterClose.addEventListener("click", closeFilterDialog);
        elements.filterDialog.addEventListener("close", () => elements.filterButton.setAttribute("aria-expanded", "false"));
        elements.previousButton.addEventListener("click", () => movePage(state.page - 1));
        elements.nextButton.addEventListener("click", () => movePage(state.page + 1));
        elements.firstButton.addEventListener("click", () => movePage(0));
        elements.lastButton.addEventListener("click", () => movePage(pagination.totalPages - 1));
        elements.pageSelect.addEventListener("change", () => movePage(Number(elements.pageSelect.value)));
        elements.grid.addEventListener("click", handleGridClick);
        elements.quickViewClose.addEventListener("click", () => elements.quickView.close());
        elements.quickView.addEventListener("close", () => quickViewTrigger?.focus());
        elements.quickViewContent.addEventListener("click", event => {
            const retry = event.target.closest("[data-quick-view-retry]");
            if (retry) openQuickView(Number(retry.dataset.quickViewRetry));
        });
        elements.filterSummary.addEventListener("click", handleFilterSummaryClick);
        document.addEventListener("storefront:storage-change", handleStorageChange);
        document.addEventListener("storefront:state-ready", syncBookmarkButtons);
        window.addEventListener("storage", handleStorageChange);
    }

    function applySearch() {
        state.keyword = normalizeKeyword(elements.searchInput.value);
        elements.searchInput.value = state.keyword;
        state.page = 0;
        loadProducts();
    }

    function movePage(page) {
        if (page < 0 || page >= pagination.totalPages || page === state.page) {
            return;
        }
        state.page = page;
        loadProducts().then(() => {
            document.getElementById("collectionMain").scrollIntoView({ behavior: "smooth", block: "start" });
        });
    }

    async function loadProducts() {
        productController?.abort();
        productController = new AbortController();
        const activeRequest = ++requestSequence;
        elements.grid.setAttribute("aria-busy", "true");
        elements.grid.innerHTML = '<div class="collection-state"><p>상품을 불러오고 있습니다.</p></div>';
        const params = new URLSearchParams({
            ...state.filters,
            ...collection.query,
            keyword: state.keyword,
            sort: effectiveSort(),
            page: state.page,
            size: state.size
        });
        syncUrl();
        try {
            const response = await fetch(`/api/front/products?${params}`, {
                signal: productController.signal
            });
            if (!response.ok) {
                throw new Error("상품을 불러오지 못했습니다.");
            }
            const payload = normalizeProductPage(await response.json());
            if (activeRequest !== requestSequence) {
                return;
            }
            if (!payload) throw new Error("상품 목록 응답이 올바르지 않습니다.");
            if (payload.redirectPage != null) {
                state.page = payload.redirectPage;
                await loadProducts();
                return;
            }
            pagination = payload.pagination;
            state.page = pagination.page;
            renderProducts(payload.products);
        } catch (error) {
            if (error.name === "AbortError" || activeRequest !== requestSequence) {
                return;
            }
            elements.grid.innerHTML = `
                <div class="collection-state">
                    <div>
                        <strong>상품을 불러오지 못했습니다.</strong>
                        <p>잠시 후 다시 시도해주세요.</p>
                        <button class="collection-state__retry" type="button" data-collection-retry>다시 시도</button>
                    </div>
                </div>`;
            elements.resultText.textContent = error.message;
            currentProducts = [];
        } finally {
            if (activeRequest === requestSequence) {
                elements.grid.setAttribute("aria-busy", "false");
            }
        }
    }

    function effectiveSort() {
        return state.sort === "DEFAULT" ? collection.query.sort || "LATEST" : state.sort;
    }

    function openFilterDialog() {
        syncFilterControls();
        if (typeof elements.filterDialog.showModal === "function") {
            elements.filterDialog.showModal();
        } else {
            elements.filterDialog.setAttribute("open", "");
        }
        elements.filterButton.setAttribute("aria-expanded", "true");
        elements.brandInput.focus();
    }

    function closeFilterDialog() {
        if (elements.filterDialog.open && typeof elements.filterDialog.close === "function") {
            elements.filterDialog.close();
        } else {
            elements.filterDialog.removeAttribute("open");
            elements.filterButton.setAttribute("aria-expanded", "false");
        }
        elements.filterButton.focus();
    }

    function applyFilters() {
        state.filters.brand = normalizeFacet(elements.brandInput.value);
        state.filters.category = normalizeFacet(elements.categoryInput.value);
        state.filters.stock = elements.stockSelect.value;
        state.filters.priceBand = elements.priceBandSelect.value;
        state.filters.lowStockThreshold = Number(elements.lowStockThreshold.value);
        state.filters.featuredOnly = elements.featuredOnly.checked;
        state.page = 0;
        closeFilterDialog();
        renderFilterSummary();
        loadProducts();
    }

    function resetFilters() {
        state.filters = {
            brand: "",
            category: "",
            stock: "ALL",
            priceBand: "ALL",
            lowStockThreshold: 20,
            featuredOnly: false
        };
        renderFilterSummary();
    }

    function syncFilterControls() {
        const locked = lockedFilterNames();
        elements.brandInput.value = state.filters.brand;
        elements.categoryInput.value = state.filters.category;
        elements.stockSelect.value = state.filters.stock;
        elements.priceBandSelect.value = state.filters.priceBand;
        elements.lowStockThreshold.value = String(state.filters.lowStockThreshold);
        elements.featuredOnly.checked = state.filters.featuredOnly;
        elements.stockSelect.disabled = locked.includes("stock");
        elements.priceBandSelect.disabled = locked.includes("priceBand");
        elements.featuredOnly.disabled = locked.includes("featuredOnly");
        elements.filterLocked.hidden = locked.length === 0;
        elements.filterLocked.textContent = locked.length
            ? `${locked.map(filterLabel).join(", ")} 조건은 이 컬렉션에 고정되어 있습니다.`
            : "";
    }

    function lockedFilterNames() {
        return ["stock", "priceBand", "featuredOnly"].filter((name) => {
            const value = collection.query[name];
            return value != null && value !== "ALL" && value !== false;
        });
    }

    function renderFilterSummary() {
        const filters = activeFilters();
        elements.filterCount.textContent = String(filters.length);
        elements.filterSummary.hidden = filters.length === 0;
        elements.filterSummary.innerHTML = filters.map((filter) => `<button type="button" data-filter-reset="${filter.key}">${escapeHtml(filter.label)} <span aria-hidden="true">×</span></button>`).join("");
    }

    function activeFilters() {
        const locked = lockedFilterNames();
        const filters = [];
        if (state.filters.brand) filters.push({ key: "brand", label: `브랜드: ${state.filters.brand}` });
        if (state.filters.category) filters.push({ key: "category", label: `카테고리: ${state.filters.category}` });
        if (!locked.includes("stock") && state.filters.stock !== "ALL") filters.push({ key: "stock", label: filterLabel(state.filters.stock) });
        if (!locked.includes("priceBand") && state.filters.priceBand !== "ALL") filters.push({ key: "priceBand", label: filterLabel(state.filters.priceBand) });
        if (state.filters.lowStockThreshold !== 20) filters.push({ key: "lowStockThreshold", label: `재고 주의: ${state.filters.lowStockThreshold}개 미만` });
        if (!locked.includes("featuredOnly") && state.filters.featuredOnly) filters.push({ key: "featuredOnly", label: "대표 상품" });
        return filters;
    }

    function handleFilterSummaryClick(event) {
        const button = event.target.closest("[data-filter-reset]");
        if (!button) return;
        const key = button.dataset.filterReset;
        if (key === "featuredOnly") state.filters[key] = false;
        else if (key === "lowStockThreshold") state.filters[key] = 20;
        else state.filters[key] = key === "stock" || key === "priceBand" ? "ALL" : "";
        state.page = 0;
        syncFilterControls();
        renderFilterSummary();
        loadProducts();
    }

    function filterLabel(value) {
        return ({ stock: "재고", priceBand: "가격대", featuredOnly: "대표 상품", STABLE: "구매 가능", LOW: "재고 주의", UNDER_200: "20만원 미만", BETWEEN_200_300: "20~30만원", OVER_300: "30만원 초과" })[value] || value;
    }

    function renderProducts(products) {
        currentProducts = products.slice();
        const total = pagination.totalElements;
        const totalPages = pagination.totalPages;
        const start = total ? state.page * state.size + 1 : 0;
        const end = Math.min(total, start + products.length - 1);
        elements.totalCount.textContent = total.toLocaleString("ko-KR");
        elements.pageCount.textContent = totalPages.toLocaleString("ko-KR");
        elements.resultText.textContent = `${collection.title} ${total.toLocaleString("ko-KR")}개 상품`;
        elements.rangeText.textContent = `${start}-${Math.max(0, end)} / ${total.toLocaleString("ko-KR")}`;
        elements.paginationText.textContent = `${totalPages ? state.page + 1 : 0} / ${totalPages}`;
        elements.pageSelect.hidden = totalPages <= 1;
        elements.pageSelect.innerHTML = Array.from({ length: totalPages }, (_, index) => `<option value="${index}">${index + 1} 페이지</option>`).join("");
        elements.pageSelect.value = String(state.page);
        elements.previousButton.disabled = Boolean(pagination.first);
        elements.nextButton.disabled = Boolean(pagination.last);
        elements.firstButton.disabled = Boolean(pagination.first);
        elements.lastButton.disabled = Boolean(pagination.last);
        elements.grid.innerHTML = products.length
            ? products.map(productCard).join("")
            : '<div class="collection-state"><div><strong>조건에 맞는 상품이 없습니다.</strong><p>검색어를 변경하거나 초기화해주세요.</p></div></div>';
        bindImageFallbacks();
    }

    function productCard(product) {
        const bookmarked = bookmarkIds().has(product.id);
        const imageUrl = product.thumbnailUrl;
        const stockSignal = collectionType === "fast-delivery"
            ? `재고 확보 ${product.stock.toLocaleString("ko-KR")}개`
            : `${product.stockStatus || "재고 확인"} · 재고 ${product.stock.toLocaleString("ko-KR")}개`;
        const detailUrl = productDetailUrl(product.id);
        return `
            <article class="collection-product">
                <button class="collection-product__wish ${bookmarked ? "is-active" : ""}" type="button"
                        data-bookmark-id="${product.id}" aria-pressed="${bookmarked}" aria-label="${escapeHtml(product.name)} 관심 상품 ${bookmarked ? "해제" : "추가"}">
                    <span aria-hidden="true">${bookmarked ? "♥" : "♡"}</span>
                </button>
                <a class="collection-product__visual" href="${detailUrl}" aria-label="${escapeHtml(product.name)} 상세 보기">
                    <img src="${escapeAttribute(imageUrl)}" alt="${escapeAttribute(product.name)}" loading="lazy">
                </a>
                <div class="collection-product__body">
                    <span class="collection-product__brand">${escapeHtml(product.brand || "Unknown")}</span>
                    <h2>${escapeHtml(product.name || "이름 없는 상품")}</h2>
                    <strong class="collection-product__price">${escapeHtml(formatPrice(product.price))}</strong>
                    <div class="collection-product__meta">
                        <span>${escapeHtml(stockSignal)}</span>
                        <span>${escapeHtml(product.category || "미분류")}</span>
                    </div>
                    <div class="collection-product__actions"><button type="button" data-quick-view-id="${product.id}">빠른 보기</button><a class="collection-product__detail" href="${detailUrl}">상품 더보기</a></div>
                </div>
            </article>`;
    }

    function productDetailUrl(productId) {
        const returnTo = `${window.location.pathname}${window.location.search}`;
        return `/front/products/${productId}?returnTo=${encodeURIComponent(returnTo)}`;
    }

    function handleGridClick(event) {
        if (event.target.closest("[data-collection-retry]")) {
            loadProducts();
            return;
        }
        const quickViewButton = event.target.closest("[data-quick-view-id]");
        if (quickViewButton) {
            quickViewTrigger = quickViewButton;
            openQuickView(Number(quickViewButton.dataset.quickViewId));
            return;
        }
        const button = event.target.closest("[data-bookmark-id]");
        if (!button) {
            return;
        }
        const productId = Number(button.dataset.bookmarkId);
        if (!Number.isSafeInteger(productId) || productId <= 0) return;
        const bookmarks = readBookmarks();
        const existingIndex = bookmarks.findIndex((product) => Number(product.id) === productId);
        if (existingIndex >= 0) {
            bookmarks.splice(existingIndex, 1);
        } else {
            const product = currentProducts.find((item) => Number(item.id) === productId);
            if (product) {
                bookmarks.unshift(product);
            }
        }
        try {
            if (window.StorefrontState) {
                window.StorefrontState.write(BOOKMARK_PRODUCTS_KEY, bookmarks.slice(0, 24));
            } else {
                window.localStorage.setItem(BOOKMARK_PRODUCTS_KEY, JSON.stringify(bookmarks.slice(0, 24)));
            }
        } catch (ignored) {
            return;
        }
        button.classList.toggle("is-active", existingIndex < 0);
        button.setAttribute("aria-pressed", String(existingIndex < 0));
        button.querySelector("span").textContent = existingIndex < 0 ? "♥" : "♡";
        button.setAttribute("aria-label", `${button.closest(".collection-product")?.querySelector("h2")?.textContent || "상품"} 관심 상품 ${existingIndex < 0 ? "해제" : "추가"}`);
    }

    async function openQuickView(productId) {
        if (!Number.isSafeInteger(productId) || productId <= 0) return;
        quickViewController?.abort();
        quickViewController = new AbortController();
        const requestSequence = ++quickViewRequestSequence;
        elements.quickViewContent.innerHTML = "<p>상품 정보를 불러오고 있습니다.</p>";
        // 열린 상태에서는 콘텐츠와 요청만 교체해 모달 API 예외를 피한다.
        if (!elements.quickView.open) {
            elements.quickView.showModal();
        }
        elements.quickViewClose.focus();
        try {
            const response = await fetch(`/api/front/products/${productId}`, { signal: quickViewController.signal });
            if (!response.ok) throw new Error("상품 정보를 불러오지 못했습니다.");
            const product = await response.json();
            if (requestSequence !== quickViewRequestSequence) return;
            if (Number(product?.id) !== productId) throw new Error("상품 정보가 올바르지 않습니다.");
            const options = Array.isArray(product.options) ? product.options : [];
            elements.quickViewContent.innerHTML = `<p>${escapeHtml(product.brand || "NOREN")}</p><h2 id="collectionQuickViewTitle">${escapeHtml(product.name || "상품")}</h2><strong>${escapeHtml(product.priceLabel || formatPrice(product.price))}</strong><p>${escapeHtml(product.stockStatus || "재고 확인")} · 재고 ${Number(product.stock || 0).toLocaleString("ko-KR")}개</p><ul>${options.length ? options.slice(0, 6).map(option => `<li>${escapeHtml(option.name)} · ${Number(option.stock || 0)}개${Number(option.additionalPrice || 0) ? ` · +${escapeHtml(formatPrice(option.additionalPrice))}` : ""}</li>`).join("") : "<li>등록된 옵션이 없습니다.</li>"}</ul><a href="${productDetailUrl(productId)}">상품 상세 보기</a>`;
        } catch (error) {
            if (error.name === "AbortError" || requestSequence !== quickViewRequestSequence) return;
            elements.quickViewContent.innerHTML = `<p>${escapeHtml(error.message || "상품 정보를 불러오지 못했습니다.")}</p><button type="button" data-quick-view-retry="${productId}">다시 시도</button>`;
        }
    }

    function handleStorageChange(event) {
        const key = event.detail?.key || event.key;
        if (key !== BOOKMARK_PRODUCTS_KEY) {
            return;
        }
        syncBookmarkButtons();
    }

    function syncBookmarkButtons() {
        const bookmarks = bookmarkIds();
        elements.grid.querySelectorAll("[data-bookmark-id]").forEach((button) => {
            const bookmarked = bookmarks.has(Number(button.dataset.bookmarkId));
            button.classList.toggle("is-active", bookmarked);
            button.setAttribute("aria-pressed", String(bookmarked));
            button.setAttribute("aria-label", `${button.closest(".collection-product")?.querySelector("h2")?.textContent || "상품"} 관심 상품 ${bookmarked ? "해제" : "추가"}`);
            button.querySelector("span").textContent = bookmarked ? "♥" : "♡";
        });
    }

    function bindImageFallbacks() {
        elements.grid.querySelectorAll("img").forEach((image) => {
            image.addEventListener("error", () => {
                if (!image.src.endsWith(PLACEHOLDER_IMAGE)) {
                    image.src = PLACEHOLDER_IMAGE;
                }
            }, { once: true });
        });
    }

    function hydrateState() {
        const params = new URLSearchParams(window.location.search);
        const requestedPage = Number(params.get("page") || 1);
        state.page = Number.isSafeInteger(requestedPage) && requestedPage > 0 ? requestedPage - 1 : 0;
        state.keyword = normalizeKeyword(params.get("keyword"));
        const requestedSort = params.get("sort");
        if (["DEFAULT", "PRICE_LOW", "PRICE_HIGH", "STOCK_DESC", "LATEST"].includes(requestedSort)) {
            state.sort = requestedSort;
        }
        state.filters.brand = normalizeFacet(params.get("brand"));
        state.filters.category = normalizeFacet(params.get("category"));
        state.filters.stock = normalizeFilterOption(params.get("stock"), ["ALL", "LOW", "STABLE"]);
        state.filters.priceBand = normalizeFilterOption(params.get("priceBand"), ["ALL", "UNDER_200", "BETWEEN_200_300", "OVER_300"]);
        state.filters.lowStockThreshold = [10, 20, 30, 50].includes(Number(params.get("lowStockThreshold")))
            ? Number(params.get("lowStockThreshold")) : 20;
        state.filters.featuredOnly = params.get("featuredOnly") === "true";
    }

    function syncUrl() {
        const params = new URLSearchParams();
        if (state.page > 0) params.set("page", state.page + 1);
        if (state.keyword) params.set("keyword", state.keyword);
        if (state.sort !== "DEFAULT") params.set("sort", state.sort);
        if (state.filters.brand) params.set("brand", state.filters.brand);
        if (state.filters.category) params.set("category", state.filters.category);
        if (state.filters.stock !== "ALL") params.set("stock", state.filters.stock);
        if (state.filters.priceBand !== "ALL") params.set("priceBand", state.filters.priceBand);
        if (state.filters.lowStockThreshold !== 20) params.set("lowStockThreshold", String(state.filters.lowStockThreshold));
        if (state.filters.featuredOnly) params.set("featuredOnly", "true");
        const query = params.toString();
        window.history.replaceState({}, "", `${window.location.pathname}${query ? `?${query}` : ""}`);
    }

    function readBookmarks() {
        try {
            const value = JSON.parse(window.localStorage.getItem(BOOKMARK_PRODUCTS_KEY) || "[]");
            if (!Array.isArray(value)) return [];
            const seen = new Set();
            return value.flatMap((item) => {
                const normalized = normalizeProduct(item);
                if (!normalized || seen.has(normalized.id)) return [];
                seen.add(normalized.id);
                return [normalized];
            }).slice(0, 24);
        } catch (error) {
            return [];
        }
    }

    function bookmarkIds() {
        return new Set(readBookmarks().map((product) => Number(product.id)));
    }

    function formatPrice(price) {
        const normalized = normalizeNonNegativeInteger(price);
        return `${normalized == null ? 0 : normalized.toLocaleString("ko-KR")}원`;
    }

    function normalizeKeyword(value) {
        return String(value || "").trim().replace(/\s+/g, " ").slice(0, 100);
    }

    function normalizeFacet(value) {
        return String(value || "").trim().replace(/\s+/g, " ").slice(0, 80);
    }

    function normalizeFilterOption(value, allowed) {
        const normalized = String(value || "ALL").trim().toUpperCase();
        return allowed.includes(normalized) ? normalized : "ALL";
    }

    function normalizePositiveInteger(value) {
        const text = String(value ?? "").trim();
        if (!/^\d+$/.test(text)) return null;
        const parsed = Number(text);
        return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : null;
    }

    function normalizeNonNegativeInteger(value) {
        const text = String(value ?? "").trim();
        if (!/^\d+$/.test(text)) return null;
        const parsed = Number(text);
        return Number.isSafeInteger(parsed) ? parsed : null;
    }

    function normalizeImageSource(value) {
        const text = String(value || "").trim();
        if (!text) return PLACEHOLDER_IMAGE;
        if (text.startsWith("/") && !text.startsWith("//")) return text;
        try {
            const url = new URL(text, window.location.origin);
            return ["http:", "https:"].includes(url.protocol) ? url.href : PLACEHOLDER_IMAGE;
        } catch (ignored) {
            return PLACEHOLDER_IMAGE;
        }
    }

    function normalizeProduct(product) {
        const id = normalizePositiveInteger(product?.id);
        const name = String(product?.name || "").trim();
        const price = normalizeNonNegativeInteger(product?.price);
        const stock = normalizeNonNegativeInteger(product?.stock);
        if (!id || !name || name.length > 200 || price == null || stock == null) return null;
        return {
            ...product,
            id,
            name,
            brand: String(product.brand || "").trim().slice(0, 100),
            category: String(product.category || "").trim().slice(0, 100),
            stockStatus: String(product.stockStatus || "").trim().slice(0, 50),
            price,
            stock,
            thumbnailUrl: normalizeImageSource(product.thumbnailUrl)
        };
    }

    function normalizeProductPage(payload) {
        const meta = payload?.pagination;
        if (!payload || !Array.isArray(payload.products) || !meta) return null;
        const page = normalizeNonNegativeInteger(meta.page);
        const size = normalizePositiveInteger(meta.size);
        const totalElements = normalizeNonNegativeInteger(meta.totalElements);
        const totalPages = normalizeNonNegativeInteger(meta.totalPages);
        if (page == null || size !== state.size || totalElements == null || totalPages == null) return null;
        const expectedPages = totalElements === 0 ? 0 : Math.ceil(totalElements / size);
        if (totalPages !== expectedPages || typeof meta.first !== "boolean" || typeof meta.last !== "boolean") return null;
        if (totalPages > 0 && state.page >= totalPages) {
            return { redirectPage: totalPages - 1 };
        }
        if (page !== state.page || meta.first !== (page === 0) || meta.last !== (totalPages === 0 || page === totalPages - 1)) return null;
        const seen = new Set();
        const products = payload.products.map(normalizeProduct);
        if (products.some((product) => !product) || products.some((product) => seen.has(product.id) || !seen.add(product.id))) return null;
        if (products.length > size || (totalElements === 0 && products.length) || (totalElements > 0 && !products.length)) return null;
        return { products, pagination: { page, size, totalElements, totalPages, first: meta.first, last: meta.last } };
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }

    function escapeAttribute(value) {
        return escapeHtml(value);
    }

    init();
}());
