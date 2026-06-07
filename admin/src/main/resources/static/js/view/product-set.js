const ProductCreate = {
    initialized: false,
    optionCount: 0,
    brands: [],
    categories: [],
    returnTo: '/admin/products',
    isSubmitting: false,
    operationPolicy: null,
    frontDisplayRankGuide: null,

    init(brands, categories) {
        if (this.initialized) {
            return;
        }
        this.initialized = true;

        // Thymeleaf에서 전달받은 데이터 저장
        this.brands = brands || [];
        this.categories = categories || [];
        this.returnTo = new URLSearchParams(window.location.search).get('returnTo') || '/admin/products';

        // 선택박스 렌더링
        this.renderSelects();

        // 이벤트 바인딩
        this.bindEvents();
        this.applyFeaturedToggleBehavior();
        this.loadFrontDisplayRankGuide();
        this.syncReturnLinks();
        this.applyOperationPolicy();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));

        CommonJS.bindMainLogoNavigation(this.returnTo);
    },

    async applyOperationPolicy(settings = null) {
        try {
            this.operationPolicy = settings || await CommonJS.fetchSystemSettings();
            const disabled = CommonJS.isAdminWriteBlocked(this.operationPolicy);
            const reason = CommonJS.getAdminWriteBlockedReason('상품 등록');
            CommonJS.setButtonDisabled(document.getElementById('btnSubmit'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnAddOption'), disabled, reason);
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
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
        document.getElementById('productCreateForm')?.addEventListener('submit', (event) => {
            event.preventDefault();
            this.submitForm();
        });
        document.getElementById('btnSubmit')?.addEventListener('click', () => {
            this.submitForm();
        });
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

        document.getElementById('frontDisplayFeatured')?.addEventListener('change', () => {
            this.applyFeaturedToggleBehavior();
        });
    },

    async loadFrontDisplayRankGuide() {
        try {
            const response = await fetch('/api/admin/product/front-display/rank-guide');
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
            hint.textContent = 'Featured 순번 정보를 불러오는 중입니다.';
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
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('상품 등록'), '알림', 'warning');
            return;
        }

        const validationResult = this.validateFormInputs();
        if (!validationResult.valid) {
            await CommonJS.alert(validationResult.message, '알림', 'warning');
            validationResult.focusElement?.focus();
            return;
        }

        // 최종 확인
        const isConfirm = await CommonJS.confirm('입력하신 정보로 상품을 등록하시겠습니까?', '상품 등록 확인');
        if (!isConfirm) return;

        const data = {
            categoryNo: parseInt(validationResult.categoryNo),
            brandNo: parseInt(validationResult.brandNo),
            nameKo: validationResult.nameKo,
            modelNum: validationResult.modelNum,
            releasePrice: validationResult.releasePrice,
            releaseDt: document.getElementById('releaseDt').value || null,
            thumbnailUrl: validationResult.thumbnailUrl,
            options: validationResult.options
        };
        const frontDisplay = validationResult.frontDisplay;

        try {
            this.isSubmitting = true;
            this.setSubmitDisabled(true);
            this.setBusySubmitText(true);
            const response = await fetch('/api/admin/product/set', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (response.ok) {
                const result = await response.json();
                const displayResponse = await fetch('/api/admin/product/front-display', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        productNo: result.productNo,
                        headline: frontDisplay.headline,
                        description: frontDisplay.description,
                        mood: frontDisplay.mood,
                        featured: frontDisplay.featured,
                        featuredRank: frontDisplay.featuredRank
                    })
                });
                if (!displayResponse.ok) {
                    const displayMessage = await CommonJS.extractErrorMessage(displayResponse, '프론트 노출 정보 저장에 실패했습니다.');
                    await CommonJS.alert(displayMessage, '오류', 'error');
                    return;
                }
                await CommonJS.alert('상품이 성공적으로 등록되었습니다.', '성공', 'success');
                const returnTo = encodeURIComponent(this.returnTo);
                window.location.href = `/admin/products/get?no=${result.productNo}&returnTo=${returnTo}`;
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
            this.setBusySubmitText(false);
        }
    },

    setSubmitDisabled(disabled) {
        document.getElementById('btnSubmit').disabled = disabled;
        document.getElementById('btnBackToProductList').disabled = disabled;
        document.getElementById('btnAddOption').disabled = disabled;
    },

    setBusySubmitText(isBusy) {
        const submitButton = document.getElementById('btnSubmit');
        if (!submitButton) return;
        if (isBusy) {
            if (!submitButton.dataset.originalText) {
                submitButton.dataset.originalText = submitButton.textContent;
            }
            submitButton.textContent = '등록 중...';
            return;
        }
        if (submitButton.dataset.originalText) {
            submitButton.textContent = submitButton.dataset.originalText;
            delete submitButton.dataset.originalText;
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
        if (!nameKo) return this.invalidResult('상품명을 입력해주세요.', nameKoEl);
        if (nameKo.length > 200) return this.invalidResult('상품명은 200자 이내로 입력해주세요.', nameKoEl);
        if (modelNum && modelNum.length > 200) return this.invalidResult('모델 번호는 200자 이내로 입력해주세요.', modelNumEl);
        if (Number.isNaN(releasePrice)) return this.invalidResult('발매가를 입력해주세요.', releasePriceEl);
        if (releasePrice < 0) return this.invalidResult('발매가는 0원 이상이어야 합니다.', releasePriceEl);
        if (thumbnailUrl && thumbnailUrl.length > 500) return this.invalidResult('썸네일 URL은 500자 이내로 입력해주세요.', thumbnailUrlEl);
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
                headline: frontDisplayHeadline,
                description: frontDisplayDescription,
                mood: frontDisplayMood,
                featured: isFeatured,
                featuredRank: isFeatured ? frontDisplayRank : 999
            }
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

            // 옵션 row는 동적 추가/삭제가 잦아서 정규화와 중복 체크를 한 흐름에서 끝내야 검증 포인트가 분산되지 않습니다.
            if (!optionName) return this.invalidResult('옵션명을 입력해주세요.', optionNameEl);
            if (optionName.length > 100) return this.invalidResult('옵션명은 100자 이내로 입력해주세요.', optionNameEl);
            if (Number.isNaN(stockCnt)) return this.invalidResult('수량을 입력해주세요.', stockCntEl);
            if (stockCnt < 0) return this.invalidResult('수량은 0개 이상이어야 합니다.', stockCntEl);
            if (Number.isNaN(additionalPrice)) return this.invalidResult('추가 금액을 입력해주세요.', additionalPriceEl);
            if (additionalPrice < 0) return this.invalidResult('추가 금액은 0원 이상이어야 합니다.', additionalPriceEl);
            if (seenOptionNames.has(optionName)) return this.invalidResult('중복된 옵션명은 저장할 수 없습니다.', optionNameEl);

            seenOptionNames.add(optionName);
            options.push({
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

    syncReturnLinks() {
        const returnContext = CommonJS.getReturnContext(this.returnTo, '상품 관리');
        const breadcrumb = document.getElementById('productListBreadcrumb');
        const backButton = document.getElementById('btnBackToProductList');
        const returnContextMeta = document.getElementById('productReturnContextMeta');

        if (breadcrumb) {
            breadcrumb.setAttribute('href', this.returnTo);
            breadcrumb.textContent = returnContext.label;
            breadcrumb.dataset.returnTo = this.returnTo;
            breadcrumb.dataset.returnLabel = returnContext.label;
        }
        if (backButton) {
            backButton.innerHTML = `<i class="fas fa-arrow-left me-2"></i>${returnContext.buttonLabel}`;
            backButton.dataset.returnTo = this.returnTo;
            backButton.dataset.returnButtonLabel = returnContext.buttonLabel;
        }
        if (returnContextMeta) {
            returnContextMeta.dataset.returnTo = this.returnTo;
            returnContextMeta.dataset.returnLabel = returnContext.label;
            returnContextMeta.dataset.returnButtonLabel = returnContext.buttonLabel;
        }
    }
};
