const ProductCreate = {
    optionCount: 0,
    brands: [],
    categories: [],

    init(brands, categories) {
        // Thymeleaf에서 전달받은 데이터 저장
        this.brands = brands || [];
        this.categories = categories || [];

        console.log('✅ 전달받은 브랜드:', this.brands);
        console.log('✅ 전달받은 카테고리:', this.categories);

        // 선택박스 렌더링
        this.renderSelects();

        // 이벤트 바인딩
        this.bindEvents();
    },

    // 브랜드 & 카테고리 선택박스 렌더링 (하나의 함수로 통합)
    renderSelects() {
        // 브랜드 선택박스 렌더링
        const brandSelect = document.getElementById('brandNo');
        this.brands.forEach(brand => {
            const option = document.createElement('option');
            option.value = brand.brandNo;
            option.textContent = brand.nameKo;
            brandSelect.appendChild(option);
        });

        // 카테고리 선택박스 렌더링
        const categorySelect = document.getElementById('categoryNo');
        this.categories.forEach(category => {
            const option = document.createElement('option');
            option.value = category.categoryNo;
            option.textContent = category.name;
            // depth 정보를 data 속성으로 저장 (필요시 사용)
            option.setAttribute('data-depth', category.depth);
            option.setAttribute('data-parent', category.parentNo || '');
            categorySelect.appendChild(option);
        });

        console.log('✅ 선택박스 렌더링 완료');
    },

    // 이벤트 바인딩
    bindEvents() {
        // 등록 버튼
        document.getElementById('btnSubmit').addEventListener('click', () => this.submitForm());

        // 옵션 추가 버튼
        document.getElementById('btnAddOption').addEventListener('click', () => this.addOption());

        // 실시간 미리보기 - 카테고리
        document.getElementById('categoryNo').addEventListener('change', (e) => {
            const selectedOption = e.target.options[e.target.selectedIndex];
            const text = selectedOption ? selectedOption.text : '-';
            document.getElementById('previewCategory').textContent = text;
        });

        // 실시간 미리보기 - 브랜드
        document.getElementById('brandNo').addEventListener('change', (e) => {
            const selectedOption = e.target.options[e.target.selectedIndex];
            const text = selectedOption ? selectedOption.text : '-';
            document.getElementById('previewBrand').textContent = text;
        });

        // 실시간 미리보기 - 상품명
        document.getElementById('nameKo').addEventListener('input', (e) => {
            document.getElementById('previewName').textContent = e.target.value || '-';
        });

        // 실시간 미리보기 - 모델번호
        document.getElementById('modelNum').addEventListener('input', (e) => {
            document.getElementById('previewModel').textContent = e.target.value || '-';
        });

        // 실시간 미리보기 - 발매가
        document.getElementById('releasePrice').addEventListener('input', (e) => {
            const price = e.target.value
                ? parseInt(e.target.value).toLocaleString() + '원'
                : '-';
            document.getElementById('previewPrice').textContent = price;
        });

        // 실시간 미리보기 - 썸네일
        document.getElementById('thumbnailUrl').addEventListener('input', (e) => {
            const url = e.target.value;
            const previewImage = document.getElementById('previewImage');

            if (url) {
                previewImage.src = url;
            } else {
                previewImage.src = 'https://via.placeholder.com/300x300?text=No+Image';
            }
        });
    },

    // 옵션 추가
    addOption() {
        this.optionCount++;
        const optionId = this.optionCount;

        const optionHtml = `
            <div class="option-item mb-2" data-option-id="${optionId}">
                <div class="input-group">
                    <input type="text" class="form-control option-name" placeholder="예: 250, M, L" required>
                    <button type="button" class="btn btn-outline-danger btn-remove-option" data-option-id="${optionId}">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
            </div>
        `;

        const optionList = document.getElementById('optionList');

        // 첫 번째 옵션이면 안내 메시지 제거
        if (this.optionCount === 1) {
            optionList.innerHTML = '';
        }

        // 새 옵션 추가
        optionList.insertAdjacentHTML('beforeend', optionHtml);

        // 삭제 버튼 이벤트 등록
        const removeBtn = optionList.querySelector(`[data-option-id="${optionId}"].btn-remove-option`);
        removeBtn.addEventListener('click', (e) => {
            const id = e.currentTarget.getAttribute('data-option-id');
            const optionItem = document.querySelector(`.option-item[data-option-id="${id}"]`);
            optionItem.remove();

            // 모든 옵션이 삭제되면 안내 메시지 다시 표시
            const remainingOptions = document.querySelectorAll('.option-item');
            if (remainingOptions.length === 0) {
                document.getElementById('optionList').innerHTML = `
                    <div class="alert alert-info mb-0">
                        <i class="fas fa-info-circle me-2"></i>
                        상품 사이즈 옵션을 추가해주세요. 예: 250, 255, 260, M, L, XL 등
                    </div>
                `;
                this.optionCount = 0;
            }
        });
    },

    // 폼 제출
    async submitForm() {
        // 필수 입력 체크
        const categoryNo = document.getElementById('categoryNo');
        if (!categoryNo.value) {
            alert('카테고리를 선택해주세요.');
            categoryNo.focus();
            return;
        }

        const brandNo = document.getElementById('brandNo');
        if (!brandNo.value) {
            alert('브랜드를 선택해주세요.');
            brandNo.focus();
            return;
        }

        const nameKo = document.getElementById('nameKo');
        if (!nameKo.value) {
            alert('상품명을 입력해주세요.');
            nameKo.focus();
            return;
        }

        const releasePrice = document.getElementById('releasePrice');
        if (!releasePrice.value) {
            alert('발매가를 입력해주세요.');
            releasePrice.focus();
            return;
        }

        // 옵션 수집
        const optionNames = [];
        const optionInputs = document.querySelectorAll('.option-name');
        optionInputs.forEach(input => {
            const value = input.value.trim();
            if (value) {
                optionNames.push(value);
            }
        });

        // Request DTO 생성
        const data = {
            categoryNo: parseInt(categoryNo.value),
            brandNo: parseInt(brandNo.value),
            nameKo: nameKo.value,
            modelNum: document.getElementById('modelNum').value || null,
            releasePrice: parseInt(releasePrice.value),
            releaseDt: document.getElementById('releaseDt').value || null,
            thumbnailUrl: document.getElementById('thumbnailUrl').value || null,
            optionNames: optionNames.length > 0 ? optionNames : null
        };

        console.log('전송 데이터:', data);

        try {
            const response = await fetch('/api/admin/product/set', {
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