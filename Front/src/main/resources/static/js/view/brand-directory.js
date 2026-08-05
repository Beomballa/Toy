(function () {
    const SAVED_BRANDS_KEY = "front-saved-brands";
    const BOOKMARK_PRODUCTS_KEY = "front-bookmark-products";
    const COMPARE_PRODUCTS_KEY = "front-compare-products";
    const PLACEHOLDER_IMAGE = "/images/product-placeholder.svg";
    const VALID_LETTERS = ["ALL", "POPULAR", "KOREAN", "LATIN", "NUMBER", "SAVED"];
    const VALID_DIRECTORY_SORTS = ["COUNT_DESC", "NAME_ASC", "COUNT_ASC"];
    const VALID_PRODUCT_SORTS = ["LATEST", "FEATURED", "PRICE_LOW", "PRICE_HIGH", "STOCK_DESC", "NAME_ASC"];
    const VALID_STOCKS = ["ALL", "STABLE", "LOW"];
    const VALID_PRICES = ["ALL", "UNDER_200", "BETWEEN_200_300", "OVER_300"];
    const VALID_SIZES = [8, 12, 24, 48];
    const state = {
        directoryKeyword: "",
        letter: "ALL",
        directorySort: "COUNT_DESC",
        brand: "",
        category: "ALL",
        stock: "ALL",
        priceBand: "ALL",
        sort: "LATEST",
        size: 12,
        page: 0
    };
    let directoryBrands = [];
    let directoryMetrics = null;
    let selectedBrandMetrics = null;
    let selectedBrandCategories = [];
    let currentProducts = [];
    let pagination = emptyPagination();
    let selectedProductIds = new Set();
    let directoryController = null;
    let directoryRequestSequence = 0;
    let productController = null;
    let productRequestSequence = 0;
    let toastTimer = null;

    const elements = {
        liveStatus: document.getElementById("brandLiveStatus"),
        directoryTotal: document.getElementById("brandDirectoryTotal"),
        directoryProducts: document.getElementById("brandDirectoryProducts"),
        directoryStock: document.getElementById("brandDirectoryStock"),
        searchForm: document.getElementById("brandSearchForm"),
        searchInput: document.getElementById("brandSearchInput"),
        searchClear: document.getElementById("brandSearchClearButton"),
        letters: Array.from(document.querySelectorAll("[data-brand-letter]")),
        directorySort: document.getElementById("brandDirectorySort"),
        indexResult: document.getElementById("brandIndexResult"),
        cardGrid: document.getElementById("brandCardGrid"),
        profile: document.getElementById("brandProfile"),
        emptyProfile: document.getElementById("brandEmptyProfile"),
        profileMonogram: document.getElementById("brandProfileMonogram"),
        profileTitle: document.getElementById("brandProfileTitle"),
        profileDescription: document.getElementById("brandProfileDescription"),
        saveButton: document.getElementById("brandSaveButton"),
        copyLink: document.getElementById("brandCopyLinkButton"),
        copySummary: document.getElementById("brandCopySummaryButton"),
        metricProducts: document.getElementById("brandMetricProducts"),
        metricAverage: document.getElementById("brandMetricAverage"),
        metricRange: document.getElementById("brandMetricRange"),
        metricStock: document.getElementById("brandMetricStock"),
        metricLowStock: document.getElementById("brandMetricLowStock"),
        metricFeatured: document.getElementById("brandMetricFeatured"),
        categoryBars: document.getElementById("brandCategoryBars"),
        products: document.getElementById("brandProducts"),
        productsTitle: document.getElementById("brandProductsTitle"),
        productResult: document.getElementById("brandProductResult"),
        categoryFilter: document.getElementById("brandCategoryFilter"),
        stockFilter: document.getElementById("brandStockFilter"),
        priceFilter: document.getElementById("brandPriceFilter"),
        productSort: document.getElementById("brandProductSort"),
        productSize: document.getElementById("brandProductSize"),
        productReset: document.getElementById("brandProductResetButton"),
        appliedFilters: document.getElementById("brandAppliedFilters"),
        productGrid: document.getElementById("brandProductGrid"),
        selectAll: document.getElementById("brandSelectAllButton"),
        compareSelected: document.getElementById("brandCompareSelectedButton"),
        clearSelection: document.getElementById("brandClearSelectionButton"),
        pagination: document.getElementById("brandPagination"),
        previous: document.getElementById("brandPreviousButton"),
        next: document.getElementById("brandNextButton"),
        pageSelect: document.getElementById("brandPageSelect"),
        pageText: document.getElementById("brandPageText"),
        rangeText: document.getElementById("brandRangeText"),
        selectionBar: document.getElementById("brandSelectionBar"),
        selectionText: document.getElementById("brandSelectionText"),
        selectionCompare: document.getElementById("brandSelectionCompareButton"),
        selectionCancel: document.getElementById("brandSelectionCancelButton"),
        toast: document.getElementById("brandToast")
    };

    function init() {
        hydrateFromUrl();
        syncControls();
        bindEvents();
        void loadDirectory();
    }

    async function loadDirectory() {
        const sequence = ++directoryRequestSequence;
        directoryController?.abort();
        directoryController = new AbortController();
        showDirectoryState("LOADING");
        try {
            const response = await fetch("/api/front/catalog/bootstrap?page=0&size=1", {
                signal: directoryController.signal
            });
            if (!response.ok) throw new Error("브랜드 데이터를 불러오지 못했습니다.");
            const payload = normalizeDirectoryResponse(await response.json());
            if (sequence !== directoryRequestSequence) return;
            directoryBrands = normalizeFacets(payload.brandFacets);
            directoryMetrics = payload.metrics;
            renderDirectorySummary();
            renderBrandCards();
            if (state.brand && directoryBrands.some((item) => item.value === state.brand)) {
                await selectBrand(state.brand, { keepFilters: true, updateUrl: false, focus: false });
            } else if (state.brand) {
                state.brand = "";
                updateUrl("replace");
            }
        } catch (error) {
            if (error?.name === "AbortError" || sequence !== directoryRequestSequence) return;
            showDirectoryState("ERROR");
        } finally {
            if (sequence === directoryRequestSequence) directoryController = null;
        }
    }

    function renderDirectorySummary() {
        elements.directoryTotal.textContent = Number(directoryMetrics?.brandCount || directoryBrands.length).toLocaleString("ko-KR");
        elements.directoryProducts.textContent = Number(directoryMetrics?.totalCount || 0).toLocaleString("ko-KR");
        elements.directoryStock.textContent = Number(directoryMetrics?.totalStock || 0).toLocaleString("ko-KR");
    }

    function renderBrandCards() {
        const savedBrands = readStringList(SAVED_BRANDS_KEY);
        const normalizedKeyword = normalize(state.directoryKeyword);
        const popularNames = new Set(directoryBrands.slice(0, 5).map((item) => item.value));
        const filtered = directoryBrands
            .filter((item) => !normalizedKeyword || normalize(item.value).includes(normalizedKeyword))
            .filter((item) => matchesLetter(item.value, state.letter, popularNames, savedBrands))
            .sort(directoryComparator(state.directorySort));
        elements.cardGrid.replaceChildren();
        elements.cardGrid.classList.remove("is-loading", "is-error");
        elements.cardGrid.setAttribute("aria-busy", "false");
        if (!filtered.length) {
            elements.cardGrid.appendChild(createDirectoryEmpty());
        } else {
            filtered.forEach((item) => elements.cardGrid.appendChild(createBrandCard(item, savedBrands)));
        }
        elements.indexResult.textContent = `${filtered.length.toLocaleString("ko-KR")}개 브랜드를 표시합니다.`;
        syncDirectoryControls();
        announce(`${filtered.length}개의 브랜드를 표시했습니다.`);
    }

    function createBrandCard(item, savedBrands) {
        const article = document.createElement("article");
        article.className = "brand-card";
        article.classList.toggle("is-selected", item.value === state.brand);
        const select = document.createElement("button");
        select.type = "button";
        select.className = "brand-card__select";
        select.setAttribute("aria-pressed", String(item.value === state.brand));
        select.setAttribute("aria-label", `${item.value} 브랜드 상품 보기`);
        const monogram = document.createElement("span");
        monogram.textContent = initials(item.value);
        const copy = document.createElement("span");
        const name = document.createElement("strong");
        name.textContent = item.value;
        const count = document.createElement("em");
        count.textContent = `${Number(item.count).toLocaleString("ko-KR")}개 상품`;
        copy.append(name, count);
        const arrow = document.createElement("i");
        arrow.setAttribute("aria-hidden", "true");
        arrow.textContent = "→";
        select.append(monogram, copy, arrow);
        select.addEventListener("click", () => selectBrand(item.value));
        const save = document.createElement("button");
        save.type = "button";
        save.className = "brand-card__save";
        const saved = savedBrands.includes(item.value);
        save.setAttribute("aria-pressed", String(saved));
        save.setAttribute("aria-label", `${item.value} 관심 브랜드 ${saved ? "해제" : "저장"}`);
        save.textContent = saved ? "★" : "☆";
        save.addEventListener("click", () => toggleSavedBrand(item.value));
        article.append(select, save);
        return article;
    }

    function createDirectoryEmpty() {
        const stateElement = document.createElement("div");
        stateElement.className = "brand-directory-state";
        const title = document.createElement("strong");
        title.textContent = "조건에 맞는 브랜드가 없습니다.";
        const text = document.createElement("p");
        text.textContent = "검색어 또는 이름 분류를 변경해 주세요.";
        const reset = document.createElement("button");
        reset.type = "button";
        reset.textContent = "전체 브랜드 보기";
        reset.addEventListener("click", resetDirectoryFilters);
        stateElement.append(title, text, reset);
        return stateElement;
    }

    function showDirectoryState(name) {
        const error = name === "ERROR";
        const stateElement = document.createElement("div");
        stateElement.className = "brand-directory-state";
        const text = document.createElement("p");
        text.textContent = error ? "브랜드 데이터를 불러오지 못했습니다." : "브랜드를 불러오는 중입니다.";
        stateElement.appendChild(text);
        if (error) {
            const retry = document.createElement("button");
            retry.type = "button";
            retry.textContent = "다시 불러오기";
            retry.addEventListener("click", loadDirectory);
            stateElement.appendChild(retry);
        }
        elements.cardGrid.replaceChildren(stateElement);
        elements.cardGrid.classList.toggle("is-loading", !error);
        elements.cardGrid.classList.toggle("is-error", error);
        elements.cardGrid.setAttribute("aria-busy", String(!error));
        elements.indexResult.textContent = error ? "브랜드 조회에 실패했습니다." : "브랜드 데이터를 불러오는 중입니다.";
    }

    async function selectBrand(brand, options = {}) {
        if (!directoryBrands.some((item) => item.value === brand)) return;
        state.brand = brand;
        state.page = options.keepFilters ? state.page : 0;
        if (!options.keepFilters) resetProductFilters(false);
        selectedProductIds.clear();
        renderBrandCards();
        showBrandWorkspace();
        showProductState("LOADING");
        if (options.updateUrl !== false) updateUrl();
        const sequence = ++productRequestSequence;
        productController?.abort();
        productController = new AbortController();
        try {
            const response = await fetch(`/api/front/catalog/bootstrap?${productParams()}`, {
                signal: productController.signal
            });
            if (!response.ok) throw new Error("브랜드 상품을 불러오지 못했습니다.");
            const payload = normalizeProductResponse(await response.json(), true);
            if (sequence !== productRequestSequence) return;
            selectedBrandMetrics = payload.metrics;
            selectedBrandCategories = normalizeFacets(payload.categoryFacets);
            renderBrandProfile();
            renderCategoryOptions();
            renderProducts(payload);
            if (options.focus !== false) elements.profileTitle.focus();
        } catch (error) {
            if (error?.name === "AbortError" || sequence !== productRequestSequence) return;
            showProductState("ERROR", true);
        }
    }

    async function loadFilteredProducts(options = {}) {
        if (!state.brand) return;
        const sequence = ++productRequestSequence;
        productController?.abort();
        productController = new AbortController();
        selectedProductIds.clear();
        showProductState("LOADING");
        if (options.updateUrl !== false) updateUrl(options.historyMode);
        try {
            const response = await fetch(`/api/front/products?${productParams()}`, {
                signal: productController.signal
            });
            if (!response.ok) throw new Error("브랜드 상품을 불러오지 못했습니다.");
            const payload = normalizeProductResponse(await response.json(), false);
            if (sequence !== productRequestSequence) return;
            renderProducts(payload);
        } catch (error) {
            if (error?.name === "AbortError" || sequence !== productRequestSequence) return;
            showProductState("ERROR", false);
        }
    }

    function productParams() {
        const params = new URLSearchParams({
            brand: state.brand,
            stock: state.stock,
            sort: state.sort,
            priceBand: state.priceBand,
            page: String(state.page),
            size: String(state.size)
        });
        if (state.category !== "ALL") params.set("category", state.category);
        return params;
    }

    function renderBrandProfile() {
        const metrics = selectedBrandMetrics || {};
        elements.profileMonogram.textContent = initials(state.brand);
        elements.profileTitle.textContent = state.brand;
        elements.profileDescription.textContent = `${Number(metrics.totalCount || 0).toLocaleString("ko-KR")}개 상품과 ${selectedBrandCategories.length}개 카테고리를 현재 재고 기준으로 분석합니다.`;
        elements.metricProducts.textContent = Number(metrics.totalCount || 0).toLocaleString("ko-KR");
        elements.metricAverage.textContent = formatPrice(metrics.averagePrice);
        elements.metricRange.textContent = `${formatPrice(metrics.minimumPrice)} - ${formatPrice(metrics.maximumPrice)}`;
        elements.metricStock.textContent = Number(metrics.totalStock || 0).toLocaleString("ko-KR");
        elements.metricLowStock.textContent = Number(metrics.lowStockCount || 0).toLocaleString("ko-KR");
        elements.metricFeatured.textContent = Number(metrics.featuredCount || 0).toLocaleString("ko-KR");
        syncSavedBrandButton();
        renderCategoryBars();
        document.title = `${state.brand} 브랜드 상품 | NOREN`;
    }

    function renderCategoryBars() {
        elements.categoryBars.replaceChildren();
        const maximum = Math.max(1, ...selectedBrandCategories.map((item) => item.count));
        selectedBrandCategories.slice(0, 8).forEach((item) => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "brand-category-bar";
            button.setAttribute("aria-label", `${item.value} ${item.count}개 상품 필터 적용`);
            const label = document.createElement("span");
            label.textContent = item.value;
            const track = document.createElement("i");
            const fill = document.createElement("b");
            fill.style.width = `${Math.max(8, Math.round(item.count / maximum * 100))}%`;
            track.appendChild(fill);
            const count = document.createElement("em");
            count.textContent = String(item.count);
            button.append(label, track, count);
            button.addEventListener("click", () => {
                state.category = item.value;
                state.page = 0;
                syncProductControls();
                void loadFilteredProducts();
            });
            elements.categoryBars.appendChild(button);
        });
    }

    function renderCategoryOptions() {
        elements.categoryFilter.replaceChildren(new Option("전체 카테고리", "ALL"));
        selectedBrandCategories.forEach((item) => {
            elements.categoryFilter.appendChild(new Option(`${item.value} (${item.count})`, item.value));
        });
        syncProductControls();
    }

    function renderProducts(payload) {
        currentProducts = payload.products.slice();
        pagination = payload.pagination;
        state.page = pagination.page;
        if (state.page !== Number(new URLSearchParams(window.location.search).get("page") || 1) - 1) {
            updateUrl("replace");
        }
        elements.productGrid.replaceChildren();
        elements.productGrid.classList.remove("is-loading", "is-error");
        elements.productGrid.setAttribute("aria-busy", "false");
        if (!currentProducts.length) {
            elements.productGrid.appendChild(createProductEmpty());
        } else {
            currentProducts.forEach((product) => elements.productGrid.appendChild(createProductCard(product)));
        }
        const total = pagination.totalElements;
        const start = total ? state.page * state.size + 1 : 0;
        const end = total ? Math.min(total, start + currentProducts.length - 1) : 0;
        elements.productResult.textContent = `${state.brand} 상품 ${total.toLocaleString("ko-KR")}개가 현재 조건에 맞습니다.`;
        elements.productsTitle.textContent = `${state.brand} 상품`;
        elements.pageText.textContent = `${pagination.totalPages ? state.page + 1 : 0} / ${pagination.totalPages} 페이지`;
        elements.rangeText.textContent = `${start}-${end} / 총 ${total.toLocaleString("ko-KR")}개`;
        elements.previous.disabled = pagination.first;
        elements.next.disabled = pagination.last;
        elements.pagination.hidden = pagination.totalPages <= 1;
        renderPageOptions();
        renderAppliedFilters();
        syncSelection();
        announce(`${currentProducts.length}개의 ${state.brand} 상품을 표시했습니다.`);
    }

    function createProductCard(product) {
        const article = document.createElement("article");
        article.className = "brand-product-card";
        article.dataset.productId = String(product.id);
        const controls = document.createElement("div");
        controls.className = "brand-product-card__controls";
        const select = document.createElement("button");
        select.type = "button";
        select.className = "brand-product-card__select";
        select.setAttribute("aria-pressed", String(selectedProductIds.has(Number(product.id))));
        select.setAttribute("aria-label", `${product.name} 비교 선택`);
        select.textContent = selectedProductIds.has(Number(product.id)) ? "✓" : "+";
        select.addEventListener("click", () => toggleProductSelection(product.id));
        const bookmark = document.createElement("button");
        bookmark.type = "button";
        bookmark.className = "brand-product-card__bookmark";
        const bookmarked = isBookmarked(product.id);
        bookmark.setAttribute("aria-pressed", String(bookmarked));
        bookmark.setAttribute("aria-label", `${product.name} 관심 상품 ${bookmarked ? "해제" : "저장"}`);
        bookmark.textContent = bookmarked ? "♥" : "♡";
        bookmark.addEventListener("click", () => toggleProductBookmark(product, bookmark));
        controls.append(select, bookmark);
        const link = document.createElement("a");
        link.className = "brand-product-card__visual";
        link.href = productDetailUrl(product.id);
        link.setAttribute("aria-label", `${product.name} 상세 보기`);
        const image = document.createElement("img");
        image.src = product.thumbnailUrl;
        image.alt = product.name || "상품 이미지";
        image.loading = "lazy";
        image.addEventListener("error", () => {
            if (!image.src.endsWith(PLACEHOLDER_IMAGE)) image.src = PLACEHOLDER_IMAGE;
        }, { once: true });
        link.appendChild(image);
        const body = document.createElement("div");
        body.className = "brand-product-card__body";
        const meta = document.createElement("p");
        meta.textContent = `${product.category || "미분류"} · ${product.stockStatus || "재고 확인"}`;
        const title = document.createElement("h3");
        title.textContent = product.name || "이름 없는 상품";
        const model = document.createElement("span");
        model.textContent = product.model || "모델 정보 없음";
        const price = document.createElement("strong");
        price.textContent = formatPrice(product.price);
        const stock = document.createElement("em");
        stock.textContent = `재고 ${Number(product.stock || 0).toLocaleString("ko-KR")}개`;
        body.append(meta, title, model, price, stock);
        article.append(controls, link, body);
        return article;
    }

    function createProductEmpty() {
        const stateElement = document.createElement("div");
        stateElement.className = "brand-directory-state";
        const title = document.createElement("strong");
        title.textContent = "조건에 맞는 상품이 없습니다.";
        const text = document.createElement("p");
        text.textContent = "재고, 가격대 또는 카테고리 조건을 초기화해 주세요.";
        const reset = document.createElement("button");
        reset.type = "button";
        reset.textContent = "상품 필터 초기화";
        reset.addEventListener("click", () => resetProductFilters(true));
        stateElement.append(title, text, reset);
        return stateElement;
    }

    function showProductState(name, bootstrapRetry) {
        const error = name === "ERROR";
        const stateElement = document.createElement("div");
        stateElement.className = "brand-directory-state";
        const text = document.createElement("p");
        text.textContent = error ? "브랜드 상품을 불러오지 못했습니다." : "브랜드 상품을 불러오는 중입니다.";
        stateElement.appendChild(text);
        if (error) {
            const retry = document.createElement("button");
            retry.type = "button";
            retry.textContent = "다시 불러오기";
            retry.addEventListener("click", () => bootstrapRetry
                ? selectBrand(state.brand, { keepFilters: true, updateUrl: false })
                : loadFilteredProducts({ updateUrl: false }));
            stateElement.appendChild(retry);
        }
        elements.productGrid.replaceChildren(stateElement);
        elements.productGrid.classList.toggle("is-loading", !error);
        elements.productGrid.classList.toggle("is-error", error);
        elements.productGrid.setAttribute("aria-busy", String(!error));
        elements.pagination.hidden = true;
    }

    function showBrandWorkspace() {
        elements.profile.hidden = false;
        elements.products.hidden = false;
        elements.emptyProfile.hidden = true;
    }

    function renderAppliedFilters() {
        elements.appliedFilters.replaceChildren();
        const filters = [
            [state.category !== "ALL", `카테고리 · ${state.category}`, () => changeProductFilter("category", "ALL")],
            [state.stock !== "ALL", `재고 · ${stockLabel(state.stock)}`, () => changeProductFilter("stock", "ALL")],
            [state.priceBand !== "ALL", `가격 · ${priceLabel(state.priceBand)}`, () => changeProductFilter("priceBand", "ALL")]
        ];
        filters.filter(([active]) => active).forEach(([, label, remove]) => {
            const button = document.createElement("button");
            button.type = "button";
            button.textContent = `${label} ×`;
            button.addEventListener("click", remove);
            elements.appliedFilters.appendChild(button);
        });
        if (!elements.appliedFilters.childElementCount) {
            const text = document.createElement("span");
            text.textContent = "전체 상품";
            elements.appliedFilters.appendChild(text);
        }
    }

    function changeProductFilter(key, value) {
        state[key] = value;
        state.page = 0;
        syncProductControls();
        void loadFilteredProducts();
    }

    function renderPageOptions() {
        elements.pageSelect.replaceChildren();
        const pages = compactPageIndexes(pagination.totalPages, state.page);
        pages.forEach((page, index) => {
            if (index > 0 && page - pages[index - 1] > 1) {
                const separator = new Option("…", "");
                separator.disabled = true;
                elements.pageSelect.appendChild(separator);
            }
            const option = new Option(`${page + 1} 페이지`, String(page), false, page === state.page);
            elements.pageSelect.appendChild(option);
        });
        elements.pageSelect.setAttribute("aria-label", `상품 페이지 선택, 전체 ${pagination.totalPages}페이지`);
    }

    function compactPageIndexes(totalPages, currentPage) {
        if (totalPages <= 0) return [];
        const indexes = new Set([0, totalPages - 1]);
        for (let page = currentPage - 2; page <= currentPage + 2; page += 1) {
            if (page >= 0 && page < totalPages) indexes.add(page);
        }
        return Array.from(indexes).sort((left, right) => left - right);
    }

    function toggleSavedBrand(brand) {
        const saved = readStringList(SAVED_BRANDS_KEY);
        const next = saved.includes(brand) ? saved.filter((item) => item !== brand) : [brand, ...saved];
        writeList(SAVED_BRANDS_KEY, next.slice(0, 30));
        renderBrandCards();
        syncSavedBrandButton();
        showToast(next.includes(brand) ? `${brand}를 관심 브랜드로 저장했습니다.` : `${brand} 관심을 해제했습니다.`);
    }

    function syncSavedBrandButton() {
        if (!state.brand) return;
        const saved = readStringList(SAVED_BRANDS_KEY).includes(state.brand);
        elements.saveButton.setAttribute("aria-pressed", String(saved));
        elements.saveButton.textContent = saved ? "관심 브랜드 해제" : "관심 브랜드 저장";
    }

    function toggleProductSelection(productId) {
        const id = Number(productId);
        if (selectedProductIds.has(id)) {
            selectedProductIds.delete(id);
        } else if (selectedProductIds.size >= 3) {
            showToast("비교 상품은 최대 3개까지 선택할 수 있습니다.");
            return;
        } else {
            selectedProductIds.add(id);
        }
        syncSelection();
    }

    function syncSelection() {
        elements.productGrid.querySelectorAll("[data-product-id]").forEach((card) => {
            const id = Number(card.dataset.productId);
            const selected = selectedProductIds.has(id);
            card.classList.toggle("is-selected", selected);
            const button = card.querySelector(".brand-product-card__select");
            button?.setAttribute("aria-pressed", String(selected));
            if (button) button.textContent = selected ? "✓" : "+";
        });
        const count = selectedProductIds.size;
        elements.selectionBar.hidden = count === 0;
        elements.selectionText.textContent = `${count}개 상품 선택`;
        elements.compareSelected.disabled = count === 0;
        elements.clearSelection.disabled = count === 0;
    }

    function selectCurrentPage() {
        currentProducts.slice(0, 3).forEach((product) => selectedProductIds.add(Number(product.id)));
        if (currentProducts.length > 3) showToast("비교 가능한 최대 3개 상품을 선택했습니다.");
        syncSelection();
    }

    function addSelectedToCompare() {
        if (!selectedProductIds.size) return;
        const selectedProducts = currentProducts.filter((product) => selectedProductIds.has(Number(product.id)));
        const stored = readObjectList(COMPARE_PRODUCTS_KEY);
        const merged = [...selectedProducts, ...stored.filter((item) => !selectedProductIds.has(Number(item.id)))].slice(0, 3);
        writeList(COMPARE_PRODUCTS_KEY, merged);
        showToast(`${selectedProducts.length}개 상품을 비교 보드에 담았습니다.`);
        selectedProductIds.clear();
        syncSelection();
    }

    function toggleProductBookmark(product, button) {
        const bookmarks = readObjectList(BOOKMARK_PRODUCTS_KEY);
        const exists = bookmarks.some((item) => Number(item.id) === Number(product.id));
        const next = exists
            ? bookmarks.filter((item) => Number(item.id) !== Number(product.id))
            : [product, ...bookmarks].slice(0, 24);
        writeList(BOOKMARK_PRODUCTS_KEY, next);
        button.setAttribute("aria-pressed", String(!exists));
        button.textContent = exists ? "♡" : "♥";
        showToast(exists ? "관심 상품에서 해제했습니다." : "관심 상품에 저장했습니다.");
    }

    function isBookmarked(productId) {
        return readObjectList(BOOKMARK_PRODUCTS_KEY).some((item) => Number(item.id) === Number(productId));
    }

    function copyBrandLink() {
        copyText(window.location.href, `${state.brand} 브랜드 링크를 복사했습니다.`);
    }

    function copyBrandSummary() {
        const metrics = selectedBrandMetrics || {};
        const text = [
            `${state.brand} 브랜드 요약`,
            `상품 ${Number(metrics.totalCount || 0).toLocaleString("ko-KR")}개`,
            `평균가 ${formatPrice(metrics.averagePrice)}`,
            `가격 범위 ${formatPrice(metrics.minimumPrice)} - ${formatPrice(metrics.maximumPrice)}`,
            `총재고 ${Number(metrics.totalStock || 0).toLocaleString("ko-KR")}개`,
            `재고주의 ${Number(metrics.lowStockCount || 0).toLocaleString("ko-KR")}개`,
            `카테고리 ${selectedBrandCategories.map((item) => `${item.value} ${item.count}`).join(", ")}`
        ].join("\n");
        copyText(text, `${state.brand} 지표를 복사했습니다.`);
    }

    async function copyText(text, successMessage) {
        try {
            if (navigator.clipboard?.writeText) {
                await navigator.clipboard.writeText(text);
            } else {
                const textarea = document.createElement("textarea");
                textarea.value = text;
                textarea.style.position = "fixed";
                textarea.style.opacity = "0";
                document.body.appendChild(textarea);
                textarea.select();
                document.execCommand("copy");
                textarea.remove();
            }
            showToast(successMessage);
        } catch (error) {
            showToast("복사하지 못했습니다. 다시 시도해 주세요.");
        }
    }

    function syncDirectoryControls() {
        elements.searchInput.value = state.directoryKeyword;
        elements.searchClear.hidden = !elements.searchInput.value;
        elements.directorySort.value = state.directorySort;
        elements.letters.forEach((button) => {
            const active = button.dataset.brandLetter === state.letter;
            button.setAttribute("aria-pressed", String(active));
        });
    }

    function syncProductControls() {
        elements.categoryFilter.value = state.category;
        elements.stockFilter.value = state.stock;
        elements.priceFilter.value = state.priceBand;
        elements.productSort.value = state.sort;
        elements.productSize.value = String(state.size);
    }

    function syncControls() {
        syncDirectoryControls();
        syncProductControls();
    }

    function bindEvents() {
        elements.searchForm.addEventListener("submit", (event) => {
            event.preventDefault();
            state.directoryKeyword = elements.searchInput.value.trim().slice(0, 80);
            renderBrandCards();
        });
        elements.searchInput.addEventListener("input", () => {
            elements.searchClear.hidden = !elements.searchInput.value;
        });
        elements.searchClear.addEventListener("click", () => {
            state.directoryKeyword = "";
            elements.searchInput.value = "";
            renderBrandCards();
            elements.searchInput.focus();
        });
        elements.letters.forEach((button) => {
            button.addEventListener("click", () => {
                state.letter = VALID_LETTERS.includes(button.dataset.brandLetter) ? button.dataset.brandLetter : "ALL";
                renderBrandCards();
            });
            button.addEventListener("keydown", handleLetterKeydown);
        });
        elements.directorySort.addEventListener("change", () => {
            state.directorySort = VALID_DIRECTORY_SORTS.includes(elements.directorySort.value)
                ? elements.directorySort.value
                : "COUNT_DESC";
            renderBrandCards();
        });
        elements.saveButton.addEventListener("click", () => toggleSavedBrand(state.brand));
        elements.copyLink.addEventListener("click", copyBrandLink);
        elements.copySummary.addEventListener("click", copyBrandSummary);
        elements.categoryFilter.addEventListener("change", () => changeProductFilter("category", elements.categoryFilter.value));
        elements.stockFilter.addEventListener("change", () => changeProductFilter("stock", elements.stockFilter.value));
        elements.priceFilter.addEventListener("change", () => changeProductFilter("priceBand", elements.priceFilter.value));
        elements.productSort.addEventListener("change", () => changeProductFilter("sort", elements.productSort.value));
        elements.productSize.addEventListener("change", () => {
            state.size = VALID_SIZES.includes(Number(elements.productSize.value)) ? Number(elements.productSize.value) : 12;
            state.page = 0;
            void loadFilteredProducts();
        });
        elements.productReset.addEventListener("click", () => resetProductFilters(true));
        elements.previous.addEventListener("click", () => movePage(state.page - 1));
        elements.next.addEventListener("click", () => movePage(state.page + 1));
        elements.pageSelect.addEventListener("change", () => movePage(Number(elements.pageSelect.value)));
        elements.selectAll.addEventListener("click", selectCurrentPage);
        elements.compareSelected.addEventListener("click", addSelectedToCompare);
        elements.clearSelection.addEventListener("click", clearSelection);
        elements.selectionCompare.addEventListener("click", addSelectedToCompare);
        elements.selectionCancel.addEventListener("click", clearSelection);
        window.addEventListener("popstate", () => {
            const previousBrand = state.brand;
            hydrateFromUrl();
            syncControls();
            renderBrandCards();
            if (state.brand && state.brand !== previousBrand) {
                void selectBrand(state.brand, { keepFilters: true, updateUrl: false, focus: false });
            } else if (state.brand) {
                void loadFilteredProducts({ updateUrl: false });
            } else {
                clearBrandWorkspace();
            }
        });
        window.addEventListener("storage", (event) => {
            if (event.key === SAVED_BRANDS_KEY) {
                renderBrandCards();
                syncSavedBrandButton();
            }
        });
        document.addEventListener("storefront:state-ready", () => {
            if (currentProducts.length) renderProducts({ products: currentProducts, pagination });
        });
        document.addEventListener("keydown", (event) => {
            if (event.key === "/" && !isTypingTarget(event.target)) {
                event.preventDefault();
                elements.searchInput.focus();
            }
            if (event.key === "Escape" && selectedProductIds.size) clearSelection();
        });
    }

    function handleLetterKeydown(event) {
        if (!["ArrowLeft", "ArrowRight"].includes(event.key)) return;
        event.preventDefault();
        const index = elements.letters.indexOf(event.currentTarget);
        const offset = event.key === "ArrowRight" ? 1 : -1;
        const next = elements.letters[(index + offset + elements.letters.length) % elements.letters.length];
        next.focus();
    }

    function movePage(page) {
        if (page < 0 || page >= pagination.totalPages || page === state.page) return;
        state.page = page;
        void loadFilteredProducts().then(() => {
            elements.productsTitle.scrollIntoView({ behavior: "smooth", block: "start" });
        });
    }

    function resetDirectoryFilters() {
        state.directoryKeyword = "";
        state.letter = "ALL";
        state.directorySort = "COUNT_DESC";
        renderBrandCards();
    }

    function resetProductFilters(load) {
        state.category = "ALL";
        state.stock = "ALL";
        state.priceBand = "ALL";
        state.sort = "LATEST";
        state.size = 12;
        state.page = 0;
        syncProductControls();
        if (load) void loadFilteredProducts();
    }

    function clearSelection() {
        selectedProductIds.clear();
        syncSelection();
    }

    function clearBrandWorkspace() {
        selectedBrandMetrics = null;
        selectedBrandCategories = [];
        currentProducts = [];
        pagination = emptyPagination();
        selectedProductIds.clear();
        elements.profile.hidden = true;
        elements.products.hidden = true;
        elements.emptyProfile.hidden = false;
        elements.selectionBar.hidden = true;
        document.title = "브랜드 탐색 | NOREN";
        announce("전체 브랜드 디렉터리로 돌아왔습니다.");
    }

    function hydrateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        state.brand = optionalText(params.get("brand"), 80);
        state.category = optionalText(params.get("category") || "ALL", 80) || "ALL";
        const stock = String(params.get("stock") || "ALL").toUpperCase();
        const price = String(params.get("priceBand") || "ALL").toUpperCase();
        const sort = String(params.get("sort") || "LATEST").toUpperCase();
        const size = Number(params.get("size") || 12);
        const page = Number(params.get("page") || 1) - 1;
        state.stock = VALID_STOCKS.includes(stock) ? stock : "ALL";
        state.priceBand = VALID_PRICES.includes(price) ? price : "ALL";
        state.sort = VALID_PRODUCT_SORTS.includes(sort) ? sort : "LATEST";
        state.size = VALID_SIZES.includes(size) ? size : 12;
        state.page = Number.isInteger(page) && page >= 0 ? page : 0;
    }

    function updateUrl(mode = "push") {
        const params = new URLSearchParams();
        if (state.brand) params.set("brand", state.brand);
        if (state.category !== "ALL") params.set("category", state.category);
        if (state.stock !== "ALL") params.set("stock", state.stock);
        if (state.priceBand !== "ALL") params.set("priceBand", state.priceBand);
        if (state.sort !== "LATEST") params.set("sort", state.sort);
        if (state.size !== 12) params.set("size", String(state.size));
        if (state.page > 0) params.set("page", String(state.page + 1));
        const query = params.toString();
        const nextUrl = `${window.location.pathname}${query ? `?${query}` : ""}`;
        if (`${window.location.pathname}${window.location.search}` === nextUrl) return;
        window.history[mode === "replace" ? "replaceState" : "pushState"](null, "", nextUrl);
    }

    function matchesLetter(name, letter, popularNames, savedBrands) {
        const first = String(name || "").trim().charAt(0);
        if (letter === "POPULAR") return popularNames.has(name);
        if (letter === "KOREAN") return /[가-힣]/.test(first);
        if (letter === "LATIN") return /[A-Za-z]/.test(first);
        if (letter === "NUMBER") return /[0-9]/.test(first);
        if (letter === "SAVED") return savedBrands.includes(name);
        return true;
    }

    function directoryComparator(sort) {
        if (sort === "NAME_ASC") return (left, right) => left.value.localeCompare(right.value, "ko-KR");
        if (sort === "COUNT_ASC") return (left, right) => left.count - right.count || left.value.localeCompare(right.value, "ko-KR");
        return (left, right) => right.count - left.count || left.value.localeCompare(right.value, "ko-KR");
    }

    function normalizeFacets(facets) {
        if (!Array.isArray(facets) || facets.length > 500) throw new Error("브랜드 분류 정보가 올바르지 않습니다.");
        const values = new Set();
        return facets.map((item) => {
            const value = requiredText(item?.value, 100, "분류명");
            const count = safeInteger(item?.count, "분류 상품 수");
            if (values.has(value)) throw new Error("브랜드 분류 정보가 중복되었습니다.");
            values.add(value);
            return { value, count };
        });
    }

    function normalizePagination(source, productCount) {
        const page = safeInteger(source?.page, "현재 페이지", 0, 100000);
        const size = safeInteger(source?.size, "페이지 크기", 1, 48);
        const totalElements = safeInteger(source?.totalElements, "전체 상품 수", 0, 1000000);
        const totalPages = safeInteger(source?.totalPages, "전체 페이지 수", 0, 125000);
        const expectedPages = totalElements === 0 ? 0 : Math.ceil(totalElements / size);
        const expectedProductCount = page < totalPages ? Math.min(size, totalElements - page * size) : 0;
        if (!VALID_SIZES.includes(size) || page !== state.page || totalPages !== expectedPages || productCount !== expectedProductCount
            || (totalPages === 0 ? page !== 0 : page >= totalPages)
            || typeof source?.first !== "boolean" || typeof source?.last !== "boolean"
            || source.first !== (page === 0) || source.last !== (totalPages === 0 || page === totalPages - 1)) {
            throw new Error("상품 페이지 정보가 올바르지 않습니다.");
        }
        return { page, size, totalElements, totalPages, first: page === 0, last: totalPages === 0 || page === totalPages - 1 };
    }

    function requiredText(value, maxLength, fieldName) {
        if (typeof value !== "string") throw new Error(`${fieldName} 정보가 올바르지 않습니다.`);
        const normalized = value.replace(/[\u0000-\u001f\u007f]/g, " ").replace(/\s+/g, " ").trim();
        if (!normalized || normalized.length > maxLength) throw new Error(`${fieldName} 정보가 올바르지 않습니다.`);
        return normalized;
    }

    function optionalText(value, maxLength) {
        if (value == null) return "";
        if (typeof value !== "string") throw new Error("상품 문구가 올바르지 않습니다.");
        const normalized = value.replace(/[\u0000-\u001f\u007f]/g, " ").replace(/\s+/g, " ").trim();
        return normalized.length <= maxLength ? normalized : "";
    }

    function safeInteger(value, fieldName, minimum = 0, maximum = Number.MAX_SAFE_INTEGER) {
        if (!Number.isSafeInteger(value) || value < minimum || value > maximum) throw new Error(`${fieldName} 정보가 올바르지 않습니다.`);
        return value;
    }

    function normalizeImageSource(value) {
        const normalized = optionalText(value, 500);
        return /^\/(?!\/)/.test(normalized) || /^https?:\/\//i.test(normalized) ? normalized : PLACEHOLDER_IMAGE;
    }

    function normalizeMetrics(source) {
        const metrics = {};
        ["totalCount", "lowStockCount", "latestDropCount", "featuredCount", "totalStock", "averagePrice",
            "minimumPrice", "maximumPrice", "brandCount", "under200Count", "between200And300Count", "over300Count"]
            .forEach((key) => { metrics[key] = safeInteger(source?.[key], key); });
        if (metrics.lowStockCount > metrics.totalCount || metrics.featuredCount > metrics.totalCount
            || metrics.under200Count + metrics.between200And300Count + metrics.over300Count !== metrics.totalCount
            || (metrics.totalCount > 0 && (metrics.minimumPrice > metrics.averagePrice || metrics.averagePrice > metrics.maximumPrice))) {
            throw new Error("브랜드 집계 정보가 올바르지 않습니다.");
        }
        return metrics;
    }

    function normalizeProduct(item) {
        const id = safeInteger(item?.id, "상품 번호", 1);
        const price = safeInteger(item?.price, "상품 가격");
        const stock = safeInteger(item?.stock, "상품 재고");
        return {
            id,
            brand: requiredText(item?.brand, 100, "브랜드"),
            category: requiredText(item?.category, 100, "카테고리"),
            name: requiredText(item?.name, 200, "상품명"),
            model: optionalText(item?.model, 100),
            price,
            stock,
            stockStatus: optionalText(item?.stockStatus, 40) || (stock <= 10 ? "품절 임박" : "재고 안정"),
            thumbnailUrl: normalizeImageSource(item?.thumbnailUrl)
        };
    }

    function normalizeProducts(items) {
        if (!Array.isArray(items)) throw new Error("브랜드 상품 정보가 올바르지 않습니다.");
        const ids = new Set();
        return items.map((item) => {
            const product = normalizeProduct(item);
            if (ids.has(product.id)) throw new Error("중복된 상품 정보가 포함되었습니다.");
            ids.add(product.id);
            if (product.brand !== state.brand) throw new Error("선택한 브랜드와 상품 정보가 일치하지 않습니다.");
            if (state.category !== "ALL" && product.category !== state.category) throw new Error("선택한 카테고리와 상품 정보가 일치하지 않습니다.");
            if (state.stock === "LOW" && product.stock >= 20 || state.stock === "STABLE" && product.stock < 20) throw new Error("재고 필터와 상품 정보가 일치하지 않습니다.");
            if (state.priceBand === "UNDER_200" && product.price >= 200000
                || state.priceBand === "BETWEEN_200_300" && (product.price < 200000 || product.price > 300000)
                || state.priceBand === "OVER_300" && product.price <= 300000) throw new Error("가격 필터와 상품 정보가 일치하지 않습니다.");
            return product;
        });
    }

    function normalizeDirectoryResponse(source) {
        if (!source || typeof source !== "object" || Array.isArray(source)) throw new Error("브랜드 응답이 올바르지 않습니다.");
        const brandFacets = normalizeFacets(source.brandFacets);
        const metrics = normalizeMetrics(source.metrics);
        if (metrics.brandCount !== brandFacets.length
            || brandFacets.reduce((sum, item) => sum + item.count, 0) !== metrics.totalCount) throw new Error("브랜드 집계가 분류 정보와 일치하지 않습니다.");
        return { brandFacets, metrics };
    }

    function normalizeProductResponse(source, includeSummary) {
        if (!source || typeof source !== "object" || Array.isArray(source)) throw new Error("상품 응답이 올바르지 않습니다.");
        const products = normalizeProducts(source.products);
        const pagination = normalizePagination(source.pagination, products.length);
        if (pagination.size !== state.size) throw new Error("요청한 페이지 크기와 응답이 일치하지 않습니다.");
        const result = { products, pagination };
        if (includeSummary) {
            result.metrics = normalizeMetrics(source.metrics);
            result.categoryFacets = normalizeFacets(source.categoryFacets);
            if (result.metrics.totalCount !== pagination.totalElements || result.metrics.brandCount !== (result.metrics.totalCount ? 1 : 0)
                || result.categoryFacets.reduce((sum, item) => sum + item.count, 0) !== result.metrics.totalCount) {
                throw new Error("브랜드 상품 집계가 페이지 정보와 일치하지 않습니다.");
            }
        }
        return result;
    }

    function emptyPagination() {
        return { page: 0, size: 12, totalElements: 0, totalPages: 0, first: true, last: true };
    }

    function productDetailUrl(productId) {
        const returnTo = `${window.location.pathname}${window.location.search}`;
        return `/front/products/${Number(productId)}?returnTo=${encodeURIComponent(returnTo)}`;
    }

    function readStringList(key) {
        try {
            const value = JSON.parse(window.localStorage.getItem(key) || "[]");
            return Array.isArray(value) ? Array.from(new Set(value
                .filter((item) => typeof item === "string")
                .map((item) => item.trim())
                .filter((item) => item && item.length <= 100))).slice(0, 30) : [];
        } catch (error) {
            return [];
        }
    }

    function readObjectList(key) {
        try {
            const value = JSON.parse(window.localStorage.getItem(key) || "[]");
            if (!Array.isArray(value)) return [];
            const ids = new Set();
            return value.flatMap((item) => {
                try {
                    const product = normalizeProduct(item);
                    if (ids.has(product.id)) return [];
                    ids.add(product.id);
                    return [product];
                } catch (error) {
                    return [];
                }
            }).slice(0, key === COMPARE_PRODUCTS_KEY ? 3 : 24);
        } catch (error) {
            return [];
        }
    }

    function writeList(key, value) {
        if (window.StorefrontState && [BOOKMARK_PRODUCTS_KEY, COMPARE_PRODUCTS_KEY].includes(key)) {
            window.StorefrontState.write(key, value);
            return;
        }
        try {
            window.localStorage.setItem(key, JSON.stringify(value));
        } catch (error) {
            showToast("브라우저 저장소에 기록하지 못했습니다.");
        }
    }

    function initials(value) {
        return String(value || "GS").trim().split(/\s+/).map((word) => word.charAt(0)).join("").slice(0, 2).toUpperCase();
    }

    function formatPrice(value) {
        return `${Number(value || 0).toLocaleString("ko-KR")}원`;
    }

    function stockLabel(value) {
        return value === "STABLE" ? "안정" : value === "LOW" ? "주의" : "전체";
    }

    function priceLabel(value) {
        return { UNDER_200: "20만원 미만", BETWEEN_200_300: "20~30만원", OVER_300: "30만원 초과" }[value] || "전체";
    }

    function normalize(value) {
        return String(value || "").trim().toLocaleLowerCase("ko-KR").replace(/\s+/g, " ");
    }

    function isTypingTarget(target) {
        return target instanceof HTMLElement && (target.matches("input, textarea, select") || target.isContentEditable);
    }

    function announce(message) {
        elements.liveStatus.textContent = "";
        window.requestAnimationFrame(() => {
            elements.liveStatus.textContent = message;
        });
    }

    function showToast(message) {
        window.clearTimeout(toastTimer);
        elements.toast.textContent = message;
        elements.toast.hidden = false;
        toastTimer = window.setTimeout(() => {
            elements.toast.hidden = true;
        }, 2200);
    }

    init();
}());
