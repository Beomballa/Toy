const OrderDetail = {
    init() {
        this.orderNo = new URLSearchParams(window.location.search).get('no');
        if (!this.orderNo) {
            CommonJS.alert('잘못된 접근입니다.', '오류', 'error', () => {
                location.href = '/admin/orders/list';
            });
            return;
        }

        this.bindEvents();
        this.getDetail();
    },

    bindEvents() {
        document.getElementById('btnUpdateStatus')?.addEventListener('click', () => {
            this.updateStatus();
        });
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
        document.getElementById('totalAmount').textContent = data.totalAmount;
        document.getElementById('itemCount').textContent = data.items.length;

        // 상태 배지
        const badge = document.getElementById('orderStatusBadge');
        badge.textContent = data.statusDesc;
        badge.className = 'badge rounded-pill ' + this.getStatusClass(data.statusCode);

        // 상태 변경 셀렉트박스 초기값
        document.getElementById('updateStatusSelect').value = data.statusCode;

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

    async updateStatus() {
        const status = document.getElementById('updateStatusSelect').value;
        const confirm = await CommonJS.confirm('주문 상태를 변경하시겠습니까?');
        
        if (!confirm) return;

        try {
            const res = await fetch('/api/admin/orders/status', {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    orderNo: this.orderNo,
                    status: status
                })
            });

            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            CommonJS.alert('상태가 변경되었습니다.', '성공', 'success', () => {
                this.getDetail(); // 다시 불러오기
            });
        } catch (err) {
            console.error('상태 변경 실패:', err);
            CommonJS.alert('상태 변경 중 오류가 발생했습니다.', '오류', 'error');
        }
    }
};
