const ProductList = {
    // 현재 페이지 상태 관리
    state: {
        page: 0,
        size: 10
    },

    init() {
        this.bindEvents();
        this.getList(); // ✅ 초기 로드 시 리스트 호출

        // 새 상품 등록 버튼
        document.getElementById('new-product').addEventListener('click', function () {
            window.location.href = '/product/set';
        });
    },

    bindEvents() {
        // 필터 변경 시 리스트 재조회
        const filters = document.querySelectorAll('.filter-form .form-select, .filter-form .form-control');
        filters.forEach(filter => {
            filter.addEventListener('change', () => {
                this.state.page = 0; // 필터 변경 시 1페이지부터
                this.getList();
            });
        });

        // 애니메이션 효과
        const animateElements = document.querySelectorAll('.animate-in');
        animateElements.forEach((el, index) => {
            setTimeout(() => {
                el.style.opacity = '0';
                el.style.animation = `fadeInUp 0.6s ease forwards ${index * 0.1}s`;
            }, 100);
        });
    },

    // API 호출 및 데이터 획득
    async getList(page = 0) {
        this.state.page = page;

        // 1. 검색 조건 수집 (HTML의 ID 기반)
        const brandNo = document.getElementById('brandFilter').value;
        const categoryNo = document.getElementById('categoryFilter').value;
        const status = document.getElementById('statusFilter').value;
        const searchKeyword = document.getElementById('searchKeyword').value;

        // 2. Query String 생성 (GET 요청용)
        const params = new URLSearchParams({
            page: this.state.page,
            size: this.state.size,
            brandNo: brandNo || '',
            categoryNo: categoryNo || '',
            status: status || '',
            searchKeyword: searchKeyword || ''
        });

        try {
            const response = await fetch(`/api/admin/product/list?${params.toString()}`);

            if (response.ok) {
                const data = await response.json(); // Page<ProductListItem> 객체
                this.renderList(data.content);      // 테이블 그리기
                // this.renderPagination(data);    // (선택) 페이지네이션 그리기
            } else {
                console.error('데이터 로드 실패');
            }
        } catch (error) {
            console.error('네트워크 에러:', error);
        }
    },

    // 테이블 렌더링 로직
    renderList(items) {
        const tbody = document.querySelector('.table tbody');

        if (!items || items.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center py-5">등록된 상품이 없습니다.</td></tr>';
            return;
        }

        tbody.innerHTML = items.map(item => `
            <tr>
                <td>
                    <div class="product-info">
                        <img src="${item.thumbnailUrl || 'https://via.placeholder.com/200'}" 
                             class="product-thumb" alt="${item.productName}">
                        <div class="product-details">
                            <div class="product-name decoration">${item.productName}</div>
                            <div class="product-subtitle">${item.productModel || '-'}</div>
                        </div>
                    </div>
                </td>
                <td><span class="badge badge-model">${item.productModel || 'N/A'}</span></td>
                <td><strong>${item.brandName}</strong></td>
                <td><strong>${item.releasePrice}</strong></td>
                <td>${item.totalStock.toLocaleString()}개</td>
                <td><span class="badge ${item.status === 'ACTIVE' ? 'badge-active' : 'bg-secondary'}">${item.status}</span></td>
                <td>${item.crtDtm}</td>
                <td class="text-end">
                    <button class="btn btn-icon btn-secondary me-1" onclick="location.href='/admin/products/edit/${item.productNo}'">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-icon btn-secondary" onclick="ProductList.deleteProduct('${item.productNo}')">
                        <i class="fas fa-trash text-danger"></i>
                    </button>
                </td>
            </tr>
        `).join('');

        document.querySelectorAll('.product-name').forEach((i) => {
            i.addEventListener('click', () => {
                window.location.href = '/product/set'
            })
        })
    }
};