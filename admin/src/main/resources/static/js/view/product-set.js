const ProductCreate = {
    optionCount: 0,
    brands: [],
    categories: [],
    returnTo: '/admin/products',
    isSubmitting: false,

    init(brands, categories) {
        // Thymeleaf에서 전달받은 데이터 저장
        this.brands = brands || [];
        this.categories = categories || [];
        this.returnTo = new URLSearchParams(window.location.search).get('returnTo') || '/admin/products';

        // 선택박스 렌더링
        this.renderSelects();

        // 이벤트 바인딩
        this.bindEvents();
        this.syncReturnLinks();

        document.getElementById("main-logo")?.addEventListener("click", () => {
            window.location.href = "/admin/products";
        });
    },

    // 브랜드 & 카테고리 선택박스 렌더링
    renderSelects() {
        const brandSelect = document.getElementById('brandNo');
        this.brands.forEach(brand => {
            const option = document.createElement('option');
            option.value = brand.brandNo;
            option.textContent = brand.nameKo;
            brandSelect.appendChild(option);
        });

        const categorySelect = document.getElementById('categoryNo');
        this.categories.forEach(category => {
            const option = document.createElement('option');
            option.value = category.categoryNo;
            option.textContent = category.name;
            categorySelect.appendChild(option);
        });
    },

    // 이벤트 바인딩
    bindEvents() {
        // 등록 버튼
        document.getElementById('btnSubmit').addEventListener('click', () => this.submitForm());
        document.getElementById('btnBackToProductList').addEventListener('click', () => {
            window.location.href = this.returnTo;
        });

        // 옵션 추가 버튼
        document.getElementById('btnAddOption').addEventListener('click', () => this.addOption());

        // 실시간 미리보기 이벤트들
        const previewMap = {
            'categoryNo': 'previewCategory',
            'brandNo': 'previewBrand',
            'nameKo': 'previewName',
            'modelNum': 'previewModel'
        };

        Object.entries(previewMap).forEach(([id, previewId]) => {
            const el = document.getElementById(id);
            if (!el) return;
            const eventType = el.tagName === 'SELECT' ? 'change' : 'input';
            el.addEventListener(eventType, (e) => {
                const text = el.tagName === 'SELECT' 
                    ? e.target.options[e.target.selectedIndex].text 
                    : e.target.value;
                document.getElementById(previewId).textContent = text || '-';
            });
        });

        document.getElementById('releasePrice').addEventListener('input', (e) => {
            const price = e.target.value
                ? parseInt(e.target.value).toLocaleString() + '원'
                : '-';
            document.getElementById('previewPrice').textContent = price;
        });

        document.getElementById('thumbnailUrl').addEventListener('input', (e) => {
            const url = e.target.value;
            document.getElementById('previewImage').src = url || 'https://via.placeholder.com/300x300?text=No+Image';
        });
    },

    // 옵션 추가
    addOption() {
        this.optionCount++;
        const optionId = this.optionCount;

        const optionHtml = `
            <tr class="option-item" data-option-id="${optionId}">
                <td>
                    <input type="text" class="form-control form-control-sm option-name" placeholder="예: 250" required>
                </td>
                <td>
                    <div class="input-group input-group-sm">
                        <input type="number" class="form-control option-price" placeholder="0" min="0" value="0" required>
                        <span class="input-group-text">원</span>
                    </div>
                </td>
                <td>
                    <div class="input-group input-group-sm">
                        <input type="number" class="form-control option-cnt" placeholder="0" min="0" value="0" required>
                        <span class="input-group-text">개</span>
                    </div>
                </td>
                <td class="text-end">
                    <button type="button" class="btn btn-sm btn-outline-danger btn-remove-option" data-option-id="${optionId}">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            </tr>
        `;

        const tbody = document.getElementById('optionTableBody');
        if (this.optionCount === 1) tbody.innerHTML = '';
        tbody.insertAdjacentHTML('beforeend', optionHtml);

        const row = tbody.querySelector(`tr[data-option-id="${optionId}"]`);
        row.querySelector('.btn-remove-option').addEventListener('click', () => {
            row.remove();
            if (document.querySelectorAll('.option-item').length === 0) {
                tbody.innerHTML = `
                    <tr><td colspan="4" class="text-center py-4 text-muted"><i class="fas fa-info-circle me-2"></i>상품 사이즈 옵션을 추가해주세요.</td></tr>
                `;
                this.optionCount = 0;
            }
        });
    },

    // 폼 제출
    async submitForm() {
        if (this.isSubmitting) {
            return;
        }

        // 필수 입력 체크
        const categoryNo = document.getElementById('categoryNo');
        const brandNo = document.getElementById('brandNo');
        const nameKo = document.getElementById('nameKo');
        const releasePrice = document.getElementById('releasePrice');

        if (!categoryNo.value) { await CommonJS.alert('카테고리를 선택해주세요.', '알림', 'warning'); categoryNo.focus(); return; }
        if (!brandNo.value) { await CommonJS.alert('브랜드를 선택해주세요.', '알림', 'warning'); brandNo.focus(); return; }
        if (!nameKo.value) { await CommonJS.alert('상품명을 입력해주세요.', '알림', 'warning'); nameKo.focus(); return; }
        if (!releasePrice.value) { await CommonJS.alert('발매가를 입력해주세요.', '알림', 'warning'); releasePrice.focus(); return; }

        // 최종 확인
        const isConfirm = await CommonJS.confirm('입력하신 정보로 상품을 등록하시겠습니까?', '상품 등록 확인');
        if (!isConfirm) return;

        // 옵션 수집
        const options = [];
        document.querySelectorAll('.option-item').forEach(input => {
            const nameInput = input.querySelector('.option-name').value.trim();
            const cntInput = parseInt(input.querySelector('.option-cnt').value) || 0;
            const priceInput = parseInt(input.querySelector('.option-price').value) || 0;
            if(nameInput){
                options.push({ 
                    optionName: nameInput, 
                    stockCnt: cntInput,
                    additionalPrice: priceInput
                });
            }
        });

        const data = {
            categoryNo: parseInt(categoryNo.value),
            brandNo: parseInt(brandNo.value),
            nameKo: nameKo.value,
            modelNum: document.getElementById('modelNum').value || null,
            releasePrice: parseInt(releasePrice.value),
            releaseDt: document.getElementById('releaseDt').value || null,
            thumbnailUrl: document.getElementById('thumbnailUrl').value || null,
            options: options
        };

        try {
            this.isSubmitting = true;
            this.setSubmitDisabled(true);
            const response = await fetch('/api/admin/product/set', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (response.ok) {
                await CommonJS.alert('상품이 성공적으로 등록되었습니다.', '성공', 'success');
                window.location.href = this.returnTo;
            } else {
                const message = await CommonJS.extractErrorMessage(response, '알 수 없는 오류');
                await CommonJS.alert('등록 실패: ' + message, '오류', 'error');
            }
        } catch (error) {
            console.error('등록 실패:', error);
            await CommonJS.alert('상품 등록 중 오류가 발생했습니다.', '오류', 'error');
        } finally {
            this.isSubmitting = false;
            this.setSubmitDisabled(false);
        }
    },

    setSubmitDisabled(disabled) {
        document.getElementById('btnSubmit').disabled = disabled;
        document.getElementById('btnBackToProductList').disabled = disabled;
    },

    syncReturnLinks() {
        const returnContext = CommonJS.getReturnContext(this.returnTo, '상품 관리');
        document.getElementById('productListBreadcrumb')?.setAttribute('href', this.returnTo);
        document.getElementById('productListBreadcrumb').textContent = returnContext.label;
        document.getElementById('btnBackToProductList').innerHTML =
            `<i class="fas fa-arrow-left me-2"></i>${returnContext.buttonLabel}`;
    }
};
