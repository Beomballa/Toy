const DashBoardListJS = {
    init() {
        this.bindSummaryActions();
        this.getStats();
    },

    bindSummaryActions() {
        document.getElementById('summaryTodayOrders')?.addEventListener('click', () => {
            const today = this.formatDate(new Date());
            this.goToOrderList({ startDate: today, endDate: today });
        });

        document.getElementById('summaryTodaySales')?.addEventListener('click', () => {
            const today = this.formatDate(new Date());
            this.goToOrderList({ startDate: today, endDate: today });
        });

        document.getElementById('summaryPreparingOrders')?.addEventListener('click', () => {
            this.goToOrderList({ status: 'PREPARING' });
        });

        document.getElementById('summaryShippingOrders')?.addEventListener('click', () => {
            this.goToOrderList({ status: 'SHIPPED' });
        });

        document.getElementById('summaryCancelledOrders')?.addEventListener('click', () => {
            this.goToOrderList({ status: 'CANCELLED' });
        });
    },

    async getStats() {
        try {
            const res = await fetch('/api/admin/dashboard/stats');
            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            const data = await res.json();
            this.renderSummary(data.summary);
            this.renderRecentOrders(data.recentOrders);
            this.renderLowStockProducts(data.lowStockProducts);
            this.renderSalesChart(data.salesChart);
            this.renderTopProductsChart(data.topProducts);
            this.renderTopBrandsChart(data.topBrands);
        } catch (err) {
            console.error('대시보드 데이터 로드 실패:', err);
        }
    },

    renderSalesChart(chartData) {
        const ctx = document.getElementById('salesChart');
        if (!ctx || !chartData) return;

        new Chart(ctx, {
            type: 'line',
            data: {
                labels: chartData.map(d => d.label),
                datasets: [{
                    label: '매출액 (원)',
                    data: chartData.map(d => d.value),
                    borderColor: '#2563eb',
                    backgroundColor: 'rgba(37, 99, 235, 0.1)',
                    borderWidth: 3,
                    fill: true,
                    tension: 0.4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: value => value.toLocaleString() + '원'
                        }
                    }
                }
            }
        });
    },

    renderTopProductsChart(chartData) {
        const ctx = document.getElementById('topProductsChart');
        if (!ctx || !chartData) return;

        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: chartData.map(d => d.label),
                datasets: [{
                    label: '판매량',
                    data: chartData.map(d => d.value),
                    backgroundColor: 'rgba(59, 130, 246, 0.8)',
                    borderRadius: 6
                }]
            },
            options: {
                indexAxis: 'y', // 가로 막대 차트
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } }
            }
        });
    },

    renderTopBrandsChart(chartData) {
        const ctx = document.getElementById('topBrandsChart');
        if (!ctx || !chartData) return;

        new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: chartData.map(d => d.label),
                datasets: [{
                    data: chartData.map(d => d.value),
                    backgroundColor: [
                        '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'
                    ],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'right' }
                },
                cutout: '60%' // 도넛 두께
            }
        });
    },

    renderSummary(summary) {
        document.getElementById('todayOrderCount').innerText = summary.todayOrderCount.toLocaleString();
        document.getElementById('todayTotalAmount').innerText = summary.todayTotalAmount;
        document.getElementById('preparingCount').innerText = summary.preparingCount.toLocaleString();
        document.getElementById('shippingCount').innerText = summary.shippingCount.toLocaleString();
        document.getElementById('cancelledCount').innerText = summary.cancelledCount.toLocaleString();
    },

    renderRecentOrders(orders) {
        const tbody = document.getElementById('recentOrderTableBody');
        if (!orders || orders.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted">최근 주문 데이터가 없습니다.</td></tr>';
            return;
        }

        tbody.innerHTML = orders.map(order => `
            <tr>
                <td class="ps-4"><span class="order-id">${order.orderNum}</span></td>
                <td><span class="fw-bold text-dark">${order.buyerName}</span></td>
                <td><span class="fw-medium">${order.totalAmount}</span></td>
                <td><span class="badge ${CommonJS.getOrderStatusMeta(order.statusCode).badgeClass}">${order.statusDesc}</span></td>
                <td class="small text-muted">${order.orderDt}</td>
                <td class="text-end pe-4">
                    <button type="button" class="btn btn-sm btn-outline-secondary" onclick="DashBoardListJS.goToOrderDetail(${order.orderNo})">상세보기</button>
                </td>
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
                <div class="d-flex align-items-center gap-2">
                    <span class="badge low-stock-badge rounded-pill">${product.stockCnt}개</span>
                    <button type="button" class="btn btn-sm btn-outline-secondary" onclick="DashBoardListJS.goToProductDetail(${product.productNo})">상세보기</button>
                </div>
            </div>
        `).join('');
    },

    goToOrderDetail(orderNo) {
        const returnTo = encodeURIComponent('/admin/dashboard');
        location.href = `/admin/orders/get?no=${orderNo}&returnTo=${returnTo}`;
    },

    goToProductDetail(productNo) {
        location.href = `/admin/products/get?no=${productNo}`;
    },

    goToOrderList(filters = {}) {
        const params = new URLSearchParams({
            page: 0,
            size: 10
        });

        if (filters.status) params.set('status', filters.status);
        if (filters.startDate) params.set('startDate', filters.startDate);
        if (filters.endDate) params.set('endDate', filters.endDate);

        location.href = `/admin/orders/list?${params.toString()}`;
    },

    formatDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }
};
