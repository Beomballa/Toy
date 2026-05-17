const ProductDetail = {
    productNo: null,
    productData: null,
    returnTo: '/admin/products',
    operationPolicy: null,

    init(bootstrapProduct = null) {
        const urlParams = new URLSearchParams(window.location.search);
        this.productNo = urlParams.get('no');
        this.returnTo = urlParams.get('returnTo') || '/admin/products';

        if (!this.productNo) {
            CommonJS.alert('상품 번호가 올바르지 않습니다.', '오류', 'error').then(() => {
                window.location.href = this.returnTo;
            });
            return;
        }

        this.syncReturnLinks();
        this.applyOperationPolicy();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));
        if (this.hasBootstrapProduct(bootstrapProduct)) {
            // 서버가 이미 조회한 상세 모델을 우선 사용해서 초기 빈 화면과 추가 왕복을 줄입니다.
            this.renderProduct(bootstrapProduct);
            this.loadProductHistory();
        } else {
            this.loadProductDetail();
        }
        this.bindEvents();

        document.getElementById("main-logo")?.addEventListener("click", () => {
            window.location.href = this.returnTo;
        });
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
                window.location.href = `/admin/products/update?no=${this.productNo}&returnTo=${returnTo}`;
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
    },

    async applyOperationPolicy(settings = null) {
        try {
            this.operationPolicy = settings || await CommonJS.fetchSystemSettings();
            const disabled = CommonJS.isAdminWriteBlocked(this.operationPolicy);
            const reason = CommonJS.getAdminWriteBlockedReason('상품 수정, 삭제, 복제');
            CommonJS.setButtonDisabled(document.getElementById('btnEdit'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnDelete'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnCloneProduct'), disabled, reason);
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
    },

    async cloneProduct() {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('상품 복제'), '알림', 'warning');
            return;
        }
        const confirmed = await CommonJS.confirm('현재 상품을 복제하시겠습니까?');
        if (!confirmed) {
            return;
        }

        try {
            const response = await fetch(`/api/admin/product/clone/${this.productNo}`, { method: 'POST' });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '상품 복제에 실패했습니다.'));
            }
            const data = await response.json();
            await CommonJS.alert('상품이 복제되었습니다.', '성공', 'success');
            window.location.href = `/admin/products/get?no=${data.productNo}&returnTo=${encodeURIComponent(window.location.pathname + window.location.search)}`;
        } catch (error) {
            CommonJS.alert(error.message, '오류', 'error');
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
            this.loadProductHistory();

        } catch (error) {
            console.error('Error:', error);
            CommonJS.alert('데이터를 불러오는 중 오류가 발생했습니다.', '오류', 'error').then(() => {
                window.location.href = this.returnTo;
            });
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
        const totalStockValueEl = document.getElementById('totalStockValue');

        if (data.options && data.options.length > 0) {
            if (optionCount) optionCount.textContent = data.optionCount ?? data.options.length;

            const optHtml = data.options.map(opt => `
                <div class="product-option-chip">
                    <span class="product-option-name">${opt.optionName}</span>
                    <span class="product-option-stock">재고 ${opt.stockQty.toLocaleString()}개</span>
                </div>
            `).join('');

            if (optionList) optionList.innerHTML = optHtml;

            if (totalStockValueEl) totalStockValueEl.textContent = `${(data.totalStock ?? 0).toLocaleString()} 개`;

        } else {
            if (optionList) optionList.innerHTML = '<p class="text-muted small">등록된 옵션 정보가 없습니다.</p>';
            if (optionCount) optionCount.textContent = String(data.optionCount ?? 0);
            if (totalStockValueEl) totalStockValueEl.textContent = `${(data.totalStock ?? 0).toLocaleString()} 개`;
        }

        const statusBadge = document.getElementById('statusBadge');
        if (statusBadge) {
            const statusCode = data.statusCode || 'ACTIVE';
            const statusMeta = CommonJS.getProductStatusMeta(statusCode);
            statusBadge.innerHTML = `<span class="badge ${statusMeta.badgeClass}">${data.statusDesc || '판매중'}</span>`;
        }
    },

    async loadProductHistory() {
        const historyListEl = document.getElementById('productHistoryList');
        const historyCountEl = document.getElementById('historyCount');

        try {
            const response = await fetch(`/api/admin/product/history?no=${this.productNo}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '상품 변경 이력을 불러오지 못했습니다.'));
            }

            const histories = await response.json();
            if (historyCountEl) {
                historyCountEl.textContent = String(histories.length);
            }

            if (!histories.length) {
                if (historyListEl) {
                    historyListEl.innerHTML = '<p class="text-muted small mb-0">등록된 변경 이력이 없습니다.</p>';
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
                            <a class="small text-decoration-none" href="/admin/products/get?no=${history.relatedProductNo}&returnTo=${returnTo}">
                                ${history.relatedProductLabel} #${history.relatedProductNo}
                            </a>
                        ` : ''}
                        ${history.activityLogPath ? `
                            <a class="small text-decoration-none" href="${history.activityLogPath}">
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
                historyListEl.innerHTML = `<p class="text-danger small mb-0">${error.message || '상품 변경 이력을 불러오지 못했습니다.'}</p>`;
            }
            if (historyCountEl) {
                historyCountEl.textContent = '0';
            }
        }
    },

    async deleteProduct() {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('상품 삭제'), '알림', 'warning');
            return;
        }
        const isConfirm = await CommonJS.confirm('정말로 이 상품을 삭제하시겠습니까?', '상품 삭제 확인', 'error');
        if (!isConfirm) return;

        try {
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
    }
};
