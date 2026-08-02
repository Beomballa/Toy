(() => {
    "use strict";

    const shell = document.querySelector("[data-store-shell]");
    if (!shell) {
        return;
    }

    const storageKeys = window.StorefrontState?.keys || {
        bookmark: "front-bookmark-products",
        compare: "front-compare-products"
    };
    const menuButton = shell.querySelector("[data-store-shell-menu-button]");
    const categoryNav = shell.querySelector("#storeShellCategoryNav");
    const searchLayer = shell.querySelector("[data-store-shell-search]");
    const searchOpenButton = shell.querySelector("[data-store-shell-search-open]");
    const searchCloseButton = shell.querySelector("[data-store-shell-search-close]");
    const searchForm = shell.querySelector("[data-store-shell-search-form]");
    const searchInput = shell.querySelector("[data-store-shell-search-input]");
    let searchReturnFocus = null;

    function storedCount(key) {
        if (window.StorefrontState) {
            return window.StorefrontState.count(key);
        }
        try {
            const value = JSON.parse(window.localStorage.getItem(key) || "[]");
            return Array.isArray(value) ? value.length : 0;
        } catch (_) {
            return 0;
        }
    }

    function syncCounts() {
        Object.entries(storageKeys).forEach(([name, key]) => {
            const count = storedCount(key);
            shell.querySelectorAll(`[data-store-shell-count="${name}"]`).forEach((target) => {
                target.textContent = String(count);
                target.hidden = count === 0;
            });
        });
    }

    function closeMenu(restoreFocus = false) {
        if (!shell.classList.contains("is-menu-open")) {
            return;
        }
        shell.classList.remove("is-menu-open");
        menuButton?.setAttribute("aria-expanded", "false");
        menuButton?.setAttribute("aria-label", "메뉴 열기");
        if (restoreFocus) {
            menuButton?.focus();
        }
    }

    function toggleMenu() {
        const willOpen = !shell.classList.contains("is-menu-open");
        shell.classList.toggle("is-menu-open", willOpen);
        menuButton?.setAttribute("aria-expanded", String(willOpen));
        menuButton?.setAttribute("aria-label", willOpen ? "메뉴 닫기" : "메뉴 열기");
        if (willOpen) {
            categoryNav?.querySelector("a")?.focus();
        }
    }

    function openSearch() {
        if (!searchLayer.hidden) return;
        closeMenu();
        searchReturnFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null;
        searchLayer.hidden = false;
        searchOpenButton?.setAttribute("aria-expanded", "true");
        document.body.classList.add("store-shell-lock");
        window.requestAnimationFrame(() => searchInput?.focus());
    }

    function closeSearch() {
        if (searchLayer.hidden) {
            return;
        }
        searchLayer.hidden = true;
        searchOpenButton?.setAttribute("aria-expanded", "false");
        document.body.classList.remove("store-shell-lock");
        if (searchReturnFocus?.isConnected) {
            searchReturnFocus.focus();
        }
        searchReturnFocus = null;
    }

    function submitSearch(event) {
        event.preventDefault();
        const keyword = String(searchInput?.value ?? "")
            .replace(/[\u0000-\u001f\u007f]/g, " ")
            .trim()
            .replace(/\s+/g, " ")
            .slice(0, 100);
        if (!keyword) {
            searchInput?.focus();
            return;
        }
        searchInput.value = keyword;
        window.location.assign(`/front?keyword=${encodeURIComponent(keyword)}#catalog`);
    }

    function trapSearchFocus(event) {
        if (event.key !== "Tab" || searchLayer.hidden) return;
        const focusable = Array.from(searchLayer.querySelectorAll("a[href], button:not([disabled]), input:not([disabled])"))
            .filter((item) => !item.hidden && item.getClientRects().length > 0);
        if (!focusable.length) return;
        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus();
        }
    }

    menuButton?.addEventListener("click", toggleMenu);
    categoryNav?.addEventListener("click", () => closeMenu());
    searchOpenButton?.addEventListener("click", openSearch);
    searchCloseButton?.addEventListener("click", closeSearch);
    searchForm?.addEventListener("submit", submitSearch);
    searchLayer?.addEventListener("click", (event) => {
        if (event.target === searchLayer) {
            closeSearch();
        }
    });
    searchLayer?.addEventListener("keydown", trapSearchFocus);
    window.addEventListener("storage", (event) => {
        if (event.key === null || Object.values(storageKeys).includes(event.key)) {
            syncCounts();
        }
    });
    window.addEventListener("resize", () => {
        if (window.innerWidth >= 768) {
            closeMenu();
        }
    });
    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !searchLayer.hidden) {
            closeSearch();
            return;
        }
        if (event.key === "Escape") {
            closeMenu(true);
        }
    });
    document.addEventListener("click", (event) => {
        if (shell.classList.contains("is-menu-open") && !shell.contains(event.target)) {
            closeMenu();
        }
        if (event.target.closest(
            "[data-bookmark-product-id], [data-compare-product-id], [data-related-bookmark-id], " +
            "[data-related-compare-id], #detailBookmarkButton, #detailCompareButton, " +
            "#detailMobileBookmarkButton, #detailMobileCompareButton"
        )) {
            window.requestAnimationFrame(syncCounts);
        }
    });
    document.addEventListener("storefront:storage-change", (event) => {
        if (!event.detail?.key || Object.values(storageKeys).includes(event.detail.key)) {
            syncCounts();
        }
    });

    syncCounts();
})();
