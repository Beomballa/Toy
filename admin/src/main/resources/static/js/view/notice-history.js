const NoticeHistoryPage = {
    initialized: false,
    modal: null,
    isOpeningDetail: false,
    isExporting: false,
    state: {
        page: 0,
        size: 20,
        returnTo: '/admin/settings/notices',
        source: '',
        logNo: ''
    },

    init() {
        if (this.initialized) return;
        this.initialized = true;
        this.modal = new bootstrap.Modal(document.getElementById('noticeHistoryDetailModal'));
        this.bindEvents();
        this.readStateFromUrl();
        this.syncReturnLinks();
        this.loadHistory();
    },

    bindEvents() {
        document.getElementById('btnSearchNoticeHistory')?.addEventListener('click', () => {
            this.state.page = 0;
            this.loadHistory();
        });
        document.getElementById('btnExportNoticeHistoryCsv')?.addEventListener('click', () => this.exportCsv());
        document.getElementById('btnResetNoticeHistory')?.addEventListener('click', () => this.resetFilters());
        document.getElementById('noticeHistoryPageSize')?.addEventListener('change', () => {
            this.state.page = 0;
            this.state.size = this.normalizePageSize(document.getElementById('noticeHistoryPageSize')?.value);
            this.loadHistory();
        });
        ['noticeHistoryNoticeNo', 'noticeHistoryAdminNo', 'noticeHistoryAdminKeyword', 'noticeHistoryStartDate', 'noticeHistoryEndDate'].forEach((id) => {
            document.getElementById(id)?.addEventListener('keydown', (event) => {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    this.state.page = 0;
                    this.loadHistory();
                }
            });
        });
        document.querySelectorAll('.notice-history-quick-filter[data-action-type]').forEach((button) => {
            button.addEventListener('click', () => {
                document.getElementById('noticeHistoryActionType').value = this.normalizeActionType(button.dataset.actionType);
                this.state.page = 0;
                this.syncQuickFilterState();
                this.loadHistory();
            });
        });
        document.querySelectorAll('[data-notice-history-date-preset]').forEach((button) => {
            button.addEventListener('click', () => this.applyDatePreset(this.normalizeDatePreset(button.dataset.noticeHistoryDatePreset)));
        });
        document.getElementById('btnBackToNoticeSource')?.addEventListener('click', () => {
            window.location.href = this.state.returnTo;
        });
        document.getElementById('noticeHistoryBody')?.addEventListener('click', (event) => {
            const detailButton = event.target.closest('[data-role="open-notice-log-detail"]');
            if (detailButton) {
                const logNo = this.normalizeOptionalPositiveNumber(detailButton.dataset.logNo);
                if (!logNo) {
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
        document.getElementById('noticeHistoryNoticeNo').value = params.get('noticeNo') || '';
        document.getElementById('noticeHistoryActionType').value = this.normalizeActionType(params.get('actionType'));
        document.getElementById('noticeHistoryAdminNo').value = params.get('adminNo') || '';
        document.getElementById('noticeHistoryAdminKeyword').value = params.get('adminKeyword') || '';
        document.getElementById('noticeHistoryStartDate').value = params.get('startDate') || '';
        document.getElementById('noticeHistoryEndDate').value = params.get('endDate') || '';
        this.state.page = this.normalizePage(params.get('page'));
        this.state.size = this.normalizePageSize(params.get('size'));
        this.state.returnTo = params.get('returnTo') || '/admin/settings/notices';
        this.state.source = params.get('source') || '';
        this.state.logNo = this.normalizeOptionalPositiveNumber(params.get('logNo'));
        document.getElementById('noticeHistoryPageSize').value = String(this.state.size);
        this.syncQuickFilterState();
        this.syncDatePresetState();
        CommonJS.renderSourceContextNotice({ noticeId: 'noticeHistorySourceContextNotice', source: this.state.source });
        CommonJS.bindMainLogoNavigation(this.state.returnTo);
    },

    buildParams() {
        const params = new URLSearchParams();
        const noticeNo = document.getElementById('noticeHistoryNoticeNo').value.trim();
        const actionType = this.normalizeActionType(document.getElementById('noticeHistoryActionType').value);
        const adminNo = document.getElementById('noticeHistoryAdminNo').value.trim();
        const adminKeyword = CommonJS.normalizeOptionalText(document.getElementById('noticeHistoryAdminKeyword').value) || '';
        const startDate = document.getElementById('noticeHistoryStartDate').value;
        const endDate = document.getElementById('noticeHistoryEndDate').value;

        if (noticeNo) params.set('noticeNo', noticeNo);
        if (actionType && actionType !== 'NOTICE_') params.set('actionType', actionType);
        if (adminNo) params.set('adminNo', adminNo);
        if (adminKeyword) params.set('adminKeyword', adminKeyword);
        if (startDate) params.set('startDate', startDate);
        if (endDate) params.set('endDate', endDate);
        if (this.state.logNo) params.set('logNo', this.state.logNo);
        if (this.state.returnTo && this.state.returnTo !== '/admin/settings/notices') params.set('returnTo', this.state.returnTo);
        if (this.state.source) params.set('source', this.state.source);
        this.state.size = this.normalizePageSize(document.getElementById('noticeHistoryPageSize')?.value);
        params.set('page', String(this.state.page));
        params.set('size', String(this.state.size));
        return params;
    },

    async loadHistory() {
        if (!this.validateState()) {
            return;
        }
        const startDate = document.getElementById('noticeHistoryStartDate')?.value;
        const endDate = document.getElementById('noticeHistoryEndDate')?.value;
        if (startDate && endDate && startDate > endDate) {
            this.renderError('시작일은 종료일보다 늦을 수 없습니다.');
            return;
        }
        const params = this.buildParams();
        history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
        this.setMetaText('운영 공지 이력을 불러오는 중입니다...');
        this.setResultMetaText('결과 메타를 계산하는 중입니다...');
        this.setPageMetaText('페이지 메타 계산 중');
        this.renderLoadingState();

        try {
            const response = await fetch(`/api/admin/settings/notices/history/list?${params.toString()}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 공지 이력을 불러오지 못했습니다.'));
            }
            const data = await response.json();
            this.renderList(data.items || []);
            this.renderMeta(data);
            this.renderPagination(data);
            this.renderResultSummary(data);
            await this.openDeepLinkedLogIfNeeded(data.items || []);
        } catch (error) {
            this.renderError(error.message);
        }
    },

    renderList(items) {
        const tbody = document.getElementById('noticeHistoryBody');
        if (!items.length) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="py-5">
                        <div class="product-empty-state">
                            <i class="fas fa-bullhorn product-empty-state-icon"></i>
                            <strong>조건에 맞는 운영 공지 이력이 없습니다.</strong>
                            <p>${this.buildEmptyStateMessage()}</p>
                        </div>
                    </td>
                </tr>
            `;
            this.setListStateMeta('empty', '조건에 맞는 운영 공지 이력이 없습니다.', 0, 0, 0, '', '');
            return;
        }

        tbody.innerHTML = items.map((item) => `
            <tr data-notice-log-row="${item.logNo}">
                <td class="ps-4 text-muted small">${item.logNo}</td>
                <td>${item.noticePath ? `<a class="text-decoration-none fw-bold" href="${this.buildNoticeDetailPath(item.noticePath)}">${item.noticeLabel}</a>` : (item.noticeLabel || '-')}</td>
                <td><span class="badge bg-dark">${item.actionLabel}</span></td>
                <td>${item.adminName}${item.adminNo ? ` <span class="text-muted small">(#${item.adminNo})</span>` : ''}</td>
                <td><code class="small">${item.ipAddress || '-'}</code></td>
                <td class="text-center">
                    <button type="button" class="btn btn-sm btn-outline-dark" data-role="open-notice-log-detail" data-log-no="${item.logNo}">상세</button>
                </td>
                <td class="text-end pe-4 small text-muted">${item.actionDtm || '-'}</td>
            </tr>
        `).join('');
    },

    renderMeta(data) {
        CommonJS.renderListMeta({
            metaTextId: 'noticeHistoryMetaText',
            filterMetaId: 'noticeHistoryFilterMeta',
            resultMetaId: 'noticeHistoryResultMeta',
            pageMetaId: 'noticeHistoryPageMeta',
            resultLabel: data.pageInfoLabel || `${data.rangeStart}-${data.rangeEnd} / ${data.totalElements}건`,
            filterCount: data.resultMeta?.filterCount ?? 0,
            querySignature: data.resultMeta?.querySignature || '',
            pageInfoLabel: data.resultMeta?.pageInfoLabel || data.pageInfoLabel || '',
            filterPrefix: '적용 필터',
            defaultResultText: '결과 메타 없음',
            defaultPageText: '페이지 메타 없음'
        });
        this.setListStateMeta('ready', '', (data.items || []).length, data.totalElements || 0, data.resultMeta?.filterCount || 0, data.resultMeta?.querySignature || '', data.resultMeta?.pageInfoLabel || data.pageInfoLabel || '');
        CommonJS.renderSourceContextNotice({ noticeId: 'noticeHistorySourceContextNotice', source: this.state.source });
    },

    renderPagination(data) {
        const pagination = document.getElementById('noticeHistoryPagination');
        if (!pagination) return;
        if (!data.totalPages) {
            pagination.innerHTML = '';
            return;
        }

        let html = '';
        for (let i = 0; i < data.totalPages; i += 1) {
            html += `
                <li class="page-item ${i === data.currentPage ? 'active' : ''}">
                    <button type="button" class="page-link" data-role="go-notice-history-page" data-page="${i}">${i + 1}</button>
                </li>
            `;
        }
        pagination.innerHTML = html;
        pagination.querySelectorAll('[data-role="go-notice-history-page"]').forEach((button) => {
            button.addEventListener('click', () => this.goPage(this.normalizePage(button.dataset.page)));
        });
    },

    async openDetail(logNo) {
        if (this.isOpeningDetail) {
            return;
        }
        if (!this.isPositiveNumber(logNo)) {
            await CommonJS.alert('상세 로그 번호가 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        this.renderDetailState('loading', '로그 상세를 불러오는 중입니다.', '선택한 공지 이력의 상세 정보와 바로가기를 준비하고 있습니다.');
        this.setDetailTargetLink('');
        this.modal.show();
        try {
            this.isOpeningDetail = true;
            const response = await fetch(`/api/admin/logs/get?no=${logNo}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '상세 로그를 불러오지 못했습니다.'));
            }
            const data = await response.json();
            const targetPath = this.buildNoticeDetailPath(data.targetPath || '');
            document.getElementById('noticeHistoryDetailBody').innerHTML = `
                <div class="mb-2"><strong>로그 번호</strong> ${data.logNo}</div>
                <div class="mb-2"><strong>관리자</strong> ${data.adminName} (#${data.adminNo})</div>
                <div class="mb-2"><strong>작업 종류</strong> ${data.actionType}</div>
                <div class="mb-2"><strong>대상</strong> ${data.targetPath ? `<a class="text-decoration-none" href="${targetPath}">${data.targetLabel}</a>` : (data.targetLabel || '-')}</div>
                <div class="mb-2"><strong>IP 주소</strong> ${data.ipAddress}</div>
                <div><strong>작업 일시</strong> ${data.actionDtm}</div>
            `;
            this.setDetailTargetLink(targetPath);
            this.state.logNo = String(logNo);
            this.highlightLogRow(logNo);
            history.replaceState(null, '', `${window.location.pathname}?${this.buildParams().toString()}`);
        } catch (error) {
            this.renderDetailState('error', '상세 로그를 불러오지 못했습니다.', error.message);
            this.setDetailTargetLink('');
        } finally {
            this.isOpeningDetail = false;
        }
    },

    renderError(message) {
        const tbody = document.getElementById('noticeHistoryBody');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="py-5">
                        <div class="product-empty-state">
                            <div class="product-empty-state__icon text-danger">
                                <i class="fa-solid fa-triangle-exclamation"></i>
                            </div>
                            <strong>운영 공지 이력을 불러오지 못했습니다.</strong>
                            <p>${this.escapeHtml(message)}</p>
                        </div>
                    </td>
                </tr>
            `;
        }
        this.setMetaText('이력 조회 실패');
        document.getElementById('noticeHistoryFilterMeta').textContent = '적용 필터 확인 불가';
        this.setResultMetaText(message);
        this.setPageMetaText('페이지 메타 확인 불가');
        document.getElementById('noticeHistoryResultSummary').textContent = '운영 공지 이력 조회에 실패했습니다.';
        document.getElementById('noticeHistoryPagination').innerHTML = '';
        this.setListStateMeta('error', message, 0, 0, 0, '', '');
    },

    async exportCsv() {
        const button = document.getElementById('btnExportNoticeHistoryCsv');
        if (this.isExporting) {
            return;
        }
        try {
            this.isExporting = true;
            CommonJS.setButtonDisabled(button, true, '내보내는 중입니다.');
            if (!this.validateState()) {
                return;
            }
            const startDate = document.getElementById('noticeHistoryStartDate')?.value || '';
            const endDate = document.getElementById('noticeHistoryEndDate')?.value || '';
            if (startDate && endDate && startDate > endDate) {
                throw new Error('시작일은 종료일보다 늦을 수 없습니다.');
            }
            const params = this.buildParams();
            params.delete('page');
            params.delete('size');
            params.delete('logNo');
            await CommonJS.downloadFile(`/api/admin/settings/notices/history/export?${params.toString()}`);
        } catch (error) {
            await CommonJS.alert(error.message || '운영 공지 이력 CSV를 내보내지 못했습니다.', '오류', 'error');
        } finally {
            this.isExporting = false;
            CommonJS.setButtonDisabled(button, false);
        }
    },

    syncQuickFilterState() {
        const currentActionType = this.normalizeActionType(document.getElementById('noticeHistoryActionType')?.value);
        document.querySelectorAll('.notice-history-quick-filter[data-action-type]').forEach((button) => {
            const active = this.normalizeActionType(button.dataset.actionType) === currentActionType;
            button.classList.toggle('active', active);
            button.classList.toggle('btn-dark', active);
            button.classList.toggle('btn-outline-dark', !active);
        });
    },

    syncReturnLinks() {
        const returnContext = CommonJS.getReturnContext(this.state.returnTo, '운영 공지');
        const breadcrumbLink = document.getElementById('noticeHistoryBreadcrumbLink');
        if (breadcrumbLink) {
            breadcrumbLink.href = this.state.returnTo;
            breadcrumbLink.textContent = returnContext.label;
        }
        const backButton = document.getElementById('btnBackToNoticeSource');
        if (backButton) {
            backButton.textContent = `${returnContext.label}로 돌아가기`;
        }
    },

    renderResultSummary(data) {
        const summary = document.getElementById('noticeHistoryResultSummary');
        if (summary) {
            summary.textContent = data.resultMeta?.querySignature || '공지 작업 로그를 기준으로 변경 이력을 조회합니다.';
        }
    },

    setMetaText(message) {
        document.getElementById('noticeHistoryMetaText').textContent = message;
    },

    setResultMetaText(message) {
        const resultMeta = document.getElementById('noticeHistoryResultMeta');
        if (resultMeta) {
            resultMeta.textContent = message;
        }
    },

    setPageMetaText(message) {
        const pageMeta = document.getElementById('noticeHistoryPageMeta');
        if (pageMeta) {
            pageMeta.textContent = message;
        }
    },

    setListStateMeta(state, message, visibleCount, totalElements, filterCount, querySignature, pageInfoLabel) {
        const metaEl = document.getElementById('noticeHistoryStateMeta');
        if (!metaEl) return;
        metaEl.dataset.listState = state;
        metaEl.dataset.stateMessage = message || '';
        metaEl.dataset.visibleCount = String(visibleCount ?? 0);
        metaEl.dataset.totalElements = String(totalElements ?? 0);
        metaEl.dataset.filterCount = String(filterCount ?? 0);
        metaEl.dataset.querySignature = querySignature || '';
        metaEl.dataset.pageInfoLabel = pageInfoLabel || '';
        metaEl.dataset.sourceContext = this.state.source || '';
    },

    goPage(page) {
        if (!Number.isInteger(page) || page < 0) {
            void CommonJS.alert('이동할 페이지 정보가 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        this.state.page = page;
        this.loadHistory();
    },

    resetFilters() {
        document.getElementById('noticeHistoryNoticeNo').value = '';
        document.getElementById('noticeHistoryActionType').value = 'NOTICE_';
        document.getElementById('noticeHistoryAdminNo').value = '';
        document.getElementById('noticeHistoryAdminKeyword').value = '';
        document.getElementById('noticeHistoryStartDate').value = '';
        document.getElementById('noticeHistoryEndDate').value = '';
        document.getElementById('noticeHistoryPageSize').value = '20';
        this.state.page = 0;
        this.state.size = 20;
        this.state.logNo = '';
        this.syncQuickFilterState();
        this.syncDatePresetState();
        this.loadHistory();
    },

    renderLoadingState() {
        const tbody = document.getElementById('noticeHistoryBody');
        if (!tbody) {
            return;
        }
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="py-5">
                    <div class="product-loading-state">
                        <div class="spinner-border spinner-border-sm text-primary" role="status" aria-hidden="true"></div>
                        <strong>운영 공지 이력을 불러오는 중입니다.</strong>
                        <p>현재 필터 조건에 맞는 공지 작업 로그를 조회하고 있습니다.</p>
                    </div>
                </td>
            </tr>
        `;
    },

    renderDetailState(type, title, description) {
        const body = document.getElementById('noticeHistoryDetailBody');
        if (!body) {
            return;
        }

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
        const noticeNo = document.getElementById('noticeHistoryNoticeNo')?.value.trim();
        const actionType = document.getElementById('noticeHistoryActionType')?.value;
        const adminNo = document.getElementById('noticeHistoryAdminNo')?.value.trim();
        const adminKeyword = CommonJS.normalizeOptionalText(document.getElementById('noticeHistoryAdminKeyword')?.value);
        const startDate = document.getElementById('noticeHistoryStartDate')?.value;
        const endDate = document.getElementById('noticeHistoryEndDate')?.value;

        if (noticeNo) parts.push(`공지 번호 ${noticeNo}`);
        if (actionType && actionType !== 'NOTICE_') parts.push(`작업 유형 ${actionType}`);
        if (adminNo) parts.push(`관리자 번호 ${adminNo}`);
        if (adminKeyword) parts.push(`관리자 "${adminKeyword}"`);
        if (startDate || endDate) parts.push(`기간 ${startDate || '전체'} ~ ${endDate || '전체'}`);

        if (!parts.length) {
            return '운영 공지 이력이 아직 없거나, 현재 페이지에 표시할 데이터가 없습니다.';
        }

        return `${parts.join(', ')} 조건에 맞는 운영 공지 이력이 없습니다.`;
    },

    applyDatePreset(preset) {
        const normalizedPreset = this.normalizeDatePreset(preset);
        const startDateInput = document.getElementById('noticeHistoryStartDate');
        const endDateInput = document.getElementById('noticeHistoryEndDate');
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
        const startDate = document.getElementById('noticeHistoryStartDate')?.value || '';
        const endDate = document.getElementById('noticeHistoryEndDate')?.value || '';
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

        document.querySelectorAll('[data-notice-history-date-preset]').forEach((button) => {
            const preset = this.normalizeDatePreset(button.dataset.noticeHistoryDatePreset);
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
        if (!this.state.logNo) {
            return;
        }
        const logNo = this.normalizeOptionalPositiveNumber(this.state.logNo);
        if (!logNo) {
            this.state.logNo = '';
            return;
        }
        const hasLog = items.some((item) => item.logNo === logNo);
        if (!hasLog || this.isOpeningDetail) {
            return;
        }
        await this.openDetail(logNo);
        this.state.logNo = '';
        history.replaceState(null, '', `${window.location.pathname}?${this.buildParams().toString()}`);
    },

    highlightLogRow(logNo) {
        const targetLogNo = this.normalizeOptionalPositiveNumber(logNo);
        document.querySelectorAll('[data-notice-log-row]').forEach((row) => {
            const selected = this.normalizeOptionalPositiveNumber(row.dataset.noticeLogRow) === targetLogNo;
            row.classList.toggle('table-active', selected);
            if (selected) {
                row.scrollIntoView({ block: 'center', behavior: 'smooth' });
            }
        });
    },

    setDetailTargetLink(targetPath) {
        const targetButton = document.getElementById('btnNoticeHistoryDetailTarget');
        if (!targetButton) {
            return;
        }
        targetButton.href = targetPath || '#';
        targetButton.classList.toggle('d-none', !targetPath);
    },

    buildNoticeDetailPath(basePath) {
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

    validateState() {
        const noticeNo = document.getElementById('noticeHistoryNoticeNo')?.value.trim() || '';
        const adminNo = document.getElementById('noticeHistoryAdminNo')?.value.trim() || '';
        const actionType = document.getElementById('noticeHistoryActionType')?.value || 'NOTICE_';
        if (noticeNo && !this.isPositiveNumber(noticeNo)) {
            void CommonJS.alert('공지 번호는 1 이상의 숫자만 입력할 수 있습니다.', '알림', 'warning');
            return false;
        }
        if (adminNo && !this.isPositiveNumber(adminNo)) {
            void CommonJS.alert('관리자 번호는 1 이상의 숫자만 입력할 수 있습니다.', '알림', 'warning');
            return false;
        }
        if (!(actionType === 'NOTICE_' || actionType.startsWith('NOTICE_'))) {
            void CommonJS.alert('작업 종류 필터 값이 올바르지 않습니다.', '알림', 'warning');
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
        return Number.isInteger(parsed) && parsed > 0 ? parsed : 20;
    },

    normalizeOptionalPositiveNumber(value) {
        return this.isPositiveNumber(value) ? String(Number(value)) : '';
    },

    normalizeActionType(value) {
        return value === 'NOTICE_' || String(value || '').startsWith('NOTICE_') ? String(value || 'NOTICE_') : 'NOTICE_';
    },

    normalizeDatePreset(value) {
        return ['today', '7days', '30days', 'clear'].includes(value) ? value : 'today';
    },

    isPositiveNumber(value) {
        return /^\d+$/.test(String(value || '')) && Number(value) > 0;
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

document.addEventListener('DOMContentLoaded', () => NoticeHistoryPage.init());
