(function () {
    const PLACEHOLDER_IMAGE = "/images/product-placeholder.svg";
    const frameImages = Array.from(document.querySelectorAll("#summerHeroMedia img"));
    const mediaToggle = document.getElementById("summerMediaToggle");
    const previousFrame = document.getElementById("summerPreviousFrame");
    const nextFrame = document.getElementById("summerNextFrame");
    const frameStatus = document.getElementById("summerFrameStatus");
    const productGrid = document.getElementById("summerProductGrid");
    const productStatus = document.getElementById("summerProductStatus");
    const productRetry = document.getElementById("summerProductRetry");
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
    let activeFrame = 0;
    let paused = reducedMotion.matches;
    let frameTimer = null;
    let productController = null;
    let productRequestSequence = 0;

    function init() {
        bindEvents();
        renderFrame();
        syncPlayback();
        void loadProducts();
    }

    function bindEvents() {
        mediaToggle?.addEventListener("click", () => {
            paused = !paused;
            syncPlayback();
        });
        previousFrame?.addEventListener("click", () => moveFrame(-1));
        nextFrame?.addEventListener("click", () => moveFrame(1));
        productRetry?.addEventListener("click", () => void loadProducts());
        reducedMotion.addEventListener?.("change", (event) => {
            paused = event.matches;
            syncPlayback();
        });
        document.addEventListener("visibilitychange", () => {
            if (document.hidden) stopPlayback();
            else syncPlayback();
        });
    }

    function moveFrame(direction) {
        activeFrame = (activeFrame + direction + frameImages.length) % frameImages.length;
        renderFrame();
        syncPlayback();
    }

    function renderFrame() {
        frameImages.forEach((image, index) => image.classList.toggle("is-active", index === activeFrame));
        if (frameStatus) frameStatus.textContent = `${String(activeFrame + 1).padStart(2, "0")} / ${String(frameImages.length).padStart(2, "0")}`;
    }

    function syncPlayback() {
        stopPlayback();
        if (mediaToggle) {
            mediaToggle.textContent = paused ? "Play" : "Pause";
            mediaToggle.setAttribute("aria-pressed", String(paused));
            mediaToggle.setAttribute("aria-label", paused ? "캠페인 미디어 재생" : "캠페인 미디어 일시정지");
        }
        if (!paused && !document.hidden && frameImages.length > 1) {
            frameTimer = window.setInterval(() => {
                activeFrame = (activeFrame + 1) % frameImages.length;
                renderFrame();
            }, 2400);
        }
    }

    function stopPlayback() {
        if (frameTimer) window.clearInterval(frameTimer);
        frameTimer = null;
    }

    async function loadProducts() {
        productController?.abort();
        productController = new AbortController();
        const activeRequest = ++productRequestSequence;
        productGrid?.setAttribute("aria-busy", "true");
        if (productGrid) productGrid.innerHTML = "";
        if (productStatus) productStatus.textContent = "상품을 불러오고 있습니다.";
        if (productRetry) productRetry.hidden = true;
        try {
            const response = await fetch("/api/front/products?page=0&size=8&sort=LATEST", { signal: productController.signal });
            if (!response.ok) throw new Error("상품 응답 오류");
            const page = normalizePage(await response.json());
            if (activeRequest !== productRequestSequence) return;
            if (!page) throw new Error("상품 응답 형식 오류");
            renderProducts(page.products);
            if (productStatus) productStatus.textContent = `${page.products.length}개 상품을 표시합니다.`;
        } catch (error) {
            if (error.name === "AbortError" || activeRequest !== productRequestSequence) return;
            if (productGrid) productGrid.innerHTML = '<div class="summer-product-state">상품을 불러오지 못했습니다.</div>';
            if (productStatus) productStatus.textContent = "상품을 불러오지 못했습니다.";
            if (productRetry) productRetry.hidden = false;
        } finally {
            if (activeRequest === productRequestSequence) productGrid?.setAttribute("aria-busy", "false");
        }
    }

    function normalizePage(value) {
        if (!value || typeof value !== "object" || !Array.isArray(value.products) || value.products.length > 8) return null;
        const products = value.products.map(normalizeProduct);
        return products.every(Boolean) ? { products } : null;
    }

    function normalizeProduct(value) {
        if (!value || typeof value !== "object") return null;
        const id = Number(value.id);
        const price = Number(value.price);
        if (!Number.isSafeInteger(id) || id <= 0 || !Number.isSafeInteger(price) || price < 0) return null;
        const text = (source, max) => typeof source === "string" && source.trim() && source.trim().length <= max ? source.trim() : null;
        const name = text(value.name, 160);
        const brand = text(value.brand, 80);
        if (!name || !brand) return null;
        return {
            id,
            name,
            brand,
            price,
            category: text(value.category, 80) || "NOREN 셀렉션",
            thumbnailUrl: text(value.thumbnailUrl, 500) || PLACEHOLDER_IMAGE
        };
    }

    function renderProducts(products) {
        if (!productGrid) return;
        if (products.length === 0) {
            productGrid.innerHTML = '<div class="summer-product-state">표시할 상품이 없습니다.</div>';
            return;
        }
        productGrid.innerHTML = products.map((product) => `
            <article class="summer-product-card">
                <a href="/front/products/${product.id}" aria-label="${escapeHtml(product.name)} 상세 보기">
                    <img src="${escapeHtml(product.thumbnailUrl)}" alt="${escapeHtml(product.name)}" loading="lazy" data-product-image>
                </a>
                <div class="summer-product-card__copy">
                    <strong>${escapeHtml(product.brand)}</strong>
                    <p>${escapeHtml(product.name)}</p>
                    <span>${product.price.toLocaleString("ko-KR")}원</span>
                    <em>${escapeHtml(product.category)}</em>
                </div>
            </article>`).join("");
        productGrid.querySelectorAll("[data-product-image]").forEach((image) => {
            image.addEventListener("error", () => {
                if (image.dataset.fallbackApplied === "true") return;
                image.dataset.fallbackApplied = "true";
                image.src = PLACEHOLDER_IMAGE;
            });
        });
    }

    function escapeHtml(value) {
        const element = document.createElement("span");
        element.textContent = String(value ?? "");
        return element.innerHTML;
    }

    init();
})();
