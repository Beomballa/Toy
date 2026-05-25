const TaskWorkloadDetail = {
    initialized: false,
    bootstrap: window.taskWorkloadDetailBootstrap || {},
    operationPolicy: null,
    reassignModal: null,
    reassignDetail: null,

    init() {
        if (this.initialized) return;
        this.initialized = true;
        const modalEl = document.getElementById('taskReassignModal');
        if (modalEl) {
            this.reassignModal = new bootstrap.Modal(modalEl);
        }
        this.bindEvents();
        this.applyOperationPolicy();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));
        this.loadDetail();
    },

    bindEvents() {
        document.getElementById('workloadOverdueTasksBody')?.addEventListener('click', (event) => {
            const completeButton = event.target.closest('[data-role="complete-overdue-task"]');
            if (completeButton) {
                this.completeTask(Number(completeButton.dataset.taskNo));
                return;
            }
            const reassignButton = event.target.closest('[data-role="reassign-overdue-task"]');
            if (reassignButton) {
                this.openReassignModal(Number(reassignButton.dataset.taskNo));
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
            const response = await fetch(`/api/admin/settings/tasks/workloads/${adminNo}`);
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
            }
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

        document.getElementById('workloadDetailTaskListButton').href = data.targetPath || '#';
        document.getElementById('workloadDetailTotalButton').href = data.targetPath || '#';
        document.getElementById('workloadDetailTodoButton').href = data.todoPath || '#';
        document.getElementById('workloadDetailInProgressButton').href = data.inProgressPath || '#';
        document.getElementById('workloadDetailOverdueButton').href = data.overduePath || '#';
        document.getElementById('workloadDetailOverdueSummaryButton').href = data.overduePath || '#';
        document.getElementById('workloadDetailLogButton').href = data.activityLogPath || '#';

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
        }
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
                <div class="fw-bold mb-1"><a class="text-decoration-none" href="${item.taskPath}">${this.escapeHtml(item.title)}</a></div>
                <div class="small text-muted">${this.escapeHtml(item.statusLabel)} · ${this.escapeHtml(item.priorityLabel)} · ${this.escapeHtml(item.dueState)}</div>
                <div class="mt-2">
                    <a class="btn btn-sm btn-outline-secondary" href="${item.historyPath}">이력</a>
                </div>
            </div>
        `).join('');
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
                        <div class="fw-bold mb-1"><a class="text-decoration-none" href="${item.taskPath}">${this.escapeHtml(item.title)}</a></div>
                        <div class="small text-muted">${this.escapeHtml(item.statusLabel)} · ${this.escapeHtml(item.priorityLabel)} · ${this.escapeHtml(item.dueState)}</div>
                    </div>
                    <div class="d-flex flex-column gap-2">
                        <a class="btn btn-sm btn-outline-secondary" href="${item.historyPath}">이력</a>
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
                <div class="fw-bold mb-1"><a class="text-decoration-none" href="${item.taskPath}">${this.escapeHtml(item.taskTitle)}</a></div>
                <div class="small text-muted">${this.escapeHtml(item.adminName)} · ${this.escapeHtml(item.commentDtm)}</div>
                <div class="small text-dark mt-2">${this.escapeHtml(item.content)}</div>
            </div>
        `).join('');
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
                    ${item.taskPath ? `<a class="text-decoration-none" href="${item.taskPath}">${this.escapeHtml(item.taskLabel || '관련 작업 보기')}</a>` : this.escapeHtml(item.taskLabel || '-')}
                </div>
            </div>
        `).join('');
    },

    syncOverdueActionState() {
        const disabled = CommonJS.isAdminWriteBlocked(this.operationPolicy || {});
        const reason = CommonJS.getAdminWriteBlockedReason('기한 초과 작업 직접 조치');
        document.querySelectorAll('[data-role="complete-overdue-task"]').forEach((button) => {
            CommonJS.setButtonDisabled(button, disabled, reason);
        });
        document.querySelectorAll('[data-role="reassign-overdue-task"]').forEach((button) => {
            CommonJS.setButtonDisabled(button, disabled, reason);
        });
        CommonJS.setButtonDisabled(document.getElementById('btnTaskReassignApply'), disabled, reason);
    },

    async completeTask(taskNo) {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('기한 초과 작업 완료 처리'), '알림', 'warning');
            return;
        }
        const confirmed = await CommonJS.confirm('이 작업을 완료 처리하시겠습니까?', '완료 처리');
        if (!confirmed) return;

        try {
            const response = await fetch(`/api/admin/settings/tasks/status/${taskNo}?status=DONE`, {
                method: 'PATCH'
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '작업을 완료 처리하지 못했습니다.'));
            }
            await CommonJS.alert('작업이 완료 처리되었습니다.', '성공', 'success');
            this.loadDetail();
        } catch (error) {
            await CommonJS.alert(error.message, '오류', 'error');
        }
    },

    async openReassignModal(taskNo) {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('기한 초과 작업 재배정'), '알림', 'warning');
            return;
        }
        this.reassignDetail = null;
        const metaEl = document.getElementById('taskReassignModalMeta');
        const listEl = document.getElementById('taskReassignRecommendationList');
        const selectEl = document.getElementById('taskReassignAssignee');
        if (metaEl) metaEl.textContent = '재배정 정보를 불러오는 중입니다...';
        if (listEl) listEl.innerHTML = '<div class="col-12"><div class="text-muted small">추천 담당자를 불러오는 중입니다...</div></div>';
        if (selectEl) selectEl.innerHTML = '<option value="">미지정</option>';
        this.reassignModal?.show();

        try {
            const response = await fetch(`/api/admin/settings/tasks/${taskNo}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '작업 상세를 불러오지 못했습니다.'));
            }
            const data = await response.json();
            this.reassignDetail = data;
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
            const response = await fetch('/api/admin/settings/tasks/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    taskNo: this.reassignDetail.taskNo,
                    title: this.reassignDetail.title,
                    description: this.reassignDetail.description,
                    status: this.reassignDetail.status,
                    priority: this.reassignDetail.priority,
                    assigneeAdminNo,
                    dueDate: this.reassignDetail.dueDate || null,
                    isPinned: this.reassignDetail.isPinned
                })
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '작업을 재배정하지 못했습니다.'));
            }
            await CommonJS.alert('작업이 재배정되었습니다.', '성공', 'success');
            this.reassignModal?.hide();
            this.loadDetail();
        } catch (error) {
            await CommonJS.alert(error.message, '오류', 'error');
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
