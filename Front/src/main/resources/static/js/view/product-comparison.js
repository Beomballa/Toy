(function () {
    const KEYS = {
        compare: "front-compare-products",
        recent: "front-recent-viewed-products",
        wishlist: "front-bookmark-products"
    };
    const PLACEHOLDER = "/images/product-placeholder.svg";
    const state = { products: [], mode: "BALANCE", sort: "SAVED", differenceOnly: false, candidateKeyword: "", failedCount: 0 };
    let requestSequence = 0;
    let loadController = null;
    let toastTimer = null;
    const elements = {
        live: document.getElementById("comparisonLiveStatus"),
        count: document.getElementById("comparisonCount"),
        priceGap: document.getElementById("comparisonPriceGap"),
        stockGap: document.getElementById("comparisonStockGap"),
        optionCount: document.getElementById("comparisonOptionCount"),
        modes: Array.from(document.querySelectorAll("[data-compare-mode]")),
        differenceOnly: document.getElementById("comparisonDifferenceOnly"),
        sort: document.getElementById("comparisonSort"),
        candidateButton: document.getElementById("comparisonCandidateButton"),
        candidates: document.getElementById("comparisonCandidates"),
        candidateClose: document.getElementById("comparisonCandidateCloseButton"),
        candidateSearch: document.getElementById("comparisonCandidateSearch"),
        candidateList: document.getElementById("comparisonCandidateList"),
        refresh: document.getElementById("comparisonRefreshButton"),
        empty: document.getElementById("comparisonEmpty"),
        emptyDescription: document.getElementById("comparisonEmptyDescription"),
        emptyRetry: document.getElementById("comparisonEmptyRetryButton"),
        emptyCandidate: document.getElementById("comparisonEmptyCandidateButton"),
        workspace: document.getElementById("comparisonWorkspace"),
        workspaceTitle: document.getElementById("comparisonWorkspaceTitle"),
        result: document.getElementById("comparisonResultText"),
        recommendation: document.getElementById("comparisonRecommendation"),
        table: document.getElementById("comparisonTable"),
        optionTable: document.getElementById("comparisonOptionTable"),
        copy: document.getElementById("comparisonCopyButton"),
        csv: document.getElementById("comparisonCsvButton"),
        link: document.getElementById("comparisonLinkButton"),
        print: document.getElementById("comparisonPrintButton"),
        clear: document.getElementById("comparisonClearButton"),
        toast: document.getElementById("comparisonToast")
    };

    function init() {
        bindEvents();
        void loadProducts();
    }

    async function loadProducts() {
        loadController?.abort();
        loadController = new AbortController();
        const sequence = ++requestSequence;
        const stored = requestedProducts();
        if (!stored.length) {
            state.products = [];
            state.failedCount = 0;
            render();
            return;
        }
        showLoading();
        const results = await Promise.allSettled(stored.slice(0, 3).map(async (item) => {
            const response = await fetch(`/api/front/products/${Number(item.id)}`, {
                signal: loadController.signal
            });
            if (!response.ok) throw new Error("상품 조회 실패");
            return normalizeProductResponse(await response.json(), Number(item.id));
        }));
        if (sequence !== requestSequence) return;
        state.failedCount = results.filter((result) => result.status === "rejected").length;
        state.products = results
            .map((result, index) => result.status === "fulfilled"
                ? result.value
                : hasStoredSnapshot(stored[index]) ? stored[index] : null)
            .filter(Boolean);
        if (!state.failedCount) {
            writeProducts(KEYS.compare, state.products);
            syncUrl("replace");
        }
        render();
        announce(state.failedCount ? `${state.failedCount}개 상품의 최신 정보를 불러오지 못했습니다.` : "비교 상품의 최신 정보를 반영했습니다.");
        loadController = null;
    }

    function showLoading() {
        elements.empty.hidden = true;
        elements.workspace.hidden = false;
        elements.table.setAttribute("aria-busy", "true");
        elements.table.innerHTML = '<div class="comparison-state">최신 상품 정보를 불러오는 중입니다.</div>';
        elements.optionTable.innerHTML = "";
        elements.refresh.disabled = true;
        elements.refresh.setAttribute("aria-busy", "true");
    }

    function render() {
        const products = sortedProducts();
        const enough = products.length >= 2;
        elements.empty.hidden = enough;
        elements.workspace.hidden = !enough;
        syncMetrics(products);
        renderCandidates();
        if (!enough) {
            elements.emptyDescription.textContent = state.failedCount
                ? `${state.failedCount}개 상품 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.`
                : "최근 본 상품이나 관심 상품에서 최대 3개까지 선택할 수 있습니다.";
            elements.emptyRetry.hidden = state.failedCount === 0;
            elements.result.textContent = state.failedCount
                ? "일부 상품 조회에 실패했습니다."
                : products.length ? "상품을 하나 더 추가해 주세요." : "비교 상품이 없습니다.";
            elements.refresh.disabled = false;
            elements.refresh.removeAttribute("aria-busy");
            return;
        }
        elements.emptyRetry.hidden = true;
        renderRecommendation(products);
        renderTable(products);
        renderOptions(products);
        elements.result.textContent = `${products.length}개 상품의 최신 가격, 재고, 옵션을 비교합니다.`;
        elements.table.setAttribute("aria-busy", "false");
        elements.refresh.disabled = false;
        elements.refresh.removeAttribute("aria-busy");
        document.title = `${products.length}개 상품 비교 | NOREN`;
    }

    function syncMetrics(products) {
        const prices = products.map((item) => Number(item.price || 0));
        const stocks = products.map((item) => Number(item.stock || 0));
        const options = new Set(products.flatMap((item) => safeOptions(item).map((option) => option.name)));
        elements.count.textContent = String(products.length);
        elements.priceGap.textContent = formatPrice(range(prices));
        elements.stockGap.textContent = `${range(stocks).toLocaleString("ko-KR")}개`;
        elements.optionCount.textContent = String(options.size);
    }

    function renderRecommendation(products) {
        const ranked = products.map((product) => ({ product, score: productScore(product, products) }))
            .sort((left, right) => right.score - left.score);
        const best = ranked[0];
        elements.recommendation.querySelector("strong").textContent = `${best.product.brand} · ${best.product.name}`;
        elements.recommendation.querySelector("p").textContent = `${modeLabel(state.mode)} 기준 ${Math.round(best.score)}점 · ${best.product.priceLabel || formatPrice(best.product.price)} · 재고 ${Number(best.product.stock || 0).toLocaleString("ko-KR")}개`;
    }

    function renderTable(products) {
        const rows = [
            { key: "price", label: "가격", values: products.map((item) => item.priceLabel || formatPrice(item.price)) },
            { key: "stock", label: "총재고", values: products.map((item) => `${Number(item.stock || 0).toLocaleString("ko-KR")}개`) },
            { key: "stockStatus", label: "재고 상태", values: products.map((item) => item.stockStatus || "-") },
            { key: "brand", label: "브랜드", values: products.map((item) => item.brand || "-") },
            { key: "category", label: "카테고리", values: products.map((item) => item.category || "-") },
            { key: "model", label: "모델번호", values: products.map((item) => item.model || "-") },
            { key: "featured", label: "대표 노출", values: products.map((item) => item.featured ? `Featured ${item.featuredRank || ""}` : "-") },
            { key: "createdDate", label: "등록일", values: products.map((item) => item.createdDate || "-") },
            { key: "mood", label: "무드", values: products.map((item) => item.mood || "-") },
            { key: "description", label: "설명", values: products.map((item) => item.description || "-") }
        ];
        const visibleRows = state.differenceOnly ? rows.filter((row) => new Set(row.values).size > 1) : rows;
        const columns = `160px repeat(${products.length}, minmax(220px, 1fr))`;
        elements.table.style.gridTemplateColumns = columns;
        elements.table.replaceChildren(createCorner(), ...products.map((product, index) => createProductHead(product, index)));
        visibleRows.forEach((row) => {
            elements.table.appendChild(cell(row.label, "comparison-table__label", "rowheader"));
            row.values.forEach((value, index) => {
                const node = cell(value, `comparison-table__value comparison-table__value--${row.key}`, "cell");
                if (isBestValue(row.key, products, index)) node.classList.add("is-best");
                elements.table.appendChild(node);
            });
        });
    }

    function createCorner() {
        const node = cell("비교 항목", "comparison-table__corner", "columnheader");
        return node;
    }

    function createProductHead(product, index) {
        const article = document.createElement("article");
        article.className = "comparison-product-head";
        article.setAttribute("role", "columnheader");
        const image = document.createElement("img");
        image.src = product.thumbnailUrl;
        image.alt = product.name || "상품 이미지";
        image.addEventListener("error", () => { image.src = PLACEHOLDER; }, { once: true });
        const brand = document.createElement("span");
        brand.textContent = product.brand || "-";
        const title = document.createElement("strong");
        title.textContent = product.name || "이름 없는 상품";
        const score = document.createElement("em");
        score.textContent = `${Math.round(productScore(product, state.products))}점`;
        const actions = document.createElement("div");
        const detail = document.createElement("a");
        detail.href = `/front/products/${Number(product.id)}?returnTo=${encodeURIComponent(location.pathname + location.search)}`;
        detail.textContent = "상세";
        const left = actionButton("←", `${product.name} 왼쪽 이동`, () => moveProduct(index, -1));
        const right = actionButton("→", `${product.name} 오른쪽 이동`, () => moveProduct(index, 1));
        const remove = actionButton("×", `${product.name} 비교 제거`, () => removeProduct(product.id));
        actions.append(detail, left, right, remove);
        article.append(image, brand, title, score, actions);
        return article;
    }

    function renderOptions(products) {
        const optionNames = Array.from(new Set(products.flatMap((item) => safeOptions(item).map((option) => option.name)))).sort(naturalCompare);
        elements.optionTable.style.gridTemplateColumns = `160px repeat(${products.length}, minmax(180px, 1fr))`;
        elements.optionTable.replaceChildren(cell("옵션", "comparison-table__corner", "columnheader"));
        products.forEach((product) => elements.optionTable.appendChild(cell(product.name, "comparison-option-table__head", "columnheader")));
        if (!optionNames.length) {
            elements.optionTable.appendChild(cell("등록된 옵션이 없습니다.", "comparison-option-table__empty", "cell"));
            return;
        }
        optionNames.forEach((name) => {
            elements.optionTable.appendChild(cell(name, "comparison-table__label", "rowheader"));
            products.forEach((product) => {
                const option = safeOptions(product).find((item) => item.name === name);
                const value = option ? `${Number(option.stock || 0).toLocaleString("ko-KR")}개${option.additionalPrice ? ` · +${formatPrice(option.additionalPrice)}` : ""}` : "-";
                const node = cell(value, "comparison-option-table__value", "cell");
                if (option && Number(option.stock || 0) > 0) node.classList.add("is-available");
                elements.optionTable.appendChild(node);
            });
        });
    }

    function renderCandidates() {
        const existingIds = new Set(state.products.map((item) => Number(item.id)));
        const keyword = normalize(state.candidateKeyword);
        const candidates = uniqueProducts([...readProducts(KEYS.recent), ...readProducts(KEYS.wishlist)])
            .filter((item) => !existingIds.has(Number(item.id)))
            .filter((item) => !keyword || normalize(`${item.brand} ${item.name} ${item.model}`).includes(keyword))
            .slice(0, 12);
        elements.candidateList.replaceChildren();
        if (!candidates.length) {
            elements.candidateList.appendChild(cell("추가할 최근·관심 상품이 없습니다.", "comparison-candidate-empty"));
            return;
        }
        candidates.forEach((product) => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "comparison-candidate";
            button.innerHTML = `<span>${escapeHtml(product.brand || "-")}</span><strong>${escapeHtml(product.name || "-")}</strong><em>${escapeHtml(product.priceLabel || formatPrice(product.price))}</em><i>추가 +</i>`;
            button.addEventListener("click", () => addCandidate(product));
            elements.candidateList.appendChild(button);
        });
    }

    function addCandidate(product) {
        const next = [...state.products.filter((item) => Number(item.id) !== Number(product.id)), product];
        state.products = next.slice(-3);
        writeProducts(KEYS.compare, state.products);
        syncUrl();
        closeCandidates();
        void loadProducts();
        showToast(next.length > 3 ? "가장 오래된 후보를 교체했습니다." : "비교 후보를 추가했습니다.");
    }

    function removeProduct(productId) {
        state.products = state.products.filter((item) => Number(item.id) !== Number(productId));
        writeProducts(KEYS.compare, state.products);
        syncUrl();
        render();
        showToast("비교 상품에서 제거했습니다.");
    }

    function moveProduct(index, offset) {
        const target = index + offset;
        if (target < 0 || target >= state.products.length) return;
        const next = state.products.slice();
        [next[index], next[target]] = [next[target], next[index]];
        state.products = next;
        writeProducts(KEYS.compare, next);
        render();
    }

    function requestedProducts() {
        const ids = Array.from(new Set(String(new URLSearchParams(location.search).get("ids") || "")
            .split(",").map(Number).filter((id) => Number.isSafeInteger(id) && id > 0))).slice(0, 3);
        const stored = readProducts(KEYS.compare);
        if (!ids.length) return stored.slice(0, 3);
        return ids.map((id) => stored.find((item) => Number(item.id) === id) || { id });
    }

    function sortedProducts() {
        const products = state.products.slice();
        if (state.sort === "PRICE_LOW") products.sort((a, b) => Number(a.price) - Number(b.price));
        if (state.sort === "STOCK_HIGH") products.sort((a, b) => Number(b.stock) - Number(a.stock));
        if (state.sort === "NAME") products.sort((a, b) => String(a.name).localeCompare(String(b.name), "ko-KR"));
        return products;
    }

    function productScore(product, products) {
        const prices = products.map((item) => Number(item.price || 0));
        const stocks = products.map((item) => Number(item.stock || 0));
        const priceScore = normalizedInverse(Number(product.price || 0), prices);
        const stockScore = normalized(Number(product.stock || 0), stocks);
        if (state.mode === "PRICE") return priceScore * .8 + stockScore * .2;
        if (state.mode === "STOCK") return priceScore * .2 + stockScore * .8;
        return priceScore * .55 + stockScore * .45;
    }

    function isBestValue(key, products, index) {
        if (key === "price") return Number(products[index].price) === Math.min(...products.map((item) => Number(item.price)));
        if (key === "stock") return Number(products[index].stock) === Math.max(...products.map((item) => Number(item.stock)));
        return false;
    }

    function copySummary() {
        const products = sortedProducts();
        const lines = products.map((item, index) => `${index + 1}. ${item.brand} ${item.name} · ${item.priceLabel || formatPrice(item.price)} · 재고 ${item.stock}개 · ${Math.round(productScore(item, products))}점`);
        copyText(`NOREN 상품 비교\n${modeLabel(state.mode)}\n${lines.join("\n")}`, "비교 요약을 복사했습니다.");
    }

    function exportCsv() {
        const rows = [["브랜드", "상품명", "모델", "카테고리", "가격", "재고", "옵션 수"], ...sortedProducts().map((item) => [item.brand, item.name, item.model, item.category, item.price, item.stock, safeOptions(item).length])];
        const csv = "\uFEFF" + rows.map((row) => row.map(csvCell).join(",")).join("\n");
        const link = document.createElement("a");
        link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
        link.download = `grade-stock-comparison-${new Date().toISOString().slice(0, 10)}.csv`;
        link.click();
        URL.revokeObjectURL(link.href);
    }

    function syncUrl(mode = "replace") {
        const ids = state.products.map((item) => Number(item.id)).filter(Boolean).join(",");
        const url = ids ? `${location.pathname}?ids=${ids}` : location.pathname;
        window.history[mode === "push" ? "pushState" : "replaceState"](null, "", url);
    }

    function openCandidates() {
        elements.candidates.hidden = false;
        elements.candidateButton.setAttribute("aria-expanded", "true");
        elements.candidateSearch.focus();
        renderCandidates();
    }

    function closeCandidates() {
        elements.candidates.hidden = true;
        elements.candidateButton.setAttribute("aria-expanded", "false");
        elements.candidateButton.focus();
    }

    function clearAll() {
        if (!window.confirm("비교 상품을 모두 비울까요?")) return;
        state.products = [];
        writeProducts(KEYS.compare, []);
        syncUrl();
        render();
    }

    function bindEvents() {
        elements.modes.forEach((button) => button.addEventListener("click", () => {
            state.mode = button.dataset.compareMode;
            elements.modes.forEach((item) => item.setAttribute("aria-pressed", String(item === button)));
            render();
        }));
        elements.differenceOnly.addEventListener("change", () => { state.differenceOnly = elements.differenceOnly.checked; render(); });
        elements.sort.addEventListener("change", () => { state.sort = elements.sort.value; render(); });
        elements.candidateButton.addEventListener("click", openCandidates);
        elements.emptyCandidate.addEventListener("click", openCandidates);
        elements.candidateClose.addEventListener("click", closeCandidates);
        elements.candidateSearch.addEventListener("input", () => { state.candidateKeyword = cleanText(elements.candidateSearch.value, 100); renderCandidates(); });
        elements.refresh.addEventListener("click", loadProducts);
        elements.emptyRetry.addEventListener("click", loadProducts);
        elements.copy.addEventListener("click", copySummary);
        elements.csv.addEventListener("click", exportCsv);
        elements.link.addEventListener("click", () => copyText(location.href, "공유 링크를 복사했습니다."));
        elements.print.addEventListener("click", () => {
            if (state.products.length < 2) {
                showToast("비교할 상품을 2개 이상 추가해주세요.");
                return;
            }
            window.print();
        });
        elements.clear.addEventListener("click", clearAll);
        window.addEventListener("storage", (event) => { if (event.key === KEYS.compare) void loadProducts(); });
        document.addEventListener("storefront:state-ready", () => void loadProducts());
        window.addEventListener("popstate", loadProducts);
        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && !elements.candidates.hidden) closeCandidates();
            if (event.key.toLowerCase() === "c" && !isTyping(event.target)) copySummary();
            const index = Number(event.key) - 1;
            if (index >= 0 && index < state.products.length && !isTyping(event.target)) location.href = `/front/products/${state.products[index].id}`;
        });
    }

    function cell(value, className, role) {
        const node = document.createElement("div");
        node.className = className || "";
        if (role) node.setAttribute("role", role);
        node.textContent = value;
        return node;
    }

    function actionButton(text, label, handler) {
        const button = document.createElement("button");
        button.type = "button";
        button.textContent = text;
        button.setAttribute("aria-label", label);
        button.addEventListener("click", handler);
        return button;
    }

    async function copyText(text, message) {
        try {
            await navigator.clipboard.writeText(text);
            showToast(message);
        } catch (error) {
            showToast("복사하지 못했습니다.");
        }
    }

    function readProducts(key) {
        try {
            const value = window.StorefrontState
                ? window.StorefrontState.read(key)
                : JSON.parse(localStorage.getItem(key) || "[]");
            if (!Array.isArray(value)) return [];
            const ids = new Set();
            return value.flatMap((item) => {
                try {
                    const snapshot = normalizeStoredSnapshot(item);
                    if (ids.has(snapshot.id)) return [];
                    ids.add(snapshot.id);
                    return [snapshot];
                } catch (error) {
                    return [];
                }
            });
        } catch (error) {
            return [];
        }
    }

    function writeProducts(key, products) {
        if (window.StorefrontState) {
            if (!window.StorefrontState.write(key, products)) {
                showToast("비교 정보를 저장하지 못했습니다.");
            }
            return;
        }
        try { localStorage.setItem(key, JSON.stringify(products)); } catch (error) { showToast("비교 정보를 저장하지 못했습니다."); }
    }

    function uniqueProducts(items) {
        return items.filter((item, index) => items.findIndex((source) => Number(source.id) === Number(item.id)) === index);
    }

    function cleanText(value, maxLength) { return String(value ?? "").trim().replace(/\s+/g, " ").slice(0, maxLength); }
    function safeInteger(value, fieldName, minimum = 0) {
        if (!Number.isSafeInteger(value) || value < minimum) throw new Error(`${fieldName} 정보가 올바르지 않습니다.`);
        return value;
    }
    function safeImage(value) {
        const image = cleanText(value, 500);
        return /^\/(?!\/)/.test(image) || /^https?:\/\//i.test(image) ? image : PLACEHOLDER;
    }
    function normalizeOptions(value) {
        if (!Array.isArray(value) || value.length > 100) throw new Error("상품 옵션 정보가 올바르지 않습니다.");
        const ids = new Set();
        const names = new Set();
        return value.map((option) => {
            const id = safeInteger(option?.id, "옵션 번호", 1);
            const name = cleanText(option?.name, 100);
            if (!name || ids.has(id) || names.has(name)) throw new Error("상품 옵션 정보가 중복되었습니다.");
            ids.add(id);
            names.add(name);
            return {
                id,
                name,
                stock: safeInteger(option.stock, "옵션 재고"),
                additionalPrice: safeInteger(option.additionalPrice, "옵션 추가 금액")
            };
        });
    }
    function normalizeStoredSnapshot(item) {
        const id = Number(item?.id);
        if (!Number.isSafeInteger(id) || id <= 0) throw new Error("상품 번호가 올바르지 않습니다.");
        return {
            ...item,
            id,
            brand: cleanText(item.brand, 100),
            name: cleanText(item.name || item.headline, 200),
            model: cleanText(item.model, 100),
            category: cleanText(item.category, 100),
            price: Number.isSafeInteger(Number(item.price)) && Number(item.price) >= 0 ? Number(item.price) : 0,
            stock: Number.isSafeInteger(Number(item.stock)) && Number(item.stock) >= 0 ? Number(item.stock) : 0,
            thumbnailUrl: safeImage(item.thumbnailUrl),
            options: []
        };
    }
    function normalizeProductResponse(item, expectedId) {
        if (!item || typeof item !== "object" || Array.isArray(item)) throw new Error("상품 응답이 올바르지 않습니다.");
        const id = safeInteger(item.id, "상품 번호", 1);
        const brand = cleanText(item.brand, 100);
        const name = cleanText(item.name, 200);
        const category = cleanText(item.category, 100);
        if (id !== expectedId || !brand || !name || !category) throw new Error("조회한 상품과 응답 정보가 일치하지 않습니다.");
        const price = safeInteger(item.price, "상품 가격");
        const stock = safeInteger(item.stock, "상품 재고");
        const options = normalizeOptions(item.options);
        if (options.length && options.reduce((sum, option) => sum + option.stock, 0) !== stock) {
            throw new Error("상품 재고가 옵션 합계와 일치하지 않습니다.");
        }
        return {
            id, brand, name, category, price, stock, options,
            model: cleanText(item.model, 100),
            headline: cleanText(item.headline, 200),
            description: cleanText(item.description, 1000),
            mood: cleanText(item.mood, 100),
            createdDate: cleanText(item.createdDate, 30),
            featured: item.featured === true,
            featuredRank: Number.isSafeInteger(item.featuredRank) && item.featuredRank > 0 ? item.featuredRank : null,
            stockStatus: cleanText(item.stockStatus, 40) || (stock <= 10 ? "품절 임박" : "재고 안정"),
            priceLabel: formatPrice(price),
            thumbnailUrl: safeImage(item.thumbnailUrl)
        };
    }
    function hasStoredSnapshot(product) { return Boolean(product?.name && product?.brand && Number.isSafeInteger(product?.price)); }
    function safeOptions(product) { return Array.isArray(product?.options) ? product.options : []; }
    function range(values) { return values.length ? Math.max(...values) - Math.min(...values) : 0; }
    function normalized(value, values) { const min = Math.min(...values); const gap = Math.max(...values) - min; return gap ? (value - min) / gap * 100 : 100; }
    function normalizedInverse(value, values) { return 100 - normalized(value, values) + (range(values) ? 0 : 100); }
    function formatPrice(value) { return `${Number(value || 0).toLocaleString("ko-KR")}원`; }
    function modeLabel(mode) { return mode === "PRICE" ? "가격 우선" : mode === "STOCK" ? "재고 우선" : "가격·재고 균형"; }
    function naturalCompare(a, b) { return String(a).localeCompare(String(b), "ko-KR", { numeric: true }); }
    function normalize(value) { return String(value || "").trim().toLocaleLowerCase("ko-KR"); }
    function csvCell(value) { return `"${String(value ?? "").replaceAll('"', '""')}"`; }
    function escapeHtml(value) { return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;").replaceAll("'", "&#039;"); }
    function isTyping(target) { return target instanceof HTMLElement && (target.matches("input, textarea, select") || target.isContentEditable); }
    function announce(message) { elements.live.textContent = ""; requestAnimationFrame(() => { elements.live.textContent = message; }); }
    function showToast(message) { clearTimeout(toastTimer); elements.toast.textContent = message; elements.toast.hidden = false; toastTimer = setTimeout(() => { elements.toast.hidden = true; }, 2200); }

    init();
}());
