const OrderDetail = {
    initialized: false,
    async init() {
        if (this.initialized) return;
        this.initialized = true;
        const params = new URLSearchParams(window.location.search);
        this.orderNo = this.normalizeOrderNo(params.get('no'));
        this.returnTo = CommonJS.normalizeAdminReturnPath(params.get('returnTo'), '/admin/orders/list');
        this.source = CommonJS.normalizeOptionalText(params.get('source')) || '';
        this.isSubmitting = false;
        this.operationPolicy = null;
        if (!this.isValidOrderNo(this.orderNo)) {
            await CommonJS.alert('잘못된 접근입니다.', '오류', 'error');
            location.href = this.returnTo;
            return;
        }

        this.syncReturnLinks();
        CommonJS.renderSourceContextNotice({ noticeId: 'orderDetailSourceContextNotice', source: this.source });
        this.bindEvents();
        this.applyOperationPolicy();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));
        await this.getDetail();
    },

    bindEvents() {
        document.getElementById('deliveryForm')?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.saveDelivery();
        });
        document.getElementById('btnCompleteDelivery')?.addEventListener('click', () => this.completeDelivery());
        document.getElementById('btnCancelOrder')?.addEventListener('click', () => this.cancelOrder());
        document.getElementById('btnSaveAdminMemo')?.addEventListener('click', () => this.saveAdminMemo());
        document.getElementById('btnOpenOrderHistory')?.addEventListener('click', () => {
            if (!this.isValidOrderNo(this.orderNo)) {
                void CommonJS.alert('유효한 주문 번호를 확인할 수 없습니다.', '알림', 'warning');
                return;
            }
            const returnTo = encodeURIComponent(window.location.pathname + window.location.search);
            const sourceQuery = this.source ? `&source=${encodeURIComponent(this.source)}` : '';
            window.location.href = `/admin/orders/history?orderNo=${this.orderNo}&returnTo=${returnTo}${sourceQuery}`;
        });
        document.getElementById('orderItemsTableBody')?.addEventListener('click', (event) => {
            const detailButton = event.target.closest('[data-role="open-order-item-product"]');
            if (detailButton) {
                const productNo = this.normalizeOrderNo(detailButton.dataset.productNo);
                if (!productNo) {
                    void CommonJS.alert('유효한 상품 번호를 확인할 수 없습니다.', '알림', 'warning');
                    return;
                }
                const returnTo = encodeURIComponent(window.location.pathname + window.location.search);
                const sourceQuery = this.source ? `&source=${encodeURIComponent(this.source)}` : '';
                window.location.href = `/admin/products/get?no=${productNo}&returnTo=${returnTo}${sourceQuery}`;
                return;
            }

            const imageButton = event.target.closest('[data-role="search-order-item-image"]');
            if (imageButton) {
                CommonJS.openImageSearch(
                    imageButton.dataset.productName,
                    imageButton.dataset.productNo,
                    ''
                );
            }
        });
    },

    async applyOperationPolicy(settings = null) {
        try {
            this.operationPolicy = settings || await CommonJS.fetchSystemSettings();
            const disabled = CommonJS.isAdminWriteBlocked(this.operationPolicy);
            const reason = CommonJS.getAdminWriteBlockedReason('주문 처리와 관리 메모 저장');

            CommonJS.setButtonDisabled(document.getElementById('btnSaveDelivery'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnCompleteDelivery'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnCancelOrder'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnSaveAdminMemo'), disabled, reason);
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
    },

    async getDetail() {
        try {
            const res = await fetch(`/api/admin/orders/get?no=${this.orderNo}`);
            if (!res.ok) {
                const error = await CommonJS.extractError(res);
                if (error.code === 'O001') {
                    await CommonJS.alert(error.message || '존재하지 않는 주문입니다.', '오류', 'error');
                    location.href = this.returnTo;
                    return;
                }
                throw new Error(error.message || '데이터를 불러오는 중 오류가 발생했습니다.');
            }

            const data = await res.json();
            this.renderDetail(data);
        } catch (err) {
            console.error('주문 상세 로드 실패:', err);
            await CommonJS.alert(err.message || '데이터를 불러오는 중 오류가 발생했습니다.', '오류', 'error');
        }
    },

    renderDetail(data) {
        this.currentDetail = data;
        this.renderSummary(data);
        this.renderActionVisibility(data);
        this.renderDeliveryInfo(data);
        this.renderOrderItems(Array.isArray(data.items) ? data.items : []);
        this.renderAdminMemo(data.adminMemo);
        this.renderOrderHistory(data.histories || []);
        void this.applyOperationPolicy(this.operationPolicy);
    },

    renderSummary(data) {
        const normalizedStatusCode = this.normalizeOrderStatusCode(data.statusCode);
        document.getElementById('orderNumDisplay').textContent = data.orderNum;
        document.getElementById('buyerName').textContent = data.buyerName;
        document.getElementById('buyerPhone').textContent = data.buyerPhone;
        document.getElementById('orderDt').textContent = data.orderDt;
        document.getElementById('orderDtMeta').textContent = `주문일시 ${data.orderDt || '-'}`;
        document.getElementById('totalAmount').textContent = data.totalAmount;
        document.getElementById('itemCount').textContent = Array.isArray(data.items) ? data.items.length : 0;

        const statusMeta = CommonJS.getOrderStatusMeta(normalizedStatusCode);
        const badge = document.getElementById('orderStatusBadge');
        badge.textContent = data.statusDesc;
        badge.className = `badge rounded-pill ${statusMeta.badgeClass}`;
    },

    renderActionVisibility(data) {
        const btnCancel = document.getElementById('btnCancelOrder');
        const btnComplete = document.getElementById('btnCompleteDelivery');
        const inputCard = document.getElementById('deliveryInputCard');
        const infoCard = document.getElementById('deliveryInfoCard');

        btnCancel.style.display = data.canCancel ? 'block' : 'none';
        inputCard.style.display = data.showDeliveryInput ? 'block' : 'none';
        infoCard.style.display = data.showDeliveryInfo ? 'block' : 'none';
        btnComplete.style.display = data.canCompleteDelivery ? 'block' : 'none';
    },

    renderDeliveryInfo(data) {
        if (data.showDeliveryInfo) {
            document.getElementById('displayCompany').innerText = data.deliveryCompany || '-';
            document.getElementById('displayTracking').innerText = data.trackingNum || '-';
        }
    },

    renderAdminMemo(adminMemo) {
        const memoInput = document.getElementById('adminMemo');
        if (memoInput) {
            memoInput.value = adminMemo || '';
        }
    },

    renderOrderItems(items) {
        const tbody = document.getElementById('orderItemsTableBody');
        if (items.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center py-4 text-muted">주문 상품 정보가 없습니다.</td></tr>';
            return;
        }

        tbody.innerHTML = items.map(item => {
            const productName = CommonJS.escapeHtml(item.productName || '-');
            const productNo = CommonJS.escapeHtml(item.productNo || '');
            const thumbnailUrl = CommonJS.escapeHtml(CommonJS.normalizeImageSource(item.thumbnailUrl));
            return `
            <tr class="item-row">
                <td class="ps-4">
                    <div style="width:64px; height:64px;">
                        <img src="${thumbnailUrl}" alt="${productName}" class="product-img">
                    </div>
                </td>
                <td>
                    <div class="fw-bold text-dark">${productName}</div>
                    <div class="text-muted small">상품번호: ${productNo}</div>
                    <div class="d-flex flex-wrap gap-2 mt-2">
                        <button type="button"
                                class="btn btn-sm btn-outline-secondary"
                                data-role="open-order-item-product"
                                data-product-no="${productNo}">
                            상품 상세
                        </button>
                        <button type="button"
                                class="btn btn-sm btn-outline-secondary"
                                data-role="search-order-item-image"
                                data-product-name="${productName}"
                                data-product-no="${productNo}">
                            이미지 검색
                        </button>
                    </div>
                </td>
                <td class="text-center fw-medium">${CommonJS.escapeHtml(item.count || 0)}개</td>
                <td class="text-end pe-4 fw-bold text-primary">${CommonJS.escapeHtml(item.orderPrice || '-')}</td>
            </tr>
        `;
        }).join('');
        tbody.querySelectorAll('img.product-img').forEach((image) => {
            image.addEventListener('error', () => CommonJS.handleImageError(image));
        });
    },

    renderOrderHistory(histories) {
        const container = document.getElementById('orderHistoryList');
        const metaTextEl = document.getElementById('orderHistoryMetaText');
        if (!container) {
            return;
        }
        const returnTo = encodeURIComponent(window.location.pathname + window.location.search);

        if (!histories.length) {
            container.innerHTML = `
                <div class="product-empty-state py-4">
                    <i class="fas fa-box-open product-empty-state-icon"></i>
                    <strong>등록된 주문 처리 이력이 없습니다.</strong>
                    <p>아직 상태 변경, 배송 처리, 관리 메모 기록이 남아 있지 않습니다.</p>
                </div>
            `;
            if (metaTextEl) {
                metaTextEl.textContent = '주문 처리 이력 0건';
            }
            return;
        }

        if (metaTextEl) {
            metaTextEl.textContent = `주문 처리 이력 ${histories.length}건`;
        }

        container.innerHTML = histories.map((history) => `
            <div class="border rounded-3 p-3">
                <div class="d-flex justify-content-between align-items-start gap-3 mb-2">
                    <div class="fw-semibold text-dark">${CommonJS.escapeHtml(history.actionLabel || history.actionType || '-')}</div>
                    <div class="text-muted small">${CommonJS.escapeHtml(history.crtDtm || '-')}</div>
                </div>
                <div class="small text-muted mb-2">
                    상태: ${CommonJS.escapeHtml(history.beforeStatusDesc || '-')} -> ${CommonJS.escapeHtml(history.afterStatusDesc || '-')}
                </div>
                ${history.reason ? `<div class="small mb-1"><span class="text-muted">사유</span> ${CommonJS.escapeHtml(history.reason)}</div>` : ''}
                ${history.adminMemoSnapshot ? `<div class="small mb-1"><span class="text-muted">메모</span> ${CommonJS.escapeHtml(history.adminMemoSnapshot)}</div>` : ''}
                ${(history.deliveryCompany || history.trackingNum) ? `<div class="small"><span class="text-muted">배송</span> ${CommonJS.escapeHtml(history.deliveryCompany || '-')} / ${CommonJS.escapeHtml(history.trackingNum || '-')}</div>` : ''}
                <div class="d-flex flex-wrap gap-2 small mt-1">
                    <a class="text-decoration-none" href="/admin/orders/history?orderNo=${encodeURIComponent(this.orderNo)}&historyNo=${encodeURIComponent(history.historyNo)}&returnTo=${returnTo}${this.source ? `&source=${encodeURIComponent(this.source)}` : ''}">이력 위치 보기</a>
                    ${this.buildActivityLogLink(history)}
                </div>
            </div>
        `).join('');
    },

    async completeDelivery() {
        if (!this.isValidCurrentOrderAction()) {
            await CommonJS.alert('유효한 주문 정보를 확인할 수 없습니다.', '알림', 'warning');
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('배송 완료 처리'), '알림', 'warning');
            return;
        }

        const isConfirm = await CommonJS.confirm('배송 완료 처리를 하시겠습니까?');
        if (!isConfirm) return;

        await this.submitOrderAction({
            url: '/api/admin/orders/delivery-complete',
            payload: { orderNo: this.orderNo, reason: this.readActionReason() },
            successMessage: '배송 완료 처리가 되었습니다.',
            fallbackErrorMessage: '배송 완료 처리 중 오류가 발생했습니다.',
            logLabel: '배송 완료 처리'
        });
    },

    buildLogPathFromBase(basePath) {
        const safeBasePath = CommonJS.normalizeAdminReturnPath(basePath, '');
        if (!safeBasePath) {
            return '';
        }
        const [path, rawQuery = ''] = safeBasePath.split('?');
        const params = new URLSearchParams(rawQuery);
        params.set('returnTo', window.location.pathname + window.location.search);
        if (this.source) {
            params.set('source', this.source);
        }
        return `${path}?${params.toString()}`;
    },

    buildActivityLogLink(history) {
        const path = this.buildLogPathFromBase(history.activityLogPath);
        if (!path) {
            return '';
        }
        return `<a class="text-decoration-none" href="${CommonJS.escapeHtml(path)}">${CommonJS.escapeHtml(history.activityLogLabel || '활동 로그 보기')}</a>`;
    },

    async cancelOrder() {
        if (!this.isValidCurrentOrderAction()) {
            await CommonJS.alert('유효한 주문 정보를 확인할 수 없습니다.', '알림', 'warning');
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('주문 취소'), '알림', 'warning');
            return;
        }

        const isConfirm = await CommonJS.confirm('주문을 취소하시겠습니까?');
        if (!isConfirm) return;

        await this.submitOrderAction({
            url: '/api/admin/orders/cancel',
            payload: { orderNo: this.orderNo, reason: this.readActionReason() },
            successMessage: '주문이 취소되었습니다.',
            fallbackErrorMessage: '주문 취소 중 오류가 발생했습니다.',
            logLabel: '주문 취소'
        });
    },

    async saveDelivery() {
        if (!this.isValidCurrentOrderAction()) {
            await CommonJS.alert('유효한 주문 정보를 확인할 수 없습니다.', '알림', 'warning');
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('배송 정보 저장'), '알림', 'warning');
            return;
        }

        const companyInput = document.getElementById('deliveryCompany');
        const trackingInput = document.getElementById('trackingNum');
        const company = companyInput.value.trim().replace(/\s+/g, ' ');
        const tracking = trackingInput.value.trim();

        if (!company || !tracking) {
            await CommonJS.alert('택배사와 운송장 번호를 모두 입력하세요.', '알림', 'warning');
            return;
        }
        if (!this.validateDeliveryPayload(company, tracking)) {
            await CommonJS.alert('배송 정보 입력값을 다시 확인하세요.', '알림', 'warning');
            return;
        }

        companyInput.value = company;
        trackingInput.value = tracking;
        if (
            this.currentDetail?.showDeliveryInfo &&
            (this.currentDetail.deliveryCompany || '') === company &&
            (this.currentDetail.trackingNum || '') === tracking
        ) {
            await CommonJS.alert('변경된 배송 정보가 없습니다.', '알림', 'info');
            return;
        }

        await this.submitOrderAction({
            url: '/api/admin/orders/delivery',
            payload: {
                orderNo: this.orderNo,
                deliveryCompany: company,
                trackingNum: tracking,
                reason: this.readActionReason()
            },
            successMessage: '배송 정보가 등록되었으며 상태가 배송중으로 변경되었습니다.',
            fallbackErrorMessage: '배송 정보 저장 중 오류가 발생했습니다.',
            logLabel: '배송 정보 저장'
        });
    },

    async saveAdminMemo() {
        if (!this.isValidCurrentOrderAction()) {
            await CommonJS.alert('유효한 주문 정보를 확인할 수 없습니다.', '알림', 'warning');
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('관리 메모 저장'), '알림', 'warning');
            return;
        }

        const adminMemo = this.normalizeAdminMemo(document.getElementById('adminMemo')?.value || '');
        const memoInput = document.getElementById('adminMemo');
        if (memoInput) {
            memoInput.value = adminMemo;
        }
        if (adminMemo.length > 500) {
            await CommonJS.alert('관리 메모는 500자 이하로 입력하세요.', '알림', 'warning');
            return;
        }
        if ((this.currentDetail?.adminMemo || '') === adminMemo) {
            await CommonJS.alert('변경된 관리 메모가 없습니다.', '알림', 'info');
            return;
        }
        await this.submitOrderAction({
            url: '/api/admin/orders/memo',
            method: 'PATCH',
            payload: {
                orderNo: this.orderNo,
                adminMemo: adminMemo
            },
            successMessage: '관리 메모가 저장되었습니다.',
            fallbackErrorMessage: '관리 메모 저장 중 오류가 발생했습니다.',
            logLabel: '관리 메모 저장'
        });
    },

    async submitOrderAction({ url, method = 'POST', payload, successMessage, fallbackErrorMessage, logLabel }) {
        if (this.isSubmitting) {
            return;
        }
        if (!this.isValidOrderActionPayload(payload)) {
            await CommonJS.alert('주문 처리 요청 값이 올바르지 않습니다.', '알림', 'warning');
            return;
        }

        this.isSubmitting = true;
        this.setActionButtonsDisabled(true);

        try {
            const res = await fetch(url, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!res.ok) {
                throw new Error(await CommonJS.extractErrorMessage(res, fallbackErrorMessage));
            }

            await this.getDetail();
            await CommonJS.alert(successMessage, '성공', 'success');
        } catch (err) {
            console.error(`${logLabel} 실패:`, err);
            await CommonJS.alert(err.message || fallbackErrorMessage, '오류', 'error');
        } finally {
            this.isSubmitting = false;
            this.setActionButtonsDisabled(false);
            void this.applyOperationPolicy(this.operationPolicy);
        }
    },

    syncReturnLinks() {
        const returnContext = CommonJS.getReturnContext(this.returnTo, '주문 관리');
        const breadcrumb = document.getElementById('orderListBreadcrumb');
        if (breadcrumb) {
            breadcrumb.setAttribute('href', this.returnTo);
            breadcrumb.textContent = returnContext.label;
        }
        const backButton = document.getElementById('btnBackToOrderList');
        if (backButton) {
            backButton.textContent = returnContext.buttonLabel;
        }
        document.getElementById('btnBackToOrderList')?.addEventListener('click', () => {
            location.href = this.returnTo;
        });
    },

    readActionReason() {
        const input = document.getElementById('orderActionReason');
        if (!input) {
            return null;
        }
        const normalized = input.value.trim().replace(/\s+/g, ' ');
        input.value = normalized;
        return normalized || null;
    },

    setActionButtonsDisabled(disabled) {
        // 상세 액션은 중복 요청이 그대로 상태 전이 중복으로 이어질 수 있어서 전송 중 버튼을 잠급니다.
        ['btnSaveDelivery', 'btnCompleteDelivery', 'btnCancelOrder', 'btnBackToOrderList', 'btnSaveAdminMemo'].forEach((id) => {
            const button = document.getElementById(id);
            if (button) {
                button.disabled = disabled;
            }
        });
    },

    validateDeliveryPayload(company, tracking) {
        if (!company || !tracking) {
            return false;
        }
        if (company.length > 50) {
            return false;
        }
        if (!/^[A-Za-z0-9-]+$/.test(tracking) || tracking.length > 50) {
            return false;
        }
        return true;
    },

    normalizeAdminMemo(rawValue) {
        return String(rawValue || '').trim().replace(/\s+/g, ' ');
    },

    isValidOrderNo(orderNo) {
        return /^\d+$/.test(String(orderNo || '')) && Number(orderNo) > 0;
    },

    normalizeOrderNo(orderNo) {
        return this.isValidOrderNo(orderNo) ? String(Number(orderNo)) : null;
    },

    normalizeOrderStatusCode(statusCode) {
        return ['ORDERED', 'PAID', 'PREPARING', 'SHIPPED', 'DELIVERED', 'CANCELLED'].includes(statusCode)
            ? statusCode
            : 'ORDERED';
    },

    isValidCurrentOrderAction() {
        return this.isValidOrderNo(this.orderNo) && this.currentDetail && this.isValidOrderNo(this.currentDetail.orderNo || this.orderNo);
    },

    isValidOrderActionPayload(payload) {
        if (!payload || !this.isValidOrderNo(payload.orderNo)) {
            return false;
        }
        if (payload.reason != null && String(payload.reason).length > 200) {
            return false;
        }
        if ('adminMemo' in payload && String(payload.adminMemo || '').length > 500) {
            return false;
        }
        if ('deliveryCompany' in payload || 'trackingNum' in payload) {
            return this.validateDeliveryPayload(payload.deliveryCompany, payload.trackingNum);
        }
        return true;
    }
};

document.addEventListener('DOMContentLoaded', () => OrderDetail.init());
