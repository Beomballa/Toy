const ProductList = {
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
    },
    requestSequence: 0,
    activeRequestController: null,
    lastAppliedQuery: null,
    lastResultMeta: null,
    lastErrorMessage: '',

    init(brands = [], categories = []) {
        this._fillSelect('brandNo',    brands,     'brandNo',    'nameKo');
        this._fillSelect('categoryNo', categories, 'categoryNo', 'name');

        this._readStateFromUrl();
        this._syncFilterInputs();
        this._renderFilterSummary();
        this._bindEvents();
        this._initAnimations();
        this.getList(); // 초기 로드

        document.getElementById('new-product')?.addEventListener('click', () => location.href = `/admin/products/set?returnTo=${encodeURIComponent(this.getReturnTo())}`);
        document.getElementById('btnSearchProducts')?.addEventListener('click', () => this.applySearchFilter());
        document.getElementById('btnExportProducts')?.addEventListener('click', () => {
            window.location.href = `/api/admin/product/export?${this.buildQueryString()}`;
        });
        document.getElementById('main-logo')?.addEventListener('click', () => location.href = '/admin/products');
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
                const productNo = productNameEl.dataset.id;
                location.href = `/admin/products/get?no=${productNo}&returnTo=${encodeURIComponent(this.getReturnTo())}`;
                return;
            }

            const imageSearchBtn = e.target.closest('.btn-image-search');
            if (imageSearchBtn) {
                CommonJS.openImageSearch(
                    imageSearchBtn.dataset.productName,
                    imageSearchBtn.dataset.modelNum,
                    imageSearchBtn.dataset.brandName
                );
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
        document.getElementById('statTotalCard')?.addEventListener('click', () => this.resetFilters());
        document.getElementById('statActiveCard')?.addEventListener('click', () => this.applyActiveFilter());
        document.getElementById('statLowStockCard')?.addEventListener('click', () => this.applyLowStockFilter());
        document.getElementById('statTodayCard')?.addEventListener('click', () => this.applyTodayFilter());
        this.bindStatCardKeyboard('statTotalCard', () => this.resetFilters());
        this.bindStatCardKeyboard('statActiveCard', () => this.applyActiveFilter());
        this.bindStatCardKeyboard('statLowStockCard', () => this.applyLowStockFilter());
        this.bindStatCardKeyboard('statTodayCard', () => this.applyTodayFilter());
        window.addEventListener('popstate', () => {
            this._readStateFromUrl();
            this._syncFilterInputs();
            this._renderFilterSummary();
            this.getList(false);
        });
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
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center py-5 text-muted">
                        <div class="product-empty-state">
                            <i class="fas fa-box-open product-empty-state-icon"></i>
                            <strong>조건에 맞는 상품이 없습니다.</strong>
                            <p>${this._buildEmptyStateMessage()}</p>
                            <button type="button" class="btn btn-sm btn-outline-secondary" data-role="reset-empty-product-filters">
                                필터 초기화
                            </button>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = items.map(item => `
            <tr>
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
                    <button type="button"
                            class="btn btn-icon btn-secondary me-1 btn-image-search"
                            data-product-name="${item.productName || ''}"
                            data-model-num="${item.productModel || ''}"
                            data-brand-name="${item.brandName || ''}"
                            title="실제 이미지 검색">
                        <i class="fas fa-image"></i>
                    </button>
                    <button type="button" class="btn btn-icon btn-secondary me-1" onclick="location.href='/admin/products/update?no=${item.productNo}&returnTo=${encodeURIComponent(ProductList.getReturnTo())}'">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button type="button" class="btn btn-icon btn-secondary" onclick="ProductList.deleteProduct('${item.productNo}')">
                        <i class="fas fa-trash text-danger"></i>
                    </button>
                </td>
            </tr>
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
                <a class="page-link" href="javascript:void(0);" onclick="ProductList.goPage(${i})">${i + 1}</a>
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
            lowStockThresholdEl.textContent = `${stats.lowStockThreshold || 100}개 미만`;
        }

        const contextLabel = stats.contextLabel || '현재 목록 기준';
        document.getElementById('stat-total-meta')?.replaceChildren(contextLabel);
        document.getElementById('stat-active-meta')?.replaceChildren(contextLabel);
        document.getElementById('stat-today-meta')?.replaceChildren(contextLabel);
        document.getElementById('stat-low-stock-meta')?.replaceChildren(contextLabel);

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
            <tr><td colspan="8" class="text-center py-5 text-danger">${message}</td></tr>`;
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
    },

    _setLoadingState(isLoading) {
        const tbody = document.getElementById('productListTableBody');
        const pageInfoText = document.getElementById('pageInfoText');
        const totalElementsCount = document.getElementById('totalElementsCount');

        if (isLoading) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center py-5 text-muted">
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
        }
    },

    goPage(page) {
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
        const isConfirm = await CommonJS.confirm('정말로 이 상품을 삭제하시겠습니까?', '상품 삭제 확인', 'error');
        if (!isConfirm) return;

        try {
            const response = await fetch(`/api/admin/product/delete/${no}`, {
                method: 'PATCH'
            });

            if (response.ok) {
                await CommonJS.alert('삭제되었습니다.', '성공', 'success');
                this.getList();
            } else {
                const message = await CommonJS.extractErrorMessage(response, '삭제에 실패했습니다.');
                await CommonJS.alert(message, '오류', 'error');
            }
        } catch (error) {
            console.error('Delete Error:', error);
            await CommonJS.alert('삭제 처리 중 오류가 발생했습니다.', '오류', 'error');
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
        if (this.state.lowStockOnly && this.state.lowStockThreshold && this.state.lowStockThreshold !== '100') {
            params.set('lowStockThreshold', this.state.lowStockThreshold);
        }
        if (this.state.createdTodayOnly) params.set('createdTodayOnly', 'true');
        if (this.state.searchKeyword) params.set('searchKeyword', this.state.searchKeyword);
        if (this.state.orderType && this.state.orderType !== 'r') params.set('orderType', this.state.orderType);

        return params.toString();
    },

    _readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        this.state.page = Number(params.get('page') || 0);
        this.state.size = Number(params.get('size') || 10);
        this.state.brandNo = params.get('brandNo') || '';
        this.state.categoryNo = params.get('categoryNo') || '';
        this.state.status = params.get('status') || '';
        this.state.lowStockOnly = params.get('lowStockOnly') === 'true';
        this.state.lowStockThreshold = this._normalizeLowStockThreshold(params.get('lowStockThreshold'));
        this.state.createdTodayOnly = params.get('createdTodayOnly') === 'true';
        this.state.searchKeyword = params.get('searchKeyword') || '';
        this.state.orderType = params.get('orderType') || 'r';
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
        return this.allowedLowStockThresholds.includes(String(value)) ? String(value) : '100';
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
        this.state.brandNo = document.getElementById('brandNo').value;
        this.state.categoryNo = document.getElementById('categoryNo').value;
        this.state.status = document.getElementById('statusFilter').value;
        this.state.lowStockOnly = document.getElementById('lowStockOnly').checked;
        this.state.lowStockThreshold = this._normalizeLowStockThreshold(document.getElementById('lowStockThreshold').value);
        this.state.createdTodayOnly = document.getElementById('createdTodayOnly').checked;
        this.state.searchKeyword = document.getElementById('searchKeyword').value.trim().replaceAll(/\s+/g, ' ');
        this.state.size = Number(document.getElementById('pageSize').value || 10);
        this.state.orderType = document.getElementById('orderType').getAttribute('data-current-value') || 'r';
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
            CommonJS.alert('검색어는 50자 이하로 입력해주세요.', '알림', 'warning');
            return false;
        }

        return true;
    },

    _syncUrlState() {
        const query = this.buildQueryString();
        const url = query ? `${window.location.pathname}?${query}` : window.location.pathname;
        history.pushState(null, '', url);
    }
};
