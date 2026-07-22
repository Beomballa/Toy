(function () {
    const documentId = Number(document.body.dataset.documentId || 0);
    const CONTENT_VISITOR_KEY = "front-content-visitor-key";
    const RECENT_CONTENT_KEY = "front-recent-content";
    let currentContent = null;

    const elements = {
        article: document.getElementById("contentDetailArticle"),
        status: document.getElementById("contentDetailStatus"),
        type: document.getElementById("contentDetailType"),
        pinned: document.getElementById("contentDetailPinned"),
        title: document.getElementById("contentDetailTitle"),
        date: document.getElementById("contentDetailDate"),
        views: document.getElementById("contentDetailViews"),
        body: document.getElementById("contentDetailBody"),
        breadcrumb: document.getElementById("contentDetailBreadcrumb"),
        error: document.getElementById("contentDetailError"),
        errorMessage: document.getElementById("contentDetailErrorMessage"),
        retryButton: document.getElementById("contentDetailRetryButton"),
        copyButton: document.getElementById("contentDetailCopyButton"),
        shareButton: document.getElementById("contentDetailShareButton"),
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
        elements.pinned.hidden = !content.pinned;
        elements.body.replaceChildren(createBodyParagraph(content.content));
        elements.article.hidden = false;
        elements.article.setAttribute("aria-busy", "false");
        elements.error.hidden = true;
        document.title = `${content.title || "콘텐츠"} | Grade Stock`;
        renderRelated(Array.isArray(content.relatedContents) ? content.relatedContents : []);
        rememberContent(content);
        void recordContentView();
        announce("콘텐츠를 불러왔습니다.");
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
        announce("콘텐츠를 불러오는 중입니다.");
    }

    function showError(message) {
        elements.article.hidden = true;
        elements.related.hidden = true;
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
    window.addEventListener("scroll", syncScrollProgress, { passive: true });
    syncScrollProgress();
    void loadContentDetail();
})();
