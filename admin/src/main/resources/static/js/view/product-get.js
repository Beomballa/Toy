const ProductDetail = {
    productNo: null,

    init() {
        // URL에서 productNo 추출
        const pathParts = window.location.pathname.split('/');
        this.productNo = pathParts[pathParts.length - 1];

        this.loadProductDetail();
        this.bindEvents();
    },

    bindEvents() {
        // 수정 버튼
        $('#btnEdit').on('click', () => {
            window.location.href = `/admin/products/${this.productNo}/edit`;
        });

        // 삭제 버튼
        $('#btnDelete').on('click', () => this.deleteProduct());
    },

    // 상품 상세 조회
    async loadProductDetail() {
        try {
            const response = await fetch(`/api/admin/products/${this.productNo}`);

            if (!response.ok) {
                throw new Error('상품 정보를 불러올 수 없습니다.');
            }

            const product = await response.json();
            this.renderProduct(product);

        } catch (error) {
            console.error('상품 조회 실패:', error);
            alert('상품 정보를 불러오는데 실패했습니다.');
            window.location.href = '/admin/products';
        }
    },

    // 상품 정보 렌더링
    renderProduct(product) {
        // 페이지 타이틀
        $('#productTitle').text(product.nameKo);
        document.title = `${product.nameKo} | Grade-Stock Admin`;

        // 상태 배지
        const statusClass = this.getStatusClass(product.status);
        const statusText = this.getStatusText(product.status);
        $('#statusBadge').html(`<span class="badge ${statusClass} fs-6">${statusText}</span>`);

        // 상품 이미지
        if (product.thumbnailUrl) {
            $('#productImage').attr('src', product.thumbnailUrl);
        }

        // 기본 정보
        $('#productNo').text(product.productNo);
        $('#categoryName').text(product.categoryName || '-');
        $('#brandName').text(product.brandName || '-');
        $('#nameKo').text(product.nameKo);
        $('#modelNum').text(product.modelNum || '-');
        $('#releasePrice').text(this.formatPrice(product.releasePrice));
        $('#releaseDt').text(this.formatDate(product.releaseDt) || '-');

        // 관리 정보
        $('#crtAdminNo').text(`관리자 #${product.crtAdminNo}`);
        $('#crtDtm').text(this.formatDateTime(product.crtDtm));
        $('#uptDtm').text(this.formatDateTime(product.uptDtm));

        if (product.thumbnailUrl) {
            $('#thumbnailUrl').attr('href', product.thumbnailUrl);
        } else {
            $('#thumbnailUrl').text('이미지 없음').removeAttr('href').removeClass('text-primary');
        }

        // 옵션 렌더링
        if (product.options && product.options.length > 0) {
            $('#optionCount').text(`${product.options.length}개`);
            const optionHtml = product.options.map(opt =>
                `<span class="badge bg-light text-dark border px-3 py-2">${opt.optionName}</span>`
            ).join('');
            $('#optionList').html(optionHtml);
        } else {
            $('#optionCount').text('0개');
            $('#optionList').html('<span class="text-muted">등록된 옵션이 없습니다.</span>');
        }
    },

    // 상품 삭제
    async deleteProduct() {
        if (!confirm('정말 이 상품을 삭제하시겠습니까?\n삭제된 상품은 복구할 수 없습니다.')) {
            return;
        }

        try {
            const response = await fetch(`/api/admin/products/${this.productNo}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                alert('상품이 삭제되었습니다.');
                window.location.href = '/admin/products';
            } else {
                const error = await response.json();
                alert('삭제 실패: ' + (error.message || '알 수 없는 오류'));
            }
        } catch (error) {
            console.error('삭제 실패:', error);
            alert('상품 삭제 중 오류가 발생했습니다.');
        }
    },

    // 유틸리티 함수
    formatPrice(price) {
        return price ? price.toLocaleString() + '원' : '-';
    },

    formatDate(dateString) {
        if (!dateString) return null;
        const date = new Date(dateString);
        return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`;
    },

    formatDateTime(dateTimeString) {
        if (!dateTimeString) return '-';
        const date = new Date(dateTimeString);
        return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
    },

    getStatusClass(status) {
        switch(status) {
            case 'ACTIVE': return 'badge-active';
            case 'HIDDEN': return 'bg-secondary';
            case 'SOLD_OUT': return 'bg-danger';
            default: return 'bg-secondary';
        }
    },

    getStatusText(status) {
        switch(status) {
            case 'ACTIVE': return '판매중';
            case 'HIDDEN': return '숨김';
            case 'SOLD_OUT': return '품절';
            default: return status;
        }
    }
};