const ProductHistoryPage = {
    initialized: false,
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
        document.querySelectorAll('.product-history-quick-filter[data-action-type]').forEach((button) => {
            button.addEventListener('click', () => {
                document.getElementById('historyActionType').value = button.dataset.actionType || '';
                this.state.page = 0;
                this.syncQuickFilterState();
                this.loadHistory();
            });
        });
        document.querySelectorAll('[data-product-history-date-preset]').forEach((button) => {
            button.addEventListener('click', () => this.applyDatePreset(button.dataset.productHistoryDatePreset));
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
        document.getElementById('historyProductNo').value = params.get('productNo') || '';
        document.getElementById('historyActionType').value = params.get('actionType') || '';
        document.getElementById('historyStartDate').value = params.get('startDate') || '';
        document.getElementById('historyEndDate').value = params.get('endDate') || '';
        document.getElementById('historyKeyword').value = params.get('keyword') || '';
        document.getElementById('historyActorNo').value = params.get('actorNo') || '';
        document.getElementById('historyActorKeyword').value = params.get('actorKeyword') || '';
        document.getElementById('historyOrderType').value = params.get('orderType') || 'latest';
        this.state.page = Number(params.get('page') || 0);
        this.state.size = Number(params.get('size') || 20);
        this.state.source = params.get('source') || '';
        this.state.returnTo = params.get('returnTo') || '';
        document.getElementById('historyPageSize').value = String(this.state.size);
        this.syncQuickFilterState();
        this.syncDatePresetState();
        CommonJS.bindMainLogoNavigation(this.state.returnTo || '/admin/products');
        CommonJS.renderSourceContextNotice({ noticeId: 'productHistorySourceContextNotice', source: this.state.source });
    },

    buildParams() {
        const params = new URLSearchParams();
        const productNo = document.getElementById('historyProductNo').value.trim();
        const actionType = document.getElementById('historyActionType').value;
        const startDate = document.getElementById('historyStartDate').value;
        const endDate = document.getElementById('historyEndDate').value;
        const keyword = CommonJS.normalizeOptionalText(document.getElementById('historyKeyword').value);
        const actorNo = document.getElementById('historyActorNo').value.trim();
        const actorKeyword = CommonJS.normalizeOptionalText(document.getElementById('historyActorKeyword').value);
        const orderType = document.getElementById('historyOrderType').value || 'latest';

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
        params.set('page', String(this.state.page));
        params.set('size', String(this.state.size));
        return params;
    },

    getReturnTo() {
        const params = this.buildParams();
        return `${window.location.pathname}?${params.toString()}`;
    },

    async loadHistory() {
        const params = this.buildParams();
        history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
        this.setMetaText('데이터를 불러오는 중입니다...');

        try {
            const response = await fetch(`/api/admin/product/history/list?${params.toString()}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '변경 이력을 불러오지 못했습니다.'));
            }
            const data = await response.json();
            this.renderList(data.items || []);
            this.renderMeta(data);
            this.renderPagination(data);
        } catch (error) {
            this.renderError(error.message);
        }
    },

    renderList(items) {
        const tbody = document.getElementById('productHistoryBody');
        if (!items.length) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center py-5 text-muted">조회된 변경 이력이 없습니다.</td></tr>';
            return;
        }

        tbody.innerHTML = items.map(item => `
            <tr>
                <td class="ps-4 text-muted small">${item.historyNo}</td>
                <td><a class="text-decoration-none fw-bold" href="${this.buildProductDetailPath(item.productNo)}">${item.productNo}</a></td>
                <td><span class="badge bg-dark">${item.actionLabel}</span></td>
                <td>
                    <div class="fw-semibold">${item.summary}</div>
                    ${item.relatedProductNo ? `
                        <div class="small">
                            <a class="text-decoration-none" href="${this.buildProductDetailPath(item.relatedProductNo)}">
                                ${item.relatedProductLabel} #${item.relatedProductNo}
                            </a>
                        </div>
                    ` : ''}
                    ${item.activityLogPath ? `
                        <div class="small">
                            <a class="text-decoration-none" href="${this.buildLogPathFromBase(item.activityLogPath)}">
                                ${item.activityLogLabel || '활동 로그 보기'}
                            </a>
                        </div>
                    ` : ''}
                    <div class="text-muted small">상태 ${item.statusSnapshot || '-'} · 옵션 ${item.optionCount}개 · 재고 ${item.totalStock}개</div>
                </td>
                <td>${item.actorName}${item.actorNo ? ` <span class="text-muted small">(#${item.actorNo})</span>` : ''}</td>
                <td class="text-muted small">${item.totalStock} / ${item.optionCount}</td>
                <td class="text-end pe-4 small text-muted">${item.actionDtm}</td>
            </tr>
        `).join('');
    },

    buildProductDetailPath(productNo) {
        const params = new URLSearchParams();
        params.set('no', String(productNo));
        params.set('returnTo', this.getReturnTo());
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        return `/admin/products/get?${params.toString()}`;
    },

    buildLogPathFromBase(basePath) {
        if (!basePath) {
            return '';
        }
        const [path, rawQuery = ''] = basePath.split('?');
        const params = new URLSearchParams(rawQuery);
        params.set('returnTo', this.getReturnTo());
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        return `${path}?${params.toString()}`;
    },

    renderMeta(data) {
        this.setMetaText(data.resultMeta?.resultLabel || data.pageInfoLabel || `${data.rangeStart}-${data.rangeEnd} / ${data.totalElements}건`);
        const filterMeta = document.getElementById('historyFilterMeta');
        if (filterMeta) {
            filterMeta.textContent = `적용 필터 ${data.resultMeta?.filterCount ?? this.countActiveFilters()}개`;
        }
        const pageMeta = document.getElementById('historyPageMeta');
        if (pageMeta) {
            pageMeta.textContent = data.resultMeta?.pageInfoLabel || data.pageInfoLabel || '페이지 메타 없음';
        }
    },

    renderPagination(data) {
        const pagination = document.getElementById('historyPagination');
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
                    <button type="button" class="page-link" data-role="go-product-history-page" data-page="${i}">${i + 1}</button>
                </li>
            `;
        }
        pagination.innerHTML = html;
        pagination.querySelectorAll('[data-role="go-product-history-page"]').forEach((button) => {
            button.addEventListener('click', () => this.goPage(Number(button.dataset.page)));
        });
    },

    renderError(message) {
        document.getElementById('productHistoryBody').innerHTML =
            `<tr><td colspan="7" class="text-center py-5 text-danger">${message}</td></tr>`;
        this.setMetaText('이력 조회 실패');
        const filterMeta = document.getElementById('historyFilterMeta');
        if (filterMeta) {
            filterMeta.textContent = '적용 필터 확인 불가';
        }
        const pageMeta = document.getElementById('historyPageMeta');
        if (pageMeta) {
            pageMeta.textContent = '페이지 메타 확인 불가';
        }
        document.getElementById('historyPagination').innerHTML = '';
    },

    async exportCsv() {
        const button = document.getElementById('btnExportProductHistoryCsv');
        if (button?.dataset.loading === 'true') {
            return;
        }

        const params = this.buildParams();
        if (button) {
            button.dataset.loading = 'true';
            button.disabled = true;
        }
        try {
            await CommonJS.downloadFile(`/api/admin/product/history/export?${params.toString()}`);
        } catch (error) {
            await CommonJS.alert(error.message || '상품 변경 이력 CSV를 내보내지 못했습니다.');
        } finally {
            if (button) {
                button.dataset.loading = 'false';
                button.disabled = false;
            }
        }
    },

    setMetaText(message) {
        document.getElementById('historyMetaText').textContent = message;
    },

    goPage(page) {
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

    syncQuickFilterState() {
        const currentActionType = document.getElementById('historyActionType')?.value || '';
        document.querySelectorAll('.product-history-quick-filter[data-action-type]').forEach((button) => {
            const active = (button.dataset.actionType || '') === currentActionType;
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
            const preset = button.dataset.productHistoryDatePreset;
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
    }
};

document.addEventListener('DOMContentLoaded', () => ProductHistoryPage.init());
