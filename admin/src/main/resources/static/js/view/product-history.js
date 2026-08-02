const ProductHistoryPage = {
    initialized: false,
    isExporting: false,
    listRequestId: 0,
    state: {
        page: 0,
        size: 20,
        source: '',
        returnTo: ''
    },

    init() {
        if (this.initialized) return;
        this.initialized = true;
        this.bindEvents();
        this.readStateFromUrl();
        CommonJS.bindMainLogoNavigation(this.state.returnTo || '/admin/products');
        this.loadHistory();
    },

    bindEvents() {
        document.getElementById('btnSearchHistory')?.addEventListener('click', () => {
            this.state.page = 0;
            this.loadHistory();
        });
        document.getElementById('btnExportProductHistoryCsv')?.addEventListener('click', () => this.exportCsv());
        document.getElementById('btnResetProductHistory')?.addEventListener('click', () => this.resetFilters());
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
        document.querySelectorAll('.product-history-quick-filter[data-action-type]').forEach((button) => {
            button.addEventListener('click', () => {
                document.getElementById('historyActionType').value = this.normalizeActionType(button.dataset.actionType);
                this.state.page = 0;
                this.syncQuickFilterState();
                this.loadHistory();
            });
        });
        document.querySelectorAll('[data-product-history-date-preset]').forEach((button) => {
            button.addEventListener('click', () => this.applyDatePreset(this.normalizeDatePreset(button.dataset.productHistoryDatePreset)));
        });
        window.addEventListener('popstate', () => {
            this.readStateFromUrl();
            this.syncQuickFilterState();
            this.syncDatePresetState();
            this.loadHistory();
        });
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        document.getElementById('historyProductNo').value = this.normalizeOptionalPositiveNumber(params.get('productNo'));
        document.getElementById('historyActionType').value = this.normalizeActionType(params.get('actionType'));
        document.getElementById('historyStartDate').value = this.normalizeDateInput(params.get('startDate'));
        document.getElementById('historyEndDate').value = this.normalizeDateInput(params.get('endDate'));
        document.getElementById('historyKeyword').value = (CommonJS.normalizeOptionalText(params.get('keyword')) || '').slice(0, 100);
        document.getElementById('historyActorNo').value = this.normalizeOptionalPositiveNumber(params.get('actorNo'));
        document.getElementById('historyActorKeyword').value = (CommonJS.normalizeOptionalText(params.get('actorKeyword')) || '').slice(0, 100);
        document.getElementById('historyOrderType').value = this.normalizeOrderType(params.get('orderType'));
        this.state.page = this.normalizePage(params.get('page'));
        this.state.size = this.normalizePageSize(params.get('size'));
        this.state.source = params.get('source') || '';
        this.state.returnTo = CommonJS.normalizeAdminReturnPath(params.get('returnTo'), '');
        document.getElementById('historyPageSize').value = String(this.state.size);
        this.syncQuickFilterState();
        this.syncDatePresetState();
        CommonJS.bindMainLogoNavigation(this.state.returnTo || '/admin/products');
        CommonJS.renderSourceContextNotice({ noticeId: 'productHistorySourceContextNotice', source: this.state.source });
    },

    buildParams() {
        const params = new URLSearchParams();
        const productNo = this.normalizeOptionalPositiveNumber(document.getElementById('historyProductNo').value);
        const actionType = this.normalizeActionType(document.getElementById('historyActionType').value);
        const startDate = document.getElementById('historyStartDate').value;
        const endDate = document.getElementById('historyEndDate').value;
        const keyword = CommonJS.normalizeOptionalText(document.getElementById('historyKeyword').value);
        const actorNo = this.normalizeOptionalPositiveNumber(document.getElementById('historyActorNo').value);
        const actorKeyword = CommonJS.normalizeOptionalText(document.getElementById('historyActorKeyword').value);
        const orderType = this.normalizeOrderType(document.getElementById('historyOrderType').value);

        if (productNo) params.set('productNo', productNo);
        if (actionType) params.set('actionType', actionType);
        if (startDate) params.set('startDate', startDate);
        if (endDate) params.set('endDate', endDate);
        if (keyword) params.set('keyword', keyword);
        if (actorNo) params.set('actorNo', actorNo);
        if (actorKeyword) params.set('actorKeyword', actorKeyword);
        if (orderType && orderType !== 'latest') params.set('orderType', orderType);
        if (this.state.source) params.set('source', this.state.source);
        if (this.state.returnTo) params.set('returnTo', this.state.returnTo);
        this.state.size = this.normalizePageSize(document.getElementById('historyPageSize')?.value);
        params.set('page', String(this.state.page));
        params.set('size', String(this.state.size));
        return params;
    },

    getReturnTo() {
        const params = this.buildParams();
        return `${window.location.pathname}?${params.toString()}`;
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
        this.setMetaText('상품 변경 이력을 불러오는 중입니다...');
        this.setResultMetaText('결과 메타를 계산하는 중입니다...');
        this.setPageMetaText('페이지 메타 계산 중');
        this.renderLoadingState();

        try {
            const response = await fetch(`/api/admin/product/history/list?${params.toString()}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '변경 이력을 불러오지 못했습니다.'));
            }
            const data = await response.json();
            if (requestId !== this.listRequestId) {
                return;
            }
            const items = Array.isArray(data.items) ? data.items : [];
            this.renderList(items);
            this.renderMeta(data);
            this.renderPagination(data);
        } catch (error) {
            if (requestId !== this.listRequestId) {
                return;
            }
            this.renderError(error.message);
        }
    },

    renderList(items) {
        const tbody = document.getElementById('productHistoryBody');
        const validItems = this.normalizeHistoryItems(items);
        if (!validItems.length) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="py-5">
                        <div class="product-empty-state">
                            <i class="fas fa-box-open product-empty-state-icon"></i>
                            <strong>조건에 맞는 상품 변경 이력이 없습니다.</strong>
                            <p>${this.escapeHtml(this.buildEmptyStateMessage())}</p>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = validItems.flatMap((item) => {
            const historyNo = this.normalizeOptionalPositiveNumber(item.historyNo);
            const productNo = this.normalizeOptionalPositiveNumber(item.productNo);
            const relatedProductNo = this.normalizeOptionalPositiveNumber(item.relatedProductNo);
            const activityLogPath = this.buildLogPathFromBase(item.activityLogPath);
            if (!historyNo || !productNo) return [];
            return [`
            <tr>
                <td class="ps-4 text-muted small">${historyNo}</td>
                <td><a class="text-decoration-none fw-bold" href="${this.buildProductDetailPath(productNo)}">${productNo}</a></td>
                <td><span class="badge bg-dark">${this.escapeHtml(item.actionLabel || '-')}</span></td>
                <td>
                    <div class="fw-semibold">${this.escapeHtml(item.summary || '-')}</div>
                    ${relatedProductNo ? `
                        <div class="small">
                            <a class="text-decoration-none" href="${this.buildProductDetailPath(relatedProductNo)}">
                                ${this.escapeHtml(item.relatedProductLabel || '-')} #${relatedProductNo}
                            </a>
                        </div>
                    ` : ''}
                    ${activityLogPath ? `
                        <div class="small">
                            <a class="text-decoration-none" href="${this.escapeHtml(activityLogPath)}">
                                ${this.escapeHtml(item.activityLogLabel || '활동 로그 보기')}
                            </a>
                        </div>
                    ` : ''}
                    <div class="text-muted small">상태 ${this.escapeHtml(item.statusSnapshot || '-')} · 옵션 ${this.formatCount(item.optionCount)}개 · 재고 ${this.formatCount(item.totalStock)}개</div>
                </td>
                <td>${this.escapeHtml(item.actorName || '-')}${this.normalizeOptionalPositiveNumber(item.actorNo) ? ` <span class="text-muted small">(#${this.normalizeOptionalPositiveNumber(item.actorNo)})</span>` : ''}</td>
                <td class="text-muted small">${this.formatCount(item.totalStock)} / ${this.formatCount(item.optionCount)}</td>
                <td class="text-end pe-4 small text-muted">${this.escapeHtml(item.actionDtm || '-')}</td>
            </tr>
        `];
        }).join('');
    },

    buildProductDetailPath(productNo) {
        if (!this.isPositiveNumber(productNo)) {
            return '#';
        }
        const params = new URLSearchParams();
        params.set('no', String(productNo));
        params.set('returnTo', this.getReturnTo());
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        return `/admin/products/get?${params.toString()}`;
    },

    buildLogPathFromBase(basePath) {
        const safeBasePath = CommonJS.normalizeAdminReturnPath(basePath, '');
        if (!safeBasePath) {
            return '';
        }
        const [path, rawQuery = ''] = safeBasePath.split('?');
        const params = new URLSearchParams(rawQuery);
        params.set('returnTo', this.getReturnTo());
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        return `${path}?${params.toString()}`;
    },

    renderMeta(data) {
        this.setMetaText(data.resultMeta?.resultLabel || data.pageInfoLabel || `${this.formatCount(data.rangeStart)}-${this.formatCount(data.rangeEnd)} / ${this.formatCount(data.totalElements)}건`);
        const filterMeta = document.getElementById('historyFilterMeta');
        if (filterMeta) {
            filterMeta.textContent = `적용 필터 ${this.normalizeNonNegativeInteger(data.resultMeta?.filterCount ?? this.countActiveFilters())}개`;
        }
        this.setResultMetaText(data.resultMeta?.querySignature || '최신순');
        this.setPageMetaText(data.resultMeta?.pageInfoLabel || data.pageInfoLabel || '페이지 메타 없음');
    },

    renderPagination(data) {
        const pagination = document.getElementById('historyPagination');
        if (!pagination) {
            return;
        }

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
                    <button type="button" class="page-link" data-role="go-product-history-page" data-page="${page}">${page + 1}</button>
                </li>
            `).join('');
        pagination.querySelectorAll('[data-role="go-product-history-page"]').forEach((button) => {
            button.addEventListener('click', () => this.goPage(this.normalizePage(button.dataset.page)));
        });
    },

    renderError(message) {
        document.getElementById('productHistoryBody').innerHTML = `
            <tr>
                <td colspan="7" class="py-5">
                    <div class="product-empty-state">
                        <div class="product-empty-state__icon text-danger">
                            <i class="fa-solid fa-triangle-exclamation"></i>
                        </div>
                        <strong>상품 변경 이력을 불러오지 못했습니다.</strong>
                        <p>${this.escapeHtml(message)}</p>
                    </div>
                </td>
            </tr>`;
        this.setMetaText('이력 조회 실패');
        const filterMeta = document.getElementById('historyFilterMeta');
        if (filterMeta) {
            filterMeta.textContent = '적용 필터 확인 불가';
        }
        this.setResultMetaText(message);
        this.setPageMetaText('페이지 메타 확인 불가');
        document.getElementById('historyPagination').innerHTML = '';
    },

    async exportCsv() {
        if (this.isExporting) {
            return;
        }
        const button = document.getElementById('btnExportProductHistoryCsv');
        try {
            this.isExporting = true;
            CommonJS.setButtonDisabled(button, true, '내보내는 중입니다.');
            if (!this.validateState()) {
                return;
            }
            const startDate = document.getElementById('historyStartDate')?.value || '';
            const endDate = document.getElementById('historyEndDate')?.value || '';
            if (startDate && endDate && startDate > endDate) {
                throw new Error('시작일은 종료일보다 늦을 수 없습니다.');
            }
            const params = this.buildParams();
            params.delete('page');
            params.delete('size');
            await CommonJS.downloadFile(`/api/admin/product/history/export?${params.toString()}`);
        } catch (error) {
            await CommonJS.alert(error.message || '상품 변경 이력 CSV를 내보내지 못했습니다.', '오류', 'error');
        } finally {
            this.isExporting = false;
            CommonJS.setButtonDisabled(button, false);
        }
    },

    setMetaText(message) {
        document.getElementById('historyMetaText').textContent = message;
    },

    setResultMetaText(message) {
        const resultMeta = document.getElementById('productHistoryResultMeta');
        if (resultMeta) {
            resultMeta.textContent = message;
        }
    },

    setPageMetaText(message) {
        const pageMeta = document.getElementById('historyPageMeta');
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
        document.getElementById('historyProductNo').value = '';
        document.getElementById('historyActionType').value = '';
        document.getElementById('historyStartDate').value = '';
        document.getElementById('historyEndDate').value = '';
        document.getElementById('historyKeyword').value = '';
        document.getElementById('historyActorNo').value = '';
        document.getElementById('historyActorKeyword').value = '';
        document.getElementById('historyOrderType').value = 'latest';
        document.getElementById('historyPageSize').value = '20';
        this.state.page = 0;
        this.state.size = 20;
        this.syncQuickFilterState();
        this.syncDatePresetState();
        this.loadHistory();
    },

    renderLoadingState() {
        const tbody = document.getElementById('productHistoryBody');
        if (!tbody) {
            return;
        }
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="py-5">
                    <div class="product-loading-state">
                        <div class="spinner-border spinner-border-sm text-primary" role="status" aria-hidden="true"></div>
                        <strong>상품 변경 이력을 불러오는 중입니다.</strong>
                        <p>현재 필터 조건에 맞는 상품 변경 내역을 조회하고 있습니다.</p>
                    </div>
                </td>
            </tr>
        `;
    },

    buildEmptyStateMessage() {
        const parts = [];
        const productNo = document.getElementById('historyProductNo')?.value.trim();
        const actionType = document.getElementById('historyActionType')?.value;
        const keyword = CommonJS.normalizeOptionalText(document.getElementById('historyKeyword')?.value);
        const actorNo = document.getElementById('historyActorNo')?.value.trim();
        const actorKeyword = CommonJS.normalizeOptionalText(document.getElementById('historyActorKeyword')?.value);
        const startDate = document.getElementById('historyStartDate')?.value;
        const endDate = document.getElementById('historyEndDate')?.value;

        if (productNo) parts.push(`상품 번호 ${productNo}`);
        if (actionType) parts.push(`작업 유형 ${actionType}`);
        if (keyword) parts.push(`검색어 "${keyword}"`);
        if (actorNo) parts.push(`작업자 번호 ${actorNo}`);
        if (actorKeyword) parts.push(`작업자 "${actorKeyword}"`);
        if (startDate || endDate) parts.push(`기간 ${startDate || '전체'} ~ ${endDate || '전체'}`);

        if (!parts.length) {
            return '상품 변경 이력이 아직 없거나, 현재 페이지에 표시할 데이터가 없습니다.';
        }

        return `${parts.join(', ')} 조건에 맞는 상품 변경 이력이 없습니다.`;
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

    syncQuickFilterState() {
        const currentActionType = this.normalizeActionType(document.getElementById('historyActionType')?.value);
        document.querySelectorAll('.product-history-quick-filter[data-action-type]').forEach((button) => {
            const active = this.normalizeActionType(button.dataset.actionType) === currentActionType;
            button.classList.toggle('active', active);
            button.classList.toggle('btn-dark', active);
            button.classList.toggle('btn-outline-dark', !active);
        });
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

        document.querySelectorAll('[data-product-history-date-preset]').forEach((button) => {
            const preset = this.normalizeDatePreset(button.dataset.productHistoryDatePreset);
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

    countActiveFilters() {
        let count = 0;
        if (document.getElementById('historyProductNo')?.value.trim()) count += 1;
        if (document.getElementById('historyActionType')?.value) count += 1;
        if (document.getElementById('historyKeyword')?.value.trim()) count += 1;
        if (document.getElementById('historyActorNo')?.value.trim()) count += 1;
        if (document.getElementById('historyActorKeyword')?.value.trim()) count += 1;
        if (document.getElementById('historyStartDate')?.value) count += 1;
        if (document.getElementById('historyEndDate')?.value) count += 1;
        if ((document.getElementById('historyOrderType')?.value || 'latest') !== 'latest') count += 1;
        return count;
    },

    validateState() {
        const productNo = document.getElementById('historyProductNo')?.value.trim() || '';
        const actorNo = document.getElementById('historyActorNo')?.value.trim() || '';
        const actionType = document.getElementById('historyActionType')?.value || '';
        const keyword = CommonJS.normalizeOptionalText(document.getElementById('historyKeyword')?.value) || '';
        const actorKeyword = CommonJS.normalizeOptionalText(document.getElementById('historyActorKeyword')?.value) || '';
        const orderType = this.normalizeOrderType(document.getElementById('historyOrderType')?.value);
        if (productNo && !this.isPositiveNumber(productNo)) {
            void CommonJS.alert('상품 번호는 1 이상의 숫자만 입력할 수 있습니다.', '알림', 'warning');
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
        if (keyword.length > 100 || actorKeyword.length > 100) {
            void CommonJS.alert('검색어는 100자 이하로 입력하세요.', '알림', 'warning');
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

    normalizeActionType(value) {
        const normalized = CommonJS.normalizeOptionalText(value) || '';
        return this.isValidActionType(normalized) ? normalized : '';
    },

    isValidActionType(value) {
        return ['', 'CREATED', 'UPDATED', 'DELETED'].includes(String(value || ''));
    },

    normalizeDatePreset(value) {
        return ['today', '7days', '30days', 'clear'].includes(value) ? value : 'today';
    },

    normalizeOrderType(value) {
        return ['latest', 'oldest'].includes(value) ? value : 'latest';
    },

    normalizeOptionalPositiveNumber(value) {
        return this.isPositiveNumber(value) ? String(Number(value)) : '';
    },

    isPositiveNumber(value) {
        return /^\d+$/.test(String(value || '')) && Number(value) > 0;
    },

    formatCount(value) {
        return this.normalizeNonNegativeInteger(value).toLocaleString();
    },

    normalizeNonNegativeInteger(value) {
        const parsed = Number(value);
        return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0;
    },

    normalizeDateInput(value) {
        const text = String(value || '');
        if (!/^\d{4}-\d{2}-\d{2}$/.test(text)) return '';
        const date = new Date(`${text}T00:00:00`);
        if (!Number.isFinite(date.getTime())) return '';
        const normalized = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
        return normalized === text ? text : '';
    },

    normalizeHistoryItems(items) {
        return Array.isArray(items)
            ? items.filter((item) => this.normalizeOptionalPositiveNumber(item?.historyNo)
                && this.normalizeOptionalPositiveNumber(item?.productNo))
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

document.addEventListener('DOMContentLoaded', () => ProductHistoryPage.init());
