(() => {
    "use strict";

    const keys = Object.freeze({
        bookmark: "front-bookmark-products",
        compare: "front-compare-products",
        recent: "front-recent-viewed-products"
    });

    function resolveKey(nameOrKey) {
        return keys[nameOrKey] || nameOrKey;
    }

    function read(nameOrKey) {
        try {
            const value = JSON.parse(window.localStorage.getItem(resolveKey(nameOrKey)) || "[]");
            return Array.isArray(value) ? value : [];
        } catch (_) {
            return [];
        }
    }

    function notify(nameOrKey) {
        const key = resolveKey(nameOrKey);
        document.dispatchEvent(new CustomEvent("storefront:storage-change", {
            detail: { key, count: read(key).length }
        }));
    }

    function write(nameOrKey, value) {
        const key = resolveKey(nameOrKey);
        try {
            window.localStorage.setItem(key, JSON.stringify(Array.isArray(value) ? value : []));
            notify(key);
            return true;
        } catch (_) {
            return false;
        }
    }

    function remove(nameOrKey) {
        const key = resolveKey(nameOrKey);
        try {
            window.localStorage.removeItem(key);
            notify(key);
            return true;
        } catch (_) {
            return false;
        }
    }

    function count(nameOrKey) {
        return read(nameOrKey).length;
    }

    window.StorefrontState = Object.freeze({ keys, read, write, remove, count, notify });
})();
