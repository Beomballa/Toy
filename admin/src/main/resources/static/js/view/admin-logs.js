const AdminLogPage = {
    initialized: false,
    modal: null,
    state: {
        page: 0,
        size: 20
    },

    init() {
        if (this.initialized) return;
        this.initialized = true;
        this.modal = new bootstrap.Modal(document.getElementById('logDetailModal'));
        this.bindEvents();
        this.readStateFromUrl();
        this.getList();
    },

    bindEvents() {
        document.querySelectorAll('[data-log-quick-filter]').forEach((button) => {
            button.addEventListener('click', () => this.applyQuickFilter(button.dataset.logQuickFilter));
        });
        document.getElementById('btnSearchLog')?.addEventListener('click', () => {
            this.state.page = 0;
            this.getList();
        });
        document.getElementById('btnResetLog')?.addEventListener('click', () => {
            this.resetFilters();
        });
        document.getElementById('logPageSize')?.addEventListener('change', () => {
            this.state.page = 0;
            this.state.size = Number(document.getElementById('logPageSize')?.value || 20);
            this.getList();
        });
        document.getElementById('logListBody')?.addEventListener('click', (event) => {
            const detailButton = event.target.closest('[data-role="open-log-detail"]');
            if (detailButton) {
                this.openDetail(Number(detailButton.dataset.logNo));
            }
        });
        ['logAdminNo', 'logActionType', 'logTargetId', 'logStartDate', 'logEndDate'].forEach((id) => {
            document.getElementById(id)?.addEventListener('keydown', (event) => {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    this.state.page = 0;
                    this.getList();
                }
            });
        });
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        document.getElementById('logAdminNo').value = params.get('adminNo') || '';
        document.getElementById('logActionType').value = params.get('actionType') || '';
        document.getElementById('logTargetId').value = params.get('targetId') || '';
        document.getElementById('logStartDate').value = params.get('startDate') || '';
        document.getElementById('logEndDate').value = params.get('endDate') || '';
        this.state.page = Number(params.get('page') || 0);
        this.state.size = Number(params.get('size') || 20);
        document.getElementById('logPageSize').value = String(this.state.size);
    },

    buildParams() {
        const params = new URLSearchParams();
        const adminNo = document.getElementById('logAdminNo').value.trim();
        const actionType = CommonJS.normalizeOptionalText(document.getElementById('logActionType').value);
        const targetId = document.getElementById('logTargetId').value.trim();
        const startDate = document.getElementById('logStartDate').value;
        const endDate = document.getElementById('logEndDate').value;

        if (adminNo) params.set('adminNo', adminNo);
        if (actionType) params.set('actionType', actionType);
        if (targetId) params.set('targetId', targetId);
        if (startDate) params.set('startDate', startDate);
        if (endDate) params.set('endDate', endDate);
        params.set('page', String(this.state.page));
        params.set('size', String(this.state.size));
        return params;
    },

    async getList() {
        const params = this.buildParams();
        history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
        this.setMetaText('데이터를 불러오는 중입니다...');

        try {
            const res = await fetch(`/api/admin/logs/list?${params.toString()}`);
            if (!res.ok) {
                throw new Error(await CommonJS.extractErrorMessage(res, '로그를 불러오지 못했습니다.'));
            }
            const data = await res.json();
            this.renderList(data.items || []);
            this.renderMeta(data);
            this.renderPagination(data);
        } catch (err) {
            document.getElementById('logListBody').innerHTML =
                `<tr><td colspan="7" class="text-center py-5 text-danger">${err.message}</td></tr>`;
            this.setMetaText('로그 조회 실패');
            document.getElementById('logFilterMeta').textContent = '적용 필터 확인 불가';
            document.getElementById('logPageMeta').textContent = '페이지 메타 확인 불가';
            document.getElementById('logPagination').innerHTML = '';
        }
    },

    renderList(items) {
        const tbody = document.getElementById('logListBody');
        if (!items.length) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center py-5">활동 로그가 없습니다.</td></tr>';
            return;
        }
        tbody.innerHTML = items.map(item => `
            <tr>
                <td class="ps-4 text-muted small">${item.logNo}</td>
                <td><span class="badge bg-light text-dark">${item.adminName} (#${item.adminNo})</span></td>
                <td><span class="fw-bold text-primary">${item.actionType}</span></td>
                <td>
                    ${item.targetPath
                        ? `<a class="text-decoration-none" href="${item.targetPath}">${item.targetLabel}</a>`
                        : (item.targetLabel || '-')}
                </td>
                <td><code class="small">${item.ipAddress}</code></td>
                <td class="text-center">
                    <button type="button" class="btn btn-sm btn-outline-dark" data-role="open-log-detail" data-log-no="${item.logNo}">상세</button>
                </td>
                <td class="text-end pe-4 small text-muted">${item.actionDtm}</td>
            </tr>
        `).join('');
    },

    renderMeta(data) {
        this.setMetaText(data.pageInfoLabel || `${data.rangeStart}-${data.rangeEnd} / ${data.totalElements}건`);
        const filterMeta = document.getElementById('logFilterMeta');
        if (filterMeta) {
            filterMeta.textContent = `적용 필터 ${data.resultMeta?.filterCount ?? 0}개`;
        }
        const pageMeta = document.getElementById('logPageMeta');
        if (pageMeta) {
            pageMeta.textContent = data.resultMeta?.querySignature || data.pageInfoLabel || '페이지 메타 없음';
        }
    },

    renderPagination(data) {
        const pagination = document.getElementById('logPagination');
        if (!pagination) {
            return;
        }
        if (!data.totalPages) {
            pagination.innerHTML = '';
            return;
        }

        let html = '';
        for (let i = 0; i < data.totalPages; i += 1) {
            html += `
                <li class="page-item ${i === data.currentPage ? 'active' : ''}">
                    <a class="page-link" href="javascript:void(0);" onclick="AdminLogPage.goPage(${i})">${i + 1}</a>
                </li>
            `;
        }
        pagination.innerHTML = html;
    },

    async openDetail(logNo) {
        document.getElementById('logDetailBody').textContent = '데이터를 불러오는 중입니다...';
        this.modal.show();
        try {
            const res = await fetch(`/api/admin/logs/get?no=${logNo}`);
            if (!res.ok) {
                throw new Error(await CommonJS.extractErrorMessage(res, '상세 로그를 불러오지 못했습니다.'));
            }
            const data = await res.json();
            document.getElementById('logDetailBody').innerHTML = `
                <div class="mb-2"><strong>로그 번호</strong> ${data.logNo}</div>
                <div class="mb-2"><strong>관리자</strong> ${data.adminName} (#${data.adminNo})</div>
                <div class="mb-2"><strong>작업 종류</strong> ${data.actionType}</div>
                <div class="mb-2"><strong>대상</strong> ${data.targetPath ? `<a class="text-decoration-none" href="${data.targetPath}">${data.targetLabel}</a>` : (data.targetLabel || '-')}</div>
                <div class="mb-2"><strong>IP 주소</strong> ${data.ipAddress}</div>
                <div><strong>작업 일시</strong> ${data.actionDtm}</div>
            `;
        } catch (err) {
            document.getElementById('logDetailBody').innerHTML = `<div class="text-danger">${err.message}</div>`;
        }
    },

    setMetaText(message) {
        document.getElementById('logMetaText').textContent = message;
    },

    goPage(page) {
        this.state.page = page;
        this.getList();
    },

    resetFilters() {
        document.getElementById('logAdminNo').value = '';
        document.getElementById('logActionType').value = '';
        document.getElementById('logTargetId').value = '';
        document.getElementById('logStartDate').value = '';
        document.getElementById('logEndDate').value = '';
        document.getElementById('logPageSize').value = '20';
        this.state.page = 0;
        this.state.size = 20;
        this.getList();
    },

    applyQuickFilter(actionType) {
        document.getElementById('logActionType').value = actionType || '';
        this.state.page = 0;
        this.getList();
    }
};

document.addEventListener('DOMContentLoaded', () => AdminLogPage.init());
