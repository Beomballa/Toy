const OrderDetail = {
    init() {
        this.orderNo = new URLSearchParams(window.location.search).get('no');
        if (!this.orderNo) {
            CommonJS.alert('잘못된 접근입니다.', '오류', 'error').then(() => {
                location.href = '/admin/orders/list';
            });
            return;
        }

        this.bindEvents();
        this.getDetail();
    },

    bindEvents() {
        document.getElementById('btnSaveDelivery')?.addEventListener('click', () => this.saveDelivery());
        document.getElementById('btnCompleteDelivery')?.addEventListener('click', () => this.completeDelivery());
        document.getElementById('btnCancelOrder')?.addEventListener('click', () => this.cancelOrder());
    },

    async getDetail() {
        try {
            const res = await fetch(`/api/admin/orders/get?no=${this.orderNo}`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            const data = await res.json();
            this.renderDetail(data);
        } catch (err) {
            console.error('주문 상세 로드 실패:', err);
            CommonJS.alert('데이터를 불러오는 중 오류가 발생했습니다.', '오류', 'error');
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

        // 상태 배지
        const badge = document.getElementById('orderStatusBadge');
        badge.textContent = data.statusDesc;
        badge.className = 'badge rounded-pill ' + this.getStatusClass(data.statusCode);

        // 버튼 노출 제어
        const btnCancel = document.getElementById('btnCancelOrder');
        const btnComplete = document.getElementById('btnCompleteDelivery');
        const inputCard = document.getElementById('deliveryInputCard');
        const infoCard = document.getElementById('deliveryInfoCard');

        // 취소 버튼: 배송 시작 전(ORDERED, PAID)일 때만 노출
        btnCancel.style.display = (data.statusCode === 'ORDERED' || data.statusCode === 'PAID') ? 'block' : 'none';

        if (data.statusCode === 'PAID') {
            inputCard.style.display = 'block';
            infoCard.style.display = 'none';
        } else if (data.statusCode === 'SHIPPED') {
            inputCard.style.display = 'none';
            infoCard.style.display = 'block';
            btnComplete.style.display = 'block'; // 배송 중일 때만 완료 버튼 노출
            document.getElementById('displayCompany').innerText = data.deliveryCompany || '-';
            document.getElementById('displayTracking').innerText = data.trackingNum || '-';
        } else if (data.statusCode === 'DELIVERED') {
            inputCard.style.display = 'none';
            infoCard.style.display = 'block';
            btnComplete.style.display = 'none';
            document.getElementById('displayCompany').innerText = data.deliveryCompany || '-';
            document.getElementById('displayTracking').innerText = data.trackingNum || '-';
        } else {
            inputCard.style.display = 'none';
            infoCard.style.display = 'none';
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

    getStatusClass(code) {
        switch(code) {
            case 'ORDERED': return 'badge-ordered';
            case 'PAID': return 'badge-paid';
            case 'PREPARING': return 'badge-preparing';
            case 'SHIPPED': return 'badge-shipped';
            case 'DELIVERED': return 'badge-delivered';
            case 'CANCELLED': return 'badge-cancelled';
            default: return 'bg-secondary';
        }
    },

    async completeDelivery() {
        const isConfirm = await CommonJS.confirm('배송 완료 처리를 하시겠습니까?');
        if (!isConfirm) return;

        try {
            const res = await fetch('/api/admin/orders/delivery-complete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ orderNo: this.orderNo })
            });

            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            await CommonJS.alert('배송 완료 처리가 되었습니다.', '성공', 'success');
            this.getDetail();
        } catch (err) {
            console.error('배송 완료 처리 실패:', err);
            await CommonJS.alert('배송 완료 처리 중 오류가 발생했습니다.', '오류', 'error');
        }
    },

    async cancelOrder() {
        const isConfirm = await CommonJS.confirm('주문을 취소하시겠습니까?');
        if (!isConfirm) return;

        try {
            const res = await fetch('/api/admin/orders/cancel', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ orderNo: this.orderNo })
            });

            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            await CommonJS.alert('주문이 취소되었습니다.', '성공', 'success');
            this.getDetail();
        } catch (err) {
            console.error('주문 취소 실패:', err);
            await CommonJS.alert('주문 취소 중 오류가 발생했습니다.', '오류', 'error');
        }
    },

    async saveDelivery() {
        const company = document.getElementById('deliveryCompany').value;
        const tracking = document.getElementById('trackingNum').value;

        if (!company || !tracking) {
            await CommonJS.alert('택배사와 운송장 번호를 모두 입력하세요.', '알림', 'warning');
            return;
        }

        try {
            const res = await fetch('/api/admin/orders/delivery', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    orderNo: this.orderNo,
                    deliveryCompany: company,
                    trackingNum: tracking
                })
            });

            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            await CommonJS.alert('배송 정보가 등록되었으며 상태가 배송중으로 변경되었습니다.', '성공', 'success');
            this.getDetail();
        } catch (err) {
            console.error('배송 정보 저장 실패:', err);
            await CommonJS.alert('배송 정보 저장 중 오류가 발생했습니다.', '오류', 'error');
        }
    }
};

document.addEventListener('DOMContentLoaded', () => OrderDetail.init());
