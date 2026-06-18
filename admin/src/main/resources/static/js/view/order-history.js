const OrderHistoryPage = {
    initialized: false,
    state: {
        page: 0,
        size: 20,
        returnTo: '/admin/orders/list',
        source: '',
        historyNo: ''
    },
    isExporting: false,

    init() {
        if (this.initialized) return;
        this.initialized = true;
        this.bindEvents();
        this.readStateFromUrl();
        this.syncReturnLinks();
        this.loadHistory();
    },

    bindEvents() {
        document.getElementById('btnSearchHistory')?.addEventListener('click', () => {
            this.state.page = 0;
            this.loadHistory();
        });
        document.getElementById('btnResetOrderHistory')?.addEventListener('click', () => this.resetFilters());
        document.getElementById('historyPageSize')?.addEventListener('change', () => {
            this.state.page = 0;
            this.state.size = Number(document.getElementById('historyPageSize')?.value || 20);
            this.loadHistory();
        });
        document.getElementById('historyKeyword')?.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                this.state.page = 0;
                this.loadHistory();
            }
        });
        document.getElementById('historyActorKeyword')?.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                this.state.page = 0;
                this.loadHistory();
            }
        });
        document.getElementById('historyOrderType')?.addEventListener('change', () => {
            this.state.page = 0;
            this.loadHistory();
        });
        document.querySelectorAll('.history-quick-filter[data-action-type]').forEach((button) => {
            button.addEventListener('click', () => {
                document.getElementById('historyActionType').value = button.dataset.actionType || '';
                this.state.page = 0;
                this.syncQuickFilterState();
                this.loadHistory();
            });
        });
        document.querySelectorAll('[data-order-history-date-preset]').forEach((button) => {
            button.addEventListener('click', () => this.applyDatePreset(button.dataset.orderHistoryDatePreset));
        });
        document.getElementById('btnBackToOrderHistorySource')?.addEventListener('click', () => {
            window.location.href = this.state.returnTo;
        });
        document.getElementById('btnExportOrderHistory')?.addEventListener('click', async () => {
            if (this.isExporting) {
                return;
            }

            try {
                this.isExporting = true;
                window.location.href = `/api/admin/orders/history/export?${this.buildExportParams().toString()}`;
            } finally {
                this.isExporting = false;
            }
        });
        window.addEventListener('popstate', () => {
            this.readStateFromUrl();
            this.syncReturnLinks();
            this.loadHistory();
        });
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        document.getElementById('historyOrderNo').value = params.get('orderNo') || '';
        document.getElementById('historyActionType').value = params.get('actionType') || '';
        document.getElementById('historyStartDate').value = params.get('startDate') || '';
        document.getElementById('historyEndDate').value = params.get('endDate') || '';
        document.getElementById('historyKeyword').value = params.get('keyword') || '';
        document.getElementById('historyActorKeyword').value = params.get('actorKeyword') || '';
        document.getElementById('historyOrderType').value = params.get('orderType') || 'latest';
        this.state.page = Number(params.get('page') || 0);
        this.state.size = Number(params.get('size') || 20);
        this.state.returnTo = params.get('returnTo') || '/admin/orders/list';
        this.state.source = params.get('source') || '';
        this.state.historyNo = params.get('historyNo') || '';
        document.getElementById('historyPageSize').value = String(this.state.size);
        this.syncQuickFilterState();
        CommonJS.bindMainLogoNavigation(this.state.returnTo);
        CommonJS.renderSourceContextNotice({ noticeId: 'orderHistorySourceContextNotice', source: this.state.source });
    },

    buildParams() {
        const params = new URLSearchParams();
        const orderNo = document.getElementById('historyOrderNo').value.trim();
        const actionType = document.getElementById('historyActionType').value;
        const startDate = document.getElementById('historyStartDate').value;
        const endDate = document.getElementById('historyEndDate').value;
        const keyword = CommonJS.normalizeOptionalText(document.getElementById('historyKeyword').value);
        const actorKeyword = CommonJS.normalizeOptionalText(document.getElementById('historyActorKeyword').value);
        const orderType = document.getElementById('historyOrderType').value || 'latest';

        if (orderNo) params.set('orderNo', orderNo);
        if (actionType) params.set('actionType', actionType);
        if (startDate) params.set('startDate', startDate);
        if (endDate) params.set('endDate', endDate);
        if (keyword) params.set('keyword', keyword);
        if (actorKeyword) params.set('actorKeyword', actorKeyword);
        if (orderType !== 'latest') params.set('orderType', orderType);
        if (this.state.returnTo && this.state.returnTo !== '/admin/orders/list') params.set('returnTo', this.state.returnTo);
        if (this.state.source) params.set('source', this.state.source);
        if (this.state.historyNo) params.set('historyNo', this.state.historyNo);
        params.set('page', String(this.state.page));
        params.set('size', String(this.state.size));
        return params;
    },

    buildExportParams() {
        const params = this.buildParams();
        params.delete('page');
        params.delete('size');
        params.delete('historyNo');
        return params;
    },

    async loadHistory() {
        const params = this.buildParams();
        history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
        this.setMetaText('데이터를 불러오는 중입니다...');

        try {
            const response = await fetch(`/api/admin/orders/history/list?${params.toString()}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '주문 처리 이력을 불러오지 못했습니다.'));
            }
            const data = await response.json();
            this.renderList(data.items || []);
            this.renderMeta(data);
            this.renderPagination(data);
            this.renderResultSummary(data);
            this.highlightHistoryRow(this.state.historyNo);
            this.consumeDeepLinkHistoryNo(data.items || []);
        } catch (error) {
            this.renderError(error.message);
        }
    },

    renderList(items) {
        const tbody = document.getElementById('orderHistoryBody');
        const returnTo = encodeURIComponent(window.location.pathname + window.location.search);
        if (!items.length) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center py-5 text-muted">조회된 주문 처리 이력이 없습니다.</td></tr>';
            return;
        }

        tbody.innerHTML = items.map((item) => `
            <tr data-order-history-row="${item.historyNo}">
                <td class="ps-4 text-muted small">${item.historyNo}</td>
                <td><a class="text-decoration-none fw-bold" href="/admin/orders/get?no=${item.orderNo}&returnTo=${returnTo}${this.state.source ? `&source=${encodeURIComponent(this.state.source)}` : ''}">${item.orderNo}</a></td>
                <td><span class="badge bg-dark">${item.actionLabel}</span></td>
                <td>
                    <div class="fw-semibold">상태 ${item.beforeStatusDesc || '-'} -> ${item.afterStatusDesc || '-'}</div>
                    ${item.reason ? `<div class="text-muted small">사유 ${CommonJS.escapeHtml(item.reason)}</div>` : ''}
                    ${item.adminMemoSnapshot ? `<div class="text-muted small">메모 ${CommonJS.escapeHtml(item.adminMemoSnapshot)}</div>` : ''}
                    ${(item.deliveryCompany || item.trackingNum) ? `<div class="text-muted small">배송 ${CommonJS.escapeHtml(item.deliveryCompany || '-')} / ${CommonJS.escapeHtml(item.trackingNum || '-')}</div>` : ''}
                    ${item.activityLogPath ? `<div class="small"><a class="text-decoration-none" href="${item.activityLogPath}">${item.activityLogLabel || '활동 로그 보기'}</a></div>` : ''}
                </td>
                <td>${item.actorName}${item.actorNo ? ` <span class="text-muted small">(#${item.actorNo})</span>` : ''}</td>
                <td class="text-end pe-4 small text-muted">${item.actionDtm || '-'}</td>
            </tr>
        `).join('');
    },

    renderMeta(data) {
        this.setMetaText(data.pageInfoLabel || `${data.rangeStart}-${data.rangeEnd} / ${data.totalElements}건`);
        const filterMeta = document.getElementById('orderHistoryFilterMeta');
        if (filterMeta) {
            filterMeta.textContent = `적용 필터 ${data.resultMeta?.filterCount ?? 0}개`;
        }
        const pageMeta = document.getElementById('orderHistoryPageMeta');
        if (pageMeta) {
            pageMeta.textContent = data.resultMeta?.pageInfoLabel || data.pageInfoLabel || '페이지 메타 없음';
        }
    },

    renderPagination(data) {
        const pagination = document.getElementById('historyPagination');
        if (!pagination) return;
        if (!data.totalPages) {
            pagination.innerHTML = '';
            return;
        }

        let html = '';
        for (let i = 0; i < data.totalPages; i += 1) {
            html += `
                <li class="page-item ${i === data.currentPage ? 'active' : ''}">
                    <button type="button" class="page-link" data-role="go-order-history-page" data-page="${i}">${i + 1}</button>
                </li>
            `;
        }
        pagination.innerHTML = html;
        pagination.querySelectorAll('[data-role="go-order-history-page"]').forEach((button) => {
            button.addEventListener('click', () => this.goPage(Number(button.dataset.page)));
        });
    },

    renderError(message) {
        document.getElementById('orderHistoryBody').innerHTML =
            `<tr><td colspan="6" class="text-center py-5 text-danger">${message}</td></tr>`;
        this.setMetaText('이력 조회 실패');
        const filterMeta = document.getElementById('orderHistoryFilterMeta');
        if (filterMeta) {
            filterMeta.textContent = '적용 필터 확인 불가';
        }
        const pageMeta = document.getElementById('orderHistoryPageMeta');
        if (pageMeta) {
            pageMeta.textContent = '페이지 메타 확인 불가';
        }
        const summary = document.getElementById('orderHistoryResultSummary');
        if (summary) {
            summary.textContent = '주문 처리 이력 조회에 실패했습니다.';
        }
        document.getElementById('historyPagination').innerHTML = '';
    },

    setMetaText(message) {
        document.getElementById('historyMetaText').textContent = message;
    },

    goPage(page) {
        this.state.page = page;
        this.loadHistory();
    },

    resetFilters() {
        document.getElementById('historyOrderNo').value = '';
        document.getElementById('historyActionType').value = '';
        document.getElementById('historyStartDate').value = '';
        document.getElementById('historyEndDate').value = '';
        document.getElementById('historyKeyword').value = '';
        document.getElementById('historyActorKeyword').value = '';
        document.getElementById('historyOrderType').value = 'latest';
        document.getElementById('historyPageSize').value = '20';
        this.state.historyNo = '';
        this.state.page = 0;
        this.state.size = 20;
        this.syncQuickFilterState();
        this.loadHistory();
    },

    syncQuickFilterState() {
        const currentActionType = document.getElementById('historyActionType')?.value || '';
        document.querySelectorAll('.history-quick-filter[data-action-type]').forEach((button) => {
            button.classList.toggle('active', (button.dataset.actionType || '') === currentActionType);
            button.classList.toggle('btn-dark', (button.dataset.actionType || '') === currentActionType);
            button.classList.toggle('btn-outline-dark', (button.dataset.actionType || '') !== currentActionType);
        });
    },

    syncReturnLinks() {
        const breadcrumbLink = document.getElementById('orderHistoryBreadcrumbLink');
        if (breadcrumbLink) {
            breadcrumbLink.href = this.state.returnTo;
        }
    },

    renderResultSummary(data) {
        const summary = document.getElementById('orderHistoryResultSummary');
        if (!summary) {
            return;
        }
        summary.textContent = data.resultMeta?.querySignature || '현재 적용된 필터를 기준으로 주문 처리 이력을 조회합니다.';
    },

    applyDatePreset(preset) {
        const startDateInput = document.getElementById('historyStartDate');
        const endDateInput = document.getElementById('historyEndDate');
        if (!startDateInput || !endDateInput) {
            return;
        }

        const today = new Date();
        const formatDate = (value) => {
            const year = value.getFullYear();
            const month = String(value.getMonth() + 1).padStart(2, '0');
            const day = String(value.getDate()).padStart(2, '0');
            return `${year}-${month}-${day}`;
        };

        if (preset === 'clear') {
            startDateInput.value = '';
            endDateInput.value = '';
        } else {
            const startDate = new Date(today);
            if (preset === '7days') {
                startDate.setDate(startDate.getDate() - 6);
            } else if (preset === '30days') {
                startDate.setDate(startDate.getDate() - 29);
            }
            startDateInput.value = formatDate(startDate);
            endDateInput.value = formatDate(today);
        }

        this.state.page = 0;
        this.loadHistory();
    },

    highlightHistoryRow(historyNo) {
        const targetHistoryNo = Number(historyNo || 0);
        document.querySelectorAll('[data-order-history-row]').forEach((row) => {
            const selected = Number(row.dataset.orderHistoryRow) === targetHistoryNo;
            row.classList.toggle('table-active', selected);
            if (selected) {
                row.scrollIntoView({ block: 'center', behavior: 'smooth' });
            }
        });
    },

    consumeDeepLinkHistoryNo(items) {
        if (!this.state.historyNo) {
            return;
        }
        const historyNo = Number(this.state.historyNo);
        if (!Number.isFinite(historyNo) || historyNo <= 0) {
            this.state.historyNo = '';
        } else if (items.some((item) => item.historyNo === historyNo)) {
            this.state.historyNo = '';
        } else {
            return;
        }
        history.replaceState(null, '', `${window.location.pathname}?${this.buildParams().toString()}`);
    }
};

document.addEventListener('DOMContentLoaded', () => OrderHistoryPage.init());
