const TaskDetailPage = {
    initialized: false,
    modal: null,
    operationPolicy: null,
    isSavingDetail: false,
    isTogglingStatus: false,
    isDeletingTask: false,
    isSavingComment: false,
    isDeletingComment: false,
    isApplyingRecommendation: false,
    state: {
        taskNo: null,
        returnTo: '/admin/settings/tasks',
        currentDetail: null,
        editingCommentNo: null
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
        CommonJS.bindMainLogoNavigation(this.state.returnTo);
        this.applyOperationPolicy();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));
        this.loadDetail();
    },

    readBootstrapState() {
        const bootstrapState = window.taskDetailBootstrap || {};
        this.state.taskNo = Number(bootstrapState.taskNo || 0);
        this.state.returnTo = bootstrapState.returnTo || '/admin/settings/tasks';
        this.state.source = bootstrapState.source || '';
        this.syncReturnLinks();
        this.syncReturnContextMeta();
        CommonJS.renderSourceContextNotice({ noticeId: 'taskDetailSourceContextNotice', source: this.state.source });
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
        document.getElementById('btnTaskCommentCancelEdit')?.addEventListener('click', () => this.resetCommentEditor());
        document.getElementById('taskCommentList')?.addEventListener('click', (event) => {
            const editButton = event.target.closest('[data-role="edit-task-comment"]');
            if (editButton) {
                this.startCommentEdit(Number(editButton.dataset.commentNo));
                return;
            }
            const deleteButton = event.target.closest('[data-role="delete-task-comment"]');
            if (deleteButton) {
                this.deleteComment(Number(deleteButton.dataset.commentNo));
            }
        });
        document.getElementById('taskAssignmentRecommendationList')?.addEventListener('click', (event) => {
            const applyButton = event.target.closest('[data-role="apply-task-recommendation"]');
            if (applyButton) {
                this.applyRecommendation(Number(applyButton.dataset.adminNo), applyButton);
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
        if (!this.isValidTaskNo(this.state.taskNo)) {
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
        const historyPath = this.buildHistoryPathFromBase(data.historyPath);
        document.getElementById('btnTaskDetailHistory').href = historyPath;
        document.getElementById('btnTaskDetailLog').href = this.buildLogPathFromBase(data.activityLogPath);
        document.getElementById('btnTaskDetailHistoryMore').href = historyPath;
        document.getElementById('btnTaskDetailLogsMore').href = this.buildLogPathFromBase(data.activityLogPath);
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

    async openEditModal() {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 작업 수정 및 삭제'), '알림', 'warning');
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
            listEl.innerHTML = `
                <div class="col-12">
                    ${this.buildStateMarkup('empty', '추천 가능한 담당자가 없습니다.', '현재 작업 조건으로는 추천할 담당자 후보를 계산하지 못했습니다.', 'fa-user-slash')}
                </div>
            `;
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
        if (this.isSavingDetail) return;
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
        if (!this.validateTaskPayload(payload)) {
            await CommonJS.alert('담당자 또는 작업 기한 입력값을 다시 확인하세요.', '알림', 'warning');
            return;
        }
        if (this.isSameAsCurrentDetail(payload, detail)) {
            await CommonJS.alert('변경된 작업 정보가 없습니다.', '알림', 'info');
            return;
        }

        try {
            this.isSavingDetail = true;
            this.setBusyButton(document.getElementById('btnTaskDetailSave'), true, '저장 중...');
            const response = await fetch('/api/admin/settings/tasks/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 작업을 저장하지 못했습니다.'));
            }
            this.setLastActionMeta('save-detail', 'success', '상세 수정');
            this.modal?.hide();
            await this.loadDetail();
            await CommonJS.alert('운영 작업이 저장되었습니다.', '성공', 'success');
        } catch (error) {
            this.setLastActionMeta('save-detail', 'error', '상세 수정');
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isSavingDetail = false;
            this.setBusyButton(document.getElementById('btnTaskDetailSave'), false);
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    async toggleStatus() {
        if (this.isTogglingStatus) return;
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 작업 상태 변경'), '알림', 'warning');
            return;
        }
        const detail = this.state.currentDetail;
        if (!detail) return;

        const nextStatus = detail.status === 'DONE' ? 'IN_PROGRESS' : 'DONE';
        if (!this.isValidTaskNo(detail.taskNo) || !this.isValidTaskStatus(nextStatus)) {
            await CommonJS.alert('유효하지 않은 작업 상태 변경 요청입니다.', '알림', 'warning');
            return;
        }
        try {
            this.isTogglingStatus = true;
            this.setBusyButton(document.getElementById('btnTaskDetailToggleStatus'), true, '처리 중...');
            const response = await fetch(`/api/admin/settings/tasks/status/${detail.taskNo}?status=${nextStatus}`, {
                method: 'PATCH'
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 작업 상태를 변경하지 못했습니다.'));
            }
            this.setLastActionMeta('toggle-status', 'success', '상태 변경');
            await this.loadDetail();
            await CommonJS.alert('운영 작업 상태가 변경되었습니다.', '성공', 'success');
        } catch (error) {
            this.setLastActionMeta('toggle-status', 'error', '상태 변경');
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isTogglingStatus = false;
            this.setBusyButton(document.getElementById('btnTaskDetailToggleStatus'), false);
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    async deleteTask() {
        if (this.isDeletingTask) return;
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 작업 삭제'), '알림', 'warning');
            return;
        }
        const detail = this.state.currentDetail;
        if (!detail) return;
        if (!this.isValidTaskNo(detail.taskNo)) {
            await CommonJS.alert('유효하지 않은 운영 작업 번호입니다.', '알림', 'warning');
            return;
        }

        const confirmed = await CommonJS.confirm('운영 작업을 삭제하시겠습니까?', '삭제 확인');
        if (!confirmed) return;

        try {
            this.isDeletingTask = true;
            this.setBusyButton(document.getElementById('btnTaskDetailDelete'), true, '삭제 중...');
            const response = await fetch(`/api/admin/settings/tasks/delete?no=${detail.taskNo}`, { method: 'DELETE' });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 작업을 삭제하지 못했습니다.'));
            }
            await CommonJS.alert('운영 작업이 삭제되었습니다.', '성공', 'success');
            window.location.href = this.state.returnTo;
        } catch (error) {
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isDeletingTask = false;
            this.setBusyButton(document.getElementById('btnTaskDetailDelete'), false);
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    async saveComment() {
        if (this.isSavingComment) return;
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 작업 메모 등록'), '알림', 'warning');
            return;
        }
        const contentEl = document.getElementById('taskCommentContent');
        const content = CommonJS.normalizeRequiredText(contentEl?.value || '');
        if (!content) {
            await CommonJS.alert('메모 내용을 입력하세요.', '알림', 'warning');
            return;
        }
        if (content.length > 2000) {
            await CommonJS.alert('작업 메모는 2000자 이하로 입력하세요.', '알림', 'warning');
            return;
        }
        if (this.state.editingCommentNo != null && !this.isValidCommentNo(this.state.editingCommentNo)) {
            await CommonJS.alert('수정할 작업 메모 번호가 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        const editingComment = this.state.currentDetail?.comments?.find((item) => Number(item.commentNo) === Number(this.state.editingCommentNo));
        if (editingComment && CommonJS.normalizeRequiredText(editingComment.content || '') === content) {
            await CommonJS.alert('변경된 메모 내용이 없습니다.', '알림', 'info');
            return;
        }

        let shouldResetCommentEditor = false;
        try {
            this.isSavingComment = true;
            const isEditing = this.state.editingCommentNo != null;
            this.setBusyButton(document.getElementById('btnTaskCommentSave'), true, isEditing ? '수정 중...' : '등록 중...');
            const response = await fetch(
                isEditing
                    ? `/api/admin/settings/tasks/${this.state.taskNo}/comments/${this.state.editingCommentNo}`
                    : `/api/admin/settings/tasks/${this.state.taskNo}/comments`,
                {
                method: isEditing ? 'PATCH' : 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ content })
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, isEditing ? '작업 메모를 수정하지 못했습니다.' : '작업 메모를 저장하지 못했습니다.'));
            }
            this.setLastActionMeta(isEditing ? 'update-comment' : 'save-comment', 'success', isEditing ? '메모 수정' : '메모 등록');
            shouldResetCommentEditor = true;
            await this.loadDetail();
            await CommonJS.alert(isEditing ? '작업 메모가 수정되었습니다.' : '작업 메모가 등록되었습니다.', '성공', 'success');
        } catch (error) {
            this.setLastActionMeta(this.state.editingCommentNo != null ? 'update-comment' : 'save-comment', 'error', this.state.editingCommentNo != null ? '메모 수정' : '메모 등록');
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isSavingComment = false;
            this.setBusyButton(document.getElementById('btnTaskCommentSave'), false);
            if (shouldResetCommentEditor) {
                this.resetCommentEditor();
            }
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    async deleteComment(commentNo) {
        if (this.isDeletingComment) return;
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 작업 메모 삭제'), '알림', 'warning');
            return;
        }
        if (!this.isValidTaskNo(this.state.taskNo) || !this.isValidCommentNo(commentNo)) {
            await CommonJS.alert('삭제할 작업 메모 정보가 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        const confirmed = await CommonJS.confirm('작업 메모를 삭제하시겠습니까?', '삭제 확인');
        if (!confirmed) return;

        let shouldResetCommentEditor = false;
        try {
            this.isDeletingComment = true;
            this.setCollectionButtonsDisabled('[data-role="delete-task-comment"]', true);
            const response = await fetch(`/api/admin/settings/tasks/${this.state.taskNo}/comments/${commentNo}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '작업 메모를 삭제하지 못했습니다.'));
            }
            this.setLastActionMeta('delete-comment', 'success', '메모 삭제');
            shouldResetCommentEditor = true;
            await this.loadDetail();
            await CommonJS.alert('작업 메모가 삭제되었습니다.', '성공', 'success');
        } catch (error) {
            this.setLastActionMeta('delete-comment', 'error', '메모 삭제');
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isDeletingComment = false;
            this.setCollectionButtonsDisabled('[data-role="delete-task-comment"]', false);
            if (shouldResetCommentEditor) {
                this.resetCommentEditor();
            }
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    renderRecentHistories(items) {
        const listEl = document.getElementById('taskDetailRecentHistoryList');
        const metaEl = document.getElementById('taskDetailHistoryStateMeta');
        if (!listEl) return;

        if (!items.length) {
            listEl.innerHTML = `
                <div class="product-empty-state py-4">
                    <i class="fas fa-clock-rotate-left product-empty-state-icon"></i>
                    <strong>최근 로그가 없습니다.</strong>
                    <p>아직 이 작업에 대한 상태 변경, 수정, 삭제 기록이 없습니다.</p>
                </div>
            `;
            const historyMetaText = document.getElementById('taskDetailHistoryMeta');
            if (historyMetaText) {
                historyMetaText.textContent = '최근 로그 0건';
            }
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
                        <a class="btn btn-sm btn-outline-secondary" href="${this.buildHistoryPathFromBase(item.historyPath)}">이력</a>
                        <a class="btn btn-sm btn-outline-secondary" href="${this.buildLogPathFromBase(item.activityLogPath)}">활동 로그</a>
                    </div>
                </div>
            </div>
        `).join('');

        const historyMetaText = document.getElementById('taskDetailHistoryMeta');
        if (historyMetaText) {
            historyMetaText.textContent = `최근 로그 ${items.length}건`;
        }

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
            listEl.innerHTML = `
                <div class="product-empty-state py-4">
                    <i class="fas fa-note-sticky product-empty-state-icon"></i>
                    <strong>등록된 작업 메모가 없습니다.</strong>
                    <p>운영 처리 과정에서 필요한 메모를 남기면 이 영역에서 바로 확인할 수 있습니다.</p>
                </div>
            `;
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
                    <div class="d-flex gap-2">
                        <button type="button" class="btn btn-sm btn-outline-secondary" data-role="edit-task-comment" data-comment-no="${item.commentNo}">수정</button>
                        <button type="button" class="btn btn-sm btn-outline-danger" data-role="delete-task-comment" data-comment-no="${item.commentNo}">삭제</button>
                    </div>
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

    startCommentEdit(commentNo) {
        const targetComment = this.state.currentDetail?.comments?.find((item) => Number(item.commentNo) === commentNo);
        if (!targetComment) {
            return;
        }
        this.state.editingCommentNo = commentNo;
        const contentEl = document.getElementById('taskCommentContent');
        if (contentEl) {
            contentEl.value = targetComment.content || '';
            contentEl.focus();
        }
        this.syncCommentEditorState();
    },

    resetCommentEditor() {
        this.state.editingCommentNo = null;
        const contentEl = document.getElementById('taskCommentContent');
        if (contentEl) {
            contentEl.value = '';
        }
        this.syncCommentEditorState();
    },

    syncCommentEditorState() {
        const isEditing = this.state.editingCommentNo != null;
        const saveButton = document.getElementById('btnTaskCommentSave');
        const cancelButton = document.getElementById('btnTaskCommentCancelEdit');
        const metaEl = document.getElementById('taskCommentEditMeta');
        const inputLabel = document.querySelector('label[for="taskCommentContent"]');
        if (saveButton) {
            saveButton.textContent = isEditing ? '메모 수정' : '메모 등록';
        }
        if (cancelButton) {
            cancelButton.classList.toggle('d-none', !isEditing);
        }
        if (metaEl) {
            metaEl.classList.toggle('d-none', !isEditing);
        }
        if (inputLabel) {
            inputLabel.textContent = isEditing ? '메모 수정' : '새 메모';
        }
    },

    renderError(message) {
        document.getElementById('taskDetailTitle').textContent = message;
        document.getElementById('taskDetailMeta').textContent = '상세 확인 불가';
        document.getElementById('taskDetailSummary').textContent = '운영 작업 상세 조회에 실패했습니다.';
        const historyList = document.getElementById('taskDetailRecentHistoryList');
        const commentList = document.getElementById('taskCommentList');
        const recommendationList = document.getElementById('taskAssignmentRecommendationList');
        const recommendationMeta = document.getElementById('taskAssignmentRecommendationMeta');
        if (historyList) {
            historyList.innerHTML = this.buildStateMarkup('error', '최근 로그를 불러오지 못했습니다.', message, 'fa-triangle-exclamation');
        }
        if (commentList) {
            commentList.innerHTML = this.buildStateMarkup('error', '작업 메모를 불러오지 못했습니다.', '잠시 후 다시 시도하거나 관련 작업 이력을 먼저 확인해주세요.', 'fa-triangle-exclamation');
        }
        if (recommendationList) {
            recommendationList.innerHTML = `
                <div class="col-12">
                    ${this.buildStateMarkup('error', '추천 담당자를 계산하지 못했습니다.', '작업 상세를 다시 불러온 뒤 추천 후보를 다시 확인해주세요.', 'fa-triangle-exclamation')}
                </div>
            `;
        }
        if (recommendationMeta) {
            recommendationMeta.textContent = '추천 담당자 계산 실패';
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
        CommonJS.renderSourceContextNotice({ noticeId: 'taskDetailSourceContextNotice', source: this.state.source });
        const historyMetaEl = document.getElementById('taskDetailHistoryStateMeta');
        if (historyMetaEl) {
            historyMetaEl.dataset.listState = 'error';
            historyMetaEl.dataset.stateMessage = message;
            historyMetaEl.dataset.visibleCount = '0';
        }
        const commentMetaEl = document.getElementById('taskCommentStateMeta');
        if (commentMetaEl) {
            commentMetaEl.dataset.listState = 'error';
            commentMetaEl.dataset.stateMessage = message;
            commentMetaEl.dataset.visibleCount = '0';
        }
    },

    buildStateMarkup(type, title, description, icon) {
        return `
            <div class="product-empty-state py-4">
                <div class="product-empty-state__icon ${type === 'error' ? 'text-danger' : 'text-primary'}">
                    <i class="fa-solid ${icon}"></i>
                </div>
                <strong>${this.escapeHtml(title)}</strong>
                <p>${this.escapeHtml(description)}</p>
            </div>
        `;
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

    async applyRecommendation(adminNo, button = null) {
        if (this.isApplyingRecommendation) return;
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 작업 배정 추천 적용'), '알림', 'warning');
            return;
        }
        const detail = this.state.currentDetail;
        if (!detail) return;
        if (!Number.isFinite(adminNo) || adminNo <= 0) {
            await CommonJS.alert('추천 담당자 정보가 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        if (Number(detail.assigneeAdminNo || 0) === adminNo) {
            await CommonJS.alert('이미 해당 담당자로 배정되어 있습니다.', '알림', 'info');
            return;
        }

        const confirmed = await CommonJS.confirm('추천 담당자로 바로 배정하시겠습니까?', '배정 확인');
        if (!confirmed) return;

        try {
            this.isApplyingRecommendation = true;
            this.setBusyButton(button, true, '배정 중...');
            this.setCollectionButtonsDisabled('[data-role="apply-task-recommendation"]', true);
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
            await this.loadDetail();
            await CommonJS.alert('추천 담당자로 배정되었습니다.', '성공', 'success');
        } catch (error) {
            this.setLastActionMeta('apply-recommendation', 'error', '배정 추천');
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isApplyingRecommendation = false;
            this.setBusyButton(button, false);
            this.setCollectionButtonsDisabled('[data-role="apply-task-recommendation"]', false);
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    validateTaskPayload(payload) {
        if (!payload) {
            return false;
        }
        if (!this.isValidTaskNo(payload.taskNo)) {
            return false;
        }
        if (!this.isValidTaskStatus(payload.status)) {
            return false;
        }
        if (!this.isValidTaskPriority(payload.priority)) {
            return false;
        }
        if (!this.isValidYn(payload.isPinned)) {
            return false;
        }
        if (payload.assigneeAdminNo != null && (!Number.isFinite(payload.assigneeAdminNo) || payload.assigneeAdminNo <= 0)) {
            return false;
        }
        if (payload.dueDate && !/^\d{4}-\d{2}-\d{2}$/.test(payload.dueDate)) {
            return false;
        }
        return true;
    },

    isValidTaskNo(taskNo) {
        return Number.isInteger(Number(taskNo)) && Number(taskNo) > 0;
    },

    isValidCommentNo(commentNo) {
        return Number.isInteger(Number(commentNo)) && Number(commentNo) > 0;
    },

    isValidTaskStatus(status) {
        return ['TODO', 'IN_PROGRESS', 'DONE', 'HOLD'].includes(status);
    },

    isValidTaskPriority(priority) {
        return ['HIGH', 'MEDIUM', 'LOW'].includes(priority);
    },

    isValidYn(value) {
        return value === 'Y' || value === 'N';
    },

    isSameAsCurrentDetail(payload, detail) {
        return payload.title === CommonJS.normalizeRequiredText(detail.title || '')
            && payload.description === (CommonJS.normalizeOptionalText(detail.description || '') || '')
            && payload.status === (detail.status || 'TODO')
            && payload.priority === (detail.priority || 'MEDIUM')
            && Number(payload.assigneeAdminNo || 0) === Number(detail.assigneeAdminNo || 0)
            && (payload.dueDate || null) === (detail.dueDate || null)
            && payload.isPinned === (detail.isPinned || 'N');
    },

    setBusyButton(button, isBusy, busyText = '처리 중...') {
        if (!button) return;
        if (isBusy) {
            if (!button.dataset.originalText) {
                button.dataset.originalText = button.textContent;
            }
            button.disabled = true;
            button.textContent = busyText;
            return;
        }
        button.disabled = false;
        if (button.dataset.originalText) {
            button.textContent = button.dataset.originalText;
            delete button.dataset.originalText;
        }
    },

    setCollectionButtonsDisabled(selector, disabled) {
        document.querySelectorAll(selector).forEach((button) => {
            button.disabled = disabled;
        });
    },

    setLastActionMeta(action, status, sourceLabel = '운영 작업 상세') {
        const metaEl = document.getElementById('taskDetailStateMeta');
        if (!metaEl) return;
        metaEl.dataset.lastAction = action || '';
        metaEl.dataset.lastActionSource = sourceLabel || '운영 작업 상세';
        metaEl.dataset.lastActionStatus = status || '';
        metaEl.dataset.lastActionHistoryPath = this.buildHistoryPath();
        metaEl.dataset.lastActionLogPath = this.buildLogPathFromBase(this.state.currentDetail?.activityLogPath);
        this.renderLastActionNotice();
    },

    syncReturnContextMeta() {
        const metaEl = document.getElementById('taskDetailStateMeta');
        if (!metaEl) return;
        metaEl.dataset.returnTo = this.state.returnTo || '/admin/settings/tasks';
        metaEl.dataset.returnContext = this.resolveReturnContext();
        metaEl.dataset.sourceContext = this.state.source || '';
        CommonJS.renderSourceContextNotice({ noticeId: 'taskDetailSourceContextNotice', source: this.state.source });
    },

    syncReturnLinks() {
        const returnContext = CommonJS.getReturnContext(this.state.returnTo, '운영 작업');
        const breadcrumbLink = document.getElementById('taskDetailBreadcrumbLink');
        if (breadcrumbLink) {
            breadcrumbLink.href = this.state.returnTo;
            breadcrumbLink.textContent = returnContext.label;
        }
        const backButton = document.getElementById('btnBackToTaskList');
        if (backButton) {
            backButton.textContent = `${returnContext.label}로 돌아가기`;
        }
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
            'update-comment:success': '작업 메모를 수정했습니다.',
            'update-comment:error': '작업 메모 수정에 실패했습니다.',
            'delete-comment:success': '작업 메모를 삭제했습니다.',
            'delete-comment:error': '작업 메모 삭제에 실패했습니다.',
            'apply-recommendation:success': '추천 담당자 배정을 반영했습니다.',
            'apply-recommendation:error': '추천 담당자 배정에 실패했습니다.'
        };
        const variants = {
            'save-detail:success': 'alert-success',
            'toggle-status:success': 'alert-primary',
            'save-comment:success': 'alert-success',
            'update-comment:success': 'alert-success',
            'delete-comment:success': 'alert-warning',
            'apply-recommendation:success': 'alert-primary',
            'save-detail:error': 'alert-danger',
            'toggle-status:error': 'alert-danger',
            'save-comment:error': 'alert-danger',
            'update-comment:error': 'alert-danger',
            'delete-comment:error': 'alert-danger',
            'apply-recommendation:error': 'alert-danger'
        };

        const sourceMessage = source ? `${source}에서 실행` : '운영 작업 상세에서 실행';
        const message = templates[`${action}:${status}`] || '조치 결과를 확인해 주세요.';
        const variantClass = variants[`${action}:${status}`] || (status === 'success' ? 'alert-success' : 'alert-danger');
        CommonJS.renderActionNotice({
            noticeId: 'taskDetailActionNotice',
            textId: 'taskDetailActionNoticeText',
            actionsId: 'taskDetailActionNoticeActions',
            action,
            status,
            variantClass,
            message: `${sourceMessage} · ${message}`,
            actionsHtml: [
            historyPath ? `<a class="btn btn-sm btn-outline-secondary" href="${historyPath}">이력</a>` : '',
            logPath ? `<a class="btn btn-sm btn-outline-secondary" href="${logPath}">활동 로그</a>` : ''
            ].join('')
        });
    },

    hideLastActionNotice(clearMeta = false) {
        CommonJS.hideActionNotice({
            noticeId: 'taskDetailActionNotice',
            textId: 'taskDetailActionNoticeText',
            actionsId: 'taskDetailActionNoticeActions',
            metaId: 'taskDetailStateMeta',
            clearMeta,
            metaKeys: [
                'lastAction',
                'lastActionSource',
                'lastActionStatus',
                'lastActionHistoryPath',
                'lastActionLogPath'
            ]
        });
    },

    buildHistoryPath() {
        return this.buildHistoryPathFromBase(this.state.currentDetail?.historyPath);
    },

    buildHistoryPathFromBase(basePath) {
        if (!basePath) return '';
        const [path, rawQuery = ''] = basePath.split('?');
        const params = new URLSearchParams(rawQuery);
        params.set('returnTo', window.location.pathname + window.location.search);
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        return `${path}?${params.toString()}`;
    },

    buildLogPathFromBase(basePath) {
        if (!basePath) return '';
        const [path, rawQuery = ''] = basePath.split('?');
        const params = new URLSearchParams(rawQuery);
        params.set('returnTo', window.location.pathname + window.location.search);
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        return `${path}?${params.toString()}`;
    }
};

document.addEventListener('DOMContentLoaded', () => TaskDetailPage.init());
