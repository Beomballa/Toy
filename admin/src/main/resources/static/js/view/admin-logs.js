const AdminLogPage = {
    initialized: false,
    modal: null,
    isLoading: false,
    isOpeningDetail: false,
    isExporting: false,
    state: {
        page: 0,
        size: 20,
        logNo: '',
        returnTo: '/admin/settings/logs',
        source: ''
    },

    init() {
        if (this.initialized) return;
        this.initialized = true;
        this.modal = new bootstrap.Modal(document.getElementById('logDetailModal'));
        this.bindEvents();
        this.readStateFromUrl();
        CommonJS.bindMainLogoNavigation(this.state.returnTo || '/admin/settings/logs');
        this.getList();
    },

    bindEvents() {
        document.querySelectorAll('[data-log-quick-filter]').forEach((button) => {
            button.addEventListener('click', () => this.applyQuickFilter(button.dataset.logQuickFilter));
        });
        document.querySelectorAll('[data-log-summary-filter]').forEach((card) => {
            card.addEventListener('click', () => this.applySummaryFilter(card.dataset.logSummaryFilter));
        });
        document.querySelectorAll('[data-log-summary-date-preset]').forEach((card) => {
            card.addEventListener('click', () => this.applySummaryDatePreset(card.dataset.logSummaryDatePreset));
        });
        document.querySelectorAll('[data-log-date-preset]').forEach((button) => {
            button.addEventListener('click', () => this.applyDatePreset(button.dataset.logDatePreset));
        });
        document.getElementById('btnSearchLog')?.addEventListener('click', () => {
            this.state.page = 0;
            this.getList();
        });
        document.getElementById('btnExportLog')?.addEventListener('click', () => {
            this.exportList();
        });
        document.getElementById('btnResetLog')?.addEventListener('click', () => {
            this.resetFilters();
        });
        document.getElementById('logPageSize')?.addEventListener('change', () => {
            this.state.page = 0;
            this.state.size = Number(document.getElementById('logPageSize')?.value || 20);
            this.getList();
        });
        document.getElementById('logListBody')?.addEventListener('click', (event) => {
            const detailButton = event.target.closest('[data-role="open-log-detail"]');
            if (detailButton) {
                this.openDetail(Number(detailButton.dataset.logNo));
            }
        });
        ['logAdminNo', 'logAdminKeyword', 'logActionType', 'logTargetId', 'logStartDate', 'logEndDate'].forEach((id) => {
            document.getElementById(id)?.addEventListener('keydown', (event) => {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    this.state.page = 0;
                    this.getList();
                }
            });
        });
        window.addEventListener('popstate', () => {
            this.readStateFromUrl();
            this.syncQuickFilterState();
            this.syncDatePresetState();
            this.getList();
        });
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        document.getElementById('logAdminNo').value = params.get('adminNo') || '';
        document.getElementById('logAdminKeyword').value = params.get('adminKeyword') || '';
        document.getElementById('logActionType').value = params.get('actionType') || '';
        document.getElementById('logTargetId').value = params.get('targetId') || '';
        document.getElementById('logStartDate').value = params.get('startDate') || '';
        document.getElementById('logEndDate').value = params.get('endDate') || '';
        this.state.logNo = params.get('logNo') || '';
        this.state.page = Number(params.get('page') || 0);
        this.state.size = Number(params.get('size') || 20);
        this.state.returnTo = params.get('returnTo') || '/admin/settings/logs';
        this.state.source = params.get('source') || '';
        document.getElementById('logPageSize').value = String(this.state.size);
        this.syncQuickFilterState();
        this.syncDatePresetState();
        this.syncSummaryCardState();
        CommonJS.bindMainLogoNavigation(this.state.returnTo || '/admin/settings/logs');
        CommonJS.renderSourceContextNotice({ noticeId: 'adminLogSourceContextNotice', source: this.state.source });
    },

    buildParams() {
        const params = new URLSearchParams();
        const adminNo = document.getElementById('logAdminNo').value.trim();
        const adminKeyword = CommonJS.normalizeOptionalText(document.getElementById('logAdminKeyword').value);
        const actionType = CommonJS.normalizeOptionalText(document.getElementById('logActionType').value);
        const targetId = document.getElementById('logTargetId').value.trim();
        const startDate = document.getElementById('logStartDate').value;
        const endDate = document.getElementById('logEndDate').value;

        if (adminNo) params.set('adminNo', adminNo);
        if (adminKeyword) params.set('adminKeyword', adminKeyword);
        if (actionType) params.set('actionType', actionType);
        if (targetId) params.set('targetId', targetId);
        if (startDate) params.set('startDate', startDate);
        if (endDate) params.set('endDate', endDate);
        if (this.state.logNo) params.set('logNo', this.state.logNo);
        if (this.state.returnTo && this.state.returnTo !== '/admin/settings/logs') params.set('returnTo', this.state.returnTo);
        if (this.state.source) params.set('source', this.state.source);
        params.set('page', String(this.state.page));
        params.set('size', String(this.state.size));
        return params;
    },

    buildExportParams() {
        const params = this.buildParams();
        params.delete('page');
        params.delete('size');
        params.delete('logNo');
        return params;
    },

    async getList() {
        if (this.isLoading) {
            return;
        }
        const params = this.buildParams();
        history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
        this.setMetaText('데이터를 불러오는 중입니다...');
        this.setResultMetaText('결과 메타를 계산하는 중입니다...');
        this.setPageMetaText('페이지 메타 계산 중');
        this.renderLoadingState();

        try {
            this.isLoading = true;
            const res = await fetch(`/api/admin/logs/list?${params.toString()}`);
            if (!res.ok) {
                throw new Error(await CommonJS.extractErrorMessage(res, '로그를 불러오지 못했습니다.'));
            }
            const data = await res.json();
            this.renderSummary(data.summary);
            this.renderList(data.items || []);
            this.renderMeta(data);
            this.renderPagination(data);
            await this.openDeepLinkedLogIfNeeded(data.items || []);
        } catch (err) {
            document.getElementById('logListBody').innerHTML =
                `<tr><td colspan="7" class="text-center py-5 text-danger">${err.message}</td></tr>`;
            this.setMetaText('로그 조회 실패');
            document.getElementById('logFilterMeta').textContent = '적용 필터 확인 불가';
            this.setResultMetaText(err.message);
            this.setPageMetaText('페이지 메타 확인 불가');
            document.getElementById('logPagination').innerHTML = '';
        } finally {
            this.isLoading = false;
        }
    },

    renderList(items) {
        const tbody = document.getElementById('logListBody');
        if (!items.length) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center py-5 text-muted">
                        <div class="product-empty-state">
                            <i class="fas fa-clipboard-list product-empty-state-icon"></i>
                            <strong>조건에 맞는 활동 로그가 없습니다.</strong>
                            <p>${this.buildEmptyStateMessage()}</p>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }
        tbody.innerHTML = items.map(item => `
            <tr data-log-row="${item.logNo}">
                <td class="ps-4 text-muted small">${item.logNo}</td>
                <td><span class="badge bg-light text-dark">${this.formatAdminBadge(item.adminName, item.adminNo)}</span></td>
                <td><span class="fw-bold text-primary">${item.actionType}</span></td>
                <td>
                    ${item.targetPath
                        ? `<a class="text-decoration-none" href="${this.buildTargetPath(item.targetPath)}">${item.targetLabel}</a>`
                        : (item.targetLabel || '-')}
                </td>
                <td><code class="small">${item.ipAddress}</code></td>
                <td class="text-center">
                    <button type="button" class="btn btn-sm btn-outline-dark" data-role="open-log-detail" data-log-no="${item.logNo}">상세</button>
                </td>
                <td class="text-end pe-4 small text-muted">${item.actionDtm}</td>
            </tr>
        `).join('');
    },

    renderSummary(summary) {
        const safeSummary = summary || {
            totalCount: 0,
            todayCount: 0,
            noticeCount: 0,
            taskCount: 0,
            commerceCount: 0,
            adminCount: 0
        };
        this.setSummaryText('logStatTotalCount', safeSummary.totalCount);
        this.setSummaryText('logStatTodayCount', safeSummary.todayCount);
        this.setSummaryText('logStatNoticeCount', safeSummary.noticeCount);
        this.setSummaryText('logStatTaskCount', safeSummary.taskCount);
        this.setSummaryText('logStatCommerceCount', safeSummary.commerceCount);
        this.setSummaryText('logStatAdminCount', safeSummary.adminCount);
    },

    renderMeta(data) {
        this.setMetaText(data.pageInfoLabel || `${data.rangeStart}-${data.rangeEnd} / ${data.totalElements}건`);
        const filterMeta = document.getElementById('logFilterMeta');
        if (filterMeta) {
            filterMeta.textContent = `적용 필터 ${data.resultMeta?.filterCount ?? 0}개`;
        }
        this.setResultMetaText(data.resultMeta?.querySignature || '최신 로그순');
        this.setPageMetaText(data.pageInfoLabel || '페이지 메타 없음');
        CommonJS.renderSourceContextNotice({ noticeId: 'adminLogSourceContextNotice', source: this.state.source });
        this.syncSummaryCardState();
    },

    renderPagination(data) {
        const pagination = document.getElementById('logPagination');
        if (!pagination) {
            return;
        }
        if (!data.totalPages) {
            pagination.innerHTML = '';
            return;
        }

        let html = '';
        for (let i = 0; i < data.totalPages; i += 1) {
            html += `
                <li class="page-item ${i === data.currentPage ? 'active' : ''}">
                    <button type="button" class="page-link" data-role="go-log-page" data-page="${i}">${i + 1}</button>
                </li>
            `;
        }
        pagination.innerHTML = html;
        pagination.querySelectorAll('[data-role="go-log-page"]').forEach((button) => {
            button.addEventListener('click', () => this.goPage(Number(button.dataset.page)));
        });
    },

    async openDetail(logNo) {
        if (this.isOpeningDetail) {
            return;
        }
        document.getElementById('logDetailBody').textContent = '데이터를 불러오는 중입니다...';
        this.modal.show();
        try {
            this.isOpeningDetail = true;
            const res = await fetch(`/api/admin/logs/get?no=${logNo}`);
            if (!res.ok) {
                throw new Error(await CommonJS.extractErrorMessage(res, '상세 로그를 불러오지 못했습니다.'));
            }
            const data = await res.json();
            document.getElementById('logDetailBody').innerHTML = `
                <div class="mb-2"><strong>로그 번호</strong> ${data.logNo}</div>
                <div class="mb-2"><strong>관리자</strong> ${this.formatAdminLabel(data.adminName, data.adminNo)}</div>
                <div class="mb-2"><strong>작업 종류</strong> ${data.actionType}</div>
                <div class="mb-2"><strong>대상</strong> ${data.targetPath ? `<a class="text-decoration-none" href="${this.buildTargetPath(data.targetPath)}">${data.targetLabel}</a>` : (data.targetLabel || '-')}</div>
                <div class="mb-2"><strong>IP 주소</strong> ${data.ipAddress}</div>
                <div><strong>작업 일시</strong> ${data.actionDtm}</div>
            `;
            this.state.logNo = String(logNo);
            this.highlightLogRow(logNo);
            history.replaceState(null, '', `${window.location.pathname}?${this.buildParams().toString()}`);
        } catch (err) {
            document.getElementById('logDetailBody').innerHTML = `<div class="text-danger">${err.message}</div>`;
        } finally {
            this.isOpeningDetail = false;
        }
    },

    setMetaText(message) {
        document.getElementById('logMetaText').textContent = message;
    },

    setResultMetaText(message) {
        const resultMeta = document.getElementById('logResultMeta');
        if (resultMeta) {
            resultMeta.textContent = message;
        }
    },

    setPageMetaText(message) {
        const pageMeta = document.getElementById('logPageMeta');
        if (pageMeta) {
            pageMeta.textContent = message;
        }
    },

    setSummaryText(id, value) {
        const el = document.getElementById(id);
        if (!el) {
            return;
        }
        el.textContent = Number(value || 0).toLocaleString();
    },

    goPage(page) {
        this.state.page = page;
        this.getList();
    },

    resetFilters() {
        document.getElementById('logAdminNo').value = '';
        document.getElementById('logAdminKeyword').value = '';
        document.getElementById('logActionType').value = '';
        document.getElementById('logTargetId').value = '';
        document.getElementById('logStartDate').value = '';
        document.getElementById('logEndDate').value = '';
        document.getElementById('logPageSize').value = '20';
        this.state.logNo = '';
        this.state.page = 0;
        this.state.size = 20;
        this.syncQuickFilterState();
        this.syncDatePresetState();
        this.getList();
    },

    renderLoadingState() {
        const tbody = document.getElementById('logListBody');
        if (!tbody) {
            return;
        }
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center py-5 text-muted">
                    <div class="product-loading-state">
                        <div class="spinner-border spinner-border-sm text-primary" role="status" aria-hidden="true"></div>
                        <strong>활동 로그를 불러오는 중입니다.</strong>
                        <p>현재 필터 조건에 맞는 운영 로그를 조회하고 있습니다.</p>
                    </div>
                </td>
            </tr>
        `;
    },

    buildEmptyStateMessage() {
        const parts = [];
        const adminNo = document.getElementById('logAdminNo')?.value.trim();
        const adminKeyword = CommonJS.normalizeOptionalText(document.getElementById('logAdminKeyword')?.value);
        const actionType = CommonJS.normalizeOptionalText(document.getElementById('logActionType')?.value);
        const targetId = document.getElementById('logTargetId')?.value.trim();
        const startDate = document.getElementById('logStartDate')?.value;
        const endDate = document.getElementById('logEndDate')?.value;

        if (adminNo) parts.push(`관리자 번호 ${adminNo}`);
        if (adminKeyword) parts.push(`관리자명 "${adminKeyword}"`);
        if (actionType) parts.push(`작업 종류 ${actionType}`);
        if (targetId) parts.push(`대상 ID ${targetId}`);
        if (startDate || endDate) parts.push(`기간 ${startDate || '전체'} ~ ${endDate || '전체'}`);

        if (!parts.length) {
            return '등록된 로그가 아직 없거나, 현재 페이지에 표시할 데이터가 없습니다.';
        }

        return `${parts.join(', ')} 조건에 맞는 로그가 없습니다.`;
    },

    applyQuickFilter(actionType) {
        document.getElementById('logActionType').value = actionType || '';
        this.state.page = 0;
        this.syncQuickFilterState();
        this.syncSummaryCardState();
        this.getList();
    },

    applySummaryFilter(actionType) {
        document.getElementById('logActionType').value = actionType || '';
        this.state.page = 0;
        this.syncQuickFilterState();
        this.syncSummaryCardState();
        this.getList();
    },

    applySummaryDatePreset(preset) {
        this.applyDatePreset(preset);
        this.syncSummaryCardState();
    },

    applyDatePreset(preset) {
        const startDateInput = document.getElementById('logStartDate');
        const endDateInput = document.getElementById('logEndDate');
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

        if (preset === 'clear') {
            startDateInput.value = '';
            endDateInput.value = '';
        } else {
            const startDate = new Date(today);
            if (preset === '7days') {
                startDate.setDate(startDate.getDate() - 6);
            } else if (preset === '30days') {
                startDate.setDate(startDate.getDate() - 29);
            }
            startDateInput.value = formatDate(startDate);
            endDateInput.value = formatDate(today);
        }

        this.state.page = 0;
        this.syncDatePresetState();
        this.getList();
    },

    async exportList() {
        if (this.isExporting) {
            return;
        }
        const params = this.buildExportParams();
        const exportButton = document.getElementById('btnExportLog');
        try {
            this.isExporting = true;
            CommonJS.setButtonDisabled(exportButton, true, '내보내는 중입니다.');
            const startDate = document.getElementById('logStartDate')?.value || '';
            const endDate = document.getElementById('logEndDate')?.value || '';
            if (startDate && endDate && startDate > endDate) {
                throw new Error('시작일은 종료일보다 늦을 수 없습니다.');
            }
            await CommonJS.downloadFile(`/api/admin/logs/export?${params.toString()}`, 'admin-logs.csv');
        } catch (error) {
            await CommonJS.alert(error.message || '활동 로그 CSV를 내보내지 못했습니다.', '오류', 'error');
        } finally {
            this.isExporting = false;
            CommonJS.setButtonDisabled(exportButton, false);
        }
    },

    async openDeepLinkedLogIfNeeded(items) {
        if (!this.state.logNo) {
            return;
        }
        const logNo = Number(this.state.logNo);
        if (!Number.isFinite(logNo) || logNo <= 0) {
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
        document.querySelectorAll('[data-log-row]').forEach((row) => {
            row.classList.toggle('table-active', Number(row.dataset.logRow) === Number(logNo));
        });
    },

    formatAdminBadge(adminName, adminNo) {
        return adminNo ? `${adminName} (#${adminNo})` : adminName;
    },

    formatAdminLabel(adminName, adminNo) {
        return adminNo ? `${adminName} (#${adminNo})` : adminName;
    },

    buildTargetPath(basePath) {
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

    syncQuickFilterState() {
        const currentActionType = CommonJS.normalizeOptionalText(document.getElementById('logActionType')?.value) || '';
        document.querySelectorAll('[data-log-quick-filter]').forEach((button) => {
            const active = (button.dataset.logQuickFilter || '') === currentActionType;
            button.classList.toggle('btn-secondary', active);
            button.classList.toggle('btn-outline-secondary', !active);
        });
    },

    syncSummaryCardState() {
        const currentActionType = CommonJS.normalizeOptionalText(document.getElementById('logActionType')?.value) || '';
        const today = this.resolveDateLabel(new Date());
        const startDate = document.getElementById('logStartDate')?.value || '';
        const endDate = document.getElementById('logEndDate')?.value || '';

        document.querySelectorAll('[data-log-summary-filter]').forEach((card) => {
            card.classList.toggle('stat-card-active', (card.dataset.logSummaryFilter || '') === currentActionType);
        });
        document.querySelectorAll('[data-log-summary-date-preset]').forEach((card) => {
            const active = card.dataset.logSummaryDatePreset === 'today' && startDate === today && endDate === today;
            card.classList.toggle('stat-card-active', active);
        });
    },

    syncDatePresetState() {
        const startDate = document.getElementById('logStartDate')?.value || '';
        const endDate = document.getElementById('logEndDate')?.value || '';
        const today = new Date();
        const todayLabel = this.resolveDateLabel(today);
        const sevenDaysAgo = new Date(today);
        sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 6);
        const thirtyDaysAgo = new Date(today);
        thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 29);

        document.querySelectorAll('[data-log-date-preset]').forEach((button) => {
            const preset = button.dataset.logDatePreset;
            const active = (
                (preset === 'today' && startDate === todayLabel && endDate === todayLabel) ||
                (preset === '7days' && startDate === this.resolveDateLabel(sevenDaysAgo) && endDate === todayLabel) ||
                (preset === '30days' && startDate === this.resolveDateLabel(thirtyDaysAgo) && endDate === todayLabel) ||
                (preset === 'clear' && !startDate && !endDate)
            );
            button.classList.toggle('btn-dark', active);
            button.classList.toggle('btn-outline-dark', !active);
        });
    },

    resolveDateLabel(value) {
        const year = value.getFullYear();
        const month = String(value.getMonth() + 1).padStart(2, '0');
        const day = String(value.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }
};

document.addEventListener('DOMContentLoaded', () => AdminLogPage.init());
