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
            this.rewriteEntryLinksWithReturnTo();
        });
        this.readStateFromUrl();
        this.applySectionFocus();
        this.rewriteEntryLinksWithReturnTo();
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
            this.goToOrderDetail(this.normalizePositiveId(detailButton.dataset.orderNo));
        });
        document.getElementById('lowStockListBody')?.addEventListener('click', (event) => {
            const detailButton = event.target.closest('[data-role="dashboard-product-detail"]');
            if (!detailButton) {
                return;
            }
            this.markDashboardSection('low-stock', true);
            this.goToProductDetail(this.normalizePositiveId(detailButton.dataset.productNo));
        });
        document.getElementById('frontDisplaySection')?.addEventListener('click', () => {
            this.markDashboardSection('front-display', true);
        });
    },

    async getStats() {
        try {
            const res = await fetch('/api/admin/dashboard/stats');
            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            const data = await res.json();
            this.renderSummary(data.summary);
            this.renderFrontDisplaySnapshot(data.frontDisplaySnapshot);
            this.renderOperationNotices(data.operationNotices);
            this.renderOperationTasks(data.operationTasks);
            this.renderUnassignedTasks(data.unassignedTasks);
            this.renderTaskWorkloadSummary(data.taskWorkloadSummary);
            this.renderTaskWorkloads(data.taskWorkloads);
            this.renderContentReactionSnapshot(data.contentReactionSnapshot);
            this.renderRecentOrders(data.recentOrders);
            this.renderLowStockProducts(data.lowStockProducts);
            this.renderSalesChart(data.salesChart);
            this.renderTopProductsChart(data.topProducts);
            this.renderTopBrandsChart(data.topBrands);
        } catch (err) {
            console.error('대시보드 데이터 로드 실패:', err);
            this.renderSectionState('frontDisplaySummaryBody', 'error', '프론트 전시 요약을 불러오지 못했습니다.', '전시 현황 계산에 필요한 데이터를 다시 확인해주세요.');
            this.renderSectionState('frontDisplayActionBody', 'error', '전시 우선 조치 대상을 불러오지 못했습니다.', '상품 전시 관리 메뉴에서 직접 전시 상태를 확인해주세요.');
            this.renderSectionState('operationNoticeBody', 'error', '운영 공지를 불러오지 못했습니다.', '잠시 후 다시 시도하거나 운영 공지 메뉴에서 직접 확인해주세요.');
            this.renderSectionState('operationTaskBody', 'error', '운영 작업을 불러오지 못했습니다.', '지금 확인이 필요한 작업을 불러오지 못했습니다.');
            this.renderSectionState('unassignedTaskBody', 'error', '미지정 작업을 불러오지 못했습니다.', '담당자 배정이 필요한 작업 목록을 불러오지 못했습니다.');
            this.renderSectionState('taskWorkloadSummaryBody', 'error', '워크로드 요약을 불러오지 못했습니다.', '담당자별 배정 현황 요약을 다시 확인해주세요.');
            this.renderSectionState('taskWorkloadBody', 'error', '담당자별 작업 현황을 불러오지 못했습니다.', '워크로드 목록을 다시 불러오거나 운영 작업 메뉴에서 직접 확인해주세요.');
            this.renderSectionState('contentReactionSnapshotBody', 'error', '콘텐츠 반응 신호를 불러오지 못했습니다.', '콘텐츠 관리에서 반응 분석을 직접 확인해 주세요.');
            this.renderTableState('recentOrderTableBody', 6, 'error', '최근 주문 내역을 불러오지 못했습니다.', '주문 목록을 다시 불러오거나 주문 관리 메뉴에서 직접 확인해주세요.');
            this.renderListState('lowStockListBody', 'error', '저재고 상품을 불러오지 못했습니다.', '재고 상태를 다시 불러오거나 상품 관리에서 직접 확인해주세요.');
            this.setSectionStateMeta('operationTaskStateMeta', 'error', '운영 작업을 불러오지 못했습니다.', 0, { pinnedCount: 0 });
            this.setSectionStateMeta('unassignedTaskStateMeta', 'error', '미지정 작업을 불러오지 못했습니다.', 0, { pinnedCount: 0 });
            this.setSectionStateMeta('taskWorkloadSummaryStateMeta', 'error', '워크로드 요약을 불러오지 못했습니다.', 0);
            this.setSectionStateMeta('taskWorkloadStateMeta', 'error', '담당자별 작업 현황을 불러오지 못했습니다.', 0, { overdueRowCount: 0 });
        }
    },

    renderContentReactionSnapshot(snapshot) {
        const body = document.getElementById('contentReactionSnapshotBody');
        if (!body) return;
        if (!snapshot) {
            this.renderSectionState(
                'contentReactionSnapshotBody',
                'empty',
                '아직 집계된 콘텐츠 반응이 없습니다.',
                '프론트 콘텐츠에서 반응이 등록되면 운영 신호가 표시됩니다.'
            );
            return;
        }
        const qualityHealthy = snapshot.dataQualityStatus === 'HEALTHY';
        const action = snapshot.priorityAction;
        body.innerHTML = `
            <div class="dashboard-reaction-snapshot">
                <dl class="dashboard-reaction-snapshot__metrics">
                    <div><dt>최근 7일 반응</dt><dd>${this.formatNumber(snapshot.totalCount)}건</dd></div>
                    <div><dt>도움 비율</dt><dd>${this.formatNumber(snapshot.helpfulRate)}%</dd></div>
                    <div><dt>평가 콘텐츠</dt><dd>${this.formatNumber(snapshot.evaluatedContentCount)}건</dd></div>
                    <div>
                        <dt>데이터 상태</dt>
                        <dd class="${qualityHealthy ? 'is-healthy' : 'is-warning'}">
                            ${qualityHealthy ? '정상' : `정리 필요 ${this.formatNumber(snapshot.orphanCount)}건`}
                        </dd>
                    </div>
                </dl>
                <div class="dashboard-reaction-snapshot__action ${action ? 'has-action' : ''}">
                    ${action ? `
                        <div>
                            <span>우선 개선 콘텐츠</span>
                            <strong>${this.escapeHtml(action.title || '제목 없음')}</strong>
                            <p>${this.escapeHtml(action.boardType)} · 개선 필요 ${this.formatNumber(action.notHelpfulCount)}건 · 도움 비율 ${this.formatNumber(action.helpfulRate)}%</p>
                        </div>
                        <a class="btn btn-sm btn-dark" href="${this.escapeHtml(action.detailPath)}">상세 확인</a>
                    ` : `
                        <div>
                            <span>운영 판단</span>
                            <strong>즉시 개선이 필요한 콘텐츠가 없습니다.</strong>
                            <p>최근 7일 반응 기준으로 안정적인 상태입니다.</p>
                        </div>
                        <a class="btn btn-sm btn-outline-secondary" href="${this.escapeHtml(snapshot.analyticsPath)}">분석 보기</a>
                    `}
                </div>
            </div>
        `;
    },

    formatNumber(value) {
        const number = Number(value);
        return Number.isFinite(number) ? number.toLocaleString('ko-KR') : '0';
    },

    renderOperationNotices(notices) {
        const body = document.getElementById('operationNoticeBody');
        if (!body) return;

        if (!notices || notices.length === 0) {
            this.renderSectionState('operationNoticeBody', 'empty', '현재 노출중인 운영 공지가 없습니다.', '운영 공지를 등록하면 이 영역에서 빠르게 확인할 수 있습니다.');
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
            this.renderSectionState('operationTaskBody', 'empty', '현재 관리가 필요한 운영 작업이 없습니다.', '지금 시점에는 별도로 확인이 필요한 운영 작업이 없습니다.');
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
            this.renderSectionState('unassignedTaskBody', 'empty', '현재 미지정 작업이 없습니다.', '모든 운영 작업에 담당자가 지정되어 있습니다.');
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
            this.renderSectionState('taskWorkloadBody', 'empty', '담당자별 워크로드 데이터가 없습니다.', '운영 작업이 배정되면 담당자별 현황이 이 영역에 표시됩니다.');
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
            this.renderSectionState('taskWorkloadSummaryBody', 'error', '워크로드 요약을 확인할 수 없습니다.', '담당자별 배정 현황 계산에 필요한 데이터를 다시 확인해주세요.');
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

    renderFrontDisplaySnapshot(snapshot) {
        const summaryBody = document.getElementById('frontDisplaySummaryBody');
        const actionBody = document.getElementById('frontDisplayActionBody');
        if (!summaryBody || !actionBody) return;

        if (!snapshot || !snapshot.summary) {
            this.renderSectionState('frontDisplaySummaryBody', 'error', '프론트 전시 요약을 확인할 수 없습니다.', '전시 상태 요약 계산에 필요한 데이터를 다시 확인해주세요.');
            this.renderSectionState('frontDisplayActionBody', 'error', '전시 우선 조치 대상을 확인할 수 없습니다.', '전시 관리 화면에서 직접 상태를 점검해주세요.');
            return;
        }

        const summary = snapshot.summary;
        summaryBody.innerHTML = `
            <div class="row g-3">
                <div class="col-md-3 col-sm-6">
                    <a class="text-decoration-none" href="${this.buildProductFrontDisplayPath({}, 'dashboard-front-display-summary')}">
                        <div class="border rounded-3 p-3 h-100 dashboard-mini-stat-card">
                            <div class="text-muted small mb-1">전시 대상</div>
                            <div class="fw-bold fs-5 text-dark">${Number(summary.totalCount || 0).toLocaleString()}</div>
                            <div class="small text-muted mt-2">Featured ${Number(summary.featuredCount || 0).toLocaleString()}건</div>
                        </div>
                    </a>
                </div>
                <div class="col-md-3 col-sm-6">
                    <a class="text-decoration-none" href="${this.buildProductFrontDisplayPath({ configured: 'UNCONFIGURED' }, 'dashboard-front-display-unconfigured-summary')}">
                        <div class="border rounded-3 p-3 h-100 dashboard-mini-stat-card">
                            <div class="text-muted small mb-1">노출 설정</div>
                            <div class="fw-bold fs-5 text-dark">${Number(summary.configuredCount || 0).toLocaleString()}</div>
                            <div class="small text-danger mt-2">미설정 ${Number(summary.unconfiguredCount || 0).toLocaleString()}건</div>
                        </div>
                    </a>
                </div>
                <div class="col-md-3 col-sm-6">
                    <a class="text-decoration-none" href="${this.buildProductFrontDisplayPath({ contentStatus: 'INCOMPLETE' }, 'dashboard-front-display-content-summary')}">
                        <div class="border rounded-3 p-3 h-100 dashboard-mini-stat-card">
                            <div class="text-muted small mb-1">전시 문구</div>
                            <div class="fw-bold fs-5 text-dark">${Number(summary.readyContentCount || 0).toLocaleString()}</div>
                            <div class="small text-danger mt-2">보완 ${Number(summary.incompleteContentCount || 0).toLocaleString()}건</div>
                        </div>
                    </a>
                </div>
                <div class="col-md-3 col-sm-6">
                    <a class="text-decoration-none" href="${this.buildProductFrontDisplayPath({ lowStockOnly: 'true' }, 'dashboard-front-display-low-stock-summary')}">
                        <div class="border rounded-3 p-3 h-100 dashboard-mini-stat-card">
                            <div class="text-muted small mb-1">저재고 전시</div>
                            <div class="fw-bold fs-5 text-danger">${Number(summary.lowStockCount || 0).toLocaleString()}</div>
                            <div class="small text-muted mt-2">기준 ${Number(summary.lowStockThreshold || 0).toLocaleString()}개 미만</div>
                        </div>
                    </a>
                </div>
            </div>
        `;

        if (!snapshot.actionItems || snapshot.actionItems.length === 0) {
            this.renderSectionState('frontDisplayActionBody', 'empty', '즉시 보완이 필요한 전시 상품이 없습니다.', '현재 기준으로 노출 누락, 문구 보완, 저재고 이슈가 있는 전시 상품이 없습니다.');
            return;
        }

        actionBody.innerHTML = `
            <div class="d-flex justify-content-between align-items-center gap-3 mb-3">
                <div>
                    <div class="fw-bold text-dark">우선 조치 상품</div>
                    <div class="small text-muted">노출 미설정, 전시 문구 보완, 저재고 조건을 기준으로 바로 손봐야 할 상품만 추렸습니다.</div>
                </div>
                <a class="btn btn-sm btn-outline-secondary" href="${this.buildProductFrontDisplayPath({}, 'dashboard-front-display-action-list')}">전시 관리로 이동</a>
            </div>
            <div class="dashboard-action-list">
                ${snapshot.actionItems.map((item) => `
                    <div class="dashboard-action-item">
                        <div class="dashboard-action-item__body">
                            <div class="d-flex align-items-center gap-2 flex-wrap mb-1">
                                <a class="fw-bold text-decoration-none text-dark"
                                   href="${this.buildProductUpdatePath(item.productNo, 'dashboard-front-display-update')}">${this.escapeHtml(item.productName)}</a>
                                ${item.featured ? '<span class="badge text-bg-dark">Featured</span>' : ''}
                                <span class="badge text-bg-light">${this.escapeHtml(item.issueLabel)}</span>
                            </div>
                            <div class="small text-muted mb-1">${this.escapeHtml(item.brandName || '-')}</div>
                            <div class="small text-dark">${this.escapeHtml(item.issueDetail)}</div>
                        </div>
                        <div class="dashboard-action-item__actions">
                            <span class="small text-muted">${Number(item.totalStock || 0).toLocaleString()}개</span>
                            <a class="btn btn-sm btn-outline-primary" href="${this.buildProductUpdatePath(item.productNo, 'dashboard-front-display-update')}">수정</a>
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
    },

    renderSectionState(elementId, type, title, description) {
        const body = document.getElementById(elementId);
        if (!body) return;

        const iconClass = type === 'error' ? 'text-danger' : 'text-primary';
        body.innerHTML = `
            <div class="product-empty-state py-4">
                <div class="product-empty-state__icon ${iconClass}">
                    <i class="fa-solid ${type === 'error' ? 'fa-triangle-exclamation' : 'fa-inbox'}"></i>
                </div>
                <strong>${this.escapeHtml(title)}</strong>
                <p>${this.escapeHtml(description)}</p>
            </div>
        `;
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

    rewriteEntryLinksWithReturnTo() {
        document.querySelectorAll('[data-dashboard-section] a[href]').forEach((anchor) => {
            const nextHref = this.buildEntryPathWithReturnTo(anchor.href, anchor.dataset.entrySource || '');
            if (nextHref) {
                anchor.href = nextHref;
            }
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
        if (!this.isPositiveId(noticeNo)) {
            return this.buildNoticeListPath({}, source);
        }
        const returnTo = this.buildNoticeListPath({}, source);
        return `/admin/settings/notices/get?no=${noticeNo}&source=${source}&returnTo=${encodeURIComponent(returnTo)}`;
    },

    buildNoticeHistoryPath(noticeNo, source = 'dashboard-notice-history') {
        if (!this.isPositiveId(noticeNo)) {
            return this.buildNoticeListPath({}, source);
        }
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

    buildProductFrontDisplayPath(filters = {}, source = 'dashboard-front-display') {
        const params = new URLSearchParams();
        Object.entries(filters).forEach(([key, value]) => {
            if (value != null && value !== '') {
                params.set(key, String(value));
            }
        });
        params.set('source', source);
        params.set('returnTo', this.getReturnTo());
        return `/admin/products/front-display?${params.toString()}`;
    },

    buildProductUpdatePath(productNo, source = 'dashboard-product-update') {
        const normalizedProductNo = this.normalizePositiveId(productNo);
        if (!normalizedProductNo) {
            return this.buildProductFrontDisplayPath({}, source);
        }
        return `/admin/products/update?no=${normalizedProductNo}&source=${source}&returnTo=${encodeURIComponent(this.getReturnTo())}`;
    },

    buildTaskDetailPath(taskNo, source = 'dashboard-task-detail') {
        const normalizedTaskNo = this.normalizePositiveId(taskNo);
        if (!normalizedTaskNo) {
            return this.buildTaskListPath({}, source);
        }
        const returnTo = this.buildTaskListPath({}, source);
        return `/admin/settings/tasks/get?no=${normalizedTaskNo}&source=${source}&returnTo=${encodeURIComponent(returnTo)}`;
    },

    buildTaskHistoryPath(taskNo, source = 'dashboard-task-history') {
        if (!this.isPositiveId(taskNo)) {
            return this.buildTaskListPath({}, source);
        }
        const returnTo = this.buildTaskListPath({}, source);
        return `/admin/settings/tasks/history?taskNo=${taskNo}&source=${source}&returnTo=${encodeURIComponent(returnTo)}`;
    },

    buildTaskWorkloadDetailPath(adminNo, source = 'dashboard-workload-detail') {
        if (!this.isPositiveId(adminNo)) {
            return this.buildTaskWorkloadPath({}, source);
        }
        const returnTo = this.buildTaskWorkloadPath({}, source);
        return `/admin/settings/tasks/workloads/get?adminNo=${adminNo}&source=${source}&returnTo=${encodeURIComponent(returnTo)}`;
    },

    buildActivityLogPathFromBase(basePath, source = 'dashboard-activity-log') {
        if (!basePath) return '#';
        let targetUrl;
        try {
            targetUrl = new URL(basePath, window.location.origin);
        } catch (error) {
            console.error('활동 로그 경로 파싱 실패:', error);
            return '#';
        }
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
        let targetUrl;
        try {
            targetUrl = new URL(basePath, window.location.origin);
        } catch (error) {
            console.error('대시보드 진입 경로 파싱 실패:', error);
            return '';
        }
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
        if (!tbody) return;

        if (!orders || orders.length === 0) {
            this.renderTableState('recentOrderTableBody', 6, 'empty', '최근 주문 데이터가 없습니다.', '새로운 주문이 발생하면 이 영역에서 바로 확인할 수 있습니다.');
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
        if (!body) return;

        if (!products || products.length === 0) {
            this.renderListState('lowStockListBody', 'empty', '재고 부족 상품이 없습니다.', '현재 기준으로 긴급 재고 보충이 필요한 상품이 없습니다.');
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
        if (!this.isPositiveId(orderNo)) {
            void CommonJS.alert('주문 번호가 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        const returnTo = encodeURIComponent(this.getReturnTo());
        location.href = `/admin/orders/get?no=${orderNo}&source=dashboard-recent-order-detail&returnTo=${returnTo}`;
    },

    goToProductDetail(productNo) {
        if (!this.isPositiveId(productNo)) {
            void CommonJS.alert('상품 번호가 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        const returnTo = encodeURIComponent(this.getReturnTo());
        location.href = `/admin/products/get?no=${productNo}&source=dashboard-low-stock-detail&returnTo=${returnTo}`;
    },

    renderTableState(elementId, colSpan, type, title, description) {
        const tbody = document.getElementById(elementId);
        if (!tbody) return;

        tbody.innerHTML = `
            <tr>
                <td colspan="${colSpan}" class="py-4">
                    <div class="product-empty-state py-2">
                        <div class="product-empty-state__icon ${type === 'error' ? 'text-danger' : 'text-primary'}">
                            <i class="fa-solid ${type === 'error' ? 'fa-triangle-exclamation' : 'fa-receipt'}"></i>
                        </div>
                        <strong>${this.escapeHtml(title)}</strong>
                        <p>${this.escapeHtml(description)}</p>
                    </div>
                </td>
            </tr>
        `;
    },

    renderListState(elementId, type, title, description) {
        const body = document.getElementById(elementId);
        if (!body) return;

        body.innerHTML = `
            <div class="product-empty-state py-4">
                <div class="product-empty-state__icon ${type === 'error' ? 'text-danger' : 'text-primary'}">
                    <i class="fa-solid ${type === 'error' ? 'fa-triangle-exclamation' : 'fa-box-open'}"></i>
                </div>
                <strong>${this.escapeHtml(title)}</strong>
                <p>${this.escapeHtml(description)}</p>
            </div>
        `;
    },

    goToOrderList(filters = {}, section = '') {
        this.markDashboardSection(section, true);
        const params = new URLSearchParams({
            page: 0,
            size: 10,
            source: section === 'summary' ? 'dashboard-summary' : 'dashboard',
            returnTo: this.getReturnTo()
        });

        const normalizedStatus = this.normalizeOrderStatus(filters.status);
        const normalizedStartDate = this.normalizeDateString(filters.startDate);
        const normalizedEndDate = this.normalizeDateString(filters.endDate);
        if (normalizedStatus) params.set('status', normalizedStatus);
        if (normalizedStartDate) params.set('startDate', normalizedStartDate);
        if (normalizedEndDate) params.set('endDate', normalizedEndDate);

        location.href = `/admin/orders/list?${params.toString()}`;
    },

    formatDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    },

    isPositiveId(value) {
        return Number.isFinite(Number(value)) && Number(value) > 0;
    },

    normalizePositiveId(value) {
        const parsed = Number(value);
        return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
    },

    normalizeOrderStatus(value) {
        return ['PREPARING', 'SHIPPED', 'CANCELLED'].includes(value) ? value : '';
    },

    normalizeDateString(value) {
        return /^\d{4}-\d{2}-\d{2}$/.test(String(value || '')) ? value : '';
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
