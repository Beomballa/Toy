const TaskWorkloadList = {
    initialized: false,
    listRequestId: 0,
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
        this.state.page = this.normalizePage(params.get('page'));
        this.state.size = this.normalizePageSize(params.get('size'));
        this.state.keyword = (params.get('keyword') || '').slice(0, 100);
        const priority = params.get('priority') || '';
        const overdueOnly = params.get('overdueOnly') || '';
        const sortBy = params.get('sortBy') || 'OVERDUE_DESC';
        this.state.priority = ['HIGH', 'MEDIUM', 'LOW'].includes(priority) ? priority : '';
        this.state.overdueOnly = overdueOnly === 'Y' ? 'Y' : '';
        this.state.sortBy = ['OVERDUE_DESC', 'TOTAL_DESC', 'TODO_DESC', 'NAME_ASC'].includes(sortBy) ? sortBy : 'OVERDUE_DESC';
        this.state.adminNo = this.normalizeOptionalPositiveNumber(params.get('adminNo'))?.toString() || '';
        this.state.focusAdminNo = this.normalizeOptionalPositiveNumber(params.get('focusAdminNo'))?.toString() || '';
        this.state.source = params.get('source') || '';
        this.state.returnTo = CommonJS.normalizeAdminReturnPath(params.get('returnTo'), '');

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
        const priority = document.getElementById('taskWorkloadPriority')?.value || '';
        this.state.priority = ['HIGH', 'MEDIUM', 'LOW'].includes(priority) ? priority : '';
        this.state.overdueOnly = document.getElementById('taskWorkloadOverdueOnly')?.checked ? 'Y' : '';
        const sortBy = document.getElementById('taskWorkloadSortBy')?.value || 'OVERDUE_DESC';
        this.state.sortBy = ['OVERDUE_DESC', 'TOTAL_DESC', 'TODO_DESC', 'NAME_ASC'].includes(sortBy) ? sortBy : 'OVERDUE_DESC';
        this.state.size = this.normalizePageSize(document.getElementById('taskWorkloadPageSize')?.value);
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
        const button = document.getElementById('btnExportTaskWorkloadCsv');
        try {
            this.updateStateFromInputs();
            this.validateState();
            const params = this.buildParams();
            params.delete('page');
            params.delete('size');
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
        const requestId = ++this.listRequestId;
        try {
            this.updateStateFromInputs();
            this.validateState();
            const params = this.buildParams();
            history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
            this.setStateMeta('loading', '담당자별 워크로드를 불러오는 중입니다...', 0, 0, 0, '', '');
            this.renderLoadingState();
            const response = await fetch(`/api/admin/settings/tasks/workloads/list?${params.toString()}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '담당자별 워크로드 조회에 실패했습니다.'));
            }
            const data = await response.json();
            if (requestId !== this.listRequestId) {
                return;
            }
            const items = Array.isArray(data.items) ? data.items : [];
            this.renderSummary(data.summary);
            this.renderList(items);
            this.renderMeta(data);
            this.renderPagination(data);
            this.highlightFocusedAdminRow();
            await this.openDeepLinkedAssigneeIfNeeded(items, requestId);
        } catch (error) {
            if (requestId !== this.listRequestId) {
                return;
            }
            this.renderListError(error.message);
        }
    },

    renderSummary(summary) {
        document.getElementById('taskWorkloadAssigneeCount').innerText = this.formatCount(summary?.assigneeCount);
        document.getElementById('taskWorkloadAssignedTaskCount').innerText = this.formatCount(summary?.assignedTaskCount);
        document.getElementById('taskWorkloadOverdueTaskCount').innerText = this.formatCount(summary?.overdueTaskCount);
        document.getElementById('taskWorkloadUnassignedTaskCount').innerText = this.formatCount(summary?.unassignedTaskCount);
    },

    renderList(items) {
        const tbody = document.getElementById('taskWorkloadListBody');
        if (!tbody) return;
        const validItems = Array.isArray(items)
            ? items.filter((item) => this.normalizeOptionalPositiveNumber(item?.assigneeAdminNo) != null)
            : [];

        if (validItems.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" class="py-5">
                        <div class="product-empty-state">
                            <i class="fas fa-user-clock product-empty-state-icon"></i>
                            <strong>조건에 맞는 담당자 워크로드가 없습니다.</strong>
                            <p>${this.escapeHtml(this.buildEmptyStateMessage())}</p>
                        </div>
                    </td>
                </tr>
            `;
            this.setStateMeta('empty', '조건에 맞는 담당자 워크로드가 없습니다.', 0, 0, 0, '', '');
            return;
        }

        tbody.innerHTML = validItems.map((item, index) => {
            const adminNo = this.normalizeOptionalPositiveNumber(item.assigneeAdminNo);
            const detailPath = this.buildWorkloadDetailPath(adminNo);
            const targetPath = this.buildContextualTaskPath(item.targetPath);
            const overduePath = this.buildContextualTaskPath(item.overduePath);
            return `
            <tr data-admin-row="${adminNo || ''}">
                <td class="ps-4 text-muted small">${this.state.page * this.state.size + index + 1}</td>
                <td>
                    <a class="fw-bold text-dark text-decoration-none" href="${this.escapeHtml(detailPath)}">${this.escapeHtml(item.assigneeAdminName || '-')}</a>
                </td>
                <td class="text-center fw-semibold">${this.formatCount(item.totalCount)}</td>
                <td class="text-center">${this.formatCount(item.todoCount)}</td>
                <td class="text-center">${this.formatCount(item.inProgressCount)}</td>
                <td class="text-center">
                    <span class="badge ${this.normalizeNonNegativeInteger(item.overdueCount) > 0 ? 'text-bg-danger' : 'text-bg-light'}">${this.formatCount(item.overdueCount)}</span>
                </td>
                <td>
                    ${item.latestCommentContent ? `
                        <div class="small fw-semibold text-dark">${this.escapeHtml(item.latestCommentTaskTitle || '작업')}</div>
                        <div class="small text-muted">${this.escapeHtml(item.latestCommentAdminName || '관리자')} · ${this.escapeHtml(item.latestCommentDtm || '-')}</div>
                        <div class="small text-dark mt-1">${this.escapeHtml(item.latestCommentContent)}</div>
                    ` : '<div class="small text-muted">최근 메모가 없습니다.</div>'}
                </td>
                <td class="text-end pe-4">
                    <a class="btn btn-sm btn-outline-dark me-1" href="${this.escapeHtml(detailPath)}">상세</a>
                    <a class="btn btn-sm btn-outline-secondary me-1" href="${this.escapeHtml(targetPath || '#')}" ${targetPath ? '' : 'aria-disabled="true" tabindex="-1"'}>담당 작업</a>
                    <a class="btn btn-sm btn-outline-secondary" href="${this.escapeHtml(overduePath || '#')}" ${overduePath ? '' : 'aria-disabled="true" tabindex="-1"'}>기한 초과</a>
                </td>
            </tr>
        `;
        }).join('');
        this.setStateMeta('ready', '', validItems.length, null, null, '', '');
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
        const visibleCount = Array.isArray(data.items)
            ? data.items.filter((item) => this.normalizeOptionalPositiveNumber(item?.assigneeAdminNo) != null).length
            : 0;
        this.setStateMeta('ready', '', visibleCount, this.normalizeNonNegativeInteger(data.totalElements), this.normalizeNonNegativeInteger(data.resultMeta?.filterCount), data.resultMeta?.querySignature || '', data.resultMeta?.pageInfoLabel || '');
    },

    renderPagination(data) {
        const paginationEl = document.getElementById('taskWorkloadPagination');
        if (!paginationEl) return;
        const totalPages = this.normalizeNonNegativeInteger(data.totalPages);
        const currentPage = Math.min(this.normalizePage(data.currentPage), Math.max(totalPages - 1, 0));

        if (totalPages <= 1) {
            paginationEl.innerHTML = '';
            return;
        }

        paginationEl.innerHTML = this.buildPaginationPages(currentPage, totalPages).map((page) => page == null
            ? '<li class="page-item disabled"><span class="page-link">…</span></li>'
            : `
                <li class="page-item ${page === currentPage ? 'active' : ''}">
                    <button type="button" class="page-link" data-role="go-page" data-page="${page}">${page + 1}</button>
                </li>
            `).join('');

        paginationEl.querySelectorAll('[data-role="go-page"]').forEach((button) => {
            button.addEventListener('click', () => {
                this.goPage(this.normalizePage(button.dataset.page));
            });
        });
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

    buildEmptyStateMessage() {
        const parts = [];
        if (this.state.keyword) {
            parts.push(`검색어 "${this.state.keyword}"`);
        }
        if (this.state.priority) {
            parts.push(`우선순위 ${this.resolvePriorityLabel(this.state.priority)}`);
        }
        if (this.state.overdueOnly === 'Y') {
            parts.push('기한 초과만');
        }
        if (this.state.sortBy) {
            parts.push(`정렬 ${this.resolveSortLabel(this.state.sortBy)}`);
        }

        if (!parts.length) {
            return '담당자별 작업 분배 데이터가 아직 없거나, 현재 페이지에 표시할 결과가 없습니다.';
        }

        return `${parts.join(', ')} 조건에 맞는 담당자 워크로드가 없습니다.`;
    },

    renderListError(message) {
        const tbody = document.getElementById('taskWorkloadListBody');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" class="py-5">
                        <div class="product-empty-state">
                            <div class="product-empty-state__icon text-danger">
                                <i class="fa-solid fa-triangle-exclamation"></i>
                            </div>
                            <strong>담당자별 워크로드 조회에 실패했습니다.</strong>
                            <p>${this.escapeHtml(message)}</p>
                        </div>
                    </td>
                </tr>
            `;
        }
        this.setStateMeta('error', message, 0, 0, 0, '', '');
        this.renderSummary(null);
        document.getElementById('taskWorkloadPagination').innerHTML = '';
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

    async openDeepLinkedAssigneeIfNeeded(items, requestId) {
        if (!this.state.adminNo || requestId !== this.listRequestId) return;
        const adminNo = this.normalizeOptionalPositiveNumber(this.state.adminNo);
        if (adminNo == null) {
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
        if (!this.isPositiveNumber(Number(adminNo))) {
            return '#';
        }
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
        const safeBasePath = CommonJS.normalizeAdminReturnPath(basePath, '');
        if (!safeBasePath) {
            return '';
        }
        const [path, rawQuery = ''] = safeBasePath.split('?');
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
    },

    resolvePriorityLabel(priority) {
        if (priority === 'HIGH') return '높음';
        if (priority === 'MEDIUM') return '보통';
        if (priority === 'LOW') return '낮음';
        return priority || '전체';
    },

    resolveSortLabel(sortBy) {
        if (sortBy === 'TOTAL_DESC') return '총 작업 많은 순';
        if (sortBy === 'TODO_DESC') return '대기 작업 많은 순';
        if (sortBy === 'NAME_ASC') return '담당자명 순';
        return '기한 초과 우선';
    },

    validateState() {
        if (this.state.keyword.length > 100) {
            throw new Error('검색어는 100자 이하로 입력하세요.');
        }
        if (this.state.priority && !['HIGH', 'MEDIUM', 'LOW'].includes(this.state.priority)) {
            throw new Error('우선순위 필터 값이 올바르지 않습니다.');
        }
        if (this.state.overdueOnly && this.state.overdueOnly !== 'Y') {
            throw new Error('기한 초과 필터 값이 올바르지 않습니다.');
        }
        if (this.state.sortBy && !['OVERDUE_DESC', 'TOTAL_DESC', 'TODO_DESC', 'NAME_ASC'].includes(this.state.sortBy)) {
            throw new Error('정렬 조건 값이 올바르지 않습니다.');
        }
    },

    normalizePage(value) {
        const page = Number(value);
        return Number.isInteger(page) && page >= 0 ? page : 0;
    },

    normalizePageSize(value) {
        const size = Number(value);
        return [10, 20, 50].includes(size) ? size : 10;
    },

    normalizeNonNegativeInteger(value) {
        const number = Number(value);
        return Number.isInteger(number) && number >= 0 ? number : 0;
    },

    formatCount(value) {
        return this.normalizeNonNegativeInteger(value).toLocaleString();
    },

    buildPaginationPages(currentPage, totalPages) {
        const pages = new Set([0, totalPages - 1]);
        for (let page = Math.max(0, currentPage - 2); page <= Math.min(totalPages - 1, currentPage + 2); page += 1) {
            pages.add(page);
        }
        return Array.from(pages).sort((left, right) => left - right).flatMap((page, index, sorted) => (
            index > 0 && page - sorted[index - 1] > 1 ? [null, page] : [page]
        ));
    },

    renderLoadingState() {
        const tbody = document.getElementById('taskWorkloadListBody');
        if (!tbody) return;
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="py-5">
                    <div class="product-loading-state">
                        <div class="spinner-border spinner-border-sm text-primary" role="status" aria-hidden="true"></div>
                        <strong>담당자별 워크로드를 불러오는 중입니다.</strong>
                        <p>현재 조건에 맞는 담당자 작업 분배 현황을 조회하고 있습니다.</p>
                    </div>
                </td>
            </tr>
        `;
    },

    normalizeOptionalPositiveNumber(value) {
        if (value == null || value === '') {
            return null;
        }
        const number = Number(value);
        return this.isPositiveNumber(number) ? number : null;
    },

    isPositiveNumber(value) {
        return Number.isInteger(value) && value > 0;
    }
};
