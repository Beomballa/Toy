const DashBoardListJS = {
    initialized: false,
    init() {
        if (this.initialized) return;
        this.initialized = true;
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
            this.renderOperationNotices(data.operationNotices);
            this.renderOperationTasks(data.operationTasks);
            this.renderTaskWorkloadSummary(data.taskWorkloadSummary);
            this.renderTaskWorkloads(data.taskWorkloads);
            this.renderRecentOrders(data.recentOrders);
            this.renderLowStockProducts(data.lowStockProducts);
            this.renderSalesChart(data.salesChart);
            this.renderTopProductsChart(data.topProducts);
            this.renderTopBrandsChart(data.topBrands);
        } catch (err) {
            console.error('대시보드 데이터 로드 실패:', err);
        }
    },

    renderOperationNotices(notices) {
        const body = document.getElementById('operationNoticeBody');
        if (!body) return;

        if (!notices || notices.length === 0) {
            body.innerHTML = '<div class="text-muted small">현재 노출중인 운영 공지가 없습니다.</div>';
            return;
        }

        body.innerHTML = notices.map((notice) => `
            <div class="border rounded-3 p-3 ${notice.pinned ? 'mb-3 bg-light' : 'mb-3'}">
                <div class="d-flex justify-content-between align-items-start gap-3">
                    <div>
                        <div class="d-flex align-items-center gap-2 mb-1">
                            ${notice.pinned ? '<span class="badge text-bg-danger">고정</span>' : ''}
                            <a class="fw-bold text-decoration-none" href="${notice.targetPath}">${this.escapeHtml(notice.title)}</a>
                        </div>
                        <div class="text-muted small mb-2">${notice.periodLabel}</div>
                        <div class="small text-dark">${this.escapeHtml(notice.content).replace(/\n/g, '<br>')}</div>
                    </div>
                    <div class="d-flex flex-column gap-2">
                        <a class="btn btn-sm btn-outline-secondary" href="${notice.targetPath}">관리</a>
                        <a class="btn btn-sm btn-outline-secondary" href="${notice.historyPath}">이력</a>
                    </div>
                </div>
            </div>
        `).join('');
    },

    renderOperationTasks(tasks) {
        const body = document.getElementById('operationTaskBody');
        if (!body) return;

        if (!tasks || tasks.length === 0) {
            body.innerHTML = '<div class="text-muted small">현재 관리가 필요한 운영 작업이 없습니다.</div>';
            return;
        }

        body.innerHTML = tasks.map((task) => `
            <div class="border rounded-3 p-3 ${task.pinned ? 'mb-3 bg-light' : 'mb-3'}">
                <div class="d-flex justify-content-between align-items-start gap-3">
                    <div>
                        <div class="d-flex align-items-center gap-2 mb-1">
                            ${task.pinned ? '<span class="badge text-bg-danger">고정</span>' : ''}
                            <a class="fw-bold text-decoration-none" href="${task.targetPath}">${this.escapeHtml(task.title)}</a>
                        </div>
                        <div class="text-muted small mb-2">${task.statusLabel} · ${task.priorityLabel} · 담당자 ${this.escapeHtml(task.assigneeName)}</div>
                        <div class="small text-dark">${this.escapeHtml(task.dueDateLabel)}</div>
                    </div>
                    <div class="d-flex flex-column gap-2">
                        <a class="btn btn-sm btn-outline-secondary" href="${task.targetPath}">관리</a>
                        <a class="btn btn-sm btn-outline-secondary" href="${task.historyPath}">이력</a>
                        <a class="btn btn-sm btn-outline-secondary" href="${task.activityLogPath}">활동 로그</a>
                    </div>
                </div>
            </div>
        `).join('');
    },

    renderTaskWorkloads(items) {
        const body = document.getElementById('taskWorkloadBody');
        if (!body) return;

        if (!items || items.length === 0) {
            body.innerHTML = '<div class="text-muted small">담당자별 워크로드 데이터가 없습니다.</div>';
            return;
        }

        body.innerHTML = items.map((item) => `
            <div class="border rounded-3 p-3 mb-3">
                <div class="d-flex justify-content-between align-items-start gap-3">
                    <div class="flex-grow-1">
                        <div class="d-flex align-items-center gap-2 mb-2">
                            <a class="fw-bold text-decoration-none" href="${item.targetPath}">${this.escapeHtml(item.assigneeName)}</a>
                            <span class="badge text-bg-light">전체 ${item.totalCount.toLocaleString()}</span>
                            ${item.overdueCount > 0 ? `<span class="badge text-bg-danger">기한 초과 ${item.overdueCount.toLocaleString()}</span>` : ''}
                        </div>
                        <div class="small text-muted">
                            대기 ${item.todoCount.toLocaleString()} · 진행중 ${item.inProgressCount.toLocaleString()}
                        </div>
                    </div>
                    <div class="d-flex flex-column gap-2">
                        <a class="btn btn-sm btn-outline-secondary" href="${item.targetPath}">담당 작업</a>
                        <a class="btn btn-sm btn-outline-secondary" href="${item.overduePath}">기한 초과</a>
                    </div>
                </div>
            </div>
        `).join('');
    },

    renderTaskWorkloadSummary(summary) {
        const body = document.getElementById('taskWorkloadSummaryBody');
        if (!body) return;

        if (!summary) {
            body.innerHTML = '<div class="text-muted small">워크로드 요약을 확인할 수 없습니다.</div>';
            return;
        }

        body.innerHTML = `
            <div class="row g-3">
                <div class="col-md-3">
                    <a class="text-decoration-none" href="${summary.workloadPath}">
                        <div class="border rounded-3 p-3 h-100">
                            <div class="text-muted small mb-1">담당자 수</div>
                            <div class="fw-bold fs-5 text-dark">${Number(summary.assigneeCount || 0).toLocaleString()}</div>
                        </div>
                    </a>
                </div>
                <div class="col-md-3">
                    <a class="text-decoration-none" href="${summary.workloadPath}">
                        <div class="border rounded-3 p-3 h-100">
                            <div class="text-muted small mb-1">배정 작업</div>
                            <div class="fw-bold fs-5 text-dark">${Number(summary.assignedTaskCount || 0).toLocaleString()}</div>
                        </div>
                    </a>
                </div>
                <div class="col-md-3">
                    <a class="text-decoration-none" href="${summary.workloadPath}?overdueOnly=Y">
                        <div class="border rounded-3 p-3 h-100">
                            <div class="text-muted small mb-1">기한 초과</div>
                            <div class="fw-bold fs-5 text-danger">${Number(summary.overdueTaskCount || 0).toLocaleString()}</div>
                        </div>
                    </a>
                </div>
                <div class="col-md-3">
                    <a class="text-decoration-none" href="${summary.unassignedPath}">
                        <div class="border rounded-3 p-3 h-100">
                            <div class="text-muted small mb-1">미지정 작업</div>
                            <div class="fw-bold fs-5 text-dark">${Number(summary.unassignedTaskCount || 0).toLocaleString()}</div>
                        </div>
                    </a>
                </div>
            </div>
        `;
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
        const returnTo = encodeURIComponent('/admin/dashboard');
        location.href = `/admin/products/get?no=${productNo}&returnTo=${returnTo}`;
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
    },

    escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }
};
