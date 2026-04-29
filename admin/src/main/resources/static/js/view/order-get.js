const OrderDetail = {
    init() {
        const params = new URLSearchParams(window.location.search);
        this.orderNo = params.get('no');
        this.returnTo = params.get('returnTo') || '/admin/orders/list';
        this.isSubmitting = false;
        if (!this.orderNo) {
            CommonJS.alert('잘못된 접근입니다.', '오류', 'error').then(() => {
                location.href = this.returnTo;
            });
            return;
        }

        this.syncReturnLinks();
        this.bindEvents();
        this.getDetail();
    },

    bindEvents() {
        document.getElementById('deliveryForm')?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.saveDelivery();
        });
        document.getElementById('btnSaveDelivery')?.addEventListener('click', () => this.saveDelivery());
        document.getElementById('btnCompleteDelivery')?.addEventListener('click', () => this.completeDelivery());
        document.getElementById('btnCancelOrder')?.addEventListener('click', () => this.cancelOrder());
    },

    async getDetail() {
        try {
            const res = await fetch(`/api/admin/orders/get?no=${this.orderNo}`);
            if (!res.ok) {
                throw new Error(await CommonJS.extractErrorMessage(res, '데이터를 불러오는 중 오류가 발생했습니다.'));
            }

            const data = await res.json();
            this.renderDetail(data);
        } catch (err) {
            console.error('주문 상세 로드 실패:', err);
            CommonJS.alert(err.message || '데이터를 불러오는 중 오류가 발생했습니다.', '오류', 'error');
        }
    },

    renderDetail(data) {
        // 마스터 정보
        document.getElementById('orderNumDisplay').textContent = data.orderNum;
        document.getElementById('buyerName').textContent = data.buyerName;
        document.getElementById('buyerPhone').textContent = data.buyerPhone;
        document.getElementById('orderDt').textContent = data.orderDt;
        document.getElementById('orderDtMeta').textContent = `주문일시 ${data.orderDt || '-'}`;
        document.getElementById('totalAmount').textContent = data.totalAmount;
        document.getElementById('itemCount').textContent = data.items.length;
        const statusMeta = CommonJS.getOrderStatusMeta(data.statusCode);

        // 상태 배지
        const badge = document.getElementById('orderStatusBadge');
        badge.textContent = data.statusDesc;
        badge.className = `badge rounded-pill ${statusMeta.badgeClass}`;

        // 버튼 노출 제어
        const btnCancel = document.getElementById('btnCancelOrder');
        const btnComplete = document.getElementById('btnCompleteDelivery');
        const inputCard = document.getElementById('deliveryInputCard');
        const infoCard = document.getElementById('deliveryInfoCard');

        btnCancel.style.display = data.canCancel ? 'block' : 'none';
        inputCard.style.display = data.showDeliveryInput ? 'block' : 'none';
        infoCard.style.display = data.showDeliveryInfo ? 'block' : 'none';
        btnComplete.style.display = data.canCompleteDelivery ? 'block' : 'none';

        if (data.showDeliveryInfo) {
            document.getElementById('displayCompany').innerText = data.deliveryCompany || '-';
            document.getElementById('displayTracking').innerText = data.trackingNum || '-';
        }

        // 아이템 목록
        const tbody = document.getElementById('orderItemsTableBody');
        if (data.items.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center py-4 text-muted">주문 상품 정보가 없습니다.</td></tr>';
            return;
        }

        tbody.innerHTML = data.items.map(item => `
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

    async completeDelivery() {
        const isConfirm = await CommonJS.confirm('배송 완료 처리를 하시겠습니까?');
        if (!isConfirm) return;

        await this.submitOrderAction({
            url: '/api/admin/orders/delivery-complete',
            payload: { orderNo: this.orderNo },
            successMessage: '배송 완료 처리가 되었습니다.',
            fallbackErrorMessage: '배송 완료 처리 중 오류가 발생했습니다.',
            logLabel: '배송 완료 처리'
        });
    },

    async cancelOrder() {
        const isConfirm = await CommonJS.confirm('주문을 취소하시겠습니까?');
        if (!isConfirm) return;

        await this.submitOrderAction({
            url: '/api/admin/orders/cancel',
            payload: { orderNo: this.orderNo },
            successMessage: '주문이 취소되었습니다.',
            fallbackErrorMessage: '주문 취소 중 오류가 발생했습니다.',
            logLabel: '주문 취소'
        });
    },

    async saveDelivery() {
        const company = document.getElementById('deliveryCompany').value;
        const tracking = document.getElementById('trackingNum').value;

        if (!company || !tracking) {
            await CommonJS.alert('택배사와 운송장 번호를 모두 입력하세요.', '알림', 'warning');
            return;
        }

        await this.submitOrderAction({
            url: '/api/admin/orders/delivery',
            payload: {
                orderNo: this.orderNo,
                deliveryCompany: company,
                trackingNum: tracking
            },
            successMessage: '배송 정보가 등록되었으며 상태가 배송중으로 변경되었습니다.',
            fallbackErrorMessage: '배송 정보 저장 중 오류가 발생했습니다.',
            logLabel: '배송 정보 저장'
        });
    },

    async submitOrderAction({ url, payload, successMessage, fallbackErrorMessage, logLabel }) {
        if (this.isSubmitting) {
            return;
        }

        this.isSubmitting = true;
        this.setActionButtonsDisabled(true);

        try {
            const res = await fetch(url, {
                method: 'POST',
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
        document.getElementById('orderListBreadcrumb')?.setAttribute('href', this.returnTo);
        document.getElementById('btnBackToOrderList')?.addEventListener('click', () => {
            location.href = this.returnTo;
        });
    },

    setActionButtonsDisabled(disabled) {
        // 상세 액션은 중복 요청이 그대로 상태 전이 중복으로 이어질 수 있어서 전송 중 버튼을 잠급니다.
        ['btnSaveDelivery', 'btnCompleteDelivery', 'btnCancelOrder', 'btnBackToOrderList'].forEach((id) => {
            const button = document.getElementById(id);
            if (button) {
                button.disabled = disabled;
            }
        });
    }
};

document.addEventListener('DOMContentLoaded', () => OrderDetail.init());
