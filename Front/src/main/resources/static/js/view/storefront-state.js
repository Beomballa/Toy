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
    const ownerKey = "front-member-activity-owner";
    const serverTypes = Object.freeze({
        [keys.bookmark]: "BOOKMARK",
        [keys.compare]: "COMPARE",
        [keys.recent]: "RECENT",
        [keys.hidden]: "HIDDEN"
    });
    const keysByServerType = Object.freeze(Object.fromEntries(
        Object.entries(serverTypes).map(([key, type]) => [type, key])
    ));
    const revisions = Object.fromEntries(Object.values(keys).map(key => [key, 0]));
    const queues = Object.fromEntries(Object.values(keys).map(key => [key, Promise.resolve()]));
    let authenticated = false;

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

    function persist(key, value) {
        window.localStorage.setItem(key, JSON.stringify(normalizeItems(key, value)));
        notify(key);
    }

    function write(nameOrKey, value) {
        const key = resolveKey(nameOrKey);
        if (!key || !Array.isArray(value)) return false;
        try {
            revisions[key] += 1;
            persist(key, value);
            queueReplace(key);
            return true;
        } catch (_) {
            return false;
        }
    }

    function remove(nameOrKey) {
        const key = resolveKey(nameOrKey);
        if (!key) return false;
        try {
            revisions[key] += 1;
            window.localStorage.removeItem(key);
            notify(key);
            queueReplace(key);
            return true;
        } catch (_) {
            return false;
        }
    }

    function count(nameOrKey) {
        return read(nameOrKey).length;
    }

    function clearLocalActivities() {
        Object.values(keys).forEach(key => {
            revisions[key] += 1;
            try {
                window.localStorage.removeItem(key);
            } catch (_) {
                return;
            }
            notify(key);
        });
    }

    function forgetSession() {
        authenticated = false;
        clearLocalActivities();
        try {
            window.localStorage.removeItem(ownerKey);
        } catch (_) {
            // 저장소 접근이 불가능해도 현재 메모리 인증 상태는 해제합니다.
        }
    }

    async function request(path, options = {}) {
        const response = await fetch(path, {
            credentials: "same-origin",
            headers: { "Content-Type": "application/json", ...(options.headers || {}) },
            ...options
        });
        if (response.status === 401) {
            authenticated = false;
            return null;
        }
        if (!response.ok) {
            throw new Error(`회원 활동 동기화 실패: ${response.status}`);
        }
        return response.json();
    }

    function applyServerActivities(response, revisionSnapshot = null, onlyKey = null) {
        if (!response || !response.activities) return;
        Object.entries(response.activities).forEach(([type, products]) => {
            const key = keysByServerType[type];
            if (!key || (onlyKey && key !== onlyKey)) return;
            if (revisionSnapshot && revisions[key] !== revisionSnapshot[key]) return;
            try {
                persist(key, products);
            } catch (_) {
                // 서버 상태는 유지되므로 다음 페이지 진입에서 다시 동기화합니다.
            }
        });
    }

    function queueReplace(key) {
        if (!authenticated || !key) return;
        queues[key] = queues[key]
            .catch(() => undefined)
            .then(async () => {
                const revision = revisions[key];
                const response = await request(`/api/front/member-activities/${serverTypes[key]}`, {
                    method: "PUT",
                    body: JSON.stringify({ productIds: read(key).map(item => item.id) })
                });
                if (revisions[key] === revision) {
                    applyServerActivities(response, null, key);
                }
            })
            .catch(() => undefined);
    }

    async function initialize() {
        try {
            const session = await request("/api/front/auth/me", { method: "GET" });
            authenticated = Boolean(session?.authenticated);
            const storedOwner = window.localStorage.getItem(ownerKey);
            if (!authenticated) {
                if (storedOwner) {
                    clearLocalActivities();
                    window.localStorage.removeItem(ownerKey);
                }
                return;
            }

            const memberOwner = String(session.memberId);
            if (storedOwner && storedOwner !== memberOwner) {
                clearLocalActivities();
            }
            window.localStorage.setItem(ownerKey, memberOwner);

            const revisionSnapshot = { ...revisions };
            const activities = {};
            Object.values(keys).forEach(key => {
                activities[serverTypes[key]] = read(key).map(item => item.id);
            });
            const response = await request("/api/front/member-activities/sync", {
                method: "PUT",
                body: JSON.stringify({ activities })
            });
            applyServerActivities(response, revisionSnapshot);
        } catch (_) {
            // 비로그인 및 일시적인 통신 실패에서는 기존 로컬 활동을 그대로 사용합니다.
        } finally {
            document.dispatchEvent(new CustomEvent("storefront:state-ready", {
                detail: { authenticated }
            }));
        }
    }

    const ready = initialize();
    window.StorefrontState = Object.freeze({ keys, read, write, remove, count, notify, forgetSession, ready });
})();
