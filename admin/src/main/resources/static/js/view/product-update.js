const ProductEdit = {
    productNo: null,
    originalData: null,

    init() {
        // URL에서 productNo 추출
        const pathParts = window.location.pathname.split('/');
        this.productNo = pathParts[pathParts.length - 2]; // /admin/products/{productNo}/edit

        this.loadCategories();
        this.loadBrands();
        this.loadProductDetail();
        this.bindEvents();
    },

    bindEvents() {
        // 취소 버튼
        $('#btnCancel').on('click', () => {
            if (confirm('수정을 취소하시겠습니까? 변경사항은 저장되지 않습니다.')) {
                window.location.href = `/admin/products/${this.productNo}`;
            }
        });

        // 수정 완료 버튼
        $('#btnSubmit').on('click', () => this.submitForm());

        // 실시간 미리보기
        $('#categoryNo').on('change', (e) => {
            const text = $(e.target).find('option:selected').text();
            $('#previewCategory').text(text || '-');
        });

        $('#brandNo').on('change', (e) => {
            const text = $(e.target).find('option:selected').text();
            $('#previewBrand').text(text || '-');
        });

        $('#nameKo').on('input', (e) => {
            $('#previewName').text(e.target.value || '-');
        });

        $('#modelNum').on('input', (e) => {
            $('#previewModel').text(e.target.value || '-');
        });

        $('#releasePrice').on('input', (e) => {
            const price = e.target.value ? parseInt(e.target.value).toLocaleString() + '원' : '-';
            $('#previewPrice').text(price);
        });

        $('#status').on('change', (e) => {
            const statusText = this.getStatusText(e.target.value);
            const statusClass = this.getStatusClass(e.target.value);
            $('#previewStatus').html(`<span class="badge ${statusClass}">${statusText}</span>`);
        });

        $('#thumbnailUrl').on('input', (e) => {
            const url = e.target.value;
            if (url) {
                $('#previewImage').attr('src', url);
            } else {
                $('#previewImage').attr('src', 'https://via.placeholder.com/300x300?text=No+Image');
            }
        });
    },

    // 카테고리 로드
    async loadCategories() {
        try {
            const response = await fetch('/api/admin/categories');
            const categories = await response.json();

            const select = $('#categoryNo');
            categories.forEach(cat => {
                select.append(`<option value="${cat.categoryNo}">${cat.name}</option>`);
            });
        } catch (error) {
            console.error('카테고리 로드 실패:', error);
        }
    },

    // 브랜드 로드
    async loadBrands() {
        try {
            const response = await fetch('/api/admin/brands');
            const brands = await response.json();

            const select = $('#brandNo');
            brands.forEach(brand => {
                select.append(`<option value="${brand.brandNo}">${brand.nameKo}</option>`);
            });
        } catch (error) {
            console.error('브랜드 로드 실패:', error);
        }
    },

    // 상품 상세 조회 및 폼 채우기
    async loadProductDetail() {
        try {
            const response = await fetch(`/api/admin/products/${this.productNo}`);

            if (!response.ok) {
                throw new Error('상품 정보를 불러올 수 없습니다.');
            }

            const product = await response.json();
            this.originalData = product;

            // 폼에 데이터 채우기
            $('#productNo').val(product.productNo);
            $('#categoryNo').val(product.categoryNo);
            $('#brandNo').val(product.brandNo);
            $('#nameKo').val(product.nameKo);
            $('#modelNum').val(product.modelNum || '');
            $('#releasePrice').val(product.releasePrice);
            $('#releaseDt').val(product.releaseDt || '');
            $('#thumbnailUrl').val(product.thumbnailUrl || '');
            $('#status').val(product.status);

            // 등록 정보
            $('#crtAdminNo').text(`관리자 #${product.crtAdminNo}`);
            $('#crtDtm').text(this.formatDateTime(product.crtDtm));
            $('#uptDtm').text(this.formatDateTime(product.uptDtm));

            // 미리보기 업데이트
            $('#previewCategory').text($('#categoryNo option:selected').text());
            $('#previewBrand').text($('#brandNo option:selected').text());
            $('#previewName').text(product.nameKo);
            $('#previewModel').text(product.modelNum || '-');
            $('#previewPrice').text(this.formatPrice(product.releasePrice));

            const statusText = this.getStatusText(product.status);
            const statusClass = this.getStatusClass(product.status);
            $('#previewStatus').html(`<span class="badge ${statusClass}">${statusText}</span>`);

            if (product.thumbnailUrl) {
                $('#previewImage').attr('src', product.thumbnailUrl);
            }

            // 페이지 타이틀
            document.title = `${product.nameKo} 수정 | Grade-Stock Admin`;

        } catch (error) {
            console.error('상품 조회 실패:', error);
            alert('상품 정보를 불러오는데 실패했습니다.');
            window.location.href = '/admin/products';
        }
    },

    // 폼 제출
    async submitForm() {
        // 필수 입력 체크
        if (!$('#categoryNo').val()) {
            alert('카테고리를 선택해주세요.');
            return;
        }

        if (!$('#brandNo').val()) {
            alert('브랜드를 선택해주세요.');
            return;
        }

        if (!$('#nameKo').val()) {
            alert('상품명을 입력해주세요.');
            return;
        }

        if (!$('#releasePrice').val()) {
            alert('발매가를 입력해주세요.');
            return;
        }

        // Request DTO 생성
        const data = {
            categoryNo: parseInt($('#categoryNo').val()),
            brandNo: parseInt($('#brandNo').val()),
            nameKo: $('#nameKo').val(),
            modelNum: $('#modelNum').val() || null,
            releasePrice: parseInt($('#releasePrice').val()),
            releaseDt: $('#releaseDt').val() || null,
            thumbnailUrl: $('#thumbnailUrl').val() || null,
            status: $('#status').val()
        };

        console.log('전송 데이터:', data);

        try {
            const response = await fetch(`/api/admin/products/${this.productNo}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            });

            if (response.ok) {
                alert('상품이 성공적으로 수정되었습니다.');
                window.location.href = `/admin/products/${this.productNo}`;
            } else {
                const error = await response.json();
                alert('수정 실패: ' + (error.message || '알 수 없는 오류'));
            }
        } catch (error) {
            console.error('수정 실패:', error);
            alert('상품 수정 중 오류가 발생했습니다.');
        }
    },

    // 유틸리티 함수
    formatPrice(price) {
        return price ? price.toLocaleString() + '원' : '-';
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