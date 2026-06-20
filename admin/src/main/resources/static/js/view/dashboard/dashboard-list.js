const DashBoardListJS = {
    initialized: false,
    state: {
        section: ''
    },
    init() {
        if (this.initialized) return;
        this.initialized = true;
        CommonJS.bindMainLogoNavigation('/admin/dashboard');
        this.bindSummaryActions();
        this.bindOperationEntryActions();
        window.addEventListener('popstate', () => {
            this.readStateFromUrl();
            this.applySectionFocus();
        });
        this.readStateFromUrl();
        this.applySectionFocus();
        this.getStats();
    },

    bindSummaryActions() {
        document.getElementById('summaryTodayOrders')?.addEventListener('click', () => {
            const today = this.formatDate(new Date());
            this.goToOrderList({ startDate: today, endDate: today }, 'summary');
        });

        document.getElementById('summaryTodaySales')?.addEventListener('click', () => {
            const today = this.formatDate(new Date());
            this.goToOrderList({ startDate: today, endDate: today }, 'summary');
        });

        document.getElementById('summaryPreparingOrders')?.addEventListener('click', () => {
            this.goToOrderList({ status: 'PREPARING' }, 'summary');
        });

        document.getElementById('summaryShippingOrders')?.addEventListener('click', () => {
            this.goToOrderList({ status: 'SHIPPED' }, 'summary');
        });

        document.getElementById('summaryCancelledOrders')?.addEventListener('click', () => {
            this.goToOrderList({ status: 'CANCELLED' }, 'summary');
        });
    },

    bindOperationEntryActions() {
        document.querySelectorAll('[data-dashboard-section] a[href]').forEach((anchor) => {
            anchor.addEventListener('click', (event) => {
                const section = anchor.closest('[data-dashboard-section]')?.dataset.dashboardSection || '';
                this.markDashboardSection(section, true);
                const nextHref = this.buildEntryPathWithReturnTo(anchor.href, anchor.dataset.entrySource || '');
                if (nextHref) {
                    event.preventDefault();
                    location.href = nextHref;
                }
            });
        });
        document.getElementById('operationTaskSection')?.addEventListener('click', (event) => {
            const anchor = event.target.closest('a[href]');
            if (!anchor) return;
            this.markSectionEntry('operationTaskStateMeta', anchor.dataset.entrySource || 'dashboard-task', anchor.dataset.entryTarget || 'task', anchor.href);
        });
        document.getElementById('taskWorkloadSection')?.addEventListener('click', (event) => {
            const anchor = event.target.closest('a[href]');
            if (!anchor) return;
            const target = anchor.dataset.entryTarget || (anchor.href.includes('overdueOnly=Y') ? 'overdue-workload' : 'workload');
            this.markSectionEntry('taskWorkloadStateMeta', anchor.dataset.entrySource || 'dashboard-workload', target, anchor.href);
            this.markSectionEntry('taskWorkloadSummaryStateMeta', anchor.dataset.entrySource || 'dashboard-workload', target, anchor.href);
        });
        document.getElementById('unassignedTaskSection')?.addEventListener('click', (event) => {
            const anchor = event.target.closest('a[href]');
            if (!anchor) return;
            this.markSectionEntry('unassignedTaskStateMeta', anchor.dataset.entrySource || 'dashboard-unassigned', anchor.dataset.entryTarget || 'unassigned-task', anchor.href);
        });
        document.getElementById('recentOrderTableBody')?.addEventListener('click', (event) => {
            const detailButton = event.target.closest('[data-role="dashboard-order-detail"]');
            if (!detailButton) {
                return;
            }
            this.markDashboardSection('recent-order', true);
            this.goToOrderDetail(Number(detailButton.dataset.orderNo));
        });
        document.getElementById('lowStockListBody')?.addEventListener('click', (event) => {
            const detailButton = event.target.closest('[data-role="dashboard-product-detail"]');
            if (!detailButton) {
                return;
            }
            this.markDashboardSection('low-stock', true);
            this.goToProductDetail(Number(detailButton.dataset.productNo));
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
            this.renderUnassignedTasks(data.unassignedTasks);
            this.renderTaskWorkloadSummary(data.taskWorkloadSummary);
            this.renderTaskWorkloads(data.taskWorkloads);
            this.renderRecentOrders(data.recentOrders);
            this.renderLowStockProducts(data.lowStockProducts);
            this.renderSalesChart(data.salesChart);
            this.renderTopProductsChart(data.topProducts);
            this.renderTopBrandsChart(data.topBrands);
        } catch (err) {
            console.error('대시보드 데이터 로드 실패:', err);
            this.setSectionStateMeta('operationTaskStateMeta', 'error', '운영 작업을 불러오지 못했습니다.', 0, { pinnedCount: 0 });
            this.setSectionStateMeta('unassignedTaskStateMeta', 'error', '미지정 작업을 불러오지 못했습니다.', 0, { pinnedCount: 0 });
            this.setSectionStateMeta('taskWorkloadSummaryStateMeta', 'error', '워크로드 요약을 불러오지 못했습니다.', 0);
            this.setSectionStateMeta('taskWorkloadStateMeta', 'error', '담당자별 작업 현황을 불러오지 못했습니다.', 0, { overdueRowCount: 0 });
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
                            <a class="fw-bold text-decoration-none"
                               href="${this.buildNoticeDetailPath(notice.noticeNo, 'dashboard-notice-title')}"
                               data-entry-source="dashboard-notice-title"
                               data-entry-target="notice-detail">${this.escapeHtml(notice.title)}</a>
                        </div>
                        <div class="text-muted small mb-2">${notice.periodLabel}</div>
                        <div class="small text-dark">${this.escapeHtml(notice.content).replace(/\n/g, '<br>')}</div>
                    </div>
                    <div class="d-flex flex-column gap-2">
                        <a class="btn btn-sm btn-outline-secondary"
                           href="${this.buildNoticeDetailPath(notice.noticeNo, 'dashboard-notice-manage')}"
                           data-entry-source="dashboard-notice-manage"
                           data-entry-target="notice-detail">관리</a>
                        <a class="btn btn-sm btn-outline-secondary"
                           href="${this.buildNoticeHistoryPath(notice.noticeNo, 'dashboard-notice-history')}"
                           data-entry-source="dashboard-notice-history"
                           data-entry-target="notice-history">이력</a>
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
            this.setSectionStateMeta('operationTaskStateMeta', 'empty', '현재 관리가 필요한 운영 작업이 없습니다.', 0, { pinnedCount: 0 });
            return;
        }

        body.innerHTML = tasks.map((task) => `
            <div class="border rounded-3 p-3 ${task.pinned ? 'mb-3 bg-light' : 'mb-3'}">
                <div class="d-flex justify-content-between align-items-start gap-3">
                    <div>
                        <div class="d-flex align-items-center gap-2 mb-1">
                            ${task.pinned ? '<span class="badge text-bg-danger">고정</span>' : ''}
                            <a class="fw-bold text-decoration-none"
                               href="${this.buildTaskDetailPath(task.taskNo, 'dashboard-task-title')}"
                               data-entry-source="dashboard-task-title"
                               data-entry-target="task-detail">${this.escapeHtml(task.title)}</a>
                        </div>
                        <div class="text-muted small mb-2">${task.statusLabel} · ${task.priorityLabel} · 담당자 ${this.escapeHtml(task.assigneeName)}</div>
                        <div class="small text-dark">${this.escapeHtml(task.dueDateLabel)}</div>
                    </div>
                    <div class="d-flex flex-column gap-2">
                        <a class="btn btn-sm btn-outline-secondary"
                           href="${this.buildTaskDetailPath(task.taskNo, 'dashboard-task-manage')}"
                           data-entry-source="dashboard-task-manage"
                           data-entry-target="task-detail">관리</a>
                        <a class="btn btn-sm btn-outline-secondary"
                           href="${this.buildTaskHistoryPath(task.taskNo, 'dashboard-task-history')}"
                           data-entry-source="dashboard-task-history"
                           data-entry-target="task-history">이력</a>
                        <a class="btn btn-sm btn-outline-secondary"
                           href="${this.buildActivityLogPathFromBase(task.activityLogPath, 'dashboard-task-activity-log')}"
                           data-entry-source="dashboard-task-activity-log"
                           data-entry-target="activity-log">활동 로그</a>
                    </div>
                </div>
            </div>
        `).join('');
        this.setSectionStateMeta(
            'operationTaskStateMeta',
            'ready',
            '',
            tasks.length,
            { pinnedCount: tasks.filter((task) => !!task.pinned).length }
        );
    },

    renderUnassignedTasks(tasks) {
        const body = document.getElementById('unassignedTaskBody');
        if (!body) return;

        if (!tasks || tasks.length === 0) {
            body.innerHTML = '<div class="text-muted small">현재 미지정 작업이 없습니다.</div>';
            this.setSectionStateMeta('unassignedTaskStateMeta', 'empty', '현재 미지정 작업이 없습니다.', 0, { pinnedCount: 0 });
            return;
        }

        body.innerHTML = tasks.map((task) => `
            <div class="border rounded-3 p-3 ${task.pinned ? 'mb-3 bg-light' : 'mb-3'}">
                <div class="d-flex justify-content-between align-items-start gap-3">
                    <div>
                        <div class="d-flex align-items-center gap-2 mb-1">
                            ${task.pinned ? '<span class="badge text-bg-danger">고정</span>' : ''}
                            <a class="fw-bold text-decoration-none"
                               href="${this.buildTaskDetailPath(task.taskNo, 'dashboard-unassigned-title')}"
                               data-entry-source="dashboard-unassigned-title"
                               data-entry-target="task-detail">${this.escapeHtml(task.title)}</a>
                        </div>
                        <div class="text-muted small mb-2">${task.statusLabel} · ${task.priorityLabel} · 담당자 미지정</div>
                        <div class="small text-dark">${this.escapeHtml(task.dueDateLabel)}</div>
                        ${task.latestCommentContent ? `
                            <div class="small text-muted mt-2">
                                최근 메모 · ${this.escapeHtml(task.latestCommentAdminName || '관리자')} · ${this.escapeHtml(task.latestCommentDtm || '-')}
                            </div>
                            <div class="small text-dark mt-1">${this.escapeHtml(task.latestCommentContent)}</div>
                        ` : `
                            <div class="small text-muted mt-2">최근 메모가 없습니다.</div>
                        `}
                    </div>
                    <div class="d-flex flex-column gap-2">
                        <a class="btn btn-sm btn-outline-secondary"
                           href="${this.buildTaskDetailPath(task.taskNo, 'dashboard-unassigned-detail')}"
                           data-entry-source="dashboard-unassigned-detail"
                           data-entry-target="task-detail">상세</a>
                        <a class="btn btn-sm btn-outline-secondary"
                           href="${this.buildTaskHistoryPath(task.taskNo, 'dashboard-unassigned-history')}"
                           data-entry-source="dashboard-unassigned-history"
                           data-entry-target="task-history">이력</a>
                        <a class="btn btn-sm btn-outline-secondary"
                           href="${this.buildActivityLogPathFromBase(task.activityLogPath, 'dashboard-unassigned-activity-log')}"
                           data-entry-source="dashboard-unassigned-activity-log"
                           data-entry-target="activity-log">활동 로그</a>
                    </div>
                </div>
            </div>
        `).join('');
        this.setSectionStateMeta(
            'unassignedTaskStateMeta',
            'ready',
            '',
            tasks.length,
            { pinnedCount: tasks.filter((task) => !!task.pinned).length }
        );
    },

    renderTaskWorkloads(items) {
        const body = document.getElementById('taskWorkloadBody');
        if (!body) return;

        if (!items || items.length === 0) {
            body.innerHTML = '<div class="text-muted small">담당자별 워크로드 데이터가 없습니다.</div>';
            this.setSectionStateMeta('taskWorkloadStateMeta', 'empty', '담당자별 워크로드 데이터가 없습니다.', 0, { overdueRowCount: 0 });
            return;
        }

        body.innerHTML = items.map((item) => `
            <div class="border rounded-3 p-3 mb-3">
                <div class="d-flex justify-content-between align-items-start gap-3">
                    <div class="flex-grow-1">
                        <div class="d-flex align-items-center gap-2 mb-2">
                            <a class="fw-bold text-decoration-none"
                               href="${this.buildTaskWorkloadDetailPath(item.assigneeAdminNo, 'dashboard-workload-assignee')}"
                               data-entry-source="dashboard-workload-assignee"
                               data-entry-target="workload-detail">${this.escapeHtml(item.assigneeName)}</a>
                            <span class="badge text-bg-light">전체 ${item.totalCount.toLocaleString()}</span>
                            ${item.overdueCount > 0 ? `<span class="badge text-bg-danger">기한 초과 ${item.overdueCount.toLocaleString()}</span>` : ''}
                        </div>
                        <div class="small text-muted">
                            대기 ${item.todoCount.toLocaleString()} · 진행중 ${item.inProgressCount.toLocaleString()}
                        </div>
                    </div>
                    <div class="d-flex flex-column gap-2">
                        <a class="btn btn-sm btn-outline-secondary"
                           href="${this.buildTaskListPath({ assigneeAdminNo: item.assigneeAdminNo }, 'dashboard-workload-task-list')}"
                           data-entry-source="dashboard-workload-task-list"
                           data-entry-target="task-list">담당 작업</a>
                        <a class="btn btn-sm btn-outline-secondary"
                           href="${this.buildTaskListPath({ assigneeAdminNo: item.assigneeAdminNo, overdueOnly: 'Y' }, 'dashboard-workload-overdue')}"
                           data-entry-source="dashboard-workload-overdue"
                           data-entry-target="task-list">기한 초과</a>
                    </div>
                </div>
            </div>
        `).join('');
        this.setSectionStateMeta(
            'taskWorkloadStateMeta',
            'ready',
            '',
            items.length,
            { overdueRowCount: items.filter((item) => Number(item.overdueCount || 0) > 0).length }
        );
    },

    renderTaskWorkloadSummary(summary) {
        const body = document.getElementById('taskWorkloadSummaryBody');
        if (!body) return;

        if (!summary) {
            body.innerHTML = '<div class="text-muted small">워크로드 요약을 확인할 수 없습니다.</div>';
            this.setSectionStateMeta('taskWorkloadSummaryStateMeta', 'error', '워크로드 요약을 확인할 수 없습니다.', 0);
            return;
        }

        body.innerHTML = `
            <div class="row g-3">
                <div class="col-md-3">
                    <a class="text-decoration-none"
                       href="${this.buildTaskWorkloadPath({}, 'dashboard-workload-summary')}"
                       data-entry-source="dashboard-workload-summary"
                       data-entry-target="workload-list">
                        <div class="border rounded-3 p-3 h-100">
                            <div class="text-muted small mb-1">담당자 수</div>
                            <div class="fw-bold fs-5 text-dark">${Number(summary.assigneeCount || 0).toLocaleString()}</div>
                        </div>
                    </a>
                </div>
                <div class="col-md-3">
                    <a class="text-decoration-none"
                       href="${this.buildTaskWorkloadPath({}, 'dashboard-workload-assigned')}"
                       data-entry-source="dashboard-workload-assigned"
                       data-entry-target="workload-list">
                        <div class="border rounded-3 p-3 h-100">
                            <div class="text-muted small mb-1">배정 작업</div>
                            <div class="fw-bold fs-5 text-dark">${Number(summary.assignedTaskCount || 0).toLocaleString()}</div>
                        </div>
                    </a>
                </div>
                <div class="col-md-3">
                    <a class="text-decoration-none"
                       href="${this.buildTaskWorkloadPath({ overdueOnly: 'Y' }, 'dashboard-workload-overdue-summary')}"
                       data-entry-source="dashboard-workload-overdue-summary"
                       data-entry-target="workload-list">
                        <div class="border rounded-3 p-3 h-100">
                            <div class="text-muted small mb-1">기한 초과</div>
                            <div class="fw-bold fs-5 text-danger">${Number(summary.overdueTaskCount || 0).toLocaleString()}</div>
                        </div>
                    </a>
                </div>
                <div class="col-md-3">
                    <a class="text-decoration-none"
                       href="${this.buildTaskListPath({ unassignedOnly: 'Y' }, 'dashboard-workload-unassigned-summary')}"
                       data-entry-source="dashboard-workload-unassigned-summary"
                       data-entry-target="task-list">
                        <div class="border rounded-3 p-3 h-100">
                            <div class="text-muted small mb-1">미지정 작업</div>
                            <div class="fw-bold fs-5 text-dark">${Number(summary.unassignedTaskCount || 0).toLocaleString()}</div>
                        </div>
                    </a>
                </div>
            </div>
        `;
        this.setSectionStateMeta('taskWorkloadSummaryStateMeta', 'ready', '', 4);
    },

    setSectionStateMeta(id, state, message, visibleCount, extra = {}) {
        const metaEl = document.getElementById(id);
        if (!metaEl) return;
        metaEl.dataset.listState = state;
        metaEl.dataset.stateMessage = message || '';
        metaEl.dataset.visibleCount = String(visibleCount ?? 0);
        Object.entries(extra).forEach(([key, value]) => {
            metaEl.dataset[key] = String(value ?? '');
        });
    },

    markSectionEntry(id, source, target, path) {
        const metaEl = document.getElementById(id);
        if (!metaEl) return;
        metaEl.dataset.lastEntrySource = source || '';
        metaEl.dataset.lastEntryTarget = target || '';
        metaEl.dataset.lastEntryPath = path || '';
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        this.state.section = params.get('section') || '';
    },

    syncState(replace = false) {
        const params = new URLSearchParams(window.location.search);
        if (this.state.section) {
            params.set('section', this.state.section);
        } else {
            params.delete('section');
        }
        const nextQuery = params.toString();
        const nextUrl = nextQuery ? `${window.location.pathname}?${nextQuery}` : window.location.pathname;
        if (replace) {
            window.history.replaceState({ path: nextUrl }, '', nextUrl);
            return;
        }
        window.history.pushState({ path: nextUrl }, '', nextUrl);
    },

    markDashboardSection(section, replace = false) {
        this.state.section = section || '';
        this.syncState(replace);
        this.applySectionFocus();
    },

    applySectionFocus() {
        document.querySelectorAll('[data-dashboard-section]').forEach((sectionEl) => {
            sectionEl.classList.toggle('dashboard-section-focus', sectionEl.dataset.dashboardSection === this.state.section);
        });
    },

    getReturnTo() {
        const params = new URLSearchParams();
        if (this.state.section) {
            params.set('section', this.state.section);
        }
        return params.toString()
            ? `${window.location.pathname}?${params.toString()}`
            : window.location.pathname;
    },

    buildNoticeListPath(filters = {}, source = 'dashboard-notice-list') {
        const params = new URLSearchParams();
        Object.entries(filters).forEach(([key, value]) => {
            if (value != null && value !== '') {
                params.set(key, String(value));
            }
        });
        params.set('source', source);
        const query = params.toString();
        return `/admin/settings/notices${query ? `?${query}` : ''}`;
    },

    buildNoticeDetailPath(noticeNo, source = 'dashboard-notice-detail') {
        const returnTo = this.buildNoticeListPath({}, source);
        return `/admin/settings/notices/get?no=${noticeNo}&source=${source}&returnTo=${encodeURIComponent(returnTo)}`;
    },

    buildNoticeHistoryPath(noticeNo, source = 'dashboard-notice-history') {
        const returnTo = this.buildNoticeListPath({}, source);
        return `/admin/settings/notices/history?noticeNo=${noticeNo}&source=${source}&returnTo=${encodeURIComponent(returnTo)}`;
    },

    buildTaskListPath(filters = {}, source = 'dashboard-task') {
        const params = new URLSearchParams();
        Object.entries(filters).forEach(([key, value]) => {
            if (value != null && value !== '') {
                params.set(key, String(value));
            }
        });
        params.set('source', source);
        const query = params.toString();
        return `/admin/settings/tasks${query ? `?${query}` : ''}`;
    },

    buildTaskWorkloadPath(filters = {}, source = 'dashboard-workload') {
        const params = new URLSearchParams();
        Object.entries(filters).forEach(([key, value]) => {
            if (value != null && value !== '') {
                params.set(key, String(value));
            }
        });
        params.set('source', source);
        return `/admin/settings/tasks/workloads?${params.toString()}`;
    },

    buildTaskDetailPath(taskNo, source = 'dashboard-task-detail') {
        return this.buildTaskListPath({
            taskNo,
            openTaskNo: taskNo,
            focusTaskNo: taskNo
        }, source);
    },

    buildTaskHistoryPath(taskNo, source = 'dashboard-task-history') {
        const returnTo = this.buildTaskListPath({}, source);
        return `/admin/settings/tasks/history?taskNo=${taskNo}&source=${source}&returnTo=${encodeURIComponent(returnTo)}`;
    },

    buildTaskWorkloadDetailPath(adminNo, source = 'dashboard-workload-detail') {
        const returnTo = this.buildTaskWorkloadPath({}, source);
        return `/admin/settings/tasks/workloads/get?adminNo=${adminNo}&source=${source}&returnTo=${encodeURIComponent(returnTo)}`;
    },

    buildActivityLogPathFromBase(basePath, source = 'dashboard-activity-log') {
        if (!basePath) return '#';
        const targetUrl = new URL(basePath, window.location.origin);
        targetUrl.searchParams.set('returnTo', this.getReturnTo());
        if (source) {
            targetUrl.searchParams.set('source', source);
        }
        return `${targetUrl.pathname}${targetUrl.search}`;
    },

    buildEntryPathWithReturnTo(basePath, source = '') {
        if (!basePath || basePath.startsWith('javascript:')) {
            return '';
        }
        const targetUrl = new URL(basePath, window.location.origin);
        if (source && !targetUrl.searchParams.get('source')) {
            targetUrl.searchParams.set('source', source);
        }
        targetUrl.searchParams.set('returnTo', this.getReturnTo());
        return `${targetUrl.pathname}${targetUrl.search}`;
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
                    <button type="button" class="btn btn-sm btn-outline-secondary" data-role="dashboard-order-detail" data-order-no="${order.orderNo}">상세보기</button>
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
                    <button type="button" class="btn btn-sm btn-outline-secondary" data-role="dashboard-product-detail" data-product-no="${product.productNo}">상세보기</button>
                </div>
            </div>
        `).join('');
    },

    goToOrderDetail(orderNo) {
        const returnTo = encodeURIComponent(this.getReturnTo());
        location.href = `/admin/orders/get?no=${orderNo}&source=dashboard-recent-order-detail&returnTo=${returnTo}`;
    },

    goToProductDetail(productNo) {
        const returnTo = encodeURIComponent(this.getReturnTo());
        location.href = `/admin/products/get?no=${productNo}&source=dashboard-low-stock-detail&returnTo=${returnTo}`;
    },

    goToOrderList(filters = {}, section = '') {
        this.markDashboardSection(section, true);
        const params = new URLSearchParams({
            page: 0,
            size: 10,
            source: section === 'summary' ? 'dashboard-summary' : 'dashboard',
            returnTo: this.getReturnTo()
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
