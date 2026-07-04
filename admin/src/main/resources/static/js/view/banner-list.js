const BannerList = {
    initialized: false,
    modal: null,
    state: {
        page: 0,
        size: 10,
        keyword: '',
        isActive: '',
        exposureStatus: '',
        bannerNo: '',
        pageSource: '',
        source: '',
        returnTo: '',
    },
    operationPolicy: null,
    saveInFlight: false,
    exportInFlight: false,
    bulkInFlight: false,
    selectedBannerNos: new Set(),
    toggleInFlight: new Set(),
    deleteInFlight: new Set(),

    init() {
        if (this.initialized) return;
        this.initialized = true;
        const modalEl = document.getElementById('bannerModal');
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
            const reason = '유지보수 모드에서는 배너 등록, 수정, 상태 변경, 삭제가 불가능합니다.';
            CommonJS.setButtonDisabled(document.getElementById('btnNewBanner'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnSaveBanner'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnApplyBannerBulk'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnBulkDeleteBanner'), disabled, reason);
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
    },

    bindEvents() {
        document.getElementById('btnNewBanner')?.addEventListener('click', () => {
            this.openModal();
        });

        document.getElementById('btnSaveBanner')?.addEventListener('click', () => this.saveBanner());
        document.getElementById('btnExportBannerCsv')?.addEventListener('click', () => this.exportCsv());
        document.getElementById('btnApplyBannerBulk')?.addEventListener('click', () => this.applyBulkOperation());
        document.getElementById('btnBulkDeleteBanner')?.addEventListener('click', () => this.applyBulkDelete());
        document.getElementById('btnClearBannerSelection')?.addEventListener('click', () => this.clearSelection());
        document.getElementById('bannerSelectPage')?.addEventListener('change', (event) => this.toggleSelectCurrentPage(event.target.checked));
        document.getElementById('bannerStatTotalCard')?.addEventListener('click', () => this.applyStatFilter('total'));
        document.getElementById('bannerStatLiveCard')?.addEventListener('click', () => this.applyStatFilter('live'));
        document.getElementById('bannerStatScheduledCard')?.addEventListener('click', () => this.applyStatFilter('scheduled'));
        document.getElementById('bannerStatEndedCard')?.addEventListener('click', () => this.applyStatFilter('ended'));
        document.getElementById('bannerStatInactiveCard')?.addEventListener('click', () => this.applyStatFilter('inactive'));

        document.getElementById('btnSearchBanner')?.addEventListener('click', () => this.getList());
        document.getElementById('btnResetBanner')?.addEventListener('click', () => this.resetFilters());
        document.getElementById('bannerPageSize')?.addEventListener('change', () => {
            this.state.page = 0;
            this._updateStateFromInputs();
            this.getList();
        });
        document.getElementById('bannerListBody')?.addEventListener('click', (event) => {
            const checkbox = event.target.closest('[data-role="select-banner"]');
            if (checkbox) {
                this.toggleSelection(Number(checkbox.dataset.bannerNo), checkbox.checked);
                return;
            }
            const editButton = event.target.closest('[data-role="edit-banner"]');
            if (editButton) {
                this.openEditModal(JSON.parse(editButton.dataset.banner));
                return;
            }

            const detailButton = event.target.closest('[data-role="open-banner-detail"]');
            if (detailButton) {
                this.openBannerDetail(Number(detailButton.dataset.bannerNo), '목록 제목');
                return;
            }

            const toggleButton = event.target.closest('[data-role="toggle-banner"]');
            if (toggleButton) {
                this.toggleActive(Number(toggleButton.dataset.bannerNo), toggleButton.dataset.nextActive);
                return;
            }

            const deleteButton = event.target.closest('[data-role="delete-banner"]');
            if (deleteButton) {
                this.deleteBanner(Number(deleteButton.dataset.bannerNo));
            }
        });
        document.getElementById('bannerKeyword')?.addEventListener('keydown', (event) => {
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
        this.state.keyword = params.get('keyword') || '';
        this.state.isActive = params.get('isActive') || '';
        this.state.exposureStatus = params.get('exposureStatus') || '';
        this.state.bannerNo = this.isValidBannerNo(params.get('bannerNo')) ? String(Number(params.get('bannerNo'))) : '';
        this.state.pageSource = params.get('source') || '';
        this.state.source = this.state.pageSource;
        this.state.returnTo = params.get('returnTo') || '';
        document.getElementById('bannerKeyword').value = this.state.keyword;
        document.getElementById('bannerIsActiveFilter').value = this.state.isActive;
        document.getElementById('bannerExposureStatusFilter').value = this.state.exposureStatus;
        document.getElementById('bannerPageSize').value = String(this.state.size);
        CommonJS.bindMainLogoNavigation(this.state.returnTo || '/admin/banners');
        CommonJS.renderSourceContextNotice({ noticeId: 'bannerSourceContextNotice', source: this.state.pageSource });
    },

    buildParams() {
        const params = new URLSearchParams();
        params.set('page', String(this.state.page));
        params.set('size', String(this.state.size));
        if (this.state.keyword) params.set('keyword', this.state.keyword);
        if (this.state.isActive) params.set('isActive', this.state.isActive);
        if (this.state.exposureStatus) params.set('exposureStatus', this.state.exposureStatus);
        if (this.state.bannerNo) params.set('bannerNo', this.state.bannerNo);
        if (this.state.pageSource) params.set('source', this.state.pageSource);
        if (this.state.returnTo) params.set('returnTo', this.state.returnTo);
        return params;
    },

    async getList() {
        try {
            this._updateStateFromInputs();
            if (!this.validateState()) {
                return;
            }
            const params = this.buildParams();
            history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
            this.setFilterMeta('적용 필터를 계산하는 중입니다...');
            this.setResultMeta('결과 메타를 계산하는 중입니다...');
            this.setPageMeta('페이지 메타를 계산하는 중입니다...');
            this.setListStateMeta('loading', '배너 목록을 불러오는 중입니다.', 0, 0, '');
            this.renderLoadingState();
            const res = await fetch(`/api/admin/banners/list?${params.toString()}`);
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '배너 목록을 불러오지 못했습니다.'));
            const data = await res.json();
            this.renderList(data.items || []);
            this.renderStats(data.bannerStats);
            this.renderMeta(data);
            this.renderPagination(data);
            await this.openDeepLinkedBannerIfNeeded(data.items || []);
        } catch (err) {
            document.getElementById('bannerMetaText').textContent = err.message;
            this.setFilterMeta(err.message);
            this.setResultMeta('결과 메타 확인 불가');
            this.setPageMeta('페이지 메타 확인 불가');
            this.renderStats(null);
            this.renderErrorState(err.message || '배너 목록을 불러오지 못했습니다.');
            document.getElementById('bannerPagination').innerHTML = '';
            this.setListStateMeta('error', err.message, 0, 0, '');
        }
    },

    renderList(items) {
        const tbody = document.getElementById('bannerListBody');
        if (!tbody) return;

        if (!items || items.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="py-5">
                        <div class="product-empty-state">
                            <i class="fas fa-images product-empty-state-icon"></i>
                            <strong>조건에 맞는 배너가 없습니다.</strong>
                            <p>${this.buildEmptyStateMessage()}</p>
                        </div>
                    </td>
                </tr>
            `;
            this.setListStateMeta('empty', '조건에 맞는 배너가 없습니다.', 0, 0, '');
            this.updateSelectionMeta([]);
            return;
        }

        tbody.innerHTML = items.map(item => `
            <tr>
                <td class="ps-4">
                    <input type="checkbox" data-role="select-banner" data-banner-no="${item.bannerNo}" ${this.selectedBannerNos.has(item.bannerNo) ? 'checked' : ''}>
                </td>
                <td class="ps-4 text-center fw-bold">${item.sortOrder}</td>
                <td>
                    <img src="${item.imageUrl}" class="banner-preview-img" alt="banner" 
                         onerror="CommonJS.handleImageError(this)">
                </td>
                <td>
                    <button type="button" class="btn btn-link p-0 fw-bold text-dark text-decoration-none" data-role="open-banner-detail" data-banner-no="${item.bannerNo}">${this.escapeHtml(item.title)}</button>
                    <div class="text-muted small">${item.targetUrl || '이동 링크 없음'}</div>
                </td>
                <td>
                    <div class="small">${item.startDtm.replace('T', ' ')}</div>
                    <div class="small text-muted">~ ${item.endDtm.replace('T', ' ')}</div>
                </td>
                <td class="text-center">
                    <span class="badge rounded-pill ${item.isActive === 'Y' ? 'badge-y' : 'badge-n'}">
                        ${item.displayStatus}
                    </span>
                </td>
                <td class="text-end pe-4">
                    <button class="btn btn-sm btn-outline-primary me-1" data-role="edit-banner" data-banner='${JSON.stringify(item).replace(/'/g, '&#39;')}'>수정</button>
                    <button class="btn btn-sm btn-outline-dark me-1" data-role="toggle-banner" data-banner-no="${item.bannerNo}" data-next-active="${item.isActive === 'Y' ? 'N' : 'Y'}">${item.isActive === 'Y' ? '중지' : '활성'}</button>
                    <button class="btn btn-sm btn-outline-danger" data-role="delete-banner" data-banner-no="${item.bannerNo}">삭제</button>
                </td>
            </tr>
        `).join('');
        this.setListStateMeta('ready', '', items.length, null, null);
        this.updateSelectionMeta(items);
    },

    renderMeta(data) {
        document.getElementById('bannerMetaText').textContent = data.resultMeta?.resultLabel || `${data.totalElements || (data.items || []).length}건 조회`;
        this.setFilterMeta(`필터 ${data.resultMeta?.appliedFilterCount ?? 0}개 · ${data.resultMeta?.querySignature || '정렬 순서 기준'}`);
        this.setResultMeta(data.resultMeta?.resultLabel || '결과 메타 없음');
        this.setPageMeta(data.resultMeta?.pageInfoLabel || '페이지 메타 없음');
        this.setListStateMeta(
            'ready',
            '',
            (data.items || []).length,
            data.totalElements || 0,
            data.resultMeta?.querySignature || ''
        );
        const metaEl = document.getElementById('bannerListStateMeta');
        if (metaEl) {
            metaEl.dataset.pageInfoLabel = data.resultMeta?.pageInfoLabel || '';
        }
    },

    renderStats(stats) {
        const totalCountEl = document.getElementById('bannerTotalCount');
        const liveCountEl = document.getElementById('bannerLiveCount');
        const scheduledCountEl = document.getElementById('bannerScheduledCount');
        const endedCountEl = document.getElementById('bannerEndedCount');
        const inactiveCountEl = document.getElementById('bannerInactiveCount');
        const contextTextEl = document.getElementById('bannerStatsContextText');
        const noticeEl = document.getElementById('bannerStatsNotice');

        if (!stats) {
            if (totalCountEl) totalCountEl.innerText = '0';
            if (liveCountEl) liveCountEl.innerText = '0';
            if (scheduledCountEl) scheduledCountEl.innerText = '0';
            if (endedCountEl) endedCountEl.innerText = '0';
            if (inactiveCountEl) inactiveCountEl.innerText = '0';
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

        contextTextEl.innerText = `${stats.contextLabel} · ${stats.querySignature}`;
        const usingQuickFilter = !!this.state.exposureStatus;
        noticeEl.innerText = usingQuickFilter
            ? '카드 수치는 기본 탐색 문맥 기준이며, 선택한 빠른 필터는 목록에만 적용됩니다.'
            : '카드 수치는 현재 탐색 문맥 기준입니다.';
        noticeEl.dataset.statsContext = usingQuickFilter ? 'base-query' : 'current-query';
    },

    renderPagination(data) {
        const paginationEl = document.getElementById('bannerPagination');
        if (!paginationEl) {
            return;
        }

        const totalPages = Number(data.totalPages || 0);
        const currentPage = Number(data.currentPage || 0);

        if (totalPages <= 1) {
            paginationEl.innerHTML = '';
            return;
        }

        paginationEl.innerHTML = Array.from({ length: totalPages }, (_, index) => `
            <li class="page-item ${index === currentPage ? 'active' : ''}">
                <button type="button" class="page-link" data-role="go-banner-page" data-page="${index}">${index + 1}</button>
            </li>
        `).join('');

        paginationEl.querySelectorAll('[data-role="go-banner-page"]').forEach((button) => {
            button.addEventListener('click', () => this.goPage(Number(button.dataset.page)));
        });
    },

    setFilterMeta(message) {
        document.getElementById('bannerFilterMeta').textContent = message;
    },

    setResultMeta(message) {
        document.getElementById('bannerResultMeta').textContent = message;
    },

    setPageMeta(message) {
        document.getElementById('bannerPageMeta').textContent = message;
    },

    setListStateMeta(state, message, visibleCount, totalElements, querySignature) {
        const metaEl = document.getElementById('bannerListStateMeta');
        if (!metaEl) {
            return;
        }

        metaEl.dataset.listState = state;
        metaEl.dataset.stateMessage = message || '';
        if (visibleCount != null) {
            metaEl.dataset.visibleCount = String(visibleCount);
        }
        if (totalElements != null) {
            metaEl.dataset.totalElements = String(totalElements);
        }
        if (querySignature != null) {
            metaEl.dataset.querySignature = querySignature;
        }
    },

    renderLoadingState() {
        const tbody = document.getElementById('bannerListBody');
        if (!tbody) {
            return;
        }
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="py-5">
                    <div class="product-loading-state">
                        <div class="spinner-border spinner-border-sm text-primary" role="status" aria-hidden="true"></div>
                        <strong>배너 목록을 불러오는 중입니다.</strong>
                        <p>현재 필터 조건에 맞는 배너 운영 목록을 조회하고 있습니다.</p>
                    </div>
                </td>
            </tr>
        `;
    },

    renderErrorState(message) {
        const tbody = document.getElementById('bannerListBody');
        if (!tbody) {
            return;
        }
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="py-5">
                    <div class="product-empty-state">
                        <div class="product-empty-state__icon text-danger">
                            <i class="fa-solid fa-triangle-exclamation"></i>
                        </div>
                        <strong>배너 목록을 불러오지 못했습니다.</strong>
                        <p>${this.escapeHtml(message)}</p>
                    </div>
                </td>
            </tr>
        `;
    },

    buildEmptyStateMessage() {
        const parts = [];
        if (this.state.keyword) {
            parts.push(`검색어 "${this.state.keyword}"`);
        }
        if (this.state.isActive) {
            parts.push(`상태 ${this.state.isActive === 'Y' ? '사용' : '중지'}`);
        }
        if (this.state.exposureStatus) {
            parts.push(`노출 기간 ${this.resolveExposureLabel(this.state.exposureStatus)}`);
        }

        if (!parts.length) {
            return '등록된 배너가 아직 없거나, 현재 페이지에 표시할 배너가 없습니다.';
        }

        return `${parts.join(', ')} 조건에 맞는 배너가 없습니다.`;
    },

    resetFilters() {
        document.getElementById('bannerKeyword').value = '';
        document.getElementById('bannerIsActiveFilter').value = '';
        document.getElementById('bannerExposureStatusFilter').value = '';
        document.getElementById('bannerPageSize').value = '10';
        this.state.page = 0;
        this.state.bannerNo = '';
        this.state.source = this.state.pageSource;
        this.getList();
    },

    applyStatFilter(type) {
        this.state.page = 0;
        document.getElementById('bannerExposureStatusFilter').value = '';
        document.getElementById('bannerIsActiveFilter').value = '';
        switch (type) {
            case 'live':
                document.getElementById('bannerExposureStatusFilter').value = 'LIVE';
                break;
            case 'scheduled':
                document.getElementById('bannerExposureStatusFilter').value = 'SCHEDULED';
                break;
            case 'ended':
                document.getElementById('bannerExposureStatusFilter').value = 'ENDED';
                break;
            case 'inactive':
                document.getElementById('bannerIsActiveFilter').value = 'N';
                break;
            case 'total':
            default:
                break;
        }
        this.getList();
    },

    async exportCsv() {
        if (this.exportInFlight) {
            return;
        }
        const button = document.getElementById('btnExportBannerCsv');
        try {
            this.exportInFlight = true;
            CommonJS.setButtonDisabled(button, true, '내보내는 중입니다.');
            this._updateStateFromInputs();
            const params = this.buildParams();
            params.delete('page');
            params.delete('size');
            await CommonJS.downloadFile(`/api/admin/banners/export?${params.toString()}`, 'banners.csv');
        } catch (error) {
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.exportInFlight = false;
            CommonJS.setButtonDisabled(button, false);
        }
    },

    toggleSelection(bannerNo, checked) {
        if (!Number.isFinite(bannerNo) || bannerNo <= 0) {
            return;
        }
        if (checked) {
            this.selectedBannerNos.add(bannerNo);
        } else {
            this.selectedBannerNos.delete(bannerNo);
        }
        const items = Array.from(document.querySelectorAll('[data-role="select-banner"]'))
            .map((checkbox) => ({ bannerNo: Number(checkbox.dataset.bannerNo) }))
            .filter((item) => Number.isFinite(item.bannerNo));
        this.updateSelectionMeta(items);
    },

    toggleSelectCurrentPage(checked) {
        document.querySelectorAll('[data-role="select-banner"]').forEach((checkbox) => {
            checkbox.checked = checked;
            const bannerNo = Number(checkbox.dataset.bannerNo);
            if (checked) {
                this.selectedBannerNos.add(bannerNo);
            } else {
                this.selectedBannerNos.delete(bannerNo);
            }
        });
        const items = Array.from(document.querySelectorAll('[data-role="select-banner"]'))
            .map((checkbox) => ({ bannerNo: Number(checkbox.dataset.bannerNo) }))
            .filter((item) => Number.isFinite(item.bannerNo));
        this.updateSelectionMeta(items);
    },

    clearSelection() {
        this.selectedBannerNos.clear();
        const selectPage = document.getElementById('bannerSelectPage');
        if (selectPage) {
            selectPage.checked = false;
        }
        document.querySelectorAll('[data-role="select-banner"]').forEach((checkbox) => {
            checkbox.checked = false;
        });
        this.updateSelectionMeta([]);
    },

    updateSelectionMeta(items) {
        const totalSelected = this.selectedBannerNos.size;
        const visibleBannerNos = new Set((items || []).map((item) => item.bannerNo));
        const visibleSelected = Array.from(this.selectedBannerNos).filter((bannerNo) => visibleBannerNos.has(bannerNo)).length;
        const metaEl = document.getElementById('bannerSelectionMeta');
        if (metaEl) {
            metaEl.textContent = totalSelected === 0
                ? '선택된 배너가 없습니다.'
                : `총 ${totalSelected}건 선택됨 · 현재 페이지 ${visibleSelected}건`;
        }
        const selectPage = document.getElementById('bannerSelectPage');
        if (selectPage) {
            selectPage.checked = items.length > 0 && visibleSelected === items.length;
        }
    },

    async applyBulkOperation() {
        if (this.bulkInFlight) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 배너 상태 변경이 불가능합니다.', '알림', 'warning');
            return;
        }
        if (this.selectedBannerNos.size === 0) {
            await CommonJS.alert('일괄 변경할 배너를 선택하세요.', '알림', 'warning');
            return;
        }
        const isActive = document.getElementById('bulkBannerIsActive').value;
        if (!isActive) {
            await CommonJS.alert('적용할 상태를 선택하세요.', '알림', 'warning');
            return;
        }
        try {
            this.bulkInFlight = true;
            const response = await fetch('/api/admin/banners/bulk-operate', {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    bannerNos: Array.from(this.selectedBannerNos),
                    isActive
                })
            });
            if (!response.ok) throw new Error(await CommonJS.extractErrorMessage(response, '배너 일괄 변경에 실패했습니다.'));
            const result = await response.json();
            await this.getList();
            await CommonJS.alert(`일괄 상태 변경이 완료되었습니다. 변경 ${result.updatedCount}건 / 유지 ${result.unchangedCount}건`, '성공', 'success');
        } catch (error) {
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.bulkInFlight = false;
        }
    },

    async applyBulkDelete() {
        if (this.bulkInFlight) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 배너 삭제가 불가능합니다.', '알림', 'warning');
            return;
        }
        if (this.selectedBannerNos.size === 0) {
            await CommonJS.alert('삭제할 배너를 선택하세요.', '알림', 'warning');
            return;
        }
        const confirmed = await CommonJS.confirm(`선택한 배너 ${this.selectedBannerNos.size}건을 삭제하시겠습니까?`);
        if (!confirmed) {
            return;
        }
        try {
            this.bulkInFlight = true;
            const response = await fetch('/api/admin/banners/bulk-delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    bannerNos: Array.from(this.selectedBannerNos)
                })
            });
            if (!response.ok) throw new Error(await CommonJS.extractErrorMessage(response, '배너 일괄 삭제에 실패했습니다.'));
            const result = await response.json();
            this.clearSelection();
            await this.getList();
            await CommonJS.alert(`일괄 삭제가 완료되었습니다. 삭제 ${result.deletedCount}건`, '성공', 'success');
        } catch (error) {
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.bulkInFlight = false;
        }
    },

    goPage(page) {
        if (!Number.isInteger(page) || page < 0) {
            return;
        }
        this.state.page = page;
        this.getList();
    },

    async openDeepLinkedBannerIfNeeded(items) {
        if (!this.state.bannerNo) {
            return;
        }
        const bannerNo = Number(this.state.bannerNo);
        if (!this.isValidBannerNo(bannerNo)) {
            this.state.bannerNo = '';
            const params = this.buildParams();
            history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
            await CommonJS.alert('유효하지 않은 배너 번호입니다.', '알림', 'warning');
            return;
        }
        const target = items.find((item) => item.bannerNo === bannerNo);
        if (target) {
            await this.openEditModal(target);
        } else if (bannerNo > 0) {
            await this.openBannerDetail(bannerNo, this.state.source || '딥링크');
        }
        this.state.bannerNo = '';
        const params = this.buildParams();
        history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
    },

    async openBannerDetail(bannerNo, source = '목록') {
        if (!this.isValidBannerNo(bannerNo)) {
            await CommonJS.alert('유효하지 않은 배너 번호입니다.', '알림', 'warning');
            return;
        }
        try {
            this.state.bannerNo = String(bannerNo);
            this.state.source = source;
            const params = this.buildParams();
            history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);

            const res = await fetch(`/api/admin/banners/${bannerNo}`);
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '배너 상세를 불러오지 못했습니다.'));
            await this.openEditModal(await res.json());
        } catch (err) {
            await CommonJS.alert(err.message, '오류', 'error');
        }
    },

    async openModal() {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 배너 등록이 불가능합니다.', '알림', 'warning');
            return;
        }
        document.getElementById('bannerForm').reset();
        document.getElementById('bannerNo').value = '';
        document.getElementById('bannerModalTitle').innerText = '신규 배너 등록';
        this.modal.show();
    },

    async openEditModal(item) {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 배너 수정이 불가능합니다.', '알림', 'warning');
            return;
        }
        if (!item || !this.isValidBannerNo(item.bannerNo)) {
            await CommonJS.alert('유효하지 않은 배너 정보입니다.', '알림', 'warning');
            return;
        }
        document.getElementById('bannerNo').value = item.bannerNo;
        document.getElementById('title').value = item.title;
        document.getElementById('imageUrl').value = item.imageUrl;
        document.getElementById('targetUrl').value = item.targetUrl || '';
        document.getElementById('startDtm').value = item.startDtm.substring(0, 16);
        document.getElementById('endDtm').value = item.endDtm.substring(0, 16);
        document.getElementById('sortOrder').value = item.sortOrder;
        document.getElementById('isActive').value = item.isActive;
        document.getElementById('bannerModalTitle').innerText = '배너 수정';
        this.modal.show();
    },

    async saveBanner() {
        if (this.saveInFlight) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 배너 저장이 불가능합니다.', '알림', 'warning');
            return;
        }
        const formData = {
            bannerNo: document.getElementById('bannerNo').value || null,
            title: CommonJS.normalizeOptionalText(document.getElementById('title').value),
            imageUrl: CommonJS.normalizeOptionalText(document.getElementById('imageUrl').value),
            targetUrl: CommonJS.normalizeOptionalText(document.getElementById('targetUrl').value) || '',
            startDtm: document.getElementById('startDtm').value,
            endDtm: document.getElementById('endDtm').value,
            sortOrder: document.getElementById('sortOrder').value,
            isActive: document.getElementById('isActive').value
        };
        const parsedBannerNo = formData.bannerNo ? Number(formData.bannerNo) : null;
        const parsedSortOrder = Number(formData.sortOrder);

        if (!formData.title || !formData.imageUrl || !formData.startDtm || !formData.endDtm) {
            await CommonJS.alert('필수 항목을 모두 입력하세요.', '알림', 'warning');
            return;
        }
        if (parsedBannerNo != null && !this.isValidBannerNo(parsedBannerNo)) {
            await CommonJS.alert('유효하지 않은 배너 번호입니다.', '알림', 'warning');
            return;
        }
        if (!Number.isInteger(parsedSortOrder) || parsedSortOrder < 0) {
            await CommonJS.alert('정렬 순서는 0 이상의 숫자여야 합니다.', '알림', 'warning');
            document.getElementById('sortOrder')?.focus();
            return;
        }
        if (!this.validateBannerPeriod(formData.startDtm, formData.endDtm)) {
            await CommonJS.alert('노출 시작 일시는 종료 일시보다 늦을 수 없습니다.', '알림', 'warning');
            document.getElementById('endDtm')?.focus();
            return;
        }
        formData.bannerNo = parsedBannerNo;
        formData.sortOrder = parsedSortOrder;

        try {
            this.saveInFlight = true;
            CommonJS.setButtonDisabled(document.getElementById('btnSaveBanner'), true, '저장 중입니다.');
            const res = await fetch('/api/admin/banners/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData)
            });

            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '저장 중 오류가 발생했습니다.'));

            this.modal.hide();
            await this.getList();
            await CommonJS.alert('성공적으로 저장되었습니다.', '성공', 'success');
        } catch (err) {
            await CommonJS.alert(err.message, '오류', 'error');
        } finally {
            this.saveInFlight = false;
            this.applyOperationPolicy(this.operationPolicy);
        }
    },

    async toggleActive(no, isActive) {
        if (this.toggleInFlight.has(no)) {
            return;
        }
        if (!this.isValidBannerNo(no)) {
            await CommonJS.alert('유효하지 않은 배너 번호입니다.', '알림', 'warning');
            return;
        }
        if (!this.isValidActiveValue(isActive)) {
            await CommonJS.alert('유효하지 않은 배너 상태 값입니다.', '알림', 'warning');
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 배너 상태 변경이 불가능합니다.', '알림', 'warning');
            return;
        }
        try {
            this.toggleInFlight.add(no);
            const res = await fetch(`/api/admin/banners/active/${no}?isActive=${isActive}`, { method: 'PATCH' });
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '상태 변경 중 오류가 발생했습니다.'));
            await this.getList();
            await CommonJS.alert('배너 상태가 변경되었습니다.', '성공', 'success');
        } catch (err) {
            await CommonJS.alert(err.message, '오류', 'error');
        } finally {
            this.toggleInFlight.delete(no);
        }
    },

    async deleteBanner(no) {
        if (this.deleteInFlight.has(no)) {
            return;
        }
        if (!this.isValidBannerNo(no)) {
            await CommonJS.alert('유효하지 않은 배너 번호입니다.', '알림', 'warning');
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 배너 삭제가 불가능합니다.', '알림', 'warning');
            return;
        }
        const confirm = await CommonJS.confirm('배너를 삭제하시겠습니까?');
        if (!confirm) return;

        try {
            this.deleteInFlight.add(no);
            const res = await fetch(`/api/admin/banners/delete?no=${no}`, { method: 'DELETE' });
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '삭제 중 오류가 발생했습니다.'));
            this.selectedBannerNos.delete(no);
            await this.getList();
            await CommonJS.alert('삭제되었습니다.', '성공', 'success');
        } catch (err) {
            await CommonJS.alert(err.message, '오류', 'error');
        } finally {
            this.deleteInFlight.delete(no);
        }
    },

    _updateStateFromInputs() {
        this.state.keyword = CommonJS.normalizeOptionalText(document.getElementById('bannerKeyword').value) || '';
        this.state.isActive = document.getElementById('bannerIsActiveFilter').value || '';
        this.state.exposureStatus = document.getElementById('bannerExposureStatusFilter').value || '';
        this.state.size = Number(document.getElementById('bannerPageSize').value || 10);
    },

    escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    },

    resolveExposureLabel(exposureStatus) {
        if (exposureStatus === 'LIVE') return '진행중';
        if (exposureStatus === 'SCHEDULED') return '대기';
        if (exposureStatus === 'ENDED') return '종료';
        return exposureStatus || '전체';
    },

    isValidBannerNo(bannerNo) {
        return Number.isInteger(Number(bannerNo)) && Number(bannerNo) > 0;
    },

    isValidActiveValue(isActive) {
        return isActive === 'Y' || isActive === 'N';
    },

    validateBannerPeriod(startDtm, endDtm) {
        if (!startDtm || !endDtm) {
            return true;
        }
        return new Date(startDtm).getTime() <= new Date(endDtm).getTime();
    },

    validateState() {
        if (this.state.keyword && this.state.keyword.length > 100) {
            void CommonJS.alert('검색어는 100자 이하로 입력하세요.', '알림', 'warning');
            return false;
        }
        if (this.state.isActive && !this.isValidActiveValue(this.state.isActive)) {
            void CommonJS.alert('활성 상태 필터 값이 올바르지 않습니다.', '알림', 'warning');
            return false;
        }
        if (this.state.exposureStatus && !['LIVE', 'SCHEDULED', 'ENDED'].includes(this.state.exposureStatus)) {
            void CommonJS.alert('노출 상태 필터 값이 올바르지 않습니다.', '알림', 'warning');
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
        return Number.isInteger(parsed) && parsed > 0 ? parsed : 10;
    }
};

document.addEventListener('DOMContentLoaded', () => BannerList.init());
