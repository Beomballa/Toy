const NoticeHistoryPage = {
    initialized: false,
    modal: null,
    isOpeningDetail: false,
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
        document.getElementById('btnResetNoticeHistory')?.addEventListener('click', () => this.resetFilters());
        document.getElementById('noticeHistoryPageSize')?.addEventListener('change', () => {
            this.state.page = 0;
            this.state.size = Number(document.getElementById('noticeHistoryPageSize')?.value || 20);
            this.loadHistory();
        });
        ['noticeHistoryNoticeNo', 'noticeHistoryAdminNo', 'noticeHistoryStartDate', 'noticeHistoryEndDate'].forEach((id) => {
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
                document.getElementById('noticeHistoryActionType').value = button.dataset.actionType || 'NOTICE_';
                this.state.page = 0;
                this.syncQuickFilterState();
                this.loadHistory();
            });
        });
        document.querySelectorAll('[data-notice-history-date-preset]').forEach((button) => {
            button.addEventListener('click', () => this.applyDatePreset(button.dataset.noticeHistoryDatePreset));
        });
        document.getElementById('btnBackToNoticeSource')?.addEventListener('click', () => {
            window.location.href = this.state.returnTo;
        });
        document.getElementById('noticeHistoryBody')?.addEventListener('click', (event) => {
            const detailButton = event.target.closest('[data-role="open-notice-log-detail"]');
            if (detailButton) {
                this.openDetail(Number(detailButton.dataset.logNo));
            }
        });
        window.addEventListener('popstate', () => {
            this.readStateFromUrl();
            this.syncReturnLinks();
            this.loadHistory();
        });
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        document.getElementById('noticeHistoryNoticeNo').value = params.get('noticeNo') || '';
        document.getElementById('noticeHistoryActionType').value = params.get('actionType') || 'NOTICE_';
        document.getElementById('noticeHistoryAdminNo').value = params.get('adminNo') || '';
        document.getElementById('noticeHistoryStartDate').value = params.get('startDate') || '';
        document.getElementById('noticeHistoryEndDate').value = params.get('endDate') || '';
        this.state.page = Number(params.get('page') || 0);
        this.state.size = Number(params.get('size') || 20);
        this.state.returnTo = params.get('returnTo') || '/admin/settings/notices';
        this.state.source = params.get('source') || '';
        this.state.logNo = params.get('logNo') || '';
        document.getElementById('noticeHistoryPageSize').value = String(this.state.size);
        this.syncQuickFilterState();
        CommonJS.renderSourceContextNotice({ noticeId: 'noticeHistorySourceContextNotice', source: this.state.source });
        CommonJS.bindMainLogoNavigation(this.state.returnTo);
    },

    buildParams() {
        const params = new URLSearchParams();
        const noticeNo = document.getElementById('noticeHistoryNoticeNo').value.trim();
        const actionType = document.getElementById('noticeHistoryActionType').value || 'NOTICE_';
        const adminNo = document.getElementById('noticeHistoryAdminNo').value.trim();
        const startDate = document.getElementById('noticeHistoryStartDate').value;
        const endDate = document.getElementById('noticeHistoryEndDate').value;

        if (noticeNo) params.set('noticeNo', noticeNo);
        if (actionType && actionType !== 'NOTICE_') params.set('actionType', actionType);
        if (adminNo) params.set('adminNo', adminNo);
        if (startDate) params.set('startDate', startDate);
        if (endDate) params.set('endDate', endDate);
        if (this.state.logNo) params.set('logNo', this.state.logNo);
        if (this.state.returnTo && this.state.returnTo !== '/admin/settings/notices') params.set('returnTo', this.state.returnTo);
        if (this.state.source) params.set('source', this.state.source);
        params.set('page', String(this.state.page));
        params.set('size', String(this.state.size));
        return params;
    },

    async loadHistory() {
        const params = this.buildParams();
        history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
        this.setMetaText('데이터를 불러오는 중입니다...');

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
            tbody.innerHTML = '<tr><td colspan="7" class="text-center py-5 text-muted">조회된 운영 공지 이력이 없습니다.</td></tr>';
            this.setListStateMeta('empty', '조회된 운영 공지 이력이 없습니다.', 0, 0, 0, '', '');
            return;
        }

        tbody.innerHTML = items.map((item) => `
            <tr data-notice-log-row="${item.logNo}">
                <td class="ps-4 text-muted small">${item.logNo}</td>
                <td>${item.noticePath ? `<a class="text-decoration-none fw-bold" href="${item.noticePath}">${item.noticeLabel}</a>` : (item.noticeLabel || '-')}</td>
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
            pageMetaId: 'noticeHistoryPageMeta',
            resultLabel: data.pageInfoLabel || `${data.rangeStart}-${data.rangeEnd} / ${data.totalElements}건`,
            filterCount: data.resultMeta?.filterCount ?? 0,
            querySignature: '',
            pageInfoLabel: data.resultMeta?.pageInfoLabel || data.pageInfoLabel || '',
            filterPrefix: '적용 필터',
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
            button.addEventListener('click', () => this.goPage(Number(button.dataset.page)));
        });
    },

    async openDetail(logNo) {
        if (this.isOpeningDetail) {
            return;
        }
        document.getElementById('noticeHistoryDetailBody').textContent = '데이터를 불러오는 중입니다...';
        this.setDetailTargetLink('');
        this.modal.show();
        try {
            this.isOpeningDetail = true;
            const response = await fetch(`/api/admin/logs/get?no=${logNo}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '상세 로그를 불러오지 못했습니다.'));
            }
            const data = await response.json();
            document.getElementById('noticeHistoryDetailBody').innerHTML = `
                <div class="mb-2"><strong>로그 번호</strong> ${data.logNo}</div>
                <div class="mb-2"><strong>관리자</strong> ${data.adminName} (#${data.adminNo})</div>
                <div class="mb-2"><strong>작업 종류</strong> ${data.actionType}</div>
                <div class="mb-2"><strong>대상</strong> ${data.targetPath ? `<a class="text-decoration-none" href="${data.targetPath}">${data.targetLabel}</a>` : (data.targetLabel || '-')}</div>
                <div class="mb-2"><strong>IP 주소</strong> ${data.ipAddress}</div>
                <div><strong>작업 일시</strong> ${data.actionDtm}</div>
            `;
            this.setDetailTargetLink(data.targetPath || '');
            this.state.logNo = String(logNo);
            this.highlightLogRow(logNo);
            history.replaceState(null, '', `${window.location.pathname}?${this.buildParams().toString()}`);
        } catch (error) {
            document.getElementById('noticeHistoryDetailBody').innerHTML = `<div class="text-danger">${error.message}</div>`;
            this.setDetailTargetLink('');
        } finally {
            this.isOpeningDetail = false;
        }
    },

    renderError(message) {
        document.getElementById('noticeHistoryBody').innerHTML =
            `<tr><td colspan="7" class="text-center py-5 text-danger">${message}</td></tr>`;
        this.setMetaText('이력 조회 실패');
        document.getElementById('noticeHistoryFilterMeta').textContent = '적용 필터 확인 불가';
        document.getElementById('noticeHistoryPageMeta').textContent = '페이지 메타 확인 불가';
        document.getElementById('noticeHistoryResultSummary').textContent = '운영 공지 이력 조회에 실패했습니다.';
        document.getElementById('noticeHistoryPagination').innerHTML = '';
        this.setListStateMeta('error', message, 0, 0, 0, '', '');
    },

    syncQuickFilterState() {
        const currentActionType = document.getElementById('noticeHistoryActionType')?.value || 'NOTICE_';
        document.querySelectorAll('.notice-history-quick-filter[data-action-type]').forEach((button) => {
            const active = (button.dataset.actionType || 'NOTICE_') === currentActionType;
            button.classList.toggle('active', active);
            button.classList.toggle('btn-dark', active);
            button.classList.toggle('btn-outline-dark', !active);
        });
    },

    syncReturnLinks() {
        const breadcrumbLink = document.getElementById('noticeHistoryBreadcrumbLink');
        if (breadcrumbLink) {
            breadcrumbLink.href = this.state.returnTo;
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
        this.state.page = page;
        this.loadHistory();
    },

    resetFilters() {
        document.getElementById('noticeHistoryNoticeNo').value = '';
        document.getElementById('noticeHistoryActionType').value = 'NOTICE_';
        document.getElementById('noticeHistoryAdminNo').value = '';
        document.getElementById('noticeHistoryStartDate').value = '';
        document.getElementById('noticeHistoryEndDate').value = '';
        document.getElementById('noticeHistoryPageSize').value = '20';
        this.state.page = 0;
        this.state.size = 20;
        this.state.logNo = '';
        this.syncQuickFilterState();
        this.loadHistory();
    },

    applyDatePreset(preset) {
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
        this.loadHistory();
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
        document.querySelectorAll('[data-notice-log-row]').forEach((row) => {
            const selected = Number(row.dataset.noticeLogRow) === Number(logNo);
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
    }
};

document.addEventListener('DOMContentLoaded', () => NoticeHistoryPage.init());
