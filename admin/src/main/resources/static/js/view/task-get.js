const TaskDetailPage = {
    initialized: false,
    modal: null,
    operationPolicy: null,
    noticeTimer: null,
    state: {
        taskNo: null,
        returnTo: '/admin/settings/tasks',
        currentDetail: null
    },

    init() {
        if (this.initialized) return;
        this.initialized = true;
        const modalEl = document.getElementById('taskDetailEditModal');
        if (modalEl) {
            this.modal = new bootstrap.Modal(modalEl);
        }
        this.readBootstrapState();
        this.bindEvents();
        this.applyOperationPolicy();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));
        this.loadDetail();
    },

    readBootstrapState() {
        const bootstrapState = window.taskDetailBootstrap || {};
        this.state.taskNo = Number(bootstrapState.taskNo || 0);
        this.state.returnTo = bootstrapState.returnTo || '/admin/settings/tasks';
        this.state.source = bootstrapState.source || '';
        const breadcrumbLink = document.getElementById('taskDetailBreadcrumbLink');
        if (breadcrumbLink) {
            breadcrumbLink.href = this.state.returnTo;
        }
        this.syncReturnContextMeta();
    },

    bindEvents() {
        document.getElementById('btnBackToTaskList')?.addEventListener('click', () => {
            window.location.href = this.state.returnTo;
        });
        document.getElementById('btnTaskDetailEdit')?.addEventListener('click', () => this.openEditModal());
        document.getElementById('btnTaskDetailSave')?.addEventListener('click', () => this.saveDetail());
        document.getElementById('btnTaskDetailToggleStatus')?.addEventListener('click', () => this.toggleStatus());
        document.getElementById('btnTaskDetailDelete')?.addEventListener('click', () => this.deleteTask());
        document.getElementById('btnTaskCommentSave')?.addEventListener('click', () => this.saveComment());
        document.getElementById('taskCommentList')?.addEventListener('click', (event) => {
            const deleteButton = event.target.closest('[data-role="delete-task-comment"]');
            if (deleteButton) {
                this.deleteComment(Number(deleteButton.dataset.commentNo));
            }
        });
        document.getElementById('taskAssignmentRecommendationList')?.addEventListener('click', (event) => {
            const applyButton = event.target.closest('[data-role="apply-task-recommendation"]');
            if (applyButton) {
                this.applyRecommendation(Number(applyButton.dataset.adminNo));
            }
        });
        document.getElementById('taskDetailActionNoticeClose')?.addEventListener('click', () => this.hideLastActionNotice(true));
    },

    async applyOperationPolicy(settings = null) {
        try {
            this.operationPolicy = settings || await CommonJS.fetchSystemSettings();
            const disabled = CommonJS.isAdminWriteBlocked(this.operationPolicy);
            const reason = CommonJS.getAdminWriteBlockedReason('운영 작업 수정 및 삭제');
            CommonJS.setButtonDisabled(document.getElementById('btnTaskDetailEdit'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnTaskDetailToggleStatus'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnTaskDetailDelete'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnTaskDetailSave'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnTaskCommentSave'), disabled, reason);
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
    },

    async loadDetail() {
        if (!this.state.taskNo) {
            this.renderError('운영 작업 번호가 없습니다.');
            return;
        }
        try {
            const response = await fetch(`/api/admin/settings/tasks/${this.state.taskNo}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 작업 상세를 불러오지 못했습니다.'));
            }
            const data = await response.json();
            this.state.currentDetail = data;
            this.renderDetail(data);
        } catch (error) {
            this.renderError(error.message);
        }
    },

    renderDetail(data) {
        document.getElementById('taskDetailTitle').textContent = data.title || '-';
        document.getElementById('taskDetailStatus').innerHTML = this.renderStatusBadge(data.statusLabel);
        document.getElementById('taskDetailPriority').textContent = data.priorityLabel || '-';
        document.getElementById('taskDetailAssignee').textContent = data.assigneeAdminName || '미지정';
        document.getElementById('taskDetailDueDate').textContent = data.dueDate || '-';
        document.getElementById('taskDetailDueState').textContent = data.dueState || '-';
        document.getElementById('taskDetailDescription').innerHTML = this.escapeHtml(data.description || '-').replace(/\n/g, '<br>');
        document.getElementById('taskDetailPinned').textContent = data.isPinned === 'Y' ? '고정' : '일반';
        document.getElementById('taskDetailCrtDtm').textContent = data.crtDtm || '-';
        document.getElementById('taskDetailTaskNo').textContent = data.taskNo || '-';
        document.getElementById('taskDetailMeta').textContent = `운영 작업 #${data.taskNo}`;
        document.getElementById('taskDetailSummary').textContent = `${data.statusLabel} · ${data.priorityLabel} · 담당자 ${data.assigneeAdminName || '미지정'}`;
        this.renderAssigneeOptions(data.assigneeOptions || []);
        this.renderAssignmentRecommendations(data.assignmentRecommendations || []);
        const historyPath = `${data.historyPath}&returnTo=${encodeURIComponent(window.location.pathname + window.location.search)}`;
        document.getElementById('btnTaskDetailHistory').href = historyPath;
        document.getElementById('btnTaskDetailLog').href = data.activityLogPath;
        document.getElementById('btnTaskDetailHistoryMore').href = historyPath;
        document.getElementById('btnTaskDetailLogsMore').href = data.activityLogPath;
        document.getElementById('btnTaskDetailToggleStatus').textContent = data.status === 'DONE' ? '진행중으로 변경' : '완료 처리';
        this.renderRecentHistories(data.recentHistories || []);
        this.renderComments(data.comments || []);

        const metaEl = document.getElementById('taskDetailStateMeta');
        if (metaEl) {
            metaEl.dataset.detailState = 'ready';
            metaEl.dataset.stateMessage = '';
            metaEl.dataset.taskNo = String(data.taskNo || '');
            metaEl.dataset.status = data.status || '';
            metaEl.dataset.returnTo = this.state.returnTo || '/admin/settings/tasks';
            metaEl.dataset.returnContext = this.resolveReturnContext();
            metaEl.dataset.sourceContext = this.state.source || '';
            if (!metaEl.dataset.lastAction) {
                metaEl.dataset.lastAction = '';
                metaEl.dataset.lastActionSource = '';
                metaEl.dataset.lastActionStatus = '';
                metaEl.dataset.lastActionHistoryPath = '';
                metaEl.dataset.lastActionLogPath = '';
            }
        }
        this.renderLastActionNotice();
    },

    openEditModal() {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 작업 수정 및 삭제'), '알림', 'warning');
            return;
        }
        const detail = this.state.currentDetail;
        if (!detail) return;

        document.getElementById('taskDetailEditTitle').value = detail.title || '';
        document.getElementById('taskDetailEditDescription').value = detail.description || '';
        document.getElementById('taskDetailEditStatus').value = detail.status || 'TODO';
        document.getElementById('taskDetailEditPriority').value = detail.priority || 'MEDIUM';
        document.getElementById('taskDetailEditAssignee').value = detail.assigneeAdminNo || '';
        document.getElementById('taskDetailEditDueDate').value = detail.dueDate || '';
        document.getElementById('taskDetailEditPinned').value = detail.isPinned || 'N';
        this.modal?.show();
    },

    renderAssigneeOptions(options) {
        const select = document.getElementById('taskDetailEditAssignee');
        if (!select) return;
        const selected = this.state.currentDetail?.assigneeAdminNo || '';
        select.innerHTML = ['<option value="">미지정</option>']
            .concat(options.map((option) => `<option value="${option.adminNo}">${this.escapeHtml(option.name)}</option>`))
            .join('');
        select.value = selected;
    },

    renderAssignmentRecommendations(items) {
        const listEl = document.getElementById('taskAssignmentRecommendationList');
        const metaEl = document.getElementById('taskAssignmentRecommendationMeta');
        if (!listEl || !metaEl) return;

        if (!items.length) {
            listEl.innerHTML = '<div class="text-muted small">추천 가능한 담당자가 없습니다.</div>';
            metaEl.textContent = '추천 가능한 담당자가 없습니다.';
            return;
        }

        listEl.innerHTML = items.map((item) => `
            <div class="col-md-4">
                <div class="border rounded-3 p-3 h-100">
                    <div class="fw-bold mb-1">${this.escapeHtml(item.adminName)}</div>
                    <div class="small text-muted mb-2">${this.escapeHtml(item.reasonLabel)}</div>
                    <div class="small text-dark">전체 ${Number(item.totalCount || 0).toLocaleString()}건 · 진행중 ${Number(item.inProgressCount || 0).toLocaleString()}건 · 기한 초과 ${Number(item.overdueCount || 0).toLocaleString()}건</div>
                    <div class="mt-3">
                        <button type="button" class="btn btn-sm btn-outline-dark" data-role="apply-task-recommendation" data-admin-no="${item.adminNo}">이 담당자로 배정</button>
                    </div>
                </div>
            </div>
        `).join('');
        metaEl.textContent = `추천 후보 ${items.length}명`;
    },

    async saveDetail() {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 작업 수정 및 삭제'), '알림', 'warning');
            return;
        }
        const detail = this.state.currentDetail;
        if (!detail) return;

        const payload = {
            taskNo: detail.taskNo,
            title: CommonJS.normalizeRequiredText(document.getElementById('taskDetailEditTitle').value || ''),
            description: CommonJS.normalizeOptionalText(document.getElementById('taskDetailEditDescription').value || ''),
            status: document.getElementById('taskDetailEditStatus').value,
            priority: document.getElementById('taskDetailEditPriority').value,
            assigneeAdminNo: this.parseOptionalNumber(document.getElementById('taskDetailEditAssignee').value),
            dueDate: document.getElementById('taskDetailEditDueDate').value || null,
            isPinned: document.getElementById('taskDetailEditPinned').value
        };

        if (!payload.title) {
            await CommonJS.alert('작업 제목을 입력하세요.', '알림', 'warning');
            return;
        }

        try {
            const response = await fetch('/api/admin/settings/tasks/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 작업을 저장하지 못했습니다.'));
            }
            this.setLastActionMeta('save-detail', 'success', '상세 수정');
            await CommonJS.alert('운영 작업이 저장되었습니다.', '성공', 'success');
            this.modal?.hide();
            this.loadDetail();
        } catch (error) {
            this.setLastActionMeta('save-detail', 'error', '상세 수정');
            await CommonJS.alert(error.message, '오류', 'error');
        }
    },

    async toggleStatus() {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 작업 상태 변경'), '알림', 'warning');
            return;
        }
        const detail = this.state.currentDetail;
        if (!detail) return;

        const nextStatus = detail.status === 'DONE' ? 'IN_PROGRESS' : 'DONE';
        try {
            const response = await fetch(`/api/admin/settings/tasks/status/${detail.taskNo}?status=${nextStatus}`, {
                method: 'PATCH'
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 작업 상태를 변경하지 못했습니다.'));
            }
            this.setLastActionMeta('toggle-status', 'success', '상태 변경');
            this.loadDetail();
        } catch (error) {
            this.setLastActionMeta('toggle-status', 'error', '상태 변경');
            await CommonJS.alert(error.message, '오류', 'error');
        }
    },

    async deleteTask() {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 작업 삭제'), '알림', 'warning');
            return;
        }
        const detail = this.state.currentDetail;
        if (!detail) return;

        const confirmed = await CommonJS.confirm('운영 작업을 삭제하시겠습니까?', '삭제 확인');
        if (!confirmed) return;

        try {
            const response = await fetch(`/api/admin/settings/tasks/delete?no=${detail.taskNo}`, { method: 'DELETE' });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 작업을 삭제하지 못했습니다.'));
            }
            await CommonJS.alert('운영 작업이 삭제되었습니다.', '성공', 'success');
            window.location.href = this.state.returnTo;
        } catch (error) {
            await CommonJS.alert(error.message, '오류', 'error');
        }
    },

    async saveComment() {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 작업 메모 등록'), '알림', 'warning');
            return;
        }
        const contentEl = document.getElementById('taskCommentContent');
        const content = (contentEl?.value || '').trim();
        if (!content) {
            await CommonJS.alert('메모 내용을 입력하세요.', '알림', 'warning');
            return;
        }

        try {
            const response = await fetch(`/api/admin/settings/tasks/${this.state.taskNo}/comments`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ content })
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '작업 메모를 저장하지 못했습니다.'));
            }
            contentEl.value = '';
            this.setLastActionMeta('save-comment', 'success', '메모 등록');
            await CommonJS.alert('작업 메모가 등록되었습니다.', '성공', 'success');
            this.loadDetail();
        } catch (error) {
            this.setLastActionMeta('save-comment', 'error', '메모 등록');
            await CommonJS.alert(error.message, '오류', 'error');
        }
    },

    async deleteComment(commentNo) {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 작업 메모 삭제'), '알림', 'warning');
            return;
        }
        const confirmed = await CommonJS.confirm('작업 메모를 삭제하시겠습니까?', '삭제 확인');
        if (!confirmed) return;

        try {
            const response = await fetch(`/api/admin/settings/tasks/${this.state.taskNo}/comments/${commentNo}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '작업 메모를 삭제하지 못했습니다.'));
            }
            this.setLastActionMeta('delete-comment', 'success', '메모 삭제');
            await CommonJS.alert('작업 메모가 삭제되었습니다.', '성공', 'success');
            this.loadDetail();
        } catch (error) {
            this.setLastActionMeta('delete-comment', 'error', '메모 삭제');
            await CommonJS.alert(error.message, '오류', 'error');
        }
    },

    renderRecentHistories(items) {
        const listEl = document.getElementById('taskDetailRecentHistoryList');
        const metaEl = document.getElementById('taskDetailHistoryStateMeta');
        if (!listEl) return;

        if (!items.length) {
            listEl.innerHTML = '<div class="text-muted small">최근 로그가 없습니다.</div>';
            if (metaEl) {
                metaEl.dataset.listState = 'empty';
                metaEl.dataset.stateMessage = '최근 로그가 없습니다.';
                metaEl.dataset.visibleCount = '0';
            }
            return;
        }

        listEl.innerHTML = items.map((item) => `
            <div class="list-group-item px-0">
                <div class="d-flex justify-content-between align-items-start gap-3">
                    <div>
                        <div class="fw-semibold">${this.escapeHtml(item.actionLabel)}</div>
                        <div class="small text-muted">${this.escapeHtml(item.adminName || '-')} · ${this.escapeHtml(item.actionDtm || '-')}</div>
                    </div>
                    <div class="d-flex gap-2">
                        <a class="btn btn-sm btn-outline-secondary" href="${item.historyPath}&returnTo=${encodeURIComponent(window.location.pathname + window.location.search)}">이력</a>
                        <a class="btn btn-sm btn-outline-secondary" href="${item.activityLogPath}">활동 로그</a>
                    </div>
                </div>
            </div>
        `).join('');

        if (metaEl) {
            metaEl.dataset.listState = 'ready';
            metaEl.dataset.stateMessage = '';
            metaEl.dataset.visibleCount = String(items.length);
        }
    },

    renderComments(items) {
        const listEl = document.getElementById('taskCommentList');
        const metaEl = document.getElementById('taskCommentStateMeta');
        const metaTextEl = document.getElementById('taskCommentMeta');
        if (!listEl) return;

        if (!items.length) {
            listEl.innerHTML = '<div class="text-muted small">등록된 작업 메모가 없습니다.</div>';
            if (metaEl) {
                metaEl.dataset.listState = 'empty';
                metaEl.dataset.stateMessage = '등록된 작업 메모가 없습니다.';
                metaEl.dataset.visibleCount = '0';
            }
            if (metaTextEl) {
                metaTextEl.textContent = '등록된 작업 메모가 없습니다.';
            }
            return;
        }

        listEl.innerHTML = items.map((item) => `
            <div class="list-group-item px-0">
                <div class="d-flex justify-content-between align-items-start gap-3">
                    <div>
                        <div class="fw-semibold">${this.escapeHtml(item.adminName || '-')} <span class="small text-muted">(#${item.adminNo || '-'})</span></div>
                        <div class="small text-muted mb-2">${this.escapeHtml(item.crtDtm || '-')}</div>
                        <div class="small text-dark">${this.escapeHtml(item.content || '-').replace(/\n/g, '<br>')}</div>
                    </div>
                    <button type="button" class="btn btn-sm btn-outline-danger" data-role="delete-task-comment" data-comment-no="${item.commentNo}">삭제</button>
                </div>
            </div>
        `).join('');

        if (metaEl) {
            metaEl.dataset.listState = 'ready';
            metaEl.dataset.stateMessage = '';
            metaEl.dataset.visibleCount = String(items.length);
        }
        if (metaTextEl) {
            metaTextEl.textContent = `최근 메모 ${items.length}건`;
        }
    },

    renderError(message) {
        document.getElementById('taskDetailTitle').textContent = message;
        document.getElementById('taskDetailMeta').textContent = '상세 확인 불가';
        document.getElementById('taskDetailSummary').textContent = '운영 작업 상세 조회에 실패했습니다.';
        const historyList = document.getElementById('taskDetailRecentHistoryList');
        if (historyList) {
            historyList.innerHTML = `<div class="text-danger small">${this.escapeHtml(message)}</div>`;
        }
        const metaEl = document.getElementById('taskDetailStateMeta');
        if (metaEl) {
            metaEl.dataset.detailState = 'error';
            metaEl.dataset.stateMessage = message;
            metaEl.dataset.returnTo = this.state.returnTo || '/admin/settings/tasks';
            metaEl.dataset.returnContext = this.resolveReturnContext();
            metaEl.dataset.sourceContext = this.state.source || '';
        }
        this.renderLastActionNotice();
        const historyMetaEl = document.getElementById('taskDetailHistoryStateMeta');
        if (historyMetaEl) {
            historyMetaEl.dataset.listState = 'error';
            historyMetaEl.dataset.stateMessage = message;
            historyMetaEl.dataset.visibleCount = '0';
        }
    },

    renderStatusBadge(statusLabel) {
        const status = statusLabel || '-';
        const badgeClass = status === '완료'
            ? 'badge-y'
            : status === '진행중'
                ? 'badge-low-stock'
                : status === '보류'
                    ? 'badge-n'
                    : 'text-bg-secondary';
        return `<span class="badge rounded-pill ${badgeClass}">${this.escapeHtml(status)}</span>`;
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
    },

    async applyRecommendation(adminNo) {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 작업 배정 추천 적용'), '알림', 'warning');
            return;
        }
        const detail = this.state.currentDetail;
        if (!detail) return;

        const confirmed = await CommonJS.confirm('추천 담당자로 바로 배정하시겠습니까?', '배정 확인');
        if (!confirmed) return;

        try {
            const response = await fetch('/api/admin/settings/tasks/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    taskNo: detail.taskNo,
                    title: detail.title,
                    description: detail.description,
                    status: detail.status,
                    priority: detail.priority,
                    assigneeAdminNo: adminNo,
                    dueDate: detail.dueDate || null,
                    isPinned: detail.isPinned
                })
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '추천 담당자 배정에 실패했습니다.'));
            }
            this.setLastActionMeta('apply-recommendation', 'success', '배정 추천');
            await CommonJS.alert('추천 담당자로 배정되었습니다.', '성공', 'success');
            this.loadDetail();
        } catch (error) {
            this.setLastActionMeta('apply-recommendation', 'error', '배정 추천');
            await CommonJS.alert(error.message, '오류', 'error');
        }
    },

    setLastActionMeta(action, status, sourceLabel = '운영 작업 상세') {
        const metaEl = document.getElementById('taskDetailStateMeta');
        if (!metaEl) return;
        metaEl.dataset.lastAction = action || '';
        metaEl.dataset.lastActionSource = sourceLabel || '운영 작업 상세';
        metaEl.dataset.lastActionStatus = status || '';
        metaEl.dataset.lastActionHistoryPath = this.buildHistoryPath();
        metaEl.dataset.lastActionLogPath = this.state.currentDetail?.activityLogPath || '';
        this.renderLastActionNotice();
    },

    syncReturnContextMeta() {
        const metaEl = document.getElementById('taskDetailStateMeta');
        if (!metaEl) return;
        metaEl.dataset.returnTo = this.state.returnTo || '/admin/settings/tasks';
        metaEl.dataset.returnContext = this.resolveReturnContext();
        metaEl.dataset.sourceContext = this.state.source || '';
    },

    resolveReturnContext() {
        const returnTo = this.state.returnTo || '';
        if (returnTo.includes('/tasks/workloads/get')) return 'workload-detail';
        if (returnTo.includes('/tasks/workloads')) return 'workload-list';
        if (returnTo.includes('/tasks/history')) return 'task-history';
        if (returnTo.includes('/admin')) return 'task-list';
        return 'unknown';
    },

    renderLastActionNotice() {
        const metaEl = document.getElementById('taskDetailStateMeta');
        const noticeEl = document.getElementById('taskDetailActionNotice');
        const noticeTextEl = document.getElementById('taskDetailActionNoticeText');
        const noticeActionsEl = document.getElementById('taskDetailActionNoticeActions');
        if (!metaEl || !noticeEl || !noticeTextEl || !noticeActionsEl) return;

        const action = metaEl.dataset.lastAction || '';
        const source = metaEl.dataset.lastActionSource || '';
        const status = metaEl.dataset.lastActionStatus || '';
        const historyPath = metaEl.dataset.lastActionHistoryPath || '';
        const logPath = metaEl.dataset.lastActionLogPath || '';

        if (!action || !status) {
            this.hideLastActionNotice(false);
            return;
        }

        const templates = {
            'save-detail:success': '운영 작업 수정 내용을 반영했습니다.',
            'save-detail:error': '운영 작업 수정에 실패했습니다.',
            'toggle-status:success': '운영 작업 상태를 변경했습니다.',
            'toggle-status:error': '운영 작업 상태 변경에 실패했습니다.',
            'save-comment:success': '작업 메모를 등록했습니다.',
            'save-comment:error': '작업 메모 등록에 실패했습니다.',
            'delete-comment:success': '작업 메모를 삭제했습니다.',
            'delete-comment:error': '작업 메모 삭제에 실패했습니다.',
            'apply-recommendation:success': '추천 담당자 배정을 반영했습니다.',
            'apply-recommendation:error': '추천 담당자 배정에 실패했습니다.'
        };
        const variants = {
            'save-detail:success': 'alert-success',
            'toggle-status:success': 'alert-primary',
            'save-comment:success': 'alert-success',
            'delete-comment:success': 'alert-warning',
            'apply-recommendation:success': 'alert-primary',
            'save-detail:error': 'alert-danger',
            'toggle-status:error': 'alert-danger',
            'save-comment:error': 'alert-danger',
            'delete-comment:error': 'alert-danger',
            'apply-recommendation:error': 'alert-danger'
        };

        const sourceMessage = source ? `${source}에서 실행` : '운영 작업 상세에서 실행';
        const message = templates[`${action}:${status}`] || '조치 결과를 확인해 주세요.';
        const variantClass = variants[`${action}:${status}`] || (status === 'success' ? 'alert-success' : 'alert-danger');
        noticeEl.classList.remove('d-none', 'alert-success', 'alert-danger', 'alert-warning', 'alert-primary');
        noticeEl.classList.add(variantClass);
        noticeTextEl.textContent = `${sourceMessage} · ${message}`;
        noticeActionsEl.innerHTML = [
            historyPath ? `<a class="btn btn-sm btn-outline-secondary" href="${historyPath}">이력</a>` : '',
            logPath ? `<a class="btn btn-sm btn-outline-secondary" href="${logPath}">활동 로그</a>` : ''
        ].join('');
        noticeEl.dataset.visible = 'Y';
        noticeEl.dataset.action = action;
        noticeEl.dataset.status = status;
        this.scheduleLastActionNoticeHide(status);
    },

    scheduleLastActionNoticeHide(status) {
        this.clearLastActionNoticeHide();
        if (status !== 'success') {
            return;
        }
        this.noticeTimer = window.setTimeout(() => this.hideLastActionNotice(true), 5000);
    },

    clearLastActionNoticeHide() {
        if (!this.noticeTimer) return;
        window.clearTimeout(this.noticeTimer);
        this.noticeTimer = null;
    },

    hideLastActionNotice(clearMeta = false) {
        this.clearLastActionNoticeHide();
        const metaEl = document.getElementById('taskDetailStateMeta');
        const noticeEl = document.getElementById('taskDetailActionNotice');
        const noticeTextEl = document.getElementById('taskDetailActionNoticeText');
        const noticeActionsEl = document.getElementById('taskDetailActionNoticeActions');
        if (!noticeEl || !noticeTextEl || !noticeActionsEl) return;

        noticeEl.classList.add('d-none');
        noticeEl.classList.remove('alert-success', 'alert-danger', 'alert-warning', 'alert-primary');
        noticeTextEl.textContent = '';
        noticeActionsEl.innerHTML = '';
        noticeEl.dataset.visible = 'N';
        noticeEl.dataset.action = '';
        noticeEl.dataset.status = '';

        if (!clearMeta || !metaEl) return;
        metaEl.dataset.lastAction = '';
        metaEl.dataset.lastActionSource = '';
        metaEl.dataset.lastActionStatus = '';
        metaEl.dataset.lastActionHistoryPath = '';
        metaEl.dataset.lastActionLogPath = '';
    },

    buildHistoryPath() {
        const detail = this.state.currentDetail;
        if (!detail?.historyPath) return '';
        return `${detail.historyPath}&returnTo=${encodeURIComponent(window.location.pathname + window.location.search)}`;
    }
};

document.addEventListener('DOMContentLoaded', () => TaskDetailPage.init());
