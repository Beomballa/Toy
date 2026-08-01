const TaskHistoryPage = {
    initialized: false,
    modal: null,
    isExporting: false,
    listRequestId: 0,
    detailRequestId: 0,
    state: {
        page: 0,
        size: 20,
        returnTo: '/admin/settings/tasks',
        source: ''
    },

    init() {
        if (this.initialized) return;
        this.initialized = true;
        const modalElement = document.getElementById('taskHistoryDetailModal');
        this.modal = new bootstrap.Modal(modalElement);
        modalElement.addEventListener('hidden.bs.modal', () => {
            this.detailRequestId += 1;
        });
        this.bindEvents();
        this.readStateFromUrl();
        this.syncReturnLinks();
        this.loadHistory();
    },

    bindEvents() {
        document.getElementById('btnSearchTaskHistory')?.addEventListener('click', () => {
            this.state.page = 0;
            this.loadHistory();
        });
        document.getElementById('btnResetTaskHistory')?.addEventListener('click', () => this.resetFilters());
        document.getElementById('btnExportTaskHistoryCsv')?.addEventListener('click', () => this.exportCsv());
        document.getElementById('taskHistoryPageSize')?.addEventListener('change', () => {
            this.state.page = 0;
            this.state.size = this.normalizePageSize(document.getElementById('taskHistoryPageSize')?.value);
            this.loadHistory();
        });
        ['taskHistoryTaskNo', 'taskHistoryAdminNo', 'taskHistoryAdminKeyword', 'taskHistoryStartDate', 'taskHistoryEndDate'].forEach((id) => {
            document.getElementById(id)?.addEventListener('keydown', (event) => {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    this.state.page = 0;
                    this.loadHistory();
                }
            });
        });
        document.querySelectorAll('.task-history-quick-filter[data-action-type]').forEach((button) => {
            button.addEventListener('click', () => {
                document.getElementById('taskHistoryActionType').value = this.normalizeActionType(button.dataset.actionType);
                this.state.page = 0;
                this.syncQuickFilterState();
                this.loadHistory();
            });
        });
        document.querySelectorAll('[data-task-history-date-preset]').forEach((button) => {
            button.addEventListener('click', () => this.applyDatePreset(this.normalizeDatePreset(button.dataset.taskHistoryDatePreset)));
        });
        document.getElementById('btnBackToTaskSource')?.addEventListener('click', () => {
            window.location.href = this.state.returnTo;
        });
        document.getElementById('taskHistoryBody')?.addEventListener('click', (event) => {
            const detailButton = event.target.closest('[data-role="open-task-log-detail"]');
            if (detailButton) {
                const logNo = this.normalizeOptionalPositiveNumber(detailButton.dataset.logNo);
                if (logNo == null) {
                    void CommonJS.alert('상세 로그 번호가 올바르지 않습니다.', '알림', 'warning');
                    return;
                }
                this.openDetail(logNo);
            }
        });
        window.addEventListener('popstate', () => {
            this.readStateFromUrl();
            this.syncReturnLinks();
            this.syncQuickFilterState();
            this.syncDatePresetState();
            this.loadHistory();
        });
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        document.getElementById('taskHistoryTaskNo').value = this.normalizeOptionalPositiveNumber(params.get('taskNo'));
        document.getElementById('taskHistoryActionType').value = this.normalizeActionType(params.get('actionType'));
        document.getElementById('taskHistoryAdminNo').value = this.normalizeOptionalPositiveNumber(params.get('adminNo'));
        document.getElementById('taskHistoryAdminKeyword').value = (params.get('adminKeyword') || '').slice(0, 100);
        document.getElementById('taskHistoryStartDate').value = this.normalizeDateInput(params.get('startDate'));
        document.getElementById('taskHistoryEndDate').value = this.normalizeDateInput(params.get('endDate'));
        this.state.logNo = this.normalizeOptionalPositiveNumber(params.get('logNo'));
        this.state.page = this.normalizePage(params.get('page'));
        this.state.size = this.normalizePageSize(params.get('size'));
        this.state.returnTo = CommonJS.normalizeAdminReturnPath(params.get('returnTo'), '/admin/settings/tasks');
        this.state.source = params.get('source') || '';
        document.getElementById('taskHistoryPageSize').value = String(this.state.size);
        this.syncQuickFilterState();
        this.syncDatePresetState();
        CommonJS.bindMainLogoNavigation(this.state.returnTo);
        CommonJS.renderSourceContextNotice({ noticeId: 'taskHistorySourceContextNotice', source: this.state.source });
    },

    buildParams() {
        const params = new URLSearchParams();
        const taskNo = document.getElementById('taskHistoryTaskNo').value.trim();
        const actionType = document.getElementById('taskHistoryActionType').value || 'TASK_';
        const adminNo = document.getElementById('taskHistoryAdminNo').value.trim();
        const adminKeyword = document.getElementById('taskHistoryAdminKeyword').value.trim();
        const startDate = document.getElementById('taskHistoryStartDate').value;
        const endDate = document.getElementById('taskHistoryEndDate').value;

        if (taskNo) params.set('taskNo', taskNo);
        if (actionType && actionType !== 'TASK_') params.set('actionType', actionType);
        if (adminNo) params.set('adminNo', adminNo);
        if (adminKeyword) params.set('adminKeyword', adminKeyword);
        if (startDate) params.set('startDate', startDate);
        if (endDate) params.set('endDate', endDate);
        if (this.state.logNo) params.set('logNo', this.state.logNo);
        if (this.state.returnTo && this.state.returnTo !== '/admin/settings/tasks') params.set('returnTo', this.state.returnTo);
        if (this.state.source) params.set('source', this.state.source);
        params.set('page', String(this.state.page));
        params.set('size', String(this.state.size));
        return params;
    },

    async loadHistory() {
        const requestId = ++this.listRequestId;
        if (!this.validateState()) {
            return;
        }
        const startDate = document.getElementById('taskHistoryStartDate')?.value;
        const endDate = document.getElementById('taskHistoryEndDate')?.value;
        if (startDate && endDate && startDate > endDate) {
            this.renderError('시작일은 종료일보다 늦을 수 없습니다.');
            return;
        }
        const params = this.buildParams();
        history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
        this.setMetaText('운영 작업 이력을 불러오는 중입니다...');
        this.setResultMetaText('결과 메타를 계산하는 중입니다...');
        this.setPageMetaText('페이지 메타 계산 중');
        this.renderLoadingState();

        try {
            const response = await fetch(`/api/admin/settings/tasks/history/list?${params.toString()}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 작업 이력을 불러오지 못했습니다.'));
            }
            const data = await response.json();
            if (requestId !== this.listRequestId) {
                return;
            }
            const items = Array.isArray(data.items) ? data.items : [];
            const validItems = this.normalizeHistoryItems(items);
            this.renderList(validItems);
            this.renderMeta(data);
            this.renderPagination(data);
            this.renderResultSummary(data);
            this.setListStateMeta('ready', '', validItems.length, this.normalizeNonNegativeInteger(data.totalElements), this.normalizeNonNegativeInteger(data.resultMeta?.filterCount), data.resultMeta?.querySignature || '', data.resultMeta?.pageInfoLabel || data.pageInfoLabel || '');
            await this.openDeepLinkedLogIfNeeded(validItems);
        } catch (error) {
            if (requestId !== this.listRequestId) {
                return;
            }
            this.renderError(error.message);
        }
    },

    renderList(items) {
        const tbody = document.getElementById('taskHistoryBody');
        const validItems = this.normalizeHistoryItems(items);
        if (!validItems.length) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="py-5">
                        <div class="product-empty-state">
                            <i class="fas fa-list-check product-empty-state-icon"></i>
                            <strong>조건에 맞는 운영 작업 이력이 없습니다.</strong>
                            <p>${this.escapeHtml(this.buildEmptyStateMessage())}</p>
                        </div>
                    </td>
                </tr>
            `;
            this.setListStateMeta('empty', '조건에 맞는 운영 작업 이력이 없습니다.', 0, 0, 0, '', '');
            return;
        }

        tbody.innerHTML = validItems.flatMap((item) => {
            const logNo = this.normalizeOptionalPositiveNumber(item.logNo);
            const taskNo = this.normalizeOptionalPositiveNumber(item.taskNo);
            const taskPath = this.buildTaskDetailPath(item.taskPath);
            if (!logNo) return [];
            return [`
            <tr data-log-row="${logNo}">
                <td class="ps-4 text-muted small">${logNo}</td>
                <td>${taskPath !== '#' ? `<a class="text-decoration-none fw-bold" href="${taskPath}">${this.escapeHtml(item.taskLabel || '-')}</a>` : this.escapeHtml(item.taskLabel || '-')}</td>
                <td><span class="badge bg-dark">${this.escapeHtml(item.actionLabel)}</span></td>
                <td>${this.formatAdminLabel(item.adminName, item.adminNo)}</td>
                <td><code class="small">${this.escapeHtml(item.ipAddress || '-')}</code></td>
                <td class="text-center">
                    <div class="d-flex justify-content-center gap-2 flex-wrap">
                        <button type="button" class="btn btn-sm btn-outline-dark" data-role="open-task-log-detail" data-log-no="${logNo}">상세</button>
                        ${taskPath !== '#' ? `<a class="btn btn-sm btn-outline-secondary" href="${taskPath}">작업</a>` : ''}
                        ${taskNo ? `<a class="btn btn-sm btn-outline-secondary" href="${this.buildTaskLogPath(taskNo)}">활동 로그</a>` : ''}
                    </div>
                </td>
                <td class="text-end pe-4 small text-muted">${this.escapeHtml(item.actionDtm || '-')}</td>
            </tr>
        `];
        }).join('');
    },

    renderMeta(data) {
        CommonJS.renderListMeta({
            metaTextId: 'taskHistoryMetaText',
            filterMetaId: 'taskHistoryFilterMeta',
            resultMetaId: 'taskHistoryResultMeta',
            pageMetaId: 'taskHistoryPageMeta',
            resultLabel: data.pageInfoLabel || `${this.formatCount(data.rangeStart)}-${this.formatCount(data.rangeEnd)} / ${this.formatCount(data.totalElements)}건`,
            filterCount: this.normalizeNonNegativeInteger(data.resultMeta?.filterCount),
            querySignature: data.resultMeta?.querySignature || '',
            pageInfoLabel: data.resultMeta?.pageInfoLabel || data.pageInfoLabel || '',
            filterPrefix: '적용 필터',
            defaultResultText: '결과 메타 없음',
            defaultPageText: '페이지 메타 없음'
        });
        const metaEl = document.getElementById('taskHistoryStateMeta');
        if (metaEl) {
            metaEl.dataset.filterCount = String(this.normalizeNonNegativeInteger(data.resultMeta?.filterCount));
            metaEl.dataset.querySignature = data.resultMeta?.querySignature || '';
            metaEl.dataset.pageInfoLabel = data.resultMeta?.pageInfoLabel || data.pageInfoLabel || '';
            metaEl.dataset.sourceContext = this.state.source || '';
        }
        CommonJS.renderSourceContextNotice({ noticeId: 'taskHistorySourceContextNotice', source: this.state.source });
    },

    renderPagination(data) {
        const pagination = document.getElementById('taskHistoryPagination');
        if (!pagination) return;
        const totalPages = this.normalizeNonNegativeInteger(data.totalPages);
        const currentPage = Math.min(this.normalizePage(data.currentPage), Math.max(totalPages - 1, 0));
        if (totalPages <= 1) {
            pagination.innerHTML = '';
            return;
        }

        pagination.innerHTML = this.buildPaginationPages(currentPage, totalPages).map((page) => page == null
            ? '<li class="page-item disabled"><span class="page-link">…</span></li>'
            : `
                <li class="page-item ${page === currentPage ? 'active' : ''}">
                    <button type="button" class="page-link" data-role="go-task-history-page" data-page="${page}">${page + 1}</button>
                </li>
            `).join('');
        pagination.querySelectorAll('[data-role="go-task-history-page"]').forEach((button) => {
            button.addEventListener('click', () => this.goPage(this.normalizePage(button.dataset.page)));
        });
    },

    async openDetail(logNo) {
        if (!this.isPositiveNumber(logNo)) {
            await CommonJS.alert('상세 로그 번호가 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        const requestId = ++this.detailRequestId;
        this.renderDetailState('loading', '로그 상세를 불러오는 중입니다.', '선택한 작업 이력의 상세 정보와 바로가기를 준비하고 있습니다.');
        this.setDetailStateMeta('loading', '로그 상세를 불러오는 중입니다.', logNo, '', '');
        this.modal.show();
        try {
            const response = await fetch(`/api/admin/logs/get?no=${logNo}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '상세 로그를 불러오지 못했습니다.'));
            }
            const data = await response.json();
            if (requestId !== this.detailRequestId) {
                return;
            }
            if (this.normalizeOptionalPositiveNumber(data?.logNo) !== String(logNo)) {
                throw new Error('요청한 작업 이력과 상세 응답이 일치하지 않습니다.');
            }
            const detailLogPath = this.buildTaskLogPath(data.targetId || '');
            const targetPath = this.buildTaskDetailPath(data.targetPath || '');
            const safeDetailLogPath = detailLogPath === '#' ? '' : detailLogPath;
            const safeTargetPath = targetPath === '#' ? '' : targetPath;
            document.getElementById('taskHistoryDetailBody').innerHTML = `
                <div class="admin-modal-detail-grid">
                    <div class="admin-modal-detail-item admin-modal-detail-item--span-6">
                        <div class="admin-modal-detail-label">로그 번호</div>
                        <div class="admin-modal-detail-value">${this.escapeHtml(data.logNo)}</div>
                    </div>
                    <div class="admin-modal-detail-item admin-modal-detail-item--span-6">
                        <div class="admin-modal-detail-label">작업 일시</div>
                        <div class="admin-modal-detail-value">${this.escapeHtml(data.actionDtm || '-')}</div>
                    </div>
                    <div class="admin-modal-detail-item admin-modal-detail-item--span-6">
                        <div class="admin-modal-detail-label">관리자</div>
                        <div class="admin-modal-detail-value">${this.formatAdminLabel(data.adminName, data.adminNo)}</div>
                    </div>
                    <div class="admin-modal-detail-item admin-modal-detail-item--span-6">
                        <div class="admin-modal-detail-label">작업 종류</div>
                        <div class="admin-modal-detail-value">${this.escapeHtml(data.actionType || '-')}</div>
                    </div>
                    <div class="admin-modal-detail-item admin-modal-detail-item--span-12">
                        <div class="admin-modal-detail-label">대상</div>
                        <div class="admin-modal-detail-value">${safeTargetPath ? `<a class="text-decoration-none" href="${safeTargetPath}">${this.escapeHtml(data.targetLabel || '-')}</a>` : this.escapeHtml(data.targetLabel || '-')}</div>
                    </div>
                    <div class="admin-modal-detail-item admin-modal-detail-item--span-12">
                        <div class="admin-modal-detail-label">IP 주소</div>
                        <div class="admin-modal-detail-value"><code>${this.escapeHtml(data.ipAddress || '-')}</code></div>
                    </div>
                </div>
            `;
            this.setDetailFooterLinks(safeTargetPath, safeDetailLogPath);
            this.setDetailStateMeta('ready', '', logNo, safeTargetPath, safeDetailLogPath);
            this.state.logNo = String(logNo);
            const listMetaEl = document.getElementById('taskHistoryStateMeta');
            if (listMetaEl) {
                listMetaEl.dataset.lastOpenedLogNo = String(logNo);
            }
            this.highlightLogRow(logNo);
            history.replaceState(null, '', `${window.location.pathname}?${this.buildParams().toString()}`);
        } catch (error) {
            if (requestId !== this.detailRequestId) {
                return;
            }
            this.renderDetailState('error', '상세 로그를 불러오지 못했습니다.', error.message);
            this.setDetailFooterLinks('', '');
            this.setDetailStateMeta('error', error.message, logNo, '', '');
        }
    },

    renderError(message) {
        const tbody = document.getElementById('taskHistoryBody');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="py-5">
                        <div class="product-empty-state">
                            <div class="product-empty-state__icon text-danger">
                                <i class="fa-solid fa-triangle-exclamation"></i>
                            </div>
                            <strong>운영 작업 이력을 불러오지 못했습니다.</strong>
                            <p>${this.escapeHtml(message)}</p>
                        </div>
                    </td>
                </tr>
            `;
        }
        this.setMetaText('이력 조회 실패');
        document.getElementById('taskHistoryFilterMeta').textContent = '적용 필터 확인 불가';
        this.setResultMetaText(message);
        this.setPageMetaText('페이지 메타 확인 불가');
        document.getElementById('taskHistoryResultSummary').textContent = '운영 작업 이력 조회에 실패했습니다.';
        document.getElementById('taskHistoryPagination').innerHTML = '';
        this.setListStateMeta('error', message, 0, 0, 0, '', '');
    },

    syncQuickFilterState() {
        const currentActionType = this.normalizeActionType(document.getElementById('taskHistoryActionType')?.value);
        document.querySelectorAll('.task-history-quick-filter[data-action-type]').forEach((button) => {
            const active = this.normalizeActionType(button.dataset.actionType) === currentActionType;
            button.classList.toggle('active', active);
            button.classList.toggle('btn-dark', active);
            button.classList.toggle('btn-outline-dark', !active);
        });
    },

    syncReturnLinks() {
        const returnContext = CommonJS.getReturnContext(this.state.returnTo, '운영 작업');
        const breadcrumbLink = document.getElementById('taskHistoryBreadcrumbLink');
        if (breadcrumbLink) {
            breadcrumbLink.href = this.state.returnTo;
            breadcrumbLink.textContent = returnContext.label;
        }
        const backButton = document.getElementById('btnBackToTaskSource');
        if (backButton) {
            backButton.textContent = `${returnContext.label}로 돌아가기`;
        }
    },

    buildTaskDetailPath(basePath) {
        const safeBasePath = CommonJS.normalizeAdminReturnPath(basePath, '');
        if (!safeBasePath) {
            return '#';
        }
        const [path, rawQuery = ''] = safeBasePath.split('?');
        const params = new URLSearchParams(rawQuery);
        params.set('returnTo', window.location.pathname + window.location.search);
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        return `${path}?${params.toString()}`;
    },

    buildTaskLogPath(taskNo) {
        const safeTaskNo = this.normalizeOptionalPositiveNumber(taskNo);
        if (!safeTaskNo) return '#';
        const params = new URLSearchParams();
        params.set('actionType', 'TASK_');
        params.set('targetId', safeTaskNo);
        params.set('returnTo', window.location.pathname + window.location.search);
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        return `/admin/logs?${params.toString()}`;
    },

    renderResultSummary(data) {
        const summary = document.getElementById('taskHistoryResultSummary');
        if (summary) {
            summary.textContent = data.resultMeta?.querySignature || '운영 작업 로그를 기준으로 변경 이력을 조회합니다.';
        }
    },

    setMetaText(message) {
        document.getElementById('taskHistoryMetaText').textContent = message;
    },

    setResultMetaText(message) {
        const resultMeta = document.getElementById('taskHistoryResultMeta');
        if (resultMeta) {
            resultMeta.textContent = message;
        }
    },

    setPageMetaText(message) {
        const pageMeta = document.getElementById('taskHistoryPageMeta');
        if (pageMeta) {
            pageMeta.textContent = message;
        }
    },

    goPage(page) {
        if (!Number.isInteger(page) || page < 0) {
            void CommonJS.alert('이동할 페이지 정보가 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        this.state.page = page;
        this.loadHistory();
    },

    async exportCsv() {
        if (this.isExporting) {
            return;
        }
        const button = document.getElementById('btnExportTaskHistoryCsv');
        try {
            if (!this.validateState()) {
                return;
            }
            this.isExporting = true;
            CommonJS.setButtonDisabled(button, true, '내보내는 중입니다.');
            const startDate = document.getElementById('taskHistoryStartDate')?.value || '';
            const endDate = document.getElementById('taskHistoryEndDate')?.value || '';
            if (startDate && endDate && startDate > endDate) {
                throw new Error('시작일은 종료일보다 늦을 수 없습니다.');
            }
            const params = this.buildParams();
            params.delete('page');
            params.delete('size');
            params.delete('logNo');
            await CommonJS.downloadFile(`/api/admin/settings/tasks/history/export?${params.toString()}`, 'task-history.csv');
        } catch (error) {
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isExporting = false;
            CommonJS.setButtonDisabled(button, false);
        }
    },

    resetFilters() {
        document.getElementById('taskHistoryTaskNo').value = '';
        document.getElementById('taskHistoryActionType').value = 'TASK_';
        document.getElementById('taskHistoryAdminNo').value = '';
        document.getElementById('taskHistoryAdminKeyword').value = '';
        document.getElementById('taskHistoryStartDate').value = '';
        document.getElementById('taskHistoryEndDate').value = '';
        document.getElementById('taskHistoryPageSize').value = '20';
        this.state.page = 0;
        this.state.size = 20;
        this.state.logNo = '';
        this.syncQuickFilterState();
        this.syncDatePresetState();
        this.loadHistory();
    },

    renderLoadingState() {
        const tbody = document.getElementById('taskHistoryBody');
        if (!tbody) {
            return;
        }
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="py-5">
                    <div class="product-loading-state">
                        <div class="spinner-border spinner-border-sm text-primary" role="status" aria-hidden="true"></div>
                        <strong>운영 작업 이력을 불러오는 중입니다.</strong>
                        <p>현재 필터 조건에 맞는 작업 변경 로그를 조회하고 있습니다.</p>
                    </div>
                </td>
            </tr>
        `;
    },

    renderDetailState(type, title, description) {
        const body = document.getElementById('taskHistoryDetailBody');
        if (!body) return;

        if (type === 'loading') {
            body.innerHTML = `
                <div class="product-loading-state py-4">
                    <div class="spinner-border spinner-border-sm text-primary" role="status" aria-hidden="true"></div>
                    <strong>${this.escapeHtml(title)}</strong>
                    <p>${this.escapeHtml(description)}</p>
                </div>
            `;
            return;
        }

        body.innerHTML = `
            <div class="product-empty-state py-4">
                <div class="product-empty-state__icon text-danger">
                    <i class="fa-solid fa-triangle-exclamation"></i>
                </div>
                <strong>${this.escapeHtml(title)}</strong>
                <p>${this.escapeHtml(description)}</p>
            </div>
        `;
    },

    buildEmptyStateMessage() {
        const parts = [];
        const taskNo = document.getElementById('taskHistoryTaskNo')?.value.trim();
        const actionType = document.getElementById('taskHistoryActionType')?.value;
        const adminNo = document.getElementById('taskHistoryAdminNo')?.value.trim();
        const adminKeyword = CommonJS.normalizeOptionalText(document.getElementById('taskHistoryAdminKeyword')?.value);
        const startDate = document.getElementById('taskHistoryStartDate')?.value;
        const endDate = document.getElementById('taskHistoryEndDate')?.value;

        if (taskNo) parts.push(`작업 번호 ${taskNo}`);
        if (actionType && actionType !== 'TASK_') parts.push(`작업 유형 ${actionType}`);
        if (adminNo) parts.push(`관리자 번호 ${adminNo}`);
        if (adminKeyword) parts.push(`관리자 "${adminKeyword}"`);
        if (startDate || endDate) parts.push(`기간 ${startDate || '전체'} ~ ${endDate || '전체'}`);

        if (!parts.length) {
            return '운영 작업 이력이 아직 없거나, 현재 페이지에 표시할 데이터가 없습니다.';
        }

        return `${parts.join(', ')} 조건에 맞는 운영 작업 이력이 없습니다.`;
    },

    applyDatePreset(preset) {
        const normalizedPreset = this.normalizeDatePreset(preset);
        const startDateInput = document.getElementById('taskHistoryStartDate');
        const endDateInput = document.getElementById('taskHistoryEndDate');
        if (!startDateInput || !endDateInput) {
            return;
        }

        const today = new Date();
        const formatDate = (value) => {
            const year = value.getFullYear();
            const month = String(value.getMonth() + 1).padStart(2, '0');
            const day = String(value.getDate()).padStart(2, '0');
            return `${year}-${month}-${day}`;
        };

        if (normalizedPreset === 'clear') {
            startDateInput.value = '';
            endDateInput.value = '';
        } else {
            const startDate = new Date(today);
            if (normalizedPreset === '7days') {
                startDate.setDate(startDate.getDate() - 6);
            } else if (normalizedPreset === '30days') {
                startDate.setDate(startDate.getDate() - 29);
            }
            startDateInput.value = formatDate(startDate);
            endDateInput.value = formatDate(today);
        }

        this.state.page = 0;
        this.syncDatePresetState();
        this.loadHistory();
    },

    syncDatePresetState() {
        const startDate = document.getElementById('taskHistoryStartDate')?.value || '';
        const endDate = document.getElementById('taskHistoryEndDate')?.value || '';
        const today = new Date();
        const formatDate = (value) => {
            const year = value.getFullYear();
            const month = String(value.getMonth() + 1).padStart(2, '0');
            const day = String(value.getDate()).padStart(2, '0');
            return `${year}-${month}-${day}`;
        };
        const todayLabel = formatDate(today);
        const sevenDaysAgo = new Date(today);
        sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 6);
        const thirtyDaysAgo = new Date(today);
        thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 29);

        document.querySelectorAll('[data-task-history-date-preset]').forEach((button) => {
            const preset = this.normalizeDatePreset(button.dataset.taskHistoryDatePreset);
            const active = (
                (preset === 'today' && startDate === todayLabel && endDate === todayLabel) ||
                (preset === '7days' && startDate === formatDate(sevenDaysAgo) && endDate === todayLabel) ||
                (preset === '30days' && startDate === formatDate(thirtyDaysAgo) && endDate === todayLabel) ||
                (preset === 'clear' && !startDate && !endDate)
            );
            button.classList.toggle('btn-secondary', active);
            button.classList.toggle('btn-outline-secondary', !active);
        });
    },

    async openDeepLinkedLogIfNeeded(items) {
        if (!this.state.logNo) return;
        const logNo = this.normalizeOptionalPositiveNumber(this.state.logNo);
        if (logNo == null) {
            this.state.logNo = '';
            return;
        }
        const target = items.find((item) => this.normalizeOptionalPositiveNumber(item.logNo) === logNo);
        if (target || logNo) await this.openDetail(logNo);
        this.state.logNo = '';
        history.replaceState(null, '', `${window.location.pathname}?${this.buildParams().toString()}`);
    },

    highlightLogRow(logNo) {
        const targetLogNo = Number(logNo || 0);
        document.querySelectorAll('[data-log-row]').forEach((row) => {
            row.classList.remove('table-warning');
        });
        if (!targetLogNo) {
            return;
        }
        const row = document.querySelector(`[data-log-row="${targetLogNo}"]`);
        if (!row) {
            return;
        }
        row.classList.add('table-warning');
        row.scrollIntoView({ block: 'center', behavior: 'smooth' });
    },

    formatAdminLabel(adminName, adminNo) {
        const safeAdminNo = this.normalizeOptionalPositiveNumber(adminNo);
        return safeAdminNo
            ? `${this.escapeHtml(adminName || '-')} (#${safeAdminNo})`
            : this.escapeHtml(adminName || '-');
    },

    escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    },

    setListStateMeta(state, message, visibleCount, totalElements, filterCount, querySignature, pageInfoLabel) {
        const metaEl = document.getElementById('taskHistoryStateMeta');
        if (!metaEl) return;
        metaEl.dataset.listState = state;
        metaEl.dataset.stateMessage = message || '';
        metaEl.dataset.visibleCount = String(visibleCount ?? 0);
        metaEl.dataset.totalElements = String(totalElements ?? 0);
        metaEl.dataset.filterCount = String(filterCount ?? 0);
        metaEl.dataset.querySignature = querySignature || '';
        metaEl.dataset.pageInfoLabel = pageInfoLabel || '';
    },

    setDetailStateMeta(state, message, logNo, targetPath, logPath) {
        const metaEl = document.getElementById('taskHistoryDetailStateMeta');
        if (!metaEl) return;
        metaEl.dataset.detailState = state;
        metaEl.dataset.stateMessage = message || '';
        metaEl.dataset.logNo = logNo == null ? '' : String(logNo);
        metaEl.dataset.targetPath = targetPath || '';
        metaEl.dataset.logPath = logPath || '';
    },

    setDetailFooterLinks(targetPath, logPath) {
        const targetButton = document.getElementById('btnTaskHistoryDetailTarget');
        const logButton = document.getElementById('btnTaskHistoryDetailLog');
        if (targetButton) {
            targetButton.href = targetPath || '#';
            targetButton.classList.toggle('d-none', !targetPath);
        }
        if (logButton) {
            logButton.href = logPath || '#';
            logButton.classList.toggle('d-none', !logPath);
        }
    },

    validateState() {
        const taskNo = document.getElementById('taskHistoryTaskNo')?.value.trim() || '';
        const adminNo = document.getElementById('taskHistoryAdminNo')?.value.trim() || '';
        const actionType = document.getElementById('taskHistoryActionType')?.value || 'TASK_';
        const adminKeyword = document.getElementById('taskHistoryAdminKeyword')?.value.trim() || '';
        if (taskNo && !this.isPositiveNumber(taskNo)) {
            void CommonJS.alert('작업 번호는 1 이상의 숫자만 입력할 수 있습니다.', '알림', 'warning');
            return false;
        }
        if (adminNo && !this.isPositiveNumber(adminNo)) {
            void CommonJS.alert('관리자 번호는 1 이상의 숫자만 입력할 수 있습니다.', '알림', 'warning');
            return false;
        }
        if (!this.isValidActionType(actionType)) {
            void CommonJS.alert('작업 종류 필터 값이 올바르지 않습니다.', '알림', 'warning');
            return false;
        }
        if (adminKeyword.length > 100) {
            void CommonJS.alert('관리자 검색어는 100자 이하로 입력하세요.', '알림', 'warning');
            return false;
        }
        return true;
    },

    normalizePage(page) {
        const parsed = Number(page);
        return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0;
    },

    normalizePageSize(size) {
        const parsed = Number(size);
        return [20, 50, 100].includes(parsed) ? parsed : 20;
    },

    normalizeOptionalPositiveNumber(value) {
        return this.isPositiveNumber(value) ? String(Number(value)) : '';
    },

    normalizeDatePreset(value) {
        return ['today', '7days', '30days', 'clear'].includes(value) ? value : 'today';
    },

    normalizeActionType(value) {
        return this.isValidActionType(value) ? String(value) : 'TASK_';
    },

    isValidActionType(value) {
        return [
            'TASK_',
            'TASK_CREATE',
            'TASK_UPDATE',
            'TASK_STATUS_UPDATE',
            'TASK_BULK_UPDATE',
            'TASK_COMMENT_CREATE',
            'TASK_COMMENT_UPDATE',
            'TASK_COMMENT_DELETE',
            'TASK_BULK_DELETE',
            'TASK_DELETE'
        ].includes(String(value || ''));
    },

    normalizeDateInput(value) {
        const text = String(value || '');
        if (!/^\d{4}-\d{2}-\d{2}$/.test(text)) return '';
        const date = new Date(`${text}T00:00:00`);
        return Number.isFinite(date.getTime()) && this.formatLocalDate(date) === text ? text : '';
    },

    formatLocalDate(value) {
        const year = value.getFullYear();
        const month = String(value.getMonth() + 1).padStart(2, '0');
        const day = String(value.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    },

    normalizeNonNegativeInteger(value) {
        const number = Number(value);
        return Number.isInteger(number) && number >= 0 ? number : 0;
    },

    formatCount(value) {
        return this.normalizeNonNegativeInteger(value).toLocaleString();
    },

    normalizeHistoryItems(items) {
        return Array.isArray(items)
            ? items.filter((item) => this.normalizeOptionalPositiveNumber(item?.logNo))
            : [];
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

    isPositiveNumber(value) {
        return /^\d+$/.test(String(value || '')) && Number(value) > 0;
    },

};

document.addEventListener('DOMContentLoaded', () => TaskHistoryPage.init());
