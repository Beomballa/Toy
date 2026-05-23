const TaskDetailPage = {
    initialized: false,
    modal: null,
    operationPolicy: null,
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
        const breadcrumbLink = document.getElementById('taskDetailBreadcrumbLink');
        if (breadcrumbLink) {
            breadcrumbLink.href = this.state.returnTo;
        }
    },

    bindEvents() {
        document.getElementById('btnBackToTaskList')?.addEventListener('click', () => {
            window.location.href = this.state.returnTo;
        });
        document.getElementById('btnTaskDetailEdit')?.addEventListener('click', () => this.openEditModal());
        document.getElementById('btnTaskDetailSave')?.addEventListener('click', () => this.saveDetail());
        document.getElementById('btnTaskDetailToggleStatus')?.addEventListener('click', () => this.toggleStatus());
        document.getElementById('btnTaskDetailDelete')?.addEventListener('click', () => this.deleteTask());
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
        const historyPath = `${data.historyPath}&returnTo=${encodeURIComponent(window.location.pathname + window.location.search)}`;
        document.getElementById('btnTaskDetailHistory').href = historyPath;
        document.getElementById('btnTaskDetailLog').href = data.activityLogPath;
        document.getElementById('btnTaskDetailHistoryMore').href = historyPath;
        document.getElementById('btnTaskDetailLogsMore').href = data.activityLogPath;
        document.getElementById('btnTaskDetailToggleStatus').textContent = data.status === 'DONE' ? '진행중으로 변경' : '완료 처리';
        this.renderRecentHistories(data.recentHistories || []);

        const metaEl = document.getElementById('taskDetailStateMeta');
        if (metaEl) {
            metaEl.dataset.detailState = 'ready';
            metaEl.dataset.stateMessage = '';
            metaEl.dataset.taskNo = String(data.taskNo || '');
            metaEl.dataset.status = data.status || '';
        }
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
            await CommonJS.alert('운영 작업이 저장되었습니다.', '성공', 'success');
            this.modal?.hide();
            this.loadDetail();
        } catch (error) {
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
            this.loadDetail();
        } catch (error) {
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
        }
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
    }
};

document.addEventListener('DOMContentLoaded', () => TaskDetailPage.init());
