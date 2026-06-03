const ContentList = {
    initialized: false,
    state: {
        page: 0,
        size: 9,
        boardType: ContentBoardConfig.normalizeBoardType(
            window.initialContentBoardType || new URLSearchParams(window.location.search).get('boardType')
        ),
        keyword: new URLSearchParams(window.location.search).get('keyword') || '',
        status: new URLSearchParams(window.location.search).get('status') || '',
        publicYn: new URLSearchParams(window.location.search).get('publicYn') || '',
        startDate: new URLSearchParams(window.location.search).get('startDate') || '',
        endDate: new URLSearchParams(window.location.search).get('endDate') || '',
        pinnedOnly: new URLSearchParams(window.location.search).get('pinnedOnly') === 'true',
        selectedIds: new Set(),
        currentPageIds: [],
        lastBulkResultMessage: '아직 일괄 적용 결과가 없습니다.'
    },

    init() {
        if (this.initialized) return;
        this.initialized = true;
        this.syncSearchField();
        this.setInitialTab();
        this.updateSidebarActive();
        this.updatePageMeta();
        this.bindEvents();
        this.applyOperationPolicy();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));
        this.getList();
    },

    setInitialTab() {
        document.querySelectorAll('.content-board-tab[data-board-type]').forEach(el => {
            if (el.dataset.boardType === this.state.boardType) {
                el.classList.add('active');
            } else {
                el.classList.remove('active');
            }
        });
    },

    updateSidebarActive() {
        document.querySelectorAll('.nav-link[data-community-nav]').forEach(el => {
            const boardType = el.dataset.communityNav;
            if (boardType === this.state.boardType) {
                el.classList.add('active');
            } else {
                el.classList.remove('active');
            }
        });
    },

    bindEvents() {
        // 보드 타입 탭 클릭
        document.querySelectorAll('.content-board-tab[data-board-type]').forEach(el => {
            el.addEventListener('click', (e) => {
                e.preventDefault();
                document.querySelectorAll('.content-board-tab[data-board-type]').forEach(link => link.classList.remove('active'));
                el.classList.add('active');
                this.state.boardType = el.dataset.boardType;
                this.state.page = 0;

                this.pushState();
                this.updateSidebarActive();
                this.updatePageMeta();
                this.getList();
            });
        });

        window.addEventListener('popstate', () => {
            const params = new URLSearchParams(window.location.search);
            // 히스토리 이동 시 URL이 현재 게시판 상태의 기준이 된다.
            this.state.boardType = ContentBoardConfig.normalizeBoardType(params.get('boardType'));
            this.state.keyword = params.get('keyword') || '';
            this.state.status = params.get('status') || '';
            this.state.publicYn = params.get('publicYn') || '';
            this.state.startDate = params.get('startDate') || '';
            this.state.endDate = params.get('endDate') || '';
            this.state.pinnedOnly = params.get('pinnedOnly') === 'true';
            this.state.page = 0;
            this.syncSearchField();
            this.setInitialTab();
            this.updateSidebarActive();
            this.updatePageMeta();
            this.getList();
        });

        // 새 글 작성 버튼
        document.getElementById('btnNewContent')?.addEventListener('click', () => {
            location.href = `/admin/content/edit?boardType=${this.state.boardType}`;
        });
        document.getElementById('btnExportContentCsv')?.addEventListener('click', () => this.exportCsv());
        document.getElementById('btnBulkDeleteContent')?.addEventListener('click', () => this.applyBulkDelete());

        document.getElementById('contentSearchForm')?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.state.keyword = document.getElementById('contentSearchKeyword')?.value.trim() || '';
            this.state.status = document.getElementById('contentStatusFilter')?.value || '';
            this.state.publicYn = document.getElementById('contentPublicFilter')?.value || '';
            this.state.startDate = document.getElementById('contentStartDate')?.value || '';
            this.state.endDate = document.getElementById('contentEndDate')?.value || '';
            this.state.pinnedOnly = document.getElementById('contentPinnedOnly')?.checked || false;
            this.state.page = 0;
            this.pushState();
            this.getList();
        });

        document.getElementById('btnResetContentSearch')?.addEventListener('click', () => {
            this.state.keyword = '';
            this.state.status = '';
            this.state.publicYn = '';
            this.state.startDate = '';
            this.state.endDate = '';
            this.state.pinnedOnly = false;
            this.state.page = 0;
            this.syncSearchField();
            this.pushState();
            this.getList();
        });

        document.getElementById('btnApplyBulkOperate')?.addEventListener('click', () => this.applyBulkOperate());
        document.getElementById('btnClearSelection')?.addEventListener('click', () => {
            this.state.selectedIds.clear();
            this.syncSelectionState();
        });
        document.getElementById('btnSelectCurrentPage')?.addEventListener('click', () => this.updateCurrentPageSelection(true));
        document.getElementById('btnDeselectCurrentPage')?.addEventListener('click', () => this.updateCurrentPageSelection(false));
        document.getElementById('contentSelectAllOnPage')?.addEventListener('change', (event) => {
            this.updateCurrentPageSelection(event.target.checked);
        });
    },

    updatePageMeta() {
        const meta = ContentBoardConfig.getMeta(this.state.boardType).list;
        const badge = ContentBoardConfig.getMeta(this.state.boardType).badge;
        const titleEl = document.getElementById('contentPageTitle');
        const breadcrumbEl = document.getElementById('contentBreadcrumb');
        const createLabelEl = document.getElementById('contentCreateLabel');
        const badgeEl = document.getElementById('contentBoardBadge');
        const descEl = document.getElementById('contentBoardDescription');

        if (titleEl) titleEl.textContent = meta.pageTitle;
        if (breadcrumbEl) breadcrumbEl.textContent = meta.breadcrumb;
        if (createLabelEl) createLabelEl.textContent = meta.createLabel;
        if (badgeEl) badgeEl.textContent = badge;
        if (descEl) descEl.textContent = meta.description;
    },

    async applyOperationPolicy(settings = null) {
        const createButton = document.getElementById('btnNewContent');
        const bulkButton = document.getElementById('btnApplyBulkOperate');
        const bulkDeleteButton = document.getElementById('btnBulkDeleteContent');
        try {
            const resolvedSettings = settings || await CommonJS.fetchSystemSettings();
            const disabled = CommonJS.isCommunityWriteBlocked(resolvedSettings);
            const reason = CommonJS.getCommunityWriteBlockedReason(resolvedSettings, '커뮤니티 작성');
            CommonJS.setButtonDisabled(createButton, disabled, reason);
            CommonJS.setButtonDisabled(bulkButton, disabled, reason);
            CommonJS.setButtonDisabled(bulkDeleteButton, disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnSelectCurrentPage'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnDeselectCurrentPage'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('contentSelectAllOnPage'), disabled, reason);
            document.querySelectorAll('[data-role="content-edit"]').forEach((button) => {
                CommonJS.setButtonDisabled(button, disabled, reason);
            });
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
    },

    async getList() {
        const params = new URLSearchParams({
            page: this.state.page,
            size: this.state.size,
            boardType: this.state.boardType
        });
        if (this.state.keyword) {
            params.set('keyword', this.state.keyword);
        }
        if (this.state.status) {
            params.set('status', this.state.status);
        }
        if (this.state.publicYn) {
            params.set('publicYn', this.state.publicYn);
        }
        if (this.state.startDate) {
            params.set('startDate', this.state.startDate);
        }
        if (this.state.endDate) {
            params.set('endDate', this.state.endDate);
        }
        if (this.state.pinnedOnly) {
            params.set('pinnedOnly', 'true');
        }

        try {
            const res = await fetch(`/api/admin/content/list?${params}`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            const data = await res.json();
            this.renderList(data.items);
            this.renderPagination(data);
        } catch (err) {
            console.error('콘텐츠 목록 로드 실패:', err);
            CommonJS.alert('목록을 불러오는 중 오류가 발생했습니다.', '오류', 'error');
        }
    },

    renderList(items) {
        const grid = document.getElementById('contentGrid');
        if (!grid) return;
        this.state.currentPageIds = (items || []).map((item) => item.id);

        if (!items || items.length === 0) {
            grid.innerHTML = `
                <div class="col-12 text-center py-5">
                    <div class="mb-3 text-muted"><i class="fas fa-folder-open fa-3x opacity-25"></i></div>
                    <div class="text-muted">등록된 콘텐츠가 없습니다.</div>
                </div>`;
            this.syncSelectionState();
            return;
        }

        grid.innerHTML = items.map(item => `
            <div class="col-md-4 mb-4">
                    <div class="card h-100 content-board-card">
                    <div class="card-body content-board-card-body">
                        <div class="content-board-card-top">
                            <div class="d-flex gap-2 align-items-center flex-wrap">
                                <label class="form-check mb-0">
                                    <input class="form-check-input content-select-checkbox" type="checkbox" data-content-id="${item.id}" ${this.state.selectedIds.has(item.id) ? 'checked' : ''}>
                                </label>
                                <span class="content-board-card-badge">${ContentBoardConfig.escapeHtml(this.getBoardLabel(item.boardType))}</span>
                                ${item.pinnedYn === 'Y' ? '<span class="badge bg-dark">고정</span>' : ''}
                                <span class="badge ${item.status === 'PUBLISHED' ? 'bg-success-subtle text-success-emphasis' : 'bg-secondary-subtle text-secondary-emphasis'}">${item.status === 'PUBLISHED' ? '게시중' : '임시저장'}</span>
                                <span class="badge ${item.publicYn === 'Y' ? 'bg-primary-subtle text-primary-emphasis' : 'bg-warning-subtle text-warning-emphasis'}">${item.publicYn === 'Y' ? '공개' : '비공개'}</span>
                            </div>
                            <span class="content-board-card-views"><i class="far fa-eye me-1"></i>${item.viewCnt}</span>
                        </div>
                        <a class="content-board-card-link" href="/admin/content/get?id=${item.id}&boardType=${item.boardType}">
                            <h5 class="card-title content-board-card-title text-line-clamp-2">${ContentBoardConfig.escapeHtml(item.title || '제목 없음')}</h5>
                        </a>
                        <p class="content-board-card-copy">${ContentBoardConfig.escapeHtml(item.contentPreview || '내용 미리보기가 없습니다.')}</p>
                    </div>
                    <div class="card-footer content-board-card-footer">
                        <span class="content-board-card-date">${item.crtDtm}</span>
                        <div class="content-board-card-actions">
                            <button class="btn btn-sm btn-light" data-role="content-detail" data-content-id="${item.id}" data-board-type="${item.boardType}">상세</button>
                            <button class="btn btn-sm btn-outline-primary" data-role="content-edit" data-content-id="${item.id}" data-board-type="${item.boardType}">수정</button>
                        </div>
                    </div>
                </div>
            </div>
        `).join('');
        this.bindSelectionEvents();
        this.syncSelectionState();
        this.bindRowActions();
        this.applyOperationPolicy();
    },

    getBoardLabel(boardType) {
        const meta = ContentBoardConfig.getMeta(boardType).list;
        return meta ? meta.boardLabel : boardType;
    },

    renderPagination(data) {
        const { totalPages, currentPage: curr } = data;
        const pagination = document.getElementById('pagination');
        if (!pagination) return;

        let html = '';
        for (let i = 0; i < totalPages; i++) {
            html += `
                <li class="page-item ${i === curr ? 'active' : ''}">
                    <a class="page-link" href="javascript:void(0);" onclick="ContentList.goPage(${i})">${i + 1}</a>
                </li>`;
        }
        pagination.innerHTML = html;
    },

    goPage(page) {
        this.state.page = page;
        this.getList();
    },

    pushState() {
        const params = this.buildQueryParams();
        const newUrl = `${window.location.pathname}?${params.toString()}`;
        window.history.pushState({ path: newUrl }, '', newUrl);
    },

    buildQueryParams() {
        const params = new URLSearchParams({ boardType: this.state.boardType });
        if (this.state.keyword) {
            params.set('keyword', this.state.keyword);
        }
        if (this.state.status) {
            params.set('status', this.state.status);
        }
        if (this.state.publicYn) {
            params.set('publicYn', this.state.publicYn);
        }
        if (this.state.startDate) {
            params.set('startDate', this.state.startDate);
        }
        if (this.state.endDate) {
            params.set('endDate', this.state.endDate);
        }
        if (this.state.pinnedOnly) {
            params.set('pinnedOnly', 'true');
        }
        return params;
    },

    syncSearchField() {
        const searchInput = document.getElementById('contentSearchKeyword');
        if (searchInput) {
            searchInput.value = this.state.keyword;
        }
        const statusInput = document.getElementById('contentStatusFilter');
        if (statusInput) {
            statusInput.value = this.state.status;
        }
        const publicInput = document.getElementById('contentPublicFilter');
        if (publicInput) {
            publicInput.value = this.state.publicYn;
        }
        const startDateInput = document.getElementById('contentStartDate');
        if (startDateInput) {
            startDateInput.value = this.state.startDate;
        }
        const endDateInput = document.getElementById('contentEndDate');
        if (endDateInput) {
            endDateInput.value = this.state.endDate;
        }
        const pinnedInput = document.getElementById('contentPinnedOnly');
        if (pinnedInput) {
            pinnedInput.checked = this.state.pinnedOnly;
        }
    },

    bindSelectionEvents() {
        document.querySelectorAll('.content-select-checkbox').forEach((checkbox) => {
            checkbox.addEventListener('change', () => {
                const id = Number(checkbox.dataset.contentId);
                if (checkbox.checked) {
                    this.state.selectedIds.add(id);
                } else {
                    this.state.selectedIds.delete(id);
                }
                this.syncSelectionState();
            });
        });
    },

    bindRowActions() {
        document.querySelectorAll('[data-role="content-detail"]').forEach((button) => {
            button.addEventListener('click', () => {
                location.href = `/admin/content/get?id=${button.dataset.contentId}&boardType=${button.dataset.boardType}`;
            });
        });
        document.querySelectorAll('[data-role="content-edit"]').forEach((button) => {
            button.addEventListener('click', async () => {
                const settings = await CommonJS.fetchSystemSettings();
                if (CommonJS.isCommunityWriteBlocked(settings)) {
                    await CommonJS.alert(CommonJS.getCommunityWriteBlockedReason(settings, '커뮤니티 수정'), '알림', 'warning');
                    return;
                }
                location.href = `/admin/content/edit?id=${button.dataset.contentId}&boardType=${button.dataset.boardType}`;
            });
        });
    },

    syncSelectionState() {
        const meta = document.getElementById('contentSelectionMeta');
        const selectedOnPageCount = this.state.currentPageIds.filter((id) => this.state.selectedIds.has(id)).length;
        if (meta) {
            meta.textContent = `전체 선택 ${this.state.selectedIds.size}건 · 현재 페이지 ${selectedOnPageCount}/${this.state.currentPageIds.length || 0}건`;
        }
        const selectAllOnPage = document.getElementById('contentSelectAllOnPage');
        if (selectAllOnPage) {
            const hasCurrentPageItems = this.state.currentPageIds.length > 0;
            selectAllOnPage.checked = hasCurrentPageItems && selectedOnPageCount === this.state.currentPageIds.length;
            selectAllOnPage.indeterminate = hasCurrentPageItems && selectedOnPageCount > 0 && selectedOnPageCount < this.state.currentPageIds.length;
        }
        const resultMeta = document.getElementById('contentBulkResultMeta');
        if (resultMeta) {
            resultMeta.textContent = this.state.lastBulkResultMessage;
        }
    },

    updateCurrentPageSelection(checked) {
        // 선택 집합은 페이지 이동 후에도 유지해서, 여러 페이지를 넘겨가며 일괄 적용할 수 있게 둔다.
        this.state.currentPageIds.forEach((id) => {
            if (checked) {
                this.state.selectedIds.add(id);
            } else {
                this.state.selectedIds.delete(id);
            }
        });
        document.querySelectorAll('.content-select-checkbox').forEach((checkbox) => {
            checkbox.checked = checked;
        });
        this.syncSelectionState();
    },

    async applyBulkOperate() {
        if (!this.state.selectedIds.size) {
            await CommonJS.alert('일괄 적용할 게시글을 선택하세요.', '알림', 'warning');
            return;
        }

        const payload = {
            ids: Array.from(this.state.selectedIds),
            status: document.getElementById('contentBulkStatus')?.value || null,
            publicYn: document.getElementById('contentBulkPublicYn')?.value || null,
            pinnedYn: document.getElementById('contentBulkPinnedYn')?.value || null
        };

        if (!payload.status && !payload.publicYn && !payload.pinnedYn) {
            await CommonJS.alert('변경할 항목을 하나 이상 선택하세요.', '알림', 'warning');
            return;
        }

        const confirmed = await CommonJS.confirm(`선택한 게시글 ${this.state.selectedIds.size}건에 일괄 적용하시겠습니까?`);
        if (!confirmed) {
            return;
        }

        const response = await fetch('/api/admin/content/bulk-operate', {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            await CommonJS.alert(await CommonJS.extractErrorMessage(response, '일괄 적용에 실패했습니다.'), '오류', 'error');
            return;
        }

        const result = await response.json();
        if (result.updatedCount > 0) {
            this.state.selectedIds.clear();
            this.state.lastBulkResultMessage = `선택 ${result.requestedCount}건 중 ${result.updatedCount}건을 변경했습니다. ${result.unchangedCount}건은 기존 상태를 유지했고, 적용 후 선택을 해제했습니다.`;
        } else {
            this.state.lastBulkResultMessage = `선택 ${result.requestedCount}건이 모두 현재 상태와 같아서 변경하지 않았습니다. 선택은 유지됩니다.`;
        }
        this.syncSelectionState();
        await CommonJS.alert(this.state.lastBulkResultMessage, '성공', 'success');
        this.getList();
    },

    async applyBulkDelete() {
        if (!this.state.selectedIds.size) {
            await CommonJS.alert('삭제할 게시글을 선택하세요.', '알림', 'warning');
            return;
        }

        const confirmed = await CommonJS.confirm(`선택한 게시글 ${this.state.selectedIds.size}건을 삭제하시겠습니까?`);
        if (!confirmed) {
            return;
        }

        const response = await fetch('/api/admin/content/bulk-delete', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ ids: Array.from(this.state.selectedIds) })
        });

        if (!response.ok) {
            await CommonJS.alert(await CommonJS.extractErrorMessage(response, '일괄 삭제에 실패했습니다.'), '오류', 'error');
            return;
        }

        const result = await response.json();
        this.state.selectedIds.clear();
        this.state.lastBulkResultMessage = `선택 ${result.requestedCount}건 중 ${result.deletedCount}건을 삭제했습니다. 누락 ${result.missingCount}건은 이미 삭제되었거나 찾을 수 없습니다.`;
        this.syncSelectionState();
        await CommonJS.alert(this.state.lastBulkResultMessage, '성공', 'success');
        this.getList();
    },

    async exportCsv() {
        const button = document.getElementById('btnExportContentCsv');
        if (button) {
            button.disabled = true;
        }
        try {
            const response = await fetch(`/api/admin/content/export?${this.buildQueryParams().toString()}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '콘텐츠 CSV 내보내기에 실패했습니다.'));
            }
            const blob = await response.blob();
            const fileName = this.extractFileName(response.headers.get('Content-Disposition'), `contents-${this.state.boardType}.csv`);
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
    }
};

document.addEventListener('DOMContentLoaded', () => ContentList.init());
