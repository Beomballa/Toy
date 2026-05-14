const OrderDetail = {
    init() {
        const params = new URLSearchParams(window.location.search);
        this.orderNo = params.get('no');
        this.returnTo = params.get('returnTo') || '/admin/orders/list';
        this.isSubmitting = false;
        this.operationPolicy = null;
        if (!this.orderNo) {
            CommonJS.alert('잘못된 접근입니다.', '오류', 'error').then(() => {
                location.href = this.returnTo;
            });
            return;
        }

        this.syncReturnLinks();
        this.bindEvents();
        this.applyOperationPolicy();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));
        this.getDetail();
    },

    bindEvents() {
        document.getElementById('deliveryForm')?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.saveDelivery();
        });
        document.getElementById('btnCompleteDelivery')?.addEventListener('click', () => this.completeDelivery());
        document.getElementById('btnCancelOrder')?.addEventListener('click', () => this.cancelOrder());
        document.getElementById('btnSaveAdminMemo')?.addEventListener('click', () => this.saveAdminMemo());
    },

    async applyOperationPolicy(settings = null) {
        try {
            this.operationPolicy = settings || await CommonJS.fetchSystemSettings();
            const disabled = CommonJS.isAdminWriteBlocked(this.operationPolicy);
            const reason = '유지보수 모드에서는 주문 처리와 관리 메모 저장이 불가능합니다.';

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
            CommonJS.alert(err.message || '데이터를 불러오는 중 오류가 발생했습니다.', '오류', 'error');
        }
    },

    renderDetail(data) {
        this.renderSummary(data);
        this.renderActionVisibility(data);
        this.renderDeliveryInfo(data);
        this.renderOrderItems(data.items);
        this.renderAdminMemo(data.adminMemo);
        this.renderOrderHistory(data.histories || []);
    },

    renderSummary(data) {
        document.getElementById('orderNumDisplay').textContent = data.orderNum;
        document.getElementById('buyerName').textContent = data.buyerName;
        document.getElementById('buyerPhone').textContent = data.buyerPhone;
        document.getElementById('orderDt').textContent = data.orderDt;
        document.getElementById('orderDtMeta').textContent = `주문일시 ${data.orderDt || '-'}`;
        document.getElementById('totalAmount').textContent = data.totalAmount;
        document.getElementById('itemCount').textContent = data.items.length;

        const statusMeta = CommonJS.getOrderStatusMeta(data.statusCode);
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

        tbody.innerHTML = items.map(item => `
            <tr class="item-row">
                <td class="ps-4">
                    <div style="width:64px; height:64px;">
                        <img src="${item.thumbnailUrl || ''}" 
                             alt="${item.productName}" class="product-img"
                             onerror="CommonJS.handleImageError(this)">
                    </div>
                </td>
                <td>
                    <div class="fw-bold text-dark">${item.productName}</div>
                    <div class="text-muted small">상품번호: ${item.productNo}</div>
                </td>
                <td class="text-center fw-medium">${item.count}개</td>
                <td class="text-end pe-4 fw-bold text-primary">${item.orderPrice}</td>
            </tr>
        `).join('');
    },

    renderOrderHistory(histories) {
        const container = document.getElementById('orderHistoryList');
        if (!container) {
            return;
        }

        if (!histories.length) {
            container.innerHTML = '<div class="text-muted">등록된 주문 처리 이력이 없습니다.</div>';
            return;
        }

        container.innerHTML = histories.map((history) => `
            <div class="border rounded-3 p-3">
                <div class="d-flex justify-content-between align-items-start gap-3 mb-2">
                    <div class="fw-semibold text-dark">${history.actionType}</div>
                    <div class="text-muted small">${history.crtDtm || '-'}</div>
                </div>
                <div class="small text-muted mb-2">
                    상태: ${history.beforeStatusDesc || '-'} -> ${history.afterStatusDesc || '-'}
                </div>
                ${history.reason ? `<div class="small mb-1"><span class="text-muted">사유</span> ${CommonJS.escapeHtml(history.reason)}</div>` : ''}
                ${history.adminMemoSnapshot ? `<div class="small mb-1"><span class="text-muted">메모</span> ${CommonJS.escapeHtml(history.adminMemoSnapshot)}</div>` : ''}
                ${(history.deliveryCompany || history.trackingNum) ? `<div class="small"><span class="text-muted">배송</span> ${CommonJS.escapeHtml(history.deliveryCompany || '-')} / ${CommonJS.escapeHtml(history.trackingNum || '-')}</div>` : ''}
            </div>
        `).join('');
    },

    async completeDelivery() {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 배송 완료 처리가 불가능합니다.', '알림', 'warning');
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

    async cancelOrder() {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 주문 취소가 불가능합니다.', '알림', 'warning');
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
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 배송 정보 저장이 불가능합니다.', '알림', 'warning');
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

        companyInput.value = company;
        trackingInput.value = tracking;

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
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 관리 메모 저장이 불가능합니다.', '알림', 'warning');
            return;
        }

        const adminMemo = (document.getElementById('adminMemo')?.value || '').trim();
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

            await CommonJS.alert(successMessage, '성공', 'success');
            await this.getDetail();
        } catch (err) {
            console.error(`${logLabel} 실패:`, err);
            await CommonJS.alert(err.message || fallbackErrorMessage, '오류', 'error');
        } finally {
            this.isSubmitting = false;
            this.setActionButtonsDisabled(false);
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
    }
};

document.addEventListener('DOMContentLoaded', () => OrderDetail.init());
