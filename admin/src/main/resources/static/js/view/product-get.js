const ProductDetail = {
    initialized: false,
    productNo: null,
    productData: null,
    returnTo: '/admin/products',
    source: '',
    operationPolicy: null,
    isCloning: false,
    isDeleting: false,
    isQuickOperating: false,

    async init(bootstrapProduct = null) {
        if (this.initialized) {
            return;
        }
        this.initialized = true;

        const urlParams = new URLSearchParams(window.location.search);
        this.productNo = this.normalizeProductNo(urlParams.get('no'));
        this.returnTo = CommonJS.normalizeOptionalText(urlParams.get('returnTo')) || '/admin/products';
        this.source = CommonJS.normalizeOptionalText(urlParams.get('source')) || '';

        if (!this.isValidProductNo(this.productNo)) {
            await CommonJS.alert('상품 번호가 올바르지 않습니다.', '오류', 'error');
            window.location.href = this.returnTo;
            return;
        }

        this.syncReturnLinks();
        CommonJS.renderSourceContextNotice({ noticeId: 'productDetailSourceContextNotice', source: this.source });
        this.applyOperationPolicy();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));
        if (this.hasBootstrapProduct(bootstrapProduct)) {
            // 서버가 이미 조회한 상세 모델을 우선 사용해서 초기 빈 화면과 추가 왕복을 줄입니다.
            this.renderProduct(bootstrapProduct);
            await this.loadFrontDisplay();
            await this.loadProductHistory();
        } else {
            await this.loadProductDetail();
        }
        this.bindEvents();

        CommonJS.bindMainLogoNavigation(this.returnTo);
    },

    hasBootstrapProduct(bootstrapProduct) {
        return !!bootstrapProduct && String(bootstrapProduct.productNo) === String(this.productNo);
    },

    bindEvents() {
        const btnBack = document.getElementById('btnBackToProductList');
        if (btnBack) {
            btnBack.addEventListener('click', () => {
                window.location.href = this.returnTo;
            });
        }

        const btnEdit = document.getElementById('btnEdit');
        if (btnEdit) {
            btnEdit.addEventListener('click', () => {
                const returnTo = encodeURIComponent(this.returnTo);
                const sourceQuery = this.source ? `&source=${encodeURIComponent(this.source)}` : '';
                window.location.href = `/admin/products/update?no=${this.productNo}&returnTo=${returnTo}${sourceQuery}`;
            });
        }

        const btnDelete = document.getElementById('btnDelete');
        if (btnDelete) {
            btnDelete.addEventListener('click', () => {
                this.deleteProduct();
            });
        }

        const btnSearchImage = document.getElementById('btnSearchImage');
        if (btnSearchImage) {
            btnSearchImage.addEventListener('click', () => {
                if (!this.productData) return;
                CommonJS.openImageSearch(
                    this.productData.productName,
                    this.productData.productModel,
                    this.productData.brandName
                );
            });
        }

        const btnCloneProduct = document.getElementById('btnCloneProduct');
        if (btnCloneProduct) {
            btnCloneProduct.addEventListener('click', () => this.cloneProduct());
        }

        document.getElementById('productQuickStatusMenu')?.addEventListener('click', (event) => {
            const button = event.target.closest('[data-role="product-quick-status"]');
            if (!button) {
                return;
            }
            this.quickOperateStatus(button.dataset.status);
        });

        document.getElementById('btnFrontDisplayManage')?.addEventListener('click', () => {
            const returnTo = encodeURIComponent(window.location.pathname + window.location.search);
            const sourceQuery = this.source ? `&source=${encodeURIComponent(this.source)}` : '';
            window.location.href = `/admin/products/update?no=${this.productNo}&returnTo=${returnTo}${sourceQuery}`;
        });
    },

    async applyOperationPolicy(settings = null) {
        try {
            this.operationPolicy = settings || await CommonJS.fetchSystemSettings();
            const disabled = CommonJS.isAdminWriteBlocked(this.operationPolicy);
            const reason = CommonJS.getAdminWriteBlockedReason('상품 수정, 삭제, 복제');
            CommonJS.setButtonDisabled(document.getElementById('btnEdit'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnDelete'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnCloneProduct'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnProductQuickStatus'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnFrontDisplayManage'), disabled, reason);
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
    },

    async quickOperateStatus(status) {
        if (this.isQuickOperating) {
            return;
        }
        const normalizedStatus = this.normalizeProductStatusCode(status);
        if (!this.productData || !this.isValidProductNo(this.productNo)) {
            await CommonJS.alert('상품 상세를 다시 불러온 후 시도해주세요.', '알림', 'warning');
            return;
        }
        if (!normalizedStatus) {
            await CommonJS.alert('변경할 상태 값이 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('상품 상태 변경'), '알림', 'warning');
            return;
        }
        if (normalizedStatus === this.normalizeProductStatusCode(this.productData.statusCode)) {
            await CommonJS.alert('이미 같은 상태입니다.', '알림', 'info');
            return;
        }

        try {
            this.isQuickOperating = true;
            this.setBusyButton(document.getElementById('btnProductQuickStatus'), true, '처리 중...');
            const response = await fetch(`/api/admin/product/${this.productNo}/quick-operate`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ status: normalizedStatus })
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '상품 상태 변경에 실패했습니다.'));
            }
            await this.loadProductDetail();
            await CommonJS.alert('상품 상태가 변경되었습니다.', '성공', 'success');
        } catch (error) {
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isQuickOperating = false;
            this.setBusyButton(document.getElementById('btnProductQuickStatus'), false);
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    async cloneProduct() {
        if (this.isCloning) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('상품 복제'), '알림', 'warning');
            return;
        }
        const confirmed = await CommonJS.confirm('현재 상품을 복제하시겠습니까?');
        if (!confirmed) {
            return;
        }
        if (!this.productData) {
            await CommonJS.alert('상품 상세를 다시 불러온 후 시도해주세요.', '알림', 'warning');
            return;
        }

        try {
            this.isCloning = true;
            this.setBusyButton(document.getElementById('btnCloneProduct'), true, '복제 중...');
            const response = await fetch(`/api/admin/product/clone/${this.productNo}`, { method: 'POST' });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '상품 복제에 실패했습니다.'));
            }
            const data = await response.json();
            await CommonJS.alert('상품이 복제되었습니다.', '성공', 'success');
            const sourceQuery = this.source ? `&source=${encodeURIComponent(this.source)}` : '';
            window.location.href = `/admin/products/get?no=${data.productNo}&returnTo=${encodeURIComponent(window.location.pathname + window.location.search)}${sourceQuery}`;
        } catch (error) {
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isCloning = false;
            this.setBusyButton(document.getElementById('btnCloneProduct'), false);
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    async loadProductDetail() {
        try {
            const response = await fetch(`/api/admin/product/get?no=${this.productNo}`);

            if (!response.ok) {
                const error = await CommonJS.extractError(response);
                if (error.code === 'P001') {
                    await CommonJS.alert(error.message || '존재하지 않는 상품입니다.', '오류', 'error');
                    window.location.href = this.returnTo;
                    return;
                }
                throw new Error(error.message || '상품 정보를 가져오는데 실패했습니다.');
            }

            const data = await response.json();
            this.renderProduct(data);
            await this.loadFrontDisplay();
            await this.loadProductHistory();

        } catch (error) {
            console.error('Error:', error);
            await CommonJS.alert('데이터를 불러오는 중 오류가 발생했습니다.', '오류', 'error');
            window.location.href = this.returnTo;
        }
    },

    renderProduct(data) {
        this.productData = data;

        const setText = (id, value) => {
            const el = document.getElementById(id);
            if (el) el.textContent = value || '-';
        };

        setText('productTitle', data.productName);
        setText('productNo', data.productNo);
        setText('categoryName', data.categoryName);
        setText('brandName', data.brandName);
        setText('nameKo', data.productName);
        setText('modelNum', data.productModel);
        setText('releaseDt', data.releaseDt);
        setText('crtDtm', data.crtDtm);
        setText('uptDtm', data.uptDtm);
        const normalizedStatusCode = this.normalizeProductStatusCode(data.statusCode);
        setText('statusTextValue', data.statusDesc || '판매중');
        setText('brandChip', data.brandName || '브랜드 -');
        setText('modelChip', data.productModel || '모델 -');
        setText('productCategoryChip', data.categoryName || 'Product Detail');
        setText('releaseMetaChip', data.releaseDt ? `발매일 ${data.releaseDt}` : '발매일 -');

        const releasePriceEl = document.getElementById('releasePrice');
        if (releasePriceEl) releasePriceEl.textContent = this.formatPrice(data.releasePrice);

        const productImage = document.getElementById('productImage');
        const thumbnailUrlLink = document.getElementById('thumbnailUrlLink');
        const thumbnailUrlLinkInline = document.getElementById('thumbnailUrlLinkInline');
        if (data.hasThumbnail) {
            if (productImage) productImage.src = data.thumbnailUrl;
            if (thumbnailUrlLink) {
                thumbnailUrlLink.href = data.thumbnailUrl;
                thumbnailUrlLink.style.display = 'inline-block';
            }
            if (thumbnailUrlLinkInline) {
                thumbnailUrlLinkInline.href = data.thumbnailUrl;
                thumbnailUrlLinkInline.style.display = 'inline-flex';
            }
        } else {
            if (productImage) productImage.src = '/images/product-placeholder.svg';
            if (thumbnailUrlLink) {
                thumbnailUrlLink.removeAttribute('href');
                thumbnailUrlLink.style.display = 'none';
            }
            if (thumbnailUrlLinkInline) {
                thumbnailUrlLinkInline.removeAttribute('href');
                thumbnailUrlLinkInline.style.display = 'none';
            }
        }

        const optionList = document.getElementById('optionList');
        const optionCount = document.getElementById('optionCount');
        const optionMetaText = document.getElementById('productOptionMetaText');
        const totalStockValueEl = document.getElementById('totalStockValue');

        if (data.options && data.options.length > 0) {
            if (optionCount) optionCount.textContent = data.optionCount ?? data.options.length;
            if (optionMetaText) optionMetaText.textContent = `옵션 ${data.optionCount ?? data.options.length}개`;

            const optHtml = data.options.map(opt => `
                <div class="product-option-chip">
                    <span class="product-option-name">${opt.optionName}</span>
                    <span class="product-option-stock">재고 ${opt.stockQty.toLocaleString()}개</span>
                </div>
            `).join('');

            if (optionList) optionList.innerHTML = optHtml;

            if (totalStockValueEl) totalStockValueEl.textContent = `${(data.totalStock ?? 0).toLocaleString()} 개`;

        } else {
            if (optionList) {
                optionList.innerHTML = `
                    <div class="product-empty-state py-4">
                        <i class="fas fa-layer-group product-empty-state-icon"></i>
                        <strong>등록된 옵션 정보가 없습니다.</strong>
                        <p>옵션이 없는 단일 상품이거나 아직 옵션 구성이 저장되지 않았습니다.</p>
                    </div>
                `;
            }
            if (optionCount) optionCount.textContent = String(data.optionCount ?? 0);
            if (optionMetaText) optionMetaText.textContent = `옵션 ${data.optionCount ?? 0}개`;
            if (totalStockValueEl) totalStockValueEl.textContent = `${(data.totalStock ?? 0).toLocaleString()} 개`;
        }

        const statusBadge = document.getElementById('statusBadge');
        if (statusBadge) {
            const statusMeta = CommonJS.getProductStatusMeta(normalizedStatusCode);
            statusBadge.innerHTML = `<span class="badge ${statusMeta.badgeClass}">${data.statusDesc || '판매중'}</span>`;
        }
    },

    async loadFrontDisplay() {
        try {
            const response = await fetch(`/api/admin/product/front-display?productNo=${this.productNo}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '프론트 노출 정보를 불러오지 못했습니다.'));
            }
            const data = await response.json();
            this.renderFrontDisplay(data);
        } catch (error) {
            console.error('Front Display Load Error:', error);
            this.renderFrontDisplay(null);
        }
    },

    renderFrontDisplay(data) {
        const setText = (id, value) => {
            const el = document.getElementById(id);
            if (el) {
                el.textContent = value || '-';
            }
        };
        if (!data) {
            setText('frontDisplayHeadline', '-');
            setText('frontDisplayMood', '-');
            setText('frontDisplayFeatured', '일반 노출');
            setText('frontDisplayRank', '999');
            setText('frontDisplayDescription', '등록된 프론트 노출 문구가 없습니다.');
            return;
        }

        setText('frontDisplayHeadline', data.headline);
        setText('frontDisplayMood', data.mood);
        setText('frontDisplayFeatured', data.featured ? 'Featured' : '일반 노출');
        setText('frontDisplayRank', String(data.featuredRank ?? 999));
        setText('frontDisplayDescription', data.description);
    },

    async loadProductHistory() {
        const historyListEl = document.getElementById('productHistoryList');
        const historyCountEl = document.getElementById('historyCount');
        const historyMetaTextEl = document.getElementById('productHistoryMetaText');

        try {
            const response = await fetch(`/api/admin/product/history?no=${this.productNo}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '상품 변경 이력을 불러오지 못했습니다.'));
            }

            const histories = await response.json();
            if (historyCountEl) {
                historyCountEl.textContent = String(histories.length);
            }
            if (historyMetaTextEl) {
                historyMetaTextEl.textContent = `변경 이력 ${histories.length}건`;
            }

            if (!histories.length) {
                if (historyListEl) {
                    historyListEl.innerHTML = `
                        <div class="product-empty-state py-4">
                            <i class="fas fa-clock-rotate-left product-empty-state-icon"></i>
                            <strong>등록된 변경 이력이 없습니다.</strong>
                            <p>아직 상품 수정, 상태 변경, 재고 관련 이력이 기록되지 않았습니다.</p>
                        </div>
                    `;
                }
                return;
            }

            if (historyListEl) {
                const returnTo = encodeURIComponent(window.location.pathname + window.location.search);
                // 상세 화면 검증에서 텍스트만 보는 것보다 action/status/count가 같이 드러나는 구조가 추적하기 쉽습니다.
                historyListEl.innerHTML = histories.map((history) => `
                    <div class="product-option-chip flex-column align-items-start">
                        <div class="d-flex justify-content-between w-100 gap-2">
                            <strong>${history.actionLabel}</strong>
                            <span class="text-muted small">${history.crtDtm || '-'}</span>
                        </div>
                        <span class="small text-muted">${history.summary}</span>
                        ${history.relatedProductNo ? `
                            <a class="small text-decoration-none" href="/admin/products/get?no=${history.relatedProductNo}&returnTo=${returnTo}${this.source ? `&source=${encodeURIComponent(this.source)}` : ''}">
                                ${history.relatedProductLabel} #${history.relatedProductNo}
                            </a>
                        ` : ''}
                        ${history.activityLogPath ? `
                            <a class="small text-decoration-none" href="${this.buildLogPathFromBase(history.activityLogPath)}">
                                ${history.activityLogLabel || '활동 로그 보기'}
                            </a>
                        ` : ''}
                        <span class="small text-muted">작업자 ${history.actorName || '-'}${history.actorNo ? ` (#${history.actorNo})` : ''}</span>
                        <span class="small text-muted">상태 ${history.statusSnapshot || '-'} · 옵션 ${history.optionCount}개 · 재고 ${Number(history.totalStock || 0).toLocaleString()}개</span>
                    </div>
                `).join('');
            }
        } catch (error) {
            console.error('History Load Error:', error);
            if (historyListEl) {
                historyListEl.innerHTML = `
                    <div class="product-empty-state py-4">
                        <i class="fas fa-triangle-exclamation product-empty-state-icon"></i>
                        <strong>변경 이력을 불러오지 못했습니다.</strong>
                        <p>${this.escapeHtml(error.message || '상품 변경 이력을 불러오지 못했습니다.')}</p>
                    </div>
                `;
            }
            if (historyCountEl) {
                historyCountEl.textContent = '0';
            }
            if (historyMetaTextEl) {
                historyMetaTextEl.textContent = '변경 이력 확인 불가';
            }
        }
    },

    async deleteProduct() {
        if (this.isDeleting) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('상품 삭제'), '알림', 'warning');
            return;
        }
        const isConfirm = await CommonJS.confirm('정말로 이 상품을 삭제하시겠습니까?', '상품 삭제 확인', 'error');
        if (!isConfirm) return;
        if (!this.productData) {
            await CommonJS.alert('상품 상세를 다시 불러온 후 시도해주세요.', '알림', 'warning');
            return;
        }

        try {
            this.isDeleting = true;
            this.setBusyButton(document.getElementById('btnDelete'), true, '삭제 중...');
            const response = await fetch(`/api/admin/product/delete/${this.productNo}`, {
                method: 'PATCH'
            });

            if (response.ok) {
                await CommonJS.alert('삭제되었습니다.', '성공', 'success');
                window.location.href = this.returnTo;
            } else {
                const error = await CommonJS.extractError(response);
                await CommonJS.alert(error.message || '삭제에 실패했습니다.', '오류', 'error');
            }
        } catch (error) {
            console.error('Delete Error:', error);
            await CommonJS.alert('삭제 처리 중 오류가 발생했습니다.', '오류', 'error');
        } finally {
            this.isDeleting = false;
            this.setBusyButton(document.getElementById('btnDelete'), false);
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    formatPrice(price) {
        if (!price) return '0원';
        return price.toLocaleString() + '원';
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
        const backButton = document.getElementById('btnBackToProductList');
        if (backButton) {
            backButton.innerHTML = `<i class="fas fa-list me-2"></i>${returnContext.buttonLabel}`;
            backButton.dataset.returnTo = this.returnTo;
            backButton.dataset.returnButtonLabel = returnContext.buttonLabel;
        }
        if (returnContextMeta) {
            // 복귀 문맥은 브라우저에서 상태 확인이 잦아서 dataset 기준점으로 남겨두는 편이 회귀 추적에 유리합니다.
            returnContextMeta.dataset.returnTo = this.returnTo;
            returnContextMeta.dataset.returnLabel = returnContext.label;
            returnContextMeta.dataset.returnButtonLabel = returnContext.buttonLabel;
        }
    },

    setBusyButton(button, isBusy, busyText = '처리 중...') {
        if (!button) return;
        if (isBusy) {
            if (!button.dataset.originalText) {
                button.dataset.originalText = button.textContent;
            }
            button.disabled = true;
            button.textContent = busyText;
            return;
        }
        button.disabled = false;
        if (button.dataset.originalText) {
            button.textContent = button.dataset.originalText;
            delete button.dataset.originalText;
        }
    },

    isValidProductNo(productNo) {
        return /^\d+$/.test(String(productNo || '')) && Number(productNo) > 0;
    },

    normalizeProductNo(productNo) {
        return this.isValidProductNo(productNo) ? String(Number(productNo)) : null;
    },

    normalizeProductStatusCode(statusCode) {
        return ['ACTIVE', 'HIDDEN', 'SOLD_OUT'].includes(statusCode) ? statusCode : 'ACTIVE';
    },

    buildLogPathFromBase(basePath) {
        if (!basePath) {
            return '';
        }
        const [path, rawQuery = ''] = basePath.split('?');
        const params = new URLSearchParams(rawQuery);
        params.set('returnTo', window.location.pathname + window.location.search);
        if (this.source) {
            params.set('source', this.source);
        }
        return `${path}?${params.toString()}`;
    }
};
