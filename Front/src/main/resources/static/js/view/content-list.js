(function () {
    const DEFAULT_STATE = { boardType: "ALL", keyword: "", page: 0, size: 8 };
    const state = { ...DEFAULT_STATE };
    let requestController = null;
    let requestSequence = 0;

    const elements = {
        liveStatus: document.getElementById("contentListLiveStatus"),
        tabs: Array.from(document.querySelectorAll("[data-content-board]")),
        searchForm: document.getElementById("contentListSearchForm"),
        keyword: document.getElementById("contentListKeyword"),
        clearButton: document.getElementById("contentListClearButton"),
        heading: document.getElementById("contentListHeading"),
        resultText: document.getElementById("contentListResultText"),
        grid: document.getElementById("contentListGrid"),
        pagination: document.getElementById("contentListPagination"),
        previousButton: document.getElementById("contentListPreviousButton"),
        nextButton: document.getElementById("contentListNextButton"),
        pageText: document.getElementById("contentListPageText")
    };

    function hydrateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        const boardType = String(params.get("boardType") || "ALL").toUpperCase();
        state.boardType = ["NOTICE", "STYLE"].includes(boardType) ? boardType : "ALL";
        state.keyword = String(params.get("keyword") || "").trim().slice(0, 100);
        const page = Number(params.get("page") || 0);
        state.page = Number.isInteger(page) && page >= 0 ? page : 0;
    }

    async function loadContents(options = {}) {
        const sequence = ++requestSequence;
        requestController?.abort();
        requestController = new AbortController();
        syncControls();
        showState("LOADING");
        if (options.updateUrl !== false) updateUrl();
        try {
            const params = new URLSearchParams({ page: String(state.page), size: String(state.size) });
            if (state.boardType !== "ALL") params.set("boardType", state.boardType);
            if (state.keyword) params.set("keyword", state.keyword);
            const response = await fetch(`/api/front/content?${params}`, { signal: requestController.signal });
            if (!response.ok) throw new Error("콘텐츠를 불러오지 못했습니다.");
            const payload = await response.json();
            if (sequence !== requestSequence) return;
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
            elements.grid.appendChild(createState("검색 조건에 맞는 공개 콘텐츠가 없습니다.", false));
        } else {
            items.forEach((item, index) => elements.grid.appendChild(createCard(item, index, payload.page)));
        }
        const boardLabel = state.boardType === "NOTICE" ? "공지" : state.boardType === "STYLE" ? "스타일" : "전체 콘텐츠";
        setText(elements.heading, boardLabel);
        setText(elements.resultText, `${Number(payload.totalElements || 0).toLocaleString("ko-KR")}개의 공개 콘텐츠가 있습니다.`);
        const totalPages = Math.max(1, Number(payload.totalPages || 0));
        setText(elements.pageText, `${Number(payload.page || 0) + 1} / ${totalPages}`);
        elements.previousButton.disabled = Boolean(payload.first);
        elements.nextButton.disabled = Boolean(payload.last) || Number(payload.totalPages || 0) === 0;
        elements.pagination.hidden = Number(payload.totalPages || 0) <= 1;
        announce(`${items.length}개의 콘텐츠를 표시했습니다.`);
    }

    function createCard(item, index, page) {
        const link = document.createElement("a");
        link.className = `content-list-card content-list-card--${item.boardType === "STYLE" ? "style" : "notice"}`;
        link.href = `/front/content/${Number(item.id)}`;
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

    function syncControls() {
        elements.keyword.value = state.keyword;
        elements.clearButton.hidden = !state.keyword;
        elements.tabs.forEach((tab) => {
            const active = tab.dataset.contentBoard === state.boardType;
            tab.setAttribute("aria-selected", String(active));
        });
    }

    function updateUrl() {
        const params = new URLSearchParams();
        if (state.boardType !== "ALL") params.set("boardType", state.boardType);
        if (state.keyword) params.set("keyword", state.keyword);
        if (state.page > 0) params.set("page", String(state.page));
        const query = params.toString();
        window.history.replaceState(null, "", query ? `${window.location.pathname}?${query}` : window.location.pathname);
    }

    function bindEvents() {
        elements.tabs.forEach((tab) => tab.addEventListener("click", () => {
            state.boardType = tab.dataset.contentBoard;
            state.page = 0;
            void loadContents();
        }));
        elements.searchForm.addEventListener("submit", (event) => {
            event.preventDefault();
            state.keyword = elements.keyword.value.trim();
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
        window.addEventListener("popstate", () => {
            hydrateFromUrl();
            void loadContents({ updateUrl: false });
        });
    }

    function changePage(direction) {
        state.page = Math.max(0, state.page + direction);
        void loadContents();
        document.getElementById("contentListHeading")?.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    function announce(message) { setText(elements.liveStatus, message); }
    function setText(element, value) { if (element) element.textContent = value; }

    hydrateFromUrl();
    bindEvents();
    void loadContents({ updateUrl: false });
})();
