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
        const btnNewProduct = document.getElementById('new-product');
        if (btnNewProduct) {
            btnNewProduct.addEventListener('click', function () {
                window.location.href = '/product/set';
            });
        }

        document.getElementById("main-logo").addEventListener("click", () => {
            window.location.href = "/admin/products";
        });
    },

    bindEvents() {
        // 1. 필터 변경 시 리스트 재조회
        const filters = document.querySelectorAll('.filter-form .form-select, .filter-form .form-control');
        filters.forEach(filter => {
            filter.addEventListener('change', () => {
                this.state.page = 0; // 필터 변경 시 1페이지부터
                this.getList();
            });

            // 검색어 입력창에서 엔터 눌렀을 때도 검색되도록
            if(filter.id === 'searchKeyword') {
                filter.addEventListener('keypress', (e) => {
                    if (e.key === 'Enter') {
                        e.preventDefault();
                        this.state.page = 0;
                        this.getList();
                    }
                });
            }
        });

        // 2. ✅ 상품명 클릭 이벤트 (이벤트 위임 방식)
        const tbody = document.getElementById('productListTableBody');
        if (tbody) {
            tbody.addEventListener('click', (e) => {
                const target = e.target;
                // 클릭된 요소가 .product-name 클래스를 가졌는지 확인
                if (target.classList.contains('product-name')) {
                    const productNo = target.getAttribute('data-id');
                    if (productNo) {
                        // 컨트롤러 규격에 맞게 이동 (?no=123)
                        window.location.href = `/product/get?no=${productNo}`;
                    }
                }
            });
        }

        // 3. 애니메이션 효과
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
            isActive: status || '', // DTO 필드명에 맞춤
            searchKeyword: searchKeyword || ''
        });

        try {
            const response = await fetch(`/api/admin/product/list?${params.toString()}`);

            if (response.ok) {
                const data = await response.json(); // Page<ProductListItem> 객체
                this.renderList(data.content);      // 테이블 그리기
                this.renderPagination(data);       // 페이지네이션 그리기
            } else {
                console.error('데이터 로드 실패');
            }
        } catch (error) {
            console.error('네트워크 에러:', error);
        }
    },

    // 테이블 렌더링 로직
    renderList(items) {
        const tbody = document.getElementById('productListTableBody');

        if (!items || items.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center py-5">등록된 상품이 없습니다.</td></tr>';
            document.getElementById('totalElementsCount').textContent = '전체 0개';
            return;
        }

        // 전체 개수 업데이트
        // (API 응답 데이터 구조에 따라 getList에서 직접 업데이트해도 됨)

        tbody.innerHTML = items.map(item => `
            <tr>
                <td>
                    <div class="product-info">
                        <img src="${item.thumbnailUrl || 'https://via.placeholder.com/200'}" 
                             class="product-thumb" alt="${item.productName}">
                        <div class="product-details">
                            <div class="product-name decoration" data-id="${item.productNo}" style="cursor:pointer;">
                                ${item.productName}
                            </div>
                            <div class="product-subtitle">${item.productModel || '-'}</div>
                        </div>
                    </div>
                </td>
                <td><span class="badge badge-model">${item.productModel || 'N/A'}</span></td>
                <td><strong>${item.brandName}</strong></td>
                <td><strong>${item.releasePrice}</strong></td>
                <td>${(item.totalStock || 0).toLocaleString()}개</td>
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
    },

    // 페이지네이션 렌더링 (간단 버전)
    renderPagination(data) {
        const pagination = document.getElementById('pagination');
        if (!pagination) return;

        let html = '';
        const totalPages = data.totalPages;
        const currPage = data.number;

        for (let i = 0; i < totalPages; i++) {
            html += `
                <li class="page-item ${i === currPage ? 'active' : ''}">
                    <a class="page-link" href="javascript:void(0);" onclick="ProductList.getList(${i})">${i + 1}</a>
                </li>
            `;
        }
        pagination.innerHTML = html;

        // 하단 정보 텍스트 업데이트
        document.getElementById('totalElementsCount').textContent = `전체 ${data.totalElements.toLocaleString()}개`;
        document.getElementById('pageInfoText').textContent =
            `Showing ${data.numberOfElements === 0 ? 0 : (data.number * data.size) + 1} to ${(data.number * data.size) + data.numberOfElements} of ${data.totalElements} entries`;
    },

    deleteProduct(no) {
        if (confirm('정말로 이 상품을 삭제하시겠습니까?')) {
            console.log('삭제 요청:', no);
        }
    }
};