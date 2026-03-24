const ProductDetail = {
    productNo: null,

    init() {
        const urlParams = new URLSearchParams(window.location.search);
        this.productNo = urlParams.get('no');

        if (!this.productNo) {
            alert('상품 번호가 올바르지 않습니다.');
            window.location.href = '/admin/products';
            return;
        }

        this.loadProductDetail();
        this.bindEvents();
    },

    bindEvents() {
        const btnEdit = document.getElementById('btnEdit');
        if (btnEdit) {
            btnEdit.addEventListener('click', () => {
                // 수정 페이지 이동
                window.location.href = `/admin/product/set?no=${this.productNo}`;
            });
        }

        const btnDelete = document.getElementById('btnDelete');
        if (btnDelete) {
            btnDelete.addEventListener('click', () => {
                this.deleteProduct();
            });
        }
    },

    async loadProductDetail() {
        try {
            // 함장님이 만든 컨트롤러 API 호출
            const response = await fetch(`/api/admin/product/get?no=${this.productNo}`);

            if (!response.ok) {
                throw new Error('상품 정보를 가져오는데 실패했습니다.');
            }

            const data = await response.json();
            this.renderProduct(data);

        } catch (error) {
            console.error('Error:', error);
            alert('데이터를 불러오는 중 오류가 발생했습니다.');
        }
    },

    renderProduct(data) {
        // 1. 헬퍼 함수 정의
        const setText = (id, value) => {
            const el = document.getElementById(id);
            if (el) el.textContent = value || '-';
        };

        // 2. 기본 정보 렌더링 (DTO 필드명 일치 확인)
        setText('productTitle', data.productName);
        setText('productNo', data.productNo);
        setText('categoryName', data.categoryName);
        setText('brandName', data.brandName);
        setText('nameKo', data.productName);
        setText('modelNum', data.productModel);
        setText('releaseDt', data.releaseDt);
        setText('crtDtm', data.crtDtm);
        setText('uptDtm', data.uptDtm);
        setText('statusTextValue', data.status || 'ACTIVE'); // 기본값 설정

        // 3. 가격 포맷
        const releasePriceEl = document.getElementById('releasePrice');
        if (releasePriceEl) releasePriceEl.textContent = this.formatPrice(data.releasePrice);

        // 4. 이미지 처리
        const productImage = document.getElementById('productImage');
        const thumbnailUrlLink = document.getElementById('thumbnailUrlLink');
        if (data.thumbnailUrl) {
            if (productImage) productImage.src = data.thumbnailUrl;
            if (thumbnailUrlLink) {
                thumbnailUrlLink.href = data.thumbnailUrl;
                thumbnailUrlLink.style.display = 'inline-block';
            }
        }

        // 5. 옵션 리스트 및 전체 재고 계산 ⭐
        const optionList = document.getElementById('optionList');
        const optionCount = document.getElementById('optionCount');
        const totalStockValueEl = document.getElementById('totalStockValue');

        if (data.options && data.options.length > 0) {
            if (optionCount) optionCount.textContent = data.options.length;

            // 옵션 리스트 HTML 생성 (OptionInfo 구조 반영)
            const optHtml = data.options.map(opt => `
                <div class="d-inline-block border rounded p-2 me-2 mb-2 bg-light">
                    <span class="fw-bold text-dark">${opt.optionName}</span>
                    <hr class="my-1">
                    <span class="text-primary small">재고: ${opt.stockQty.toLocaleString()}개</span>
                </div>
            `).join('');

            if (optionList) optionList.innerHTML = optHtml;

            // 전체 재고 합산 계산
            const totalStock = data.options.reduce((acc, cur) => acc + (cur.stockQty || 0), 0);
            if (totalStockValueEl) totalStockValueEl.textContent = totalStock.toLocaleString() + ' 개';

        } else {
            if (optionList) optionList.innerHTML = '<p class="text-muted small">등록된 옵션 정보가 없습니다.</p>';
            if (totalStockValueEl) totalStockValueEl.textContent = '0 개';
        }

        // 6. 상태 배지
        const statusBadge = document.getElementById('statusBadge');
        if (statusBadge) {
            const status = data.status || 'ACTIVE';
            const statusClass = status === 'ACTIVE' ? 'bg-success' : 'bg-secondary';
            statusBadge.innerHTML = `<span class="badge ${statusClass} fs-6">${status}</span>`;
        }
    },

    async deleteProduct() {
        if (!confirm('정말로 이 상품을 삭제하시겠습니까?')) return;

        try {
            const response = await fetch(`/api/admin/product/${this.productNo}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                alert('삭제되었습니다.');
                window.location.href = '/admin/products';
            } else {
                alert('삭제에 실패했습니다.');
            }
        } catch (error) {
            console.error('Delete Error:', error);
        }
    },

    formatPrice(price) {
        if (!price) return '0원';
        return price.toLocaleString() + '원';
    }
};

document.addEventListener('DOMContentLoaded', () => {
    ProductDetail.init();
});