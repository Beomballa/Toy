const ProductList = {
    initialized: false,
    allowedLowStockThresholds: ['10', '30', '50', '100'],

    state: {
        page: 0,
        size: 10,
        brandNo: '',
        categoryNo: '',
        status: '',
        lowStockOnly: false,
        lowStockThreshold: '100',
        createdTodayOnly: false,
        searchKeyword: '',
        orderType: 'r',
        source: '',
        returnTo: '',
    },
    defaultLowStockThreshold: '100',
    requestSequence: 0,
    activeRequestController: null,
    lastAppliedQuery: null,
    lastResultMeta: null,
    lastErrorMessage: '',
    lastTotalElements: 0,
    operationPolicy: null,
    isDeletingProduct: false,
    isCloningProduct: false,
    isExporting: false,
    bulkInFlight: false,
    quickOperateInFlight: new Set(),
    selectedProductNos: new Set(),

    init(brands = [], categories = [], initialLowStockThreshold = 100) {
        if (this.initialized) {
            return;
        }
        this.initialized = true;

        this.defaultLowStockThreshold = this._normalizeLowStockThreshold(String(initialLowStockThreshold));
        this.state.lowStockThreshold = this.defaultLowStockThreshold;
        this._fillSelect('brandNo',    brands,     'brandNo',    'nameKo');
        this._fillSelect('categoryNo', categories, 'categoryNo', 'name');

        this._readStateFromUrl();
        this._syncFilterInputs();
        CommonJS.bindMainLogoNavigation(this.state.returnTo || '/admin/products');
        CommonJS.renderSourceContextNotice({ noticeId: 'productListSourceContextNotice', source: this.state.source });
        const frontDisplayButton = document.getElementById('btnGoProductFrontDisplay');
        if (frontDisplayButton) {
            frontDisplayButton.href = `/admin/products/front-display?source=product-list-front-display&returnTo=${encodeURIComponent(this.getReturnTo())}`;
        }
        this._renderFilterSummary();
        this._bindEvents();
        this._initAnimations();
        this.applyOperationPolicy();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));
        this.getList(); // 초기 로드

        document.getElementById('new-product')?.addEventListener('click', async () => {
            if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
                await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('상품 등록'), '알림', 'warning');
                return;
            }

            location.href = `/admin/products/set?source=product-list&returnTo=${encodeURIComponent(this.getReturnTo())}`;
        });
        document.getElementById('btnSearchProducts')?.addEventListener('click', () => this.applySearchFilter());
        document.getElementById('btnExportProducts')?.addEventListener('click', async () => {
            if (this.isExporting) {
                return;
            }
            const button = document.getElementById('btnExportProducts');
            try {
                if (!this.validateState()) {
                    return;
                }
                this.isExporting = true;
                CommonJS.setButtonDisabled(button, true, '내보내는 중입니다.');
                const params = new URLSearchParams(this.buildQueryString());
                params.delete('page');
                params.delete('size');
                await CommonJS.downloadFile(`/api/admin/product/export?${params.toString()}`, 'products.csv');
            } catch (error) {
                await CommonJS.alert(error.message, '오류', 'error');
            } finally {
                this.isExporting = false;
                CommonJS.setButtonDisabled(button, false);
            }
        });
        document.getElementById('btnApplyProductBulk')?.addEventListener('click', () => this.applyBulkOperation());
        document.getElementById('btnBulkDuplicateProduct')?.addEventListener('click', () => this.applyBulkDuplicate());
        document.getElementById('btnBulkDeleteProduct')?.addEventListener('click', () => this.applyBulkDelete());
        document.getElementById('btnClearProductSelection')?.addEventListener('click', () => this.clearSelection());
        document.getElementById('productSelectPage')?.addEventListener('change', (event) => this.toggleSelectCurrentPage(event.target.checked));
    },

    _fillSelect(selectId, items, valueKey, labelKey) {
        const select = document.getElementById(selectId);
        if (!select || !items?.length) return;

        const fragment = document.createDocumentFragment();
        items.forEach(item => {
            const opt = document.createElement('option');
            opt.value = item[valueKey];
            opt.textContent = item[labelKey];
            fragment.appendChild(opt);
        });
        select.appendChild(fragment);
    },

    _bindEvents() {
        const FILTER_IDS = ['brandNo', 'categoryNo', 'statusFilter', 'lowStockOnly', 'lowStockThreshold', 'createdTodayOnly', 'searchKeyword', 'pageSize', 'orderType'];
        FILTER_IDS.forEach(id => {
            const el = document.getElementById(id);
            if (!el) return;
            el.addEventListener('change', () => { this.state.page = 0; this._updateStateFromInputs(); this.getList(); });
            if (el.tagName === 'INPUT') {
                el.addEventListener('keydown', e => {
                    if (e.key === 'Enter') { e.preventDefault(); this.state.page = 0; this._updateStateFromInputs(); this.getList(); }
                });
            }
        });

        document.getElementById('productListTableBody')?.addEventListener('click', e => {
            if (e.target.closest('[data-role="reset-empty-product-filters"]')) {
                this.resetFilters();
                return;
            }

            const productNameEl = e.target.closest('.product-name');
            if (productNameEl) {
                const productNo = this._normalizeOptionalPositiveNumber(productNameEl.dataset.id);
                if (!this._isPositiveNumber(productNo)) {
                    void CommonJS.alert('상품 번호가 올바르지 않습니다.', '알림', 'warning');
                    return;
                }
                location.href = `/admin/products/get?no=${productNo}&source=product-list&returnTo=${encodeURIComponent(this.getReturnTo())}`;
                return;
            }

            const checkbox = e.target.closest('[data-role="select-product"]');
            if (checkbox) {
                this.toggleSelection(this._normalizeOptionalPositiveNumber(checkbox.dataset.productNo), checkbox.checked);
                return;
            }

            const imageSearchBtn = e.target.closest('.btn-image-search');
            if (imageSearchBtn) {
                CommonJS.openImageSearch(
                    imageSearchBtn.dataset.productName,
                    imageSearchBtn.dataset.modelNum,
                    imageSearchBtn.dataset.brandName
                );
                return;
            }

            const editButton = e.target.closest('[data-role="edit-product"]');
            if (editButton) {
                if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
                    void CommonJS.alert(CommonJS.getAdminWriteBlockedReason('상품 수정'), '알림', 'warning');
                    return;
                }
                const productNo = this._normalizeOptionalPositiveNumber(editButton.dataset.productNo);
                if (!this._isPositiveNumber(productNo)) {
                    void CommonJS.alert('상품 번호가 올바르지 않습니다.', '알림', 'warning');
                    return;
                }
                location.href = `/admin/products/update?no=${productNo}&source=product-list&returnTo=${encodeURIComponent(this.getReturnTo())}`;
                return;
            }

            const cloneButton = e.target.closest('[data-role="clone-product"]');
            if (cloneButton) {
                const productNo = this._normalizeOptionalPositiveNumber(cloneButton.dataset.productNo);
                if (!this._isPositiveNumber(productNo)) {
                    void CommonJS.alert('상품 번호가 올바르지 않습니다.', '알림', 'warning');
                    return;
                }
                this.cloneProduct(productNo);
                return;
            }

            const quickOperateButton = e.target.closest('[data-role="quick-operate-product"]');
            if (quickOperateButton) {
                const productNo = this._normalizeOptionalPositiveNumber(quickOperateButton.dataset.productNo);
                if (!this._isPositiveNumber(productNo)) {
                    void CommonJS.alert('상품 번호가 올바르지 않습니다.', '알림', 'warning');
                    return;
                }
                this.quickOperateProduct(productNo, quickOperateButton.dataset.status);
                return;
            }

            const deleteButton = e.target.closest('[data-role="delete-product"]');
            if (deleteButton) {
                const productNo = this._normalizeOptionalPositiveNumber(deleteButton.dataset.productNo);
                if (!this._isPositiveNumber(productNo)) {
                    void CommonJS.alert('상품 번호가 올바르지 않습니다.', '알림', 'warning');
                    return;
                }
                this.deleteProduct(productNo);
            }
        });

        document.querySelectorAll('.dropdown-item').forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();

                const val = e.target.getAttribute('data-value');
                const text = e.target.textContent;

                const btn = document.getElementById('orderType');
                btn.setAttribute('data-current-value', val); // data-value 하나로 계속 쓰는 방식
                btn.textContent = text;

                // 2. 즉시 조회 실행
                this.state.page = 0;
                this.state.orderType = val;
                this.getList();
            });
        });

        document.getElementById('btnResetFilter')?.addEventListener('click', () => this.resetFilters());
        document.getElementById('productFilterSummaryChips')?.addEventListener('click', (event) => {
            const removeButton = event.target.closest('[data-filter-remove]');
            if (!removeButton) {
                return;
            }

            this.clearFilter(removeButton.dataset.filterRemove);
        });
        document.getElementById('statTotalCard')?.addEventListener('click', () => this.clearQuickFilters());
        document.getElementById('statActiveCard')?.addEventListener('click', () => this.applyActiveFilter());
        document.getElementById('statLowStockCard')?.addEventListener('click', () => this.applyLowStockFilter());
        document.getElementById('statTodayCard')?.addEventListener('click', () => this.applyTodayFilter());
        this.bindStatCardKeyboard('statTotalCard', () => this.clearQuickFilters());
        this.bindStatCardKeyboard('statActiveCard', () => this.applyActiveFilter());
        this.bindStatCardKeyboard('statLowStockCard', () => this.applyLowStockFilter());
        this.bindStatCardKeyboard('statTodayCard', () => this.applyTodayFilter());
        document.getElementById('pagination')?.addEventListener('click', (event) => {
            const pageButton = event.target.closest('[data-role="go-product-page"]');
            if (!pageButton) {
                return;
            }
            this.goPage(this._normalizePage(pageButton.dataset.page));
        });
        window.addEventListener('popstate', () => {
            this._readStateFromUrl();
            this._syncFilterInputs();
            this._renderFilterSummary();
            this.getList(false);
        });
    },

    async applyOperationPolicy(settings = null) {
        try {
            this.operationPolicy = settings || await CommonJS.fetchSystemSettings();
            const disabled = CommonJS.isAdminWriteBlocked(this.operationPolicy);
            CommonJS.setButtonDisabled(
                document.getElementById('new-product'),
                disabled,
                CommonJS.getAdminWriteBlockedReason('상품 등록, 수정, 삭제')
            );
            CommonJS.setButtonDisabled(
                document.getElementById('btnApplyProductBulk'),
                disabled,
                CommonJS.getAdminWriteBlockedReason('상품 일괄 변경')
            );
            CommonJS.setButtonDisabled(
                document.getElementById('btnBulkDuplicateProduct'),
                disabled,
                CommonJS.getAdminWriteBlockedReason('상품 일괄 복제')
            );
            CommonJS.setButtonDisabled(
                document.getElementById('btnBulkDeleteProduct'),
                disabled,
                CommonJS.getAdminWriteBlockedReason('상품 일괄 삭제')
            );
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
    },

    _initAnimations() {
        document.querySelectorAll('.animate-in').forEach((el, i) => {
            el.style.opacity = '0';
            el.style.animation = `fadeInUp 0.6s ease forwards ${i * 0.1}s`;
        });
    },

    async getList(pushState = true) {
        this._updateStateFromInputs();
        if (!this.validateState()) {
            return;
        }
        if (pushState) {
            this._syncUrlState();
        }
        this._renderFilterSummary();

        const params = new URLSearchParams({
            page: this.state.page,
            size: this.state.size,
            brandNo: this.state.brandNo,
            categoryNo: this.state.categoryNo,
            status: this.state.status,
            lowStockOnly: this.state.lowStockOnly,
            lowStockThreshold: this.state.lowStockThreshold,
            createdTodayOnly: this.state.createdTodayOnly,
            searchKeyword: this.state.searchKeyword,
            orderType: this.state.orderType,
        });
        const requestId = ++this.requestSequence;
        this.activeRequestController?.abort();
        this.activeRequestController = new AbortController();
        this._setLoadingState(true);

        try {
            const res = await fetch(`/api/admin/product/list?${params}`, {
                signal: this.activeRequestController.signal,
            });
            if (!res.ok) {
                const message = await CommonJS.extractErrorMessage(res, '상품 목록 조회 중 오류가 발생했습니다.');
                const error = new Error(message);
                error.userMessage = message;
                throw error;
            }

            const data = await res.json();
            // 빠른 필터 전환 시 먼저 보낸 요청이 늦게 도착할 수 있어서, 최신 요청만 화면에 반영합니다.
            if (requestId !== this.requestSequence) {
                return;
            }

            this.lastErrorMessage = '';
            this._applyServerAppliedQuery(data.appliedQuery);
            this.lastResultMeta = data.resultMeta || null;
            this.lastTotalElements = Number(data.totalElements || 0);
            this._renderList(data.products);
            this._renderPagination(data);
            this._updateStats(data.productStats);
            this._renderFilterSummary();

        } catch (err) {
            if (err.name === 'AbortError') {
                return;
            }
            console.error('상품 목록 로드 실패:', err);
            this.lastErrorMessage = err.userMessage || '상품 목록 조회 중 오류가 발생했습니다.';
            this.lastTotalElements = 0;
            this._showError(this.lastErrorMessage);
        } finally {
            if (requestId === this.requestSequence) {
                this._setLoadingState(false);
            }
        }
    },

    _renderList(items) {
        const tbody = document.getElementById('productListTableBody');
        if (!items?.length) {
            const emptyMessage = this._buildEmptyStateMessage();
            tbody.innerHTML = `
                <tr>
                    <td colspan="9" class="py-5">
                        <div class="product-empty-state">
                            <i class="fas fa-box-open product-empty-state-icon"></i>
                            <strong>조건에 맞는 상품이 없습니다.</strong>
                            <p>${emptyMessage}</p>
                            <button type="button" class="btn btn-sm btn-outline-secondary" data-role="reset-empty-product-filters">
                                필터 초기화
                            </button>
                        </div>
                    </td>
                </tr>
            `;
            this._setListStateMeta('empty', emptyMessage, 0);
            this.updateSelectionMeta([]);
            return;
        }

        tbody.innerHTML = items.map(item => `
            <tr>
                <td class="ps-4">
                    <input type="checkbox"
                           data-role="select-product"
                           data-product-no="${item.productNo}"
                           ${this.selectedProductNos.has(item.productNo) ? 'checked' : ''}>
                </td>
                <td class="ps-4">
                    <div class="product-info">
                        <div class="product-thumb-container" style="width:56px; height:56px;">
                            <img src="${item.thumbnailUrl || ''}"
                                 class="product-thumb" alt="thumb"
                                 onerror="CommonJS.handleImageError(this)">
                        </div>
                        <div class="product-details">
                            <div class="product-name decoration" data-id="${item.productNo}" style="cursor:pointer;">
                                ${item.productName}
                            </div>
                            <div class="product-subtitle text-muted" style="font-size:0.8rem;">${item.productModel || '-'}</div>
                        </div>
                    </div>
                </td>
                <td><span class="badge badge-model">${item.productModel || 'N/A'}</span></td>
                <td><strong>${item.brandName}</strong></td>
                <td><strong>${item.releasePrice || '-'}</strong></td>
                <td>${(item.totalStock ?? 0).toLocaleString()}개</td>
                <td>
                    <span class="badge ${CommonJS.getProductStatusMeta(item.statusCode).badgeClass}">
                        ${item.statusDesc}
                    </span>
                </td>
                <td class="small text-muted">${item.crtDtm}</td>
                <td class="text-end pe-4">
                    <div class="btn-group me-1">
                        <button type="button" class="btn btn-sm btn-outline-dark dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false">
                            상태
                        </button>
                        <ul class="dropdown-menu dropdown-menu-end">
                            ${this.renderQuickStatusMenu(item)}
                        </ul>
                    </div>
                    <button type="button"
                            class="btn btn-icon btn-secondary me-1 btn-image-search"
                            data-product-name="${item.productName || ''}"
                            data-model-num="${item.productModel || ''}"
                            data-brand-name="${item.brandName || ''}"
                            title="실제 이미지 검색">
                        <i class="fas fa-image"></i>
                    </button>
                    <button type="button"
                            class="btn btn-icon btn-secondary me-1"
                            data-role="clone-product"
                            data-product-no="${item.productNo}"
                            ${this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy) ? `disabled title="${CommonJS.getAdminWriteBlockedReason('상품 복제')}"` : ''}>
                        <i class="fas fa-copy"></i>
                    </button>
                    <button type="button"
                            class="btn btn-icon btn-secondary me-1"
                            data-role="edit-product"
                            data-product-no="${item.productNo}"
                            ${this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy) ? `disabled title="${CommonJS.getAdminWriteBlockedReason('상품 수정')}"` : ''}>
                        <i class="fas fa-edit"></i>
                    </button>
                    <button type="button"
                            class="btn btn-icon btn-secondary"
                            data-role="delete-product"
                            data-product-no="${item.productNo}"
                            ${this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy) ? `disabled title="${CommonJS.getAdminWriteBlockedReason('상품 삭제')}"` : ''}>
                        <i class="fas fa-trash text-danger"></i>
                    </button>
                </td>
            </tr>
        `).join('');
        this._setListStateMeta('ready', '', items.length);
        this.updateSelectionMeta(items);
    },

    renderQuickStatusMenu(item) {
        const options = [
            {code: 'ACTIVE', label: '판매중'},
            {code: 'HIDDEN', label: '숨김'},
            {code: 'SOLD_OUT', label: '품절'}
        ];
        return options.map((option) => `
            <li>
                <button type="button"
                        class="dropdown-item ${item.statusCode === option.code ? 'active' : ''}"
                        data-role="quick-operate-product"
                        data-product-no="${item.productNo}"
                        data-status="${option.code}">
                    ${option.label}${item.statusCode === option.code ? ' 적용중' : ''}
                </button>
            </li>
        `).join('');
    },

    _renderPagination(data) {
        const { totalPages, currentPage: curr, totalElements, resultMeta } = data;
        const pagination = document.getElementById('pagination');
        const pageMetaText = document.getElementById('pageMetaText');
        let html = '';
        for (let i = 0; i < totalPages; i++) {
            html += `
            <li class="page-item ${i === curr ? 'active' : ''}">
                <button type="button" class="page-link" data-role="go-product-page" data-page="${i}">${i + 1}</button>
            </li>`;
        }
        pagination.innerHTML = html;
        const listCountLabel = resultMeta?.resultLabel || (
            this._hasActiveFilters()
                ? `검색 결과 ${totalElements.toLocaleString()}개`
                : `전체 ${totalElements.toLocaleString()}개`
        );
        const pageInfoLabel = resultMeta?.pageInfoLabel || (
            totalElements === 0
                ? '조건에 맞는 상품이 없습니다.'
                : `${listCountLabel} / ${Math.max(totalPages, 1)}페이지`
        );

        document.getElementById('totalElementsCount').textContent = listCountLabel;
        document.getElementById('pageInfoText').textContent = pageInfoLabel;
        if (pageMetaText) {
            const pageMetaLabel = resultMeta
                ? `페이지 크기 ${resultMeta.pageSize} · ${resultMeta.rangeStart}-${resultMeta.rangeEnd}`
                : `페이지 크기 ${this.state.size} · 0-0`;
            pageMetaText.textContent = pageMetaLabel;
        }
        this.updateSelectionMeta(Array.isArray(data.products) ? data.products : []);
    },

    _updateStats(stats) {
        if (!stats) return;

        const map = {
            'stat-total-count':  stats.totalCount,
            'stat-active-count': stats.activeCount,
            'stat-low-stock':    stats.lowStockCount,
            'stat-today-count':  stats.todayCount,
        };

        Object.entries(map).forEach(([id, val]) => {
            const el = document.getElementById(id);
            if (el) el.textContent = (val || 0).toLocaleString();
        });

        const lowStockThresholdEl = document.getElementById('stat-low-stock-threshold');
        if (lowStockThresholdEl) {
            lowStockThresholdEl.textContent = `${stats.lowStockThreshold || this.defaultLowStockThreshold}개 미만`;
        }

        const contextLabel = stats.contextLabel || '현재 목록 기준';
        document.getElementById('stat-total-meta')?.replaceChildren(contextLabel);
        document.getElementById('stat-active-meta')?.replaceChildren(contextLabel);
        document.getElementById('stat-today-meta')?.replaceChildren(contextLabel);
        document.getElementById('stat-low-stock-meta')?.replaceChildren(contextLabel);
        this._renderStatsNotice(contextLabel, stats.querySignature || '');

        const statCardIds = ['statTotalCard', 'statActiveCard', 'statLowStockCard', 'statTodayCard'];
        statCardIds.forEach((id) => {
            const cardEl = document.getElementById(id);
            if (!cardEl) {
                return;
            }

            // 통계 카드도 서버가 계산한 목록 문맥을 그대로 들고 있어야 화면과 자동 검증 기준이 어긋나지 않습니다.
            cardEl.dataset.querySignature = stats.querySignature || this.lastResultMeta?.querySignature || '';
            cardEl.dataset.contextLabel = contextLabel;
        });
    },

    _renderStatsNotice(contextLabel, querySignature) {
        const noticeEl = document.getElementById('productStatsNotice');
        if (!noticeEl) {
            return;
        }

        const normalizedContext = contextLabel || '현재 목록 기준';
        const message = normalizedContext === '기본 필터 기준'
            ? '카드 수치는 기본 필터 기준이며, 선택한 빠른 필터는 목록에만 적용됩니다.'
            : '카드 수치는 현재 목록 기준입니다.';
        noticeEl.textContent = message;
        noticeEl.dataset.statsContext = normalizedContext;
        noticeEl.dataset.querySignature = querySignature;
    },

    _applyServerAppliedQuery(appliedQuery) {
        if (!appliedQuery) {
            return;
        }

        this.lastAppliedQuery = appliedQuery;
        this.state.brandNo = appliedQuery.brandNo ? String(appliedQuery.brandNo) : '';
        this.state.categoryNo = appliedQuery.categoryNo ? String(appliedQuery.categoryNo) : '';
        this.state.status = appliedQuery.statusCode || '';
        this.state.lowStockOnly = Boolean(appliedQuery.lowStockOnly);
        this.state.createdTodayOnly = Boolean(appliedQuery.createdTodayOnly);
        this.state.searchKeyword = appliedQuery.searchKeyword || '';
        this.state.orderType = appliedQuery.orderTypeCode || 'r';

        // 서버 기준으로 실제 조회된 조건을 다시 맞춰야 프런트 요약과 QueryDSL 조건이 어긋나지 않습니다.
        this.state.lowStockThreshold = this._normalizeLowStockThreshold(
            appliedQuery.lowStockThreshold || this.state.lowStockThreshold
        );

        this._syncFilterInputs();
        this._syncUrlState();
    },

    _showError(message = '데이터 로드 중 오류가 발생했습니다.') {
        document.getElementById('productListTableBody').innerHTML = `
            <tr>
                <td colspan="9" class="py-5">
                    <div class="product-empty-state">
                        <div class="product-empty-state__icon text-danger">
                            <i class="fas fa-triangle-exclamation"></i>
                        </div>
                        <strong>상품 목록을 불러오지 못했습니다.</strong>
                        <p>${message}</p>
                    </div>
                </td>
            </tr>`;
        const pageInfoText = document.getElementById('pageInfoText');
        const totalElementsCount = document.getElementById('totalElementsCount');
        const pageMetaText = document.getElementById('pageMetaText');
        if (pageInfoText) {
            pageInfoText.textContent = message;
        }
        if (totalElementsCount) {
            totalElementsCount.textContent = '조회 실패';
        }
        if (pageMetaText) {
            pageMetaText.textContent = '페이지 메타 확인 불가';
        }
        this._setListStateMeta('error', message, 0);
    },

    _setLoadingState(isLoading) {
        const tbody = document.getElementById('productListTableBody');
        const pageInfoText = document.getElementById('pageInfoText');
        const totalElementsCount = document.getElementById('totalElementsCount');

        if (isLoading) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="9" class="py-5">
                        <div class="product-loading-state">
                            <div class="spinner-border spinner-border-sm text-primary" role="status" aria-hidden="true"></div>
                            <strong>상품 목록을 다시 불러오는 중입니다.</strong>
                            <p>현재 필터 조건에 맞는 결과를 조회하고 있습니다.</p>
                        </div>
                    </td>
                </tr>
            `;
            if (pageInfoText) {
                pageInfoText.textContent = '상품 목록 조회 중';
            }
            if (totalElementsCount) {
                totalElementsCount.textContent = '조회 중...';
            }
            const pageMetaText = document.getElementById('pageMetaText');
            if (pageMetaText) {
                pageMetaText.textContent = '페이지 메타 계산 중';
            }
            this._setListStateMeta('loading', '상품 목록 조회 중', 0);
        }
    },

    goPage(page) {
        if (!Number.isInteger(page) || page < 0) {
            void CommonJS.alert('이동할 페이지 정보가 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        this.state.page = page;
        this.getList();
    },

    resetFilters() {
        this.state = {
            page: 0,
            size: this.state.size,
            brandNo: '',
            categoryNo: '',
            status: '',
            lowStockOnly: false,
            lowStockThreshold: this.state.lowStockThreshold,
            createdTodayOnly: false,
            searchKeyword: '',
            orderType: 'r',
        };
        this._syncFilterInputs();
        this._renderFilterSummary();
        this.getList();
    },

    clearQuickFilters() {
        this.state.page = 0;
        // 상단 카드는 기본 탐색 문맥(브랜드/카테고리/검색)을 유지한 채 빠른 필터만 걷어내야 합니다.
        this.state.status = '';
        this.state.lowStockOnly = false;
        this.state.createdTodayOnly = false;
        this._syncFilterInputs();
        this._renderFilterSummary();
        this.getList();
    },

    applyLowStockFilter() {
        this.state.page = 0;
        this.state.status = '';
        this.state.lowStockOnly = true;
        this.state.createdTodayOnly = false;
        this._syncFilterInputs();
        this.getList();
    },

    applyActiveFilter() {
        this.state.page = 0;
        this.state.status = 'ACTIVE';
        this.state.lowStockOnly = false;
        this.state.createdTodayOnly = false;
        this._syncFilterInputs();
        this.getList();
    },

    applyTodayFilter() {
        this.state.page = 0;
        this.state.createdTodayOnly = true;
        this.state.status = '';
        this.state.lowStockOnly = false;
        this._syncFilterInputs();
        this.getList();
    },

    applySearchFilter() {
        this.state.page = 0;
        this._updateStateFromInputs();
        this.getList();
    },

    async deleteProduct(no) {
        if (this.isDeletingProduct) {
            return;
        }
        if (!this._isPositiveNumber(no)) {
            await CommonJS.alert('상품 번호가 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('상품 삭제'), '알림', 'warning');
            return;
        }

        const isConfirm = await CommonJS.confirm('정말로 이 상품을 삭제하시겠습니까?', '상품 삭제 확인', 'error');
        if (!isConfirm) return;

        try {
            this.isDeletingProduct = true;
            this._setDeleteButtonsDisabled(true);
            const response = await fetch(`/api/admin/product/delete/${no}`, {
                method: 'PATCH'
            });

            if (response.ok) {
                this.selectedProductNos.delete(Number(no));
                await this.getList();
                await CommonJS.alert('삭제되었습니다.', '성공', 'success');
            } else {
                const message = await CommonJS.extractErrorMessage(response, '삭제에 실패했습니다.');
                await CommonJS.alert(message, '오류', 'error');
            }
        } catch (error) {
            console.error('Delete Error:', error);
            await CommonJS.alert('삭제 처리 중 오류가 발생했습니다.', '오류', 'error');
        } finally {
            this.isDeletingProduct = false;
            this._setDeleteButtonsDisabled(false);
        }
    },

    _setDeleteButtonsDisabled(disabled) {
        document.querySelectorAll('[data-role="delete-product"]').forEach((button) => {
            button.disabled = disabled;
        });
    },

    async cloneProduct(productNo) {
        if (this.isCloningProduct) {
            return;
        }
        if (!this._isPositiveNumber(productNo)) {
            await CommonJS.alert('상품 번호가 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('상품 복제'), '알림', 'warning');
            return;
        }

        const confirmed = await CommonJS.confirm('선택한 상품을 복제하시겠습니까?', '상품 복제', 'info');
        if (!confirmed) {
            return;
        }

        try {
            this.isCloningProduct = true;
            this._setCloneButtonsDisabled(true);
            const response = await fetch(`/api/admin/product/clone/${productNo}`, {
                method: 'POST'
            });

            if (!response.ok) {
                const message = await CommonJS.extractErrorMessage(response, '상품 복제에 실패했습니다.');
                await CommonJS.alert(message, '오류', 'error');
                return;
            }

            const result = await response.json();
            await this.getList();
            await CommonJS.alert(`상품이 복제되었습니다. 생성 번호: ${result.productNo}`, '성공', 'success');
        } catch (error) {
            console.error('Clone Error:', error);
            await CommonJS.alert('복제 처리 중 오류가 발생했습니다.', '오류', 'error');
        } finally {
            this.isCloningProduct = false;
            this._setCloneButtonsDisabled(false);
        }
    },

    _setCloneButtonsDisabled(disabled) {
        document.querySelectorAll('[data-role="clone-product"]').forEach((button) => {
            button.disabled = disabled;
        });
    },

    async quickOperateProduct(productNo, status) {
        if (this.quickOperateInFlight.has(productNo)) {
            return;
        }
        const normalizedStatus = this._normalizeProductStatus(status);
        if (!this._isPositiveNumber(productNo)) {
            await CommonJS.alert('상품 번호가 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        if (!normalizedStatus) {
            await CommonJS.alert('변경할 상태 값이 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('상품 상태 변경'), '알림', 'warning');
            return;
        }

        try {
            this.quickOperateInFlight.add(productNo);
            this._setQuickOperateButtonsDisabled(productNo, true);
            const response = await fetch(`/api/admin/product/${productNo}/quick-operate`, {
                method: 'PATCH',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({status: normalizedStatus})
            });

            if (!response.ok) {
                const message = await CommonJS.extractErrorMessage(response, '상품 상태 변경에 실패했습니다.');
                await CommonJS.alert(message, '오류', 'error');
                return;
            }

            const result = await response.json();
            await this.getList();
            await CommonJS.alert(
                `상태 변경 완료\n변경 ${result.updatedCount}건 · 동일 상태 ${result.unchangedCount}건`,
                '성공',
                'success'
            );
        } catch (error) {
            console.error('상품 빠른 상태 변경 실패:', error);
            await CommonJS.alert('상품 상태 변경 중 오류가 발생했습니다.', '오류', 'error');
        } finally {
            this.quickOperateInFlight.delete(productNo);
            this._setQuickOperateButtonsDisabled(productNo, false);
        }
    },

    _setQuickOperateButtonsDisabled(productNo, disabled) {
        document.querySelectorAll(`[data-role="quick-operate-product"][data-product-no="${productNo}"]`).forEach((button) => {
            button.disabled = disabled;
        });
    },

    toggleSelection(productNo, checked) {
        if (!Number.isFinite(productNo) || productNo <= 0) {
            return;
        }

        if (checked) {
            this.selectedProductNos.add(productNo);
        } else {
            this.selectedProductNos.delete(productNo);
        }

        const items = Array.from(document.querySelectorAll('[data-role="select-product"]'))
            .map((checkbox) => ({productNo: this._normalizeOptionalPositiveNumber(checkbox.dataset.productNo)}))
            .filter((item) => this._isPositiveNumber(item.productNo));
        this.updateSelectionMeta(items);
    },

    toggleSelectCurrentPage(checked) {
        document.querySelectorAll('[data-role="select-product"]').forEach((checkbox) => {
            const productNo = this._normalizeOptionalPositiveNumber(checkbox.dataset.productNo);
            if (!this._isPositiveNumber(productNo)) {
                checkbox.checked = false;
                return;
            }
            checkbox.checked = checked;
            if (checked) {
                this.selectedProductNos.add(productNo);
                return;
            }
            this.selectedProductNos.delete(productNo);
        });

        const items = Array.from(document.querySelectorAll('[data-role="select-product"]'))
            .map((checkbox) => ({productNo: this._normalizeOptionalPositiveNumber(checkbox.dataset.productNo)}))
            .filter((item) => this._isPositiveNumber(item.productNo));
        this.updateSelectionMeta(items);
    },

    clearSelection() {
        this.selectedProductNos.clear();
        const selectPage = document.getElementById('productSelectPage');
        if (selectPage) {
            selectPage.checked = false;
        }
        document.querySelectorAll('[data-role="select-product"]').forEach((checkbox) => {
            checkbox.checked = false;
        });
        this.updateSelectionMeta([]);
    },

    updateSelectionMeta(items) {
        const totalSelected = this.selectedProductNos.size;
        const visibleProductNos = new Set((items || []).map((item) => item.productNo));
        const visibleSelected = Array.from(this.selectedProductNos).filter((productNo) => visibleProductNos.has(productNo)).length;
        const metaEl = document.getElementById('productSelectionMeta');
        if (metaEl) {
            metaEl.textContent = totalSelected === 0
                ? '선택된 상품이 없습니다.'
                : `총 ${totalSelected}건 선택 · 현재 페이지 ${visibleSelected}건`;
        }

        const selectPage = document.getElementById('productSelectPage');
        if (selectPage) {
            const selectableCount = visibleProductNos.size;
            selectPage.checked = selectableCount > 0 && visibleSelected === selectableCount;
        }
    },

    async applyBulkOperation() {
        if (this.bulkInFlight) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('상품 일괄 변경'), '알림', 'warning');
            return;
        }
        if (!this.selectedProductNos.size) {
            await CommonJS.alert('일괄 변경할 상품을 선택해주세요.', '알림', 'warning');
            return;
        }

        const status = this._normalizeProductStatus(document.getElementById('bulkProductStatus')?.value);
        if (!status) {
            await CommonJS.alert('변경할 상태를 선택해주세요.', '알림', 'warning');
            return;
        }

        const confirmed = await CommonJS.confirm(`선택한 상품 ${this.selectedProductNos.size}건의 상태를 일괄 변경하시겠습니까?`, '상품 일괄 변경', 'warning');
        if (!confirmed) {
            return;
        }

        try {
            this.bulkInFlight = true;
            const response = await fetch('/api/admin/product/bulk-operate', {
                method: 'PATCH',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                    productNos: Array.from(this.selectedProductNos),
                    status
                })
            });
            if (!response.ok) {
                const message = await CommonJS.extractErrorMessage(response, '상품 일괄 변경에 실패했습니다.');
                await CommonJS.alert(message, '오류', 'error');
                return;
            }

            const result = await response.json();
            document.getElementById('bulkProductStatus').value = '';
            this.clearSelection();
            await this.getList();
            await CommonJS.alert(
                `일괄 변경 완료\n변경 ${result.updatedCount}건 · 동일 상태 ${result.unchangedCount}건 · 삭제 제외 ${result.blockedCount}건 · 누락 ${result.missingCount}건`,
                '성공',
                'success'
            );
        } catch (error) {
            console.error('상품 일괄 변경 실패:', error);
            await CommonJS.alert('상품 일괄 변경 중 오류가 발생했습니다.', '오류', 'error');
        } finally {
            this.bulkInFlight = false;
        }
    },

    async applyBulkDelete() {
        if (this.bulkInFlight) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('상품 일괄 삭제'), '알림', 'warning');
            return;
        }
        if (!this.selectedProductNos.size) {
            await CommonJS.alert('일괄 삭제할 상품을 선택해주세요.', '알림', 'warning');
            return;
        }

        const confirmed = await CommonJS.confirm(`선택한 상품 ${this.selectedProductNos.size}건을 일괄 삭제하시겠습니까?`, '상품 일괄 삭제', 'error');
        if (!confirmed) {
            return;
        }

        try {
            this.bulkInFlight = true;
            const response = await fetch('/api/admin/product/bulk-delete', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                    productNos: Array.from(this.selectedProductNos)
                })
            });
            if (!response.ok) {
                const message = await CommonJS.extractErrorMessage(response, '상품 일괄 삭제에 실패했습니다.');
                await CommonJS.alert(message, '오류', 'error');
                return;
            }

            const result = await response.json();
            this.clearSelection();
            await this.getList();
            await CommonJS.alert(
                `일괄 삭제 완료\n삭제 ${result.deletedCount}건 · 기삭제 ${result.alreadyDeletedCount}건 · 누락 ${result.missingCount}건`,
                '성공',
                'success'
            );
        } catch (error) {
            console.error('상품 일괄 삭제 실패:', error);
            await CommonJS.alert('상품 일괄 삭제 중 오류가 발생했습니다.', '오류', 'error');
        } finally {
            this.bulkInFlight = false;
        }
    },

    async applyBulkDuplicate() {
        if (this.bulkInFlight) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('상품 일괄 복제'), '알림', 'warning');
            return;
        }
        if (!this.selectedProductNos.size) {
            await CommonJS.alert('복제할 상품을 선택해주세요.', '알림', 'warning');
            return;
        }

        const confirmed = await CommonJS.confirm(`선택한 상품 ${this.selectedProductNos.size}건을 복제하시겠습니까?`, '상품 일괄 복제', 'info');
        if (!confirmed) {
            return;
        }

        try {
            this.bulkInFlight = true;
            const response = await fetch('/api/admin/product/bulk-duplicate', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                    productNos: Array.from(this.selectedProductNos)
                })
            });
            if (!response.ok) {
                const message = await CommonJS.extractErrorMessage(response, '상품 일괄 복제에 실패했습니다.');
                await CommonJS.alert(message, '오류', 'error');
                return;
            }

            const result = await response.json();
            this.clearSelection();
            await this.getList();
            await CommonJS.alert(
                `일괄 복제 완료\n생성 ${result.createdCount}건 · 삭제 제외 ${result.blockedCount}건 · 누락 ${result.missingCount}건`,
                '성공',
                'success'
            );
        } catch (error) {
            console.error('상품 일괄 복제 실패:', error);
            await CommonJS.alert('상품 일괄 복제 중 오류가 발생했습니다.', '오류', 'error');
        } finally {
            this.bulkInFlight = false;
        }
    },

    getReturnTo() {
        const query = this.buildQueryString();
        return query ? `${window.location.pathname}?${query}` : window.location.pathname;
    },

    buildQueryString() {
        const params = new URLSearchParams();
        params.set('page', this.state.page);
        params.set('size', this.state.size);

        if (this.state.brandNo) params.set('brandNo', this.state.brandNo);
        if (this.state.categoryNo) params.set('categoryNo', this.state.categoryNo);
        if (this.state.status) params.set('status', this.state.status);
        if (this.state.lowStockOnly) params.set('lowStockOnly', 'true');
        if (this.state.lowStockOnly && this.state.lowStockThreshold && this.state.lowStockThreshold !== this.defaultLowStockThreshold) {
            params.set('lowStockThreshold', this.state.lowStockThreshold);
        }
        if (this.state.createdTodayOnly) params.set('createdTodayOnly', 'true');
        if (this.state.searchKeyword) params.set('searchKeyword', this.state.searchKeyword);
        if (this.state.orderType && this.state.orderType !== 'r') params.set('orderType', this.state.orderType);
        if (this.state.source) params.set('source', this.state.source);
        if (this.state.returnTo) params.set('returnTo', this.state.returnTo);

        return params.toString();
    },

    _readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        this.state.page = this._normalizePage(params.get('page'));
        this.state.size = this._normalizePageSize(params.get('size'));
        this.state.brandNo = this._normalizeOptionalPositiveNumber(params.get('brandNo'));
        this.state.categoryNo = this._normalizeOptionalPositiveNumber(params.get('categoryNo'));
        this.state.status = this._normalizeProductStatus(params.get('status'));
        this.state.lowStockOnly = params.get('lowStockOnly') === 'true';
        this.state.lowStockThreshold = this._normalizeLowStockThreshold(params.get('lowStockThreshold') || this.defaultLowStockThreshold);
        this.state.createdTodayOnly = params.get('createdTodayOnly') === 'true';
        this.state.searchKeyword = CommonJS.normalizeOptionalText(params.get('searchKeyword')) || '';
        this.state.orderType = this._normalizeOrderType(params.get('orderType'));
        this.state.source = params.get('source') || '';
        this.state.returnTo = params.get('returnTo') || '';
    },

    _syncFilterInputs() {
        document.getElementById('brandNo').value = this.state.brandNo;
        document.getElementById('categoryNo').value = this.state.categoryNo;
        document.getElementById('statusFilter').value = this.state.status;
        document.getElementById('lowStockOnly').checked = this.state.lowStockOnly;
        document.getElementById('lowStockThreshold').value = this._normalizeLowStockThreshold(this.state.lowStockThreshold);
        this._syncLowStockThresholdAvailability();
        document.getElementById('createdTodayOnly').checked = this.state.createdTodayOnly;
        document.getElementById('searchKeyword').value = this.state.searchKeyword;
        document.getElementById('pageSize').value = String(this.state.size);

        const orderButton = document.getElementById('orderType');
        // 목록 문맥은 상세/수정 복귀에도 쓰이므로 URL 상태와 버튼 표시를 함께 맞춥니다.
        const orderTypeLabel = {
            r: '최신순',
            p: '발매가순',
            c: '재고순',
        }[this.state.orderType] || '최신순';

        orderButton.setAttribute('data-current-value', this.state.orderType);
        orderButton.textContent = orderTypeLabel;
    },

    _renderFilterSummary() {
        const summaryEl = document.getElementById('productFilterSummary');
        const titleEl = document.getElementById('productFilterSummaryTitle');
        const descEl = document.getElementById('productFilterSummaryDescription');
        const metaEl = document.getElementById('productFilterSummaryMeta');
        const signatureEl = document.getElementById('productFilterSummarySignature');
        const chipsEl = document.getElementById('productFilterSummaryChips');
        if (!titleEl || !descEl || !chipsEl) {
            return;
        }

        const statusTextMap = {
            ACTIVE: '판매중 상품',
            HIDDEN: '숨김 상품',
            SOLD_OUT: '품절 상품',
        };

        let title = '전체 상품';
        if (this.state.createdTodayOnly) {
            title = '오늘 등록 상품';
        } else if (this.state.lowStockOnly) {
            title = '품절 임박 상품';
        } else if (this.state.status) {
            title = statusTextMap[this.state.status] || '상태 필터 상품';
        }

        const chips = [];
        if (this.state.brandNo) {
            chips.push(this._createFilterChip('brandNo', 'fa-tags', this._findSelectLabel('brandNo')));
        }
        if (this.state.categoryNo) {
            chips.push(this._createFilterChip('categoryNo', 'fa-layer-group', this._findSelectLabel('categoryNo')));
        }
        if (this.state.status) {
            chips.push(this._createFilterChip('status', 'fa-circle-check', statusTextMap[this.state.status] || this.state.status));
        }
        if (this.state.lowStockOnly) {
            chips.push(this._createFilterChip('lowStockOnly', 'fa-triangle-exclamation', `재고 ${this.state.lowStockThreshold}개 미만`));
        }
        if (this.state.createdTodayOnly) {
            chips.push(this._createFilterChip('createdTodayOnly', 'fa-calendar-day', '오늘 등록만'));
        }
        if (this.state.searchKeyword) {
            chips.push(this._createFilterChip('searchKeyword', 'fa-magnifying-glass', `검색: ${this.state.searchKeyword}`));
        }

        const summaryDesc = this.lastResultMeta
            ? `${this.lastResultMeta.appliedFilterCount}개의 필터 조건이 적용되어 있습니다.`
            : (chips.length ? `${chips.length}개의 필터 조건이 적용되어 있습니다.` : '모든 상품을 보고 있습니다.');

        titleEl.textContent = title;
        descEl.textContent = this.lastResultMeta?.hasActiveFilters ? summaryDesc : '모든 상품을 보고 있습니다.';
        if (metaEl) {
            const appliedText = this._hasPendingSearchInput()
                ? '검색어 변경 미적용'
                : (this.lastResultMeta ? '현재 목록 기준' : '브라우저 기본 상태');
            const orderTypeLabel = this.lastResultMeta?.orderTypeLabel || '최신순';
            metaEl.textContent = `정렬: ${orderTypeLabel} · ${appliedText}`;
        }
        if (signatureEl) {
            signatureEl.textContent = this.lastResultMeta?.querySignature || '최신순';
        }
        if (summaryEl) {
            // 브라우저 자동 검증은 보이는 문구보다 안정적인 속성 기준점이 있어야 회귀를 덜 놓칩니다.
            summaryEl.dataset.querySignature = this.lastResultMeta?.querySignature || '';
            summaryEl.dataset.resultLabel = this.lastResultMeta?.resultLabel || '';
            summaryEl.dataset.filterCount = String(this.lastResultMeta?.appliedFilterCount ?? chips.length);
        }
        chipsEl.innerHTML = chips.length ? chips.join('') : this._createStaticFilterChip('fa-sliders', '추가 필터 없음');
        this._syncStatCardState();
    },

    _syncStatCardState() {
        const activeMap = {
            statTotalCard: !this.state.status && !this.state.lowStockOnly && !this.state.createdTodayOnly,
            statActiveCard: this.state.status === 'ACTIVE' && !this.state.lowStockOnly && !this.state.createdTodayOnly,
            statLowStockCard: this.state.lowStockOnly,
            statTodayCard: this.state.createdTodayOnly,
        };

        Object.entries(activeMap).forEach(([id, isActive]) => {
            document.getElementById(id)?.classList.toggle('stat-card-active', isActive);
        });
    },

    _findSelectLabel(selectId) {
        const select = document.getElementById(selectId);
        if (!select) {
            return '';
        }

        return select.options[select.selectedIndex]?.text || '';
    },

    _createFilterChip(filterKey, iconClass, label) {
        return `
            <span class="product-filter-chip">
                <i class="fas ${iconClass}"></i>
                <span>${label}</span>
                <button type="button" class="product-filter-chip-remove" data-filter-remove="${filterKey}" aria-label="${label} 필터 해제">
                    <i class="fas fa-xmark"></i>
                </button>
            </span>
        `;
    },

    _createStaticFilterChip(iconClass, label) {
        return `<span class="product-filter-chip"><i class="fas ${iconClass}"></i><span>${label}</span></span>`;
    },

    _buildEmptyStateMessage() {
        if (this.state.searchKeyword) {
            return `"${this.state.searchKeyword}" 검색 결과가 없습니다. 필터를 줄이거나 검색어를 바꿔보세요.`;
        }
        if (this.state.brandNo || this.state.categoryNo || this.state.status || this.state.lowStockOnly || this.state.createdTodayOnly) {
            return '현재 필터 조합에 맞는 상품이 없습니다. 필터를 일부 해제해 보세요.';
        }

        return '등록된 상품이 아직 없거나, 현재 페이지에 표시할 데이터가 없습니다.';
    },

    _hasActiveFilters() {
        return Boolean(
            this.state.brandNo ||
            this.state.categoryNo ||
            this.state.status ||
            this.state.lowStockOnly ||
            this.state.createdTodayOnly ||
            this.state.searchKeyword
        );
    },

    _normalizeLowStockThreshold(value) {
        // URL 조작이나 오래된 북마크로 허용되지 않은 값이 들어오면 기본 임계값으로 수렴시킵니다.
        return this.allowedLowStockThresholds.includes(String(value))
            ? String(value)
            : (this.defaultLowStockThreshold || '100');
    },

    _syncLowStockThresholdAvailability() {
        const lowStockThresholdSelect = document.getElementById('lowStockThreshold');
        if (!lowStockThresholdSelect) {
            return;
        }

        lowStockThresholdSelect.disabled = !this.state.lowStockOnly;
    },

    clearFilter(filterKey) {
        this.state.page = 0;

        // 카드 shortcut 필터와 일반 폼 필터가 같은 state를 공유하므로 해제도 같은 state에서 정리합니다.
        const clearActions = {
            brandNo: () => { this.state.brandNo = ''; },
            categoryNo: () => { this.state.categoryNo = ''; },
            status: () => { this.state.status = ''; },
            lowStockOnly: () => { this.state.lowStockOnly = false; },
            createdTodayOnly: () => { this.state.createdTodayOnly = false; },
            searchKeyword: () => { this.state.searchKeyword = ''; },
        };

        clearActions[filterKey]?.();
        this._syncFilterInputs();
        this.getList();
    },

    _updateStateFromInputs() {
        this.state.brandNo = this._normalizeOptionalPositiveNumber(document.getElementById('brandNo').value);
        this.state.categoryNo = this._normalizeOptionalPositiveNumber(document.getElementById('categoryNo').value);
        this.state.status = this._normalizeProductStatus(document.getElementById('statusFilter').value);
        this.state.lowStockOnly = document.getElementById('lowStockOnly').checked;
        this.state.lowStockThreshold = this._normalizeLowStockThreshold(document.getElementById('lowStockThreshold').value);
        this.state.createdTodayOnly = document.getElementById('createdTodayOnly').checked;
        this.state.searchKeyword = document.getElementById('searchKeyword').value.trim().replaceAll(/\s+/g, ' ');
        this.state.size = this._normalizePageSize(document.getElementById('pageSize').value || 10);
        const orderType = document.getElementById('orderType').getAttribute('data-current-value') || 'r';
        this.state.orderType = this._normalizeOrderType(orderType);
    },

    _hasPendingSearchInput() {
        const searchInput = document.getElementById('searchKeyword');
        if (!searchInput) {
            return false;
        }

        // 검색창은 Enter/검색 버튼 전까지 state에 반영되지 않으므로 현재 목록 기준과 분리해서 봅니다.
        const normalizedKeyword = searchInput.value.trim().replaceAll(/\s+/g, ' ');
        return normalizedKeyword !== this.state.searchKeyword;
    },

    bindStatCardKeyboard(elementId, action) {
        document.getElementById(elementId)?.addEventListener('keypress', (event) => {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                action();
            }
        });
    },

    validateState() {
        // 검색/다운로드가 같은 요청 경계를 타므로 프런트에서도 같은 길이 제한을 먼저 맞춥니다.
        if (this.state.searchKeyword && this.state.searchKeyword.length > 50) {
            void CommonJS.alert('검색어는 50자 이하로 입력해주세요.', '알림', 'warning');
            return false;
        }
        if (this.state.brandNo && !this._isPositiveNumber(this.state.brandNo)) {
            void CommonJS.alert('브랜드 필터 값이 올바르지 않습니다.', '알림', 'warning');
            return false;
        }
        if (this.state.categoryNo && !this._isPositiveNumber(this.state.categoryNo)) {
            void CommonJS.alert('카테고리 필터 값이 올바르지 않습니다.', '알림', 'warning');
            return false;
        }
        if (this.state.status && !this._normalizeProductStatus(this.state.status)) {
            void CommonJS.alert('상품 상태 필터 값이 올바르지 않습니다.', '알림', 'warning');
            return false;
        }
        if (this.state.orderType !== this._normalizeOrderType(this.state.orderType)) {
            void CommonJS.alert('정렬 값이 올바르지 않습니다.', '알림', 'warning');
            return false;
        }

        return true;
    },

    _normalizePage(page) {
        const parsed = Number(page);
        return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0;
    },

    _normalizePageSize(size) {
        const parsed = Number(size);
        return Number.isInteger(parsed) && parsed > 0 ? parsed : 10;
    },

    _normalizeProductStatus(value) {
        return ['ACTIVE', 'HIDDEN', 'SOLD_OUT'].includes(value) ? value : '';
    },

    _normalizeOrderType(value) {
        return ['r', 'p', 'c'].includes(value) ? value : 'r';
    },

    _normalizeOptionalPositiveNumber(value) {
        return this._isPositiveNumber(value) ? String(Number(value)) : '';
    },

    _isPositiveNumber(value) {
        return /^\d+$/.test(String(value || '')) && Number(value) > 0;
    },

    _syncUrlState() {
        const query = this.buildQueryString();
        const url = query ? `${window.location.pathname}?${query}` : window.location.pathname;
        history.pushState(null, '', url);
    },

    _setListStateMeta(state, message, visibleCount) {
        const stateMetaEl = document.getElementById('productListStateMeta');
        if (!stateMetaEl) {
            return;
        }

        // 인앱 브라우저 검증은 렌더된 문구보다 안정적인 dataset 기준점이 있어야 회귀를 덜 놓칩니다.
        stateMetaEl.dataset.listState = state;
        stateMetaEl.dataset.stateMessage = message || '';
        stateMetaEl.dataset.totalElements = String(this.lastTotalElements);
        stateMetaEl.dataset.visibleCount = String(visibleCount ?? 0);
        stateMetaEl.dataset.querySignature = this.lastResultMeta?.querySignature || this._buildClientQuerySignature();
    },

    _buildClientQuerySignature() {
        const signatureParts = [];
        const orderTypeLabel = {
            r: '최신순',
            p: '발매가순',
            c: '재고순',
        }[this.state.orderType] || '최신순';

        signatureParts.push(orderTypeLabel);
        if (this.state.searchKeyword) {
            signatureParts.push(`검색=${this.state.searchKeyword}`);
        }
        if (this.state.status) {
            signatureParts.push(`상태=${this.state.status}`);
        }
        if (this.state.lowStockOnly) {
            signatureParts.push(`재고<${this.state.lowStockThreshold}`);
        }
        if (this.state.createdTodayOnly) {
            signatureParts.push('오늘등록');
        }

        return signatureParts.join(' · ');
    }
};
