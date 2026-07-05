const TaskList = {
    initialized: false,
    modal: null,
    operationPolicy: null,
    isSavingTask: false,
    isUpdatingStatus: false,
    isDeletingTask: false,
    isDuplicatingTask: false,
    isApplyingBulk: false,
    isBulkDuplicatingTask: false,
    isExportingTask: false,
    selectedTaskNos: new Set(),
    state: {
        page: 0,
        size: 10,
        keyword: '',
        status: '',
        priority: '',
        assigneeAdminNo: '',
        isPinned: '',
        commentedOnly: '',
        dueWithinDays: '',
        dueState: '',
        sortBy: 'PINNED_DUE',
        dueDateFrom: '',
        dueDateTo: '',
        overdueOnly: '',
        unassignedOnly: '',
        taskNo: '',
        openTaskNo: '',
        focusTaskNo: '',
        source: '',
        returnTo: ''
    },

    init() {
        if (this.initialized) return;
        this.initialized = true;
        const modalEl = document.getElementById('taskModal');
        if (modalEl) {
            this.modal = new bootstrap.Modal(modalEl);
        }
        this.bindEvents();
        this.readStateFromUrl();
        this.applyOperationPolicy();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));
        window.addEventListener('popstate', () => {
            this.readStateFromUrl();
            this.getList();
        });
        this.getList();
    },

    async applyOperationPolicy(settings = null) {
        try {
            this.operationPolicy = settings || await CommonJS.fetchSystemSettings();
            const disabled = CommonJS.isAdminWriteBlocked(this.operationPolicy);
            const reason = CommonJS.getAdminWriteBlockedReason('운영 작업 등록 및 수정');
            CommonJS.setButtonDisabled(document.getElementById('btnNewTask'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnSaveTask'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnApplyTaskBulk'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnBulkDuplicateTask'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnBulkDeleteTask'), disabled, reason);
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
    },

    bindEvents() {
        document.getElementById('btnNewTask')?.addEventListener('click', () => {
            void this.openModal();
        });
        document.getElementById('btnSaveTask')?.addEventListener('click', () => this.saveTask());
        document.getElementById('btnSearchTask')?.addEventListener('click', () => {
            this.state.page = 0;
            this.getList();
        });
        document.getElementById('btnResetTask')?.addEventListener('click', () => this.resetFilters());
        document.getElementById('btnExportTaskCsv')?.addEventListener('click', () => this.exportCsv());
        document.getElementById('btnApplyTaskBulk')?.addEventListener('click', () => this.applyBulkOperation());
        document.getElementById('btnBulkDuplicateTask')?.addEventListener('click', () => this.applyBulkDuplicate());
        document.getElementById('btnBulkDeleteTask')?.addEventListener('click', () => this.applyBulkDelete());
        document.getElementById('btnClearTaskSelection')?.addEventListener('click', () => this.clearSelection());
        document.getElementById('bulkTaskDueDateClear')?.addEventListener('change', (event) => {
            const dueDateInput = document.getElementById('bulkTaskDueDate');
            if (!dueDateInput) return;
            if (event.target.checked) {
                dueDateInput.value = '';
                dueDateInput.disabled = true;
            } else {
                dueDateInput.disabled = false;
            }
        });
        document.getElementById('taskSelectPage')?.addEventListener('change', (event) => this.toggleSelectCurrentPage(event.target.checked));
        document.getElementById('taskPageSize')?.addEventListener('change', () => {
            this.state.page = 0;
            this.updateStateFromInputs();
            this.getList();
        });
        document.getElementById('taskKeyword')?.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                this.state.page = 0;
                this.getList();
            }
        });
        document.getElementById('taskNoFilter')?.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                this.state.page = 0;
                this.getList();
            }
        });
        document.getElementById('taskStatTotalCard')?.addEventListener('click', () => this.applyStatFilter('total'));
        document.getElementById('taskStatTodoCard')?.addEventListener('click', () => this.applyStatFilter('TODO'));
        document.getElementById('taskStatProgressCard')?.addEventListener('click', () => this.applyStatFilter('IN_PROGRESS'));
        document.getElementById('taskStatOverdueCard')?.addEventListener('click', () => this.applyStatFilter('overdue'));
        document.getElementById('taskStatUnassignedCard')?.addEventListener('click', () => this.applyStatFilter('unassigned'));
        document.getElementById('taskListActionNoticeClose')?.addEventListener('click', () => this.hideLastActionNotice(true));
        document.getElementById('taskUnassignedOnly')?.addEventListener('change', (event) => {
            const assigneeFilter = document.getElementById('taskAssigneeFilter');
            if (!assigneeFilter) return;
            if (event.target.checked) {
                assigneeFilter.value = '';
                assigneeFilter.disabled = true;
            } else {
                assigneeFilter.disabled = false;
            }
        });
        document.getElementById('taskListBody')?.addEventListener('click', (event) => {
            const checkbox = event.target.closest('[data-role="select-task"]');
            if (checkbox) {
                const taskNo = this.normalizeOptionalPositiveNumber(checkbox.dataset.taskNo);
                if (taskNo == null) {
                    return;
                }
                this.toggleSelection(taskNo, checkbox.checked);
                return;
            }
            const editButton = event.target.closest('[data-role="edit-task"]');
            if (editButton) {
                const task = this.parseTaskDataset(editButton.dataset.task);
                if (!task) {
                    void CommonJS.alert('수정할 운영 작업 정보를 읽을 수 없습니다.', '알림', 'warning');
                    return;
                }
                void this.openEditModal(task);
                return;
            }

            const statusButton = event.target.closest('[data-role="update-task-status"]');
            if (statusButton) {
                const taskNo = this.normalizeOptionalPositiveNumber(statusButton.dataset.taskNo);
                if (taskNo == null) {
                    void CommonJS.alert('유효한 운영 작업 번호를 확인할 수 없습니다.', '알림', 'warning');
                    return;
                }
                if (!this.isValidTaskStatus(statusButton.dataset.status)) {
                    void CommonJS.alert('변경할 작업 상태 값이 올바르지 않습니다.', '알림', 'warning');
                    return;
                }
                this.updateStatus(taskNo, statusButton.dataset.status);
                return;
            }

            const duplicateButton = event.target.closest('[data-role="duplicate-task"]');
            if (duplicateButton) {
                const taskNo = this.normalizeOptionalPositiveNumber(duplicateButton.dataset.taskNo);
                if (taskNo == null) {
                    void CommonJS.alert('유효한 운영 작업 번호를 확인할 수 없습니다.', '알림', 'warning');
                    return;
                }
                this.duplicateTask(taskNo);
                return;
            }

            const deleteButton = event.target.closest('[data-role="delete-task"]');
            if (deleteButton) {
                const taskNo = this.normalizeOptionalPositiveNumber(deleteButton.dataset.taskNo);
                if (taskNo == null) {
                    void CommonJS.alert('유효한 운영 작업 번호를 확인할 수 없습니다.', '알림', 'warning');
                    return;
                }
                this.deleteTask(taskNo);
            }
        });
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        this.state.page = this.normalizePage(params.get('page'));
        this.state.size = this.normalizePageSize(params.get('size'));
        this.state.keyword = params.get('keyword') || '';
        this.state.status = params.get('status') || '';
        this.state.priority = params.get('priority') || '';
        this.state.assigneeAdminNo = this.normalizeOptionalPositiveNumber(params.get('assigneeAdminNo'))?.toString() || '';
        this.state.isPinned = params.get('isPinned') || '';
        this.state.commentedOnly = params.get('commentedOnly') || '';
        this.state.dueWithinDays = this.normalizeOptionalPositiveNumber(params.get('dueWithinDays'))?.toString() || '';
        this.state.dueState = params.get('dueState') || '';
        this.state.sortBy = params.get('sortBy') || 'PINNED_DUE';
        this.state.dueDateFrom = params.get('dueDateFrom') || '';
        this.state.dueDateTo = params.get('dueDateTo') || '';
        this.state.overdueOnly = params.get('overdueOnly') || '';
        this.state.unassignedOnly = params.get('unassignedOnly') || '';
        this.state.taskNo = this.normalizeOptionalPositiveNumber(params.get('taskNo'))?.toString() || '';
        this.state.openTaskNo = this.normalizeOptionalPositiveNumber(params.get('openTaskNo'))?.toString() || '';
        this.state.focusTaskNo = this.normalizeOptionalPositiveNumber(params.get('focusTaskNo'))?.toString() || '';
        this.state.source = params.get('source') || '';
        this.state.returnTo = params.get('returnTo') || '';
        document.getElementById('taskKeyword').value = this.state.keyword;
        document.getElementById('taskNoFilter').value = this.state.taskNo;
        document.getElementById('taskStatusFilter').value = this.state.status;
        document.getElementById('taskPriorityFilter').value = this.state.priority;
        document.getElementById('taskPinnedFilter').value = this.state.isPinned;
        document.getElementById('taskDueWithinDaysFilter').value = this.state.dueWithinDays;
        document.getElementById('taskDueStateFilter').value = this.state.dueState;
        document.getElementById('taskSortBy').value = this.state.sortBy;
        document.getElementById('taskDueDateFrom').value = this.state.dueDateFrom;
        document.getElementById('taskDueDateTo').value = this.state.dueDateTo;
        document.getElementById('taskPageSize').value = String(this.state.size);
        document.getElementById('taskOverdueOnly').checked = this.state.overdueOnly === 'Y';
        document.getElementById('taskUnassignedOnly').checked = this.state.unassignedOnly === 'Y';
        document.getElementById('taskCommentedOnly').value = this.state.commentedOnly;
        CommonJS.bindMainLogoNavigation(this.state.returnTo || '/admin/settings/tasks');
        CommonJS.renderSourceContextNotice({ noticeId: 'taskSourceContextNotice', source: this.state.source });
    },

    updateStateFromInputs() {
        this.state.keyword = document.getElementById('taskKeyword').value.trim();
        this.state.status = document.getElementById('taskStatusFilter').value;
        this.state.priority = document.getElementById('taskPriorityFilter').value;
        this.state.assigneeAdminNo = document.getElementById('taskAssigneeFilter').value;
        this.state.isPinned = document.getElementById('taskPinnedFilter').value;
        this.state.commentedOnly = document.getElementById('taskCommentedOnly')?.value || '';
        this.state.dueWithinDays = document.getElementById('taskDueWithinDaysFilter')?.value || '';
        this.state.dueState = document.getElementById('taskDueStateFilter').value;
        this.state.sortBy = document.getElementById('taskSortBy').value || 'PINNED_DUE';
        this.state.dueDateFrom = document.getElementById('taskDueDateFrom').value;
        this.state.dueDateTo = document.getElementById('taskDueDateTo').value;
        this.state.size = this.normalizePageSize(document.getElementById('taskPageSize').value);
        this.state.overdueOnly = document.getElementById('taskOverdueOnly')?.checked ? 'Y' : '';
        this.state.unassignedOnly = document.getElementById('taskUnassignedOnly')?.checked ? 'Y' : '';
        this.state.taskNo = this.parseOptionalNumber(document.getElementById('taskNoFilter')?.value)?.toString() || '';
        if (this.state.unassignedOnly === 'Y') {
            this.state.assigneeAdminNo = '';
        }
    },

    buildParams() {
        const params = new URLSearchParams();
        params.set('page', String(this.state.page));
        params.set('size', String(this.state.size));
        if (this.state.keyword) params.set('keyword', this.state.keyword);
        if (this.state.status) params.set('status', this.state.status);
        if (this.state.priority) params.set('priority', this.state.priority);
        if (this.state.assigneeAdminNo) params.set('assigneeAdminNo', this.state.assigneeAdminNo);
        if (this.state.isPinned) params.set('isPinned', this.state.isPinned);
        if (this.state.commentedOnly) params.set('commentedOnly', this.state.commentedOnly);
        if (this.state.dueWithinDays) params.set('dueWithinDays', this.state.dueWithinDays);
        if (this.state.dueState) params.set('dueState', this.state.dueState);
        if (this.state.sortBy && this.state.sortBy !== 'PINNED_DUE') params.set('sortBy', this.state.sortBy);
        if (this.state.dueDateFrom) params.set('dueDateFrom', this.state.dueDateFrom);
        if (this.state.dueDateTo) params.set('dueDateTo', this.state.dueDateTo);
        if (this.state.overdueOnly) params.set('overdueOnly', this.state.overdueOnly);
        if (this.state.unassignedOnly) params.set('unassignedOnly', this.state.unassignedOnly);
        if (this.state.taskNo) params.set('taskNo', this.state.taskNo);
        if (this.state.openTaskNo) params.set('openTaskNo', this.state.openTaskNo);
        if (this.state.focusTaskNo) params.set('focusTaskNo', this.state.focusTaskNo);
        if (this.state.source) params.set('source', this.state.source);
        if (this.state.returnTo) params.set('returnTo', this.state.returnTo);
        return params;
    },

    async getList() {
        try {
            this.updateStateFromInputs();
            this.validateState();
            if (this.hasInvalidDueDateRange()) {
                throw new Error('기한 시작일은 종료일보다 늦을 수 없습니다.');
            }
            const params = this.buildParams();
            history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
            this.setFilterMeta('적용 필터를 계산하는 중입니다...');
            this.setResultMeta('결과 메타를 계산하는 중입니다...');
            this.setPageMeta('페이지 메타를 계산하는 중입니다...');
            this.setListStateMeta('loading', '운영 작업을 불러오는 중입니다.', 0, 0, '');
            this.renderTableState('loading', '운영 작업을 불러오는 중입니다.', '현재 필터 기준 목록과 상태 요약을 함께 계산하고 있습니다.');

            const response = await fetch(`/api/admin/settings/tasks/list?${params.toString()}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 작업 목록을 불러오지 못했습니다.'));
            }

            const data = await response.json();
            this.renderAssigneeOptions(data.assigneeOptions || []);
            this.renderList(data.items || []);
            this.renderStats(data.taskStats);
            this.renderMeta(data);
            this.syncStatFilterState();
            this.renderPagination(data);
            await this.openDeepLinkedTaskIfNeeded(data.items || []);
        } catch (error) {
            document.getElementById('taskMetaText').textContent = error.message;
            this.setFilterMeta(error.message);
            this.setResultMeta('결과 메타 확인 불가');
            this.setPageMeta('페이지 메타 확인 불가');
            this.renderTableState('error', '운영 작업 목록을 불러오지 못했습니다.', error.message);
            document.getElementById('taskPagination').innerHTML = '';
            this.renderStats(null);
            this.setListStateMeta('error', error.message, 0, 0, '');
        }
    },

    async openDeepLinkedTaskIfNeeded(items) {
        if (!this.state.openTaskNo) {
            return;
        }
        const taskNo = Number(this.state.openTaskNo);
        const target = items.find((item) => item.taskNo === taskNo);
        if (target) {
            this.openEditModal(target);
        } else if (taskNo > 0) {
            try {
                const response = await fetch(`/api/admin/settings/tasks/${taskNo}`);
                if (response.ok) {
                    this.openEditModal(await response.json());
                }
            } catch (error) {
                console.error('딥링크 운영 작업 상세 로드 실패:', error);
            }
        }
        this.state.openTaskNo = '';
        const params = this.buildParams();
        history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
    },

    renderAssigneeOptions(options) {
        const filters = document.getElementById('taskAssigneeFilter');
        const form = document.getElementById('taskAssignee');
        const bulkAssignee = document.getElementById('bulkTaskAssignee');
        if (!filters || !form || !bulkAssignee) return;

        const selectedFilter = this.state.assigneeAdminNo || '';
        const selectedForm = form.value || '';
        const optionHtml = ['<option value="">전체</option>']
            .concat(options.map((option) => `<option value="${option.adminNo}">${this.escapeHtml(option.name)}</option>`))
            .join('');
        filters.innerHTML = optionHtml;
        filters.value = selectedFilter;
        filters.disabled = this.state.unassignedOnly === 'Y';

        const formOptionHtml = ['<option value="">미지정</option>']
            .concat(options.map((option) => `<option value="${option.adminNo}">${this.escapeHtml(option.name)}</option>`))
            .join('');
        form.innerHTML = formOptionHtml;
        form.value = selectedForm;
        const selectedBulk = bulkAssignee.value || '';
        bulkAssignee.innerHTML = ['<option value="">변경 안 함</option>', '<option value="__UNASSIGN__">담당 해제</option>']
            .concat(options.map((option) => `<option value="${option.adminNo}">${this.escapeHtml(option.name)}</option>`))
            .join('');
        bulkAssignee.value = selectedBulk;
    },

    renderList(items) {
        const tbody = document.getElementById('taskListBody');
        if (!tbody) return;
        const currentPath = encodeURIComponent(window.location.pathname + window.location.search);

        if (!items || items.length === 0) {
            this.renderTableState('empty', '등록된 운영 작업이 없습니다.', '상태, 담당자, 우선순위 조건을 조정하거나 새 운영 작업을 등록해 보세요.');
            this.setListStateMeta('empty', '등록된 운영 작업이 없습니다.', 0, 0, '');
            this.updateSelectionMeta([]);
            return;
        }

        tbody.innerHTML = items.map((item) => `
            <tr data-task-row="${item.taskNo}">
                <td class="ps-4">
                    <input type="checkbox" data-role="select-task" data-task-no="${item.taskNo}" ${this.selectedTaskNos.has(item.taskNo) ? 'checked' : ''}>
                </td>
                <td class="ps-4 text-muted small">${item.taskNo}</td>
                <td>
                    <div class="d-flex align-items-center gap-2 mb-1">
                        ${item.isPinned === 'Y' ? '<span class="badge text-bg-danger">고정</span>' : ''}
                        <a class="fw-bold text-dark text-decoration-none" href="${this.buildTaskDetailPath(item.taskNo, 'task-list-row-title')}">${this.escapeHtml(item.title)}</a>
                    </div>
                    <div class="small text-muted text-truncate" style="max-width: 440px;">${this.escapeHtml(item.description || '-')}</div>
                    ${Number(item.commentCount || 0) > 0 ? `
                        <div class="small text-muted mt-2">최근 메모 ${Number(item.commentCount || 0).toLocaleString()}건</div>
                        <div class="small text-dark text-truncate" style="max-width: 440px;">${this.escapeHtml(item.latestCommentPreview || '-')}</div>
                        <div class="small text-muted">${this.escapeHtml(item.latestCommentMeta || '-')}</div>
                    ` : ''}
                </td>
                <td class="text-center">
                    <span class="badge rounded-pill ${this.resolveStatusBadgeClass(item.status)}">${item.statusLabel}</span>
                </td>
                <td class="text-center">
                    <span class="badge rounded-pill ${this.resolvePriorityBadgeClass(item.priority)}">${item.priorityLabel}</span>
                </td>
                <td>${this.escapeHtml(item.assigneeAdminName)}</td>
                <td>
                    <div class="small">${item.dueDate}</div>
                    <div class="small text-muted">${item.dueState}</div>
                </td>
                <td class="text-end pe-4">
                    <button class="btn btn-sm btn-outline-primary me-1" data-role="edit-task" data-task='${JSON.stringify(item).replace(/'/g, '&#39;')}'>수정</button>
                    <button class="btn btn-sm btn-outline-secondary me-1" data-role="duplicate-task" data-task-no="${item.taskNo}">복제</button>
                    <a class="btn btn-sm btn-outline-secondary me-1" href="${this.buildTaskHistoryPathFromBase(item.historyPath)}">${item.historyLabel}</a>
                    <a class="btn btn-sm btn-outline-secondary me-1" href="${this.buildTaskLogPathFromBase(item.activityLogPath, item.taskNo)}">${item.activityLogLabel}</a>
                    <div class="btn-group">
                        <button class="btn btn-sm btn-outline-dark dropdown-toggle" data-bs-toggle="dropdown">상태</button>
                        <ul class="dropdown-menu dropdown-menu-end">
                            ${this.renderStatusMenu(item)}
                        </ul>
                    </div>
                    <button class="btn btn-sm btn-outline-danger ms-1" data-role="delete-task" data-task-no="${item.taskNo}">삭제</button>
                </td>
            </tr>
        `).join('');

        this.setListStateMeta('ready', '', items.length, null, null);
        this.highlightFocusedTaskRow();
        this.updateSelectionMeta(items);
    },

    renderStatusMenu(item) {
        return [
            ['TODO', '대기'],
            ['IN_PROGRESS', '진행중'],
            ['DONE', '완료'],
            ['HOLD', '보류']
        ].map(([value, label]) => `
            <li><button type="button" class="dropdown-item ${item.status === value ? 'active' : ''}" data-role="update-task-status" data-task-no="${item.taskNo}" data-status="${value}">${label}</button></li>
        `).join('');
    },

    renderTableState(type, title, description) {
        const tbody = document.getElementById('taskListBody');
        if (!tbody) return;

        const content = type === 'loading'
            ? `
                <div class="product-loading-state">
                    <div class="spinner-border spinner-border-sm text-primary" role="status" aria-hidden="true"></div>
                    <strong>${this.escapeHtml(title)}</strong>
                    <p>${this.escapeHtml(description)}</p>
                </div>
            `
            : `
                <div class="product-empty-state">
                    <div class="product-empty-state__icon ${type === 'error' ? 'text-danger' : 'text-primary'}">
                        <i class="fa-solid ${type === 'error' ? 'fa-triangle-exclamation' : 'fa-list-check'}"></i>
                    </div>
                    <strong>${this.escapeHtml(title)}</strong>
                    <p>${this.escapeHtml(description)}</p>
                </div>
            `;

        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="py-5">
                    ${content}
                </td>
            </tr>
        `;
    },

    renderStats(stats) {
        const totalCountEl = document.getElementById('taskTotalCount');
        const todoCountEl = document.getElementById('taskTodoCount');
        const inProgressCountEl = document.getElementById('taskInProgressCount');
        const overdueCountEl = document.getElementById('taskOverdueCount');
        const unassignedCountEl = document.getElementById('taskUnassignedCount');
        const contextTextEl = document.getElementById('taskStatsContextText');
        const noticeEl = document.getElementById('taskStatsNotice');

        if (!stats) {
            totalCountEl.innerText = '0';
            todoCountEl.innerText = '0';
            inProgressCountEl.innerText = '0';
            overdueCountEl.innerText = '0';
            unassignedCountEl.innerText = '0';
            contextTextEl.innerText = '카드 기준을 확인할 수 없습니다.';
            noticeEl.innerText = '카드 기준을 확인할 수 없습니다.';
            noticeEl.dataset.statsContext = 'error';
            return;
        }

        totalCountEl.innerText = Number(stats.totalCount || 0).toLocaleString();
        todoCountEl.innerText = Number(stats.todoCount || 0).toLocaleString();
        inProgressCountEl.innerText = Number(stats.inProgressCount || 0).toLocaleString();
        overdueCountEl.innerText = Number(stats.overdueCount || 0).toLocaleString();
        unassignedCountEl.innerText = Number(stats.unassignedCount || 0).toLocaleString();
        contextTextEl.innerText = `${stats.contextLabel} · ${stats.querySignature}`;
        const usingQuickFilter = !!this.state.status
            || !!this.state.priority
            || !!this.state.isPinned
            || !!this.state.dueState
            || !!this.state.overdueOnly
            || !!this.state.commentedOnly
            || !!this.state.dueWithinDays
            || this.state.sortBy !== 'PINNED_DUE';
        noticeEl.innerText = usingQuickFilter
            ? '카드 수치는 기본 탐색 문맥 기준이며, 선택한 빠른 필터는 목록에만 적용됩니다.'
            : '카드 수치는 현재 탐색 문맥 기준입니다.';
        noticeEl.dataset.statsContext = usingQuickFilter ? 'base-query' : 'current-query';
    },

    renderMeta(data) {
        CommonJS.renderListMeta({
            metaTextId: 'taskMetaText',
            filterMetaId: 'taskFilterMeta',
            resultMetaId: 'taskResultMeta',
            pageMetaId: 'taskPageMeta',
            resultLabel: data.resultMeta?.resultLabel || `${data.totalElements || 0}건 조회`,
            filterCount: data.resultMeta?.appliedFilterCount ?? 0,
            querySignature: data.resultMeta?.querySignature || '고정 우선 · 마감 임박 순',
            pageInfoLabel: data.resultMeta?.pageInfoLabel || '',
            filterPrefix: '필터',
            defaultResultText: '결과 메타 없음',
            defaultPageText: '페이지 메타 없음'
        });
        this.setListStateMeta('ready', '', (data.items || []).length, data.totalElements || 0, data.resultMeta?.querySignature || '');
        const metaEl = document.getElementById('taskListStateMeta');
        if (metaEl) {
            metaEl.dataset.pageInfoLabel = data.resultMeta?.pageInfoLabel || '';
        }
    },

    renderPagination(data) {
        const paginationEl = document.getElementById('taskPagination');
        if (!paginationEl) return;
        const totalPages = Number(data.totalPages || 0);
        const currentPage = Number(data.currentPage || 0);
        if (totalPages <= 1) {
            paginationEl.innerHTML = '';
            return;
        }

        paginationEl.innerHTML = Array.from({length: totalPages}, (_, index) => `
            <li class="page-item ${index === currentPage ? 'active' : ''}">
                <button type="button" class="page-link" data-role="go-task-page" data-page="${index}">${index + 1}</button>
            </li>
        `).join('');

        paginationEl.querySelectorAll('[data-role="go-task-page"]').forEach((button) => {
            button.addEventListener('click', () => this.goPage(this.normalizePage(button.dataset.page)));
        });
    },

    async openModal() {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 작업 등록 및 수정'), '알림', 'warning');
            return;
        }
        document.getElementById('taskModalTitle').textContent = '운영 작업 등록';
        document.getElementById('taskNo').value = '';
        document.getElementById('taskTitle').value = '';
        document.getElementById('taskDescription').value = '';
        document.getElementById('taskStatus').value = 'TODO';
        document.getElementById('taskPriority').value = 'HIGH';
        document.getElementById('taskAssignee').value = '';
        document.getElementById('taskDueDate').value = '';
        document.getElementById('taskIsPinned').value = 'N';
        this.modal?.show();
    },

    async openEditModal(task) {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 작업 등록 및 수정'), '알림', 'warning');
            return;
        }
        document.getElementById('taskModalTitle').textContent = '운영 작업 수정';
        document.getElementById('taskNo').value = task.taskNo;
        document.getElementById('taskTitle').value = task.title || '';
        document.getElementById('taskDescription').value = task.description === '-' ? '' : (task.description || '');
        document.getElementById('taskStatus').value = task.status || 'TODO';
        document.getElementById('taskPriority').value = task.priority || 'MEDIUM';
        document.getElementById('taskAssignee').value = task.assigneeAdminNo || '';
        document.getElementById('taskDueDate').value = task.dueDate && task.dueDate !== '-' ? task.dueDate : '';
        document.getElementById('taskIsPinned').value = task.isPinned || 'N';
        this.modal?.show();
    },

    async saveTask() {
        if (this.isSavingTask) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 작업 등록 및 수정'), '알림', 'warning');
            return;
        }

        const title = CommonJS.normalizeRequiredText(document.getElementById('taskTitle')?.value || '');
        if (!title) {
            await CommonJS.alert('작업 제목을 입력해주세요.', '알림', 'warning');
            return;
        }

        const payload = {
            taskNo: this.parseOptionalNumber(document.getElementById('taskNo')?.value),
            title,
            description: CommonJS.normalizeOptionalText(document.getElementById('taskDescription')?.value || ''),
            status: document.getElementById('taskStatus')?.value || 'TODO',
            priority: document.getElementById('taskPriority')?.value || 'MEDIUM',
            assigneeAdminNo: this.parseOptionalNumber(document.getElementById('taskAssignee')?.value),
            dueDate: document.getElementById('taskDueDate')?.value || null,
            isPinned: document.getElementById('taskIsPinned')?.value || 'N'
        };

        try {
            this.isSavingTask = true;
            this.setBusyButton(document.getElementById('btnSaveTask'), true, '저장 중...');
            const response = await fetch('/api/admin/settings/tasks/save', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(payload)
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 작업 저장에 실패했습니다.'));
            }
            this.setLastActionMeta('save-task', 'success', payload.taskNo ? '목록 수정' : '목록 등록', payload.taskNo);
            this.modal?.hide();
            await this.getList();
            await CommonJS.alert(payload.taskNo ? '운영 작업이 수정되었습니다.' : '운영 작업이 등록되었습니다.', '성공', 'success');
        } catch (error) {
            this.setLastActionMeta('save-task', 'error', payload.taskNo ? '목록 수정' : '목록 등록', payload.taskNo);
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isSavingTask = false;
            this.setBusyButton(document.getElementById('btnSaveTask'), false);
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    async updateStatus(taskNo, status) {
        if (!this.isPositiveNumber(taskNo)) {
            await CommonJS.alert('유효한 운영 작업 번호를 확인할 수 없습니다.', '알림', 'warning');
            return;
        }
        if (!this.isValidTaskStatus(status)) {
            await CommonJS.alert('변경할 작업 상태 값이 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        if (this.isUpdatingStatus) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 작업 상태 변경'), '알림', 'warning');
            return;
        }
        try {
            this.isUpdatingStatus = true;
            this.setCollectionButtonsDisabled('[data-role="update-task-status"]', true);
            const response = await fetch(`/api/admin/settings/tasks/status/${taskNo}?status=${encodeURIComponent(status)}`, {method: 'PATCH'});
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '상태 변경에 실패했습니다.'));
            }
            this.setLastActionMeta('update-status', 'success', '목록 상태 변경', taskNo);
            await this.getList();
            await CommonJS.alert('운영 작업 상태가 변경되었습니다.', '성공', 'success');
        } catch (error) {
            this.setLastActionMeta('update-status', 'error', '목록 상태 변경', taskNo);
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isUpdatingStatus = false;
            this.setCollectionButtonsDisabled('[data-role="update-task-status"]', false);
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    async deleteTask(taskNo) {
        if (!this.isPositiveNumber(taskNo)) {
            await CommonJS.alert('유효한 운영 작업 번호를 확인할 수 없습니다.', '알림', 'warning');
            return;
        }
        if (this.isDeletingTask) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 작업 삭제'), '알림', 'warning');
            return;
        }
        const confirmed = await CommonJS.confirm('운영 작업을 삭제하시겠습니까?', '삭제 확인');
        if (!confirmed) {
            return;
        }
        try {
            this.isDeletingTask = true;
            this.setCollectionButtonsDisabled('[data-role="delete-task"]', true);
            const response = await fetch(`/api/admin/settings/tasks/delete?no=${taskNo}`, {method: 'DELETE'});
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 작업 삭제에 실패했습니다.'));
            }
            this.setLastActionMeta('delete-task', 'success', '목록 삭제', taskNo);
            await this.getList();
            await CommonJS.alert('운영 작업이 삭제되었습니다.', '성공', 'success');
        } catch (error) {
            this.setLastActionMeta('delete-task', 'error', '목록 삭제', taskNo);
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isDeletingTask = false;
            this.setCollectionButtonsDisabled('[data-role="delete-task"]', false);
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    async duplicateTask(taskNo) {
        if (!this.isPositiveNumber(taskNo)) {
            await CommonJS.alert('유효한 운영 작업 번호를 확인할 수 없습니다.', '알림', 'warning');
            return;
        }
        if (this.isDuplicatingTask) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 작업 복제'), '알림', 'warning');
            return;
        }
        try {
            this.isDuplicatingTask = true;
            this.setCollectionButtonsDisabled('[data-role="duplicate-task"]', true);
            const response = await fetch(`/api/admin/settings/tasks/${taskNo}/duplicate`, {method: 'POST'});
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 작업 복제에 실패했습니다.'));
            }
            const result = await response.json();
            this.state.focusTaskNo = result.taskNo ? String(result.taskNo) : '';
            this.setLastActionMeta('duplicate-task', 'success', '목록 복제', result.taskNo || taskNo);
            await this.getList();
            await CommonJS.alert(`운영 작업 #${result.taskNo}가 복제 등록되었습니다.`, '성공', 'success');
        } catch (error) {
            this.setLastActionMeta('duplicate-task', 'error', '목록 복제', taskNo);
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isDuplicatingTask = false;
            this.setCollectionButtonsDisabled('[data-role="duplicate-task"]', false);
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    async applyBulkOperation() {
        if (this.isApplyingBulk) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 작업 일괄 변경'), '알림', 'warning');
            return;
        }
        if (this.selectedTaskNos.size === 0) {
            await CommonJS.alert('선택된 운영 작업이 없습니다.', '알림', 'warning');
            return;
        }

        const payload = {
            taskNos: Array.from(this.selectedTaskNos),
            status: document.getElementById('bulkTaskStatus')?.value || null,
            priority: document.getElementById('bulkTaskPriority')?.value || null,
            assigneeAdminNo: this.resolveBulkAssigneeAdminNo(),
            assigneeMode: this.resolveBulkAssigneeMode(),
            isPinned: document.getElementById('bulkTaskPinned')?.value || null,
            dueDate: this.resolveBulkDueDate(),
            dueDateMode: this.resolveBulkDueDateMode()
        };

        if (!payload.status && !payload.priority && !payload.assigneeAdminNo && !payload.assigneeMode && !payload.isPinned && !payload.dueDate && !payload.dueDateMode) {
            await CommonJS.alert('일괄 변경할 항목을 선택하세요.', '알림', 'warning');
            return;
        }

        try {
            this.isApplyingBulk = true;
            this.setBusyButton(document.getElementById('btnApplyTaskBulk'), true, '적용 중...');
            const response = await fetch('/api/admin/settings/tasks/bulk-operate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 작업 일괄 변경에 실패했습니다.'));
            }

            const result = await response.json();
            this.setLastActionMeta('bulk-operate', 'success', '목록 일괄 변경');
            if (result.updatedCount > 0) {
                this.clearSelection(false);
            }
            await this.getList();
            await CommonJS.alert(`요청 ${result.requestedCount}건 · 변경 ${result.updatedCount}건 · 유지 ${result.unchangedCount}건`, '일괄 변경 결과', 'success');
        } catch (error) {
            this.setLastActionMeta('bulk-operate', 'error', '목록 일괄 변경');
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isApplyingBulk = false;
            this.setBusyButton(document.getElementById('btnApplyTaskBulk'), false);
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    async applyBulkDuplicate() {
        const taskNos = Array.from(this.selectedTaskNos);
        if (taskNos.length === 0) {
            await CommonJS.alert('복제할 운영 작업을 선택하세요.', '알림', 'warning');
            return;
        }
        if (this.isBulkDuplicatingTask) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 작업 일괄 복제'), '알림', 'warning');
            return;
        }
        try {
            this.isBulkDuplicatingTask = true;
            this.setBusyButton(document.getElementById('btnBulkDuplicateTask'), true, '복제 중...');
            const response = await fetch('/api/admin/settings/tasks/bulk-duplicate', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({taskNos})
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 작업 일괄 복제에 실패했습니다.'));
            }
            const result = await response.json();
            const createdTaskNos = Array.isArray(result.createdTaskNos) ? result.createdTaskNos : [];
            this.state.focusTaskNo = createdTaskNos.length > 0 ? String(createdTaskNos[0]) : '';
            this.setLastActionMeta('bulk-duplicate', 'success', '목록 일괄 복제', createdTaskNos[0] || null);
            await this.getList();
            await CommonJS.alert(`선택한 운영 작업 ${Number(result.createdCount || 0).toLocaleString()}건을 복제했습니다.`, '성공', 'success');
        } catch (error) {
            this.setLastActionMeta('bulk-duplicate', 'error', '목록 일괄 복제');
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isBulkDuplicatingTask = false;
            this.setBusyButton(document.getElementById('btnBulkDuplicateTask'), false);
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    async applyBulkDelete() {
        if (this.isApplyingBulk) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 작업 일괄 삭제'), '알림', 'warning');
            return;
        }
        if (this.selectedTaskNos.size === 0) {
            await CommonJS.alert('삭제할 운영 작업을 선택하세요.', '알림', 'warning');
            return;
        }

        const confirmed = await CommonJS.confirm(`선택한 운영 작업 ${this.selectedTaskNos.size}건을 삭제하시겠습니까?`);
        if (!confirmed) {
            return;
        }

        try {
            this.isApplyingBulk = true;
            this.setBusyButton(document.getElementById('btnBulkDeleteTask'), true, '삭제 중...');
            const response = await fetch('/api/admin/settings/tasks/bulk-delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ taskNos: Array.from(this.selectedTaskNos) })
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 작업 일괄 삭제에 실패했습니다.'));
            }

            const result = await response.json();
            this.setLastActionMeta('bulk-delete', 'success', '목록 일괄 삭제');
            this.clearSelection(false);
            await this.getList();
            await CommonJS.alert(`요청 ${result.requestedCount}건 · 삭제 ${result.deletedCount}건 · 누락 ${result.missingCount}건`, '일괄 삭제 결과', 'success');
        } catch (error) {
            this.setLastActionMeta('bulk-delete', 'error', '목록 일괄 삭제');
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isApplyingBulk = false;
            this.setBusyButton(document.getElementById('btnBulkDeleteTask'), false);
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    toggleSelection(taskNo, checked) {
        if (checked) {
            this.selectedTaskNos.add(taskNo);
        } else {
            this.selectedTaskNos.delete(taskNo);
        }
        this.updateSelectionMetaFromDom();
    },

    toggleSelectCurrentPage(checked) {
        const currentPageItems = this.getCurrentPageTaskNos();
        currentPageItems.forEach((taskNo) => {
            if (checked) {
                this.selectedTaskNos.add(taskNo);
            } else {
                this.selectedTaskNos.delete(taskNo);
            }
        });
        document.querySelectorAll('[data-role="select-task"]').forEach((checkbox) => {
            checkbox.checked = checked;
        });
        this.updateSelectionMetaFromDom();
    },

    clearSelection(reload = true) {
        this.selectedTaskNos.clear();
        document.querySelectorAll('[data-role="select-task"]').forEach((checkbox) => {
            checkbox.checked = false;
        });
        const selectPage = document.getElementById('taskSelectPage');
        if (selectPage) {
            selectPage.checked = false;
            selectPage.indeterminate = false;
        }
        this.updateSelectionMetaFromDom();
        if (reload) {
            document.getElementById('bulkTaskStatus').value = '';
            document.getElementById('bulkTaskPriority').value = '';
            document.getElementById('bulkTaskAssignee').value = '';
            document.getElementById('bulkTaskPinned').value = '';
            document.getElementById('bulkTaskDueDate').value = '';
            document.getElementById('bulkTaskDueDate').disabled = false;
            document.getElementById('bulkTaskDueDateClear').checked = false;
        }
    },

    updateSelectionMeta(items) {
        const selectionMeta = document.getElementById('taskSelectionMeta');
        const selectedCount = this.selectedTaskNos.size;
        const currentPageSelectedCount = (items || []).filter((item) => this.selectedTaskNos.has(item.taskNo)).length;
        const selectPage = document.getElementById('taskSelectPage');
        if (selectPage && items && items.length > 0) {
            selectPage.checked = currentPageSelectedCount === items.length;
            selectPage.indeterminate = currentPageSelectedCount > 0 && currentPageSelectedCount < items.length;
        } else if (selectPage) {
            selectPage.checked = false;
            selectPage.indeterminate = false;
        }

        if (selectionMeta) {
            if (selectedCount === 0) {
                selectionMeta.textContent = '선택된 작업이 없습니다.';
            } else {
                selectionMeta.textContent = `선택 ${selectedCount}건 · 현재 페이지 ${currentPageSelectedCount}건`;
            }
        }
    },

    updateSelectionMetaFromDom() {
        const items = Array.from(document.querySelectorAll('[data-role="select-task"]')).map((checkbox) => ({
            taskNo: this.normalizeOptionalPositiveNumber(checkbox.dataset.taskNo)
        })).filter((item) => item.taskNo != null);
        this.updateSelectionMeta(items);
    },

    getCurrentPageTaskNos() {
        return Array.from(document.querySelectorAll('[data-role="select-task"]'))
            .map((checkbox) => this.normalizeOptionalPositiveNumber(checkbox.dataset.taskNo))
            .filter((taskNo) => taskNo != null);
    },

    applyStatFilter(type) {
        this.state.page = 0;
        if (type === 'total') {
            this.state.status = '';
            this.state.priority = '';
            this.state.overdueOnly = '';
            this.state.unassignedOnly = '';
            this.state.dueState = '';
        } else if (type === 'overdue') {
            this.state.status = '';
            this.state.priority = '';
            this.state.overdueOnly = 'Y';
            this.state.unassignedOnly = '';
            this.state.dueState = '';
        } else if (type === 'unassigned') {
            this.state.status = '';
            this.state.priority = '';
            this.state.overdueOnly = '';
            this.state.unassignedOnly = 'Y';
            this.state.assigneeAdminNo = '';
            this.state.dueState = '';
        } else {
            this.state.status = type;
            this.state.priority = '';
            this.state.overdueOnly = '';
            this.state.unassignedOnly = '';
            this.state.dueState = '';
        }
        this.syncStatFilterState(type);
        document.getElementById('taskStatusFilter').value = this.state.status;
        document.getElementById('taskPriorityFilter').value = this.state.priority;
        document.getElementById('taskDueStateFilter').value = this.state.dueState;
        document.getElementById('taskOverdueOnly').checked = this.state.overdueOnly === 'Y';
        document.getElementById('taskUnassignedOnly').checked = this.state.unassignedOnly === 'Y';
        const assigneeFilter = document.getElementById('taskAssigneeFilter');
        if (assigneeFilter) {
            assigneeFilter.disabled = this.state.unassignedOnly === 'Y';
            if (this.state.unassignedOnly === 'Y') {
                assigneeFilter.value = '';
            }
        }
        this.getList();
    },

    goPage(page) {
        if (!Number.isInteger(page) || page < 0) {
            void CommonJS.alert('이동할 페이지 정보가 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        this.state.page = page;
        this.getList();
    },

    resetFilters() {
        document.getElementById('taskKeyword').value = '';
        document.getElementById('taskNoFilter').value = '';
        document.getElementById('taskStatusFilter').value = '';
        document.getElementById('taskPriorityFilter').value = '';
        document.getElementById('taskAssigneeFilter').value = '';
        document.getElementById('taskPinnedFilter').value = '';
        document.getElementById('taskDueStateFilter').value = '';
        document.getElementById('taskSortBy').value = 'PINNED_DUE';
        document.getElementById('taskDueDateFrom').value = '';
        document.getElementById('taskDueDateTo').value = '';
        document.getElementById('taskAssigneeFilter').disabled = false;
        document.getElementById('taskOverdueOnly').checked = false;
        document.getElementById('taskUnassignedOnly').checked = false;
        document.getElementById('taskCommentedOnly').value = '';
        document.getElementById('taskDueWithinDaysFilter').value = '';
        document.getElementById('taskPageSize').value = '10';
        this.state.page = 0;
        this.state.size = 10;
        this.state.keyword = '';
        this.state.status = '';
        this.state.priority = '';
        this.state.assigneeAdminNo = '';
        this.state.isPinned = '';
        this.state.commentedOnly = '';
        this.state.dueWithinDays = '';
        this.state.dueState = '';
        this.state.sortBy = 'PINNED_DUE';
        this.state.dueDateFrom = '';
        this.state.dueDateTo = '';
        this.state.overdueOnly = '';
        this.state.unassignedOnly = '';
        this.state.taskNo = '';
        this.state.openTaskNo = '';
        this.state.focusTaskNo = '';
        this.clearSelection(false);
        this.syncStatFilterState('total');
        this.getList();
    },

    async exportCsv() {
        if (this.isExportingTask) {
            return;
        }
        const button = document.getElementById('btnExportTaskCsv');
        try {
            this.isExportingTask = true;
            CommonJS.setButtonDisabled(button, true, '내보내는 중입니다.');
            this.updateStateFromInputs();
            this.validateState();
            if (this.hasInvalidDueDateRange()) {
                throw new Error('기한 시작일은 종료일보다 늦을 수 없습니다.');
            }
            const params = this.buildParams();
            params.delete('page');
            params.delete('size');
            await CommonJS.downloadFile(`/api/admin/settings/tasks/export?${params.toString()}`, 'tasks.csv');
        } catch (error) {
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isExportingTask = false;
            CommonJS.setButtonDisabled(button, false);
        }
    },

    setFilterMeta(message) {
        document.getElementById('taskFilterMeta').textContent = message;
    },

    setResultMeta(message) {
        document.getElementById('taskResultMeta').textContent = message;
    },

    setPageMeta(message) {
        document.getElementById('taskPageMeta').textContent = message;
    },

    setListStateMeta(state, message, visibleCount, totalElements, querySignature) {
        const metaEl = document.getElementById('taskListStateMeta');
        if (!metaEl) return;
        metaEl.dataset.listState = state;
        metaEl.dataset.stateMessage = message || '';
        if (visibleCount != null) metaEl.dataset.visibleCount = String(visibleCount);
        if (totalElements != null) metaEl.dataset.totalElements = String(totalElements);
        if (querySignature != null) metaEl.dataset.querySignature = querySignature;
        metaEl.dataset.activeStatFilter = this.resolveActiveStatFilter();
        metaEl.dataset.highlightTaskNo = this.state.focusTaskNo || '';
        metaEl.dataset.sourceContext = this.state.source || '';
        CommonJS.renderSourceContextNotice({ noticeId: 'taskSourceContextNotice', source: this.state.source });
    },

    hasInvalidDueDateRange() {
        return !!this.state.dueDateFrom && !!this.state.dueDateTo && this.state.dueDateFrom > this.state.dueDateTo;
    },

    resolveActiveStatFilter() {
        if (this.state.unassignedOnly === 'Y') return 'unassigned';
        if (this.state.overdueOnly === 'Y') return 'overdue';
        if (this.state.status === 'TODO') return 'TODO';
        if (this.state.status === 'IN_PROGRESS') return 'IN_PROGRESS';
        return 'total';
    },

    syncStatFilterState(activeType = null) {
        const currentType = activeType || this.resolveActiveStatFilter();
        const mapping = {
            total: 'taskStatTotalCard',
            TODO: 'taskStatTodoCard',
            IN_PROGRESS: 'taskStatProgressCard',
            overdue: 'taskStatOverdueCard',
            unassigned: 'taskStatUnassignedCard'
        };
        Object.entries(mapping).forEach(([type, id]) => {
            const card = document.getElementById(id);
            if (!card) return;
            const active = type === currentType;
            card.classList.toggle('border-dark', active);
            card.classList.toggle('shadow', active);
            card.dataset.active = active ? 'Y' : 'N';
        });
    },

    highlightFocusedTaskRow() {
        const focusTaskNo = Number(this.state.focusTaskNo || 0);
        document.querySelectorAll('[data-task-row]').forEach((row) => {
            row.classList.remove('table-warning');
        });
        if (!focusTaskNo) {
            return;
        }
        const row = document.querySelector(`[data-task-row="${focusTaskNo}"]`);
        if (!row) {
            return;
        }
        row.classList.add('table-warning');
        row.scrollIntoView({ block: 'center', behavior: 'smooth' });
        this.state.focusTaskNo = '';
        const metaEl = document.getElementById('taskListStateMeta');
        if (metaEl) {
            metaEl.dataset.highlightTaskNo = '';
        }
        const params = this.buildParams();
        history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
    },

    setLastActionMeta(action, status, sourceLabel, taskNo = null) {
        const metaEl = document.getElementById('taskListStateMeta');
        if (!metaEl) return;
        metaEl.dataset.lastAction = action || '';
        metaEl.dataset.lastActionSource = sourceLabel || '운영 작업 목록';
        metaEl.dataset.lastActionStatus = status || '';
        metaEl.dataset.lastActionTaskNo = taskNo == null ? '' : String(taskNo);
        metaEl.dataset.lastActionHistoryPath = taskNo == null ? '' : this.buildTaskHistoryPath(taskNo);
        metaEl.dataset.lastActionLogPath = taskNo == null ? '' : this.buildTaskLogPath(taskNo);
        this.renderLastActionNotice();
    },

    renderLastActionNotice() {
        const metaEl = document.getElementById('taskListStateMeta');
        const noticeEl = document.getElementById('taskListActionNotice');
        const noticeTextEl = document.getElementById('taskListActionNoticeText');
        const noticeActionsEl = document.getElementById('taskListActionNoticeActions');
        if (!metaEl || !noticeEl || !noticeTextEl || !noticeActionsEl) return;

        const action = metaEl.dataset.lastAction || '';
        const source = metaEl.dataset.lastActionSource || '';
        const status = metaEl.dataset.lastActionStatus || '';
        const taskNo = metaEl.dataset.lastActionTaskNo || '';
        const historyPath = metaEl.dataset.lastActionHistoryPath || '';
        const logPath = metaEl.dataset.lastActionLogPath || '';
        const taskLabel = taskNo ? `작업 #${taskNo}` : '운영 작업';

        if (!action || !status) {
            this.hideLastActionNotice(false);
            return;
        }

        const templates = {
            'save-task:success': taskNo ? `${taskLabel} 저장을 반영했습니다.` : '운영 작업 저장을 반영했습니다.',
            'save-task:error': '운영 작업 저장에 실패했습니다.',
            'update-status:success': `${taskLabel} 상태를 변경했습니다.`,
            'update-status:error': `${taskLabel} 상태 변경에 실패했습니다.`,
            'delete-task:success': `${taskLabel} 삭제를 반영했습니다.`,
            'delete-task:error': `${taskLabel} 삭제에 실패했습니다.`,
            'duplicate-task:success': `${taskLabel} 복제를 반영했습니다.`,
            'duplicate-task:error': `${taskLabel} 복제에 실패했습니다.`,
            'bulk-duplicate:success': '선택한 운영 작업 복제를 반영했습니다.',
            'bulk-duplicate:error': '운영 작업 일괄 복제에 실패했습니다.',
            'bulk-operate:success': '선택한 운영 작업 일괄 변경을 반영했습니다.',
            'bulk-operate:error': '운영 작업 일괄 변경에 실패했습니다.',
            'bulk-delete:success': '선택한 운영 작업 일괄 삭제를 반영했습니다.',
            'bulk-delete:error': '운영 작업 일괄 삭제에 실패했습니다.'
        };
        const variants = {
            'save-task:success': 'alert-success',
            'update-status:success': 'alert-primary',
            'delete-task:success': 'alert-warning',
            'duplicate-task:success': 'alert-info',
            'bulk-duplicate:success': 'alert-info',
            'bulk-operate:success': 'alert-primary',
            'bulk-delete:success': 'alert-warning',
            'save-task:error': 'alert-danger',
            'update-status:error': 'alert-danger',
            'delete-task:error': 'alert-danger',
            'duplicate-task:error': 'alert-danger',
            'bulk-duplicate:error': 'alert-danger',
            'bulk-operate:error': 'alert-danger',
            'bulk-delete:error': 'alert-danger'
        };

        const sourceMessage = source ? `${source}에서 실행` : '운영 작업 목록에서 실행';
        const message = templates[`${action}:${status}`] || '조치 결과를 확인해 주세요.';
        const variantClass = variants[`${action}:${status}`] || (status === 'success' ? 'alert-success' : 'alert-danger');
        CommonJS.renderActionNotice({
            noticeId: 'taskListActionNotice',
            textId: 'taskListActionNoticeText',
            actionsId: 'taskListActionNoticeActions',
            action,
            status,
            variantClass,
            message: `${sourceMessage} · ${message}`,
            actionsHtml: [
            taskNo ? `<a class="btn btn-sm btn-outline-dark" href="${this.buildTaskDetailPath(taskNo, 'task-list-action-notice')}">작업 열기</a>` : '',
            historyPath ? `<a class="btn btn-sm btn-outline-secondary" href="${historyPath}">이력</a>` : '',
            logPath ? `<a class="btn btn-sm btn-outline-secondary" href="${logPath}">활동 로그</a>` : ''
            ].join('')
        });
    },

    hideLastActionNotice(clearMeta = false) {
        CommonJS.hideActionNotice({
            noticeId: 'taskListActionNotice',
            textId: 'taskListActionNoticeText',
            actionsId: 'taskListActionNoticeActions',
            metaId: 'taskListStateMeta',
            clearMeta,
            metaKeys: [
                'lastAction',
                'lastActionSource',
                'lastActionStatus',
                'lastActionTaskNo',
                'lastActionHistoryPath',
                'lastActionLogPath'
            ]
        });
    },

    buildTaskHistoryPath(taskNo) {
        const params = new URLSearchParams();
        params.set('taskNo', String(taskNo));
        params.set('returnTo', window.location.pathname + window.location.search);
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        return `/admin/settings/tasks/history?${params.toString()}`;
    },

    buildTaskHistoryPathFromBase(basePath) {
        if (!basePath) {
            return '#';
        }
        const [path, rawQuery = ''] = basePath.split('?');
        const params = new URLSearchParams(rawQuery);
        params.set('returnTo', window.location.pathname + window.location.search);
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        return `${path}?${params.toString()}`;
    },

    buildTaskDetailPath(taskNo, source = 'task-list-detail') {
        if (!this.isPositiveNumber(Number(taskNo))) {
            return '#';
        }
        const returnTo = encodeURIComponent(window.location.pathname + window.location.search);
        return `/admin/settings/tasks?taskNo=${taskNo}&openTaskNo=${taskNo}&focusTaskNo=${taskNo}&returnTo=${returnTo}&source=${encodeURIComponent(source)}`;
    },

    buildTaskLogPath(taskNo) {
        if (!this.isPositiveNumber(Number(taskNo))) {
            return '#';
        }
        const params = new URLSearchParams();
        params.set('actionType', 'TASK_');
        params.set('targetId', String(taskNo));
        params.set('returnTo', window.location.pathname + window.location.search);
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        return `/admin/logs?${params.toString()}`;
    },

    buildTaskLogPathFromBase(basePath, taskNo = null) {
        if (!basePath && taskNo == null) {
            return '#';
        }
        if (!basePath) {
            return this.buildTaskLogPath(taskNo);
        }
        const [path, rawQuery = ''] = basePath.split('?');
        const params = new URLSearchParams(rawQuery);
        params.set('returnTo', window.location.pathname + window.location.search);
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        return `${path}?${params.toString()}`;
    },

    parseOptionalNumber(value) {
        if (value == null || value === '') {
            return null;
        }
        return Number(value);
    },

    validateState() {
        if (this.state.keyword.length > 100) {
            throw new Error('검색어는 100자 이하로 입력하세요.');
        }
        if (this.state.status && !this.isValidTaskStatus(this.state.status)) {
            throw new Error('작업 상태 필터 값이 올바르지 않습니다.');
        }
        if (this.state.priority && !this.isValidTaskPriority(this.state.priority)) {
            throw new Error('우선순위 필터 값이 올바르지 않습니다.');
        }
        if (this.state.isPinned && !this.isValidYn(this.state.isPinned)) {
            throw new Error('고정 여부 필터 값이 올바르지 않습니다.');
        }
        if (this.state.commentedOnly && !['Y', 'N'].includes(this.state.commentedOnly)) {
            throw new Error('댓글 여부 필터 값이 올바르지 않습니다.');
        }
        if (this.state.dueState && !['OVERDUE', 'TODAY', 'UPCOMING', 'NONE'].includes(this.state.dueState)) {
            throw new Error('기한 상태 필터 값이 올바르지 않습니다.');
        }
        if (this.state.sortBy && !['PINNED_DUE', 'DUE_ASC', 'DUE_DESC', 'PRIORITY_DESC', 'LATEST'].includes(this.state.sortBy)) {
            throw new Error('정렬 조건 값이 올바르지 않습니다.');
        }
        if (this.state.overdueOnly && this.state.overdueOnly !== 'Y') {
            throw new Error('기한 초과 필터 값이 올바르지 않습니다.');
        }
        if (this.state.unassignedOnly && this.state.unassignedOnly !== 'Y') {
            throw new Error('미배정 필터 값이 올바르지 않습니다.');
        }
    },

    normalizePage(value) {
        const page = Number(value);
        return Number.isInteger(page) && page >= 0 ? page : 0;
    },

    normalizePageSize(value) {
        const size = Number(value);
        return Number.isInteger(size) && size > 0 ? size : 10;
    },

    normalizeOptionalPositiveNumber(value) {
        if (value == null || value === '') {
            return null;
        }
        const number = Number(value);
        return this.isPositiveNumber(number) ? number : null;
    },

    parseTaskDataset(value) {
        try {
            return value ? JSON.parse(value) : null;
        } catch (error) {
            console.error('운영 작업 dataset 파싱 실패:', error);
            return null;
        }
    },

    isPositiveNumber(value) {
        return Number.isInteger(value) && value > 0;
    },

    isValidTaskStatus(value) {
        return ['TODO', 'IN_PROGRESS', 'DONE', 'ON_HOLD'].includes(value);
    },

    isValidTaskPriority(value) {
        return ['HIGH', 'MEDIUM', 'LOW'].includes(value);
    },

    isValidYn(value) {
        return value === 'Y' || value === 'N';
    },

    resolveBulkAssigneeAdminNo() {
        const value = document.getElementById('bulkTaskAssignee')?.value;
        if (value == null || value === '' || value === '__UNASSIGN__') {
            return null;
        }
        return this.parseOptionalNumber(value);
    },

    resolveBulkAssigneeMode() {
        return document.getElementById('bulkTaskAssignee')?.value === '__UNASSIGN__' ? 'CLEAR' : null;
    },

    resolveBulkDueDate() {
        if (document.getElementById('bulkTaskDueDateClear')?.checked) {
            return null;
        }
        return document.getElementById('bulkTaskDueDate')?.value || null;
    },

    resolveBulkDueDateMode() {
        return document.getElementById('bulkTaskDueDateClear')?.checked ? 'CLEAR' : null;
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

    resolveStatusBadgeClass(status) {
        switch (status) {
            case 'DONE':
                return 'badge-y';
            case 'IN_PROGRESS':
                return 'badge-low-stock';
            case 'HOLD':
                return 'badge-n';
            default:
                return 'text-bg-secondary';
        }
    },

    resolvePriorityBadgeClass(priority) {
        switch (priority) {
            case 'HIGH':
                return 'badge-low-stock';
            case 'LOW':
                return 'badge-n';
            default:
                return 'text-bg-secondary';
        }
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

document.addEventListener('DOMContentLoaded', () => TaskList.init());
