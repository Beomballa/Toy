(() => {
    "use strict";

    const keys = Object.freeze({
        bookmark: "front-bookmark-products",
        compare: "front-compare-products",
        recent: "front-recent-viewed-products",
        hidden: "front-hidden-products"
    });
    const limits = Object.freeze({
        [keys.bookmark]: 24,
        [keys.compare]: 3,
        [keys.recent]: 12,
        [keys.hidden]: 12
    });
    const allowedKeys = new Set(Object.values(keys));

    function resolveKey(nameOrKey) {
        const key = keys[nameOrKey] || nameOrKey;
        return allowedKeys.has(key) ? key : null;
    }

    function normalizeItems(key, value) {
        if (!key || !Array.isArray(value)) return [];
        const ids = new Set();
        return value.flatMap((item) => {
            if (!item || typeof item !== "object" || Array.isArray(item)) return [];
            const id = Number(item.id);
            if (!Number.isSafeInteger(id) || id <= 0 || ids.has(id)) return [];
            ids.add(id);
            return [{ ...item, id }];
        }).slice(0, limits[key]);
    }

    function read(nameOrKey) {
        const key = resolveKey(nameOrKey);
        if (!key) return [];
        try {
            return normalizeItems(key, JSON.parse(window.localStorage.getItem(key) || "[]"));
        } catch (_) {
            return [];
        }
    }

    function notify(nameOrKey) {
        const key = resolveKey(nameOrKey);
        if (!key) return false;
        document.dispatchEvent(new CustomEvent("storefront:storage-change", {
            detail: { key, count: read(key).length }
        }));
    }

    function write(nameOrKey, value) {
        const key = resolveKey(nameOrKey);
        if (!key || !Array.isArray(value)) return false;
        try {
            window.localStorage.setItem(key, JSON.stringify(normalizeItems(key, value)));
            notify(key);
            return true;
        } catch (_) {
            return false;
        }
    }

    function remove(nameOrKey) {
        const key = resolveKey(nameOrKey);
        if (!key) return false;
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
