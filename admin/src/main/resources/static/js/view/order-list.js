const OrderList = {
    initialized: false,
    maxDateRangeDays: 92,
    maxKeywordLength: 50,
    state: null,
    operationPolicy: null,
    isLoading: false,
    isExporting: false,

    init() {
        if (this.initialized) return;
        this.initialized = true;
        this.state = this.readStateFromUrl();
        this.syncFilterFields();
        CommonJS.bindMainLogoNavigation(this.state.returnTo || '/admin/orders');
        CommonJS.renderSourceContextNotice({ noticeId: 'orderListSourceContextNotice', source: this.state.source });
        this.bindEvents();
        this.applyOperationPolicy();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));
        this.getList();
    },

    bindEvents() {
        // 필터 적용 버튼
        document.getElementById('btnFilter')?.addEventListener('click', () => {
            this.state.page = 0;
            this.captureFilterState();
            if (!this.validateDateRange()) {
                return;
            }
            this.pushState();
            this.getList();
        });

        document.getElementById('btnResetFilter')?.addEventListener('click', () => {
            this.resetFilters();
            this.pushState();
            this.getList();
        });

        document.getElementById('pageSize')?.addEventListener('change', () => {
            this.state.page = 0;
            this.state.size = Number(document.getElementById('pageSize')?.value || 10);
            this.pushState();
            this.getList();
        });

        document.getElementById('btnExportOrders')?.addEventListener('click', async () => {
            if (this.isExporting) {
                return;
            }
            if (this.operationPolicy && CommonJS.isOrderExportBlocked(this.operationPolicy)) {
                await CommonJS.alert(CommonJS.getOrderExportBlockedReason(), '알림', 'warning');
                return;
            }
            this.captureFilterState();
            if (!this.validateDateRange()) {
                return;
            }
            try {
                this.isExporting = true;
                this.setBusyExportButton(true);
                const params = this.buildStateParams();
                params.delete('page');
                params.delete('size');
                await CommonJS.downloadFile(`/api/admin/orders/export?${params.toString()}`, 'orders.csv');
            } catch (error) {
                await CommonJS.alert(error.message, '오류', 'error');
            } finally {
                this.isExporting = false;
                this.setBusyExportButton(false);
            }
        });

        document.querySelectorAll('[data-date-preset]').forEach((button) => {
            button.addEventListener('click', () => {
                this.applyDatePreset(Number(button.dataset.datePreset || 0));
                this.pushState();
                this.getList();
            });
        });

        document.getElementById('startDate')?.addEventListener('change', () => this.syncDatePresetButtons());
        document.getElementById('endDate')?.addEventListener('change', () => this.syncDatePresetButtons());

        document.getElementById('orderStatusSummaryRow')?.addEventListener('click', (event) => {
            const summaryButton = event.target.closest('[data-role="apply-order-status-summary"]');
            if (!summaryButton) {
                return;
            }
            this.applyStatusSummaryFilter(summaryButton.dataset.statusCode || '');
        });

        document.getElementById('orderListTableBody')?.addEventListener('click', (event) => {
            const detailButton = event.target.closest('[data-role="go-order-detail"]');
            if (!detailButton) {
                return;
            }
            location.href = this.buildDetailUrl(detailButton.dataset.orderNo);
        });

        document.getElementById('pagination')?.addEventListener('click', (event) => {
            const pageButton = event.target.closest('[data-role="go-order-page"]');
            if (!pageButton) {
                return;
            }
            this.goPage(Number(pageButton.dataset.page));
        });

        // 검색 조건은 URL에 남겨서 새로고침/뒤로가기 때도 같은 문맥을 유지한다.
        document.getElementById('searchKeyword')?.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.state.page = 0;
                this.captureFilterState();
                if (!this.validateDateRange()) {
                    return;
                }
                this.pushState();
                this.getList();
            }
        });

        window.addEventListener('popstate', () => {
            this.state = this.readStateFromUrl();
            this.syncFilterFields();
            CommonJS.bindMainLogoNavigation(this.state.returnTo || '/admin/orders');
            CommonJS.renderSourceContextNotice({ noticeId: 'orderListSourceContextNotice', source: this.state.source });
            this.getList();
        });
    },

    async applyOperationPolicy(settings = null) {
        const exportButton = document.getElementById('btnExportOrders');
        try {
            this.operationPolicy = settings || await CommonJS.fetchSystemSettings();
            CommonJS.setButtonDisabled(
                exportButton,
                CommonJS.isOrderExportBlocked(this.operationPolicy),
                CommonJS.getOrderExportBlockedReason()
            );
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
    },

    async getList() {
        if (this.isLoading) {
            return;
        }
        this.captureFilterState();
        if (!this.validateDateRange()) {
            this.renderSummaryState('error', '상태별 집계를 불러오지 않았습니다.', '조회 기간과 검색 조건을 다시 확인해주세요.');
            this.renderTableState('error', '주문 내역을 불러오지 않았습니다.', '조회 기간과 검색 조건을 다시 확인해주세요.');
            this.renderMeta({
                totalElements: 0,
                currentPage: this.state.page,
                totalPages: 0,
                errorMessage: '조회 조건 확인 필요'
            });
            return;
        }
        const params = new URLSearchParams({
            page: this.state.page,
            size: this.state.size,
            status: this.state.status,
            startDate: this.state.startDate,
            endDate: this.state.endDate,
            searchKeyword: this.state.searchKeyword
        });
        const tbody = document.getElementById('orderListTableBody');
        if (tbody) {
            this.renderTableState('loading', '주문 내역을 불러오는 중입니다.', '현재 필터 기준 주문 목록과 상태 집계를 함께 계산하고 있습니다.');
        }
        this.renderSummaryState('loading', '상태별 집계를 불러오는 중입니다.', '현재 필터 기준 주문 상태별 건수를 계산하고 있습니다.');

        try {
            this.isLoading = true;
            this.renderMeta({
                totalElements: 0,
                currentPage: this.state.page,
                totalPages: 0
            });
            const res = await fetch(`/api/admin/orders/list?${params}`);
            if (!res.ok) {
                throw new Error(await CommonJS.extractErrorMessage(res, '데이터를 불러오는 중 오류가 발생했습니다.'));
            }

            const data = await res.json();
            this.renderStatusSummaries(data.statusSummaries || []);
            this.renderList(data.orders);
            this.renderMeta(data);
            this.renderPagination(data);
        } catch (err) {
            console.error('주문 목록 로드 실패:', err);
            this.renderSummaryState('error', '상태별 집계를 불러오지 못했습니다.', '잠시 후 다시 시도하거나 필터 조건을 조정해주세요.');
            this.renderTableState('error', '주문 내역을 불러오지 못했습니다.', '잠시 후 다시 시도하거나 주문 검색 조건을 조정해주세요.');
            this.renderMeta({
                totalElements: 0,
                currentPage: this.state.page,
                totalPages: 0,
                errorMessage: err.message
            });
            await CommonJS.alert(err.message || '데이터를 불러오는 중 오류가 발생했습니다.', '오류', 'error');
        } finally {
            this.isLoading = false;
        }
    },

    renderStatusSummaries(items) {
        const container = document.getElementById('orderStatusSummaryRow');
        if (!container) return;

        const totalCount = (items || []).reduce((sum, item) => sum + Number(item.count || 0), 0);
        const selectedStatus = this.state.status || '';

        if (!items.length) {
            this.renderSummaryState('empty', '현재 필터에 해당하는 상태별 집계가 없습니다.', '기간이나 검색 조건을 조정하면 다른 주문 상태를 확인할 수 있습니다.');
            return;
        }

        const cards = [
            {
                statusCode: '',
                statusDesc: '전체 주문',
                count: totalCount,
                hint: '현재 조건 기준 전체 주문'
            },
            ...items.map((item) => ({
                statusCode: item.statusCode || '',
                statusDesc: item.statusDesc,
                count: Number(item.count || 0),
                hint: `${item.statusDesc} 상태 주문`
            }))
        ];

        container.innerHTML = cards.map((item) => {
            const active = (item.statusCode || '') === selectedStatus;
            const percentage = totalCount > 0 ? Math.round((Number(item.count || 0) / totalCount) * 100) : 0;
            return `
                <button type="button"
                        class="admin-summary-card order-status-summary-card text-start ${active ? 'stat-card-active' : ''}"
                        data-role="apply-order-status-summary"
                        data-status-code="${item.statusCode || ''}">
                    <div class="admin-summary-card__label">${item.statusDesc}</div>
                    <div class="admin-summary-card__value">${Number(item.count || 0).toLocaleString()}건</div>
                    <div class="admin-summary-card__hint">${item.hint} · ${percentage}%</div>
                </button>
            `;
        }).join('');
    },

    renderList(items) {
        const tbody = document.getElementById('orderListTableBody');
        if (!tbody) return;

        if (!items || items.length === 0) {
            this.renderTableState('empty', '주문 내역이 없습니다.', '기간, 상태, 검색어 조건을 조정하거나 빠른 상태 카드를 눌러 다른 주문 문맥을 확인하세요.');
            return;
        }

        tbody.innerHTML = items.map(item => {
            const statusMeta = CommonJS.getOrderStatusMeta(item.statusCode);

            return `
                <tr>
                    <td class="ps-4"><span class="order-id">${item.orderNum}</span></td>
                    <td>${item.orderDt}</td>
                    <td>
                        <div class="buyer-info">
                            <div class="fw-bold">${item.buyerName}</div>
                            <div class="text-muted small">${item.buyerPhone}</div>
                        </div>
                    </td>
                    <td>${item.productSummary}</td>
                    <td><strong>${item.totalAmount}</strong></td>
                    <td><span class="badge ${statusMeta.badgeClass}">${item.statusDesc}</span></td>
                    <td class="text-end pe-4">
                        <button type="button" class="btn btn-sm btn-outline-secondary" data-role="go-order-detail" data-order-no="${item.orderNo}">상세보기</button>
                    </td>
                </tr>
            `;
        }).join('');
    },

    renderPagination(data) {
        const { totalPages, currentPage: curr, totalElements } = data;
        const pagination = document.getElementById('pagination');
        if (!pagination) return;

        let html = '';
        for (let i = 0; i < totalPages; i++) {
            html += `
                <li class="page-item ${i === curr ? 'active' : ''}">
                    <button type="button" class="page-link" data-role="go-order-page" data-page="${i}">${i + 1}</button>
                </li>`;
        }
        pagination.innerHTML = html;

        const infoEl = document.getElementById('pageInfoText');
        if (infoEl) {
            infoEl.textContent = this.buildPageInfoLabel(data);
        }

        const totalCountEl = document.getElementById('totalElementsCount');
        if (totalCountEl) {
            totalCountEl.textContent = `전체 ${Number(totalElements || 0).toLocaleString()}건`;
        }
    },

    renderSummaryState(type, title, description) {
        const container = document.getElementById('orderStatusSummaryRow');
        if (!container) return;

        if (type === 'loading') {
            container.innerHTML = `
                <div class="product-loading-state py-4">
                    <div class="spinner-border spinner-border-sm text-primary" role="status" aria-hidden="true"></div>
                    <strong>${title}</strong>
                    <p>${description}</p>
                </div>
            `;
            return;
        }

        const iconClass = type === 'error' ? 'fa-triangle-exclamation text-danger' : 'fa-chart-pie text-primary';
        container.innerHTML = `
            <div class="product-empty-state py-4">
                <div class="product-empty-state__icon">
                    <i class="fa-solid ${iconClass}"></i>
                </div>
                <strong>${title}</strong>
                <p>${description}</p>
            </div>
        `;
    },

    renderTableState(type, title, description) {
        const tbody = document.getElementById('orderListTableBody');
        if (!tbody) return;

        const stateMarkup = type === 'loading'
            ? `
                <div class="product-loading-state">
                    <div class="spinner-border spinner-border-sm text-primary" role="status" aria-hidden="true"></div>
                    <strong>${title}</strong>
                    <p>${description}</p>
                </div>
            `
            : `
                <div class="product-empty-state">
                    <div class="product-empty-state__icon">
                        <i class="fa-solid ${type === 'error' ? 'fa-triangle-exclamation text-danger' : 'fa-box-open text-primary'}"></i>
                    </div>
                    <strong>${title}</strong>
                    <p>${description}</p>
                </div>
            `;

        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="py-5">
                    ${stateMarkup}
                </td>
            </tr>
        `;
    },

    goPage(page) {
        this.state.page = page;
        this.pushState();
        this.getList();
    },

    captureFilterState() {
        this.state.status = document.getElementById('orderStatus')?.value || '';
        this.state.startDate = document.getElementById('startDate')?.value || '';
        this.state.endDate = document.getElementById('endDate')?.value || '';
        const rawKeyword = document.getElementById('searchKeyword')?.value || '';
        this.state.searchKeyword = rawKeyword.trim().replace(/\s+/g, ' ');
    },

    syncFilterFields() {
        const statusEl = document.getElementById('orderStatus');
        const startDateEl = document.getElementById('startDate');
        const endDateEl = document.getElementById('endDate');
        const searchKeywordEl = document.getElementById('searchKeyword');
        const pageSizeEl = document.getElementById('pageSize');

        if (statusEl) statusEl.value = this.state.status;
        if (startDateEl) startDateEl.value = this.state.startDate;
        if (endDateEl) endDateEl.value = this.state.endDate;
        if (searchKeywordEl) searchKeywordEl.value = this.state.searchKeyword;
        if (pageSizeEl) pageSizeEl.value = String(this.state.size);
        this.syncDatePresetButtons();
    },

    resetFilters() {
        this.state.page = 0;
        this.state.status = '';
        this.state.startDate = '';
        this.state.endDate = '';
        this.state.searchKeyword = '';
        this.syncFilterFields();
    },

    applyStatusSummaryFilter(statusCode) {
        this.state.page = 0;
        this.state.status = this.state.status === (statusCode || '') ? '' : (statusCode || '');
        this.syncFilterFields();
        this.pushState();
        this.getList();
    },

    applyDatePreset(days) {
        const today = new Date();
        const endDate = this.formatDate(today);
        const startDate = new Date(today);

        startDate.setDate(today.getDate() - Math.max(days - 1, 0));

        this.state.page = 0;
        this.state.startDate = this.formatDate(startDate);
        this.state.endDate = endDate;
        this.syncFilterFields();
    },

    renderMeta(data = {}) {
        const totalElements = Number(data.totalElements || 0);
        const resultLabel = data.errorMessage
            ? data.errorMessage
            : (totalElements === 0 ? '조회 결과 없음' : `검색 결과 ${totalElements.toLocaleString()}건`);
        const pageInfoLabel = data.errorMessage
            ? '페이지 메타 확인 불가'
            : this.buildPageInfoLabel(data);
        const querySignature = this.buildQuerySignature();

        CommonJS.renderListMeta({
            metaTextId: 'orderMetaText',
            filterMetaId: 'orderFilterMeta',
            resultMetaId: 'orderResultMeta',
            pageMetaId: 'orderPageMeta',
            resultLabel,
            filterCount: this.countActiveFilters(),
            querySignature,
            pageInfoLabel,
            filterPrefix: '필터',
            defaultResultText: '결과 메타 없음',
            defaultPageText: '페이지 메타 없음'
        });

        const summaryGuideEl = document.getElementById('orderSummaryGuideText');
        if (summaryGuideEl) {
            summaryGuideEl.textContent = this.state.status
                ? `상태 요약 카드를 다시 누르면 빠른 상태 필터를 해제합니다. 현재 선택: ${this.resolveStatusLabel(this.state.status)}`
                : '상태 요약 카드를 누르면 해당 상태로 바로 필터링합니다.';
        }
    },

    countActiveFilters() {
        let count = 0;
        if (this.state.status) count += 1;
        if (this.state.startDate) count += 1;
        if (this.state.endDate) count += 1;
        if (this.state.searchKeyword) count += 1;
        return count;
    },

    buildQuerySignature() {
        const tokens = ['주문 최신순'];
        if (this.state.status) {
            tokens.push(`상태=${this.resolveStatusLabel(this.state.status)}`);
        }
        if (this.state.startDate || this.state.endDate) {
            tokens.push(`기간=${this.state.startDate || '시작 미지정'}~${this.state.endDate || '종료 미지정'}`);
        }
        if (this.state.searchKeyword) {
            tokens.push(`검색=${this.state.searchKeyword}`);
        }
        return tokens.join(' · ');
    },

    buildPageInfoLabel(data = {}) {
        const totalElements = Number(data.totalElements || 0);
        if (totalElements === 0) {
            return '조건에 맞는 주문이 없습니다.';
        }

        const size = Number(this.state.size || data.size || 10);
        const currentPage = Number(data.currentPage ?? this.state.page ?? 0);
        const totalPages = Math.max(Number(data.totalPages || 0), 1);
        const rangeStart = currentPage * size + 1;
        const visibleCount = Array.isArray(data.orders) ? data.orders.length : Math.min(size, totalElements - currentPage * size);
        const rangeEnd = Math.min(totalElements, rangeStart + Math.max(visibleCount, 0) - 1);
        return `${rangeStart}-${rangeEnd} / ${totalElements.toLocaleString()}건 · ${totalPages}페이지`;
    },

    resolveStatusLabel(statusCode) {
        const labels = {
            ORDERED: '주문완료',
            PAID: '결제완료',
            PREPARING: '배송준비',
            SHIPPED: '배송중',
            DELIVERED: '배송완료',
            CANCELLED: '주문취소'
        };
        return labels[statusCode] || '전체 상태';
    },

    syncDatePresetButtons() {
        const activePreset = this.resolveActiveDatePreset();
        document.querySelectorAll('[data-date-preset]').forEach((button) => {
            const isActive = Number(button.dataset.datePreset || 0) === activePreset;
            button.classList.toggle('btn-dark', isActive);
            button.classList.toggle('text-white', isActive);
            button.classList.toggle('btn-outline-light', !isActive);
            button.classList.toggle('text-muted', !isActive);
        });
    },

    resolveActiveDatePreset() {
        if (!this.state.startDate || !this.state.endDate) {
            return null;
        }

        const today = new Date();
        const todayText = this.formatDate(today);
        if (this.state.endDate !== todayText) {
            return null;
        }

        const diff = Math.floor((new Date(`${this.state.endDate}T00:00:00`) - new Date(`${this.state.startDate}T00:00:00`)) / (1000 * 60 * 60 * 24)) + 1;
        return [1, 7, 30].includes(diff) ? diff : null;
    },

    pushState() {
        const newUrl = `${window.location.pathname}?${this.buildQueryString()}`;
        window.history.pushState({ path: newUrl }, '', newUrl);
    },

    buildDetailUrl(orderNo) {
        const returnTo = encodeURIComponent(`${window.location.pathname}?${this.buildQueryString()}`);
        return `/admin/orders/get?no=${orderNo}&source=order-list&returnTo=${returnTo}`;
    },

    buildQueryString() {
        return this.buildStateParams().toString();
    },

    validateDateRange() {
        if (this.state.searchKeyword && this.state.searchKeyword.length > this.maxKeywordLength) {
            void CommonJS.alert(`검색어는 ${this.maxKeywordLength}자 이내로 입력할 수 있습니다.`, '알림', 'warning');
            return false;
        }

        if (!this.state.startDate || !this.state.endDate) {
            return true;
        }

        if (this.state.startDate > this.state.endDate) {
            void CommonJS.alert('시작일은 종료일보다 늦을 수 없습니다.', '알림', 'warning');
            return false;
        }

        const startDate = new Date(`${this.state.startDate}T00:00:00`);
        const endDate = new Date(`${this.state.endDate}T00:00:00`);
        const diffDays = Math.floor((endDate - startDate) / (1000 * 60 * 60 * 24));

        if (diffDays > this.maxDateRangeDays) {
            void CommonJS.alert(`조회 기간은 ${this.maxDateRangeDays + 1}일 이내로만 설정할 수 있습니다.`, '알림', 'warning');
            return false;
        }

        return true;
    },

    setBusyExportButton(isBusy) {
        const exportButton = document.getElementById('btnExportOrders');
        if (!exportButton) return;
        if (isBusy) {
            if (!exportButton.dataset.originalText) {
                exportButton.dataset.originalText = exportButton.textContent;
            }
            exportButton.disabled = true;
            exportButton.textContent = '내보내는 중...';
            return;
        }
        exportButton.disabled = false;
        if (exportButton.dataset.originalText) {
            exportButton.textContent = exportButton.dataset.originalText;
            delete exportButton.dataset.originalText;
        }
    },

    formatDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        const page = Number(params.get('page') || 0);
        const size = Number(params.get('size') || 10);
        return {
            page: Number.isFinite(page) && page >= 0 ? page : 0,
            size: Number.isFinite(size) && size > 0 ? size : 10,
            status: params.get('status') || '',
            startDate: params.get('startDate') || '',
            endDate: params.get('endDate') || '',
            searchKeyword: (params.get('searchKeyword') || '').trim().replace(/\s+/g, ' '),
            source: params.get('source') || '',
            returnTo: params.get('returnTo') || ''
        };
    },

    buildStateParams() {
        // URL state를 한 곳에서만 조립해야 필터 항목이 늘어나도 popstate와 상세 복귀가 같이 유지됩니다.
        const params = new URLSearchParams({
            page: this.state.page,
            size: this.state.size
        });

        if (this.state.status) params.set('status', this.state.status);
        if (this.state.startDate) params.set('startDate', this.state.startDate);
        if (this.state.endDate) params.set('endDate', this.state.endDate);
        if (this.state.searchKeyword) params.set('searchKeyword', this.state.searchKeyword);
        if (this.state.source) params.set('source', this.state.source);
        if (this.state.returnTo) params.set('returnTo', this.state.returnTo);

        return params;
    }
};

document.addEventListener('DOMContentLoaded', () => OrderList.init());
