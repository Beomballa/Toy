(function () {
    const documentId = Number(document.body.dataset.documentId || 0);
    const CONTENT_VISITOR_KEY = "front-content-visitor-key";
    const RECENT_CONTENT_KEY = "front-recent-content";
    const BOOKMARKED_CONTENT_KEY = "front-bookmarked-content";
    const READING_PROGRESS_KEY = "front-content-reading-progress";
    const FONT_SCALE_KEY = "front-content-font-scale";
    const CONTENT_RETURN_URL_KEY = "front-content-return-url";
    const MIN_FONT_SCALE = 0.9;
    const MAX_FONT_SCALE = 1.2;
    const FONT_SCALE_STEP = 0.1;
    let currentContent = null;
    let fontScale = 1;
    let lastSavedProgress = -1;
    let scrollFrame = null;
    let reactionRequestInFlight = false;
    let memoryVisitorKey = null;
    let detailRequestController = null;
    let detailRequestSequence = 0;
    let reactionRequestSequence = 0;

    const elements = {
        article: document.getElementById("contentDetailArticle"),
        status: document.getElementById("contentDetailStatus"),
        type: document.getElementById("contentDetailType"),
        pinned: document.getElementById("contentDetailPinned"),
        title: document.getElementById("contentDetailTitle"),
        date: document.getElementById("contentDetailDate"),
        views: document.getElementById("contentDetailViews"),
        readingTime: document.getElementById("contentDetailReadingTime"),
        characterCount: document.getElementById("contentDetailCharacterCount"),
        body: document.getElementById("contentDetailBody"),
        breadcrumb: document.getElementById("contentDetailBreadcrumb"),
        error: document.getElementById("contentDetailError"),
        errorMessage: document.getElementById("contentDetailErrorMessage"),
        retryButton: document.getElementById("contentDetailRetryButton"),
        copyButton: document.getElementById("contentDetailCopyButton"),
        shareButton: document.getElementById("contentDetailShareButton"),
        bookmarkButton: document.getElementById("contentDetailBookmarkButton"),
        fontDecreaseButton: document.getElementById("contentDetailFontDecreaseButton"),
        fontIncreaseButton: document.getElementById("contentDetailFontIncreaseButton"),
        fontResetButton: document.getElementById("contentDetailFontResetButton"),
        fontScaleLabel: document.getElementById("contentDetailFontScaleLabel"),
        readerProgress: document.getElementById("contentDetailReaderProgress"),
        progressText: document.getElementById("contentDetailProgressText"),
        resume: document.getElementById("contentDetailResume"),
        resumeText: document.getElementById("contentDetailResumeText"),
        resumeButton: document.getElementById("contentDetailResumeButton"),
        resumeDismissButton: document.getElementById("contentDetailResumeDismissButton"),
        navigation: document.getElementById("contentDetailNavigation"),
        newerLink: document.getElementById("contentDetailNewerLink"),
        newerTitle: document.getElementById("contentDetailNewerTitle"),
        newerDate: document.getElementById("contentDetailNewerDate"),
        olderLink: document.getElementById("contentDetailOlderLink"),
        olderTitle: document.getElementById("contentDetailOlderTitle"),
        olderDate: document.getElementById("contentDetailOlderDate"),
        related: document.getElementById("contentDetailRelated"),
        relatedGrid: document.getElementById("contentDetailRelatedGrid"),
        scrollProgress: document.getElementById("contentDetailScrollProgress"),
        reaction: document.getElementById("contentDetailReaction"),
        reactionSummary: document.getElementById("contentDetailReactionSummary"),
        reactionStatus: document.getElementById("contentDetailReactionStatus"),
        reactionMeter: document.getElementById("contentDetailReactionMeter"),
        helpfulButton: document.getElementById("contentDetailHelpfulButton"),
        notHelpfulButton: document.getElementById("contentDetailNotHelpfulButton"),
        helpfulCount: document.getElementById("contentDetailHelpfulCount"),
        notHelpfulCount: document.getElementById("contentDetailNotHelpfulCount"),
        reactionRetryButton: document.getElementById("contentDetailReactionRetryButton")
    };

    async function loadContentDetail() {
        detailRequestController?.abort();
        detailRequestController = new AbortController();
        const requestController = detailRequestController;
        const requestSequence = ++detailRequestSequence;
        showLoading();
        try {
            if (!Number.isInteger(documentId) || documentId <= 0) {
                throw new Error("올바르지 않은 콘텐츠 주소입니다.");
            }
            const response = await fetch(`/api/front/content/${documentId}`, { signal: requestController.signal });
            if (!response.ok) {
                throw new Error(response.status === 404
                    ? "공개가 종료되었거나 존재하지 않는 콘텐츠입니다."
                    : "콘텐츠를 불러오지 못했습니다.");
            }
            const content = normalizeContentDetail(await response.json());
            if (requestSequence !== detailRequestSequence) return;
            currentContent = content;
            renderContent(currentContent);
        } catch (error) {
            if (error?.name === "AbortError" || requestSequence !== detailRequestSequence) return;
            showError(error instanceof Error ? error.message : "콘텐츠를 불러오지 못했습니다.");
        } finally {
            if (detailRequestController === requestController) detailRequestController = null;
        }
    }

    function detailText(value, label, limit, required = false) {
        if (typeof value !== "string") {
            if (required) throw new Error(`${label}이 올바르지 않습니다.`);
            return "";
        }
        const normalized = value.replace(/[\u0000-\u001f\u007f]/g, " ").replace(/\s+/g, " ").trim();
        if ((required && !normalized) || normalized.length > limit) throw new Error(`${label}이 올바르지 않습니다.`);
        return normalized;
    }

    function detailInteger(value, label, minimum = 0) {
        if (!Number.isSafeInteger(value) || value < minimum) throw new Error(`${label}이 올바르지 않습니다.`);
        return value;
    }

    function normalizeNavigation(value, boardType) {
        if (value == null) return null;
        const id = detailInteger(value.id, "인접 콘텐츠 번호", 1);
        if (id === documentId || value.boardType !== boardType) throw new Error("인접 콘텐츠가 올바르지 않습니다.");
        return { id, boardType, title: detailText(value.title, "인접 콘텐츠 제목", 200, true), createdDate: detailText(value.createdDate, "인접 콘텐츠 날짜", 30) };
    }

    function normalizeRelatedContents(value, boardType) {
        if (!Array.isArray(value) || value.length > 4) throw new Error("연관 콘텐츠 목록이 올바르지 않습니다.");
        const ids = new Set();
        return value.map((item) => {
            const id = detailInteger(item?.id, "연관 콘텐츠 번호", 1);
            if (id === documentId || ids.has(id) || item.boardType !== boardType) throw new Error("연관 콘텐츠가 중복되었거나 올바르지 않습니다.");
            ids.add(id);
            return {
                id,
                boardType,
                title: detailText(item.title, "연관 콘텐츠 제목", 200, true),
                summary: detailText(item.summary, "연관 콘텐츠 요약", 500),
                viewCount: detailInteger(item.viewCount, "연관 콘텐츠 조회수"),
                pinned: item.pinned === true,
                createdDate: detailText(item.createdDate, "연관 콘텐츠 날짜", 30)
            };
        });
    }

    function normalizeContentDetail(value) {
        if (!value || typeof value !== "object" || Array.isArray(value)) throw new Error("콘텐츠 응답이 올바르지 않습니다.");
        const id = detailInteger(value.id, "콘텐츠 번호", 1);
        const boardType = ["NOTICE", "STYLE"].includes(value.boardType) ? value.boardType : "";
        if (id !== documentId || !boardType) throw new Error("요청한 콘텐츠와 응답이 일치하지 않습니다.");
        const content = detailText(value.content, "콘텐츠 본문", 50000);
        const characterCount = detailInteger(value.characterCount, "콘텐츠 글자 수");
        if (characterCount !== Array.from(content).length) throw new Error("콘텐츠 글자 수가 본문과 일치하지 않습니다.");
        return {
            id,
            boardType,
            title: detailText(value.title, "콘텐츠 제목", 200, true),
            content,
            viewCount: detailInteger(value.viewCount, "콘텐츠 조회수"),
            pinned: value.pinned === true,
            createdDate: detailText(value.createdDate, "콘텐츠 날짜", 30),
            estimatedReadMinutes: detailInteger(value.estimatedReadMinutes, "예상 읽기 시간", 1),
            characterCount,
            newerContent: normalizeNavigation(value.newerContent, boardType),
            olderContent: normalizeNavigation(value.olderContent, boardType),
            relatedContents: normalizeRelatedContents(value.relatedContents, boardType)
        };
    }

    function renderContent(content) {
        const typeLabel = content.boardType === "NOTICE" ? "NOTICE" : "STYLE EDIT";
        setText(elements.type, typeLabel);
        setText(elements.title, content.title || "제목 없는 콘텐츠");
        setText(elements.breadcrumb, content.title || "콘텐츠");
        setText(elements.date, content.createdDate || "최근 게시");
        setText(elements.views, `조회 ${Number(content.viewCount || 0).toLocaleString("ko-KR")}`);
        setText(elements.readingTime, `약 ${Math.max(1, Number(content.estimatedReadMinutes || 1))}분`);
        setText(elements.characterCount, `${Number(content.characterCount || 0).toLocaleString("ko-KR")}자`);
        elements.pinned.hidden = !content.pinned;
        elements.body.replaceChildren(createBodyParagraph(content.content));
        elements.article.hidden = false;
        elements.article.setAttribute("aria-busy", "false");
        elements.error.hidden = true;
        document.title = `${content.title || "콘텐츠"} | NOREN`;
        renderNavigation(content.newerContent, content.olderContent);
        renderRelated(Array.isArray(content.relatedContents) ? content.relatedContents : []);
        syncBookmarkButton();
        renderResumePrompt();
        rememberContent(content);
        void recordContentView();
        void loadReactionSummary();
        window.requestAnimationFrame(syncScrollProgress);
        announce("콘텐츠를 불러왔습니다.");
    }

    function renderNavigation(newerContent, olderContent) {
        renderNavigationItem(
            elements.newerLink,
            elements.newerTitle,
            elements.newerDate,
            newerContent
        );
        renderNavigationItem(
            elements.olderLink,
            elements.olderTitle,
            elements.olderDate,
            olderContent
        );
        elements.navigation.hidden = !newerContent && !olderContent;
    }

    function renderNavigationItem(link, title, date, item) {
        link.hidden = !item;
        if (!item) return;
        link.href = `/front/content/${Number(item.id)}`;
        setText(title, item.title || "제목 없는 콘텐츠");
        setText(date, item.createdDate || "최근 게시");
    }

    async function recordContentView() {
        try {
            const response = await fetch(`/api/front/content/${documentId}/views`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ visitorKey: resolveVisitorKey() })
            });
            if (!response.ok) return;
            const payload = await response.json();
            if (typeof payload?.counted !== "boolean" || !Number.isSafeInteger(payload.viewCount)
                || payload.viewCount < currentContent.viewCount) return;
            currentContent.viewCount = payload.viewCount;
            setText(elements.views, `조회 ${currentContent.viewCount.toLocaleString("ko-KR")}`);
        } catch (error) {
            // Reading remains available even when engagement recording is temporarily unavailable.
        }
    }

    async function loadReactionSummary() {
        const requestSequence = ++reactionRequestSequence;
        setReactionLoading(true);
        elements.reaction.hidden = false;
        elements.reactionRetryButton.hidden = true;
        setText(elements.reactionStatus, "반응을 불러오는 중입니다.");
        try {
            const response = await fetch(`/api/front/content/${documentId}/reactions`, {
                headers: { "X-Content-Visitor-Key": resolveVisitorKey() }
            });
            if (!response.ok) throw new Error("반응을 불러오지 못했습니다.");
            const payload = normalizeReaction(await response.json());
            if (requestSequence !== reactionRequestSequence) return;
            renderReaction(payload);
        } catch (error) {
            if (requestSequence !== reactionRequestSequence) return;
            showReactionError("반응 정보를 불러오지 못했습니다.");
        } finally {
            if (requestSequence === reactionRequestSequence) setReactionLoading(false);
        }
    }

    async function submitReaction(reaction) {
        if (!currentContent || reactionRequestInFlight) return;
        const requestSequence = ++reactionRequestSequence;
        setReactionLoading(true);
        setText(elements.reactionStatus, "반응을 저장하는 중입니다.");
        try {
            const response = await fetch(`/api/front/content/${documentId}/reactions`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    visitorKey: resolveVisitorKey(),
                    reaction
                })
            });
            if (!response.ok) throw new Error("반응을 저장하지 못했습니다.");
            const payload = normalizeReaction(await response.json(), reaction);
            if (requestSequence !== reactionRequestSequence) return;
            renderReaction(payload);
            announce(payload.changed ? "콘텐츠 반응을 저장했습니다." : "이미 선택한 반응입니다.");
        } catch (error) {
            if (requestSequence !== reactionRequestSequence) return;
            showReactionError("반응을 저장하지 못했습니다. 다시 시도해 주세요.");
            announce("콘텐츠 반응을 저장하지 못했습니다.");
        } finally {
            if (requestSequence === reactionRequestSequence) setReactionLoading(false);
        }
    }

    function normalizeReaction(value, expectedReaction = null) {
        const helpfulCount = detailInteger(value?.helpfulCount, "도움 반응 수");
        const notHelpfulCount = detailInteger(value?.notHelpfulCount, "아쉬움 반응 수");
        const totalCount = detailInteger(value?.totalCount, "전체 반응 수");
        const helpfulRate = detailInteger(value?.helpfulRate, "도움 반응 비율");
        const selectedReaction = value?.selectedReaction == null ? "" : value.selectedReaction;
        if (totalCount !== helpfulCount + notHelpfulCount || helpfulRate > 100
            || (totalCount ? helpfulRate !== Math.round(helpfulCount * 100 / totalCount) : helpfulRate !== 0)
            || !["", "HELPFUL", "NOT_HELPFUL"].includes(selectedReaction)
            || typeof value?.changed !== "boolean"
            || (expectedReaction && selectedReaction !== expectedReaction)) {
            throw new Error("콘텐츠 반응 응답이 올바르지 않습니다.");
        }
        return { helpfulCount, notHelpfulCount, totalCount, helpfulRate, selectedReaction, changed: value.changed };
    }

    function renderReaction(payload) {
        const helpfulCount = Math.max(0, Number(payload.helpfulCount || 0));
        const notHelpfulCount = Math.max(0, Number(payload.notHelpfulCount || 0));
        const totalCount = Math.max(0, Number(payload.totalCount || helpfulCount + notHelpfulCount));
        const helpfulRate = Math.min(100, Math.max(0, Number(payload.helpfulRate || 0)));
        const selectedReaction = payload.selectedReaction || "";
        setText(elements.helpfulCount, helpfulCount.toLocaleString("ko-KR"));
        setText(elements.notHelpfulCount, notHelpfulCount.toLocaleString("ko-KR"));
        setText(elements.reactionSummary, totalCount > 0
            ? `${totalCount.toLocaleString("ko-KR")}명 중 ${helpfulRate}%가 도움됐다고 답했습니다.`
            : "첫 번째 반응을 남겨주세요.");
        setText(elements.reactionStatus, selectedReaction === "HELPFUL"
            ? "도움됐어요를 선택했습니다."
            : selectedReaction === "NOT_HELPFUL"
                ? "아쉬워요를 선택했습니다."
                : "한 번 선택한 뒤에도 반응을 변경할 수 있습니다.");
        elements.reactionMeter.style.width = `${helpfulRate}%`;
        elements.helpfulButton.setAttribute("aria-pressed", String(selectedReaction === "HELPFUL"));
        elements.notHelpfulButton.setAttribute("aria-pressed", String(selectedReaction === "NOT_HELPFUL"));
        elements.reactionRetryButton.hidden = true;
    }

    function setReactionLoading(loading) {
        reactionRequestInFlight = loading;
        elements.reaction.setAttribute("aria-busy", String(loading));
        elements.helpfulButton.disabled = loading;
        elements.notHelpfulButton.disabled = loading;
        elements.reactionRetryButton.disabled = loading;
    }

    function showReactionError(message) {
        setText(elements.reactionStatus, message);
        elements.reactionRetryButton.hidden = false;
    }

    function resolveVisitorKey() {
        try {
            const saved = String(window.localStorage.getItem(CONTENT_VISITOR_KEY) || "").trim();
            if (isValidVisitorKey(saved)) return saved;
            const generated = createVisitorKey();
            window.localStorage.setItem(CONTENT_VISITOR_KEY, generated);
            return generated;
        } catch (error) {
            memoryVisitorKey ||= createVisitorKey();
            return memoryVisitorKey;
        }
    }

    function createVisitorKey() {
        return window.crypto?.randomUUID?.() || fallbackVisitorKey();
    }

    function isValidVisitorKey(value) {
        return /^[A-Za-z0-9-]{16,64}$/.test(value);
    }

    function fallbackVisitorKey() {
        return `visitor-${Date.now()}-${Math.random().toString(36).slice(2, 14)}`;
    }

    function rememberContent(content) {
        try {
            const saved = JSON.parse(window.localStorage.getItem(RECENT_CONTENT_KEY) || "[]");
            const item = {
                id: Number(content.id),
                boardType: content.boardType,
                title: content.title,
                createdDate: content.createdDate,
                viewedAt: new Date().toISOString()
            };
            const next = [item].concat(Array.isArray(saved) ? saved.filter((recent) => Number(recent.id) !== item.id) : [])
                .slice(0, 6);
            window.localStorage.setItem(RECENT_CONTENT_KEY, JSON.stringify(next));
        } catch (error) {
            // Private browsing can reject storage without affecting the content page.
        }
    }

    function readBookmarks() {
        try {
            const value = JSON.parse(window.localStorage.getItem(BOOKMARKED_CONTENT_KEY) || "[]");
            if (!Array.isArray(value)) return [];
            const ids = new Set();
            return value.slice(0, 100).flatMap((item) => {
                const id = Number(item?.id);
                if (!Number.isSafeInteger(id) || id <= 0 || ids.has(id) || !["NOTICE", "STYLE"].includes(item.boardType)) return [];
                ids.add(id);
                return [{ id, boardType: item.boardType, title: detailText(item.title, "저장 콘텐츠 제목", 200), createdDate: detailText(item.createdDate, "저장 콘텐츠 날짜", 30), savedAt: detailText(item.savedAt, "저장 시각", 40) }];
            }).slice(0, 50);
        } catch (error) {
            return [];
        }
    }

    function toggleBookmark() {
        if (!currentContent) return;
        try {
            const bookmarks = readBookmarks();
            const contentId = Number(currentContent.id);
            const bookmarked = bookmarks.some((item) => Number(item.id) === contentId);
            const next = bookmarked
                ? bookmarks.filter((item) => Number(item.id) !== contentId)
                : [{
                    id: contentId,
                    boardType: currentContent.boardType,
                    title: currentContent.title,
                    createdDate: currentContent.createdDate,
                    savedAt: new Date().toISOString()
                }].concat(bookmarks).slice(0, 50);
            window.localStorage.setItem(BOOKMARKED_CONTENT_KEY, JSON.stringify(next));
            syncBookmarkButton();
            announce(bookmarked ? "관심 콘텐츠에서 제거했습니다." : "관심 콘텐츠로 저장했습니다.");
        } catch (error) {
            announce("관심 콘텐츠를 저장하지 못했습니다.");
        }
    }

    function syncBookmarkButton() {
        const bookmarked = currentContent
            && readBookmarks().some((item) => Number(item.id) === Number(currentContent.id));
        elements.bookmarkButton.setAttribute("aria-pressed", String(Boolean(bookmarked)));
        setText(elements.bookmarkButton, bookmarked ? "저장됨" : "관심 저장");
    }

    function restoreFontScale() {
        try {
            const saved = window.localStorage.getItem(FONT_SCALE_KEY);
            const parsed = saved === null ? Number.NaN : Number(saved);
            fontScale = Number.isFinite(parsed) ? clampFontScale(parsed) : 1;
        } catch (error) {
            fontScale = 1;
        }
        applyFontScale();
    }

    function changeFontScale(direction) {
        fontScale = clampFontScale(fontScale + direction * FONT_SCALE_STEP);
        persistFontScale();
        applyFontScale();
    }

    function resetFontScale() {
        fontScale = 1;
        persistFontScale();
        applyFontScale();
    }

    function clampFontScale(value) {
        return Math.min(MAX_FONT_SCALE, Math.max(MIN_FONT_SCALE, Math.round(value * 10) / 10));
    }

    function persistFontScale() {
        try {
            window.localStorage.setItem(FONT_SCALE_KEY, String(fontScale));
        } catch (error) {
            // The selected size still applies for the current page.
        }
    }

    function applyFontScale() {
        elements.article.style.setProperty("--content-font-scale", String(fontScale));
        setText(elements.fontScaleLabel, `${Math.round(fontScale * 100)}%`);
        elements.fontDecreaseButton.disabled = fontScale <= MIN_FONT_SCALE;
        elements.fontIncreaseButton.disabled = fontScale >= MAX_FONT_SCALE;
    }

    function readProgressEntries() {
        try {
            const value = JSON.parse(window.localStorage.getItem(READING_PROGRESS_KEY) || "{}");
            if (!value || typeof value !== "object" || Array.isArray(value)) return {};
            return Object.fromEntries(Object.entries(value).flatMap(([id, item]) => {
                const numericId = Number(id);
                const progress = Number(item?.progress);
                if (!Number.isSafeInteger(numericId) || numericId <= 0 || !Number.isInteger(progress) || progress < 0 || progress > 100) return [];
                return [[String(numericId), { progress, updatedAt: detailText(item.updatedAt, "읽기 기록 시각", 40) }]];
            }).slice(0, 30));
        } catch (error) {
            return {};
        }
    }

    function renderResumePrompt() {
        const saved = readProgressEntries()[String(documentId)];
        const progress = Number(saved?.progress || 0);
        elements.resume.hidden = progress < 10 || progress >= 95;
        if (!elements.resume.hidden) {
            elements.resume.dataset.progress = String(progress);
            setText(elements.resumeText, `이전에 ${progress}%까지 읽었습니다.`);
        }
    }

    function resumeReading() {
        const progress = Number(elements.resume.dataset.progress || 0);
        const bodyTop = elements.body.getBoundingClientRect().top + window.scrollY;
        const target = bodyTop + elements.body.offsetHeight * progress / 100 - window.innerHeight * 0.2;
        window.scrollTo({ top: Math.max(0, target), behavior: "smooth" });
        elements.resume.hidden = true;
        announce(`${progress}% 지점부터 이어 읽습니다.`);
    }

    function dismissResume() {
        const entries = readProgressEntries();
        delete entries[String(documentId)];
        persistProgressEntries(entries);
        elements.resume.hidden = true;
        announce("이전 읽기 위치를 삭제했습니다.");
    }

    function persistReadingProgress(progress) {
        if (!currentContent || progress < 1 || Math.abs(progress - lastSavedProgress) < 5) return;
        const entries = readProgressEntries();
        entries[String(documentId)] = { progress, updatedAt: new Date().toISOString() };
        const trimmed = Object.fromEntries(Object.entries(entries)
            .sort((left, right) => String(right[1]?.updatedAt || "").localeCompare(String(left[1]?.updatedAt || "")))
            .slice(0, 30));
        persistProgressEntries(trimmed);
        lastSavedProgress = progress;
    }

    function persistProgressEntries(entries) {
        try {
            window.localStorage.setItem(READING_PROGRESS_KEY, JSON.stringify(entries));
        } catch (error) {
            // Reading remains available when progress persistence is blocked.
        }
    }

    function createBodyParagraph(content) {
        const paragraph = document.createElement("p");
        paragraph.textContent = content || "등록된 본문이 없습니다.";
        return paragraph;
    }

    function renderRelated(items) {
        elements.relatedGrid.replaceChildren();
        elements.related.hidden = items.length === 0;
        items.forEach((item, index) => {
            const link = document.createElement("a");
            link.className = "content-detail-related-card";
            link.href = `/front/content/${Number(item.id)}`;
            const sequence = document.createElement("span");
            sequence.textContent = String(index + 1).padStart(2, "0");
            const title = document.createElement("strong");
            title.textContent = item.title || "제목 없는 콘텐츠";
            const summary = document.createElement("p");
            summary.textContent = item.summary || "내용을 확인해 주세요.";
            const meta = document.createElement("em");
            meta.textContent = `${item.createdDate || "최근 게시"} · 조회 ${Number(item.viewCount || 0).toLocaleString("ko-KR")}`;
            link.append(sequence, title, summary, meta);
            elements.relatedGrid.appendChild(link);
        });
    }

    function showLoading() {
        elements.article.hidden = false;
        elements.article.setAttribute("aria-busy", "true");
        elements.error.hidden = true;
        elements.related.hidden = true;
        elements.navigation.hidden = true;
        elements.resume.hidden = true;
        elements.reaction.hidden = true;
        announce("콘텐츠를 불러오는 중입니다.");
    }

    function showError(message) {
        elements.article.hidden = true;
        elements.related.hidden = true;
        elements.navigation.hidden = true;
        elements.resume.hidden = true;
        elements.reaction.hidden = true;
        elements.error.hidden = false;
        setText(elements.errorMessage, message);
        announce(message);
        elements.retryButton.focus();
    }

    async function copySummary() {
        if (!currentContent) return;
        const text = `${currentContent.title}\n${currentContent.content}\n${window.location.href}`;
        await copyText(text, "콘텐츠 요약을 복사했습니다.");
    }

    async function shareContent() {
        if (!currentContent) return;
        const shareData = { title: currentContent.title, text: currentContent.content, url: window.location.href };
        if (navigator.share) {
            try {
                await navigator.share(shareData);
                announce("콘텐츠를 공유했습니다.");
                return;
            } catch (error) {
                if (error?.name === "AbortError") return;
            }
        }
        await copyText(window.location.href, "콘텐츠 주소를 복사했습니다.");
    }

    async function copyText(text, message) {
        if (!navigator.clipboard?.writeText) {
            announce("이 브라우저에서는 복사를 지원하지 않습니다.");
            return;
        }
        try {
            await navigator.clipboard.writeText(text);
            announce(message);
        } catch (error) {
            announce("복사하지 못했습니다.");
        }
    }

    function syncScrollProgress() {
        const scrollable = document.documentElement.scrollHeight - window.innerHeight;
        const progress = scrollable > 0 ? Math.min(100, Math.max(0, window.scrollY / scrollable * 100)) : 0;
        elements.scrollProgress.style.width = `${progress}%`;
        const bodyTop = elements.body.getBoundingClientRect().top + window.scrollY;
        const readingStart = bodyTop - window.innerHeight * 0.35;
        const readingEnd = bodyTop + elements.body.offsetHeight - window.innerHeight * 0.45;
        const readingRange = Math.max(1, readingEnd - readingStart);
        const readingProgress = Math.round(Math.min(100, Math.max(0, (window.scrollY - readingStart) / readingRange * 100)));
        setText(elements.progressText, `${readingProgress}%`);
        elements.readerProgress.setAttribute("aria-valuenow", String(readingProgress));
        persistReadingProgress(readingProgress);
    }

    function requestScrollProgressSync() {
        if (scrollFrame !== null) return;
        scrollFrame = window.requestAnimationFrame(() => {
            syncScrollProgress();
            scrollFrame = null;
        });
    }

    function restoreReturnLinks() {
        try {
            const stored = window.sessionStorage.getItem(CONTENT_RETURN_URL_KEY);
            if (!stored) return;
            const url = new URL(stored, window.location.origin);
            if (url.origin !== window.location.origin || url.pathname !== "/front/content") return;
            document.querySelectorAll("[data-content-return-link]").forEach((link) => {
                link.href = `${url.pathname}${url.search}`;
            });
        } catch (error) {
            // Static archive links remain as a safe fallback.
        }
    }

    function handleReaderShortcut(event) {
        if (event.metaKey || event.ctrlKey || event.altKey) return;
        if (event.target instanceof Element && event.target.closest("input, textarea, select, button, a")) return;
        if (event.key.toLowerCase() === "b") {
            event.preventDefault();
            toggleBookmark();
        }
        if (event.key === "[") {
            event.preventDefault();
            changeFontScale(-1);
        }
        if (event.key === "]") {
            event.preventDefault();
            changeFontScale(1);
        }
    }

    function announce(message) {
        setText(elements.status, message);
    }

    function setText(element, value) {
        if (element) element.textContent = value;
    }

    elements.retryButton.addEventListener("click", loadContentDetail);
    elements.copyButton.addEventListener("click", copySummary);
    elements.shareButton.addEventListener("click", shareContent);
    elements.bookmarkButton.addEventListener("click", toggleBookmark);
    elements.fontDecreaseButton.addEventListener("click", () => changeFontScale(-1));
    elements.fontIncreaseButton.addEventListener("click", () => changeFontScale(1));
    elements.fontResetButton.addEventListener("click", resetFontScale);
    elements.resumeButton.addEventListener("click", resumeReading);
    elements.resumeDismissButton.addEventListener("click", dismissResume);
    elements.helpfulButton.addEventListener("click", () => submitReaction("HELPFUL"));
    elements.notHelpfulButton.addEventListener("click", () => submitReaction("NOT_HELPFUL"));
    elements.reactionRetryButton.addEventListener("click", loadReactionSummary);
    window.addEventListener("scroll", requestScrollProgressSync, { passive: true });
    window.addEventListener("resize", requestScrollProgressSync);
    window.addEventListener("keydown", handleReaderShortcut);
    restoreReturnLinks();
    restoreFontScale();
    syncScrollProgress();
    void loadContentDetail();
})();
