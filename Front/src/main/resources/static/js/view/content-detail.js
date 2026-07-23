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
        scrollProgress: document.getElementById("contentDetailScrollProgress")
    };

    async function loadContentDetail() {
        showLoading();
        try {
            if (!Number.isInteger(documentId) || documentId <= 0) {
                throw new Error("올바르지 않은 콘텐츠 주소입니다.");
            }
            const response = await fetch(`/api/front/content/${documentId}`);
            if (!response.ok) {
                throw new Error(response.status === 404
                    ? "공개가 종료되었거나 존재하지 않는 콘텐츠입니다."
                    : "콘텐츠를 불러오지 못했습니다.");
            }
            currentContent = await response.json();
            renderContent(currentContent);
        } catch (error) {
            showError(error instanceof Error ? error.message : "콘텐츠를 불러오지 못했습니다.");
        }
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
        document.title = `${content.title || "콘텐츠"} | Grade Stock`;
        renderNavigation(content.newerContent, content.olderContent);
        renderRelated(Array.isArray(content.relatedContents) ? content.relatedContents : []);
        syncBookmarkButton();
        renderResumePrompt();
        rememberContent(content);
        void recordContentView();
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
            currentContent.viewCount = Number(payload.viewCount || currentContent.viewCount || 0);
            setText(elements.views, `조회 ${currentContent.viewCount.toLocaleString("ko-KR")}`);
        } catch (error) {
            // Reading remains available even when engagement recording is temporarily unavailable.
        }
    }

    function resolveVisitorKey() {
        try {
            const saved = window.localStorage.getItem(CONTENT_VISITOR_KEY);
            if (saved) return saved;
            const generated = window.crypto?.randomUUID?.() || fallbackVisitorKey();
            window.localStorage.setItem(CONTENT_VISITOR_KEY, generated);
            return generated;
        } catch (error) {
            return fallbackVisitorKey();
        }
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
            return Array.isArray(value) ? value : [];
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
            return value && typeof value === "object" && !Array.isArray(value) ? value : {};
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
        announce("콘텐츠를 불러오는 중입니다.");
    }

    function showError(message) {
        elements.article.hidden = true;
        elements.related.hidden = true;
        elements.navigation.hidden = true;
        elements.resume.hidden = true;
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
    window.addEventListener("scroll", requestScrollProgressSync, { passive: true });
    window.addEventListener("resize", requestScrollProgressSync);
    window.addEventListener("keydown", handleReaderShortcut);
    restoreReturnLinks();
    restoreFontScale();
    syncScrollProgress();
    void loadContentDetail();
})();
