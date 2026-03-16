const ProductCreate = {
    optionCount: 0,

    init() {
        this.loadCategories();
        this.loadBrands();
        this.bindEvents();
    },

    // 이벤트 바인딩
    bindEvents() {
        // 등록 버튼
        $('#btnSubmit').on('click', () => this.submitForm());

        // 옵션 추가 버튼
        $('#btnAddOption').on('click', () => this.addOption());

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
            alert('카테고리를 불러오는데 실패했습니다.');
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
            alert('브랜드를 불러오는데 실패했습니다.');
        }
    },

    // 옵션 추가
    addOption() {
        this.optionCount++;
        const optionHtml = `
      <div class="option-item mb-2" data-option-id="${this.optionCount}">
        <div class="input-group">
          <input type="text" class="form-control option-name" placeholder="예: 250, M, L" required>
          <button type="button" class="btn btn-outline-danger btn-remove-option" data-option-id="${this.optionCount}">
            <i class="fas fa-times"></i>
          </button>
        </div>
      </div>
    `;

        if (this.optionCount === 1) {
            $('#optionList').empty();
        }

        $('#optionList').append(optionHtml);

        // 삭제 버튼 이벤트
        $(`.btn-remove-option[data-option-id="${this.optionCount}"]`).on('click', (e) => {
            const id = $(e.currentTarget).data('option-id');
            $(`.option-item[data-option-id="${id}"]`).remove();

            if ($('.option-item').length === 0) {
                $('#optionList').html(`
          <div class="alert alert-info mb-0">
            <i class="fas fa-info-circle me-2"></i>
            상품 사이즈 옵션을 추가해주세요.
          </div>
        `);
                this.optionCount = 0;
            }
        });
    },

    // 폼 제출
    async submitForm() {
        // 필수 입력 체크
        if (!$('#categoryNo').val()) {
            alert('카테고리를 선택해주세요.');
            $('#categoryNo').focus();
            return;
        }

        if (!$('#brandNo').val()) {
            alert('브랜드를 선택해주세요.');
            $('#brandNo').focus();
            return;
        }

        if (!$('#nameKo').val()) {
            alert('상품명을 입력해주세요.');
            $('#nameKo').focus();
            return;
        }

        if (!$('#releasePrice').val()) {
            alert('발매가를 입력해주세요.');
            $('#releasePrice').focus();
            return;
        }

        // 옵션 수집
        const optionNames = [];
        $('.option-name').each(function() {
            const value = $(this).val().trim();
            if (value) {
                optionNames.push(value);
            }
        });

        // Request DTO 생성
        const data = {
            categoryNo: parseInt($('#categoryNo').val()),
            brandNo: parseInt($('#brandNo').val()),
            nameKo: $('#nameKo').val(),
            modelNum: $('#modelNum').val() || null,
            releasePrice: parseInt($('#releasePrice').val()),
            releaseDt: $('#releaseDt').val() || null,
            thumbnailUrl: $('#thumbnailUrl').val() || null,
            optionNames: optionNames.length > 0 ? optionNames : null
        };

        console.log('전송 데이터:', data);

        try {
            const response = await fetch('/api/admin/products', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            });

            if (response.ok) {
                const result = await response.json();
                alert('상품이 성공적으로 등록되었습니다.');
                window.location.href = '/admin/products/' + result.productNo;
            } else {
                const error = await response.json();
                alert('등록 실패: ' + (error.message || '알 수 없는 오류'));
            }
        } catch (error) {
            console.error('등록 실패:', error);
            alert('상품 등록 중 오류가 발생했습니다.');
        }
    }
};
