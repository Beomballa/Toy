const NoticeList = {
    initialized: false,
    modal: null,
    operationPolicy: null,
    state: {
        page: 0,
        size: 10,
        keyword: '',
        isActive: '',
        isPinned: '',
        visibilityStatus: '',
        noticeNo: '',
        source: '',
        returnTo: ''
    },
    selectedNoticeNos: new Set(),
    saveInFlight: false,
    exportInFlight: false,
    bulkInFlight: false,
    toggleInFlight: new Set(),
    deleteInFlight: new Set(),

    init() {
        if (this.initialized) return;
        this.initialized = true;
        const modalEl = document.getElementById('noticeModal');
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
            const reason = CommonJS.getAdminWriteBlockedReason('운영 공지 등록 및 수정');
            CommonJS.setButtonDisabled(document.getElementById('btnNewNotice'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnSaveNotice'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnApplyNoticeBulk'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnBulkDeleteNotice'), disabled, reason);
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
    },

    bindEvents() {
        document.getElementById('btnNewNotice')?.addEventListener('click', () => this.openModal());
        document.getElementById('btnSaveNotice')?.addEventListener('click', () => this.saveNotice());
        document.getElementById('btnSearchNotice')?.addEventListener('click', () => this.getList());
        document.getElementById('btnResetNotice')?.addEventListener('click', () => this.resetFilters());
        document.getElementById('btnExportNoticeCsv')?.addEventListener('click', () => this.exportCsv());
        document.getElementById('btnApplyNoticeBulk')?.addEventListener('click', () => this.applyBulkOperation());
        document.getElementById('btnBulkDeleteNotice')?.addEventListener('click', () => this.applyBulkDelete());
        document.getElementById('btnClearNoticeSelection')?.addEventListener('click', () => this.clearSelection());
        document.getElementById('noticeSelectPage')?.addEventListener('change', (event) => this.toggleSelectCurrentPage(event.target.checked));
        document.getElementById('noticeStatTotalCard')?.addEventListener('click', () => this.applyStatFilter('total'));
        document.getElementById('noticeStatLiveCard')?.addEventListener('click', () => this.applyStatFilter('live'));
        document.getElementById('noticeStatScheduledCard')?.addEventListener('click', () => this.applyStatFilter('scheduled'));
        document.getElementById('noticeStatEndedCard')?.addEventListener('click', () => this.applyStatFilter('ended'));
        document.getElementById('noticeStatInactiveCard')?.addEventListener('click', () => this.applyStatFilter('inactive'));
        document.getElementById('noticeStatPinnedCard')?.addEventListener('click', () => this.applyStatFilter('pinned'));
        document.getElementById('noticeListActionNoticeClose')?.addEventListener('click', () => this.hideLastActionNotice(true));
        document.getElementById('noticePageSize')?.addEventListener('change', () => {
            this.state.page = 0;
            this.updateStateFromInputs();
            this.getList();
        });
        document.getElementById('noticeKeyword')?.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                this.state.page = 0;
                this.getList();
            }
        });
        document.getElementById('noticeListBody')?.addEventListener('click', (event) => {
            const checkbox = event.target.closest('[data-role="select-notice"]');
            if (checkbox) {
                const noticeNo = this.normalizeOptionalPositiveNumber(checkbox.dataset.noticeNo);
                if (noticeNo == null) {
                    return;
                }
                this.toggleSelection(noticeNo, checkbox.checked);
                return;
            }
            const editButton = event.target.closest('[data-role="edit-notice"]');
            if (editButton) {
                const notice = this.parseNoticeDataset(editButton.dataset.notice);
                if (!notice) {
                    void CommonJS.alert('수정할 운영 공지 정보를 읽을 수 없습니다.', '알림', 'warning');
                    return;
                }
                this.openEditModal(notice);
                return;
            }

            const toggleButton = event.target.closest('[data-role="toggle-notice"]');
            if (toggleButton) {
                const noticeNo = this.normalizeOptionalPositiveNumber(toggleButton.dataset.noticeNo);
                if (noticeNo == null) {
                    void CommonJS.alert('유효한 운영 공지 번호를 확인할 수 없습니다.', '알림', 'warning');
                    return;
                }
                this.toggleActive(noticeNo, toggleButton.dataset.nextActive);
                return;
            }

            const pinButton = event.target.closest('[data-role="toggle-notice-pinned"]');
            if (pinButton) {
                const noticeNo = this.normalizeOptionalPositiveNumber(pinButton.dataset.noticeNo);
                if (noticeNo == null) {
                    void CommonJS.alert('유효한 운영 공지 번호를 확인할 수 없습니다.', '알림', 'warning');
                    return;
                }
                this.togglePinned(noticeNo, pinButton.dataset.nextPinned);
                return;
            }

            const deleteButton = event.target.closest('[data-role="delete-notice"]');
            if (deleteButton) {
                const noticeNo = this.normalizeOptionalPositiveNumber(deleteButton.dataset.noticeNo);
                if (noticeNo == null) {
                    void CommonJS.alert('유효한 운영 공지 번호를 확인할 수 없습니다.', '알림', 'warning');
                    return;
                }
                this.deleteNotice(noticeNo);
            }
        });
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        this.state.page = this.normalizePage(params.get('page'));
        this.state.size = this.normalizePageSize(params.get('size'));
        this.state.keyword = CommonJS.normalizeOptionalText(params.get('keyword')) || '';
        this.state.isActive = this.normalizeYnFilterValue(params.get('isActive'));
        this.state.isPinned = this.normalizeYnFilterValue(params.get('isPinned'));
        this.state.visibilityStatus = this.normalizeVisibilityStatusValue(params.get('visibilityStatus'));
        this.state.noticeNo = this.normalizeOptionalPositiveNumber(params.get('noticeNo'))?.toString() || '';
        this.state.source = params.get('source') || '';
        this.state.returnTo = params.get('returnTo') || '';
        document.getElementById('noticeKeyword').value = this.state.keyword;
        document.getElementById('noticeIsActiveFilter').value = this.state.isActive;
        document.getElementById('noticeIsPinnedFilter').value = this.state.isPinned;
        document.getElementById('noticeVisibilityStatusFilter').value = this.state.visibilityStatus;
        document.getElementById('noticePageSize').value = String(this.state.size);
        CommonJS.bindMainLogoNavigation(this.state.returnTo || '/admin/settings/notices');
        CommonJS.renderSourceContextNotice({ noticeId: 'noticeSourceContextNotice', source: this.state.source });
    },

    updateStateFromInputs() {
        this.state.keyword = CommonJS.normalizeOptionalText(document.getElementById('noticeKeyword').value) || '';
        this.state.isActive = this.normalizeYnFilterValue(document.getElementById('noticeIsActiveFilter').value);
        this.state.isPinned = this.normalizeYnFilterValue(document.getElementById('noticeIsPinnedFilter').value);
        this.state.visibilityStatus = this.normalizeVisibilityStatusValue(document.getElementById('noticeVisibilityStatusFilter').value);
        this.state.size = this.normalizePageSize(document.getElementById('noticePageSize').value);
    },

    buildParams() {
        const params = new URLSearchParams();
        params.set('page', String(this.state.page));
        params.set('size', String(this.state.size));
        if (this.state.keyword) params.set('keyword', this.state.keyword);
        if (this.state.isActive && this.isValidYn(this.state.isActive)) params.set('isActive', this.state.isActive);
        if (this.state.isPinned && this.isValidYn(this.state.isPinned)) params.set('isPinned', this.state.isPinned);
        if (this.state.visibilityStatus && this.isValidVisibilityStatus(this.state.visibilityStatus)) params.set('visibilityStatus', this.state.visibilityStatus);
        if (this.state.noticeNo) params.set('noticeNo', this.state.noticeNo);
        if (this.state.source) params.set('source', this.state.source);
        if (this.state.returnTo) params.set('returnTo', this.state.returnTo);
        return params;
    },

    async getList() {
        try {
            this.updateStateFromInputs();
            this.validateState();
            const params = this.buildParams();
            history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
            this.syncHistoryLink();
            this.setFilterMeta('적용 필터를 계산하는 중입니다...');
            this.setResultMeta('결과 메타를 계산하는 중입니다...');
            this.setPageMeta('페이지 메타를 계산하는 중입니다...');
            this.setListStateMeta('loading', '운영 공지를 불러오는 중입니다.', 0, 0, '');
            const tbody = document.getElementById('noticeListBody');
            if (tbody) {
                this.renderTableState('loading', '운영 공지를 불러오는 중입니다.', '현재 필터 기준 목록과 상태 요약을 함께 계산하고 있습니다.');
            }

            const res = await fetch(`/api/admin/settings/notices/list?${params.toString()}`);
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '운영 공지 목록을 불러오지 못했습니다.'));

            const data = await res.json();
            this.renderList(data.items || []);
            this.renderStats(data.noticeStats);
            this.renderMeta(data);
            this.syncStatCardState();
            this.renderPagination(data);
            await this.openDeepLinkedNoticeIfNeeded(data.items || []);
        } catch (err) {
            document.getElementById('noticeMetaText').textContent = err.message;
            this.setFilterMeta(err.message);
            this.setResultMeta('결과 메타 확인 불가');
            this.setPageMeta('페이지 메타 확인 불가');
            this.renderStats(null);
            this.renderTableState('error', '운영 공지 목록을 불러오지 못했습니다.', err.message);
            document.getElementById('noticePagination').innerHTML = '';
            this.setListStateMeta('error', err.message, 0, 0, '');
            this.syncStatCardState();
        }
    },

    renderList(items) {
        const tbody = document.getElementById('noticeListBody');
        if (!tbody) return;

        if (!items || items.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center py-5">
                        <div class="product-empty-state">
                            <i class="fas fa-bullhorn product-empty-state-icon"></i>
                            <strong>등록된 운영 공지가 없습니다.</strong>
                            <p>상태, 고정, 노출 상태 조건을 조정하거나 신규 공지를 등록해 운영 안내 흐름을 채워보세요.</p>
                        </div>
                    </td>
                </tr>
            `;
            this.setListStateMeta('empty', '등록된 운영 공지가 없습니다.', 0, 0, '');
            this.updateSelectionMeta([]);
            return;
        }

        tbody.innerHTML = items.map((item) => `
            <tr>
                <td class="ps-4">
                    <input type="checkbox" data-role="select-notice" data-notice-no="${item.noticeNo}" ${this.selectedNoticeNos.has(item.noticeNo) ? 'checked' : ''}>
                </td>
                <td class="text-muted small">${item.noticeNo}</td>
                <td>
                    <div class="d-flex align-items-center gap-2 mb-1">
                        ${item.isPinned === 'Y' ? '<span class="badge text-bg-danger">고정</span>' : ''}
                        <a class="fw-bold text-dark text-decoration-none" href="${this.buildNoticeDetailPath(item.noticeNo)}">${this.escapeHtml(item.title)}</a>
                    </div>
                    <div class="small text-muted text-truncate" style="max-width: 520px;">${this.escapeHtml(item.content)}</div>
                </td>
                <td>
                    <div class="small">${item.startDtm}</div>
                    <div class="small text-muted">~ ${item.endDtm}</div>
                </td>
                <td class="text-center">
                    <span class="badge rounded-pill ${item.isPinned === 'Y' ? 'badge-y' : 'badge-n'}">${item.isPinned === 'Y' ? '고정' : '일반'}</span>
                </td>
                <td class="text-center">
                    <span class="badge rounded-pill ${item.isActive === 'Y' ? 'badge-y' : 'badge-n'}">${item.displayStatus}</span>
                </td>
                <td class="text-end pe-4">
                    <button class="btn btn-sm btn-outline-primary me-1" data-role="edit-notice" data-notice='${JSON.stringify(item).replace(/'/g, '&#39;')}'>수정</button>
                    <a class="btn btn-sm btn-outline-secondary me-1" href="${this.buildNoticeHistoryPath(item.historyPath)}" ${item.historyPath ? '' : 'tabindex="-1" aria-disabled="true"'}>이력</a>
                    <a class="btn btn-sm btn-outline-secondary me-1" href="${this.buildNoticeLogPathFromBase(item.activityLogPath, item.noticeNo)}" ${item.activityLogPath ? '' : 'tabindex="-1" aria-disabled="true"'}>${item.activityLogLabel}</a>
                    <button class="btn btn-sm btn-outline-warning me-1" data-role="toggle-notice-pinned" data-notice-no="${item.noticeNo}" data-next-pinned="${item.isPinned === 'Y' ? 'N' : 'Y'}">${item.isPinned === 'Y' ? '고정해제' : '고정'}</button>
                    <button class="btn btn-sm btn-outline-dark me-1" data-role="toggle-notice" data-notice-no="${item.noticeNo}" data-next-active="${item.isActive === 'Y' ? 'N' : 'Y'}">${item.isActive === 'Y' ? '비활성' : '활성'}</button>
                    <button class="btn btn-sm btn-outline-danger" data-role="delete-notice" data-notice-no="${item.noticeNo}">삭제</button>
                </td>
            </tr>
        `).join('');

        this.setListStateMeta('ready', '', items.length, null, null);
        this.updateSelectionMeta(items);
    },

    async openDeepLinkedNoticeIfNeeded(items) {
        if (!this.state.noticeNo) {
            return;
        }

        const noticeNo = Number(this.state.noticeNo);
        const target = items.find((item) => item.noticeNo === noticeNo);
        if (target) {
            this.openEditModal(target);
        } else if (noticeNo > 0) {
            try {
                const res = await fetch(`/api/admin/settings/notices/${noticeNo}`);
                if (res.ok) {
                    this.openEditModal(await res.json());
                }
            } catch (error) {
                console.error('딥링크 공지 상세 로드 실패:', error);
            }
        }
        this.state.noticeNo = '';
        const params = this.buildParams();
        history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
    },

    renderMeta(data) {
        CommonJS.renderListMeta({
            metaTextId: 'noticeMetaText',
            filterMetaId: 'noticeFilterMeta',
            resultMetaId: 'noticeResultMeta',
            pageMetaId: 'noticePageMeta',
            resultLabel: data.resultMeta?.resultLabel || `${data.totalElements || 0}건 조회`,
            filterCount: data.resultMeta?.appliedFilterCount ?? 0,
            querySignature: data.resultMeta?.querySignature || '고정 우선 최신순',
            pageInfoLabel: data.resultMeta?.pageInfoLabel || '',
            filterPrefix: '필터',
            defaultResultText: '결과 메타 없음',
            defaultPageText: '페이지 메타 없음'
        });
        this.setListStateMeta(
            'ready',
            '',
            (data.items || []).length,
            data.totalElements || 0,
            data.resultMeta?.querySignature || ''
        );
        const metaEl = document.getElementById('noticeListStateMeta');
        if (metaEl) {
            metaEl.dataset.pageInfoLabel = data.resultMeta?.pageInfoLabel || '';
            metaEl.dataset.sourceContext = this.state.source || '';
        }
        CommonJS.renderSourceContextNotice({ noticeId: 'noticeSourceContextNotice', source: this.state.source });
        this.renderLastActionNotice();
    },

    async exportCsv() {
        if (this.exportInFlight) {
            return;
        }
        const button = document.getElementById('btnExportNoticeCsv');
        try {
            this.exportInFlight = true;
            CommonJS.setButtonDisabled(button, true, '내보내는 중입니다.');
            const params = this.buildParams();
            params.delete('page');
            params.delete('size');
            await CommonJS.downloadFile(`/api/admin/settings/notices/export?${params.toString()}`, 'notices.csv');
        } catch (error) {
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.exportInFlight = false;
            CommonJS.setButtonDisabled(button, false);
        }
    },

    syncHistoryLink() {
        const historyLink = document.getElementById('btnNoticeHistory');
        if (!historyLink) {
            return;
        }
        historyLink.href = this.buildNoticeHistoryPathFromBase('/admin/settings/notices/history');
    },

    buildNoticeDetailPath(noticeNo) {
        const params = new URLSearchParams();
        params.set('no', String(noticeNo));
        params.set('returnTo', this.getCurrentLocation());
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        return `/admin/settings/notices/get?${params.toString()}`;
    },

    buildNoticeHistoryPathFromBase(basePath) {
        if (!basePath) {
            return '#';
        }
        const [path, rawQuery = ''] = basePath.split('?');
        const params = new URLSearchParams(rawQuery);
        params.set('returnTo', this.getCurrentLocation());
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        return `${path}?${params.toString()}`;
    },

    getCurrentLocation() {
        return window.location.pathname + window.location.search;
    },

    renderStats(stats) {
        const totalCountEl = document.getElementById('noticeTotalCount');
        const liveCountEl = document.getElementById('noticeLiveCount');
        const scheduledCountEl = document.getElementById('noticeScheduledCount');
        const endedCountEl = document.getElementById('noticeEndedCount');
        const inactiveCountEl = document.getElementById('noticeInactiveCount');
        const pinnedCountEl = document.getElementById('noticePinnedCount');
        const contextTextEl = document.getElementById('noticeStatsContextText');
        const noticeEl = document.getElementById('noticeStatsNotice');

        if (!stats) {
            if (totalCountEl) totalCountEl.innerText = '0';
            if (liveCountEl) liveCountEl.innerText = '0';
            if (scheduledCountEl) scheduledCountEl.innerText = '0';
            if (endedCountEl) endedCountEl.innerText = '0';
            if (inactiveCountEl) inactiveCountEl.innerText = '0';
            if (pinnedCountEl) pinnedCountEl.innerText = '0';
            if (contextTextEl) contextTextEl.innerText = '카드 기준을 확인할 수 없습니다.';
            if (noticeEl) {
                noticeEl.innerText = '카드 기준을 확인할 수 없습니다.';
                noticeEl.dataset.statsContext = 'error';
            }
            const guideTextEl = document.getElementById('noticeSummaryGuideText');
            if (guideTextEl) {
                guideTextEl.textContent = '요약 카드를 눌러 빠른 필터를 적용할 수 있습니다.';
            }
            return;
        }

        totalCountEl.innerText = Number(stats.totalCount || 0).toLocaleString();
        liveCountEl.innerText = Number(stats.liveCount || 0).toLocaleString();
        scheduledCountEl.innerText = Number(stats.scheduledCount || 0).toLocaleString();
        endedCountEl.innerText = Number(stats.endedCount || 0).toLocaleString();
        inactiveCountEl.innerText = Number(stats.inactiveCount || 0).toLocaleString();
        pinnedCountEl.innerText = Number(stats.pinnedCount || 0).toLocaleString();

        contextTextEl.innerText = `${stats.contextLabel} · ${stats.querySignature}`;
        const usingQuickFilter = !!this.state.visibilityStatus || !!this.state.isPinned;
        noticeEl.innerText = usingQuickFilter
            ? '카드 수치는 기본 탐색 문맥 기준이며, 선택한 빠른 필터는 목록에만 적용됩니다.'
            : '카드 수치는 현재 탐색 문맥 기준입니다.';
        noticeEl.dataset.statsContext = usingQuickFilter ? 'base-query' : 'current-query';
        const guideTextEl = document.getElementById('noticeSummaryGuideText');
        if (guideTextEl) {
            guideTextEl.textContent = this.resolveSummaryGuideText();
        }
    },

    renderPagination(data) {
        const paginationEl = document.getElementById('noticePagination');
        if (!paginationEl) return;

        const totalPages = Number(data.totalPages || 0);
        const currentPage = Number(data.currentPage || 0);
        if (totalPages <= 1) {
            paginationEl.innerHTML = '';
            return;
        }

        paginationEl.innerHTML = Array.from({ length: totalPages }, (_, index) => `
            <li class="page-item ${index === currentPage ? 'active' : ''}">
                <button type="button" class="page-link" data-role="go-notice-page" data-page="${index}">${index + 1}</button>
            </li>
        `).join('');

        paginationEl.querySelectorAll('[data-role="go-notice-page"]').forEach((button) => {
            button.addEventListener('click', () => this.goPage(this.normalizePage(button.dataset.page)));
        });
    },

    updateSelectionMeta(items) {
        const selectionMeta = document.getElementById('noticeSelectionMeta');
        const selectedCount = this.selectedNoticeNos.size;
        const currentPageSelectedCount = (items || []).filter((item) => this.selectedNoticeNos.has(item.noticeNo)).length;
        const selectPage = document.getElementById('noticeSelectPage');
        if (selectPage && items && items.length > 0) {
            selectPage.checked = currentPageSelectedCount === items.length;
            selectPage.indeterminate = currentPageSelectedCount > 0 && currentPageSelectedCount < items.length;
        } else if (selectPage) {
            selectPage.checked = false;
            selectPage.indeterminate = false;
        }

        if (selectionMeta) {
            if (selectedCount === 0) {
                selectionMeta.textContent = '선택된 공지가 없습니다.';
            } else {
                selectionMeta.textContent = `선택 ${selectedCount}건 · 현재 페이지 ${currentPageSelectedCount}건`;
            }
        }
    },

    setFilterMeta(message) {
        document.getElementById('noticeFilterMeta').textContent = message;
    },

    setResultMeta(message) {
        document.getElementById('noticeResultMeta').textContent = message;
    },

    setPageMeta(message) {
        document.getElementById('noticePageMeta').textContent = message;
    },

    renderTableState(type, title, description) {
        const tbody = document.getElementById('noticeListBody');
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
                        <i class="fa-solid ${type === 'error' ? 'fa-triangle-exclamation' : 'fa-bullhorn'}"></i>
                    </div>
                    <strong>${this.escapeHtml(title)}</strong>
                    <p>${this.escapeHtml(description)}</p>
                </div>
            `;

        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="py-5">
                    ${content}
                </td>
            </tr>
        `;
    },

    setListStateMeta(state, message, visibleCount, totalElements, querySignature) {
        const metaEl = document.getElementById('noticeListStateMeta');
        if (!metaEl) return;
        metaEl.dataset.listState = state;
        metaEl.dataset.stateMessage = message || '';
        if (visibleCount != null) metaEl.dataset.visibleCount = String(visibleCount);
        if (totalElements != null) metaEl.dataset.totalElements = String(totalElements);
        if (querySignature != null) metaEl.dataset.querySignature = querySignature;
    },

    resetFilters() {
        document.getElementById('noticeKeyword').value = '';
        document.getElementById('noticeIsActiveFilter').value = '';
        document.getElementById('noticeIsPinnedFilter').value = '';
        document.getElementById('noticeVisibilityStatusFilter').value = '';
        document.getElementById('noticePageSize').value = '10';
        this.state.page = 0;
        this.state.noticeNo = '';
        this.getList();
    },

    clearSelection() {
        this.selectedNoticeNos.clear();
        document.querySelectorAll('[data-role="select-notice"]').forEach((checkbox) => {
            checkbox.checked = false;
        });
        this.updateSelectionMeta(this.getCurrentPageItems());
    },

    goPage(page) {
        if (!Number.isInteger(page) || page < 0) {
            void CommonJS.alert('이동할 페이지 정보가 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        this.state.page = page;
        this.getList();
    },

    async openModal() {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 공지 등록 및 수정'), '알림', 'warning');
            return;
        }
        document.getElementById('noticeForm').reset();
        document.getElementById('noticeNo').value = '';
        document.getElementById('noticeIsActive').value = 'Y';
        document.getElementById('noticeIsPinned').value = 'N';
        document.getElementById('noticeModalTitle').innerText = '운영 공지 등록';
        this.modal.show();
    },

    async openEditModal(item) {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 공지 등록 및 수정'), '알림', 'warning');
            return;
        }
        document.getElementById('noticeNo').value = item.noticeNo;
        document.getElementById('noticeTitle').value = item.title;
        document.getElementById('noticeContent').value = item.content;
        document.getElementById('noticeIsActive').value = item.isActive;
        document.getElementById('noticeIsPinned').value = item.isPinned;
        document.getElementById('noticeStartDtm').value = this.toDateTimeLocalValue(item.startDtm);
        document.getElementById('noticeEndDtm').value = this.toDateTimeLocalValue(item.endDtm);
        document.getElementById('noticeModalTitle').innerText = '운영 공지 수정';
        this.modal.show();
    },

    async saveNotice() {
        if (this.saveInFlight) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 공지 등록 및 수정'), '알림', 'warning');
            return;
        }

        const formData = {
            noticeNo: document.getElementById('noticeNo').value || null,
            title: document.getElementById('noticeTitle').value.trim(),
            content: document.getElementById('noticeContent').value.trim(),
            isActive: document.getElementById('noticeIsActive').value,
            isPinned: document.getElementById('noticeIsPinned').value,
            startDtm: this.toNullableDateTime(document.getElementById('noticeStartDtm').value),
            endDtm: this.toNullableDateTime(document.getElementById('noticeEndDtm').value)
        };

        if (!formData.title || !formData.content) {
            await CommonJS.alert('공지 제목과 내용을 입력하세요.', '알림', 'warning');
            return;
        }
        if (!this.validateNoticePeriod(formData.startDtm, formData.endDtm)) {
            await CommonJS.alert('시작 일시는 종료 일시보다 늦을 수 없습니다.', '알림', 'warning');
            return;
        }

        try {
            this.saveInFlight = true;
            CommonJS.setButtonDisabled(document.getElementById('btnSaveNotice'), true, '저장 중입니다.');
            const res = await fetch('/api/admin/settings/notices/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData)
            });
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '운영 공지를 저장하지 못했습니다.'));

            const savedNotice = await res.json();
            const savedNoticeNo = Number(savedNotice.noticeNo || formData.noticeNo || 0) || null;
            this.setLastActionMeta('save-notice', 'success', formData.noticeNo ? '목록 수정' : '목록 등록', savedNoticeNo);

            this.modal.hide();
            await this.getList();
            await CommonJS.alert('운영 공지가 저장되었습니다.', '성공', 'success');
        } catch (err) {
            this.setLastActionMeta('save-notice', 'error', formData.noticeNo ? '목록 수정' : '목록 등록', formData.noticeNo);
            await CommonJS.alert(err.message, '오류', 'error');
        } finally {
            this.saveInFlight = false;
            this.applyOperationPolicy(this.operationPolicy);
        }
    },

    async toggleActive(noticeNo, isActive) {
        if (!this.isPositiveNumber(noticeNo)) {
            await CommonJS.alert('유효한 운영 공지 번호를 확인할 수 없습니다.', '알림', 'warning');
            return;
        }
        if (!this.isValidYn(isActive)) {
            await CommonJS.alert('변경할 공지 상태 값이 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        if (this.toggleInFlight.has(noticeNo)) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 공지 상태 변경'), '알림', 'warning');
            return;
        }
        try {
            this.toggleInFlight.add(noticeNo);
            const res = await fetch(`/api/admin/settings/notices/active/${noticeNo}?isActive=${isActive}`, {
                method: 'PATCH'
            });
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '공지 상태를 변경하지 못했습니다.'));
            this.setLastActionMeta('toggle-active', 'success', '목록 상태 변경', noticeNo);
            await this.getList();
        } catch (err) {
            this.setLastActionMeta('toggle-active', 'error', '목록 상태 변경', noticeNo);
            await CommonJS.alert(err.message, '오류', 'error');
        } finally {
            this.toggleInFlight.delete(noticeNo);
        }
    },

    async togglePinned(noticeNo, isPinned) {
        if (!this.isPositiveNumber(noticeNo)) {
            await CommonJS.alert('유효한 운영 공지 번호를 확인할 수 없습니다.', '알림', 'warning');
            return;
        }
        if (!this.isValidYn(isPinned)) {
            await CommonJS.alert('변경할 고정 상태 값이 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        if (this.toggleInFlight.has(`pinned-${noticeNo}`)) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 공지 고정 상태 변경'), '알림', 'warning');
            return;
        }
        try {
            this.toggleInFlight.add(`pinned-${noticeNo}`);
            const res = await fetch(`/api/admin/settings/notices/pinned/${noticeNo}?isPinned=${isPinned}`, {
                method: 'PATCH'
            });
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '공지 고정 상태를 변경하지 못했습니다.'));
            this.setLastActionMeta('toggle-pinned', 'success', '목록 고정 상태 변경', noticeNo);
            await this.getList();
        } catch (err) {
            this.setLastActionMeta('toggle-pinned', 'error', '목록 고정 상태 변경', noticeNo);
            await CommonJS.alert(err.message, '오류', 'error');
        } finally {
            this.toggleInFlight.delete(`pinned-${noticeNo}`);
        }
    },

    async deleteNotice(noticeNo) {
        if (!this.isPositiveNumber(noticeNo)) {
            await CommonJS.alert('유효한 운영 공지 번호를 확인할 수 없습니다.', '알림', 'warning');
            return;
        }
        if (this.deleteInFlight.has(noticeNo)) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 공지 삭제'), '알림', 'warning');
            return;
        }
        const confirmed = await CommonJS.confirm('운영 공지를 삭제하시겠습니까?', '삭제 확인');
        if (!confirmed) return;

        try {
            this.deleteInFlight.add(noticeNo);
            const res = await fetch(`/api/admin/settings/notices/delete?no=${noticeNo}`, {
                method: 'DELETE'
            });
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '운영 공지를 삭제하지 못했습니다.'));
            this.setLastActionMeta('delete-notice', 'success', '목록 삭제', noticeNo);
            await this.getList();
            await CommonJS.alert('운영 공지가 삭제되었습니다.', '성공', 'success');
        } catch (err) {
            this.setLastActionMeta('delete-notice', 'error', '목록 삭제', noticeNo);
            await CommonJS.alert(err.message, '오류', 'error');
        } finally {
            this.deleteInFlight.delete(noticeNo);
        }
    },

    toNullableDateTime(value) {
        return value ? `${value}:00` : null;
    },

    validateNoticePeriod(startDtm, endDtm) {
        if (!startDtm || !endDtm) {
            return true;
        }
        return startDtm <= endDtm;
    },

    toDateTimeLocalValue(value) {
        if (!value || value === '-') {
            return '';
        }
        return value.substring(0, 16);
    },

    applyStatFilter(type) {
        const normalizedType = this.normalizeStatFilter(type);
        this.state.page = 0;
        const currentQuickFilter = this.resolveActiveStatFilter();
        if (currentQuickFilter === normalizedType || (normalizedType === 'total' && !currentQuickFilter)) {
            document.getElementById('noticeIsActiveFilter').value = '';
            document.getElementById('noticeIsPinnedFilter').value = '';
            document.getElementById('noticeVisibilityStatusFilter').value = '';
            this.getList();
            return;
        }
        document.getElementById('noticeIsActiveFilter').value = '';
        document.getElementById('noticeIsPinnedFilter').value = '';
        document.getElementById('noticeVisibilityStatusFilter').value = '';
        switch (normalizedType) {
            case 'total':
                break;
            case 'live':
                document.getElementById('noticeVisibilityStatusFilter').value = 'LIVE';
                break;
            case 'scheduled':
                document.getElementById('noticeVisibilityStatusFilter').value = 'SCHEDULED';
                break;
            case 'ended':
                document.getElementById('noticeVisibilityStatusFilter').value = 'ENDED';
                break;
            case 'inactive':
                document.getElementById('noticeVisibilityStatusFilter').value = 'INACTIVE';
                break;
            case 'pinned':
                document.getElementById('noticeIsPinnedFilter').value = 'Y';
                break;
            default:
                break;
        }
        this.getList();
    },

    resolveActiveStatFilter() {
        if (document.getElementById('noticeIsPinnedFilter')?.value === 'Y') {
            return 'pinned';
        }
        const visibilityStatus = document.getElementById('noticeVisibilityStatusFilter')?.value || '';
        if (visibilityStatus === 'LIVE') return 'live';
        if (visibilityStatus === 'SCHEDULED') return 'scheduled';
        if (visibilityStatus === 'ENDED') return 'ended';
        if (visibilityStatus === 'INACTIVE') return 'inactive';
        return '';
    },

    syncStatCardState() {
        const activeFilter = this.resolveActiveStatFilter();
        document.getElementById('noticeStatTotalCard')?.classList.toggle('stat-card-active', !activeFilter);
        document.getElementById('noticeStatLiveCard')?.classList.toggle('stat-card-active', activeFilter === 'live');
        document.getElementById('noticeStatScheduledCard')?.classList.toggle('stat-card-active', activeFilter === 'scheduled');
        document.getElementById('noticeStatEndedCard')?.classList.toggle('stat-card-active', activeFilter === 'ended');
        document.getElementById('noticeStatInactiveCard')?.classList.toggle('stat-card-active', activeFilter === 'inactive');
        document.getElementById('noticeStatPinnedCard')?.classList.toggle('stat-card-active', activeFilter === 'pinned');
    },

    resolveSummaryGuideText() {
        const activeFilter = this.resolveActiveStatFilter();
        if (!activeFilter) {
            return '요약 카드를 누르면 해당 문맥으로 바로 필터링합니다.';
        }

        const labels = {
            live: '노출중',
            scheduled: '예약 공지',
            ended: '종료 공지',
            inactive: '비활성 공지',
            pinned: '고정 공지'
        };
        return `현재 빠른 필터: ${labels[activeFilter] || '전체 공지'} · 같은 카드를 다시 누르면 해제됩니다.`;
    },

    toggleSelection(noticeNo, checked) {
        if (checked) {
            this.selectedNoticeNos.add(noticeNo);
        } else {
            this.selectedNoticeNos.delete(noticeNo);
        }
        this.updateSelectionMeta(this.getCurrentPageItems());
    },

    toggleSelectCurrentPage(checked) {
        this.getCurrentPageItems().forEach((item) => {
            const noticeNo = this.normalizeOptionalPositiveNumber(item.noticeNo);
            if (noticeNo == null) {
                return;
            }
            if (checked) {
                this.selectedNoticeNos.add(noticeNo);
            } else {
                this.selectedNoticeNos.delete(noticeNo);
            }
        });
        document.querySelectorAll('[data-role="select-notice"]').forEach((checkbox) => {
            const noticeNo = this.normalizeOptionalPositiveNumber(checkbox.dataset.noticeNo);
            if (noticeNo == null) {
                checkbox.checked = false;
                return;
            }
            checkbox.checked = checked;
        });
        this.updateSelectionMeta(this.getCurrentPageItems());
    },

    getCurrentPageItems() {
        return Array.from(document.querySelectorAll('[data-role="edit-notice"]'))
            .map((button) => this.parseNoticeDataset(button.dataset.notice))
            .filter(Boolean);
    },

    async applyBulkOperation() {
        if (this.bulkInFlight) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 공지 일괄 변경'), '알림', 'warning');
            return;
        }

        if (this.selectedNoticeNos.size === 0) {
            await CommonJS.alert('일괄 적용할 운영 공지를 선택하세요.', '알림', 'warning');
            return;
        }

        const payload = {
            noticeNos: Array.from(this.selectedNoticeNos),
            isActive: this.normalizeBulkYnActionValue(document.getElementById('bulkNoticeIsActive').value),
            isPinned: this.normalizeBulkYnActionValue(document.getElementById('bulkNoticeIsPinned').value)
        };

        if (!payload.isActive && !payload.isPinned) {
            await CommonJS.alert('일괄 변경할 상태를 선택하세요.', '알림', 'warning');
            return;
        }

        try {
            this.bulkInFlight = true;
            const res = await fetch('/api/admin/settings/notices/bulk-operate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '운영 공지 일괄 변경에 실패했습니다.'));

            const result = await res.json();
            this.setLastActionMeta('bulk-operate', 'success', '목록 일괄 변경');
            this.clearSelection();
            document.getElementById('bulkNoticeIsActive').value = '';
            document.getElementById('bulkNoticeIsPinned').value = '';
            await this.getList();
            await CommonJS.alert(`총 ${result.requestedCount}건 중 ${result.updatedCount}건 변경, ${result.unchangedCount}건 유지되었습니다.`, '성공', 'success');
        } catch (err) {
            this.setLastActionMeta('bulk-operate', 'error', '목록 일괄 변경');
            await CommonJS.alert(err.message, '오류', 'error');
        } finally {
            this.bulkInFlight = false;
        }
    },

    async applyBulkDelete() {
        if (this.bulkInFlight) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 공지 일괄 삭제'), '알림', 'warning');
            return;
        }
        if (this.selectedNoticeNos.size === 0) {
            await CommonJS.alert('삭제할 운영 공지를 선택하세요.', '알림', 'warning');
            return;
        }

        const confirmed = await CommonJS.confirm(`선택한 운영 공지 ${this.selectedNoticeNos.size}건을 삭제하시겠습니까?`, '일괄 삭제 확인');
        if (!confirmed) {
            return;
        }

        try {
            this.bulkInFlight = true;
            const res = await fetch('/api/admin/settings/notices/bulk-delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ noticeNos: Array.from(this.selectedNoticeNos) })
            });
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '운영 공지 일괄 삭제에 실패했습니다.'));

            const result = await res.json();
            this.setLastActionMeta('bulk-delete', 'success', '목록 일괄 삭제');
            this.clearSelection();
            await this.getList();
            await CommonJS.alert(`총 ${result.requestedCount}건 중 ${result.deletedCount}건 삭제, ${result.missingCount}건은 이미 없었습니다.`, '성공', 'success');
        } catch (err) {
            this.setLastActionMeta('bulk-delete', 'error', '목록 일괄 삭제');
            await CommonJS.alert(err.message, '오류', 'error');
        } finally {
            this.bulkInFlight = false;
        }
    },

    setLastActionMeta(action, status, sourceLabel, noticeNo = null) {
        const metaEl = document.getElementById('noticeListStateMeta');
        if (!metaEl) return;
        metaEl.dataset.lastAction = action || '';
        metaEl.dataset.lastActionSource = sourceLabel || '운영 공지 목록';
        metaEl.dataset.lastActionStatus = status || '';
        metaEl.dataset.lastActionNoticeNo = noticeNo == null || noticeNo === '' ? '' : String(noticeNo);
        metaEl.dataset.lastActionHistoryPath = noticeNo == null || noticeNo === '' ? '' : this.buildNoticeHistoryPath(noticeNo);
        metaEl.dataset.lastActionLogPath = noticeNo == null || noticeNo === '' ? '' : this.buildNoticeLogPath(noticeNo);
        this.renderLastActionNotice();
    },

    renderLastActionNotice() {
        const metaEl = document.getElementById('noticeListStateMeta');
        const noticeEl = document.getElementById('noticeListActionNotice');
        const noticeTextEl = document.getElementById('noticeListActionNoticeText');
        const noticeActionsEl = document.getElementById('noticeListActionNoticeActions');
        if (!metaEl || !noticeEl || !noticeTextEl || !noticeActionsEl) return;

        const action = metaEl.dataset.lastAction || '';
        const source = metaEl.dataset.lastActionSource || '';
        const status = metaEl.dataset.lastActionStatus || '';
        const noticeNo = metaEl.dataset.lastActionNoticeNo || '';
        const historyPath = metaEl.dataset.lastActionHistoryPath || '';
        const logPath = metaEl.dataset.lastActionLogPath || '';
        const noticeLabel = noticeNo ? `운영 공지 #${noticeNo}` : '운영 공지';

        if (!action || !status) {
            this.hideLastActionNotice(false);
            return;
        }

        const templates = {
            'save-notice:success': `${noticeLabel} 저장을 반영했습니다.`,
            'save-notice:error': '운영 공지 저장에 실패했습니다.',
            'toggle-active:success': `${noticeLabel} 상태를 변경했습니다.`,
            'toggle-active:error': `${noticeLabel} 상태 변경에 실패했습니다.`,
            'delete-notice:success': `${noticeLabel} 삭제를 반영했습니다.`,
            'delete-notice:error': `${noticeLabel} 삭제에 실패했습니다.`,
            'bulk-operate:success': '선택한 운영 공지 일괄 변경을 반영했습니다.',
            'bulk-operate:error': '운영 공지 일괄 변경에 실패했습니다.',
            'bulk-delete:success': '선택한 운영 공지 일괄 삭제를 반영했습니다.',
            'bulk-delete:error': '운영 공지 일괄 삭제에 실패했습니다.'
        };
        const variants = {
            'save-notice:success': 'alert-success',
            'toggle-active:success': 'alert-primary',
            'delete-notice:success': 'alert-warning',
            'bulk-operate:success': 'alert-primary',
            'bulk-delete:success': 'alert-warning',
            'save-notice:error': 'alert-danger',
            'toggle-active:error': 'alert-danger',
            'delete-notice:error': 'alert-danger',
            'bulk-operate:error': 'alert-danger',
            'bulk-delete:error': 'alert-danger'
        };

        const sourceMessage = source ? `${source}에서 실행` : '운영 공지 목록에서 실행';
        const message = templates[`${action}:${status}`] || '조치 결과를 확인해 주세요.';
        const variantClass = variants[`${action}:${status}`] || (status === 'success' ? 'alert-success' : 'alert-danger');
        CommonJS.renderActionNotice({
            noticeId: 'noticeListActionNotice',
            textId: 'noticeListActionNoticeText',
            actionsId: 'noticeListActionNoticeActions',
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
            noticeId: 'noticeListActionNotice',
            textId: 'noticeListActionNoticeText',
            actionsId: 'noticeListActionNoticeActions',
            metaId: 'noticeListStateMeta',
            clearMeta,
            metaKeys: [
                'lastAction',
                'lastActionSource',
                'lastActionStatus',
                'lastActionNoticeNo',
                'lastActionHistoryPath',
                'lastActionLogPath'
            ]
        });
    },

    buildNoticeHistoryPath(noticeNo) {
        const params = new URLSearchParams();
        params.set('noticeNo', String(noticeNo));
        params.set('returnTo', window.location.pathname + window.location.search);
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        return `/admin/settings/notices/history?${params.toString()}`;
    },

    buildNoticeLogPath(noticeNo) {
        const params = new URLSearchParams();
        params.set('actionType', 'NOTICE_');
        params.set('targetId', String(noticeNo));
        params.set('returnTo', window.location.pathname + window.location.search);
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        return `/admin/logs?${params.toString()}`;
    },

    buildNoticeLogPathFromBase(basePath, noticeNo = null) {
        if (!basePath && noticeNo == null) {
            return '#';
        }
        if (!basePath) {
            return this.buildNoticeLogPath(noticeNo);
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
        if (this.state.keyword.length > 100) {
            throw new Error('검색어는 100자 이하로 입력하세요.');
        }
        if (this.state.isActive && !this.normalizeYnFilterValue(this.state.isActive)) {
            throw new Error('공지 활성 상태 필터 값이 올바르지 않습니다.');
        }
        if (this.state.isPinned && !this.normalizeYnFilterValue(this.state.isPinned)) {
            throw new Error('공지 고정 상태 필터 값이 올바르지 않습니다.');
        }
        if (this.state.visibilityStatus && !this.normalizeVisibilityStatusValue(this.state.visibilityStatus)) {
            throw new Error('공지 노출 상태 필터 값이 올바르지 않습니다.');
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

    parseNoticeDataset(value) {
        try {
            return value ? JSON.parse(value) : null;
        } catch (error) {
            console.error('운영 공지 dataset 파싱 실패:', error);
            return null;
        }
    },

    normalizeStatFilter(value) {
        return ['total', 'live', 'scheduled', 'ended', 'inactive', 'pinned'].includes(value) ? value : 'total';
    },

    normalizeYnFilterValue(value) {
        return this.isValidYn(value) ? value : '';
    },

    normalizeVisibilityStatusValue(value) {
        return this.isValidVisibilityStatus(value) ? value : '';
    },

    normalizeBulkYnActionValue(value) {
        return this.isValidYn(value) ? value : null;
    },

    isPositiveNumber(value) {
        return Number.isInteger(value) && value > 0;
    },

    isValidYn(value) {
        return value === 'Y' || value === 'N';
    },

    isValidVisibilityStatus(value) {
        return ['LIVE', 'SCHEDULED', 'ENDED', 'INACTIVE'].includes(value);
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

document.addEventListener('DOMContentLoaded', () => NoticeList.init());
