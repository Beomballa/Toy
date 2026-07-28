const ProductUpdate = {
    initialized: false,
    optionCount: 0,
    productNo: null,
    returnTo: '/admin/products',
    source: '',
    isSubmitting: false,
    isResettingFrontDisplay: false,
    operationPolicy: null,
    frontDisplayData: null,
    frontDisplayRankGuide: null,

    async init(bootstrapProduct = null) {
        if (this.initialized) {
            return;
        }
        this.initialized = true;

        const urlParams = new URLSearchParams(window.location.search);
        this.productNo = this.normalizeProductNo(urlParams.get('no'));
        this.returnTo = CommonJS.normalizeAdminReturnPath(urlParams.get('returnTo'), '/admin/products');
        this.source = CommonJS.normalizeOptionalText(urlParams.get('source')) || '';

        if (!this.isValidProductNo(this.productNo)) {
            await CommonJS.alert('상품 번호가 유효하지 않습니다.', '오류', 'error');
            window.location.href = this.returnTo;
            return;
        }

        this.syncReturnLinks();
        CommonJS.renderSourceContextNotice({ noticeId: 'productUpdateSourceContextNotice', source: this.source });
        this.applyOperationPolicy();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));
        if (this.hasBootstrapProduct(bootstrapProduct)) {
            // 수정 화면도 서버가 이미 가진 상세 모델을 먼저 써서 초기 로딩 왕복을 줄입니다.
            this.fillForm(bootstrapProduct);
            await this.loadFrontDisplayData();
            await this.loadFrontDisplayRankGuide();
            this.updatePreview();
        } else {
            await this.loadProductData();
        }
        this.bindEvents();
        this.applyFeaturedToggleBehavior();

        CommonJS.bindMainLogoNavigation(this.returnTo);
    },

    async applyOperationPolicy(settings = null) {
        try {
            this.operationPolicy = settings || await CommonJS.fetchSystemSettings();
            const disabled = CommonJS.isAdminWriteBlocked(this.operationPolicy);
            const reason = CommonJS.getAdminWriteBlockedReason('상품 수정');
            CommonJS.setButtonDisabled(document.getElementById('btnUpdate'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnAddOption'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnResetFrontDisplay'), disabled, reason);
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
            if (!this.isValidProductNo(this.productNo)) {
                void CommonJS.alert('상품 번호가 유효하지 않습니다.', '알림', 'warning');
                return;
            }
            const sourceQuery = this.source ? `&source=${encodeURIComponent(this.source)}` : '';
            window.location.href = `/admin/products/get?no=${this.productNo}&returnTo=${encodeURIComponent(this.returnTo)}${sourceQuery}`;
        });
        document.getElementById('btnResetFrontDisplay')?.addEventListener('click', () => this.resetFrontDisplay());
        document.getElementById('frontDisplayFeatured')?.addEventListener('change', () => {
            this.applyFeaturedToggleBehavior();
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
            const [productResponse, displayResponse] = await Promise.all([
                fetch(`/api/admin/product/get?no=${this.productNo}`),
                fetch(`/api/admin/product/front-display?productNo=${this.productNo}`)
            ]);
            if (!productResponse.ok) {
                const error = await CommonJS.extractError(productResponse);
                throw new Error(error.message || '상품 정보를 불러오는데 실패했습니다.');
            }

            const data = await productResponse.json();
            this.fillForm(data);
            if (displayResponse.ok) {
                this.fillFrontDisplayForm(await displayResponse.json());
            }
            await this.loadFrontDisplayRankGuide();
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
        document.getElementById('productStatus').value = this.normalizeProductStatus(data.statusCode);

        const tbody = document.getElementById('optionTableBody');
        tbody.innerHTML = '';
        if (data.options && data.options.length > 0) {
            data.options.forEach(opt => {
                this.addOption(opt.optionName, opt.stockQty, opt.additionalPrice, opt.optionNo);
            });
        } else {
            this.showEmptyOptionMessage();
        }
    },

    async loadFrontDisplayData() {
        try {
            const response = await fetch(`/api/admin/product/front-display?productNo=${this.productNo}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '프론트 노출 정보를 불러오지 못했습니다.'));
            }
            this.fillFrontDisplayForm(await response.json());
        } catch (error) {
            console.error('Front Display Load Error:', error);
            this.fillFrontDisplayForm(null);
        }
    },

    fillFrontDisplayForm(data) {
        this.frontDisplayData = data;
        document.getElementById('frontDisplayHeadline').value = data?.headline || '';
        document.getElementById('frontDisplayDescription').value = data?.description || '';
        document.getElementById('frontDisplayMood').value = data?.mood || '';
        document.getElementById('frontDisplayFeatured').value = String(Boolean(data?.featured));
        document.getElementById('frontDisplayRank').value = this.normalizeFeaturedRankValue(data?.featuredRank, Boolean(data?.featured));
        this.applyFeaturedToggleBehavior();
    },

    async loadFrontDisplayRankGuide() {
        try {
            const response = await fetch(`/api/admin/product/front-display/rank-guide?productNo=${this.productNo}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, 'Featured 순번 정보를 불러오지 못했습니다.'));
            }
            this.frontDisplayRankGuide = await response.json();
            this.renderFrontDisplayRankGuide();
            this.applyFeaturedToggleBehavior();
        } catch (error) {
            console.error('Featured rank guide load failed:', error);
            this.renderFrontDisplayRankGuide(error.message || 'Featured 순번 정보를 불러오지 못했습니다.');
        }
    },

    applyFeaturedToggleBehavior() {
        const featuredSelect = document.getElementById('frontDisplayFeatured');
        const rankInput = document.getElementById('frontDisplayRank');
        if (!featuredSelect || !rankInput) {
            return;
        }

        const isFeatured = featuredSelect.value === 'true';
        rankInput.disabled = !isFeatured;
        rankInput.classList.toggle('bg-light', !isFeatured);

        if (!isFeatured) {
            rankInput.value = '999';
            return;
        }

        const currentRank = Number(rankInput.value);
        if (Number.isNaN(currentRank) || currentRank === 999 || currentRank < 1) {
            rankInput.value = String(this.frontDisplayRankGuide?.recommendedRank || 1);
        }
    },

    renderFrontDisplayRankGuide(errorMessage = '') {
        const hint = document.getElementById('frontDisplayRankGuide');
        if (!hint) {
            return;
        }
        if (errorMessage) {
            hint.textContent = errorMessage;
            hint.classList.remove('text-muted');
            hint.classList.add('text-danger');
            return;
        }

        const guide = this.frontDisplayRankGuide;
        if (!guide) {
            hint.textContent = 'Featured 순번 가이드를 불러오는 중입니다.';
            hint.classList.remove('text-danger');
            hint.classList.add('text-muted');
            return;
        }

        const occupied = Array.isArray(guide.occupiedRanks) && guide.occupiedRanks.length
            ? guide.occupiedRanks.join(', ')
            : '없음';
        const available = Array.isArray(guide.availableRanks) && guide.availableRanks.length
            ? guide.availableRanks.join(', ')
            : '-';
        hint.textContent = `추천 ${guide.recommendedRank} · 사용중 ${occupied} · 빈 순번 ${available}`;
        hint.classList.remove('text-danger');
        hint.classList.add('text-muted');
    },

    addOption(name = '', qty = 0, addPrice = 0, persistedOptionNo = null) {
        this.optionCount++;
        const optionId = this.optionCount;
        const lowStockClass = qty < 10 ? 'text-danger fw-bold' : '';

        const optionHtml = `
            <tr class="option-item" data-option-id="${optionId}" ${persistedOptionNo ? `data-option-no="${persistedOptionNo}"` : ''}>
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
        if (!this.isValidProductNo(this.productNo)) {
            await CommonJS.alert('상품 번호가 유효하지 않습니다.', '알림', 'warning');
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('상품 수정'), '알림', 'warning');
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
            status: this.normalizeProductStatus(document.getElementById('productStatus').value),
            options: validationResult.options
        };
        const frontDisplayData = validationResult.frontDisplay;
        if (!this.isValidFrontDisplayPayload(frontDisplayData)) {
            await CommonJS.alert('프론트 노출 입력값을 다시 확인해주세요.', '알림', 'warning');
            return;
        }
        if (!this.validateFrontDisplayRank(frontDisplayData)) {
            await CommonJS.alert('프론트 노출 순서를 다시 확인해주세요.', '알림', 'warning');
            document.getElementById('frontDisplayRank')?.focus();
            return;
        }

        try {
            // 상품과 옵션 식별자 갱신이 한 번만 반영되도록 중복 요청을 먼저 막습니다.
            this.isSubmitting = true;
            this.setSubmitDisabled(true);
            const response = await fetch('/api/admin/product/update', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (response.ok) {
                const displayResponse = await fetch('/api/admin/product/front-display', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(frontDisplayData)
                });
                if (!displayResponse.ok) {
                    const displayMessage = await CommonJS.extractErrorMessage(displayResponse, '프론트 노출 정보 저장에 실패했습니다.');
                    await CommonJS.alert(displayMessage, '오류', 'error');
                    return;
                }
                await CommonJS.alert('상품 정보가 성공적으로 수정되었습니다.', '성공', 'success');
                const sourceQuery = this.source ? `&source=${encodeURIComponent(this.source)}` : '';
                window.location.href = `/admin/products/get?no=${this.productNo}&returnTo=${encodeURIComponent(this.returnTo)}${sourceQuery}`;
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
        document.getElementById('btnResetFrontDisplay').disabled = disabled;
    },

    async resetFrontDisplay() {
        if (this.isResettingFrontDisplay) {
            return;
        }
        if (!this.isValidProductNo(this.productNo)) {
            await CommonJS.alert('상품 번호가 유효하지 않습니다.', '알림', 'warning');
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('프론트 노출 정보 초기화'), '알림', 'warning');
            return;
        }

        const confirmed = await CommonJS.confirm('프론트 노출 정보를 초기화하시겠습니까?', '초기화 확인');
        if (!confirmed) {
            return;
        }

        try {
            this.isResettingFrontDisplay = true;
            this.setSubmitDisabled(true);
            const response = await fetch(`/api/admin/product/front-display/${this.productNo}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '프론트 노출 정보 초기화에 실패했습니다.'));
            }
            this.fillFrontDisplayForm(null);
            await this.loadFrontDisplayRankGuide();
            await CommonJS.alert('프론트 노출 정보가 초기화되었습니다.', '성공', 'success');
        } catch (error) {
            console.error('Front Display Reset Error:', error);
            await CommonJS.alert(error.message || '프론트 노출 정보 초기화 중 오류가 발생했습니다.', '오류', 'error');
        } finally {
            this.isResettingFrontDisplay = false;
            this.setSubmitDisabled(false);
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    validateFormInputs() {
        const categoryNoEl = document.getElementById('categoryNo');
        const brandNoEl = document.getElementById('brandNo');
        const nameKoEl = document.getElementById('nameKo');
        const modelNumEl = document.getElementById('modelNum');
        const releasePriceEl = document.getElementById('releasePrice');
        const thumbnailUrlEl = document.getElementById('thumbnailUrl');
        const frontDisplayHeadlineEl = document.getElementById('frontDisplayHeadline');
        const frontDisplayDescriptionEl = document.getElementById('frontDisplayDescription');
        const frontDisplayMoodEl = document.getElementById('frontDisplayMood');
        const frontDisplayRankEl = document.getElementById('frontDisplayRank');

        const categoryNo = categoryNoEl.value;
        const brandNo = brandNoEl.value;
        const nameKo = this.normalizeRequiredText(nameKoEl.value);
        const modelNum = this.normalizeOptionalText(modelNumEl.value);
        const thumbnailUrl = this.normalizeOptionalText(thumbnailUrlEl.value);
        const frontDisplayHeadline = this.normalizeRequiredText(frontDisplayHeadlineEl.value);
        const frontDisplayDescription = this.normalizeRequiredText(frontDisplayDescriptionEl.value);
        const frontDisplayMood = this.normalizeRequiredText(frontDisplayMoodEl.value);
        const frontDisplayRank = Number(frontDisplayRankEl.value);
        const releasePrice = Number(releasePriceEl.value);

        if (!categoryNo) return this.invalidResult('카테고리를 선택해주세요.', categoryNoEl);
        if (!brandNo) return this.invalidResult('브랜드를 선택해주세요.', brandNoEl);
        if (!this.isValidProductLookupId(categoryNo)) return this.invalidResult('카테고리 값이 올바르지 않습니다.', categoryNoEl);
        if (!this.isValidProductLookupId(brandNo)) return this.invalidResult('브랜드 값이 올바르지 않습니다.', brandNoEl);
        if (!nameKo) return this.invalidResult('상품명을 입력해주세요.', nameKoEl);
        if (nameKo.length > 200) return this.invalidResult('상품명은 200자 이내로 입력해주세요.', nameKoEl);
        if (modelNum && modelNum.length > 200) return this.invalidResult('모델 번호는 200자 이내로 입력해주세요.', modelNumEl);
        if (Number.isNaN(releasePrice)) return this.invalidResult('발매가를 입력해주세요.', releasePriceEl);
        if (releasePrice < 0) return this.invalidResult('발매가는 0원 이상이어야 합니다.', releasePriceEl);
        if (thumbnailUrl && thumbnailUrl.length > 500) return this.invalidResult('썸네일 URL은 500자 이내로 입력해주세요.', thumbnailUrlEl);
        const productStatus = this.normalizeProductStatus(document.getElementById('productStatus').value);
        if (!productStatus) return this.invalidResult('상품 상태 값이 올바르지 않습니다.', document.getElementById('productStatus'));
        if (!frontDisplayHeadline) return this.invalidResult('프론트 헤드라인을 입력해주세요.', frontDisplayHeadlineEl);
        if (frontDisplayHeadline.length > 120) return this.invalidResult('프론트 헤드라인은 120자 이내로 입력해주세요.', frontDisplayHeadlineEl);
        if (!frontDisplayDescription) return this.invalidResult('프론트 설명 문구를 입력해주세요.', frontDisplayDescriptionEl);
        if (frontDisplayDescription.length > 1000) return this.invalidResult('프론트 설명 문구는 1000자 이내로 입력해주세요.', frontDisplayDescriptionEl);
        if (!frontDisplayMood) return this.invalidResult('프론트 무드 키워드를 입력해주세요.', frontDisplayMoodEl);
        if (frontDisplayMood.length > 120) return this.invalidResult('프론트 무드 키워드는 120자 이내로 입력해주세요.', frontDisplayMoodEl);
        const isFeatured = document.getElementById('frontDisplayFeatured').value === 'true';
        if (isFeatured && Number.isNaN(frontDisplayRank)) return this.invalidResult('프론트 노출 순서를 입력해주세요.', frontDisplayRankEl);
        if (isFeatured && (frontDisplayRank < 1 || frontDisplayRank > 999)) {
            return this.invalidResult('프론트 노출 순서는 1~999 사이여야 합니다.', frontDisplayRankEl);
        }

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
            frontDisplay: {
                productNo: parseInt(this.productNo),
                headline: frontDisplayHeadline,
                description: frontDisplayDescription,
                mood: frontDisplayMood,
                featured: isFeatured,
                featuredRank: isFeatured ? frontDisplayRank : 999
            },
            productStatus
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
            const optionNo = Number(item.dataset.optionNo);
            options.push({
                optionNo: Number.isSafeInteger(optionNo) && optionNo > 0 ? optionNo : null,
                optionName,
                stockCnt,
                additionalPrice
            });
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

    validateFrontDisplayRank(frontDisplay) {
        if (!frontDisplay) {
            return false;
        }
        if (!frontDisplay.featured) {
            return frontDisplay.featuredRank === 999;
        }
        return Number.isFinite(frontDisplay.featuredRank) && frontDisplay.featuredRank >= 1 && frontDisplay.featuredRank <= 999;
    },

    isValidProductNo(productNo) {
        return /^\d+$/.test(String(productNo || '')) && Number(productNo) > 0;
    },

    normalizeProductNo(productNo) {
        return this.isValidProductNo(productNo) ? String(Number(productNo)) : null;
    },

    isValidProductLookupId(value) {
        return /^\d+$/.test(String(value || '')) && Number(value) > 0;
    },

    normalizeProductStatus(status) {
        return ['ACTIVE', 'HIDDEN', 'SOLD_OUT'].includes(status) ? status : '';
    },

    normalizeFeaturedRankValue(rank, featured) {
        const parsed = Number(rank);
        if (!featured) {
            return 999;
        }
        return Number.isInteger(parsed) && parsed >= 1 && parsed <= 999 ? parsed : (this.frontDisplayRankGuide?.recommendedRank || 1);
    },

    isValidFrontDisplayPayload(frontDisplay) {
        if (!frontDisplay || !this.isValidProductNo(frontDisplay.productNo)) {
            return false;
        }
        if (!frontDisplay.headline || frontDisplay.headline.length > 120) {
            return false;
        }
        if (!frontDisplay.description || frontDisplay.description.length > 1000) {
            return false;
        }
        if (!frontDisplay.mood || frontDisplay.mood.length > 120) {
            return false;
        }
        return this.validateFrontDisplayRank(frontDisplay);
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
