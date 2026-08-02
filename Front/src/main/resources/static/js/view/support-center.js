(function () {
    const RECENT_SEARCH_KEY = "grade-stock-support-searches";
    const VALID_VIEWS = ["faq", "notice"];
    const VALID_TOPICS = ["ALL", "SHOPPING", "ORDER", "ACCOUNT", "SERVICE"];
    const VALID_SORTS = ["LATEST", "POPULAR", "OLDEST"];
    const VALID_SIZES = [5, 10, 20];
    const FAQS = [
        {
            id: "product-search",
            topic: "SHOPPING",
            question: "상품명이나 모델번호로 상품을 찾을 수 있나요?",
            answer: "메인 상품 탐색의 검색창에서 상품명, 모델번호, 브랜드를 함께 검색할 수 있습니다. 검색 결과가 없으면 검색어를 짧게 줄여 다시 확인해 주세요.",
            keywords: ["검색", "모델", "브랜드", "상품"]
        },
        {
            id: "stock-status",
            topic: "SHOPPING",
            question: "재고 상태는 어떤 기준으로 표시되나요?",
            answer: "옵션별 재고 합계를 기준으로 재고 안정과 품절 임박 상태를 표시합니다. 정확한 수량은 상품 상세의 사이즈별 재고 영역에서 확인할 수 있습니다.",
            keywords: ["재고", "옵션", "사이즈", "품절"]
        },
        {
            id: "product-compare",
            topic: "SHOPPING",
            question: "여러 상품의 가격과 재고를 비교하려면 어떻게 하나요?",
            answer: "상품 카드의 비교 버튼을 누르면 최대 3개 상품을 MY 쇼핑 활동의 비교 탭에서 가격, 재고, 카테고리 기준으로 확인할 수 있습니다.",
            keywords: ["비교", "가격", "재고", "MY"]
        },
        {
            id: "featured-products",
            topic: "SHOPPING",
            question: "추천과 랭킹 상품은 어디에서 볼 수 있나요?",
            answer: "상단 탐색의 추천 또는 랭킹을 선택하면 독립 컬렉션 화면으로 이동합니다. 각 화면에서 정렬과 페이지 이동을 이용해 더 많은 상품을 볼 수 있습니다.",
            keywords: ["추천", "랭킹", "컬렉션", "더보기"]
        },
        {
            id: "order-lookup",
            topic: "ORDER",
            question: "비회원 주문 상태는 어떻게 조회하나요?",
            answer: "주문 완료 시 발급된 주문번호와 주문자 연락처를 주문 조회 화면에 입력해 주세요. 두 정보가 일치해야 주문 상태와 배송 정보를 볼 수 있습니다.",
            keywords: ["주문", "비회원", "주문번호", "배송"]
        },
        {
            id: "order-number",
            topic: "ORDER",
            question: "주문번호를 잊어버렸을 때 확인할 수 있나요?",
            answer: "같은 브라우저에서 주문을 완료했다면 주문 조회 화면의 최근 주문 불러오기를 사용할 수 있습니다. 브라우저 저장소를 비웠다면 자동 복원할 수 없습니다.",
            keywords: ["주문번호", "최근 주문", "저장소"]
        },
        {
            id: "checkout-stock",
            topic: "ORDER",
            question: "결제 전에 재고가 부족하면 어떻게 되나요?",
            answer: "주문서에서 현재 재고를 다시 확인하며 구매 수량보다 재고가 적은 상품은 주문할 수 없습니다. 장바구니에서 수량을 조정한 뒤 다시 진행해 주세요.",
            keywords: ["결제", "재고", "수량", "장바구니"]
        },
        {
            id: "order-demo",
            topic: "ORDER",
            question: "실제 결제와 배송이 진행되는 서비스인가요?",
            answer: "현재 Grade Stock은 상품 탐색과 주문 흐름을 검증하는 데모 서비스입니다. 실제 결제 승인, 판매 계약, 택배 배송은 제공하지 않습니다.",
            keywords: ["결제", "배송", "데모", "거래"]
        },
        {
            id: "wishlist",
            topic: "ACCOUNT",
            question: "관심 상품은 어디에 저장되나요?",
            answer: "관심 상품은 현재 브라우저의 로컬 저장소에 보관됩니다. MY 쇼핑 활동의 관심 상품 탭에서 검색, 정렬, 일괄 삭제를 할 수 있습니다.",
            keywords: ["관심 상품", "저장", "MY", "로컬"]
        },
        {
            id: "recent-products",
            topic: "ACCOUNT",
            question: "최근 본 상품 기록을 삭제할 수 있나요?",
            answer: "MY 쇼핑 활동의 최근 본 상품 탭에서 개별 상품을 선택해 삭제하거나 현재 탭 전체를 비울 수 있습니다.",
            keywords: ["최근 본 상품", "기록", "삭제", "MY"]
        },
        {
            id: "hidden-products",
            topic: "ACCOUNT",
            question: "숨긴 상품을 다시 목록에 표시하려면 어떻게 하나요?",
            answer: "MY 쇼핑 활동의 숨긴 상품 탭에서 상품을 선택한 뒤 삭제를 누르면 숨김 기록이 해제되어 카탈로그에 다시 표시됩니다.",
            keywords: ["숨긴 상품", "복원", "카탈로그", "MY"]
        },
        {
            id: "browser-storage",
            topic: "SERVICE",
            question: "다른 기기에서도 관심 상품과 비교 목록이 유지되나요?",
            answer: "현재 개인화 정보는 로그인 계정이 아닌 브라우저 저장소를 사용합니다. 다른 기기나 브라우저에는 자동으로 동기화되지 않습니다.",
            keywords: ["동기화", "브라우저", "기기", "저장소"]
        },
        {
            id: "content-notice",
            topic: "SERVICE",
            question: "서비스 공지와 업데이트 소식은 어디에서 확인하나요?",
            answer: "이 화면의 공지사항 탭 또는 에디토리얼 화면에서 공개된 운영 공지를 확인할 수 있습니다. 조회순과 최신순 정렬도 지원합니다.",
            keywords: ["공지", "업데이트", "에디토리얼", "운영"]
        },
        {
            id: "accessibility",
            topic: "SERVICE",
            question: "키보드만으로 고객지원 화면을 사용할 수 있나요?",
            answer: "Tab 키로 모든 컨트롤을 이동할 수 있으며 슬래시 키로 검색창에 바로 접근합니다. Escape 키는 검색어를 지우고 FAQ의 좌우 방향키는 주제를 이동합니다.",
            keywords: ["키보드", "접근성", "단축키", "검색"]
        }
    ];

    const state = {
        view: "faq",
        topic: "ALL",
        keyword: "",
        noticePage: 0,
        noticeSize: 10,
        noticeSort: "LATEST",
        expandedFaqIds: new Set()
    };
    let noticeController = null;
    let noticeSequence = 0;
    let toastTimer = null;

    const elements = {
        liveStatus: document.getElementById("supportLiveStatus"),
        searchForm: document.getElementById("supportSearchForm"),
        keyword: document.getElementById("supportKeyword"),
        searchClear: document.getElementById("supportSearchClearButton"),
        suggestedKeywords: Array.from(document.querySelectorAll("[data-support-keyword]")),
        recent: document.getElementById("supportRecentSearches"),
        recentList: document.getElementById("supportRecentSearchList"),
        recentClear: document.getElementById("supportRecentClearButton"),
        topics: Array.from(document.querySelectorAll("[data-support-topic]")),
        views: Array.from(document.querySelectorAll("[data-support-view]")),
        faqPanel: document.getElementById("supportFaqPanel"),
        noticePanel: document.getElementById("supportNoticePanel"),
        faqList: document.getElementById("supportFaqList"),
        faqEmpty: document.getElementById("supportFaqEmpty"),
        faqResult: document.getElementById("supportFaqResultText"),
        faqCount: document.getElementById("supportFaqCount"),
        noticeCount: document.getElementById("supportNoticeCount"),
        appliedFilters: document.getElementById("supportAppliedFilters"),
        expandAll: document.getElementById("supportExpandAllButton"),
        reset: document.getElementById("supportResetButton"),
        emptyReset: document.getElementById("supportFaqEmptyResetButton"),
        noticeResult: document.getElementById("supportNoticeResultText"),
        noticeSort: document.getElementById("supportNoticeSort"),
        noticeSize: document.getElementById("supportNoticeSize"),
        noticeList: document.getElementById("supportNoticeList"),
        noticePagination: document.getElementById("supportNoticePagination"),
        noticePrevious: document.getElementById("supportNoticePreviousButton"),
        noticeNext: document.getElementById("supportNoticeNextButton"),
        noticePageSelect: document.getElementById("supportNoticePageSelect"),
        noticePageText: document.getElementById("supportNoticePageText"),
        copySummary: document.getElementById("supportCopySummaryButton"),
        topButton: document.getElementById("supportTopButton"),
        toast: document.getElementById("supportToast")
    };

    function hydrateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        const view = String(params.get("view") || "faq").toLowerCase();
        const topic = String(params.get("topic") || "ALL").toUpperCase();
        const sort = String(params.get("sort") || "LATEST").toUpperCase();
        const size = Number(params.get("size") || 10);
        const page = Number(params.get("page") || 0);
        state.view = VALID_VIEWS.includes(view) ? view : "faq";
        state.topic = VALID_TOPICS.includes(topic) ? topic : "ALL";
        state.keyword = normalizeSearchKeyword(params.get("keyword"));
        state.noticeSort = VALID_SORTS.includes(sort) ? sort : "LATEST";
        state.noticeSize = VALID_SIZES.includes(size) ? size : 10;
        state.noticePage = Number.isInteger(page) && page >= 0 ? page : 0;
    }

    function updateUrl(mode = "push") {
        const params = new URLSearchParams();
        if (state.view !== "faq") params.set("view", state.view);
        if (state.keyword) params.set("keyword", state.keyword);
        if (state.view === "faq" && state.topic !== "ALL") params.set("topic", state.topic);
        if (state.view === "notice" && state.noticeSort !== "LATEST") params.set("sort", state.noticeSort);
        if (state.view === "notice" && state.noticeSize !== 10) params.set("size", String(state.noticeSize));
        if (state.view === "notice" && state.noticePage > 0) params.set("page", String(state.noticePage));
        const query = params.toString();
        const nextUrl = query ? `${window.location.pathname}?${query}` : window.location.pathname;
        if (`${window.location.pathname}${window.location.search}` === nextUrl) return;
        window.history[mode === "replace" ? "replaceState" : "pushState"](null, "", nextUrl);
    }

    function render() {
        syncControls();
        renderFaqs();
        renderRecentSearches();
        if (state.view === "notice") void loadNotices();
        updateDocumentTitle();
    }

    function syncControls() {
        elements.keyword.value = state.keyword;
        elements.searchClear.hidden = !elements.keyword.value;
        elements.noticeSort.value = state.noticeSort;
        elements.noticeSize.value = String(state.noticeSize);
        elements.faqPanel.hidden = state.view !== "faq";
        elements.noticePanel.hidden = state.view !== "notice";
        elements.views.forEach((button) => {
            const active = button.dataset.supportView === state.view;
            button.setAttribute("aria-selected", String(active));
            button.tabIndex = active ? 0 : -1;
        });
        elements.topics.forEach((button) => {
            const active = button.dataset.supportTopic === state.topic;
            button.classList.toggle("is-active", active);
            button.setAttribute("aria-pressed", String(active));
        });
    }

    function renderFaqs() {
        const query = normalize(state.keyword);
        const filtered = FAQS.filter((faq) => {
            const matchesTopic = state.topic === "ALL" || faq.topic === state.topic;
            const haystack = normalize([faq.question, faq.answer, ...faq.keywords].join(" "));
            return matchesTopic && (!query || haystack.includes(query));
        });
        elements.faqList.replaceChildren(...filtered.map(createFaqItem));
        elements.faqList.hidden = filtered.length === 0;
        elements.faqEmpty.hidden = filtered.length > 0;
        elements.faqCount.textContent = String(filtered.length);
        elements.faqResult.textContent = `${filtered.length}개의 도움말이 현재 조건에 맞습니다.`;
        elements.expandAll.disabled = filtered.length === 0;
        const allExpanded = filtered.length > 0 && filtered.every((faq) => state.expandedFaqIds.has(faq.id));
        elements.expandAll.textContent = allExpanded ? "모두 접기" : "모두 펼치기";
        renderAppliedFilters();
        announce(`${filtered.length}개의 자주 묻는 질문을 표시했습니다.`);
    }

    function createFaqItem(faq, index) {
        const article = document.createElement("article");
        article.className = "support-faq";
        article.dataset.faqId = faq.id;
        const heading = document.createElement("h3");
        const button = document.createElement("button");
        button.type = "button";
        button.id = `supportFaqButton-${faq.id}`;
        button.setAttribute("aria-expanded", String(state.expandedFaqIds.has(faq.id)));
        button.setAttribute("aria-controls", `supportFaqAnswer-${faq.id}`);
        const number = document.createElement("span");
        number.textContent = String(index + 1).padStart(2, "0");
        const question = document.createElement("strong");
        question.textContent = faq.question;
        const topic = document.createElement("em");
        topic.textContent = topicLabel(faq.topic);
        const icon = document.createElement("i");
        icon.setAttribute("aria-hidden", "true");
        button.append(number, question, topic, icon);
        heading.appendChild(button);
        const answer = document.createElement("div");
        answer.className = "support-faq__answer";
        answer.id = `supportFaqAnswer-${faq.id}`;
        answer.setAttribute("role", "region");
        answer.setAttribute("aria-labelledby", button.id);
        answer.hidden = !state.expandedFaqIds.has(faq.id);
        const text = document.createElement("p");
        text.textContent = faq.answer;
        const actions = document.createElement("div");
        const copy = document.createElement("button");
        copy.type = "button";
        copy.textContent = "답변 복사";
        copy.addEventListener("click", () => copyText(`${faq.question}\n${faq.answer}`, "답변을 복사했습니다."));
        const link = document.createElement("button");
        link.type = "button";
        link.textContent = "링크 복사";
        link.addEventListener("click", () => copyFaqLink(faq));
        actions.append(copy, link);
        answer.append(text, actions);
        button.addEventListener("click", () => toggleFaq(faq.id));
        article.append(heading, answer);
        return article;
    }

    function toggleFaq(id) {
        if (state.expandedFaqIds.has(id)) {
            state.expandedFaqIds.delete(id);
        } else {
            state.expandedFaqIds.add(id);
        }
        renderFaqs();
        document.getElementById(`supportFaqButton-${id}`)?.focus();
    }

    function toggleAllFaqs() {
        const visibleIds = Array.from(elements.faqList.querySelectorAll("[data-faq-id]"))
            .map((item) => item.dataset.faqId);
        const allExpanded = visibleIds.every((id) => state.expandedFaqIds.has(id));
        visibleIds.forEach((id) => allExpanded ? state.expandedFaqIds.delete(id) : state.expandedFaqIds.add(id));
        renderFaqs();
    }

    async function loadNotices(options = {}) {
        const sequence = ++noticeSequence;
        noticeController?.abort();
        noticeController = new AbortController();
        showNoticeState("LOADING");
        if (options.updateUrl !== false) updateUrl(options.historyMode);
        const params = new URLSearchParams({
            boardType: "NOTICE",
            page: String(state.noticePage),
            size: String(state.noticeSize),
            sort: state.noticeSort
        });
        if (state.keyword) params.set("keyword", state.keyword);
        try {
            const response = await fetch(`/api/front/content?${params}`, { signal: noticeController.signal });
            if (!response.ok) throw new Error("공지사항을 불러오지 못했습니다.");
            const payload = normalizeNoticeResponse(await response.json());
            if (sequence !== noticeSequence) return;
            const totalPages = payload.totalPages;
            if (totalPages > 0 && state.noticePage >= totalPages) {
                state.noticePage = totalPages - 1;
                void loadNotices({ historyMode: "replace" });
                return;
            }
            renderNotices(payload);
        } catch (error) {
            if (error?.name === "AbortError" || sequence !== noticeSequence) return;
            showNoticeState("ERROR");
        } finally {
            if (sequence === noticeSequence) noticeController = null;
        }
    }

    function renderNotices(payload) {
        const items = payload.items;
        elements.noticeList.replaceChildren();
        elements.noticeList.classList.remove("is-loading", "is-error");
        elements.noticeList.setAttribute("aria-busy", "false");
        if (items.length === 0) {
            const stateElement = document.createElement("div");
            stateElement.className = "support-notice-state";
            stateElement.textContent = "검색 조건에 맞는 공지사항이 없습니다.";
            elements.noticeList.appendChild(stateElement);
        } else {
            items.forEach((item, index) => elements.noticeList.appendChild(createNotice(item, index, payload.page)));
        }
        const totalElements = payload.totalElements;
        const totalPages = Math.max(1, payload.totalPages);
        const currentPage = payload.page;
        elements.noticeCount.textContent = totalElements.toLocaleString("ko-KR");
        elements.noticeResult.textContent = `${totalElements.toLocaleString("ko-KR")}개의 공개 공지가 있습니다.`;
        elements.noticePageText.textContent = `${currentPage + 1} / ${totalPages} 페이지`;
        elements.noticePrevious.disabled = Boolean(payload.first);
        elements.noticeNext.disabled = payload.last || payload.totalPages === 0;
        elements.noticePagination.hidden = payload.totalPages <= 1;
        renderNoticePageOptions(totalPages, currentPage);
        announce(`${items.length}개의 공지사항을 표시했습니다.`);
    }

    function createNotice(item, index, page) {
        const article = document.createElement("article");
        article.className = "support-notice";
        const link = document.createElement("a");
        link.href = `/front/content/${item.id}`;
        const number = document.createElement("span");
        number.textContent = item.pinned ? "PIN" : String(page * state.noticeSize + index + 1).padStart(2, "0");
        if (item.pinned) number.className = "is-pinned";
        const body = document.createElement("div");
        const meta = document.createElement("p");
        meta.textContent = `${item.createdDate} · 조회 ${item.viewCount.toLocaleString("ko-KR")}`;
        const title = document.createElement("h3");
        title.textContent = item.title;
        const summary = document.createElement("p");
        summary.textContent = item.summary || "내용을 확인해 주세요.";
        body.append(meta, title, summary);
        const arrow = document.createElement("em");
        arrow.textContent = "보기 →";
        link.append(number, body, arrow);
        article.appendChild(link);
        return article;
    }

    function showNoticeState(name) {
        const error = name === "ERROR";
        const stateElement = document.createElement("div");
        stateElement.className = "support-notice-state";
        const text = document.createElement("p");
        text.textContent = error ? "공지사항을 불러오지 못했습니다." : "공지사항을 불러오는 중입니다.";
        stateElement.appendChild(text);
        if (error) {
            const retry = document.createElement("button");
            retry.type = "button";
            retry.textContent = "다시 불러오기";
            retry.addEventListener("click", () => loadNotices({ updateUrl: false }));
            stateElement.appendChild(retry);
        }
        elements.noticeList.replaceChildren(stateElement);
        elements.noticeList.classList.toggle("is-loading", !error);
        elements.noticeList.classList.toggle("is-error", error);
        elements.noticeList.setAttribute("aria-busy", String(!error));
        elements.noticePagination.hidden = true;
        announce(error ? "공지사항 조회에 실패했습니다." : "공지사항을 불러오는 중입니다.");
    }

    function renderNoticePageOptions(totalPages, currentPage) {
        elements.noticePageSelect.replaceChildren();
        const visiblePages = compactPageIndexes(totalPages, currentPage);
        visiblePages.forEach((page, index) => {
            const previousPage = visiblePages[index - 1];
            if (previousPage !== undefined && page - previousPage > 1) {
                const separator = document.createElement("option");
                separator.disabled = true;
                separator.textContent = "…";
                elements.noticePageSelect.appendChild(separator);
            }
            const option = document.createElement("option");
            option.value = String(page);
            option.textContent = `${page + 1} 페이지`;
            option.selected = page === currentPage;
            elements.noticePageSelect.appendChild(option);
        });
        elements.noticePageSelect.setAttribute("aria-label", `공지 페이지 선택, 전체 ${totalPages}페이지`);
    }

    function compactPageIndexes(totalPages, currentPage) {
        const indexes = new Set([0, totalPages - 1]);
        for (let page = currentPage - 2; page <= currentPage + 2; page += 1) {
            if (page >= 0 && page < totalPages) indexes.add(page);
        }
        return Array.from(indexes).sort((left, right) => left - right);
    }

    function requiredText(value, maxLength, fieldName) {
        const normalized = String(value ?? "").trim();
        if (!normalized || normalized.length > maxLength) throw new Error(`${fieldName} 정보가 올바르지 않습니다.`);
        return normalized;
    }

    function optionalText(value, maxLength) {
        const normalized = String(value ?? "").trim();
        return normalized.length <= maxLength ? normalized : "";
    }

    function safeInteger(value, fieldName, minimum = 0) {
        if (!Number.isSafeInteger(value) || value < minimum) throw new Error(`${fieldName} 정보가 올바르지 않습니다.`);
        return value;
    }

    function normalizeNoticeResponse(source) {
        if (!source || typeof source !== "object" || Array.isArray(source) || !Array.isArray(source.items)) {
            throw new Error("공지사항 응답이 올바르지 않습니다.");
        }
        const page = safeInteger(source.page, "현재 페이지");
        const size = safeInteger(source.size, "페이지 크기", 1);
        const totalElements = safeInteger(source.totalElements, "전체 공지 수");
        const totalPages = safeInteger(source.totalPages, "전체 페이지 수");
        if (page !== state.noticePage || size !== state.noticeSize || source.sort !== state.noticeSort
            || totalPages !== (totalElements === 0 ? 0 : Math.ceil(totalElements / size))
            || source.items.length > size || Boolean(source.first) !== (page === 0)
            || Boolean(source.last) !== (totalPages === 0 || page >= totalPages - 1)) {
            throw new Error("공지사항 페이지 정보가 올바르지 않습니다.");
        }
        const ids = new Set();
        const items = source.items.map((item) => {
            const id = safeInteger(item?.id, "공지 번호", 1);
            if (ids.has(id) || item?.boardType !== "NOTICE" || typeof item.pinned !== "boolean") {
                throw new Error("공지사항 항목이 올바르지 않습니다.");
            }
            ids.add(id);
            return {
                id,
                title: requiredText(item.title, 200, "공지 제목"),
                summary: optionalText(item.summary, 500),
                viewCount: safeInteger(item.viewCount, "조회 수"),
                pinned: item.pinned,
                createdDate: requiredText(item.createdDate, 30, "등록일")
            };
        });
        const pageViewCount = items.reduce((sum, item) => sum + item.viewCount, 0);
        const pagePinnedCount = items.filter((item) => item.pinned).length;
        if (safeInteger(source.pageViewCount, "페이지 조회 수") !== pageViewCount
            || safeInteger(source.pagePinnedCount, "고정 공지 수") !== pagePinnedCount
            || safeInteger(source.pageNoticeCount, "공지 수") !== items.length
            || safeInteger(source.pageStyleCount, "스타일 수") !== 0) {
            throw new Error("공지사항 집계 정보가 올바르지 않습니다.");
        }
        return {
            items, page, size, totalElements, totalPages,
            first: page === 0,
            last: totalPages === 0 || page >= totalPages - 1,
            sort: source.sort
        };
    }

    function renderAppliedFilters() {
        elements.appliedFilters.replaceChildren();
        if (state.topic !== "ALL") elements.appliedFilters.appendChild(createFilterChip(topicLabel(state.topic), () => setTopic("ALL")));
        if (state.keyword) elements.appliedFilters.appendChild(createFilterChip(`검색 · ${state.keyword}`, clearSearch));
        if (!elements.appliedFilters.childElementCount) {
            const text = document.createElement("span");
            text.textContent = "전체 도움말";
            elements.appliedFilters.appendChild(text);
        }
    }

    function createFilterChip(label, onRemove) {
        const button = document.createElement("button");
        button.type = "button";
        button.textContent = `${label} ×`;
        button.addEventListener("click", onRemove);
        return button;
    }

    function setView(view, options = {}) {
        if (!VALID_VIEWS.includes(view)) return;
        state.view = view;
        state.noticePage = 0;
        syncControls();
        if (options.updateUrl !== false) updateUrl();
        if (view === "notice") void loadNotices({ updateUrl: false });
        updateDocumentTitle();
        document.querySelector(`[data-support-view="${view}"]`)?.focus();
    }

    function setTopic(topic) {
        if (!VALID_TOPICS.includes(topic)) return;
        state.topic = topic;
        state.view = "faq";
        syncControls();
        renderFaqs();
        updateUrl();
    }

    function submitSearch(keyword) {
        state.keyword = normalizeSearchKeyword(keyword ?? elements.keyword.value);
        state.noticePage = 0;
        elements.keyword.value = state.keyword;
        if (state.keyword) rememberSearch(state.keyword);
        syncControls();
        renderFaqs();
        renderRecentSearches();
        updateUrl();
        if (state.view === "notice") void loadNotices({ updateUrl: false });
        document.getElementById(state.view === "faq" ? "supportFaqHeading" : "supportNoticeHeading")?.focus?.();
    }

    function clearSearch() {
        state.keyword = "";
        state.noticePage = 0;
        elements.keyword.value = "";
        syncControls();
        renderFaqs();
        updateUrl();
        if (state.view === "notice") void loadNotices({ updateUrl: false });
    }

    function resetSearch() {
        state.keyword = "";
        state.topic = "ALL";
        state.noticePage = 0;
        state.expandedFaqIds.clear();
        syncControls();
        renderFaqs();
        updateUrl();
        if (state.view === "notice") void loadNotices({ updateUrl: false });
    }

    function rememberSearch(keyword) {
        const searches = readRecentSearches().filter((item) => item !== keyword);
        searches.unshift(keyword);
        writeRecentSearches(searches.slice(0, 6));
    }

    function readRecentSearches() {
        try {
            const parsed = JSON.parse(window.localStorage.getItem(RECENT_SEARCH_KEY) || "[]");
            return Array.isArray(parsed) ? Array.from(new Set(parsed
                .filter((item) => typeof item === "string")
                .map(normalizeSearchKeyword)
                .filter(Boolean))).slice(0, 6) : [];
        } catch (error) {
            return [];
        }
    }

    function writeRecentSearches(searches) {
        try {
            const normalized = Array.from(new Set((Array.isArray(searches) ? searches : [])
                .filter((item) => typeof item === "string")
                .map(normalizeSearchKeyword)
                .filter(Boolean))).slice(0, 6);
            window.localStorage.setItem(RECENT_SEARCH_KEY, JSON.stringify(normalized));
        } catch (error) {
            // 저장소가 제한되어도 현재 검색 기능은 유지한다.
        }
    }

    function renderRecentSearches() {
        const searches = readRecentSearches();
        elements.recent.hidden = searches.length === 0;
        elements.recentList.replaceChildren(...searches.map((keyword) => {
            const button = document.createElement("button");
            button.type = "button";
            button.textContent = keyword;
            button.addEventListener("click", () => submitSearch(keyword));
            return button;
        }));
    }

    function copyFaqLink(faq) {
        const url = new URL(window.location.href);
        url.search = "";
        url.searchParams.set("view", "faq");
        url.searchParams.set("keyword", faq.question);
        copyText(url.toString(), "도움말 링크를 복사했습니다.");
    }

    function copyCurrentSummary() {
        const topic = state.topic === "ALL" ? "전체" : topicLabel(state.topic);
        const keyword = state.keyword || "없음";
        const text = `Grade Stock 고객지원 문의\n화면: ${state.view === "faq" ? "자주 묻는 질문" : "공지사항"}\n주제: ${topic}\n검색어: ${keyword}\n경로: ${window.location.href}`;
        copyText(text, "현재 문의 내용을 복사했습니다.");
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

    function showToast(message) {
        window.clearTimeout(toastTimer);
        elements.toast.textContent = message;
        elements.toast.hidden = false;
        toastTimer = window.setTimeout(() => {
            elements.toast.hidden = true;
        }, 2200);
    }

    function handleViewKeydown(event) {
        if (!["ArrowLeft", "ArrowRight"].includes(event.key)) return;
        event.preventDefault();
        const currentIndex = elements.views.indexOf(event.currentTarget);
        const offset = event.key === "ArrowRight" ? 1 : -1;
        const nextIndex = (currentIndex + offset + elements.views.length) % elements.views.length;
        setView(elements.views[nextIndex].dataset.supportView);
    }

    function handleTopicKeydown(event) {
        if (!["ArrowUp", "ArrowDown"].includes(event.key)) return;
        event.preventDefault();
        const currentIndex = elements.topics.indexOf(event.currentTarget);
        const offset = event.key === "ArrowDown" ? 1 : -1;
        const nextIndex = (currentIndex + offset + elements.topics.length) % elements.topics.length;
        elements.topics[nextIndex].focus();
    }

    function bindEvents() {
        elements.searchForm.addEventListener("submit", (event) => {
            event.preventDefault();
            submitSearch();
        });
        elements.keyword.addEventListener("input", () => {
            elements.searchClear.hidden = !elements.keyword.value;
        });
        elements.searchClear.addEventListener("click", () => {
            clearSearch();
            elements.keyword.focus();
        });
        elements.suggestedKeywords.forEach((button) => button.addEventListener("click", () => submitSearch(button.dataset.supportKeyword)));
        elements.recentClear.addEventListener("click", () => {
            writeRecentSearches([]);
            renderRecentSearches();
            showToast("최근 검색을 삭제했습니다.");
        });
        elements.topics.forEach((button) => {
            button.addEventListener("click", () => setTopic(button.dataset.supportTopic));
            button.addEventListener("keydown", handleTopicKeydown);
        });
        elements.views.forEach((button) => {
            button.addEventListener("click", () => setView(button.dataset.supportView));
            button.addEventListener("keydown", handleViewKeydown);
        });
        elements.expandAll.addEventListener("click", toggleAllFaqs);
        elements.reset.addEventListener("click", resetSearch);
        elements.emptyReset.addEventListener("click", resetSearch);
        elements.noticeSort.addEventListener("change", () => {
            state.noticeSort = VALID_SORTS.includes(elements.noticeSort.value) ? elements.noticeSort.value : "LATEST";
            state.noticePage = 0;
            void loadNotices();
        });
        elements.noticeSize.addEventListener("change", () => {
            const size = Number(elements.noticeSize.value);
            state.noticeSize = VALID_SIZES.includes(size) ? size : 10;
            state.noticePage = 0;
            void loadNotices();
        });
        elements.noticePrevious.addEventListener("click", () => changeNoticePage(-1));
        elements.noticeNext.addEventListener("click", () => changeNoticePage(1));
        elements.noticePageSelect.addEventListener("change", () => {
            state.noticePage = Math.max(0, Number(elements.noticePageSelect.value) || 0);
            void loadNotices();
        });
        elements.copySummary.addEventListener("click", copyCurrentSummary);
        elements.topButton.addEventListener("click", () => window.scrollTo({ top: 0, behavior: "smooth" }));
        window.addEventListener("scroll", () => {
            elements.topButton.hidden = window.scrollY < 640;
        }, { passive: true });
        window.addEventListener("popstate", () => {
            hydrateFromUrl();
            render();
        });
        window.addEventListener("storage", (event) => {
            if (event.key === RECENT_SEARCH_KEY) renderRecentSearches();
        });
        document.addEventListener("keydown", (event) => {
            if (event.key === "/" && !isTypingTarget(event.target)) {
                event.preventDefault();
                elements.keyword.focus();
            }
            if (event.key === "Escape" && document.activeElement === elements.keyword && elements.keyword.value) {
                clearSearch();
            }
        });
    }

    function changeNoticePage(offset) {
        const nextPage = state.noticePage + offset;
        if (nextPage < 0) return;
        state.noticePage = nextPage;
        void loadNotices();
        document.getElementById("supportNoticeHeading")?.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    function updateTopicCounts() {
        document.getElementById("supportTopicAllCount").textContent = String(FAQS.length);
        ["SHOPPING", "ORDER", "ACCOUNT", "SERVICE"].forEach((topic) => {
            const id = `supportTopic${topic.charAt(0)}${topic.slice(1).toLowerCase()}Count`;
            document.getElementById(id).textContent = String(FAQS.filter((faq) => faq.topic === topic).length);
        });
    }

    function updateDocumentTitle() {
        const label = state.view === "faq" ? "자주 묻는 질문" : "공지사항";
        document.title = `${label} | Grade Stock 고객지원`;
    }

    function topicLabel(topic) {
        return { SHOPPING: "상품 탐색", ORDER: "주문·결제", ACCOUNT: "관심·기록", SERVICE: "서비스 이용" }[topic] || "전체";
    }

    function normalize(value) {
        return String(value || "").trim().toLocaleLowerCase("ko-KR").replace(/\s+/g, " ");
    }

    function normalizeSearchKeyword(value) {
        return String(value ?? "").replace(/[\u0000-\u001f\u007f]/g, " ").trim().replace(/\s+/g, " ").slice(0, 100);
    }

    function announce(message) {
        elements.liveStatus.textContent = "";
        window.requestAnimationFrame(() => {
            elements.liveStatus.textContent = message;
        });
    }

    function isTypingTarget(target) {
        return target instanceof HTMLElement && (target.matches("input, textarea, select") || target.isContentEditable);
    }

    hydrateFromUrl();
    updateTopicCounts();
    bindEvents();
    render();
}());
