const ProductList = {

    state: {
        page: 0,
        size: 10,
    },

    // ─────────────────────────────────────────
    // 초기화
    // ─────────────────────────────────────────
    init(brands = [], categories = []) {
        // ✅ 셀렉트 박스 렌더링 (공통 헬퍼로 통합)
        this._fillSelect('brandNo',    brands,     'brandNo',    'nameKo');
        this._fillSelect('categoryNo', categories, 'categoryNo', 'name');

        this._bindEvents();
        this._initAnimations();
        this.getList();

        document.getElementById('new-product')
            ?.addEventListener('click', () => location.href = '/product/set');

        document.getElementById('main-logo')
            ?.addEventListener('click', () => location.href = '/admin/products');
    },

    // ─────────────────────────────────────────
    // 공통 셀렉트 박스 채우기
    // ✅ renderSelects() 내 중복 로직을 하나로 통합
    // ─────────────────────────────────────────
    _fillSelect(selectId, items, valueKey, labelKey) {
        const select = document.getElementById(selectId);
        if (!select || !items?.length) return;

        const fragment = document.createDocumentFragment();
        items.forEach(item => {
            const opt = document.createElement('option');
            opt.value = item[valueKey];
            opt.textContent = item[labelKey];
            // 카테고리의 경우 depth 등 추가 속성 보존
            if (item.depth    != null) opt.dataset.depth  = item.depth;
            if (item.parentNo != null) opt.dataset.parent = item.parentNo;
            fragment.appendChild(opt);
        });
        select.appendChild(fragment);
    },

    // ─────────────────────────────────────────
    // 이벤트 바인딩
    // ✅ 필터 change + 엔터 검색을 하나의 루프로 처리
    // ─────────────────────────────────────────
    _bindEvents() {
        const FILTER_IDS = ['brandNo', 'categoryNo', 'statusFilter', 'searchKeyword'];

        FILTER_IDS.forEach(id => {
            const el = document.getElementById(id);
            if (!el) return;

            el.addEventListener('change', () => { this.state.page = 0; this.getList(); });

            // 텍스트 입력창: Enter 키 검색
            if (el.tagName === 'INPUT') {
                el.addEventListener('keypress', e => {
                    if (e.key === 'Enter') { e.preventDefault(); this.state.page = 0; this.getList(); }
                });
            }
        });

        // ✅ 상품명 클릭 이벤트 위임 (tbody 한 번만 등록)
        document.getElementById('productListTableBody')
            ?.addEventListener('click', e => {
                const productNo = e.target.closest('.product-name')?.dataset.id;
                if (productNo) location.href = `/product/get?no=${productNo}`;
            });
    },

    // ─────────────────────────────────────────
    // 진입 애니메이션 (init에서 분리)
    // ─────────────────────────────────────────
    _initAnimations() {
        document.querySelectorAll('.animate-in').forEach((el, i) => {
            el.style.opacity = '0';
            el.style.animation = `fadeInUp 0.6s ease forwards ${i * 0.1}s`;
        });
    },

    // ─────────────────────────────────────────
    // API 호출
    // ✅ page 파라미터 제거 → state.page 일원화
    // ─────────────────────────────────────────
    async getList() {
        const params = new URLSearchParams({
            page:          this.state.page,
            size:          this.state.size,
            brandNo:       document.getElementById('brandNo').value,
            categoryNo:    document.getElementById('categoryNo').value,
            isActive:      document.getElementById('statusFilter').value,
            searchKeyword: document.getElementById('searchKeyword').value,
        });

        try {
            const res = await fetch(`/api/admin/product/list?${params}`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            const data = await res.json();
            this._renderList(data.content);
            this._renderPagination(data);
            this._updateStats(data);          // ✅ stat 카드 업데이트 추가

        } catch (err) {
            console.error('상품 목록 로드 실패:', err);
            this._showError();
        }
    },

    // ─────────────────────────────────────────
    // 테이블 렌더링
    // ─────────────────────────────────────────
    _renderList(items) {
        const tbody = document.getElementById('productListTableBody');
        if (!items?.length) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center py-5">등록된 상품이 없습니다.</td></tr>';
            return;
        }

        tbody.innerHTML = items.map(item => `
            <tr>
                <td class="ps-4">
                    <div class="product-info">
                        <img src="${item.thumbnailUrl || 'https://via.placeholder.com/200'}"
                             class="product-thumb" alt="${item.productName}">
                        <div class="product-details">
                            <div class="product-name decoration"
                                 data-id="${item.productNo}" style="cursor:pointer;">
                                ${item.productName}
                            </div>
                            <div class="product-subtitle">${item.productModel || '-'}</div>
                        </div>
                    </div>
                </td>
                <td><span class="badge badge-model">${item.productModel || 'N/A'}</span></td>
                <td><strong>${item.brandName}</strong></td>
                <td><strong>${item.releasePrice?.toLocaleString() ?? '-'}</strong></td>
                <td>${(item.totalStock ?? 0).toLocaleString()}개</td>
                <td>
                    <span class="badge ${item.status === 'ACTIVE' ? 'badge-active' : 'bg-secondary'}">
                        ${item.status}
                    </span>
                </td>
                <td>${item.crtDtm}</td>
                <td class="text-end pe-4">
                    <button class="btn btn-icon btn-secondary me-1"
                            onclick="location.href='/admin/products/edit/${item.productNo}'">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-icon btn-secondary"
                            onclick="ProductList.deleteProduct('${item.productNo}')">
                        <i class="fas fa-trash text-danger"></i>
                    </button>
                </td>
            </tr>
        `).join('');
    },

    // ─────────────────────────────────────────
    // 페이지네이션 렌더링
    // ─────────────────────────────────────────
    _renderPagination(data) {
        const { totalPages, number: curr, totalElements, numberOfElements, size } = data;

        const html = Array.from({ length: totalPages }, (_, i) => `
            <li class="page-item ${i === curr ? 'active' : ''}">
                <a class="page-link" href="javascript:void(0);"
                   onclick="ProductList.goPage(${i})">${i + 1}</a>
            </li>
        `).join('');

        document.getElementById('pagination').innerHTML = html;
        document.getElementById('totalElementsCount').textContent = `전체 ${totalElements.toLocaleString()}개`;
        document.getElementById('pageInfoText').textContent =
            `Showing ${numberOfElements === 0 ? 0 : curr * size + 1} to ${curr * size + numberOfElements} of ${totalElements} entries`;
    },

    // ─────────────────────────────────────────
    // Stat 카드 업데이트
    // ✅ 기존 코드에서 누락된 기능 추가
    //    (API 응답에 stats 필드가 있을 경우 반영)
    // ─────────────────────────────────────────
    _updateStats(data) {
        const { stats } = data;
        if (!stats) return;

        const map = {
            'stat-total-count':  stats.totalCount,
            'stat-active-count': stats.activeCount,
            'stat-low-stock':    stats.lowStockCount,
            'stat-today-count':  stats.todayCount,
        };
        Object.entries(map).forEach(([id, val]) => {
            const el = document.getElementById(id);
            if (el && val != null) el.textContent = val.toLocaleString();
        });
    },

    // ─────────────────────────────────────────
    // 에러 표시
    // ─────────────────────────────────────────
    _showError() {
        const tbody = document.getElementById('productListTableBody');
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="text-center py-5 text-danger">
                    <i class="fas fa-exclamation-circle me-2"></i>
                    데이터를 불러오는 데 실패했습니다. 잠시 후 다시 시도해주세요.
                </td>
            </tr>`;
    },

    // ─────────────────────────────────────────
    // 페이지 이동 (외부에서 호출)
    // ✅ getList(page) 파라미터 방식 → goPage()로 명확화
    // ─────────────────────────────────────────
    goPage(page) {
        this.state.page = page;
        this.getList();
    },

    // ─────────────────────────────────────────
    // 삭제
    // ─────────────────────────────────────────
    async deleteProduct(no) {
        if (!confirm('정말로 이 상품을 삭제하시겠습니까?')) return;

        try {
            const res = await fetch(`/api/admin/product/delete/${no}`, { method: 'PATCH' });
            if (!res.ok) throw new Error();
            alert('삭제되었습니다.');
            location.href = '/admin/products';
        } catch {
            alert('삭제에 실패했습니다.');
        }
    },
};