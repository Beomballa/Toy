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
    const collectionType = document.body.dataset.collectionType;
    const collection = COLLECTIONS[collectionType] || COLLECTIONS.recommended;
    const state = {
        page: 0,
        size: 20,
        keyword: "",
        sort: "DEFAULT"
    };
    let pagination = {
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true
    };
    let currentProducts = [];
    const elements = {
        eyebrow: document.getElementById("collectionEyebrow"),
        title: document.getElementById("collectionTitle"),
        description: document.getElementById("collectionDescription"),
        totalCount: document.getElementById("collectionTotalCount"),
        pageCount: document.getElementById("collectionPageCount"),
        searchInput: document.getElementById("collectionSearchInput"),
        sortSelect: document.getElementById("collectionSortSelect"),
        searchButton: document.getElementById("collectionSearchButton"),
        resetButton: document.getElementById("collectionResetButton"),
        resultText: document.getElementById("collectionResultText"),
        rangeText: document.getElementById("collectionRangeText"),
        grid: document.getElementById("collectionGrid"),
        previousButton: document.getElementById("collectionPreviousButton"),
        nextButton: document.getElementById("collectionNextButton"),
        paginationText: document.getElementById("collectionPaginationText")
    };

    function init() {
        hydrateState();
        elements.eyebrow.textContent = collection.eyebrow;
        elements.title.textContent = collection.title;
        elements.description.textContent = collection.description;
        document.title = `${collection.title} | Grade Stock`;
        document.querySelector(`.collection-header nav a[href$="/${collectionType}"]`)?.classList.add("is-active");
        elements.searchInput.value = state.keyword;
        elements.sortSelect.value = state.sort;
        bindEvents();
        loadProducts();
    }

    function bindEvents() {
        elements.searchButton.addEventListener("click", applySearch);
        elements.searchInput.addEventListener("keydown", (event) => {
            if (event.key === "Enter") {
                applySearch();
            }
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
            loadProducts();
        });
        elements.previousButton.addEventListener("click", () => movePage(state.page - 1));
        elements.nextButton.addEventListener("click", () => movePage(state.page + 1));
        elements.grid.addEventListener("click", handleGridClick);
    }

    function applySearch() {
        state.keyword = elements.searchInput.value.trim();
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
        elements.grid.setAttribute("aria-busy", "true");
        elements.grid.innerHTML = '<div class="collection-state"><p>상품을 불러오고 있습니다.</p></div>';
        const params = new URLSearchParams({
            ...collection.query,
            keyword: state.keyword,
            sort: effectiveSort(),
            page: state.page,
            size: state.size
        });
        syncUrl();
        try {
            const response = await fetch(`/api/front/products?${params}`);
            if (!response.ok) {
                throw new Error("상품을 불러오지 못했습니다.");
            }
            const payload = await response.json();
            pagination = payload.pagination || pagination;
            state.page = Number(pagination.page || 0);
            renderProducts(Array.isArray(payload.products) ? payload.products : []);
        } catch (error) {
            elements.grid.innerHTML = `
                <div class="collection-state">
                    <div><strong>상품을 불러오지 못했습니다.</strong><p>잠시 후 다시 시도해주세요.</p></div>
                </div>`;
            elements.resultText.textContent = error.message;
        } finally {
            elements.grid.setAttribute("aria-busy", "false");
        }
    }

    function effectiveSort() {
        return state.sort === "DEFAULT" ? collection.query.sort || "LATEST" : state.sort;
    }

    function renderProducts(products) {
        currentProducts = products.slice();
        const total = Number(pagination.totalElements || 0);
        const totalPages = Number(pagination.totalPages || 0);
        const start = total ? state.page * state.size + 1 : 0;
        const end = Math.min(total, start + products.length - 1);
        elements.totalCount.textContent = total.toLocaleString("ko-KR");
        elements.pageCount.textContent = totalPages.toLocaleString("ko-KR");
        elements.resultText.textContent = `${collection.title} ${total.toLocaleString("ko-KR")}개 상품`;
        elements.rangeText.textContent = `${start}-${Math.max(0, end)} / ${total.toLocaleString("ko-KR")}`;
        elements.paginationText.textContent = `${totalPages ? state.page + 1 : 0} / ${totalPages}`;
        elements.previousButton.disabled = Boolean(pagination.first);
        elements.nextButton.disabled = Boolean(pagination.last);
        elements.grid.innerHTML = products.length
            ? products.map(productCard).join("")
            : '<div class="collection-state"><div><strong>조건에 맞는 상품이 없습니다.</strong><p>검색어를 변경하거나 초기화해주세요.</p></div></div>';
        bindImageFallbacks();
    }

    function productCard(product) {
        const bookmarked = bookmarkIds().has(Number(product.id));
        const imageUrl = product.thumbnailUrl || PLACEHOLDER_IMAGE;
        const stockSignal = collectionType === "fast-delivery"
            ? `재고 확보 ${Number(product.stock || 0).toLocaleString("ko-KR")}개`
            : `${product.stockStatus || "재고 확인"} · 재고 ${Number(product.stock || 0).toLocaleString("ko-KR")}개`;
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
                    <strong class="collection-product__price">${escapeHtml(product.priceLabel || formatPrice(product.price))}</strong>
                    <div class="collection-product__meta">
                        <span>${escapeHtml(stockSignal)}</span>
                        <span>${escapeHtml(product.category || "미분류")}</span>
                    </div>
                    <a class="collection-product__detail" href="${detailUrl}">상품 더보기</a>
                </div>
            </article>`;
    }

    function productDetailUrl(productId) {
        const returnTo = `${window.location.pathname}${window.location.search}`;
        return `/front/products/${productId}?returnTo=${encodeURIComponent(returnTo)}`;
    }

    function handleGridClick(event) {
        const button = event.target.closest("[data-bookmark-id]");
        if (!button) {
            return;
        }
        const productId = Number(button.dataset.bookmarkId);
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
        window.localStorage.setItem(BOOKMARK_PRODUCTS_KEY, JSON.stringify(bookmarks.slice(0, 24)));
        button.classList.toggle("is-active", existingIndex < 0);
        button.setAttribute("aria-pressed", String(existingIndex < 0));
        button.querySelector("span").textContent = existingIndex < 0 ? "♥" : "♡";
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
        state.page = Math.max(0, Number(params.get("page") || 1) - 1);
        state.keyword = (params.get("keyword") || "").trim();
        const requestedSort = params.get("sort");
        if (["DEFAULT", "PRICE_LOW", "PRICE_HIGH", "STOCK_DESC", "LATEST"].includes(requestedSort)) {
            state.sort = requestedSort;
        }
    }

    function syncUrl() {
        const params = new URLSearchParams();
        if (state.page > 0) params.set("page", state.page + 1);
        if (state.keyword) params.set("keyword", state.keyword);
        if (state.sort !== "DEFAULT") params.set("sort", state.sort);
        const query = params.toString();
        window.history.replaceState({}, "", `${window.location.pathname}${query ? `?${query}` : ""}`);
    }

    function readBookmarks() {
        try {
            const value = JSON.parse(window.localStorage.getItem(BOOKMARK_PRODUCTS_KEY) || "[]");
            return Array.isArray(value) ? value : [];
        } catch (error) {
            return [];
        }
    }

    function bookmarkIds() {
        return new Set(readBookmarks().map((product) => Number(product.id)));
    }

    function formatPrice(price) {
        return Number(price || 0).toLocaleString("ko-KR") + "원";
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
