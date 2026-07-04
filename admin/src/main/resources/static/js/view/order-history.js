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
                CommonJS.setButtonDisabled(document.getElementById('btnExportOrderHistory'), true, '내보내는 중입니다.');
                const startDate = document.getElementById('historyStartDate')?.value || '';
                const endDate = document.getElementById('historyEndDate')?.value || '';
                if (startDate && endDate && startDate > endDate) {
                    throw new Error('시작일은 종료일보다 늦을 수 없습니다.');
                }
                await CommonJS.downloadFile(`/api/admin/orders/history/export?${this.buildExportParams().toString()}`, 'order-history.csv');
            } catch (error) {
                await CommonJS.alert(error.message || '주문 처리 이력 CSV를 내보내지 못했습니다.', '오류', 'error');
            } finally {
                this.isExporting = false;
                CommonJS.setButtonDisabled(document.getElementById('btnExportOrderHistory'), false);
            }
        });
        window.addEventListener('popstate', () => {
            this.readStateFromUrl();
            this.syncReturnLinks();
            this.syncQuickFilterState();
            this.syncDatePresetState();
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
        document.getElementById('historyActorNo').value = params.get('actorNo') || '';
        document.getElementById('historyActorKeyword').value = params.get('actorKeyword') || '';
        document.getElementById('historyOrderType').value = params.get('orderType') || 'latest';
        this.state.page = this.normalizePage(params.get('page'));
        this.state.size = this.normalizePageSize(params.get('size'));
        this.state.returnTo = params.get('returnTo') || '/admin/orders/list';
        this.state.source = params.get('source') || '';
        this.state.historyNo = this.normalizeOptionalPositiveNumber(params.get('historyNo'));
        document.getElementById('historyPageSize').value = String(this.state.size);
        this.syncQuickFilterState();
        this.syncDatePresetState();
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
        const actorNo = document.getElementById('historyActorNo').value.trim();
        const actorKeyword = CommonJS.normalizeOptionalText(document.getElementById('historyActorKeyword').value);
        const orderType = document.getElementById('historyOrderType').value || 'latest';

        if (orderNo) params.set('orderNo', orderNo);
        if (actionType) params.set('actionType', actionType);
        if (startDate) params.set('startDate', startDate);
        if (endDate) params.set('endDate', endDate);
        if (keyword) params.set('keyword', keyword);
        if (actorNo) params.set('actorNo', actorNo);
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
        if (!this.validateState()) {
            return;
        }
        const startDate = document.getElementById('historyStartDate')?.value;
        const endDate = document.getElementById('historyEndDate')?.value;
        if (startDate && endDate && startDate > endDate) {
            this.renderError('시작일은 종료일보다 늦을 수 없습니다.');
            return;
        }
        const params = this.buildParams();
        history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
        this.setMetaText('주문 처리 이력을 불러오는 중입니다...');
        this.setResultMetaText('결과 메타를 계산하는 중입니다...');
        this.setPageMetaText('페이지 메타 계산 중');
        this.renderLoadingState();

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
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="py-5">
                        <div class="product-empty-state">
                            <i class="fas fa-receipt product-empty-state-icon"></i>
                            <strong>조건에 맞는 주문 처리 이력이 없습니다.</strong>
                            <p>${this.buildEmptyStateMessage()}</p>
                        </div>
                    </td>
                </tr>
            `;
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
                    ${item.activityLogPath ? `<div class="small"><a class="text-decoration-none" href="${this.buildLogPathFromBase(item.activityLogPath)}">${item.activityLogLabel || '활동 로그 보기'}</a></div>` : ''}
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
        this.setResultMetaText(data.resultMeta?.querySignature || '최신순');
        this.setPageMetaText(data.resultMeta?.pageInfoLabel || data.pageInfoLabel || '페이지 메타 없음');
    },

    buildLogPathFromBase(basePath) {
        if (!basePath) {
            return '';
        }
        const [path, rawQuery = ''] = basePath.split('?');
        const params = new URLSearchParams(rawQuery);
        params.set('returnTo', window.location.pathname + window.location.search);
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        return `${path}?${params.toString()}`;
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
        const tbody = document.getElementById('orderHistoryBody');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="py-5">
                        <div class="product-empty-state">
                            <div class="product-empty-state__icon text-danger">
                                <i class="fa-solid fa-triangle-exclamation"></i>
                            </div>
                            <strong>주문 처리 이력을 불러오지 못했습니다.</strong>
                            <p>${this.escapeHtml(message)}</p>
                        </div>
                    </td>
                </tr>
            `;
        }
        this.setMetaText('이력 조회 실패');
        const filterMeta = document.getElementById('orderHistoryFilterMeta');
        if (filterMeta) {
            filterMeta.textContent = '적용 필터 확인 불가';
        }
        this.setResultMetaText(message);
        this.setPageMetaText('페이지 메타 확인 불가');
        const summary = document.getElementById('orderHistoryResultSummary');
        if (summary) {
            summary.textContent = '주문 처리 이력 조회에 실패했습니다.';
        }
        document.getElementById('historyPagination').innerHTML = '';
    },

    setMetaText(message) {
        document.getElementById('historyMetaText').textContent = message;
    },

    setResultMetaText(message) {
        const resultMeta = document.getElementById('orderHistoryResultMeta');
        if (resultMeta) {
            resultMeta.textContent = message;
        }
    },

    setPageMetaText(message) {
        const pageMeta = document.getElementById('orderHistoryPageMeta');
        if (pageMeta) {
            pageMeta.textContent = message;
        }
    },

    goPage(page) {
        if (!Number.isInteger(page) || page < 0) {
            return;
        }
        this.state.page = page;
        this.loadHistory();
    },

    resetFilters() {
        document.getElementById('historyOrderNo').value = '';
        document.getElementById('historyActionType').value = '';
        document.getElementById('historyStartDate').value = '';
        document.getElementById('historyEndDate').value = '';
        document.getElementById('historyKeyword').value = '';
        document.getElementById('historyActorNo').value = '';
        document.getElementById('historyActorKeyword').value = '';
        document.getElementById('historyOrderType').value = 'latest';
        document.getElementById('historyPageSize').value = '20';
        this.state.historyNo = '';
        this.state.page = 0;
        this.state.size = 20;
        this.syncQuickFilterState();
        this.syncDatePresetState();
        this.loadHistory();
    },

    renderLoadingState() {
        const tbody = document.getElementById('orderHistoryBody');
        if (!tbody) {
            return;
        }
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="py-5">
                    <div class="product-loading-state">
                        <div class="spinner-border spinner-border-sm text-primary" role="status" aria-hidden="true"></div>
                        <strong>주문 처리 이력을 불러오는 중입니다.</strong>
                        <p>현재 필터 조건에 맞는 주문 처리 변경 내역을 조회하고 있습니다.</p>
                    </div>
                </td>
            </tr>
        `;
    },

    escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    },

    buildEmptyStateMessage() {
        const parts = [];
        const orderNo = document.getElementById('historyOrderNo')?.value.trim();
        const actionType = document.getElementById('historyActionType')?.value;
        const keyword = CommonJS.normalizeOptionalText(document.getElementById('historyKeyword')?.value);
        const actorNo = document.getElementById('historyActorNo')?.value.trim();
        const actorKeyword = CommonJS.normalizeOptionalText(document.getElementById('historyActorKeyword')?.value);
        const startDate = document.getElementById('historyStartDate')?.value;
        const endDate = document.getElementById('historyEndDate')?.value;

        if (orderNo) parts.push(`주문 번호 ${orderNo}`);
        if (actionType) parts.push(`작업 유형 ${actionType}`);
        if (keyword) parts.push(`검색어 "${keyword}"`);
        if (actorNo) parts.push(`작업자 번호 ${actorNo}`);
        if (actorKeyword) parts.push(`작업자 "${actorKeyword}"`);
        if (startDate || endDate) parts.push(`기간 ${startDate || '전체'} ~ ${endDate || '전체'}`);

        if (!parts.length) {
            return '주문 처리 이력이 아직 없거나, 현재 페이지에 표시할 데이터가 없습니다.';
        }

        return `${parts.join(', ')} 조건에 맞는 주문 처리 이력이 없습니다.`;
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
        const returnContext = CommonJS.getReturnContext(this.state.returnTo, '주문 관리');
        const breadcrumbLink = document.getElementById('orderHistoryBreadcrumbLink');
        if (breadcrumbLink) {
            breadcrumbLink.href = this.state.returnTo;
            breadcrumbLink.textContent = returnContext.label;
        }
        const backButton = document.getElementById('btnBackToOrderHistorySource');
        if (backButton) {
            backButton.textContent = `${returnContext.label}로 돌아가기`;
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
        this.syncDatePresetState();
        this.loadHistory();
    },

    syncDatePresetState() {
        const startDate = document.getElementById('historyStartDate')?.value || '';
        const endDate = document.getElementById('historyEndDate')?.value || '';
        const today = new Date();
        const formatDate = (value) => {
            const year = value.getFullYear();
            const month = String(value.getMonth() + 1).padStart(2, '0');
            const day = String(value.getDate()).padStart(2, '0');
            return `${year}-${month}-${day}`;
        };
        const todayLabel = formatDate(today);
        const sevenDaysAgo = new Date(today);
        sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 6);
        const thirtyDaysAgo = new Date(today);
        thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 29);

        document.querySelectorAll('[data-order-history-date-preset]').forEach((button) => {
            const preset = button.dataset.orderHistoryDatePreset;
            const active = (
                (preset === 'today' && startDate === todayLabel && endDate === todayLabel) ||
                (preset === '7days' && startDate === formatDate(sevenDaysAgo) && endDate === todayLabel) ||
                (preset === '30days' && startDate === formatDate(thirtyDaysAgo) && endDate === todayLabel) ||
                (preset === 'clear' && !startDate && !endDate)
            );
            button.classList.toggle('btn-secondary', active);
            button.classList.toggle('btn-outline-secondary', !active);
        });
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
    },

    validateState() {
        const orderNo = document.getElementById('historyOrderNo')?.value.trim() || '';
        const actorNo = document.getElementById('historyActorNo')?.value.trim() || '';
        const orderType = document.getElementById('historyOrderType')?.value || 'latest';
        if (orderNo && !this.isPositiveNumber(orderNo)) {
            void CommonJS.alert('주문 번호는 1 이상의 숫자만 입력할 수 있습니다.', '알림', 'warning');
            return false;
        }
        if (actorNo && !this.isPositiveNumber(actorNo)) {
            void CommonJS.alert('작업자 번호는 1 이상의 숫자만 입력할 수 있습니다.', '알림', 'warning');
            return false;
        }
        if (!['latest', 'oldest'].includes(orderType)) {
            void CommonJS.alert('정렬 조건이 올바르지 않습니다.', '알림', 'warning');
            return false;
        }
        return true;
    },

    normalizePage(page) {
        const parsed = Number(page);
        return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0;
    },

    normalizePageSize(size) {
        const parsed = Number(size);
        return Number.isInteger(parsed) && parsed > 0 ? parsed : 20;
    },

    normalizeOptionalPositiveNumber(value) {
        return this.isPositiveNumber(value) ? String(Number(value)) : '';
    },

    isPositiveNumber(value) {
        return /^\d+$/.test(String(value || '')) && Number(value) > 0;
    }
};

document.addEventListener('DOMContentLoaded', () => OrderHistoryPage.init());
