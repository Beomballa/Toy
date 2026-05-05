const ProductDetail = {
    productNo: null,
    productData: null,
    returnTo: '/admin/products',

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
        if (this.hasBootstrapProduct(bootstrapProduct)) {
            // 서버가 이미 조회한 상세 모델을 우선 사용해서 초기 빈 화면과 추가 왕복을 줄입니다.
            this.renderProduct(bootstrapProduct);
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

    async deleteProduct() {
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
        if (breadcrumb) {
            breadcrumb.setAttribute('href', this.returnTo);
            breadcrumb.textContent = returnContext.label;
        }
        const backButton = document.getElementById('btnBackToProductList');
        if (backButton) {
            backButton.innerHTML = `<i class="fas fa-list me-2"></i>${returnContext.buttonLabel}`;
        }
    }
};
