const BrandList = {
    initialized: false,
    modal: null,
    state: {
        page: 0,
        size: 10,
        keyword: '',
        isActive: '',
        source: '',
        returnTo: '',
    },
    operationPolicy: null,
    saveInFlight: false,
    exportInFlight: false,
    bulkInFlight: false,
    selectedBrandNos: new Set(),
    deleteInFlight: new Set(),

    init() {
        if (this.initialized) return;
        this.initialized = true;
        const modalEl = document.getElementById('brandModal');
        if (modalEl) {
            this.modal = new bootstrap.Modal(modalEl);
        } else {
            console.error('브랜드 모달 엘리먼트를 찾을 수 없습니다.');
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
            const reason = '유지보수 모드에서는 브랜드 등록, 수정, 삭제가 불가능합니다.';
            CommonJS.setButtonDisabled(document.getElementById('btnNewBrand'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnSaveBrand'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnApplyBrandBulk'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnBulkDeleteBrand'), disabled, reason);
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
    },

    bindEvents() {
        document.getElementById('btnNewBrand')?.addEventListener('click', () => {
            this.openModal();
        });

        document.getElementById('btnSaveBrand')?.addEventListener('click', () => this.saveBrand());
        document.getElementById('btnExportBrand')?.addEventListener('click', () => this.exportList());
        document.getElementById('btnApplyBrandBulk')?.addEventListener('click', () => this.applyBulkOperation());
        document.getElementById('btnBulkDeleteBrand')?.addEventListener('click', () => this.applyBulkDelete());
        document.getElementById('btnClearBrandSelection')?.addEventListener('click', () => this.clearSelection());
        document.getElementById('brandSelectPage')?.addEventListener('change', (event) => this.toggleSelectCurrentPage(event.target.checked));
        document.getElementById('brandStatTotalCard')?.addEventListener('click', () => this.applyStatFilter('total'));
        document.getElementById('brandStatActiveCard')?.addEventListener('click', () => this.applyStatFilter('active'));
        document.getElementById('brandStatInactiveCard')?.addEventListener('click', () => this.applyStatFilter('inactive'));

        document.getElementById('btnSearchBrand')?.addEventListener('click', () => this.getList());
        document.getElementById('btnResetBrand')?.addEventListener('click', () => this.resetFilters());
        document.getElementById('brandPageSize')?.addEventListener('change', () => {
            this.state.page = 0;
            this._updateStateFromInputs();
            this.getList();
        });
        document.getElementById('brandListBody')?.addEventListener('click', (event) => {
            const checkbox = event.target.closest('[data-role="select-brand"]');
            if (checkbox) {
                this.toggleSelection(Number(checkbox.dataset.brandNo), checkbox.checked);
                return;
            }

            const editButton = event.target.closest('[data-role="edit-brand"]');
            if (editButton) {
                this.openModal(Number(editButton.dataset.brandNo));
                return;
            }

            const deleteButton = event.target.closest('[data-role="delete-brand"]');
            if (deleteButton) {
                this.deleteBrand(Number(deleteButton.dataset.brandNo));
            }
        });
        document.getElementById('brandKeyword')?.addEventListener('keydown', (event) => {
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
        this.state.isActive = params.get('isActive') || '';
        this.state.source = params.get('source') || '';
        this.state.returnTo = params.get('returnTo') || '';
        document.getElementById('brandKeyword').value = this.state.keyword;
        document.getElementById('brandIsActiveFilter').value = this.state.isActive;
        document.getElementById('brandPageSize').value = String(this.state.size);
        CommonJS.bindMainLogoNavigation(this.state.returnTo || '/admin/brands');
        CommonJS.renderSourceContextNotice({ noticeId: 'brandSourceContextNotice', source: this.state.source });
    },

    buildParams() {
        const params = new URLSearchParams();
        params.set('page', String(this.state.page));
        params.set('size', String(this.state.size));
        if (this.state.keyword) params.set('keyword', this.state.keyword);
        if (this.state.isActive) params.set('isActive', this.state.isActive);
        if (this.state.source) params.set('source', this.state.source);
        if (this.state.returnTo) params.set('returnTo', this.state.returnTo);
        return params;
    },

    async getList() {
        try {
            this._updateStateFromInputs();
            const params = this.buildParams();
            history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
            this.setFilterMeta('적용 필터를 계산하는 중입니다...');
            this.setResultMeta('결과 메타를 계산하는 중입니다...');
            this.setPageMeta('페이지 메타를 계산하는 중입니다...');
            this.setListStateMeta('loading', '브랜드 목록을 불러오는 중입니다.', 0, 0, '');
            this.renderLoadingState();
            const res = await fetch(`/api/admin/brands/list?${params.toString()}`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            this.renderList(data.items || []);
            this.renderStats(data.brandStats);
            this.renderMeta(data);
            this.renderPagination(data);
        } catch (err) {
            console.error('브랜드 목록 로드 실패:', err);
            document.getElementById('brandMetaText').textContent = '브랜드 목록 조회 실패';
            this.setFilterMeta('브랜드 목록을 불러오지 못했습니다.');
            this.setResultMeta('결과 메타 확인 불가');
            this.setPageMeta('페이지 메타 확인 불가');
            this.renderStats(null);
            document.getElementById('brandPagination').innerHTML = '';
            this.setListStateMeta('error', '브랜드 목록 조회 실패', 0, 0, '');
        }
    },

    renderList(items) {
        const tbody = document.getElementById('brandListBody');
        if (!tbody) return;

        if (!items || items.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center py-5 text-muted">
                        <div class="product-empty-state">
                            <i class="fas fa-tags product-empty-state-icon"></i>
                            <strong>조건에 맞는 브랜드가 없습니다.</strong>
                            <p>${this.buildEmptyStateMessage()}</p>
                        </div>
                    </td>
                </tr>
            `;
            this.setListStateMeta('empty', '조건에 맞는 브랜드가 없습니다.', 0, 0, '');
            this.updateSelectionMeta([]);
            return;
        }

        tbody.innerHTML = items.map(item => `
            <tr>
                <td class="ps-4">
                    <input type="checkbox" data-role="select-brand" data-brand-no="${item.brandNo}" ${this.selectedBrandNos.has(item.brandNo) ? 'checked' : ''}>
                </td>
                <td class="ps-4 text-muted">${item.brandNo}</td>
                <td>
                    <div class="brand-logo-wrapper">
                        <img src="${item.logoUrl || ''}" class="brand-logo-img" alt="${this.escapeHtml(item.nameKo)}"
                             data-role="brand-logo" data-brand-name="${this.escapeHtml(item.nameKo)}">
                    </div>
                </td>
                <td class="fw-bold text-dark">${item.nameKo}</td>
                <td class="text-muted">${item.nameEn || '-'}</td>
                <td class="text-center">
                    <span class="badge rounded-pill ${item.isActive === 'Y' ? 'badge-y' : 'badge-n'}">
                        ${item.isActive === 'Y' ? '사용중' : '중지'}
                    </span>
                </td>
                <td class="text-end pe-4">
                    <button class="btn btn-sm btn-outline-primary me-1" data-role="edit-brand" data-brand-no="${item.brandNo}">수정</button>
                    <button class="btn btn-sm btn-outline-danger" data-role="delete-brand" data-brand-no="${item.brandNo}">삭제</button>
                </td>
            </tr>
        `).join('');
        this.bindLogoFallbacks();
        this.setListStateMeta('ready', '', items.length, null, null);
        this.updateSelectionMeta(items);
    },

    renderMeta(data) {
        document.getElementById('brandMetaText').textContent = data.resultMeta?.resultLabel || `${data.totalElements || (data.items || []).length}건 조회`;
        this.setFilterMeta(`필터 ${data.resultMeta?.appliedFilterCount ?? 0}개 · ${data.resultMeta?.querySignature || '브랜드명 기준'}`);
        this.setResultMeta(data.resultMeta?.resultLabel || '결과 메타 없음');
        this.setPageMeta(data.resultMeta?.pageInfoLabel || '페이지 메타 없음');
        this.setListStateMeta(
            'ready',
            '',
            (data.items || []).length,
            data.totalElements || 0,
            data.resultMeta?.querySignature || ''
        );
        const metaEl = document.getElementById('brandListStateMeta');
        if (metaEl) {
            metaEl.dataset.pageInfoLabel = data.resultMeta?.pageInfoLabel || '';
        }
    },

    renderStats(stats) {
        const totalCountEl = document.getElementById('brandTotalCount');
        const activeCountEl = document.getElementById('brandActiveCount');
        const inactiveCountEl = document.getElementById('brandInactiveCount');
        const contextTextEl = document.getElementById('brandStatsContextText');
        const noticeEl = document.getElementById('brandStatsNotice');

        if (!stats) {
            if (totalCountEl) totalCountEl.innerText = '0';
            if (activeCountEl) activeCountEl.innerText = '0';
            if (inactiveCountEl) inactiveCountEl.innerText = '0';
            if (contextTextEl) contextTextEl.innerText = '카드 기준을 확인할 수 없습니다.';
            if (noticeEl) {
                noticeEl.innerText = '카드 기준을 확인할 수 없습니다.';
                noticeEl.dataset.statsContext = 'error';
            }
            return;
        }

        totalCountEl.innerText = Number(stats.totalCount || 0).toLocaleString();
        activeCountEl.innerText = Number(stats.activeCount || 0).toLocaleString();
        inactiveCountEl.innerText = Number(stats.inactiveCount || 0).toLocaleString();
        contextTextEl.innerText = `${stats.contextLabel} · ${stats.querySignature}`;
        const usingQuickFilter = !!this.state.isActive;
        noticeEl.innerText = usingQuickFilter
            ? '카드 수치는 기본 탐색 문맥 기준이며, 선택한 빠른 필터는 목록에만 적용됩니다.'
            : '카드 수치는 현재 탐색 문맥 기준입니다.';
        noticeEl.dataset.statsContext = usingQuickFilter ? 'base-query' : 'current-query';
    },

    renderPagination(data) {
        const paginationEl = document.getElementById('brandPagination');
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
                <button type="button" class="page-link" data-role="go-brand-page" data-page="${index}">${index + 1}</button>
            </li>
        `).join('');

        paginationEl.querySelectorAll('[data-role="go-brand-page"]').forEach((button) => {
            button.addEventListener('click', () => this.goPage(Number(button.dataset.page)));
        });
    },

    setFilterMeta(message) {
        document.getElementById('brandFilterMeta').textContent = message;
    },

    setResultMeta(message) {
        document.getElementById('brandResultMeta').textContent = message;
    },

    setPageMeta(message) {
        document.getElementById('brandPageMeta').textContent = message;
    },

    setListStateMeta(state, message, visibleCount, totalElements, querySignature) {
        const metaEl = document.getElementById('brandListStateMeta');
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
        const tbody = document.getElementById('brandListBody');
        if (!tbody) {
            return;
        }
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center py-5 text-muted">
                    <div class="product-loading-state">
                        <div class="spinner-border spinner-border-sm text-primary" role="status" aria-hidden="true"></div>
                        <strong>브랜드 목록을 불러오는 중입니다.</strong>
                        <p>현재 필터 조건에 맞는 브랜드 운영 목록을 조회하고 있습니다.</p>
                    </div>
                </td>
            </tr>
        `;
    },

    bindLogoFallbacks() {
        document.querySelectorAll('[data-role="brand-logo"]').forEach((image) => {
            image.addEventListener('error', () => {
                CommonJS.handleImageError(image, image.dataset.brandName || '');
            }, { once: true });
        });
    },

    buildEmptyStateMessage() {
        const parts = [];
        if (this.state.keyword) {
            parts.push(`검색어 "${this.state.keyword}"`);
        }
        if (this.state.isActive) {
            parts.push(`상태 ${this.state.isActive === 'Y' ? '사용' : '중지'}`);
        }

        if (!parts.length) {
            return '등록된 브랜드가 아직 없거나, 현재 페이지에 표시할 브랜드가 없습니다.';
        }

        return `${parts.join(', ')} 조건에 맞는 브랜드가 없습니다.`;
    },

    toggleSelection(brandNo, checked) {
        if (!Number.isFinite(brandNo) || brandNo <= 0) {
            return;
        }
        if (checked) {
            this.selectedBrandNos.add(brandNo);
        } else {
            this.selectedBrandNos.delete(brandNo);
        }
        const items = Array.from(document.querySelectorAll('[data-role="select-brand"]'))
            .map((checkbox) => ({ brandNo: Number(checkbox.dataset.brandNo) }))
            .filter((item) => Number.isFinite(item.brandNo));
        this.updateSelectionMeta(items);
    },

    toggleSelectCurrentPage(checked) {
        document.querySelectorAll('[data-role="select-brand"]').forEach((checkbox) => {
            checkbox.checked = checked;
            const brandNo = Number(checkbox.dataset.brandNo);
            if (checked) {
                this.selectedBrandNos.add(brandNo);
            } else {
                this.selectedBrandNos.delete(brandNo);
            }
        });
        const items = Array.from(document.querySelectorAll('[data-role="select-brand"]'))
            .map((checkbox) => ({ brandNo: Number(checkbox.dataset.brandNo) }))
            .filter((item) => Number.isFinite(item.brandNo));
        this.updateSelectionMeta(items);
    },

    clearSelection() {
        this.selectedBrandNos.clear();
        const selectPage = document.getElementById('brandSelectPage');
        if (selectPage) {
            selectPage.checked = false;
        }
        document.querySelectorAll('[data-role="select-brand"]').forEach((checkbox) => {
            checkbox.checked = false;
        });
        this.updateSelectionMeta([]);
    },

    updateSelectionMeta(items) {
        const totalSelected = this.selectedBrandNos.size;
        const visibleBrandNos = new Set((items || []).map((item) => item.brandNo));
        const visibleSelected = Array.from(this.selectedBrandNos).filter((brandNo) => visibleBrandNos.has(brandNo)).length;
        const metaEl = document.getElementById('brandSelectionMeta');
        if (metaEl) {
            metaEl.textContent = totalSelected === 0
                ? '선택된 브랜드가 없습니다.'
                : `총 ${totalSelected}건 선택 · 현재 페이지 ${visibleSelected}건`;
        }
        const selectPage = document.getElementById('brandSelectPage');
        if (selectPage) {
            const selectableCount = visibleBrandNos.size;
            selectPage.checked = selectableCount > 0 && visibleSelected === selectableCount;
        }
    },

    resetFilters() {
        document.getElementById('brandKeyword').value = '';
        document.getElementById('brandIsActiveFilter').value = '';
        document.getElementById('brandPageSize').value = '10';
        this.state.page = 0;
        this.getList();
    },

    applyStatFilter(type) {
        this.state.page = 0;
        document.getElementById('brandIsActiveFilter').value = '';
        switch (type) {
            case 'active':
                document.getElementById('brandIsActiveFilter').value = 'Y';
                break;
            case 'inactive':
                document.getElementById('brandIsActiveFilter').value = 'N';
                break;
            case 'total':
            default:
                break;
        }
        this.getList();
    },

    goPage(page) {
        this.state.page = page;
        this.getList();
    },

    async exportList() {
        if (this.exportInFlight) {
            return;
        }
        this._updateStateFromInputs();
        try {
            this.exportInFlight = true;
            CommonJS.setButtonDisabled(document.getElementById('btnExportBrand'), true, '내보내는 중입니다.');
            const params = this.buildParams();
            params.delete('page');
            params.delete('size');
            await CommonJS.downloadFile(`/api/admin/brands/export?${params.toString()}`, 'brands.csv');
        } catch (error) {
            await CommonJS.alert(error.message || '브랜드 CSV를 내보내지 못했습니다.', '오류', 'error');
        } finally {
            this.exportInFlight = false;
            CommonJS.setButtonDisabled(document.getElementById('btnExportBrand'), false);
        }
    },

    async openModal(brandNo) {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 브랜드 등록 및 수정이 불가능합니다.', '알림', 'warning');
            return;
        }
        document.getElementById('brandForm').reset();
        document.getElementById('brandNo').value = '';
        document.getElementById('brandModalTitle').innerText = '신규 브랜드 등록';

        if (brandNo) {
            try {
                const res = await fetch(`/api/admin/brands/get?no=${brandNo}`);
                if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '브랜드 정보를 불러오지 못했습니다.'));
                const data = await res.json();
                document.getElementById('brandNo').value = data.brandNo;
                document.getElementById('nameKo').value = data.nameKo;
                document.getElementById('nameEn').value = data.nameEn || '';
                document.getElementById('logoUrl').value = data.logoUrl || '';
                document.getElementById('isActive').value = data.isActive || 'Y';
                document.getElementById('brandModalTitle').innerText = '브랜드 정보 수정';
            } catch (err) {
                await CommonJS.alert(err.message || '브랜드 정보를 불러오지 못했습니다.', '오류', 'error');
                return;
            }
        }
        this.modal.show();
    },

    async saveBrand() {
        if (this.saveInFlight) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 브랜드 저장이 불가능합니다.', '알림', 'warning');
            return;
        }
        const brandNo = document.getElementById('brandNo').value;
        const nameKo = document.getElementById('nameKo').value;
        if (!nameKo) {
            await CommonJS.alert('브랜드명을 입력하세요.', '알림', 'warning');
            return;
        }

        const data = {
            brandNo: brandNo || null,
            nameKo: nameKo,
            nameEn: document.getElementById('nameEn').value,
            logoUrl: document.getElementById('logoUrl').value,
            isActive: document.getElementById('isActive').value
        };

        try {
            this.saveInFlight = true;
            CommonJS.setButtonDisabled(document.getElementById('btnSaveBrand'), true, '저장 중입니다.');
            const res = await fetch('/api/admin/brands/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '저장 중 오류가 발생했습니다.'));

            this.modal.hide();
            await this.getList();
            await CommonJS.alert('성공적으로 저장되었습니다.', '성공', 'success');
        } catch (err) {
            await CommonJS.alert(err.message || '저장 중 오류가 발생했습니다.', '오류', 'error');
        } finally {
            this.saveInFlight = false;
            this.applyOperationPolicy(this.operationPolicy);
        }
    },

    async deleteBrand(brandNo) {
        if (this.deleteInFlight.has(brandNo)) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 브랜드 삭제가 불가능합니다.', '알림', 'warning');
            return;
        }
        const confirm = await CommonJS.confirm('정말 삭제하시겠습니까?');
        if (!confirm) return;

        try {
            this.deleteInFlight.add(brandNo);
            const res = await fetch(`/api/admin/brands/delete?no=${brandNo}`, { method: 'DELETE' });
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '삭제 중 오류가 발생했습니다. (연관된 상품이 있을 수 있습니다)'));
            await this.getList();
            await CommonJS.alert('삭제되었습니다.', '성공', 'success');
        } catch (err) {
            await CommonJS.alert(err.message || '삭제 중 오류가 발생했습니다. (연관된 상품이 있을 수 있습니다)', '오류', 'error');
        } finally {
            this.deleteInFlight.delete(brandNo);
        }
    },

    async applyBulkOperation() {
        if (this.bulkInFlight) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 브랜드 상태 변경이 불가능합니다.', '알림', 'warning');
            return;
        }
        if (this.selectedBrandNos.size === 0) {
            await CommonJS.alert('일괄 적용할 브랜드를 선택하세요.', '알림', 'warning');
            return;
        }
        const isActive = document.getElementById('bulkBrandIsActive').value;
        if (!isActive) {
            await CommonJS.alert('변경할 상태를 선택하세요.', '알림', 'warning');
            return;
        }

        try {
            this.bulkInFlight = true;
            const res = await fetch('/api/admin/brands/bulk-operate', {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    brandNos: Array.from(this.selectedBrandNos),
                    isActive: isActive
                })
            });
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '브랜드 일괄 상태 변경에 실패했습니다.'));
            const result = await res.json();
            await this.getList();
            await CommonJS.alert(`요청 ${result.requestedCount}건 중 ${result.updatedCount}건 변경, ${result.unchangedCount}건 동일 상태입니다.`, '성공', 'success');
        } catch (err) {
            await CommonJS.alert(err.message || '브랜드 일괄 상태 변경에 실패했습니다.', '오류', 'error');
        } finally {
            this.bulkInFlight = false;
        }
    },

    async applyBulkDelete() {
        if (this.bulkInFlight) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 브랜드 삭제가 불가능합니다.', '알림', 'warning');
            return;
        }
        if (this.selectedBrandNos.size === 0) {
            await CommonJS.alert('일괄 삭제할 브랜드를 선택하세요.', '알림', 'warning');
            return;
        }
        const confirmed = await CommonJS.confirm(`선택한 브랜드 ${this.selectedBrandNos.size}건을 삭제하시겠습니까?`);
        if (!confirmed) {
            return;
        }

        try {
            this.bulkInFlight = true;
            const res = await fetch('/api/admin/brands/bulk-delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    brandNos: Array.from(this.selectedBrandNos)
                })
            });
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '브랜드 일괄 삭제에 실패했습니다.'));
            const result = await res.json();
            this.clearSelection();
            await this.getList();
            await CommonJS.alert(`요청 ${result.requestedCount}건 중 ${result.deletedCount}건 삭제, ${result.blockedCount}건 상품 연관으로 유지, ${result.missingCount}건 미존재입니다.`, '성공', 'success');
        } catch (err) {
            await CommonJS.alert(err.message || '브랜드 일괄 삭제에 실패했습니다.', '오류', 'error');
        } finally {
            this.bulkInFlight = false;
        }
    },

    _updateStateFromInputs() {
        this.state.keyword = CommonJS.normalizeOptionalText(document.getElementById('brandKeyword').value) || '';
        this.state.isActive = document.getElementById('brandIsActiveFilter').value || '';
        this.state.size = Number(document.getElementById('brandPageSize').value || 10);
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

document.addEventListener('DOMContentLoaded', () => BrandList.init());
