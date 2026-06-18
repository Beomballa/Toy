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
        source: ''
    },
    selectedNoticeNos: new Set(),
    saveInFlight: false,
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
        CommonJS.bindMainLogoNavigation('/admin/settings/notices');
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
                this.toggleSelection(Number(checkbox.dataset.noticeNo), checkbox.checked);
                return;
            }
            const editButton = event.target.closest('[data-role="edit-notice"]');
            if (editButton) {
                this.openEditModal(JSON.parse(editButton.dataset.notice));
                return;
            }

            const toggleButton = event.target.closest('[data-role="toggle-notice"]');
            if (toggleButton) {
                this.toggleActive(Number(toggleButton.dataset.noticeNo), toggleButton.dataset.nextActive);
                return;
            }

            const deleteButton = event.target.closest('[data-role="delete-notice"]');
            if (deleteButton) {
                this.deleteNotice(Number(deleteButton.dataset.noticeNo));
            }
        });
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        this.state.page = Number(params.get('page') || 0);
        this.state.size = Number(params.get('size') || 10);
        this.state.keyword = params.get('keyword') || '';
        this.state.isActive = params.get('isActive') || '';
        this.state.isPinned = params.get('isPinned') || '';
        this.state.visibilityStatus = params.get('visibilityStatus') || '';
        this.state.noticeNo = params.get('noticeNo') || '';
        this.state.source = params.get('source') || '';
        document.getElementById('noticeKeyword').value = this.state.keyword;
        document.getElementById('noticeIsActiveFilter').value = this.state.isActive;
        document.getElementById('noticeIsPinnedFilter').value = this.state.isPinned;
        document.getElementById('noticeVisibilityStatusFilter').value = this.state.visibilityStatus;
        document.getElementById('noticePageSize').value = String(this.state.size);
        CommonJS.renderSourceContextNotice({ noticeId: 'noticeSourceContextNotice', source: this.state.source });
    },

    updateStateFromInputs() {
        this.state.keyword = document.getElementById('noticeKeyword').value.trim();
        this.state.isActive = document.getElementById('noticeIsActiveFilter').value;
        this.state.isPinned = document.getElementById('noticeIsPinnedFilter').value;
        this.state.visibilityStatus = document.getElementById('noticeVisibilityStatusFilter').value;
        this.state.size = Number(document.getElementById('noticePageSize').value || 10);
    },

    buildParams() {
        const params = new URLSearchParams();
        params.set('page', String(this.state.page));
        params.set('size', String(this.state.size));
        if (this.state.keyword) params.set('keyword', this.state.keyword);
        if (this.state.isActive) params.set('isActive', this.state.isActive);
        if (this.state.isPinned) params.set('isPinned', this.state.isPinned);
        if (this.state.visibilityStatus) params.set('visibilityStatus', this.state.visibilityStatus);
        if (this.state.noticeNo) params.set('noticeNo', this.state.noticeNo);
        if (this.state.source) params.set('source', this.state.source);
        return params;
    },

    async getList() {
        try {
            this.updateStateFromInputs();
            const params = this.buildParams();
            history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
            this.syncHistoryLink();
            this.setFilterMeta('적용 필터를 계산하는 중입니다...');
            this.setResultMeta('결과 메타를 계산하는 중입니다...');
            this.setPageMeta('페이지 메타를 계산하는 중입니다...');
            this.setListStateMeta('loading', '운영 공지를 불러오는 중입니다.', 0, 0, '');

            const res = await fetch(`/api/admin/settings/notices/list?${params.toString()}`);
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '운영 공지 목록을 불러오지 못했습니다.'));

            const data = await res.json();
            this.renderList(data.items || []);
            this.renderStats(data.noticeStats);
            this.renderMeta(data);
            this.renderPagination(data);
            await this.openDeepLinkedNoticeIfNeeded(data.items || []);
        } catch (err) {
            document.getElementById('noticeMetaText').textContent = err.message;
            this.setFilterMeta(err.message);
            this.setResultMeta('결과 메타 확인 불가');
            this.setPageMeta('페이지 메타 확인 불가');
            this.renderStats(null);
            document.getElementById('noticeListBody').innerHTML = `<tr><td colspan="7" class="text-center py-5 text-danger">${err.message}</td></tr>`;
            document.getElementById('noticePagination').innerHTML = '';
            this.setListStateMeta('error', err.message, 0, 0, '');
        }
    },

    renderList(items) {
        const tbody = document.getElementById('noticeListBody');
        if (!tbody) return;

        if (!items || items.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center py-5 text-muted">등록된 운영 공지가 없습니다.</td></tr>';
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
                    <a class="btn btn-sm btn-outline-secondary me-1" href="${item.activityLogPath || '#'}" ${item.activityLogPath ? '' : 'tabindex="-1" aria-disabled="true"'}>${item.activityLogLabel}</a>
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
        const button = document.getElementById('btnExportNoticeCsv');
        if (button) {
            button.disabled = true;
        }
        try {
            const response = await fetch(`/api/admin/settings/notices/export?${this.buildParams().toString()}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 공지 CSV 내보내기에 실패했습니다.'));
            }
            const blob = await response.blob();
            const fileName = this.extractFileName(response.headers.get('Content-Disposition'), 'notices.csv');
            this.downloadBlob(blob, fileName);
        } catch (error) {
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            if (button) {
                button.disabled = false;
            }
        }
    },

    extractFileName(contentDisposition, fallback) {
        const matched = contentDisposition?.match(/filename=\"?([^\";]+)\"?/i);
        return matched?.[1] || fallback;
    },

    downloadBlob(blob, fileName) {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(url);
    },

    syncHistoryLink() {
        const historyLink = document.getElementById('btnNoticeHistory');
        if (!historyLink) {
            return;
        }
        historyLink.href = this.buildNoticeHistoryPath('/admin/settings/notices/history');
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

    buildNoticeHistoryPath(basePath) {
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
            button.addEventListener('click', () => this.goPage(Number(button.dataset.page)));
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

    async deleteNotice(noticeNo) {
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

    toDateTimeLocalValue(value) {
        if (!value || value === '-') {
            return '';
        }
        return value.substring(0, 16);
    },

    applyStatFilter(type) {
        this.state.page = 0;
        document.getElementById('noticeIsActiveFilter').value = '';
        document.getElementById('noticeIsPinnedFilter').value = '';
        document.getElementById('noticeVisibilityStatusFilter').value = '';
        switch (type) {
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
            if (checked) {
                this.selectedNoticeNos.add(item.noticeNo);
            } else {
                this.selectedNoticeNos.delete(item.noticeNo);
            }
        });
        document.querySelectorAll('[data-role="select-notice"]').forEach((checkbox) => {
            checkbox.checked = checked;
        });
        this.updateSelectionMeta(this.getCurrentPageItems());
    },

    getCurrentPageItems() {
        return Array.from(document.querySelectorAll('[data-role="edit-notice"]')).map((button) => JSON.parse(button.dataset.notice));
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
            isActive: document.getElementById('bulkNoticeIsActive').value || null,
            isPinned: document.getElementById('bulkNoticeIsPinned').value || null
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
        return `/admin/settings/notices/history?noticeNo=${noticeNo}&returnTo=${encodeURIComponent(window.location.pathname + window.location.search)}`;
    },

    buildNoticeLogPath(noticeNo) {
        return `/admin/logs?actionType=NOTICE_&targetId=${noticeNo}`;
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
