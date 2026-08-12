(() => {
    "use strict";
    const params = new URLSearchParams(location.search);
    const state = { status: params.get("status") || "ALL", reportedOnly: params.get("reportedOnly") === "true", page: 0, hasNext: false, loading: false };
    const body = document.getElementById("reviewListBody");
    const moreButton = document.getElementById("reviewMoreButton");

    const escapeHtml = value => String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;").replaceAll("'", "&#39;");
    const statusLabel = status => status === "HIDDEN" ? "숨김" : "노출";
    const detailsTemplate = (type, review) => `<template data-review-${type}-detail="${Number(review.reviewId)}">${escapeHtml(JSON.stringify(type === "report" ? review.reports || [] : review.statusHistories || []))}</template>`;

    async function load(reset = false) {
        if (state.loading) return;
        if (reset) { state.page = 0; state.hasNext = false; body.replaceChildren(); }
        state.loading = true;
        moreButton.disabled = true;
        try {
            const response = await fetch(`/api/admin/reviews?status=${encodeURIComponent(state.status)}&reportedOnly=${state.reportedOnly}&page=${state.page}&size=20`, { headers: { Accept: "application/json" } });
            if (!response.ok) throw new Error("후기 목록을 불러오지 못했습니다.");
            const payload = await response.json();
            const reviews = Array.isArray(payload.reviews) ? payload.reviews : [];
            if (!reviews.length && state.page === 0) {
                body.innerHTML = '<tr><td colspan="9" class="text-center py-5 text-muted">조건에 맞는 후기가 없습니다.</td></tr>';
            } else {
                body.insertAdjacentHTML("beforeend", reviews.map(review => `<tr><td class="ps-4"><strong>${escapeHtml(review.brandName)}</strong><div class="small text-muted text-truncate admin-review-product-name">${escapeHtml(review.productName)}</div></td><td>${escapeHtml(review.reviewerName)}</td><td>${"★".repeat(Math.min(5, Math.max(1, Number(review.rating) || 1)))}</td><td class="admin-review-content">${escapeHtml(review.content)}</td><td>${Number(review.reportCount) > 0 ? `<button class="btn btn-sm btn-outline-danger" type="button" data-review-reports="${Number(review.reviewId)}">${Number(review.reportCount)}건</button>${detailsTemplate("report", review)}` : '<span class="text-muted small">-</span>'}</td><td>${Array.isArray(review.statusHistories) && review.statusHistories.length > 0 ? `<button class="btn btn-sm btn-outline-primary" type="button" data-review-histories="${Number(review.reviewId)}">${Number(review.statusHistories.length)}건</button>${detailsTemplate("history", review)}` : '<span class="text-muted small">-</span>'}</td><td class="small text-muted">${escapeHtml(review.createdAt)}</td><td><span class="badge ${review.status === "HIDDEN" ? "text-bg-secondary" : "text-bg-success"}">${statusLabel(review.status)}</span></td><td class="text-end pe-4"><button class="btn btn-sm ${review.status === "HIDDEN" ? "btn-outline-success" : "btn-outline-secondary"}" type="button" data-review-id="${Number(review.reviewId)}" data-next-status="${review.status === "HIDDEN" ? "VISIBLE" : "HIDDEN"}">${review.status === "HIDDEN" ? "복구" : "숨김"}</button></td></tr>`).join(""));
            }
            state.page += 1;
            state.hasNext = payload.hasNext === true;
            moreButton.hidden = !state.hasNext;
            document.getElementById("reviewListMeta").textContent = `전체 ${Math.max(0, Number(payload.totalCount) || 0)}건`;
            document.getElementById("reviewPageMeta").textContent = `${state.page} / ${Math.max(0, Number(payload.totalPages) || 0)} 페이지`;
        } catch (error) {
            if (!body.children.length) body.innerHTML = '<tr><td colspan="9" class="text-center py-5 text-danger">후기 목록을 불러오지 못했습니다.</td></tr>';
            await CommonJS.alert(error.message, "오류", "error");
        } finally {
            state.loading = false;
            moreButton.disabled = false;
        }
    }

    document.getElementById("reviewStatusFilters").addEventListener("click", event => {
        const button = event.target.closest("[data-status]");
        if (!button || state.status === button.dataset.status) return;
        state.status = button.dataset.status;
        document.querySelectorAll("[data-status]").forEach(item => item.className = `btn btn-sm ${item === button ? "btn-dark" : "btn-outline-secondary"}`);
        syncUrl();
        load(true);
    });
    document.getElementById("reviewReportedOnlyButton").addEventListener("click", event => {
        state.reportedOnly = !state.reportedOnly;
        event.currentTarget.classList.toggle("btn-danger", state.reportedOnly);
        event.currentTarget.classList.toggle("btn-outline-danger", !state.reportedOnly);
        event.currentTarget.setAttribute("aria-pressed", String(state.reportedOnly));
        syncUrl();
        load(true);
    });
    body.addEventListener("click", async event => {
        const reportButton = event.target.closest("[data-review-reports]");
        if (reportButton) {
            const raw = body.querySelector(`[data-review-report-detail="${reportButton.dataset.reviewReports}"]`)?.textContent || "[]";
            try {
                const reports = JSON.parse(raw);
                const detail = reports.map((report, index) => `${index + 1}. ${report.reason}${report.detail ? ` - ${report.detail}` : ""} (${report.createdAt})`).join("\n") || "신고 상세가 없습니다.";
                await CommonJS.alert(detail, "신고 사유", "info");
            } catch (_) {
                await CommonJS.alert("신고 사유를 읽지 못했습니다.", "오류", "error");
            }
            return;
        }
        const historyButton = event.target.closest("[data-review-histories]");
        if (historyButton) {
            const raw = body.querySelector(`[data-review-history-detail="${historyButton.dataset.reviewHistories}"]`)?.textContent || "[]";
            try {
                const histories = JSON.parse(raw);
                const detail = histories.map((history, index) => `${index + 1}. ${history.actionLabel}: ${history.beforeStatusLabel} → ${history.afterStatusLabel} (${history.actorName}, ${history.createdAt})`).join("\n") || "상태 변경 이력이 없습니다.";
                await CommonJS.alert(detail, "후기 처리 이력", "info");
            } catch (_) {
                await CommonJS.alert("처리 이력을 읽지 못했습니다.", "오류", "error");
            }
            return;
        }
        const button = event.target.closest("[data-review-id]");
        if (!button) return;
        const nextStatus = button.dataset.nextStatus;
        const action = nextStatus === "HIDDEN" ? "숨김" : "복구";
        if (!await CommonJS.confirm(`이 후기를 ${action} 처리할까요?`, "후기 상태 변경")) return;
        button.disabled = true;
        try {
            const response = await fetch(`/api/admin/reviews/${encodeURIComponent(button.dataset.reviewId)}/status`, { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ status: nextStatus }) });
            if (!response.ok) throw new Error("후기 상태를 변경하지 못했습니다.");
            load(true);
        } catch (error) {
            button.disabled = false;
            await CommonJS.alert(error.message, "오류", "error");
        }
    });
    moreButton.addEventListener("click", () => load());
    document.querySelector(`[data-status="${state.status}"]`)?.classList.replace("btn-outline-secondary", "btn-dark");
    document.getElementById("reviewReportedOnlyButton").classList.toggle("btn-danger", state.reportedOnly);
    document.getElementById("reviewReportedOnlyButton").classList.toggle("btn-outline-danger", !state.reportedOnly);
    document.getElementById("reviewReportedOnlyButton").setAttribute("aria-pressed", String(state.reportedOnly));
    load(true);

    function syncUrl() {
        const next = new URLSearchParams({ status: state.status });
        if (state.reportedOnly) next.set("reportedOnly", "true");
        history.replaceState(null, "", `/admin/reviews?${next}`);
    }
})();
