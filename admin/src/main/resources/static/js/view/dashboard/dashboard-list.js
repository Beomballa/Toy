const DashBoardListJS = {
    init() {
        this.getStats();
    },

    async getStats() {
        try {
            const res = await fetch('/api/admin/dashboard/stats');
            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            const data = await res.json();
            this.renderSummary(data.summary);
            this.renderRecentOrders(data.recentOrders);
            this.renderLowStockProducts(data.lowStockProducts);
        } catch (err) {
            console.error('대시보드 데이터 로드 실패:', err);
        }
    },

    renderSummary(summary) {
        document.getElementById('todayOrderCount').innerText = summary.todayOrderCount.toLocaleString();
        document.getElementById('todayTotalAmount').innerText = summary.todayTotalAmount;
        document.getElementById('preparingCount').innerText = summary.preparingCount.toLocaleString();
        document.getElementById('cancelledCount').innerText = summary.cancelledCount.toLocaleString();
    },

    renderRecentOrders(orders) {
        const tbody = document.getElementById('recentOrderTableBody');
        if (!orders || orders.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center py-4 text-muted">최근 주문 데이터가 없습니다.</td></tr>';
            return;
        }

        tbody.innerHTML = orders.map(order => `
            <tr>
                <td class="ps-4"><span class="order-id">${order.orderNum}</span></td>
                <td><span class="fw-bold text-dark">${order.buyerName}</span></td>
                <td><span class="fw-medium">${order.totalAmount}</span></td>
                <td><span class="badge ${this.getStatusClass(order.statusDesc)}">${order.statusDesc}</span></td>
                <td class="text-end pe-4 small text-muted">${order.orderDt}</td>
            </tr>
        `).join('');
    },

    renderLowStockProducts(products) {
        const body = document.getElementById('lowStockListBody');
        if (!products || products.length === 0) {
            body.innerHTML = '<div class="p-4 text-center text-muted">재고 부족 상품이 없습니다.</div>';
            return;
        }

        body.innerHTML = products.map(product => `
            <div class="list-group-item d-flex justify-content-between align-items-center p-3 border-0 border-bottom">
                <div>
                    <div class="fw-bold small">${product.productName}</div>
                    <div class="text-muted" style="font-size: 0.75rem;">${product.brandName}</div>
                </div>
                <span class="badge low-stock-badge rounded-pill">${product.stockCnt}개</span>
            </div>
        `).join('');
    },

    getStatusClass(desc) {
        if (desc === '주문취소') return 'bg-danger-subtle text-danger';
        if (desc === '결제완료') return 'bg-success-subtle text-success';
        if (desc === '배송중') return 'bg-warning-subtle text-warning';
        return 'bg-primary-subtle text-primary';
    }
};
