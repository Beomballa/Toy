const TaskWorkloadList = {
    initialized: false,
    isLoading: false,
    isExporting: false,
    state: {
        page: 0,
        size: 10,
        keyword: '',
        priority: '',
        overdueOnly: '',
        sortBy: 'OVERDUE_DESC',
        adminNo: '',
        focusAdminNo: '',
        source: '',
        returnTo: ''
    },

    init() {
        if (this.initialized) return;
        this.initialized = true;
        this.bindEvents();
        this.readStateFromUrl();
        this.getList();
    },

    bindEvents() {
        document.getElementById('btnSearchTaskWorkload')?.addEventListener('click', () => {
            this.state.page = 0;
            this.getList();
        });
        document.getElementById('btnExportTaskWorkloadCsv')?.addEventListener('click', () => this.exportCsv());
        document.getElementById('btnResetTaskWorkload')?.addEventListener('click', () => this.resetFilters());
        document.getElementById('taskWorkloadPageSize')?.addEventListener('change', () => {
            this.state.page = 0;
            this.updateStateFromInputs();
            this.getList();
        });
        document.getElementById('taskWorkloadKeyword')?.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                this.state.page = 0;
                this.getList();
            }
        });
        window.addEventListener('popstate', () => {
            this.readStateFromUrl();
            this.getList();
        });
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        this.state.page = Number(params.get('page') || 0);
        this.state.size = Number(params.get('size') || 10);
        this.state.keyword = params.get('keyword') || '';
        this.state.priority = params.get('priority') || '';
        this.state.overdueOnly = params.get('overdueOnly') || '';
        this.state.sortBy = params.get('sortBy') || 'OVERDUE_DESC';
        this.state.adminNo = params.get('adminNo') || '';
        this.state.focusAdminNo = params.get('focusAdminNo') || '';
        this.state.source = params.get('source') || '';
        this.state.returnTo = params.get('returnTo') || '';

        document.getElementById('taskWorkloadKeyword').value = this.state.keyword;
        document.getElementById('taskWorkloadPriority').value = this.state.priority;
        document.getElementById('taskWorkloadSortBy').value = this.state.sortBy;
        document.getElementById('taskWorkloadOverdueOnly').checked = this.state.overdueOnly === 'Y';
        document.getElementById('taskWorkloadPageSize').value = String(this.state.size);
        CommonJS.bindMainLogoNavigation(this.state.returnTo || '/admin/settings/tasks/workloads');
        CommonJS.renderSourceContextNotice({ noticeId: 'taskWorkloadSourceContextNotice', source: this.state.source });
        const backButton = document.getElementById('btnBackToTaskList');
        const returnContext = CommonJS.getReturnContext(this.state.returnTo || '/admin/settings/tasks', '운영 작업');
        if (backButton) {
            backButton.href = this.state.returnTo || '/admin/settings/tasks';
            backButton.textContent = `${returnContext.label}로 돌아가기`;
        }
    },

    updateStateFromInputs() {
        this.state.keyword = (document.getElementById('taskWorkloadKeyword')?.value || '').trim();
        this.state.priority = document.getElementById('taskWorkloadPriority')?.value || '';
        this.state.overdueOnly = document.getElementById('taskWorkloadOverdueOnly')?.checked ? 'Y' : '';
        this.state.sortBy = document.getElementById('taskWorkloadSortBy')?.value || 'OVERDUE_DESC';
    },

    buildParams() {
        const params = new URLSearchParams();
        params.set('page', String(this.state.page));
        params.set('size', String(this.state.size));
        if (this.state.keyword) params.set('keyword', this.state.keyword);
        if (this.state.priority) params.set('priority', this.state.priority);
        if (this.state.overdueOnly) params.set('overdueOnly', this.state.overdueOnly);
        if (this.state.sortBy && this.state.sortBy !== 'OVERDUE_DESC') params.set('sortBy', this.state.sortBy);
        if (this.state.adminNo) params.set('adminNo', this.state.adminNo);
        if (this.state.focusAdminNo) params.set('focusAdminNo', this.state.focusAdminNo);
        if (this.state.source) params.set('source', this.state.source);
        if (this.state.returnTo) params.set('returnTo', this.state.returnTo);
        return params;
    },

    async exportCsv() {
        if (this.isExporting) {
            return;
        }
        this.updateStateFromInputs();
        const params = this.buildParams();
        params.delete('page');
        params.delete('size');
        const button = document.getElementById('btnExportTaskWorkloadCsv');
        try {
            this.isExporting = true;
            CommonJS.setButtonDisabled(button, true, '내보내는 중입니다.');
            await CommonJS.downloadFile(`/api/admin/settings/tasks/workloads/export?${params.toString()}`, 'task-workloads.csv');
        } catch (error) {
            await CommonJS.alert(error.message || '담당자별 워크로드 CSV를 내보내지 못했습니다.', '오류', 'error');
        } finally {
            this.isExporting = false;
            CommonJS.setButtonDisabled(button, false);
        }
    },

    async getList() {
        if (this.isLoading) {
            return;
        }
        this.updateStateFromInputs();
        const params = this.buildParams();
        history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
        this.setStateMeta('loading', '담당자별 워크로드를 불러오는 중입니다...', 0, 0, 0, '', '');
        const tbody = document.getElementById('taskWorkloadListBody');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center py-5 text-muted">담당자별 워크로드를 불러오는 중입니다.</td></tr>';
        }

        try {
            this.isLoading = true;
            const response = await fetch(`/api/admin/settings/tasks/workloads/list?${params.toString()}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '담당자별 워크로드 조회에 실패했습니다.'));
            }
            const data = await response.json();
            this.renderSummary(data.summary);
            this.renderList(data.items || []);
            this.renderMeta(data);
            this.renderPagination(data);
            this.highlightFocusedAdminRow();
            await this.openDeepLinkedAssigneeIfNeeded(data.items || []);
        } catch (error) {
            this.renderListError(error.message);
        } finally {
            this.isLoading = false;
        }
    },

    renderSummary(summary) {
        document.getElementById('taskWorkloadAssigneeCount').innerText = Number(summary?.assigneeCount || 0).toLocaleString();
        document.getElementById('taskWorkloadAssignedTaskCount').innerText = Number(summary?.assignedTaskCount || 0).toLocaleString();
        document.getElementById('taskWorkloadOverdueTaskCount').innerText = Number(summary?.overdueTaskCount || 0).toLocaleString();
        document.getElementById('taskWorkloadUnassignedTaskCount').innerText = Number(summary?.unassignedTaskCount || 0).toLocaleString();
    },

    renderList(items) {
        const tbody = document.getElementById('taskWorkloadListBody');
        if (!tbody) return;

        if (!items || items.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center py-5 text-muted">조건에 맞는 담당자 워크로드가 없습니다.</td></tr>';
            this.setStateMeta('empty', '조건에 맞는 담당자 워크로드가 없습니다.', 0, 0, 0, '', '');
            return;
        }

        tbody.innerHTML = items.map((item, index) => `
            <tr data-admin-row="${item.assigneeAdminNo}">
                <td class="ps-4 text-muted small">${this.state.page * this.state.size + index + 1}</td>
                <td>
                    <a class="fw-bold text-dark text-decoration-none" href="${this.buildWorkloadDetailPath(item.assigneeAdminNo)}">${this.escapeHtml(item.assigneeAdminName)}</a>
                </td>
                <td class="text-center fw-semibold">${Number(item.totalCount || 0).toLocaleString()}</td>
                <td class="text-center">${Number(item.todoCount || 0).toLocaleString()}</td>
                <td class="text-center">${Number(item.inProgressCount || 0).toLocaleString()}</td>
                <td class="text-center">
                    <span class="badge ${Number(item.overdueCount || 0) > 0 ? 'text-bg-danger' : 'text-bg-light'}">${Number(item.overdueCount || 0).toLocaleString()}</span>
                </td>
                <td>
                    ${item.latestCommentContent ? `
                        <div class="small fw-semibold text-dark">${this.escapeHtml(item.latestCommentTaskTitle || '작업')}</div>
                        <div class="small text-muted">${this.escapeHtml(item.latestCommentAdminName || '관리자')} · ${this.escapeHtml(item.latestCommentDtm || '-')}</div>
                        <div class="small text-dark mt-1">${this.escapeHtml(item.latestCommentContent)}</div>
                    ` : '<div class="small text-muted">최근 메모가 없습니다.</div>'}
                </td>
                <td class="text-end pe-4">
                    <a class="btn btn-sm btn-outline-dark me-1" href="${this.buildWorkloadDetailPath(item.assigneeAdminNo)}">상세</a>
                    <a class="btn btn-sm btn-outline-secondary me-1" href="${this.buildContextualTaskPath(item.targetPath)}">담당 작업</a>
                    <a class="btn btn-sm btn-outline-secondary" href="${this.buildContextualTaskPath(item.overduePath)}">기한 초과</a>
                </td>
            </tr>
        `).join('');
    },

    renderMeta(data) {
        CommonJS.renderListMeta({
            metaTextId: 'taskWorkloadMetaText',
            filterMetaId: 'taskWorkloadFilterMeta',
            resultMetaId: 'taskWorkloadResultMeta',
            pageMetaId: 'taskWorkloadPageMeta',
            resultLabel: data.resultMeta?.resultLabel || `${data.totalElements || 0}명 조회`,
            filterCount: data.resultMeta?.filterCount ?? 0,
            querySignature: data.resultMeta?.querySignature || '기한 초과 우선 · 진행중 우선',
            pageInfoLabel: data.resultMeta?.pageInfoLabel || '',
            filterPrefix: '필터',
            defaultResultText: '결과 메타 없음',
            defaultPageText: '페이지 메타 없음'
        });
        this.setStateMeta('ready', '', (data.items || []).length, data.totalElements || 0, data.resultMeta?.filterCount || 0, data.resultMeta?.querySignature || '', data.resultMeta?.pageInfoLabel || '');
    },

    renderPagination(data) {
        const paginationEl = document.getElementById('taskWorkloadPagination');
        if (!paginationEl) return;
        const totalPages = Number(data.totalPages || 0);
        const currentPage = Number(data.currentPage || 0);

        if (totalPages <= 1) {
            paginationEl.innerHTML = '';
            return;
        }

        paginationEl.innerHTML = Array.from({ length: totalPages }, (_, index) => `
            <li class="page-item ${index === currentPage ? 'active' : ''}">
                <button type="button" class="page-link" data-role="go-page" data-page="${index}">${index + 1}</button>
            </li>
        `).join('');

        paginationEl.querySelectorAll('[data-role="go-page"]').forEach((button) => {
            button.addEventListener('click', () => {
                this.state.page = Number(button.dataset.page);
                this.getList();
            });
        });
    },

    resetFilters() {
        this.state = {
            page: 0,
            size: 10,
            keyword: '',
            priority: '',
            overdueOnly: '',
            sortBy: 'OVERDUE_DESC',
            adminNo: '',
            focusAdminNo: '',
            source: this.state.source || '',
            returnTo: this.state.returnTo || ''
        };
        document.getElementById('taskWorkloadKeyword').value = '';
        document.getElementById('taskWorkloadPriority').value = '';
        document.getElementById('taskWorkloadSortBy').value = 'OVERDUE_DESC';
        document.getElementById('taskWorkloadOverdueOnly').checked = false;
        document.getElementById('taskWorkloadPageSize').value = '10';
        this.getList();
    },

    renderListError(message) {
        const tbody = document.getElementById('taskWorkloadListBody');
        if (tbody) {
            tbody.innerHTML = `<tr><td colspan="8" class="text-center py-5 text-danger">${this.escapeHtml(message)}</td></tr>`;
        }
        this.setStateMeta('error', message, 0, 0, 0, '', '');
        document.getElementById('taskWorkloadMetaText').textContent = '조회 실패';
        document.getElementById('taskWorkloadFilterMeta').textContent = '필터 메타 확인 불가';
        document.getElementById('taskWorkloadResultMeta').textContent = '결과 메타 확인 불가';
        document.getElementById('taskWorkloadPageMeta').textContent = '페이지 메타 확인 불가';
    },

    setStateMeta(state, message, visibleCount, totalCount, filterCount, querySignature, pageInfoLabel) {
        const meta = document.getElementById('taskWorkloadStateMeta');
        if (!meta) return;
        meta.dataset.listState = state;
        meta.dataset.stateMessage = message || '';
        meta.dataset.visibleCount = String(visibleCount || 0);
        meta.dataset.totalElements = String(totalCount || 0);
        meta.dataset.filterCount = String(filterCount || 0);
        meta.dataset.querySignature = querySignature || '';
        meta.dataset.pageInfoLabel = pageInfoLabel || '';
        meta.dataset.highlightAdminNo = this.state.focusAdminNo || '';
        meta.dataset.sourceContext = this.state.source || '';
        CommonJS.renderSourceContextNotice({ noticeId: 'taskWorkloadSourceContextNotice', source: this.state.source });
    },

    async openDeepLinkedAssigneeIfNeeded(items) {
        if (!this.state.adminNo) return;
        const adminNo = Number(this.state.adminNo);
        if (!adminNo) {
            this.state.adminNo = '';
            return;
        }
        const target = items.find((item) => Number(item.assigneeAdminNo) === adminNo);
        if (target || adminNo > 0) {
            const meta = document.getElementById('taskWorkloadStateMeta');
            if (meta) {
                meta.dataset.lastOpenedAdminNo = String(adminNo);
            }
            window.location.href = this.buildWorkloadDetailPath(adminNo);
            return;
        }
        this.state.adminNo = '';
        history.replaceState(null, '', `${window.location.pathname}?${this.buildParams().toString()}`);
    },

    buildWorkloadDetailPath(adminNo) {
        const returnParams = this.buildParams();
        returnParams.set('focusAdminNo', String(adminNo));
        returnParams.delete('adminNo');
        const params = new URLSearchParams();
        params.set('adminNo', String(adminNo));
        params.set('returnTo', `${window.location.pathname}?${returnParams.toString()}`);
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        return `/admin/settings/tasks/workloads/get?${params.toString()}`;
    },

    buildContextualTaskPath(basePath) {
        if (!basePath) {
            return '#';
        }
        const [path, rawQuery = ''] = basePath.split('?');
        const params = new URLSearchParams(rawQuery);
        params.set('returnTo', `${window.location.pathname}?${this.buildParams().toString()}`);
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        return `${path}?${params.toString()}`;
    },

    highlightFocusedAdminRow() {
        const focusAdminNo = Number(this.state.focusAdminNo || 0);
        document.querySelectorAll('[data-admin-row]').forEach((row) => {
            row.classList.remove('table-warning');
        });
        if (!focusAdminNo) {
            return;
        }
        const row = document.querySelector(`[data-admin-row="${focusAdminNo}"]`);
        if (!row) {
            return;
        }
        row.classList.add('table-warning');
        row.scrollIntoView({ block: 'center', behavior: 'smooth' });
        this.state.focusAdminNo = '';
        const meta = document.getElementById('taskWorkloadStateMeta');
        if (meta) {
            meta.dataset.highlightAdminNo = '';
        }
        history.replaceState(null, '', `${window.location.pathname}?${this.buildParams().toString()}`);
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
