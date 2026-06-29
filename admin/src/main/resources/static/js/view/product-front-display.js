const ProductFrontDisplayList = {
    state: {
        keyword: '',
        status: '',
        brandNo: '',
        categoryNo: '',
        configured: '',
        contentStatus: '',
        featuredOnly: false,
        lowStockOnly: false,
        lowStockThreshold: 20,
        sort: 'FEATURED',
        source: '',
        returnTo: ''
    },
    initialLowStockThreshold: 20,

    init() {
        const thresholdInput = document.getElementById('displayLowStockThreshold');
        this.initialLowStockThreshold = Number(thresholdInput?.value || 20);
        this.state.lowStockThreshold = this.initialLowStockThreshold;
        this.readStateFromUrl();
        this.syncFilterInputs();
        this.syncReturnLinks();
        this.bindEvents();
        this.load();
    },

    bindEvents() {
        document.getElementById('btnSearchDisplay')?.addEventListener('click', () => this.search());
        document.getElementById('btnResetDisplayFilter')?.addEventListener('click', () => this.reset());
        document.getElementById('btnExportDisplay')?.addEventListener('click', () => this.exportCsv());
        document.getElementById('displaySummaryTotalCard')?.addEventListener('click', () => this.applySummaryFilter('ALL'));
        document.getElementById('displaySummaryConfiguredCard')?.addEventListener('click', () => this.applySummaryFilter('CONFIGURED'));
        document.getElementById('displaySummaryFeaturedCard')?.addEventListener('click', () => this.applySummaryFilter('FEATURED'));
        document.getElementById('displaySummaryLowStockCard')?.addEventListener('click', () => this.applySummaryFilter('LOW_STOCK'));
        document.getElementById('displayKeyword')?.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                this.search();
            }
        });
        window.addEventListener('popstate', () => {
            this.readStateFromUrl();
            this.syncFilterInputs();
            this.syncReturnLinks();
            this.load();
        });
    },

    buildParams() {
        const params = new URLSearchParams();
        if (this.state.keyword) {
            params.set('keyword', this.state.keyword);
        }
        if (this.state.status) {
            params.set('status', this.state.status);
        }
        if (this.state.brandNo) {
            params.set('brandNo', this.state.brandNo);
        }
        if (this.state.categoryNo) {
            params.set('categoryNo', this.state.categoryNo);
        }
        if (this.state.configured) {
            params.set('configured', this.state.configured);
        }
        if (this.state.contentStatus) {
            params.set('contentStatus', this.state.contentStatus);
        }
        if (this.state.featuredOnly) {
            params.set('featuredOnly', 'true');
        }
        if (this.state.lowStockOnly) {
            params.set('lowStockOnly', 'true');
        }
        params.set('lowStockThreshold', String(this.state.lowStockThreshold));
        params.set('sort', this.state.sort);
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        if (this.state.returnTo) {
            params.set('returnTo', this.state.returnTo);
        }
        return params;
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        const lowStockThreshold = Number(params.get('lowStockThreshold') || this.initialLowStockThreshold);

        this.state = {
            keyword: CommonJS.normalizeOptionalText(params.get('keyword')) || '',
            status: params.get('status') || '',
            brandNo: params.get('brandNo') || '',
            categoryNo: params.get('categoryNo') || '',
            configured: params.get('configured') || '',
            contentStatus: params.get('contentStatus') || '',
            featuredOnly: params.get('featuredOnly') === 'true',
            lowStockOnly: params.get('lowStockOnly') === 'true',
            lowStockThreshold: Number.isFinite(lowStockThreshold) && lowStockThreshold > 0
                ? lowStockThreshold
                : this.initialLowStockThreshold,
            sort: params.get('sort') || 'FEATURED',
            source: params.get('source') || '',
            returnTo: params.get('returnTo') || ''
        };
    },

    syncFilterInputs() {
        const setValue = (id, value) => {
            const target = document.getElementById(id);
            if (target) {
                target.value = value;
            }
        };

        setValue('displayKeyword', this.state.keyword);
        setValue('displayStatus', this.state.status);
        setValue('displayBrand', this.state.brandNo);
        setValue('displayCategory', this.state.categoryNo);
        setValue('displayConfigured', this.state.configured);
        setValue('displayContentStatus', this.state.contentStatus);
        setValue('displayLowStockThreshold', String(this.state.lowStockThreshold));
        setValue('displaySort', this.state.sort);

        const featuredOnly = document.getElementById('featuredOnly');
        if (featuredOnly) {
            featuredOnly.checked = this.state.featuredOnly;
        }

        const lowStockOnly = document.getElementById('lowStockOnly');
        if (lowStockOnly) {
            lowStockOnly.checked = this.state.lowStockOnly;
        }

        this.syncToggleCardState();
    },

    syncReturnLinks() {
        CommonJS.bindMainLogoNavigation(this.state.returnTo || '/admin/products/front-display');
        CommonJS.renderSourceContextNotice({ noticeId: 'productFrontDisplaySourceContextNotice', source: this.state.source });
        const backButton = document.getElementById('btnBackToProductList');
        const returnContext = CommonJS.getReturnContext(this.state.returnTo || '/admin/products', '상품 관리');
        if (backButton) {
            backButton.href = this.state.returnTo || '/admin/products';
            backButton.innerHTML = `<i class="fas fa-arrow-left me-2"></i>${returnContext.buttonLabel}`;
        }
    },

    syncUrlState() {
        const params = this.buildParams();
        const queryString = params.toString();
        const nextUrl = queryString ? `${window.location.pathname}?${queryString}` : window.location.pathname;
        window.history.pushState({}, '', nextUrl);
    },

    getReturnTo() {
        const params = this.buildParams();
        const queryString = params.toString();
        return queryString ? `${window.location.pathname}?${queryString}` : window.location.pathname;
    },

    async load() {
        const params = this.buildParams();
        try {
            this.renderLoading();
            this.renderMeta({
                resultMeta: null,
                summary: {
                    totalCount: 0
                }
            });
            const response = await fetch(`/api/admin/product/front-display/list?${params.toString()}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '프론트 노출 목록을 불러오지 못했습니다.'));
            }
            const payload = await response.json();
            this.render(payload);
        } catch (error) {
            console.error('Front display list load failed:', error);
            this.renderError(error.message || '프론트 노출 목록을 불러오지 못했습니다.');
        }
    },

    search() {
        this.state.keyword = CommonJS.normalizeOptionalText(document.getElementById('displayKeyword')?.value) || '';
        this.state.status = document.getElementById('displayStatus')?.value || '';
        this.state.brandNo = document.getElementById('displayBrand')?.value || '';
        this.state.categoryNo = document.getElementById('displayCategory')?.value || '';
        this.state.configured = document.getElementById('displayConfigured')?.value || '';
        this.state.contentStatus = document.getElementById('displayContentStatus')?.value || '';
        this.state.featuredOnly = document.getElementById('featuredOnly')?.checked || false;
        this.state.lowStockOnly = document.getElementById('lowStockOnly')?.checked || false;
        this.state.lowStockThreshold = this.normalizeLowStockThreshold(document.getElementById('displayLowStockThreshold')?.value);
        this.state.sort = document.getElementById('displaySort')?.value || 'FEATURED';
        this.syncFilterInputs();
        this.syncUrlState();
        this.load();
    },

    async exportCsv() {
        const button = document.getElementById('btnExportDisplay');
        const params = this.buildParams();
        try {
            CommonJS.setButtonDisabled(button, true, '내보내는 중입니다.');
            await CommonJS.downloadFile(`/api/admin/product/front-display/export?${params.toString()}`, 'front-display-products.csv');
        } catch (error) {
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            CommonJS.setButtonDisabled(button, false);
        }
    },

    reset() {
        this.state = {
            keyword: '',
            status: '',
            brandNo: '',
            categoryNo: '',
            configured: '',
            contentStatus: '',
            featuredOnly: false,
            lowStockOnly: false,
            lowStockThreshold: this.initialLowStockThreshold,
            sort: 'FEATURED',
            source: this.state.source,
            returnTo: this.state.returnTo
        };
        this.syncFilterInputs();
        this.syncUrlState();
        this.load();
    },

    render(payload) {
        const tbody = document.getElementById('frontDisplayTableBody');
        const resultCount = document.getElementById('displayResultCount');
        const filterSummary = document.getElementById('displayFilterSummary');
        if (!tbody || !resultCount || !filterSummary) {
            return;
        }

        const items = Array.isArray(payload?.items) ? payload.items : [];
        const summary = payload?.summary || {
            totalCount: items.length,
            configuredCount: 0,
            unconfiguredCount: 0,
            featuredCount: 0,
            lowStockCount: 0,
            lowStockThreshold: this.state.lowStockThreshold
        };
        const resultMeta = payload?.resultMeta || null;

        resultCount.textContent = resultMeta?.resultLabel || `전체 ${summary.totalCount}건`;
        filterSummary.textContent = resultMeta?.querySignature || this.buildSummary();
        this.renderSummary(summary);
        this.renderMeta(payload);
        this.syncSummaryCardState();

        if (!items.length) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center py-5">
                        <div class="product-empty-state">
                            <i class="fas fa-layer-group product-empty-state-icon"></i>
                            <strong>조건에 맞는 전시 상품이 없습니다.</strong>
                            <p>노출 설정, 전시 문구, Featured, 저재고 조건을 조정해서 다른 전시 후보를 확인하세요.</p>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        const returnTo = encodeURIComponent(this.getReturnTo());
        tbody.innerHTML = items.map((item) => `
            <tr>
                <td class="ps-4">
                    <div class="fw-bold">${this.escapeHtml(item.productName || '-')}</div>
                    <div class="small text-muted">#${item.productNo} · ${this.escapeHtml(item.headline || '미설정')}</div>
                </td>
                <td>
                    <div>${this.escapeHtml(item.brandName || '-')}</div>
                    <div class="small text-muted">${this.escapeHtml(item.categoryName || '-')}</div>
                </td>
                <td>${Number(item.totalStock || 0).toLocaleString()}개</td>
                <td>${this.escapeHtml(item.statusDescription || item.status || '-')}</td>
                <td>
                    <div>${item.displayConfigured ? '설정됨' : '미설정'}</div>
                    <div class="small text-muted">${item.contentReady ? '전시 문구 완성' : '보완 필요'}</div>
                </td>
                <td>${item.featured ? `Y / ${item.featuredRank}` : 'N'}</td>
                <td class="text-end pe-4">
                    <div class="btn-group btn-group-sm">
                        <a class="btn btn-outline-secondary" href="/admin/products/get?no=${item.productNo}&source=product-front-display&returnTo=${returnTo}">상세</a>
                        <a class="btn btn-outline-primary" href="/admin/products/update?no=${item.productNo}&source=product-front-display&returnTo=${returnTo}">수정</a>
                    </div>
                </td>
            </tr>
        `).join('');
    },

    renderError(message) {
        const tbody = document.getElementById('frontDisplayTableBody');
        if (!tbody) {
            return;
        }
        this.renderSummary({
            totalCount: 0,
            configuredCount: 0,
            unconfiguredCount: 0,
            featuredCount: 0,
            lowStockCount: 0,
            lowStockThreshold: this.state.lowStockThreshold
        });
        this.renderMeta({ errorMessage: message, resultMeta: null, summary: { totalCount: 0 } });
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-5 text-danger">${this.escapeHtml(message)}</td></tr>`;
    },

    renderLoading() {
        const tbody = document.getElementById('frontDisplayTableBody');
        if (!tbody) {
            return;
        }
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center py-5">
                    <div class="product-loading-state">
                        <i class="fas fa-spinner fa-spin product-empty-state-icon"></i>
                        <strong>전시 상품을 불러오는 중입니다.</strong>
                        <p>현재 필터 기준으로 상품 노출 설정과 요약 지표를 함께 계산하고 있습니다.</p>
                    </div>
                </td>
            </tr>
        `;
    },

    renderSummary(summary) {
        const setText = (id, value) => {
            const target = document.getElementById(id);
            if (target) {
                target.textContent = value;
            }
        };
        setText('displaySummaryTotal', Number(summary.totalCount || 0).toLocaleString());
        setText('displaySummaryConfigured', Number(summary.configuredCount || 0).toLocaleString());
        setText('displaySummaryUnconfigured', `미설정 ${Number(summary.unconfiguredCount || 0).toLocaleString()}건`);
        setText(
            'displaySummaryContentQuality',
            `전시 문구 완성 ${Number(summary.readyContentCount || 0).toLocaleString()}건 · 보완 ${Number(summary.incompleteContentCount || 0).toLocaleString()}건`
        );
        setText('displaySummaryFeatured', Number(summary.featuredCount || 0).toLocaleString());
        setText('displaySummaryLowStock', Number(summary.lowStockCount || 0).toLocaleString());
        setText('displaySummaryThreshold', `기준 ${Number(summary.lowStockThreshold || this.state.lowStockThreshold).toLocaleString()}개 미만`);
    },

    renderMeta(payload = {}) {
        const resultMeta = payload?.resultMeta || null;
        const totalCount = Number(payload?.summary?.totalCount || 0);
        const resultLabel = payload?.errorMessage
            ? payload.errorMessage
            : (resultMeta?.resultLabel || `전체 ${totalCount.toLocaleString()}건`);
        const querySignature = resultMeta?.querySignature || this.buildSummary();
        const pageInfoLabel = resultMeta?.pageInfoLabel || '전시 대상은 단일 목록으로 조회합니다.';
        const filterCount = Number(resultMeta?.filterCount || this.countActiveFilters());

        CommonJS.renderListMeta({
            metaTextId: 'displayFilterSummary',
            filterMetaId: 'displayFilterMeta',
            resultMetaId: 'displayResultMeta',
            pageMetaId: 'displayPageMeta',
            resultLabel,
            filterCount,
            querySignature,
            pageInfoLabel,
            filterPrefix: '필터',
            defaultResultText: '결과 메타 없음',
            defaultPageText: '페이지 메타 없음'
        });
    },

    buildSummary() {
        const tokens = [];
        if (this.state.featuredOnly) {
            tokens.push('Featured만');
        }
        if (this.state.status) {
            const statusLabel = document.getElementById('displayStatus')?.selectedOptions?.[0]?.textContent?.trim();
            tokens.push(`상태 ${statusLabel || this.state.status}`);
        }
        const brandLabel = document.getElementById('displayBrand')?.selectedOptions?.[0]?.textContent?.trim();
        if (this.state.brandNo && brandLabel) {
            tokens.push(`브랜드 ${brandLabel}`);
        }
        const categoryLabel = document.getElementById('displayCategory')?.selectedOptions?.[0]?.textContent?.trim();
        if (this.state.categoryNo && categoryLabel) {
            tokens.push(`카테고리 ${categoryLabel}`);
        }
        if (this.state.configured === 'CONFIGURED') {
            tokens.push('설정됨만');
        }
        if (this.state.configured === 'UNCONFIGURED') {
            tokens.push('미설정만');
        }
        if (this.state.contentStatus === 'READY') {
            tokens.push('전시 문구 완성');
        }
        if (this.state.contentStatus === 'INCOMPLETE') {
            tokens.push('전시 문구 보완 필요');
        }
        if (this.state.keyword) {
            tokens.push(`검색 ${this.state.keyword}`);
        }
        if (this.state.lowStockOnly) {
            tokens.push(`저재고 ${this.state.lowStockThreshold}개 미만`);
        }
        tokens.push(this.sortLabel(this.state.sort));
        return tokens.length ? tokens.join(' · ') : '전체 상품 기준';
    },

    countActiveFilters() {
        let count = 0;
        if (this.state.keyword) count += 1;
        if (this.state.status) count += 1;
        if (this.state.brandNo) count += 1;
        if (this.state.categoryNo) count += 1;
        if (this.state.configured) count += 1;
        if (this.state.contentStatus) count += 1;
        if (this.state.featuredOnly) count += 1;
        if (this.state.lowStockOnly) count += 1;
        if (this.state.lowStockThreshold !== this.initialLowStockThreshold) count += 1;
        if (this.state.sort && this.state.sort !== 'FEATURED') count += 1;
        return count;
    },

    normalizeLowStockThreshold(rawValue) {
        const parsed = Number(rawValue || this.initialLowStockThreshold);
        return Number.isFinite(parsed) && parsed > 0 ? parsed : this.initialLowStockThreshold;
    },

    applySummaryFilter(type) {
        if (type === 'ALL') {
            this.state.configured = '';
            this.state.featuredOnly = false;
            this.state.lowStockOnly = false;
        } else if (type === 'CONFIGURED') {
            this.state.configured = this.state.configured === 'CONFIGURED' ? '' : 'CONFIGURED';
        } else if (type === 'FEATURED') {
            this.state.featuredOnly = !this.state.featuredOnly;
        } else if (type === 'LOW_STOCK') {
            this.state.lowStockOnly = !this.state.lowStockOnly;
        }

        this.syncFilterInputs();
        this.syncUrlState();
        this.load();
    },

    syncSummaryCardState() {
        const totalCard = document.getElementById('displaySummaryTotalCard');
        const configuredCard = document.getElementById('displaySummaryConfiguredCard');
        const featuredCard = document.getElementById('displaySummaryFeaturedCard');
        const lowStockCard = document.getElementById('displaySummaryLowStockCard');
        const hasFocusedSummary = this.state.configured === 'CONFIGURED' || this.state.featuredOnly || this.state.lowStockOnly;

        totalCard?.classList.toggle('stat-card-active', !hasFocusedSummary);
        configuredCard?.classList.toggle('stat-card-active', this.state.configured === 'CONFIGURED');
        featuredCard?.classList.toggle('stat-card-active', this.state.featuredOnly);
        lowStockCard?.classList.toggle('stat-card-active', this.state.lowStockOnly);
    },

    syncToggleCardState() {
        const featuredOnly = document.getElementById('featuredOnly');
        featuredOnly?.closest('.product-front-display-toggle-item')?.classList.toggle('is-active', !!featuredOnly?.checked);

        const lowStockOnly = document.getElementById('lowStockOnly');
        lowStockOnly?.closest('.product-front-display-toggle-item')?.classList.toggle('is-active', !!lowStockOnly?.checked);
    },

    sortLabel(sort) {
        switch (sort) {
            case 'LATEST':
                return '최신 등록순';
            case 'STOCK_ASC':
                return '재고 낮은 순';
            case 'STOCK_DESC':
                return '재고 높은 순';
            case 'PRICE_HIGH':
                return '발매가 높은 순';
            case 'PRICE_LOW':
                return '발매가 낮은 순';
            default:
                return 'Featured 우선';
        }
    },

    escapeHtml(value) {
        return String(value)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }
};

document.addEventListener('DOMContentLoaded', () => ProductFrontDisplayList.init());
