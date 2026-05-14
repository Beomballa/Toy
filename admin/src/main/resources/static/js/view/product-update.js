const ProductUpdate = {
    optionCount: 0,
    productNo: null,
    returnTo: '/admin/products',
    isSubmitting: false,
    operationPolicy: null,

    init(bootstrapProduct = null) {
        const urlParams = new URLSearchParams(window.location.search);
        this.productNo = urlParams.get('no');
        this.returnTo = urlParams.get('returnTo') || '/admin/products';

        if (!this.productNo) {
            CommonJS.alert('상품 번호가 유효하지 않습니다.', '오류', 'error').then(() => {
                window.location.href = this.returnTo;
            });
            return;
        }

        this.syncReturnLinks();
        this.applyOperationPolicy();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));
        if (this.hasBootstrapProduct(bootstrapProduct)) {
            // 수정 화면도 서버가 이미 가진 상세 모델을 먼저 써서 초기 로딩 왕복을 줄입니다.
            this.fillForm(bootstrapProduct);
            this.updatePreview();
        } else {
            this.loadProductData();
        }
        this.bindEvents();

        document.getElementById("main-logo")?.addEventListener("click", () => {
            window.location.href = this.returnTo;
        });
    },

    async applyOperationPolicy(settings = null) {
        try {
            this.operationPolicy = settings || await CommonJS.fetchSystemSettings();
            const disabled = CommonJS.isAdminWriteBlocked(this.operationPolicy);
            const reason = '유지보수 모드에서는 상품 수정이 불가능합니다.';
            CommonJS.setButtonDisabled(document.getElementById('btnUpdate'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnAddOption'), disabled, reason);
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
    },

    hasBootstrapProduct(bootstrapProduct) {
        return !!bootstrapProduct && String(bootstrapProduct.productNo) === String(this.productNo);
    },

    bindEvents() {
        document.getElementById('productUpdateForm')?.addEventListener('submit', (event) => {
            event.preventDefault();
            this.updateForm();
        });
        document.getElementById('btnAddOption').addEventListener('click', () => this.addOption());
        document.getElementById('btnCancelEdit')?.addEventListener('click', () => {
            window.location.href = `/admin/products/get?no=${this.productNo}&returnTo=${encodeURIComponent(this.returnTo)}`;
        });

        const previewIds = ['categoryNo', 'brandNo', 'nameKo', 'modelNum', 'releasePrice', 'thumbnailUrl'];
        previewIds.forEach(id => {
            const el = document.getElementById(id);
            if (!el) return;
            const eventType = el.tagName === 'SELECT' ? 'change' : 'input';
            el.addEventListener(eventType, () => this.updatePreview());
        });
    },

    async loadProductData() {
        try {
            const response = await fetch(`/api/admin/product/get?no=${this.productNo}`);
            if (!response.ok) {
                const error = await CommonJS.extractError(response);
                throw new Error(error.message || '상품 정보를 불러오는데 실패했습니다.');
            }

            const data = await response.json();
            this.fillForm(data);
            this.updatePreview();
        } catch (error) {
            console.error('Data Load Error:', error);
            await CommonJS.alert('데이터 로드 중 오류가 발생했습니다.', '오류', 'error');
            window.location.href = this.returnTo;
        }
    },

    fillForm(data) {
        document.getElementById('productNo').value = data.productNo;
        document.getElementById('categoryNo').value = data.categoryNo || '';
        document.getElementById('brandNo').value = data.brandNo || '';
        document.getElementById('nameKo').value = data.productName || '';
        document.getElementById('modelNum').value = data.productModel || '';
        document.getElementById('releasePrice').value = data.releasePrice || 0;
        document.getElementById('releaseDt').value = data.releaseDt || '';
        document.getElementById('thumbnailUrl').value = data.thumbnailUrl || '';
        document.getElementById('productStatus').value = data.statusCode || 'ACTIVE';

        const tbody = document.getElementById('optionTableBody');
        tbody.innerHTML = '';
        if (data.options && data.options.length > 0) {
            data.options.forEach(opt => {
                this.addOption(opt.optionName, opt.stockQty, opt.additionalPrice);
            });
        } else {
            this.showEmptyOptionMessage();
        }
    },

    addOption(name = '', qty = 0, addPrice = 0) {
        this.optionCount++;
        const optionId = this.optionCount;
        const lowStockClass = qty < 10 ? 'text-danger fw-bold' : '';

        const optionHtml = `
            <tr class="option-item" data-option-id="${optionId}">
                <td>
                    <input type="text" class="form-control form-control-sm option-name" placeholder="예: 250" value="${name}" required>
                </td>
                <td>
                    <div class="input-group input-group-sm">
                        <input type="number" class="form-control option-price" placeholder="0" min="0" value="${addPrice}" required>
                        <span class="input-group-text">원</span>
                    </div>
                </td>
                <td>
                    <div class="input-group input-group-sm">
                        <input type="number" class="form-control option-cnt ${lowStockClass}" placeholder="0" min="0" value="${qty}" required>
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
        if (tbody.querySelector('.alert-info')) {
             tbody.innerHTML = '';
        }
        tbody.insertAdjacentHTML('beforeend', optionHtml);

        const row = tbody.querySelector(`tr[data-option-id="${optionId}"]`);
        row.querySelector('.option-cnt').addEventListener('input', (e) => {
            if (parseInt(e.target.value) < 10) e.target.classList.add('text-danger', 'fw-bold');
            else e.target.classList.remove('text-danger', 'fw-bold');
        });

        row.querySelector('.btn-remove-option').addEventListener('click', () => {
            row.remove();
            if (document.querySelectorAll('.option-item').length === 0) {
                this.showEmptyOptionMessage();
                this.optionCount = 0;
            }
        });
    },

    showEmptyOptionMessage() {
        document.getElementById('optionTableBody').innerHTML = `
            <tr><td colspan="4" class="text-center py-4 text-muted"><i class="fas fa-info-circle me-2"></i>상품 옵션을 추가해주세요.</td></tr>
        `;
    },

    updatePreview() {
        const categorySelect = document.getElementById('categoryNo');
        const brandSelect = document.getElementById('brandNo');
        const price = document.getElementById('releasePrice').value;
        document.getElementById('previewPrice').textContent = price ? parseInt(price).toLocaleString() + '원' : '-';
        document.getElementById('previewCategory').textContent = categorySelect.options[categorySelect.selectedIndex]?.text || '-';
        document.getElementById('previewBrand').textContent = brandSelect.options[brandSelect.selectedIndex]?.text || '-';
        document.getElementById('previewName').textContent = document.getElementById('nameKo').value || '-';
        document.getElementById('previewModel').textContent = document.getElementById('modelNum').value || '-';
        document.getElementById('previewStatus').textContent =
            document.getElementById('productStatus').options[document.getElementById('productStatus').selectedIndex]?.text || '판매중 (ACTIVE)';

        const url = document.getElementById('thumbnailUrl').value;
        const previewImage = document.getElementById('previewImage');
        const previewText = document.getElementById('previewText');

        if (url) {
            previewImage.src = url;
            previewImage.classList.remove('d-none');
            previewText.classList.add('d-none');
        } else {
            previewImage.src = '';
            previewImage.classList.add('d-none');
            previewText.classList.remove('d-none');
        }
    },

    async updateForm() {
        if (this.isSubmitting) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 상품 수정이 불가능합니다.', '알림', 'warning');
            return;
        }

        const validationResult = this.validateFormInputs();
        if (!validationResult.valid) {
            await CommonJS.alert(validationResult.message, '알림', 'warning');
            validationResult.focusElement?.focus();
            return;
        }

        const isConfirm = await CommonJS.confirm('상품 정보를 수정하시겠습니까?', '상품 수정 확인');
        if (!isConfirm) return;

        const data = {
            productNo: parseInt(this.productNo),
            categoryNo: parseInt(validationResult.categoryNo),
            brandNo: parseInt(validationResult.brandNo),
            nameKo: validationResult.nameKo,
            modelNum: validationResult.modelNum,
            releasePrice: validationResult.releasePrice,
            releaseDt: document.getElementById('releaseDt').value || null,
            thumbnailUrl: validationResult.thumbnailUrl,
            status: document.getElementById('productStatus').value,
            options: validationResult.options
        };

        try {
            // 수정 API는 옵션 삭제/재등록까지 같이 처리하므로 중복 요청을 먼저 막습니다.
            this.isSubmitting = true;
            this.setSubmitDisabled(true);
            const response = await fetch('/api/admin/product/update', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (response.ok) {
                await CommonJS.alert('상품 정보가 성공적으로 수정되었습니다.', '성공', 'success');
                window.location.href = `/admin/products/get?no=${this.productNo}&returnTo=${encodeURIComponent(this.returnTo)}`;
            } else {
                const message = await CommonJS.extractErrorMessage(response, '알 수 없는 오류');
                await CommonJS.alert('수정 실패: ' + message, '오류', 'error');
            }
        } catch (error) {
            console.error('Update Error:', error);
            await CommonJS.alert('수정 중 오류가 발생했습니다.', '오류', 'error');
        } finally {
            this.isSubmitting = false;
            this.setSubmitDisabled(false);
        }
    },

    setSubmitDisabled(disabled) {
        document.getElementById('btnUpdate').disabled = disabled;
        document.getElementById('btnCancelEdit').disabled = disabled;
        document.getElementById('btnAddOption').disabled = disabled;
    },

    validateFormInputs() {
        const categoryNoEl = document.getElementById('categoryNo');
        const brandNoEl = document.getElementById('brandNo');
        const nameKoEl = document.getElementById('nameKo');
        const modelNumEl = document.getElementById('modelNum');
        const releasePriceEl = document.getElementById('releasePrice');
        const thumbnailUrlEl = document.getElementById('thumbnailUrl');

        const categoryNo = categoryNoEl.value;
        const brandNo = brandNoEl.value;
        const nameKo = this.normalizeRequiredText(nameKoEl.value);
        const modelNum = this.normalizeOptionalText(modelNumEl.value);
        const thumbnailUrl = this.normalizeOptionalText(thumbnailUrlEl.value);
        const releasePrice = Number(releasePriceEl.value);

        if (!categoryNo) return this.invalidResult('카테고리를 선택해주세요.', categoryNoEl);
        if (!brandNo) return this.invalidResult('브랜드를 선택해주세요.', brandNoEl);
        if (!nameKo) return this.invalidResult('상품명을 입력해주세요.', nameKoEl);
        if (nameKo.length > 200) return this.invalidResult('상품명은 200자 이내로 입력해주세요.', nameKoEl);
        if (modelNum && modelNum.length > 200) return this.invalidResult('모델 번호는 200자 이내로 입력해주세요.', modelNumEl);
        if (Number.isNaN(releasePrice)) return this.invalidResult('발매가를 입력해주세요.', releasePriceEl);
        if (releasePrice < 0) return this.invalidResult('발매가는 0원 이상이어야 합니다.', releasePriceEl);
        if (thumbnailUrl && thumbnailUrl.length > 500) return this.invalidResult('썸네일 URL은 500자 이내로 입력해주세요.', thumbnailUrlEl);

        const optionValidation = this.collectAndValidateOptions();
        if (!optionValidation.valid) {
            return optionValidation;
        }

        return {
            valid: true,
            categoryNo,
            brandNo,
            nameKo,
            modelNum,
            releasePrice,
            thumbnailUrl,
            options: optionValidation.options,
        };
    },

    collectAndValidateOptions() {
        const seenOptionNames = new Set();
        const options = [];

        for (const item of document.querySelectorAll('.option-item')) {
            const optionNameEl = item.querySelector('.option-name');
            const stockCntEl = item.querySelector('.option-cnt');
            const additionalPriceEl = item.querySelector('.option-price');
            const optionName = this.normalizeRequiredText(optionNameEl.value);
            const stockCnt = Number(stockCntEl.value);
            const additionalPrice = Number(additionalPriceEl.value || 0);

            // 옵션 row는 동적 추가/삭제가 자주 일어나서 여기서 정규화와 중복 체크를 같이 묶어야 흐름을 따라가기 쉽습니다.
            if (!optionName) return this.invalidResult('옵션명을 입력해주세요.', optionNameEl);
            if (optionName.length > 100) return this.invalidResult('옵션명은 100자 이내로 입력해주세요.', optionNameEl);
            if (Number.isNaN(stockCnt)) return this.invalidResult('수량을 입력해주세요.', stockCntEl);
            if (stockCnt < 0) return this.invalidResult('수량은 0개 이상이어야 합니다.', stockCntEl);
            if (Number.isNaN(additionalPrice)) return this.invalidResult('추가 금액을 입력해주세요.', additionalPriceEl);
            if (additionalPrice < 0) return this.invalidResult('추가 금액은 0원 이상이어야 합니다.', additionalPriceEl);
            if (seenOptionNames.has(optionName)) return this.invalidResult('중복된 옵션명은 저장할 수 없습니다.', optionNameEl);

            seenOptionNames.add(optionName);
            options.push({ optionName, stockCnt, additionalPrice });
        }

        return { valid: true, options };
    },

    invalidResult(message, focusElement) {
        return { valid: false, message, focusElement };
    },

    normalizeRequiredText(value) {
        return CommonJS.normalizeRequiredText(value);
    },

    normalizeOptionalText(value) {
        return CommonJS.normalizeOptionalText(value);
    },

    syncReturnLinks() {
        const returnContext = CommonJS.getReturnContext(this.returnTo, '상품 관리');
        const breadcrumb = document.getElementById('productListBreadcrumb');
        const returnContextMeta = document.getElementById('productReturnContextMeta');
        if (breadcrumb) {
            breadcrumb.setAttribute('href', this.returnTo);
            breadcrumb.textContent = returnContext.label;
            breadcrumb.dataset.returnTo = this.returnTo;
            breadcrumb.dataset.returnLabel = returnContext.label;
        }
        const cancelButton = document.getElementById('btnCancelEdit');
        if (cancelButton) {
            cancelButton.textContent = returnContext.buttonLabel;
            cancelButton.dataset.returnTo = this.returnTo;
            cancelButton.dataset.returnButtonLabel = returnContext.buttonLabel;
        }
        if (returnContextMeta) {
            returnContextMeta.dataset.returnTo = this.returnTo;
            returnContextMeta.dataset.returnLabel = returnContext.label;
            returnContextMeta.dataset.returnButtonLabel = returnContext.buttonLabel;
        }
    }
};
