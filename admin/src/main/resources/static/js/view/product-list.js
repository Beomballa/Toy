const ProductList = {

    state: {
        page: 0,
        size: 10,
        brandNo: '',
        categoryNo: '',
        status: '',
        searchKeyword: '',
        orderType: 'r',
    },

    init(brands = [], categories = []) {
        this._fillSelect('brandNo',    brands,     'brandNo',    'nameKo');
        this._fillSelect('categoryNo', categories, 'categoryNo', 'name');

        this._readStateFromUrl();
        this._syncFilterInputs();
        this._bindEvents();
        this._initAnimations();
        this.getList(); // 초기 로드

        document.getElementById('new-product')?.addEventListener('click', () => location.href = `/admin/products/set?returnTo=${encodeURIComponent(this.getReturnTo())}`);
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
        const FILTER_IDS = ['brandNo', 'categoryNo', 'statusFilter', 'searchKeyword', 'pageSize', 'orderType'];
        FILTER_IDS.forEach(id => {
            const el = document.getElementById(id);
            if (!el) return;
            el.addEventListener('change', () => { this.state.page = 0; this._updateStateFromInputs(); this.getList(); });
            if (el.tagName === 'INPUT') {
                el.addEventListener('keypress', e => {
                    if (e.key === 'Enter') { e.preventDefault(); this.state.page = 0; this._updateStateFromInputs(); this.getList(); }
                });
            }
        });

        document.getElementById('productListTableBody')?.addEventListener('click', e => {
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
        window.addEventListener('popstate', () => {
            this._readStateFromUrl();
            this._syncFilterInputs();
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
        if (pushState) {
            this._syncUrlState();
        }

        const params = new URLSearchParams({
            page: this.state.page,
            size: this.state.size,
            brandNo: this.state.brandNo,
            categoryNo: this.state.categoryNo,
            status: this.state.status,
            searchKeyword: this.state.searchKeyword,
            orderType: this.state.orderType,
        });

        try {
            const res = await fetch(`/api/admin/product/list?${params}`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            const data = await res.json();

            this._renderList(data.products);
            this._renderPagination(data);
            this._updateStats(data.productStats);

        } catch (err) {
            console.error('상품 목록 로드 실패:', err);
            this._showError();
        }
    },

    _renderList(items) {
        const tbody = document.getElementById('productListTableBody');
        if (!items?.length) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center py-5 text-muted">조회된 상품이 없습니다.</td></tr>';
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
        const { totalPages, currentPage: curr, totalElements } = data;
        const pagination = document.getElementById('pagination');
        let html = '';
        for (let i = 0; i < totalPages; i++) {
            html += `
            <li class="page-item ${i === curr ? 'active' : ''}">
                <a class="page-link" href="javascript:void(0);" onclick="ProductList.goPage(${i})">${i + 1}</a>
            </li>`;
        }
        pagination.innerHTML = html;
        document.getElementById('totalElementsCount').textContent = `전체 ${totalElements.toLocaleString()}개`;
        document.getElementById('pageInfoText').textContent = `전체 ${totalElements.toLocaleString()}개 중 ${totalPages}페이지`;
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
    },

    _showError() {
        document.getElementById('productListTableBody').innerHTML = `
            <tr><td colspan="8" class="text-center py-5 text-danger">데이터 로드 중 오류가 발생했습니다.</td></tr>`;
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
            searchKeyword: '',
            orderType: 'r',
        };
        this._syncFilterInputs();
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
        this.state.searchKeyword = params.get('searchKeyword') || '';
        this.state.orderType = params.get('orderType') || 'r';
    },

    _syncFilterInputs() {
        document.getElementById('brandNo').value = this.state.brandNo;
        document.getElementById('categoryNo').value = this.state.categoryNo;
        document.getElementById('statusFilter').value = this.state.status;
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

    _updateStateFromInputs() {
        this.state.brandNo = document.getElementById('brandNo').value;
        this.state.categoryNo = document.getElementById('categoryNo').value;
        this.state.status = document.getElementById('statusFilter').value;
        this.state.searchKeyword = document.getElementById('searchKeyword').value.trim().replaceAll(/\s+/g, ' ');
        this.state.size = Number(document.getElementById('pageSize').value || 10);
        this.state.orderType = document.getElementById('orderType').getAttribute('data-current-value') || 'r';
    },

    _syncUrlState() {
        const query = this.buildQueryString();
        const url = query ? `${window.location.pathname}?${query}` : window.location.pathname;
        history.pushState(null, '', url);
    }
};
