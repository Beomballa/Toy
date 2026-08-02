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
    listRequestId: 0,

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
            this.state.size = this.normalizePageSize(document.getElementById('historyPageSize')?.value);
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
                document.getElementById('historyActionType').value = this.normalizeActionType(button.dataset.actionType);
                this.state.page = 0;
                this.syncQuickFilterState();
                this.loadHistory();
            });
        });
        document.querySelectorAll('[data-order-history-date-preset]').forEach((button) => {
            button.addEventListener('click', () => this.applyDatePreset(this.normalizeDatePreset(button.dataset.orderHistoryDatePreset)));
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
                if (!this.validateState()) {
                    return;
                }
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
        document.getElementById('historyOrderNo').value = this.normalizeOptionalPositiveNumber(params.get('orderNo'));
        document.getElementById('historyActionType').value = this.normalizeActionType(params.get('actionType'));
        document.getElementById('historyStartDate').value = this.normalizeDateInput(params.get('startDate'));
        document.getElementById('historyEndDate').value = this.normalizeDateInput(params.get('endDate'));
        document.getElementById('historyKeyword').value = (CommonJS.normalizeOptionalText(params.get('keyword')) || '').slice(0, 50);
        document.getElementById('historyActorNo').value = this.normalizeOptionalPositiveNumber(params.get('actorNo'));
        document.getElementById('historyActorKeyword').value = (CommonJS.normalizeOptionalText(params.get('actorKeyword')) || '').slice(0, 50);
        document.getElementById('historyOrderType').value = this.normalizeOrderType(params.get('orderType'));
        this.state.page = this.normalizePage(params.get('page'));
        this.state.size = this.normalizePageSize(params.get('size'));
        this.state.returnTo = CommonJS.normalizeAdminReturnPath(params.get('returnTo'), '/admin/orders/list');
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
        const orderNo = this.normalizeOptionalPositiveNumber(document.getElementById('historyOrderNo').value);
        const actionType = this.normalizeActionType(document.getElementById('historyActionType').value);
        const startDate = document.getElementById('historyStartDate').value;
        const endDate = document.getElementById('historyEndDate').value;
        const keyword = CommonJS.normalizeOptionalText(document.getElementById('historyKeyword').value);
        const actorNo = this.normalizeOptionalPositiveNumber(document.getElementById('historyActorNo').value);
        const actorKeyword = CommonJS.normalizeOptionalText(document.getElementById('historyActorKeyword').value);
        const orderType = this.normalizeOrderType(document.getElementById('historyOrderType').value);

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
        this.state.size = this.normalizePageSize(document.getElementById('historyPageSize')?.value);
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
        const requestId = ++this.listRequestId;
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
            if (requestId !== this.listRequestId) {
                return;
            }
            const items = Array.isArray(data.items) ? data.items : [];
            const validItems = this.normalizeHistoryItems(items);
            this.renderList(validItems);
            this.renderMeta(data);
            this.renderPagination(data);
            this.renderResultSummary(data);
            this.highlightHistoryRow(this.state.historyNo);
            this.consumeDeepLinkHistoryNo(validItems);
        } catch (error) {
            if (requestId !== this.listRequestId) {
                return;
            }
            this.renderError(error.message);
        }
    },

    renderList(items) {
        const tbody = document.getElementById('orderHistoryBody');
        const returnTo = encodeURIComponent(window.location.pathname + window.location.search);
        const validItems = this.normalizeHistoryItems(items);
        if (!validItems.length) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="py-5">
                        <div class="product-empty-state">
                            <i class="fas fa-receipt product-empty-state-icon"></i>
                            <strong>조건에 맞는 주문 처리 이력이 없습니다.</strong>
                            <p>${this.escapeHtml(this.buildEmptyStateMessage())}</p>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = validItems.flatMap((item) => {
            const historyNo = this.normalizeOptionalPositiveNumber(item.historyNo);
            const orderNo = this.normalizeOptionalPositiveNumber(item.orderNo);
            if (!historyNo || !orderNo) return [];
            return [`
            <tr data-order-history-row="${historyNo}">
                <td class="ps-4 text-muted small">${historyNo}</td>
                <td><a class="text-decoration-none fw-bold" href="/admin/orders/get?no=${orderNo}&returnTo=${returnTo}${this.state.source ? `&source=${encodeURIComponent(this.state.source)}` : ''}">${orderNo}</a></td>
                <td><span class="badge bg-dark">${CommonJS.escapeHtml(item.actionLabel || '-')}</span></td>
                <td>
                    <div class="fw-semibold">상태 ${CommonJS.escapeHtml(item.beforeStatusDesc || '-')} -> ${CommonJS.escapeHtml(item.afterStatusDesc || '-')}</div>
                    ${item.reason ? `<div class="text-muted small">사유 ${CommonJS.escapeHtml(item.reason)}</div>` : ''}
                    ${item.adminMemoSnapshot ? `<div class="text-muted small">메모 ${CommonJS.escapeHtml(item.adminMemoSnapshot)}</div>` : ''}
                    ${(item.deliveryCompany || item.trackingNum) ? `<div class="text-muted small">배송 ${CommonJS.escapeHtml(item.deliveryCompany || '-')} / ${CommonJS.escapeHtml(item.trackingNum || '-')}</div>` : ''}
                    ${this.buildActivityLogLink(item)}
                </td>
                <td>${CommonJS.escapeHtml(item.actorName || '-')}${this.normalizeOptionalPositiveNumber(item.actorNo) ? ` <span class="text-muted small">(#${this.normalizeOptionalPositiveNumber(item.actorNo)})</span>` : ''}</td>
                <td class="text-end pe-4 small text-muted">${CommonJS.escapeHtml(item.actionDtm || '-')}</td>
            </tr>
        `];
        }).join('');
    },

    renderMeta(data) {
        this.setMetaText(data.pageInfoLabel || `${this.formatCount(data.rangeStart)}-${this.formatCount(data.rangeEnd)} / ${this.formatCount(data.totalElements)}건`);
        const filterMeta = document.getElementById('orderHistoryFilterMeta');
        if (filterMeta) {
            filterMeta.textContent = `적용 필터 ${this.normalizeNonNegativeInteger(data.resultMeta?.filterCount)}개`;
        }
        this.setResultMetaText(data.resultMeta?.querySignature || '최신순');
        this.setPageMetaText(data.resultMeta?.pageInfoLabel || data.pageInfoLabel || '페이지 메타 없음');
    },

    buildLogPathFromBase(basePath) {
        const safeBasePath = CommonJS.normalizeAdminReturnPath(basePath, '');
        if (!safeBasePath) {
            return '';
        }
        const [path, rawQuery = ''] = safeBasePath.split('?');
        const params = new URLSearchParams(rawQuery);
        params.set('returnTo', window.location.pathname + window.location.search);
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        return `${path}?${params.toString()}`;
    },

    buildActivityLogLink(item) {
        const path = this.buildLogPathFromBase(item.activityLogPath);
        if (!path) {
            return '';
        }
        return `<div class="small"><a class="text-decoration-none" href="${CommonJS.escapeHtml(path)}">${CommonJS.escapeHtml(item.activityLogLabel || '활동 로그 보기')}</a></div>`;
    },

    renderPagination(data) {
        const pagination = document.getElementById('historyPagination');
        if (!pagination) return;
        const totalPages = this.normalizeNonNegativeInteger(data.totalPages);
        const currentPage = Math.min(this.normalizePage(data.currentPage), Math.max(totalPages - 1, 0));
        if (totalPages <= 1) {
            pagination.innerHTML = '';
            return;
        }

        pagination.innerHTML = this.buildPaginationPages(currentPage, totalPages).map((page) => page == null
            ? '<li class="page-item disabled"><span class="page-link">…</span></li>'
            : `
                <li class="page-item ${page === currentPage ? 'active' : ''}">
                    <button type="button" class="page-link" data-role="go-order-history-page" data-page="${page}">${page + 1}</button>
                </li>
            `).join('');
        pagination.querySelectorAll('[data-role="go-order-history-page"]').forEach((button) => {
            button.addEventListener('click', () => this.goPage(this.normalizePage(button.dataset.page)));
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
            void CommonJS.alert('이동할 페이지 정보가 올바르지 않습니다.', '알림', 'warning');
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
        const currentActionType = this.normalizeActionType(document.getElementById('historyActionType')?.value);
        document.querySelectorAll('.history-quick-filter[data-action-type]').forEach((button) => {
            const actionType = this.normalizeActionType(button.dataset.actionType);
            button.classList.toggle('active', actionType === currentActionType);
            button.classList.toggle('btn-dark', actionType === currentActionType);
            button.classList.toggle('btn-outline-dark', actionType !== currentActionType);
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
        const normalizedPreset = this.normalizeDatePreset(preset);
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

        if (normalizedPreset === 'clear') {
            startDateInput.value = '';
            endDateInput.value = '';
        } else {
            const startDate = new Date(today);
            if (normalizedPreset === '7days') {
                startDate.setDate(startDate.getDate() - 6);
            } else if (normalizedPreset === '30days') {
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
            const preset = this.normalizeDatePreset(button.dataset.orderHistoryDatePreset);
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
        const targetHistoryNo = this.normalizeOptionalPositiveNumber(historyNo);
        document.querySelectorAll('[data-order-history-row]').forEach((row) => {
            const selected = this.normalizeOptionalPositiveNumber(row.dataset.orderHistoryRow) === targetHistoryNo;
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
        const historyNo = this.normalizeOptionalPositiveNumber(this.state.historyNo);
        if (!historyNo) {
            this.state.historyNo = '';
        } else {
            this.state.historyNo = '';
        }
        history.replaceState(null, '', `${window.location.pathname}?${this.buildParams().toString()}`);
    },

    validateState() {
        const orderNo = document.getElementById('historyOrderNo')?.value.trim() || '';
        const actorNo = document.getElementById('historyActorNo')?.value.trim() || '';
        const actionType = document.getElementById('historyActionType')?.value || '';
        const keyword = CommonJS.normalizeOptionalText(document.getElementById('historyKeyword')?.value) || '';
        const actorKeyword = CommonJS.normalizeOptionalText(document.getElementById('historyActorKeyword')?.value) || '';
        const startDate = document.getElementById('historyStartDate')?.value || '';
        const endDate = document.getElementById('historyEndDate')?.value || '';
        const orderType = this.normalizeOrderType(document.getElementById('historyOrderType')?.value);
        if (orderNo && !this.isPositiveNumber(orderNo)) {
            void CommonJS.alert('주문 번호는 1 이상의 숫자만 입력할 수 있습니다.', '알림', 'warning');
            return false;
        }
        if (actorNo && !this.isPositiveNumber(actorNo)) {
            void CommonJS.alert('작업자 번호는 1 이상의 숫자만 입력할 수 있습니다.', '알림', 'warning');
            return false;
        }
        if (!this.isValidActionType(actionType)) {
            void CommonJS.alert('작업 유형이 올바르지 않습니다.', '알림', 'warning');
            return false;
        }
        if (keyword.length > 50 || actorKeyword.length > 50) {
            void CommonJS.alert('검색어는 50자 이하로 입력하세요.', '알림', 'warning');
            return false;
        }
        if (!this.isValidDateRange(startDate, endDate)) {
            void CommonJS.alert('조회 기간은 최대 92일까지 선택할 수 있습니다.', '알림', 'warning');
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
        return [20, 50, 100].includes(parsed) ? parsed : 20;
    },

    normalizeOptionalPositiveNumber(value) {
        return this.isPositiveNumber(value) ? String(Number(value)) : '';
    },

    normalizeActionType(value) {
        const normalized = CommonJS.normalizeOptionalText(value) || '';
        return this.isValidActionType(normalized) ? normalized : '';
    },

    isValidActionType(value) {
        return ['', 'STATUS_CHANGE', 'DELIVERY_START', 'DELIVERY_COMPLETE', 'CANCEL', 'ADMIN_MEMO'].includes(String(value || ''));
    },

    normalizeDatePreset(value) {
        return ['today', '7days', '30days', 'clear'].includes(value) ? value : 'today';
    },

    normalizeOrderType(value) {
        return ['latest', 'oldest'].includes(value) ? value : 'latest';
    },

    isPositiveNumber(value) {
        return /^\d+$/.test(String(value || '')) && Number(value) > 0;
    },

    normalizeNonNegativeInteger(value) {
        const parsed = Number(value);
        return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0;
    },

    formatCount(value) {
        return this.normalizeNonNegativeInteger(value).toLocaleString();
    },

    normalizeDateInput(value) {
        const text = String(value || '');
        if (!/^\d{4}-\d{2}-\d{2}$/.test(text)) return '';
        const date = new Date(`${text}T00:00:00`);
        if (!Number.isFinite(date.getTime())) return '';
        const normalized = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
        return normalized === text ? text : '';
    },

    isValidDateRange(startDate, endDate) {
        if (!startDate || !endDate) return true;
        const start = new Date(`${startDate}T00:00:00`);
        const end = new Date(`${endDate}T00:00:00`);
        const rangeDays = Math.floor((end.getTime() - start.getTime()) / 86400000);
        return Number.isFinite(rangeDays) && rangeDays <= 92;
    },

    normalizeHistoryItems(items) {
        return Array.isArray(items)
            ? items.filter((item) => this.normalizeOptionalPositiveNumber(item?.historyNo)
                && this.normalizeOptionalPositiveNumber(item?.orderNo))
            : [];
    },

    buildPaginationPages(currentPage, totalPages) {
        const pages = new Set([0, totalPages - 1]);
        for (let page = Math.max(0, currentPage - 2); page <= Math.min(totalPages - 1, currentPage + 2); page += 1) {
            pages.add(page);
        }
        return Array.from(pages).sort((left, right) => left - right).flatMap((page, index, sorted) => (
            index > 0 && page - sorted[index - 1] > 1 ? [null, page] : [page]
        ));
    }
};

document.addEventListener('DOMContentLoaded', () => OrderHistoryPage.init());
