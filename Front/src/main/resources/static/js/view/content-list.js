(function () {
    const DEFAULT_STATE = { boardType: "ALL", keyword: "", page: 0, size: 8, sort: "LATEST" };
    const VALID_SIZES = [4, 8, 12, 20];
    const VALID_SORTS = ["LATEST", "POPULAR", "OLDEST"];
    const RECENT_CONTENT_KEY = "front-recent-content";
    const CONTENT_RETURN_URL_KEY = "front-content-return-url";
    const state = { ...DEFAULT_STATE };
    let requestController = null;
    let requestSequence = 0;

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
        recentClearButton: document.getElementById("contentRecentClearButton")
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
        const link = document.createElement("a");
        link.className = `content-list-card content-list-card--${item.boardType === "STYLE" ? "style" : "notice"}`;
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
        return link;
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
        elements.recentClearButton.addEventListener("click", clearRecentContents);
    }

    function renderRecentContents() {
        const items = readRecentContents();
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
            date.textContent = recentTimeLabel(item.viewedAt);
            link.append(type, title, date);
            elements.recentGrid.appendChild(link);
        });
    }

    function readRecentContents() {
        try {
            const value = JSON.parse(window.localStorage.getItem(RECENT_CONTENT_KEY) || "[]");
            return Array.isArray(value) ? value.slice(0, 6) : [];
        } catch (error) {
            return [];
        }
    }

    function clearRecentContents() {
        try {
            window.localStorage.removeItem(RECENT_CONTENT_KEY);
        } catch (error) {
            // Storage can be unavailable in private browsing mode.
        }
        renderRecentContents();
        announce("최근 읽은 콘텐츠를 비웠습니다.");
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
    void loadContents({ historyMode: "replace" });
})();
