const TaskWorkloadDetail = {
    initialized: false,
    bootstrap: window.taskWorkloadDetailBootstrap || {},
    operationPolicy: null,
    reassignModal: null,
    reassignDetail: null,
    isCompletingTask: false,
    isRaisingPriority: false,
    isApplyingReassignment: false,

    init() {
        if (this.initialized) return;
        this.initialized = true;
        const modalEl = document.getElementById('taskReassignModal');
        if (modalEl) {
            this.reassignModal = new bootstrap.Modal(modalEl);
        }
        this.bindEvents();
        this.syncReturnLinks();
        this.syncReturnContextMeta();
        CommonJS.bindMainLogoNavigation(this.bootstrap.returnTo || '/admin/settings/tasks/workloads/get');
        CommonJS.renderSourceContextNotice({ noticeId: 'taskWorkloadDetailSourceContextNotice', source: this.bootstrap.source || '' });
        this.applyOperationPolicy();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));
        this.loadDetail();
    },

    bindEvents() {
        document.getElementById('workloadRecentTasksBody')?.addEventListener('click', (event) => {
            const completeButton = event.target.closest('[data-role="complete-task-from-context"]');
            if (completeButton) {
                this.completeTask(Number(completeButton.dataset.taskNo), '최근 작업');
                return;
            }
            const priorityButton = event.target.closest('[data-role="raise-priority-from-context"]');
            if (priorityButton) {
                this.raisePriority(Number(priorityButton.dataset.taskNo), '최근 작업');
                return;
            }
            const reassignButton = event.target.closest('[data-role="reassign-task-from-context"]');
            if (reassignButton) {
                this.openReassignModal(Number(reassignButton.dataset.taskNo), '최근 작업');
            }
        });
        document.getElementById('workloadOverdueTasksBody')?.addEventListener('click', (event) => {
            const completeButton = event.target.closest('[data-role="complete-overdue-task"]');
            if (completeButton) {
                this.completeTask(Number(completeButton.dataset.taskNo), '기한 초과 작업');
                return;
            }
            const priorityButton = event.target.closest('[data-role="raise-overdue-priority"]');
            if (priorityButton) {
                this.raisePriority(Number(priorityButton.dataset.taskNo), '기한 초과 작업');
                return;
            }
            const reassignButton = event.target.closest('[data-role="reassign-overdue-task"]');
            if (reassignButton) {
                this.openReassignModal(Number(reassignButton.dataset.taskNo), '기한 초과 작업');
            }
        });
        document.getElementById('workloadRecentCommentsBody')?.addEventListener('click', (event) => {
            const completeButton = event.target.closest('[data-role="complete-task-from-context"]');
            if (completeButton) {
                this.completeTask(Number(completeButton.dataset.taskNo), '최근 메모');
                return;
            }
            const priorityButton = event.target.closest('[data-role="raise-priority-from-context"]');
            if (priorityButton) {
                this.raisePriority(Number(priorityButton.dataset.taskNo), '최근 메모');
                return;
            }
            const reassignButton = event.target.closest('[data-role="reassign-task-from-context"]');
            if (reassignButton) {
                this.openReassignModal(Number(reassignButton.dataset.taskNo), '최근 메모');
            }
        });
        document.getElementById('workloadRecentHistoriesBody')?.addEventListener('click', (event) => {
            const completeButton = event.target.closest('[data-role="complete-task-from-context"]');
            if (completeButton) {
                this.completeTask(Number(completeButton.dataset.taskNo), '최근 활동');
                return;
            }
            const priorityButton = event.target.closest('[data-role="raise-priority-from-context"]');
            if (priorityButton) {
                this.raisePriority(Number(priorityButton.dataset.taskNo), '최근 활동');
                return;
            }
            const reassignButton = event.target.closest('[data-role="reassign-task-from-context"]');
            if (reassignButton) {
                this.openReassignModal(Number(reassignButton.dataset.taskNo), '최근 활동');
            }
        });
        document.getElementById('taskReassignRecommendationList')?.addEventListener('click', (event) => {
            const applyButton = event.target.closest('[data-role="apply-reassign-recommendation"]');
            if (!applyButton) return;
            const assigneeSelect = document.getElementById('taskReassignAssignee');
            if (assigneeSelect) {
                assigneeSelect.value = applyButton.dataset.adminNo || '';
            }
        });
        document.getElementById('btnTaskReassignApply')?.addEventListener('click', () => this.applyReassignment());
        document.getElementById('taskWorkloadActionNoticeClose')?.addEventListener('click', () => this.hideLastActionNotice(true));
    },

    async applyOperationPolicy(settings = null) {
        try {
            this.operationPolicy = settings || await CommonJS.fetchSystemSettings();
            this.syncOverdueActionState();
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
    },

    async loadDetail() {
        try {
            const adminNo = Number(this.bootstrap.adminNo || 0);
            if (!adminNo) {
                throw new Error('담당자 번호가 올바르지 않습니다.');
            }
            const params = new URLSearchParams();
            if (this.bootstrap.returnTo) {
                params.set('returnTo', this.bootstrap.returnTo);
            }
            const response = await fetch(`/api/admin/settings/tasks/workloads/${adminNo}${params.toString() ? `?${params.toString()}` : ''}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '담당자 워크로드 상세를 불러오지 못했습니다.'));
            }
            const data = await response.json();
            this.renderDetail(data);
        } catch (error) {
            document.getElementById('taskWorkloadDetailTitle').textContent = error.message;
            document.getElementById('workloadDetailMetaText').textContent = '상세 메타 확인 불가';
            document.getElementById('workloadRecentTasksBody').innerHTML = `<div class="text-danger small">${this.escapeHtml(error.message)}</div>`;
            document.getElementById('workloadOverdueTasksBody').innerHTML = '<div class="text-muted small">기한 초과 작업을 확인할 수 없습니다.</div>';
            document.getElementById('workloadRecentCommentsBody').innerHTML = '<div class="text-muted small">최근 메모를 확인할 수 없습니다.</div>';
            document.getElementById('workloadRecentHistoriesBody').innerHTML = '<div class="text-muted small">최근 활동을 확인할 수 없습니다.</div>';
            const metaEl = document.getElementById('taskWorkloadDetailStateMeta');
            if (metaEl) {
                metaEl.dataset.detailState = 'error';
                metaEl.dataset.stateMessage = error.message;
                metaEl.dataset.overdueCount = '0';
                metaEl.dataset.returnTo = this.bootstrap.returnTo || '/admin/settings/tasks/workloads';
                metaEl.dataset.returnContext = this.resolveReturnContext();
                metaEl.dataset.sourceContext = this.bootstrap.source || '';
                metaEl.dataset.lastActionTaskPath = '';
                metaEl.dataset.lastActionHistoryPath = '';
            }
            this.renderLastActionNotice();
        }
    },

    renderDetail(data) {
        document.getElementById('taskWorkloadDetailTitle').textContent = `${data.assigneeAdminName} 워크로드 상세`;
        document.getElementById('workloadDetailAssigneeName').textContent = `${data.assigneeAdminName} · 관리자 #${data.assigneeAdminNo}`;
        document.getElementById('workloadDetailMetaText').textContent = `최근 작업 ${Number(data.summary?.totalCount || 0).toLocaleString()}건 기준`;

        document.getElementById('workloadDetailTotalCount').textContent = Number(data.summary?.totalCount || 0).toLocaleString();
        document.getElementById('workloadDetailTodoCount').textContent = Number(data.summary?.todoCount || 0).toLocaleString();
        document.getElementById('workloadDetailInProgressCount').textContent = Number(data.summary?.inProgressCount || 0).toLocaleString();
        document.getElementById('workloadDetailOverdueCount').textContent = Number(data.summary?.overdueCount || 0).toLocaleString();

        document.getElementById('workloadDetailTaskListButton').href = this.buildContextualPath(data.targetPath) || '#';
        document.getElementById('workloadDetailTotalButton').href = this.buildContextualPath(data.targetPath) || '#';
        document.getElementById('workloadDetailTodoButton').href = this.buildContextualPath(data.todoPath) || '#';
        document.getElementById('workloadDetailInProgressButton').href = this.buildContextualPath(data.inProgressPath) || '#';
        document.getElementById('workloadDetailOverdueButton').href = this.buildContextualPath(data.overduePath) || '#';
        document.getElementById('workloadDetailOverdueSummaryButton').href = this.buildContextualPath(data.overduePath) || '#';
        document.getElementById('workloadDetailLogButton').href = this.buildLogPathFromBase(data.activityLogPath) || '#';

        this.renderRecentTasks(data.recentTasks || []);
        this.renderOverdueTasks(data.overdueTasks || []);
        this.renderRecentComments(data.recentComments || []);
        this.renderRecentHistories(data.recentHistories || []);
        const metaEl = document.getElementById('taskWorkloadDetailStateMeta');
        if (metaEl) {
            metaEl.dataset.detailState = 'ready';
            metaEl.dataset.stateMessage = '';
            metaEl.dataset.assigneeAdminNo = String(data.assigneeAdminNo || '');
            metaEl.dataset.overdueCount = String(data.summary?.overdueCount || 0);
            metaEl.dataset.returnTo = this.bootstrap.returnTo || '/admin/settings/tasks/workloads';
            metaEl.dataset.returnContext = this.resolveReturnContext();
            metaEl.dataset.sourceContext = this.bootstrap.source || '';
            if (!metaEl.dataset.lastAction) {
                metaEl.dataset.lastAction = '';
                metaEl.dataset.lastActionSource = '';
                metaEl.dataset.lastActionTaskNo = '';
                metaEl.dataset.lastActionStatus = '';
                metaEl.dataset.lastActionTaskPath = '';
                metaEl.dataset.lastActionHistoryPath = '';
            }
        }
        this.renderLastActionNotice();
    },

    syncReturnContextMeta() {
        const metaEl = document.getElementById('taskWorkloadDetailStateMeta');
        if (!metaEl) return;
        metaEl.dataset.returnTo = this.bootstrap.returnTo || '/admin/settings/tasks/workloads';
        metaEl.dataset.returnContext = this.resolveReturnContext();
        metaEl.dataset.sourceContext = this.bootstrap.source || '';
        CommonJS.renderSourceContextNotice({ noticeId: 'taskWorkloadDetailSourceContextNotice', source: this.bootstrap.source || '' });
    },

    syncReturnLinks() {
        const taskBreadcrumb = document.getElementById('taskWorkloadTaskBreadcrumbLink');
        const workloadBreadcrumb = document.getElementById('taskWorkloadListBreadcrumbLink');
        const returnButton = document.getElementById('taskWorkloadReturnButton');
        const returnTo = this.bootstrap.returnTo || '/admin/settings/tasks/workloads';
        const workloadListPath = this.buildCurrentListPath();
        const returnContext = CommonJS.getReturnContext(returnTo, '담당자별 워크로드');

        if (taskBreadcrumb) {
            taskBreadcrumb.href = returnTo;
            taskBreadcrumb.textContent = returnContext.label;
        }
        if (workloadBreadcrumb) {
            workloadBreadcrumb.href = workloadListPath;
        }
        if (returnButton) {
            returnButton.href = returnTo;
            returnButton.innerHTML = `<i class="fas fa-arrow-left me-2"></i>${returnContext.label}로 돌아가기`;
        }
    },

    resolveReturnContext() {
        const returnTo = this.bootstrap.returnTo || '';
        if (returnTo.includes('/tasks/get')) return 'task-detail';
        if (returnTo.includes('/tasks/history')) return 'task-history';
        if (returnTo.includes('/tasks/workloads')) return 'workload-list';
        if (returnTo.includes('/admin')) return 'dashboard-or-task';
        return 'unknown';
    },

    renderRecentTasks(items) {
        const body = document.getElementById('workloadRecentTasksBody');
        if (!body) return;
        if (!items.length) {
            body.innerHTML = '<div class="text-muted small">최근 작업이 없습니다.</div>';
            return;
        }
        body.innerHTML = items.map((item) => `
            <div class="border rounded-3 p-3 mb-3">
                <div class="fw-bold mb-1"><a class="text-decoration-none" href="${this.buildTaskDetailPath(item.taskNo)}">${this.escapeHtml(item.title)}</a></div>
                <div class="small text-muted">${this.escapeHtml(item.statusLabel)} · ${this.escapeHtml(item.priorityLabel)} · ${this.escapeHtml(item.dueState)}</div>
                <div class="mt-2 d-flex gap-2 flex-wrap">
                    <a class="btn btn-sm btn-outline-secondary" href="${this.buildTaskHistoryPath(item.taskNo)}">이력</a>
                    <button type="button" class="btn btn-sm btn-outline-warning" data-role="raise-priority-from-context" data-task-no="${item.taskNo}">우선순위 높음</button>
                    <button type="button" class="btn btn-sm btn-outline-success" data-role="complete-task-from-context" data-task-no="${item.taskNo}">완료 처리</button>
                    <button type="button" class="btn btn-sm btn-outline-dark" data-role="reassign-task-from-context" data-task-no="${item.taskNo}">재배정</button>
                </div>
            </div>
        `).join('');
        this.syncOverdueActionState();
    },

    renderOverdueTasks(items) {
        const body = document.getElementById('workloadOverdueTasksBody');
        if (!body) return;
        if (!items.length) {
            body.innerHTML = '<div class="text-muted small">기한 초과 작업이 없습니다.</div>';
            return;
        }
        body.innerHTML = items.map((item) => `
            <div class="border rounded-3 p-3 mb-3 bg-light-subtle">
                <div class="d-flex justify-content-between align-items-start gap-3">
                    <div>
                        <div class="fw-bold mb-1"><a class="text-decoration-none" href="${this.buildTaskDetailPath(item.taskNo)}">${this.escapeHtml(item.title)}</a></div>
                        <div class="small text-muted">${this.escapeHtml(item.statusLabel)} · ${this.escapeHtml(item.priorityLabel)} · ${this.escapeHtml(item.dueState)}</div>
                    </div>
                    <div class="d-flex flex-column gap-2">
                        <a class="btn btn-sm btn-outline-secondary" href="${this.buildTaskHistoryPath(item.taskNo)}">이력</a>
                        <button type="button" class="btn btn-sm btn-outline-warning" data-role="raise-overdue-priority" data-task-no="${item.taskNo}">우선순위 높음</button>
                        <button type="button" class="btn btn-sm btn-outline-success" data-role="complete-overdue-task" data-task-no="${item.taskNo}">완료 처리</button>
                        <button type="button" class="btn btn-sm btn-outline-dark" data-role="reassign-overdue-task" data-task-no="${item.taskNo}">재배정</button>
                    </div>
                </div>
            </div>
        `).join('');
        this.syncOverdueActionState();
    },

    renderRecentComments(items) {
        const body = document.getElementById('workloadRecentCommentsBody');
        if (!body) return;
        if (!items.length) {
            body.innerHTML = '<div class="text-muted small">최근 메모가 없습니다.</div>';
            return;
        }
        body.innerHTML = items.map((item) => `
            <div class="border rounded-3 p-3 mb-3">
                <div class="fw-bold mb-1"><a class="text-decoration-none" href="${this.buildTaskDetailPath(item.taskNo)}">${this.escapeHtml(item.taskTitle)}</a></div>
                <div class="small text-muted">${this.escapeHtml(item.adminName)} · ${this.escapeHtml(item.commentDtm)}</div>
                <div class="small text-dark mt-2">${this.escapeHtml(item.content)}</div>
                <div class="mt-2 d-flex gap-2 flex-wrap">
                    <button type="button" class="btn btn-sm btn-outline-warning" data-role="raise-priority-from-context" data-task-no="${item.taskNo}">우선순위 높음</button>
                    <button type="button" class="btn btn-sm btn-outline-success" data-role="complete-task-from-context" data-task-no="${item.taskNo}">완료 처리</button>
                    <button type="button" class="btn btn-sm btn-outline-dark" data-role="reassign-task-from-context" data-task-no="${item.taskNo}">이 작업 재배정</button>
                </div>
            </div>
        `).join('');
        this.syncOverdueActionState();
    },

    renderRecentHistories(items) {
        const body = document.getElementById('workloadRecentHistoriesBody');
        if (!body) return;
        if (!items.length) {
            body.innerHTML = '<div class="text-muted small">최근 활동이 없습니다.</div>';
            return;
        }
        body.innerHTML = items.map((item) => `
            <div class="border rounded-3 p-3 mb-3">
                <div class="fw-bold mb-1">${this.escapeHtml(item.actionLabel)}</div>
                <div class="small text-muted">${this.escapeHtml(item.adminName)} · ${this.escapeHtml(item.actionDtm)}</div>
                <div class="small text-dark mt-2">
                    ${item.taskNo ? `<a class="text-decoration-none" href="${this.buildTaskDetailPath(item.taskNo)}">${this.escapeHtml(item.taskLabel || '관련 작업 보기')}</a>` : this.escapeHtml(item.taskLabel || '-')}
                </div>
                ${item.taskNo ? `<div class="mt-2 d-flex gap-2 flex-wrap"><button type="button" class="btn btn-sm btn-outline-warning" data-role="raise-priority-from-context" data-task-no="${item.taskNo}">우선순위 높음</button><button type="button" class="btn btn-sm btn-outline-success" data-role="complete-task-from-context" data-task-no="${item.taskNo}">완료 처리</button><button type="button" class="btn btn-sm btn-outline-dark" data-role="reassign-task-from-context" data-task-no="${item.taskNo}">이 작업 재배정</button></div>` : ''}
            </div>
        `).join('');
        this.syncOverdueActionState();
    },

    syncOverdueActionState() {
        const disabled = CommonJS.isAdminWriteBlocked(this.operationPolicy || {});
        const reason = CommonJS.getAdminWriteBlockedReason('기한 초과 작업 직접 조치');
        document.querySelectorAll('[data-role="raise-overdue-priority"]').forEach((button) => {
            CommonJS.setButtonDisabled(button, disabled, reason);
        });
        document.querySelectorAll('[data-role="raise-priority-from-context"]').forEach((button) => {
            CommonJS.setButtonDisabled(button, disabled, reason);
        });
        document.querySelectorAll('[data-role="complete-overdue-task"]').forEach((button) => {
            CommonJS.setButtonDisabled(button, disabled, reason);
        });
        document.querySelectorAll('[data-role="complete-task-from-context"]').forEach((button) => {
            CommonJS.setButtonDisabled(button, disabled, reason);
        });
        document.querySelectorAll('[data-role="reassign-overdue-task"]').forEach((button) => {
            CommonJS.setButtonDisabled(button, disabled, reason);
        });
        document.querySelectorAll('[data-role="reassign-task-from-context"]').forEach((button) => {
            CommonJS.setButtonDisabled(button, disabled, reason);
        });
        CommonJS.setButtonDisabled(document.getElementById('btnTaskReassignApply'), disabled, reason);
    },

    setContextActionButtonsDisabled(disabled) {
        [
            '[data-role="raise-overdue-priority"]',
            '[data-role="raise-priority-from-context"]',
            '[data-role="complete-overdue-task"]',
            '[data-role="complete-task-from-context"]',
            '[data-role="reassign-overdue-task"]',
            '[data-role="reassign-task-from-context"]'
        ].forEach((selector) => {
            document.querySelectorAll(selector).forEach((button) => {
                button.disabled = disabled;
            });
        });
    },

    setReassignBusyState(isBusy) {
        const applyButton = document.getElementById('btnTaskReassignApply');
        if (!applyButton) return;
        if (isBusy) {
            if (!applyButton.dataset.originalText) {
                applyButton.dataset.originalText = applyButton.textContent;
            }
            applyButton.disabled = true;
            applyButton.textContent = '재배정 중...';
            return;
        }
        applyButton.disabled = false;
        if (applyButton.dataset.originalText) {
            applyButton.textContent = applyButton.dataset.originalText;
            delete applyButton.dataset.originalText;
        }
    },

    async completeTask(taskNo, sourceLabel = '워크로드 상세') {
        if (this.isCompletingTask) return;
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('기한 초과 작업 완료 처리'), '알림', 'warning');
            return;
        }
        const confirmed = await CommonJS.confirm('이 작업을 완료 처리하시겠습니까?', '완료 처리');
        if (!confirmed) return;

        try {
            this.isCompletingTask = true;
            this.setContextActionButtonsDisabled(true);
            const response = await fetch(`/api/admin/settings/tasks/status/${taskNo}?status=DONE`, {
                method: 'PATCH'
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '작업을 완료 처리하지 못했습니다.'));
            }
            this.setLastActionMeta('complete', taskNo, 'success', sourceLabel);
            await this.loadDetail();
            await CommonJS.alert('작업이 완료 처리되었습니다.', '성공', 'success');
        } catch (error) {
            this.setLastActionMeta('complete', taskNo, 'error', sourceLabel);
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isCompletingTask = false;
            this.setContextActionButtonsDisabled(false);
            this.syncOverdueActionState();
        }
    },

    async raisePriority(taskNo, sourceLabel = '워크로드 상세') {
        if (this.isRaisingPriority) return;
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('기한 초과 작업 우선순위 변경'), '알림', 'warning');
            return;
        }

        try {
            this.isRaisingPriority = true;
            this.setContextActionButtonsDisabled(true);
            const detail = await this.fetchTaskDetail(taskNo);
            if (detail.priority === 'HIGH') {
                await CommonJS.alert('이미 우선순위가 높음입니다.', '알림', 'info');
                return;
            }
            const confirmed = await CommonJS.confirm('이 작업의 우선순위를 높음으로 변경하시겠습니까?', '우선순위 변경');
            if (!confirmed) return;

            await this.saveTaskDetail({
                taskNo: detail.taskNo,
                title: detail.title,
                description: detail.description,
                status: detail.status,
                priority: 'HIGH',
                assigneeAdminNo: detail.assigneeAdminNo,
                dueDate: detail.dueDate || null,
                isPinned: detail.isPinned
            }, '작업 우선순위를 변경하지 못했습니다.');
            this.setLastActionMeta('raise-priority', taskNo, 'success', sourceLabel);
            await this.loadDetail();
            await CommonJS.alert('우선순위가 높음으로 변경되었습니다.', '성공', 'success');
        } catch (error) {
            this.setLastActionMeta('raise-priority', taskNo, 'error', sourceLabel);
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isRaisingPriority = false;
            this.setContextActionButtonsDisabled(false);
            this.syncOverdueActionState();
        }
    },

    async openReassignModal(taskNo, sourceLabel = '워크로드 상세') {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('기한 초과 작업 재배정'), '알림', 'warning');
            return;
        }
        this.reassignDetail = { sourceLabel };
        const metaEl = document.getElementById('taskReassignModalMeta');
        const listEl = document.getElementById('taskReassignRecommendationList');
        const selectEl = document.getElementById('taskReassignAssignee');
        if (metaEl) metaEl.textContent = '재배정 정보를 불러오는 중입니다...';
        if (listEl) listEl.innerHTML = '<div class="col-12"><div class="text-muted small">추천 담당자를 불러오는 중입니다...</div></div>';
        if (selectEl) selectEl.innerHTML = '<option value="">미지정</option>';
        this.reassignModal?.show();

        try {
            const data = await this.fetchTaskDetail(taskNo);
            this.reassignDetail = { ...data, sourceLabel };
            if (metaEl) {
                metaEl.textContent = `${data.title || '-'} · 현재 담당자 ${data.assigneeAdminName || '미지정'} · ${data.dueState || '-'}`;
            }
            this.renderReassignAssigneeOptions(data.assigneeOptions || [], data.assigneeAdminNo);
            this.renderReassignRecommendations(data.assignmentRecommendations || []);
            this.syncOverdueActionState();
        } catch (error) {
            if (metaEl) metaEl.textContent = error.message;
            if (listEl) listEl.innerHTML = `<div class="col-12"><div class="text-danger small">${this.escapeHtml(error.message)}</div></div>`;
        }
    },

    renderReassignAssigneeOptions(options, selectedAssigneeAdminNo) {
        const select = document.getElementById('taskReassignAssignee');
        if (!select) return;
        select.innerHTML = ['<option value="">미지정</option>']
            .concat(options.map((option) => `<option value="${option.adminNo}">${this.escapeHtml(option.name)}</option>`))
            .join('');
        select.value = selectedAssigneeAdminNo || '';
    },

    renderReassignRecommendations(items) {
        const listEl = document.getElementById('taskReassignRecommendationList');
        if (!listEl) return;
        if (!items.length) {
            listEl.innerHTML = '<div class="col-12"><div class="text-muted small">추천 가능한 담당자가 없습니다.</div></div>';
            return;
        }
        listEl.innerHTML = items.map((item) => `
            <div class="col-md-4">
                <div class="border rounded-3 p-3 h-100">
                    <div class="fw-bold mb-1">${this.escapeHtml(item.adminName)}</div>
                    <div class="small text-muted mb-2">${this.escapeHtml(item.reasonLabel)}</div>
                    <div class="small text-dark">전체 ${Number(item.totalCount || 0).toLocaleString()}건 · 진행중 ${Number(item.inProgressCount || 0).toLocaleString()}건 · 기한 초과 ${Number(item.overdueCount || 0).toLocaleString()}건</div>
                    <div class="mt-3">
                        <button type="button" class="btn btn-sm btn-outline-dark" data-role="apply-reassign-recommendation" data-admin-no="${item.adminNo}">이 담당자로 선택</button>
                    </div>
                </div>
            </div>
        `).join('');
    },

    async applyReassignment() {
        if (this.isApplyingReassignment) return;
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('기한 초과 작업 재배정'), '알림', 'warning');
            return;
        }
        if (!this.reassignDetail) {
            await CommonJS.alert('재배정할 작업 정보를 다시 불러와 주세요.', '알림', 'warning');
            return;
        }
        const assigneeAdminNo = this.parseOptionalNumber(document.getElementById('taskReassignAssignee')?.value);
        const confirmed = await CommonJS.confirm('선택한 담당자로 재배정하시겠습니까?', '재배정 확인');
        if (!confirmed) return;

        try {
            this.isApplyingReassignment = true;
            this.setReassignBusyState(true);
            await this.saveTaskDetail({
                taskNo: this.reassignDetail.taskNo,
                title: this.reassignDetail.title,
                description: this.reassignDetail.description,
                status: this.reassignDetail.status,
                priority: this.reassignDetail.priority,
                assigneeAdminNo,
                dueDate: this.reassignDetail.dueDate || null,
                isPinned: this.reassignDetail.isPinned
            }, '작업을 재배정하지 못했습니다.');
            this.setLastActionMeta('reassign', this.reassignDetail.taskNo, 'success', this.reassignDetail.sourceLabel || '워크로드 상세');
            this.reassignModal?.hide();
            await this.loadDetail();
            await CommonJS.alert('작업이 재배정되었습니다.', '성공', 'success');
        } catch (error) {
            this.setLastActionMeta('reassign', this.reassignDetail?.taskNo, 'error', this.reassignDetail?.sourceLabel || '워크로드 상세');
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isApplyingReassignment = false;
            this.setReassignBusyState(false);
            this.syncOverdueActionState();
        }
    },

    setLastActionMeta(action, taskNo, status, sourceLabel = '워크로드 상세') {
        const metaEl = document.getElementById('taskWorkloadDetailStateMeta');
        if (!metaEl) return;
        metaEl.dataset.lastAction = action || '';
        metaEl.dataset.lastActionSource = sourceLabel || '워크로드 상세';
        metaEl.dataset.lastActionTaskNo = taskNo == null ? '' : String(taskNo);
        metaEl.dataset.lastActionStatus = status || '';
        metaEl.dataset.lastActionTaskPath = taskNo == null ? '' : this.buildTaskDetailPath(taskNo);
        metaEl.dataset.lastActionHistoryPath = taskNo == null ? '' : this.buildTaskHistoryPath(taskNo);
        this.renderLastActionNotice();
    },

    renderLastActionNotice() {
        const metaEl = document.getElementById('taskWorkloadDetailStateMeta');
        const noticeEl = document.getElementById('taskWorkloadActionNotice');
        const noticeTextEl = document.getElementById('taskWorkloadActionNoticeText');
        const noticeActionsEl = document.getElementById('taskWorkloadActionNoticeActions');
        if (!metaEl || !noticeEl || !noticeTextEl || !noticeActionsEl) return;

        const action = metaEl.dataset.lastAction || '';
        const source = metaEl.dataset.lastActionSource || '';
        const taskNo = metaEl.dataset.lastActionTaskNo || '';
        const status = metaEl.dataset.lastActionStatus || '';
        const taskPath = metaEl.dataset.lastActionTaskPath || '';
        const historyPath = metaEl.dataset.lastActionHistoryPath || '';
        const taskLabel = taskNo ? `작업 #${taskNo}` : '작업';

        if (!action || !status) {
            this.hideLastActionNotice(false);
            return;
        }

        const templates = {
            'complete:success': `${taskLabel} 완료 처리를 반영했습니다.`,
            'complete:error': `${taskLabel} 완료 처리에 실패했습니다.`,
            'raise-priority:success': `${taskLabel} 우선순위를 높음으로 올렸습니다.`,
            'raise-priority:error': `${taskLabel} 우선순위 변경에 실패했습니다.`,
            'reassign:success': `${taskLabel} 담당자를 변경했습니다.`,
            'reassign:error': `${taskLabel} 재배정에 실패했습니다.`
        };
        const variants = {
            'complete:success': 'alert-success',
            'raise-priority:success': 'alert-warning',
            'reassign:success': 'alert-primary',
            'complete:error': 'alert-danger',
            'raise-priority:error': 'alert-danger',
            'reassign:error': 'alert-danger'
        };

        const message = templates[`${action}:${status}`] || `${taskLabel} 조치 결과를 확인해 주세요.`;
        const sourceMessage = source ? `${source}에서 실행` : '워크로드 상세에서 실행';
        const isSuccess = status === 'success';
        const variantClass = variants[`${action}:${status}`] || (isSuccess ? 'alert-success' : 'alert-danger');
        CommonJS.renderActionNotice({
            noticeId: 'taskWorkloadActionNotice',
            textId: 'taskWorkloadActionNoticeText',
            actionsId: 'taskWorkloadActionNoticeActions',
            action,
            status,
            variantClass,
            message: `${sourceMessage} · ${message}`,
            actionsHtml: [
            taskPath ? `<a class="btn btn-sm ${isSuccess ? 'btn-outline-success' : 'btn-outline-danger'}" href="${taskPath}">작업 상세</a>` : '',
            historyPath ? `<a class="btn btn-sm btn-outline-secondary" href="${historyPath}">이력</a>` : ''
            ].join('')
        });
        noticeEl.dataset.taskNo = taskNo;
    },

    hideLastActionNotice(clearMeta = false) {
        CommonJS.hideActionNotice({
            noticeId: 'taskWorkloadActionNotice',
            textId: 'taskWorkloadActionNoticeText',
            actionsId: 'taskWorkloadActionNoticeActions',
            metaId: 'taskWorkloadDetailStateMeta',
            clearMeta,
            metaKeys: [
                'lastAction',
                'lastActionSource',
                'lastActionTaskNo',
                'lastActionStatus',
                'lastActionTaskPath',
                'lastActionHistoryPath'
            ]
        });
        const noticeEl = document.getElementById('taskWorkloadActionNotice');
        if (noticeEl) {
            noticeEl.dataset.taskNo = '';
        }
    },

    buildTaskDetailPath(taskNo) {
        const params = new URLSearchParams();
        params.set('no', String(taskNo));
        params.set('returnTo', this.buildCurrentDetailPath());
        if (this.bootstrap.source) {
            params.set('source', this.bootstrap.source);
        }
        return `/admin/settings/tasks/get?${params.toString()}`;
    },

    buildTaskHistoryPath(taskNo) {
        const params = new URLSearchParams();
        params.set('taskNo', String(taskNo));
        params.set('returnTo', this.buildCurrentDetailPath());
        if (this.bootstrap.source) {
            params.set('source', this.bootstrap.source);
        }
        return `/admin/settings/tasks/history?${params.toString()}`;
    },

    buildCurrentDetailPath() {
        const params = new URLSearchParams();
        params.set('adminNo', String(this.bootstrap.adminNo || 0));
        if (this.bootstrap.returnTo) {
            params.set('returnTo', this.bootstrap.returnTo);
        }
        if (this.bootstrap.source) {
            params.set('source', this.bootstrap.source);
        }
        return `/admin/settings/tasks/workloads/get?${params.toString()}`;
    },

    buildCurrentListPath() {
        const params = new URLSearchParams();
        if (this.bootstrap.returnTo) {
            params.set('returnTo', this.bootstrap.returnTo);
        }
        if (this.bootstrap.source) {
            params.set('source', this.bootstrap.source);
        }
        const query = params.toString();
        return query ? `/admin/settings/tasks/workloads?${query}` : '/admin/settings/tasks/workloads';
    },

    buildContextualPath(basePath) {
        if (!basePath) {
            return '';
        }
        const [path, rawQuery = ''] = basePath.split('?');
        const params = new URLSearchParams(rawQuery);
        params.set('returnTo', this.buildCurrentDetailPath());
        if (this.bootstrap.source) {
            params.set('source', this.bootstrap.source);
        }
        return `${path}?${params.toString()}`;
    },

    buildLogPathFromBase(basePath) {
        if (!basePath) {
            return '';
        }
        const [path, rawQuery = ''] = basePath.split('?');
        const params = new URLSearchParams(rawQuery);
        params.set('returnTo', this.buildCurrentDetailPath());
        if (this.bootstrap.source) {
            params.set('source', this.bootstrap.source);
        }
        return `${path}?${params.toString()}`;
    },

    async fetchTaskDetail(taskNo) {
        const response = await fetch(`/api/admin/settings/tasks/${taskNo}`);
        if (!response.ok) {
            throw new Error(await CommonJS.extractErrorMessage(response, '작업 상세를 불러오지 못했습니다.'));
        }
        return response.json();
    },

    async saveTaskDetail(payload, fallbackMessage) {
        const response = await fetch('/api/admin/settings/tasks/save', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!response.ok) {
            throw new Error(await CommonJS.extractErrorMessage(response, fallbackMessage));
        }
    },

    parseOptionalNumber(value) {
        if (value == null || value === '') return null;
        return Number(value);
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
