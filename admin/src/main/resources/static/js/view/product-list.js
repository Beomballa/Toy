const ProductList = {

    state: {
        page: 0,
        size: 10,
    },

    init(brands = [], categories = []) {
        this._fillSelect('brandNo',    brands,     'brandNo',    'nameKo');
        this._fillSelect('categoryNo', categories, 'categoryNo', 'name');

        this._bindEvents();
        this._initAnimations();
        this.getList(); // 초기 로드

        document.getElementById('new-product')?.addEventListener('click', () => location.href = '/product/set');
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
        const FILTER_IDS = ['brandNo', 'categoryNo', 'statusFilter', 'searchKeyword'];
        FILTER_IDS.forEach(id => {
            const el = document.getElementById(id);
            if (!el) return;
            el.addEventListener('change', () => { this.state.page = 0; this.getList(); });
            if (el.tagName === 'INPUT') {
                el.addEventListener('keypress', e => {
                    if (e.key === 'Enter') { e.preventDefault(); this.state.page = 0; this.getList(); }
                });
            }
        });

        document.getElementById('productListTableBody')?.addEventListener('click', e => {
            const productNameEl = e.target.closest('.product-name');
            if (productNameEl) {
                const productNo = productNameEl.dataset.id;
                location.href = `/product/get?no=${productNo}`;
            }
        });
    },

    _initAnimations() {
        document.querySelectorAll('.animate-in').forEach((el, i) => {
            el.style.opacity = '0';
            el.style.animation = `fadeInUp 0.6s ease forwards ${i * 0.1}s`;
        });
    },

    // ─────────────────────────────────────────
    // ✅ API 호출 (구조 분해 할당 적용)
    // ─────────────────────────────────────────
    async getList() {
        const params = new URLSearchParams({
            page: this.state.page,
            size: this.state.size,
            brandNo: document.getElementById('brandNo').value,
            categoryNo: document.getElementById('categoryNo').value,
            isActive: document.getElementById('statusFilter').value,
            searchKeyword: document.getElementById('searchKeyword').value,
        });

        try {
            const res = await fetch(`/api/admin/product/list?${params}`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            const data = await res.json();

            // ✅ 데이터 구조에 따라 products 내부의 정보를 전달
            this._renderList(data.products.content);
            this._renderPagination(data.products);

            // ✅ 서버가 준 진짜 통계 데이터를 전달
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
                        <img src="${item.thumbnailUrl || 'https://via.placeholder.com/200'}"
                             class="product-thumb" alt="thumb">
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
                    <span class="badge ${item.status === 'ACTIVE' ? 'badge-active' : 'bg-secondary'}">
                        ${item.status}
                    </span>
                </td>
                <td class="small text-muted">${item.crtDtm}</td>
                <td class="text-end pe-4">
                    <button class="btn btn-icon btn-secondary me-1" onclick="location.href='/product/set?no=${item.productNo}'">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-icon btn-secondary" onclick="ProductList.deleteProduct('${item.productNo}')">
                        <i class="fas fa-trash text-danger"></i>
                    </button>
                </td>
            </tr>
        `).join('');
    },

    _renderPagination(pageData) {
        const { totalPages, number: curr, totalElements, numberOfElements, size } = pageData;
        const pagination = document.getElementById('pagination');

        let html = '';
        // 간단한 페이지네이션 생성 로직
        for (let i = 0; i < totalPages; i++) {
            html += `
                <li class="page-item ${i === curr ? 'active' : ''}">
                    <a class="page-link" href="javascript:void(0);" onclick="ProductList.goPage(${i})">${i + 1}</a>
                </li>`;
        }
        pagination.innerHTML = html;

        document.getElementById('totalElementsCount').textContent = `전체 ${totalElements.toLocaleString()}개`;
        document.getElementById('pageInfoText').textContent =
            `Showing ${numberOfElements === 0 ? 0 : curr * size + 1} to ${curr * size + numberOfElements} of ${totalElements} entries`;
    },

    // ─────────────────────────────────────────
    // ✅ 통계 카드 업데이트 (서버 응답값 기반)
    // ─────────────────────────────────────────
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

    async deleteProduct(no) {
        if (!confirm('정말로 삭제하시겠습니까?')) return;
        // 삭제 로직 생략 (기존 유지)
    }
};