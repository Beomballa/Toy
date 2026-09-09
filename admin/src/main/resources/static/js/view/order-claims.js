(() => {
    "use strict";
    const state = { page: 0, status: "ALL", requestId: 0, action: null, submitting: false };
    const modalElement = document.getElementById("claimActionModal");
    const actionModal = new bootstrap.Modal(modalElement);
    const statusLabels = { APPROVED: "승인", REJECTED: "반려", COMPLETED: "완료" };
    const escapeHtml = value => String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;").replaceAll("'", "&#39;");

    function syncUrl() {
        const params = new URLSearchParams({ page: String(state.page), status: state.status });
        history.replaceState(null, "", `${location.pathname}?${params}`);
    }
    function readUrl() {
        const params = new URLSearchParams(location.search);
        state.page = Math.max(0, Number.parseInt(params.get("page"), 10) || 0);
        state.status = ["ALL", "REQUESTED", "APPROVED", "REJECTED", "COMPLETED", "CANCELLED"].includes(params.get("status")) ? params.get("status") : "ALL";
        document.getElementById("claimStatus").value = state.status;
    }
    function actionButtons(claim) {
        if (claim.status === "REQUESTED") return `<button class="btn btn-sm btn-primary" data-claim-action="APPROVED" data-claim-no="${claim.claimNo}">승인</button> <button class="btn btn-sm btn-outline-danger" data-claim-action="REJECTED" data-claim-no="${claim.claimNo}">반려</button>`;
        if (claim.status === "APPROVED") return `<button class="btn btn-sm btn-success" data-claim-action="COMPLETED" data-claim-no="${claim.claimNo}">완료</button>`;
        return "-";
    }
    function render(data) {
        const claims = Array.isArray(data.claims) ? data.claims : [];
        document.getElementById("claimTotalCount").textContent = `전체 ${Math.max(0, Number(data.totalElements) || 0).toLocaleString("ko-KR")}건`;
        document.getElementById("claimResultMeta").textContent = claims.length ? `${data.page + 1}페이지 요청을 표시합니다.` : "조건에 맞는 요청이 없습니다.";
        document.getElementById("claimTableBody").innerHTML = claims.length ? claims.map(claim => `<tr><td class="ps-4"><strong>${escapeHtml(claim.claimTypeLabel)}</strong><br><small class="text-muted">#${claim.claimNo}</small></td><td><a href="/admin/orders/get?no=${claim.orderNo}" class="text-decoration-none">${escapeHtml(claim.orderNumber)}</a></td><td>#${claim.memberNo}</td><td class="text-break" style="min-width:220px">${escapeHtml(claim.reason)}</td><td class="text-nowrap">${escapeHtml(claim.requestedAt)}</td><td><span class="badge text-bg-light border">${escapeHtml(claim.statusLabel)}</span></td><td class="text-end pe-4 text-nowrap">${actionButtons(claim)}</td></tr>`).join("") : '<tr><td colspan="7" class="py-5 text-center text-muted">조건에 맞는 요청이 없습니다.</td></tr>';
        const pages = Math.max(0, Number(data.totalPages) || 0);
        const current = data.page;
        const start = Math.max(0, Math.min(current - 2, pages - 5));
        const pageButton = (page, label, disabled = false) => `<li class="page-item ${disabled ? "disabled" : ""} ${page === current && label === String(page + 1) ? "active" : ""}"><button type="button" class="page-link" data-claim-page="${page}" ${disabled ? "disabled" : ""} ${page === current && label === String(page + 1) ? 'aria-current="page"' : ""}>${label}</button></li>`;
        document.getElementById("claimPagination").innerHTML = pages <= 1 ? "" :
            pageButton(0, "처음", current === 0) + pageButton(current - 1, "이전", current === 0) +
            Array.from({ length: Math.min(pages, 5) }, (_, index) => pageButton(start + index, String(start + index + 1))).join("") +
            pageButton(current + 1, "다음", current >= pages - 1) + pageButton(pages - 1, "마지막", current >= pages - 1);
    }
    async function load() {
        const requestId = ++state.requestId;
        document.getElementById("claimPagination").innerHTML = "";
        document.getElementById("claimTableBody").innerHTML = '<tr><td colspan="7" class="py-5 text-center text-muted">요청을 불러오는 중입니다.</td></tr>';
        try {
            const response = await fetch(`/api/admin/orders/claims?status=${encodeURIComponent(state.status)}&page=${state.page}`, { headers: { Accept: "application/json" } });
            if (!response.ok) throw new Error(await CommonJS.extractErrorMessage(response, "요청을 불러오지 못했습니다."));
            const data = await response.json();
            if (requestId !== state.requestId) return;
            if (!Number.isSafeInteger(data.page) || data.page < 0 || !Number.isSafeInteger(data.totalPages) || data.totalPages < 0) throw new Error("페이지 정보가 올바르지 않습니다.");
            const lastPage = Math.max(0, data.totalPages - 1);
            if (state.page > lastPage) {
                state.page = lastPage;
                syncUrl();
                return load();
            }
            render(data);
        } catch (error) {
            if (requestId === state.requestId) document.getElementById("claimTableBody").innerHTML = `<tr><td colspan="7" class="py-5 text-center text-danger">${escapeHtml(error.message || "요청을 불러오지 못했습니다.")}</td></tr>`;
        }
    }
    document.getElementById("claimFilterButton").addEventListener("click", () => { state.page = 0; state.status = document.getElementById("claimStatus").value; syncUrl(); load(); });
    document.getElementById("claimResetButton").addEventListener("click", () => { state.page = 0; state.status = "ALL"; document.getElementById("claimStatus").value = state.status; syncUrl(); load(); });
    document.getElementById("claimPagination").addEventListener("click", event => { const button = event.target.closest("[data-claim-page]"); if (!button) return; state.page = Number(button.dataset.claimPage); syncUrl(); load(); });
    document.getElementById("claimTableBody").addEventListener("click", event => { const button = event.target.closest("[data-claim-action]"); if (!button || state.submitting) return; state.action = { claimNo: Number(button.dataset.claimNo), status: button.dataset.claimAction }; document.getElementById("claimActionTitle").textContent = `요청 ${statusLabels[state.action.status]} 처리`; document.getElementById("claimActionDescription").textContent = `처리 결과가 회원의 교환·반품 이력에 반영됩니다.`; document.getElementById("claimActionMemo").value = ""; actionModal.show(); });
    modalElement.addEventListener("hide.bs.modal", event => {
        if (state.submitting) event.preventDefault();
    });
    modalElement.addEventListener("hidden.bs.modal", () => { state.action = null; });
    document.getElementById("claimActionForm").addEventListener("submit", async event => {
        event.preventDefault();
        const memo = document.getElementById("claimActionMemo").value.trim();
        if (state.submitting || !state.action || !memo) return;
        const action = { ...state.action };
        const submit = document.getElementById("claimActionSubmitButton");
        state.submitting = true;
        submit.disabled = true;
        let completed = false;
        try {
            const response = await fetch("/api/admin/orders/claims/status", {
                method: "PATCH", headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ claimNo: action.claimNo, status: action.status, memo })
            });
            if (!response.ok) throw new Error(await CommonJS.extractErrorMessage(response, "요청을 처리하지 못했습니다."));
            completed = true;
            state.action = null;
        } catch (error) {
            await CommonJS.alert(error.message || "요청을 처리하지 못했습니다.", "오류", "error");
        } finally {
            state.submitting = false;
            submit.disabled = false;
        }
        if (completed) {
            actionModal.hide();
            load();
        }
    });
    readUrl(); load();
})();
