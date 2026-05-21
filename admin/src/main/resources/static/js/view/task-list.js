const TaskList = {
    initialized: false,
    modal: null,
    operationPolicy: null,
    state: {
        page: 0,
        size: 10,
        keyword: '',
        status: '',
        priority: '',
        assigneeAdminNo: '',
        overdueOnly: '',
        taskNo: ''
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
        this.getList();
    },

    async applyOperationPolicy(settings = null) {
        try {
            this.operationPolicy = settings || await CommonJS.fetchSystemSettings();
            const disabled = CommonJS.isAdminWriteBlocked(this.operationPolicy);
            const reason = CommonJS.getAdminWriteBlockedReason('운영 작업 등록 및 수정');
            CommonJS.setButtonDisabled(document.getElementById('btnNewTask'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnSaveTask'), disabled, reason);
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
    },

    bindEvents() {
        document.getElementById('btnNewTask')?.addEventListener('click', () => this.openModal());
        document.getElementById('btnSaveTask')?.addEventListener('click', () => this.saveTask());
        document.getElementById('btnSearchTask')?.addEventListener('click', () => this.getList());
        document.getElementById('btnResetTask')?.addEventListener('click', () => this.resetFilters());
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
        document.getElementById('taskStatTotalCard')?.addEventListener('click', () => this.applyStatFilter('total'));
        document.getElementById('taskStatTodoCard')?.addEventListener('click', () => this.applyStatFilter('TODO'));
        document.getElementById('taskStatProgressCard')?.addEventListener('click', () => this.applyStatFilter('IN_PROGRESS'));
        document.getElementById('taskStatOverdueCard')?.addEventListener('click', () => this.applyStatFilter('overdue'));
        document.getElementById('taskListBody')?.addEventListener('click', (event) => {
            const editButton = event.target.closest('[data-role="edit-task"]');
            if (editButton) {
                this.openEditModal(JSON.parse(editButton.dataset.task));
                return;
            }

            const statusButton = event.target.closest('[data-role="update-task-status"]');
            if (statusButton) {
                this.updateStatus(Number(statusButton.dataset.taskNo), statusButton.dataset.status);
                return;
            }

            const deleteButton = event.target.closest('[data-role="delete-task"]');
            if (deleteButton) {
                this.deleteTask(Number(deleteButton.dataset.taskNo));
            }
        });
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        this.state.page = Number(params.get('page') || 0);
        this.state.size = Number(params.get('size') || 10);
        this.state.keyword = params.get('keyword') || '';
        this.state.status = params.get('status') || '';
        this.state.priority = params.get('priority') || '';
        this.state.assigneeAdminNo = params.get('assigneeAdminNo') || '';
        this.state.overdueOnly = params.get('overdueOnly') || '';
        this.state.taskNo = params.get('taskNo') || '';
        document.getElementById('taskKeyword').value = this.state.keyword;
        document.getElementById('taskStatusFilter').value = this.state.status;
        document.getElementById('taskPriorityFilter').value = this.state.priority;
        document.getElementById('taskPageSize').value = String(this.state.size);
    },

    updateStateFromInputs() {
        this.state.keyword = document.getElementById('taskKeyword').value.trim();
        this.state.status = document.getElementById('taskStatusFilter').value;
        this.state.priority = document.getElementById('taskPriorityFilter').value;
        this.state.assigneeAdminNo = document.getElementById('taskAssigneeFilter').value;
        this.state.size = Number(document.getElementById('taskPageSize').value || 10);
    },

    buildParams() {
        const params = new URLSearchParams();
        params.set('page', String(this.state.page));
        params.set('size', String(this.state.size));
        if (this.state.keyword) params.set('keyword', this.state.keyword);
        if (this.state.status) params.set('status', this.state.status);
        if (this.state.priority) params.set('priority', this.state.priority);
        if (this.state.assigneeAdminNo) params.set('assigneeAdminNo', this.state.assigneeAdminNo);
        if (this.state.overdueOnly) params.set('overdueOnly', this.state.overdueOnly);
        if (this.state.taskNo) params.set('taskNo', this.state.taskNo);
        return params;
    },

    async getList() {
        try {
            this.updateStateFromInputs();
            const params = this.buildParams();
            history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
            this.setFilterMeta('적용 필터를 계산하는 중입니다...');
            this.setResultMeta('결과 메타를 계산하는 중입니다...');
            this.setPageMeta('페이지 메타를 계산하는 중입니다...');
            this.setListStateMeta('loading', '운영 작업을 불러오는 중입니다.', 0, 0, '');

            const response = await fetch(`/api/admin/settings/tasks/list?${params.toString()}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 작업 목록을 불러오지 못했습니다.'));
            }

            const data = await response.json();
            this.renderAssigneeOptions(data.assigneeOptions || []);
            this.renderList(data.items || []);
            this.renderStats(data.taskStats);
            this.renderMeta(data);
            this.renderPagination(data);
            await this.openDeepLinkedTaskIfNeeded(data.items || []);
        } catch (error) {
            document.getElementById('taskMetaText').textContent = error.message;
            this.setFilterMeta(error.message);
            this.setResultMeta('결과 메타 확인 불가');
            this.setPageMeta('페이지 메타 확인 불가');
            document.getElementById('taskListBody').innerHTML = `<tr><td colspan="7" class="text-center py-5 text-danger">${error.message}</td></tr>`;
            document.getElementById('taskPagination').innerHTML = '';
            this.renderStats(null);
            this.setListStateMeta('error', error.message, 0, 0, '');
        }
    },

    async openDeepLinkedTaskIfNeeded(items) {
        if (!this.state.taskNo) {
            return;
        }
        const taskNo = Number(this.state.taskNo);
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
        this.state.taskNo = '';
        const params = this.buildParams();
        history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
    },

    renderAssigneeOptions(options) {
        const filters = document.getElementById('taskAssigneeFilter');
        const form = document.getElementById('taskAssignee');
        if (!filters || !form) return;

        const selectedFilter = this.state.assigneeAdminNo || '';
        const selectedForm = form.value || '';
        const optionHtml = ['<option value="">전체</option>']
            .concat(options.map((option) => `<option value="${option.adminNo}">${this.escapeHtml(option.name)}</option>`))
            .join('');
        filters.innerHTML = optionHtml;
        filters.value = selectedFilter;

        const formOptionHtml = ['<option value="">미지정</option>']
            .concat(options.map((option) => `<option value="${option.adminNo}">${this.escapeHtml(option.name)}</option>`))
            .join('');
        form.innerHTML = formOptionHtml;
        form.value = selectedForm;
    },

    renderList(items) {
        const tbody = document.getElementById('taskListBody');
        if (!tbody) return;

        if (!items || items.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center py-5 text-muted">등록된 운영 작업이 없습니다.</td></tr>';
            this.setListStateMeta('empty', '등록된 운영 작업이 없습니다.', 0, 0, '');
            return;
        }

        tbody.innerHTML = items.map((item) => `
            <tr>
                <td class="ps-4 text-muted small">${item.taskNo}</td>
                <td>
                    <div class="d-flex align-items-center gap-2 mb-1">
                        ${item.isPinned === 'Y' ? '<span class="badge text-bg-danger">고정</span>' : ''}
                        <span class="fw-bold">${this.escapeHtml(item.title)}</span>
                    </div>
                    <div class="small text-muted text-truncate" style="max-width: 440px;">${this.escapeHtml(item.description || '-')}</div>
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
                    <a class="btn btn-sm btn-outline-secondary me-1" href="${item.activityLogPath}">${item.activityLogLabel}</a>
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

    renderStats(stats) {
        const totalCountEl = document.getElementById('taskTotalCount');
        const todoCountEl = document.getElementById('taskTodoCount');
        const inProgressCountEl = document.getElementById('taskInProgressCount');
        const overdueCountEl = document.getElementById('taskOverdueCount');
        const contextTextEl = document.getElementById('taskStatsContextText');
        const noticeEl = document.getElementById('taskStatsNotice');

        if (!stats) {
            totalCountEl.innerText = '0';
            todoCountEl.innerText = '0';
            inProgressCountEl.innerText = '0';
            overdueCountEl.innerText = '0';
            contextTextEl.innerText = '카드 기준을 확인할 수 없습니다.';
            noticeEl.innerText = '카드 기준을 확인할 수 없습니다.';
            noticeEl.dataset.statsContext = 'error';
            return;
        }

        totalCountEl.innerText = Number(stats.totalCount || 0).toLocaleString();
        todoCountEl.innerText = Number(stats.todoCount || 0).toLocaleString();
        inProgressCountEl.innerText = Number(stats.inProgressCount || 0).toLocaleString();
        overdueCountEl.innerText = Number(stats.overdueCount || 0).toLocaleString();
        contextTextEl.innerText = `${stats.contextLabel} · ${stats.querySignature}`;
        const usingQuickFilter = !!this.state.status || !!this.state.priority;
        noticeEl.innerText = usingQuickFilter
            ? '카드 수치는 기본 탐색 문맥 기준이며, 선택한 빠른 필터는 목록에만 적용됩니다.'
            : '카드 수치는 현재 탐색 문맥 기준입니다.';
        noticeEl.dataset.statsContext = usingQuickFilter ? 'base-query' : 'current-query';
    },

    renderMeta(data) {
        document.getElementById('taskMetaText').textContent = data.resultMeta?.resultLabel || `${data.totalElements || 0}건 조회`;
        this.setFilterMeta(`필터 ${data.resultMeta?.appliedFilterCount ?? 0}개 · ${data.resultMeta?.querySignature || '고정 우선 · 마감 임박 순'}`);
        this.setResultMeta(data.resultMeta?.resultLabel || '결과 메타 없음');
        this.setPageMeta(data.resultMeta?.pageInfoLabel || '페이지 메타 없음');
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
            button.addEventListener('click', () => this.goPage(Number(button.dataset.page)));
        });
    },

    openModal() {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            alert(CommonJS.getAdminWriteBlockedReason('운영 작업 등록 및 수정'));
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

    openEditModal(task) {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            alert(CommonJS.getAdminWriteBlockedReason('운영 작업 등록 및 수정'));
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
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            alert(CommonJS.getAdminWriteBlockedReason('운영 작업 등록 및 수정'));
            return;
        }

        const title = CommonJS.normalizeRequiredText(document.getElementById('taskTitle')?.value || '');
        if (!title) {
            alert('작업 제목을 입력해주세요.');
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
            const response = await fetch('/api/admin/settings/tasks/save', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(payload)
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 작업 저장에 실패했습니다.'));
            }
            this.modal?.hide();
            this.getList();
        } catch (error) {
            alert(error.message);
        }
    },

    async updateStatus(taskNo, status) {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            alert(CommonJS.getAdminWriteBlockedReason('운영 작업 상태 변경'));
            return;
        }
        try {
            const response = await fetch(`/api/admin/settings/tasks/status/${taskNo}?status=${encodeURIComponent(status)}`, {method: 'PATCH'});
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '상태 변경에 실패했습니다.'));
            }
            this.getList();
        } catch (error) {
            alert(error.message);
        }
    },

    async deleteTask(taskNo) {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            alert(CommonJS.getAdminWriteBlockedReason('운영 작업 삭제'));
            return;
        }
        if (!confirm('운영 작업을 삭제하시겠습니까?')) {
            return;
        }
        try {
            const response = await fetch(`/api/admin/settings/tasks/delete?no=${taskNo}`, {method: 'DELETE'});
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 작업 삭제에 실패했습니다.'));
            }
            this.getList();
        } catch (error) {
            alert(error.message);
        }
    },

    applyStatFilter(type) {
        this.state.page = 0;
        if (type === 'total') {
            this.state.status = '';
            this.state.priority = '';
            this.state.overdueOnly = '';
        } else if (type === 'overdue') {
            this.state.status = '';
            this.state.priority = '';
            this.state.overdueOnly = 'Y';
        } else {
            this.state.status = type;
            this.state.overdueOnly = '';
        }
        document.getElementById('taskStatusFilter').value = this.state.status;
        document.getElementById('taskPriorityFilter').value = this.state.priority;
        this.getList();
    },

    goPage(page) {
        this.state.page = page;
        this.getList();
    },

    resetFilters() {
        document.getElementById('taskKeyword').value = '';
        document.getElementById('taskStatusFilter').value = '';
        document.getElementById('taskPriorityFilter').value = '';
        document.getElementById('taskAssigneeFilter').value = '';
        document.getElementById('taskPageSize').value = '10';
        this.state.page = 0;
        this.state.size = 10;
        this.state.keyword = '';
        this.state.status = '';
        this.state.priority = '';
        this.state.assigneeAdminNo = '';
        this.state.overdueOnly = '';
        this.state.taskNo = '';
        this.getList();
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
    },

    parseOptionalNumber(value) {
        if (value == null || value === '') {
            return null;
        }
        return Number(value);
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
