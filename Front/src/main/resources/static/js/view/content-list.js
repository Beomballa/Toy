(function () {
    const DEFAULT_STATE = { boardType: "ALL", keyword: "", page: 0, size: 8, sort: "LATEST" };
    const VALID_SIZES = [4, 8, 12, 20];
    const VALID_SORTS = ["LATEST", "POPULAR", "OLDEST"];
    const RECENT_CONTENT_KEY = "front-recent-content";
    const BOOKMARKED_CONTENT_KEY = "front-bookmarked-content";
    const READING_PROGRESS_KEY = "front-content-reading-progress";
    const CONTENT_RETURN_URL_KEY = "front-content-return-url";
    const SAVED_CONTENT_PREVIEW_LIMIT = 6;
    const state = { ...DEFAULT_STATE };
    let requestController = null;
    let requestSequence = 0;
    let savedContentsExpanded = false;
    let savedHashFocused = false;

    const elements = {
        liveStatus: document.getElementById("contentListLiveStatus"),
        tabs: Array.from(document.querySelectorAll("[data-content-board]")),
        searchForm: document.getElementById("contentListSearchForm"),
        keyword: document.getElementById("contentListKeyword"),
        clearButton: document.getElementById("contentListClearButton"),
        sortSelect: document.getElementById("contentListSortSelect"),
        sizeSelect: document.getElementById("contentListSizeSelect"),
        resetButton: document.getElementById("contentListResetButton"),
        heading: document.getElementById("contentListHeading"),
        resultText: document.getElementById("contentListResultText"),
        pageViews: document.getElementById("contentListPageViews"),
        pagePinned: document.getElementById("contentListPagePinned"),
        pageNotices: document.getElementById("contentListPageNotices"),
        pageStyles: document.getElementById("contentListPageStyles"),
        appliedFilters: document.getElementById("contentListAppliedFilters"),
        grid: document.getElementById("contentListGrid"),
        pagination: document.getElementById("contentListPagination"),
        previousButton: document.getElementById("contentListPreviousButton"),
        nextButton: document.getElementById("contentListNextButton"),
        pageSelect: document.getElementById("contentListPageSelect"),
        pageText: document.getElementById("contentListPageText"),
        rangeText: document.getElementById("contentListRangeText"),
        recentBoard: document.getElementById("contentRecentBoard"),
        recentGrid: document.getElementById("contentRecentGrid"),
        recentClearButton: document.getElementById("contentRecentClearButton"),
        savedUtilityCount: document.getElementById("contentSavedUtilityCount"),
        savedBoard: document.getElementById("contentSavedBoard"),
        savedGrid: document.getElementById("contentSavedGrid"),
        savedCount: document.getElementById("contentSavedCount"),
        savedNoticeCount: document.getElementById("contentSavedNoticeCount"),
        savedStyleCount: document.getElementById("contentSavedStyleCount"),
        savedReadingCount: document.getElementById("contentSavedReadingCount"),
        savedCopyButton: document.getElementById("contentSavedCopyButton"),
        savedClearButton: document.getElementById("contentSavedClearButton"),
        savedExpandButton: document.getElementById("contentSavedExpandButton")
    };

    function hydrateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        const boardType = String(params.get("boardType") || "ALL").toUpperCase();
        state.boardType = ["NOTICE", "STYLE"].includes(boardType) ? boardType : "ALL";
        state.keyword = String(params.get("keyword") || "").trim().slice(0, 100);
        const page = Number(params.get("page") || 0);
        state.page = Number.isInteger(page) && page >= 0 ? page : 0;
        const size = Number(params.get("size") || DEFAULT_STATE.size);
        state.size = VALID_SIZES.includes(size) ? size : DEFAULT_STATE.size;
        const sort = String(params.get("sort") || DEFAULT_STATE.sort).toUpperCase();
        state.sort = VALID_SORTS.includes(sort) ? sort : DEFAULT_STATE.sort;
    }

    async function loadContents(options = {}) {
        const sequence = ++requestSequence;
        requestController?.abort();
        requestController = new AbortController();
        syncControls();
        showState("LOADING");
        if (options.updateUrl !== false) updateUrl(options.historyMode);
        try {
            const params = new URLSearchParams({
                page: String(state.page),
                size: String(state.size),
                sort: state.sort
            });
            if (state.boardType !== "ALL") params.set("boardType", state.boardType);
            if (state.keyword) params.set("keyword", state.keyword);
            const response = await fetch(`/api/front/content?${params}`, { signal: requestController.signal });
            if (!response.ok) throw new Error("콘텐츠를 불러오지 못했습니다.");
            const payload = await response.json();
            if (sequence !== requestSequence) return;
            if (correctOutOfRangePage(payload)) return;
            renderPage(payload);
        } catch (error) {
            if (error?.name === "AbortError" || sequence !== requestSequence) return;
            showState("ERROR");
        }
    }

    function renderPage(payload) {
        const items = Array.isArray(payload?.items) ? payload.items : [];
        elements.grid.replaceChildren();
        elements.grid.classList.remove("is-loading", "is-error");
        elements.grid.setAttribute("aria-busy", "false");
        if (!items.length) {
            elements.grid.appendChild(createEmptyState());
        } else {
            items.forEach((item, index) => elements.grid.appendChild(createCard(item, index, payload.page)));
        }
        const boardLabel = state.boardType === "NOTICE" ? "공지" : state.boardType === "STYLE" ? "스타일" : "전체 콘텐츠";
        setText(elements.heading, boardLabel);
        const totalElements = Number(payload.totalElements || 0);
        setText(elements.resultText, `${totalElements.toLocaleString("ko-KR")}개의 공개 콘텐츠가 있습니다.`);
        const totalPages = Math.max(1, Number(payload.totalPages || 0));
        const currentPage = Number(payload.page || 0);
        setText(elements.pageText, `${currentPage + 1} / ${totalPages} 페이지`);
        setText(elements.pageViews, Number(payload.pageViewCount || 0).toLocaleString("ko-KR"));
        setText(elements.pagePinned, Number(payload.pagePinnedCount || 0).toLocaleString("ko-KR"));
        setText(elements.pageNotices, Number(payload.pageNoticeCount || 0).toLocaleString("ko-KR"));
        setText(elements.pageStyles, Number(payload.pageStyleCount || 0).toLocaleString("ko-KR"));
        const rangeStart = totalElements === 0 ? 0 : currentPage * state.size + 1;
        const rangeEnd = totalElements === 0 ? 0 : Math.min(totalElements, rangeStart + items.length - 1);
        setText(elements.rangeText, `${rangeStart}-${rangeEnd} / 총 ${totalElements.toLocaleString("ko-KR")}개`);
        renderPageOptions(totalPages, currentPage);
        renderAppliedFilters();
        elements.previousButton.disabled = Boolean(payload.first);
        elements.nextButton.disabled = Boolean(payload.last) || Number(payload.totalPages || 0) === 0;
        elements.pagination.hidden = Number(payload.totalPages || 0) <= 1;
        document.title = `${boardLabel} ${totalElements.toLocaleString("ko-KR")}개 | Grade Stock`;
        announce(`${items.length}개의 콘텐츠를 ${sortLabel(state.sort)}으로 표시했습니다.`);
    }

    function correctOutOfRangePage(payload) {
        const totalPages = Number(payload?.totalPages || 0);
        if (totalPages > 0 && state.page >= totalPages) {
            state.page = totalPages - 1;
            void loadContents({ historyMode: "replace" });
            return true;
        }
        return false;
    }

    function createCard(item, index, page) {
        const card = document.createElement("article");
        card.className = `content-list-card content-list-card--${item.boardType === "STYLE" ? "style" : "notice"}`;
        const link = document.createElement("a");
        link.className = "content-list-card__link";
        link.href = `/front/content/${Number(item.id)}`;
        link.addEventListener("click", rememberReturnUrl);
        const visual = document.createElement("div");
        visual.className = "content-list-card__visual";
        const type = document.createElement("span");
        type.textContent = item.boardType === "STYLE" ? "STYLE EDIT" : "NOTICE";
        const number = document.createElement("strong");
        number.textContent = String(Number(page || 0) * state.size + index + 1).padStart(2, "0");
        visual.append(type, number);
        const body = document.createElement("div");
        body.className = "content-list-card__body";
        const meta = document.createElement("div");
        meta.className = "content-list-card__meta";
        if (item.pinned) {
            const pinned = document.createElement("strong");
            pinned.textContent = "PINNED";
            meta.appendChild(pinned);
        }
        const date = document.createElement("span");
        date.textContent = item.createdDate || "최근 게시";
        const views = document.createElement("span");
        views.textContent = `조회 ${Number(item.viewCount || 0).toLocaleString("ko-KR")}`;
        meta.append(date, views);
        const title = document.createElement("h3");
        title.textContent = item.title || "제목 없는 콘텐츠";
        const summary = document.createElement("p");
        summary.textContent = item.summary || "내용을 확인해 주세요.";
        const more = document.createElement("em");
        more.textContent = "읽어보기 →";
        body.append(meta, title, summary, more);
        link.append(visual, body);
        const bookmark = createBookmarkButton(item);
        card.append(link, bookmark);
        return card;
    }

    function createBookmarkButton(item) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "content-list-card__bookmark";
        button.dataset.contentBookmarkId = String(Number(item.id));
        button.setAttribute("aria-pressed", String(isBookmarked(item.id)));
        button.setAttribute("aria-label", `${item.title || "콘텐츠"} 관심 저장`);
        button.textContent = isBookmarked(item.id) ? "저장됨" : "저장";
        button.addEventListener("click", () => toggleCardBookmark(item));
        return button;
    }

    function showState(name) {
        const isError = name === "ERROR";
        elements.grid.replaceChildren(createState(
            isError ? "콘텐츠를 불러오지 못했습니다." : "콘텐츠를 불러오는 중입니다.",
            isError
        ));
        elements.grid.classList.toggle("is-loading", !isError);
        elements.grid.classList.toggle("is-error", isError);
        elements.grid.setAttribute("aria-busy", String(!isError));
        elements.pagination.hidden = true;
        announce(isError ? "콘텐츠 조회에 실패했습니다." : "콘텐츠를 불러오는 중입니다.");
    }

    function createState(message, retryable) {
        const stateElement = document.createElement("div");
        stateElement.className = "content-list-state";
        const text = document.createElement("p");
        text.textContent = message;
        stateElement.appendChild(text);
        if (retryable) {
            const retry = document.createElement("button");
            retry.type = "button";
            retry.textContent = "다시 불러오기";
            retry.addEventListener("click", () => loadContents({ updateUrl: false }));
            stateElement.appendChild(retry);
        }
        return stateElement;
    }

    function createEmptyState() {
        const emptyState = createState("검색 조건에 맞는 공개 콘텐츠가 없습니다.", false);
        if (hasActiveFilters()) {
            const reset = document.createElement("button");
            reset.type = "button";
            reset.textContent = "전체 콘텐츠 보기";
            reset.addEventListener("click", resetFilters);
            emptyState.appendChild(reset);
        }
        return emptyState;
    }

    function syncControls() {
        elements.keyword.value = state.keyword;
        elements.sortSelect.value = state.sort;
        elements.sizeSelect.value = String(state.size);
        elements.clearButton.hidden = !state.keyword;
        elements.resetButton.disabled = !hasActiveFilters();
        elements.tabs.forEach((tab) => {
            const active = tab.dataset.contentBoard === state.boardType;
            tab.setAttribute("aria-selected", String(active));
            tab.tabIndex = active ? 0 : -1;
        });
    }

    function updateUrl(historyMode = "push") {
        const params = new URLSearchParams();
        if (state.boardType !== "ALL") params.set("boardType", state.boardType);
        if (state.keyword) params.set("keyword", state.keyword);
        if (state.page > 0) params.set("page", String(state.page));
        if (state.size !== DEFAULT_STATE.size) params.set("size", String(state.size));
        if (state.sort !== DEFAULT_STATE.sort) params.set("sort", state.sort);
        const query = params.toString();
        const nextUrl = query ? `${window.location.pathname}?${query}` : window.location.pathname;
        const currentUrl = `${window.location.pathname}${window.location.search}`;
        if (nextUrl === currentUrl) return;
        if (historyMode === "replace") {
            window.history.replaceState(null, "", nextUrl);
        } else {
            window.history.pushState(null, "", nextUrl);
        }
    }

    function bindEvents() {
        elements.tabs.forEach((tab) => tab.addEventListener("click", () => {
            state.boardType = tab.dataset.contentBoard;
            state.page = 0;
            void loadContents();
        }));
        elements.tabs.forEach((tab) => tab.addEventListener("keydown", handleTabKeydown));
        elements.searchForm.addEventListener("submit", (event) => {
            event.preventDefault();
            state.keyword = elements.keyword.value.trim().slice(0, 100);
            state.page = 0;
            void loadContents();
        });
        elements.keyword.addEventListener("input", () => {
            elements.clearButton.hidden = !elements.keyword.value;
        });
        elements.clearButton.addEventListener("click", () => {
            elements.keyword.value = "";
            state.keyword = "";
            state.page = 0;
            void loadContents();
            elements.keyword.focus();
        });
        elements.previousButton.addEventListener("click", () => changePage(-1));
        elements.nextButton.addEventListener("click", () => changePage(1));
        elements.pageSelect.addEventListener("change", () => {
            state.page = Math.max(0, Number(elements.pageSelect.value) || 0);
            void loadContents();
            focusListHeading();
        });
        elements.sortSelect.addEventListener("change", () => {
            state.sort = VALID_SORTS.includes(elements.sortSelect.value)
                ? elements.sortSelect.value
                : DEFAULT_STATE.sort;
            state.page = 0;
            void loadContents();
        });
        elements.sizeSelect.addEventListener("change", () => {
            const size = Number(elements.sizeSelect.value);
            state.size = VALID_SIZES.includes(size) ? size : DEFAULT_STATE.size;
            state.page = 0;
            void loadContents();
        });
        elements.resetButton.addEventListener("click", resetFilters);
        window.addEventListener("popstate", () => {
            hydrateFromUrl();
            void loadContents({ updateUrl: false });
        });
        window.addEventListener("hashchange", () => {
            savedHashFocused = false;
            focusSavedBoardFromHash(readBookmarks().length);
        });
        window.addEventListener("storage", handleStorageChange);
        elements.recentClearButton.addEventListener("click", clearRecentContents);
        elements.savedCopyButton.addEventListener("click", copySavedLinks);
        elements.savedClearButton.addEventListener("click", clearSavedContents);
        elements.savedExpandButton.addEventListener("click", toggleSavedContents);
    }

    function renderRecentContents() {
        const items = readRecentContents();
        const progress = readReadingProgress();
        elements.recentGrid.replaceChildren();
        elements.recentBoard.hidden = items.length === 0;
        items.forEach((item) => {
            const link = document.createElement("a");
            link.href = `/front/content/${Number(item.id)}`;
            link.className = "content-recent-card";
            link.addEventListener("click", rememberReturnUrl);
            const type = document.createElement("span");
            type.textContent = item.boardType === "STYLE" ? "STYLE EDIT" : "NOTICE";
            const title = document.createElement("strong");
            title.textContent = item.title || "제목 없는 콘텐츠";
            const date = document.createElement("em");
            const readingProgress = progress[String(Number(item.id))]?.progress;
            date.textContent = Number(readingProgress) > 0
                ? `${recentTimeLabel(item.viewedAt)} · ${Math.round(Number(readingProgress))}% 읽음`
                : recentTimeLabel(item.viewedAt);
            link.append(type, title, date);
            elements.recentGrid.appendChild(link);
        });
    }

    function readRecentContents() {
        try {
            const value = JSON.parse(window.localStorage.getItem(RECENT_CONTENT_KEY) || "[]");
            return normalizeStoredContentItems(value, 6);
        } catch (error) {
            return [];
        }
    }

    function clearRecentContents() {
        try {
            window.localStorage.removeItem(RECENT_CONTENT_KEY);
        } catch (error) {
            announce("최근 읽은 콘텐츠를 비우지 못했습니다.");
            return;
        }
        renderRecentContents();
        announce("최근 읽은 콘텐츠를 비웠습니다.");
    }

    function readBookmarks() {
        try {
            const value = JSON.parse(window.localStorage.getItem(BOOKMARKED_CONTENT_KEY) || "[]");
            return normalizeStoredContentItems(value, 50);
        } catch (error) {
            return [];
        }
    }

    function normalizeStoredContentItems(value, limit) {
        if (!Array.isArray(value)) return [];
        const seen = new Set();
        return value
            .filter((item) => {
                const id = Number(item?.id);
                if (!Number.isSafeInteger(id) || id <= 0 || seen.has(id)) return false;
                seen.add(id);
                return true;
            })
            .map((item) => ({ ...item, id: Number(item.id) }))
            .slice(0, limit);
    }

    function readReadingProgress() {
        try {
            const value = JSON.parse(window.localStorage.getItem(READING_PROGRESS_KEY) || "{}");
            return value && typeof value === "object" && !Array.isArray(value) ? value : {};
        } catch (error) {
            return {};
        }
    }

    function writeBookmarks(bookmarks) {
        window.localStorage.setItem(BOOKMARKED_CONTENT_KEY, JSON.stringify(bookmarks.slice(0, 50)));
    }

    function isBookmarked(contentId) {
        return readBookmarks().some((item) => Number(item.id) === Number(contentId));
    }

    function toggleCardBookmark(item) {
        try {
            const bookmarks = readBookmarks();
            const contentId = Number(item.id);
            const bookmarked = bookmarks.some((saved) => Number(saved.id) === contentId);
            const next = bookmarked
                ? bookmarks.filter((saved) => Number(saved.id) !== contentId)
                : [{
                    id: contentId,
                    boardType: item.boardType,
                    title: item.title,
                    createdDate: item.createdDate,
                    savedAt: new Date().toISOString()
                }].concat(bookmarks).slice(0, 50);
            writeBookmarks(next);
            renderSavedContents();
            syncVisibleBookmarkButtons();
            announce(bookmarked ? "관심 콘텐츠에서 제거했습니다." : "관심 콘텐츠로 저장했습니다.");
        } catch (error) {
            announce("관심 콘텐츠를 저장하지 못했습니다.");
        }
    }

    function syncVisibleBookmarkButtons() {
        const savedIds = new Set(readBookmarks().map((item) => Number(item.id)));
        document.querySelectorAll("[data-content-bookmark-id]").forEach((button) => {
            const pressed = savedIds.has(Number(button.dataset.contentBookmarkId));
            button.setAttribute("aria-pressed", String(pressed));
            button.textContent = pressed ? "저장됨" : "저장";
        });
    }

    function renderSavedContents() {
        const bookmarks = readBookmarks();
        const progress = readReadingProgress();
        const visibleItems = savedContentsExpanded
            ? bookmarks
            : bookmarks.slice(0, SAVED_CONTENT_PREVIEW_LIMIT);
        elements.savedGrid.replaceChildren();
        elements.savedBoard.hidden = bookmarks.length === 0;
        setText(elements.savedUtilityCount, String(bookmarks.length));
        setText(elements.savedCount, String(bookmarks.length));
        setText(elements.savedNoticeCount, String(bookmarks.filter((item) => item.boardType === "NOTICE").length));
        setText(elements.savedStyleCount, String(bookmarks.filter((item) => item.boardType === "STYLE").length));
        setText(elements.savedReadingCount, String(bookmarks.filter((item) => {
            const value = Number(progress[String(Number(item.id))]?.progress || 0);
            return value > 0 && value < 100;
        }).length));
        visibleItems.forEach((item) => elements.savedGrid.appendChild(createSavedCard(item, progress)));
        elements.savedExpandButton.hidden = bookmarks.length <= SAVED_CONTENT_PREVIEW_LIMIT;
        elements.savedExpandButton.textContent = savedContentsExpanded
            ? "접기"
            : `전체 ${bookmarks.length}개 보기`;
        focusSavedBoardFromHash(bookmarks.length);
    }

    function createSavedCard(item, progress) {
        const card = document.createElement("article");
        card.className = "content-saved-card";
        const link = document.createElement("a");
        link.href = `/front/content/${Number(item.id)}`;
        link.addEventListener("click", rememberReturnUrl);
        const type = document.createElement("span");
        type.textContent = item.boardType === "STYLE" ? "STYLE EDIT" : "NOTICE";
        const title = document.createElement("strong");
        title.textContent = item.title || "제목 없는 콘텐츠";
        const meta = document.createElement("em");
        const value = Math.round(Number(progress[String(Number(item.id))]?.progress || 0));
        meta.textContent = value > 0 ? `${value}% 읽음 · ${savedTimeLabel(item.savedAt)}` : savedTimeLabel(item.savedAt);
        link.append(type, title, meta);
        const remove = document.createElement("button");
        remove.type = "button";
        remove.textContent = "삭제";
        remove.setAttribute("aria-label", `${item.title || "콘텐츠"} 관심 목록에서 삭제`);
        remove.addEventListener("click", () => removeSavedContent(item.id));
        card.append(link, remove);
        return card;
    }

    function removeSavedContent(contentId) {
        try {
            writeBookmarks(readBookmarks().filter((item) => Number(item.id) !== Number(contentId)));
            renderSavedContents();
            syncVisibleBookmarkButtons();
            announce("관심 콘텐츠에서 제거했습니다.");
        } catch (error) {
            announce("관심 콘텐츠를 삭제하지 못했습니다.");
        }
    }

    function clearSavedContents() {
        const bookmarks = readBookmarks();
        if (!bookmarks.length || !window.confirm("관심 콘텐츠를 모두 비울까요?")) return;
        try {
            window.localStorage.removeItem(BOOKMARKED_CONTENT_KEY);
            savedContentsExpanded = false;
            renderSavedContents();
            syncVisibleBookmarkButtons();
            announce("관심 콘텐츠를 모두 비웠습니다.");
        } catch (error) {
            announce("관심 콘텐츠를 비우지 못했습니다.");
        }
    }

    async function copySavedLinks() {
        const bookmarks = readBookmarks();
        if (!bookmarks.length) {
            announce("복사할 관심 콘텐츠가 없습니다.");
            return;
        }
        const text = bookmarks.map((item) => {
            const url = new URL(`/front/content/${Number(item.id)}`, window.location.origin);
            return `${item.title || "제목 없는 콘텐츠"} ${url}`;
        }).join("\n");
        try {
            await navigator.clipboard.writeText(text);
            announce(`${bookmarks.length}개의 관심 콘텐츠 링크를 복사했습니다.`);
        } catch (error) {
            announce("관심 콘텐츠 링크를 복사하지 못했습니다.");
        }
    }

    function toggleSavedContents() {
        savedContentsExpanded = !savedContentsExpanded;
        renderSavedContents();
        if (!savedContentsExpanded) elements.savedBoard.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    function handleStorageChange(event) {
        if (![BOOKMARKED_CONTENT_KEY, READING_PROGRESS_KEY, RECENT_CONTENT_KEY].includes(event.key)) return;
        if (event.key === RECENT_CONTENT_KEY || event.key === READING_PROGRESS_KEY) renderRecentContents();
        if (event.key === BOOKMARKED_CONTENT_KEY || event.key === READING_PROGRESS_KEY) {
            renderSavedContents();
            syncVisibleBookmarkButtons();
        }
    }

    function focusSavedBoardFromHash(itemCount) {
        if (savedHashFocused || window.location.hash !== "#contentSavedBoard") return;
        savedHashFocused = true;
        if (!itemCount) {
            announce("저장한 관심 콘텐츠가 없습니다.");
            return;
        }
        window.requestAnimationFrame(() => elements.savedBoard.scrollIntoView({ block: "start" }));
    }

    function savedTimeLabel(value) {
        const savedAt = new Date(value);
        if (Number.isNaN(savedAt.getTime())) return "저장됨";
        const minutes = Math.max(0, Math.floor((Date.now() - savedAt.getTime()) / 60000));
        if (minutes < 1) return "방금 저장";
        if (minutes < 60) return `${minutes}분 전 저장`;
        if (minutes < 1440) return `${Math.floor(minutes / 60)}시간 전 저장`;
        return `${Math.floor(minutes / 1440)}일 전 저장`;
    }

    function recentTimeLabel(value) {
        const viewedAt = new Date(value);
        if (Number.isNaN(viewedAt.getTime())) return "최근 읽음";
        const minutes = Math.max(0, Math.floor((Date.now() - viewedAt.getTime()) / 60000));
        if (minutes < 1) return "방금 읽음";
        if (minutes < 60) return `${minutes}분 전`;
        if (minutes < 1440) return `${Math.floor(minutes / 60)}시간 전`;
        return `${Math.floor(minutes / 1440)}일 전`;
    }

    function rememberReturnUrl() {
        try {
            window.sessionStorage.setItem(CONTENT_RETURN_URL_KEY, window.location.href);
        } catch (error) {
            // Navigation remains available when session storage is unavailable.
        }
    }

    function changePage(direction) {
        state.page = Math.max(0, state.page + direction);
        void loadContents();
        focusListHeading();
    }

    function focusListHeading() {
        elements.heading?.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    function resetFilters() {
        Object.assign(state, DEFAULT_STATE);
        elements.keyword.value = "";
        void loadContents();
        elements.tabs[0]?.focus();
    }

    function hasActiveFilters() {
        return state.boardType !== DEFAULT_STATE.boardType
            || Boolean(state.keyword)
            || state.sort !== DEFAULT_STATE.sort
            || state.size !== DEFAULT_STATE.size;
    }

    function renderPageOptions(totalPages, currentPage) {
        elements.pageSelect.replaceChildren();
        for (let page = 0; page < totalPages; page += 1) {
            const option = document.createElement("option");
            option.value = String(page);
            option.textContent = `${page + 1} 페이지`;
            option.selected = page === currentPage;
            elements.pageSelect.appendChild(option);
        }
    }

    function renderAppliedFilters() {
        elements.appliedFilters.replaceChildren();
        const filters = [
            state.boardType !== "ALL" ? (state.boardType === "NOTICE" ? "공지" : "스타일") : "전체 유형",
            state.keyword ? `검색: ${state.keyword}` : null,
            sortLabel(state.sort),
            `${state.size}개씩 보기`
        ].filter(Boolean);
        filters.forEach((label) => {
            const chip = document.createElement("span");
            chip.textContent = label;
            elements.appliedFilters.appendChild(chip);
        });
    }

    function sortLabel(sort) {
        if (sort === "POPULAR") return "조회순";
        if (sort === "OLDEST") return "오래된순";
        return "최신순";
    }

    function handleTabKeydown(event) {
        if (!["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key)) return;
        event.preventDefault();
        const currentIndex = elements.tabs.indexOf(event.currentTarget);
        let nextIndex = currentIndex;
        if (event.key === "Home") nextIndex = 0;
        if (event.key === "End") nextIndex = elements.tabs.length - 1;
        if (event.key === "ArrowLeft") nextIndex = (currentIndex - 1 + elements.tabs.length) % elements.tabs.length;
        if (event.key === "ArrowRight") nextIndex = (currentIndex + 1) % elements.tabs.length;
        elements.tabs[nextIndex]?.focus();
        elements.tabs[nextIndex]?.click();
    }

    function announce(message) { setText(elements.liveStatus, message); }
    function setText(element, value) { if (element) element.textContent = value; }

    hydrateFromUrl();
    bindEvents();
    renderRecentContents();
    renderSavedContents();
    void loadContents({ historyMode: "replace" });
})();
