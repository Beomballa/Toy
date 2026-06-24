const CategoryList = {
    initialized: false,
    modal: null,
    operationPolicy: null,
    state: {
        page: 0,
        size: 10,
        keyword: '',
        isActive: '',
        source: '',
        returnTo: '',
        selectedParentNo: null,
        selectedParentName: '',
        depth1List: [],
        depth2List: []
    },
    saveInFlight: false,
    exportInFlight: false,
    bulkInFlight: false,
    selectedCategoryNos: new Set(),
    deleteInFlight: new Set(),

    init() {
        if (this.initialized) return;
        this.initialized = true;
        const modalEl = document.getElementById('categoryModal');
        if (modalEl) {
            this.modal = new bootstrap.Modal(modalEl);
        } else {
            console.error('카테고리 모달 엘리먼트를 찾을 수 없습니다.');
        }
        this.bindEvents();
        this.readStateFromUrl();
        this.applyOperationPolicy();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));
        this.getDepth1List();
    },

    async applyOperationPolicy(settings = null) {
        try {
            this.operationPolicy = settings || await CommonJS.fetchSystemSettings();
            const disabled = CommonJS.isAdminWriteBlocked(this.operationPolicy);
            const reason = '유지보수 모드에서는 카테고리 등록, 수정, 삭제가 불가능합니다.';
            CommonJS.setButtonDisabled(document.getElementById('btnNewRootCategory'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnNewSubCategory'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnSaveCategory'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnApplyCategoryBulk'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnBulkDeleteCategory'), disabled, reason);
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
    },

    bindEvents() {
        document.getElementById('btnNewRootCategory')?.addEventListener('click', () => this.openModal(1));
        document.getElementById('btnNewSubCategory')?.addEventListener('click', () => this.openModal(2));
        document.getElementById('btnExportCategory')?.addEventListener('click', () => this.exportList());
        document.getElementById('btnApplyCategoryBulk')?.addEventListener('click', () => this.applyBulkOperation());
        document.getElementById('btnBulkDeleteCategory')?.addEventListener('click', () => this.applyBulkDelete());
        document.getElementById('btnClearCategorySelection')?.addEventListener('click', () => this.clearSelection());
        document.getElementById('categorySelectSubPage')?.addEventListener('change', (event) => this.toggleSelectVisibleSubPage(event.target.checked));
        document.getElementById('btnSearchCategory')?.addEventListener('click', () => this.getDepth1List());
        document.getElementById('btnResetCategory')?.addEventListener('click', () => this.resetFilters());
        document.getElementById('categoryPageSize')?.addEventListener('change', () => {
            this.state.page = 0;
            this._updateStateFromInputs();
            this.getDepth1List();
        });
        document.getElementById('categoryKeyword')?.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                this.state.page = 0;
                this.getDepth1List();
            }
        });
        document.getElementById('btnSaveCategory')?.addEventListener('click', () => this.saveCategory());
        document.getElementById('depth1Body')?.addEventListener('click', (event) => {
            const checkbox = event.target.closest('[data-role="select-root-category"]');
            if (checkbox) {
                event.stopPropagation();
                this.toggleSelection(Number(checkbox.dataset.categoryNo), checkbox.checked);
                return;
            }

            const parentItem = event.target.closest('[data-role="select-parent"]');
            if (parentItem) {
                this.getDepth2List(Number(parentItem.dataset.parentNo), parentItem.dataset.parentName);
                return;
            }

            const editRootButton = event.target.closest('[data-role="edit-root-category"]');
            if (editRootButton) {
                event.stopPropagation();
                this.openModal(1, JSON.parse(editRootButton.dataset.category));
            }
        });
        document.getElementById('depth2ListBody')?.addEventListener('click', (event) => {
            const checkbox = event.target.closest('[data-role="select-sub-category"]');
            if (checkbox) {
                this.toggleSelection(Number(checkbox.dataset.categoryNo), checkbox.checked);
                return;
            }

            const editSubButton = event.target.closest('[data-role="edit-sub-category"]');
            if (editSubButton) {
                this.openModal(2, JSON.parse(editSubButton.dataset.category));
                return;
            }

            const deleteSubButton = event.target.closest('[data-role="delete-sub-category"]');
            if (deleteSubButton) {
                this.deleteCategory(Number(deleteSubButton.dataset.categoryNo));
            }
        });
        window.addEventListener('popstate', () => {
            this.readStateFromUrl();
            this.getDepth1List();
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
        this.state.selectedParentNo = Number(params.get('parentNo') || 0) || null;
        document.getElementById('categoryKeyword').value = this.state.keyword;
        document.getElementById('categoryIsActiveFilter').value = this.state.isActive;
        document.getElementById('categoryPageSize').value = String(this.state.size);
        CommonJS.bindMainLogoNavigation(this.state.returnTo || '/admin/categories');
        CommonJS.renderSourceContextNotice({ noticeId: 'categorySourceContextNotice', source: this.state.source });
    },

    buildParams() {
        const params = new URLSearchParams();
        params.set('page', String(this.state.page));
        params.set('size', String(this.state.size));
        params.set('depth', '1');
        if (this.state.keyword) params.set('keyword', this.state.keyword);
        if (this.state.isActive) params.set('isActive', this.state.isActive);
        if (this.state.selectedParentNo) params.set('parentNo', String(this.state.selectedParentNo));
        if (this.state.source) params.set('source', this.state.source);
        if (this.state.returnTo) params.set('returnTo', this.state.returnTo);
        return params;
    },

    async getDepth1List() {
        try {
            this._updateStateFromInputs();
            const params = this.buildParams();
            history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
            this.setFilterMeta('적용 필터를 계산하는 중입니다...');
            this.setResultMeta('결과 메타를 계산하는 중입니다...');
            this.setPageMeta('페이지 메타를 계산하는 중입니다...');
            this.setListStateMeta('loading', '카테고리 목록을 불러오는 중입니다.', 0, 0, '');
            const res = await fetch(`/api/admin/categories/list?${params.toString()}`);
            const data = await res.json();
            this.state.depth1List = data.items || [];
            this.renderDepth1();
            this.renderDepth1Meta(data);
            this.renderPagination(data);
            this.restoreSelectedParent();
        } catch (err) {
            console.error('1차 카테고리 로드 실패:', err);
            this.setFilterMeta('카테고리 목록을 불러오지 못했습니다.');
            this.setResultMeta('결과 메타 확인 불가');
            this.setPageMeta('페이지 메타 확인 불가');
            document.getElementById('categoryPagination').innerHTML = '';
            this.setListStateMeta('error', '카테고리 목록을 불러오지 못했습니다.', 0, 0, '');
        }
    },

    async getDepth2List(parentNo, parentName) {
        this.state.selectedParentNo = parentNo;
        this.state.selectedParentName = parentName;
        history.replaceState(null, '', `${window.location.pathname}?${this.buildParams().toString()}`);
        
        document.getElementById('parentCategoryName').innerText = `> ${parentName}`;
        const disabled = !!(this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy));
        CommonJS.setButtonDisabled(
            document.getElementById('btnNewSubCategory'),
            disabled,
            '유지보수 모드에서는 하위 카테고리를 추가할 수 없습니다.'
        );

        try {
            const res = await fetch(`/api/admin/categories/sub?parentNo=${parentNo}`);
            const data = await res.json();
            this.state.depth2List = data;
            this.renderDepth2();
            this.setSubCategoryMeta(`선택된 대분류 ${parentName} · 하위 카테고리 ${data.length}건`);
        } catch (err) {
            console.error('2차 카테고리 로드 실패:', err);
            this.setSubCategoryMeta('하위 카테고리 메타 확인 불가');
        }
    },

    renderDepth1() {
        const body = document.getElementById('depth1Body');
        if (!this.state.depth1List || this.state.depth1List.length === 0) {
            body.innerHTML = '<div class="text-center py-5 text-muted">등록된 카테고리가 없습니다.</div>';
            this.setListStateMeta('empty', '등록된 카테고리가 없습니다.', 0, 0, '');
            this.updateSelectionMeta();
            return;
        }

        body.innerHTML = this.state.depth1List.map(item => `
            <div class="category-item ${this.state.selectedParentNo === item.categoryNo ? 'active' : ''}" 
                 data-role="select-parent" data-parent-no="${item.categoryNo}" data-parent-name="${item.name.replace(/"/g, '&quot;')}">
                <div class="category-item__top">
                    <div class="category-item__title-wrap">
                        <div class="form-check mb-2">
                            <input class="form-check-input" type="checkbox" data-role="select-root-category" data-category-no="${item.categoryNo}" ${this.selectedCategoryNos.has(item.categoryNo) ? 'checked' : ''}>
                        </div>
                        <div class="category-item__eyebrow">ROOT CATEGORY</div>
                        <div class="category-item__title">${item.name}</div>
                    </div>
                    <button class="btn btn-xs btn-link category-item__edit" data-role="edit-root-category" data-category='${JSON.stringify(item).replace(/'/g, '&#39;')}' aria-label="${item.name} 수정">
                        <i class="fas fa-pen"></i>
                    </button>
                </div>
                <div class="category-item__bottom">
                    <div class="category-item__meta">
                        <span class="category-item__code">#${item.categoryNo}</span>
                        <span class="category-item__hint">선택 시 중분류 목록을 오른쪽에서 확인합니다.</span>
                    </div>
                    <span class="badge rounded-pill ${item.isActive === 'Y' ? 'badge-y' : 'badge-n'}">
                        ${item.isActive === 'Y' ? '사용중' : '중지'}
                    </span>
                </div>
            </div>
        `).join('');
        this.setListStateMeta('ready', '', this.state.depth1List.length, null, null);
        this.updateSelectionMeta();
    },

    renderDepth2() {
        const wrapper = document.getElementById('depth2TableWrapper');
        const emptyMsg = document.getElementById('depth2EmptyMessage');
        const tbody = document.getElementById('depth2ListBody');

        emptyMsg.classList.add('d-none');
        wrapper.classList.remove('d-none');

        if (!this.state.depth2List || this.state.depth2List.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center py-5 text-muted">하위 카테고리가 없습니다.</td></tr>';
            this.updateSelectionMeta();
            return;
        }

        tbody.innerHTML = this.state.depth2List.map(item => `
            <tr>
                <td class="ps-4">
                    <input type="checkbox" data-role="select-sub-category" data-category-no="${item.categoryNo}" ${this.selectedCategoryNos.has(item.categoryNo) ? 'checked' : ''}>
                </td>
                <td class="ps-4 text-muted small">${item.categoryNo}</td>
                <td class="fw-bold">${item.name}</td>
                <td class="text-center">
                    <span class="badge rounded-pill ${item.isActive === 'Y' ? 'badge-y' : 'badge-n'}">
                        ${item.isActive === 'Y' ? '사용중' : '중지'}
                    </span>
                </td>
                <td class="text-end pe-4">
                    <button class="btn btn-sm btn-outline-primary me-1" data-role="edit-sub-category" data-category='${JSON.stringify(item).replace(/'/g, '&#39;')}'>수정</button>
                    <button class="btn btn-sm btn-outline-danger" data-role="delete-sub-category" data-category-no="${item.categoryNo}">삭제</button>
                </td>
            </tr>
        `).join('');
        this.updateSelectionMeta();
    },

    renderDepth1Meta(data) {
        this.setFilterMeta(`필터 ${data.resultMeta?.appliedFilterCount ?? 0}개 · ${data.resultMeta?.querySignature || '대분류 기준'}`);
        this.setResultMeta(data.resultMeta?.resultLabel || `${this.state.depth1List.length}건`);
        this.setPageMeta(data.resultMeta?.pageInfoLabel || '페이지 메타 없음');
        this.setListStateMeta(
            'ready',
            '',
            this.state.depth1List.length,
            data.totalElements || 0,
            data.resultMeta?.querySignature || ''
        );
        const metaEl = document.getElementById('categoryListStateMeta');
        if (metaEl) {
            metaEl.dataset.pageInfoLabel = data.resultMeta?.pageInfoLabel || '';
        }
    },

    renderPagination(data) {
        const paginationEl = document.getElementById('categoryPagination');
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
                <button type="button" class="page-link" data-role="go-category-page" data-page="${index}">${index + 1}</button>
            </li>
        `).join('');

        paginationEl.querySelectorAll('[data-role="go-category-page"]').forEach((button) => {
            button.addEventListener('click', () => this.goPage(Number(button.dataset.page)));
        });
    },

    async exportList() {
        if (this.exportInFlight) {
            return;
        }
        this._updateStateFromInputs();
        try {
            this.exportInFlight = true;
            CommonJS.setButtonDisabled(document.getElementById('btnExportCategory'), true, '내보내는 중입니다.');
            const params = this.buildParams();
            params.delete('page');
            params.delete('size');
            await CommonJS.downloadFile(`/api/admin/categories/export?${params.toString()}`, 'categories.csv');
        } catch (error) {
            await CommonJS.alert(error.message || '카테고리 CSV를 내보내지 못했습니다.', '오류', 'error');
        } finally {
            this.exportInFlight = false;
            CommonJS.setButtonDisabled(document.getElementById('btnExportCategory'), false);
        }
    },

    setFilterMeta(message) {
        document.getElementById('categoryFilterMeta').textContent = message;
    },

    setResultMeta(message) {
        document.getElementById('categoryResultMeta').textContent = message;
    },

    setPageMeta(message) {
        document.getElementById('categoryPageMeta').textContent = message;
    },

    setListStateMeta(state, message, visibleCount, totalElements, querySignature) {
        const metaEl = document.getElementById('categoryListStateMeta');
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

    toggleSelection(categoryNo, checked) {
        if (!Number.isFinite(categoryNo) || categoryNo <= 0) {
            return;
        }
        if (checked) {
            this.selectedCategoryNos.add(categoryNo);
        } else {
            this.selectedCategoryNos.delete(categoryNo);
        }
        this.updateSelectionMeta();
    },

    toggleSelectVisibleSubPage(checked) {
        document.querySelectorAll('[data-role="select-sub-category"]').forEach((checkbox) => {
            checkbox.checked = checked;
            const categoryNo = Number(checkbox.dataset.categoryNo);
            if (checked) {
                this.selectedCategoryNos.add(categoryNo);
            } else {
                this.selectedCategoryNos.delete(categoryNo);
            }
        });
        this.updateSelectionMeta();
    },

    clearSelection() {
        this.selectedCategoryNos.clear();
        const subPageCheckbox = document.getElementById('categorySelectSubPage');
        if (subPageCheckbox) {
            subPageCheckbox.checked = false;
        }
        document.querySelectorAll('[data-role="select-root-category"], [data-role="select-sub-category"]').forEach((checkbox) => {
            checkbox.checked = false;
        });
        this.updateSelectionMeta();
    },

    updateSelectionMeta() {
        const totalSelected = this.selectedCategoryNos.size;
        const visibleSubNos = Array.from(document.querySelectorAll('[data-role="select-sub-category"]'))
                .map((checkbox) => Number(checkbox.dataset.categoryNo))
                .filter((categoryNo) => Number.isFinite(categoryNo));
        const visibleSubNoSet = new Set(visibleSubNos);
        const visibleSubSelected = Array.from(this.selectedCategoryNos).filter((categoryNo) => visibleSubNoSet.has(categoryNo)).length;
        const metaEl = document.getElementById('categorySelectionMeta');
        if (metaEl) {
            metaEl.textContent = totalSelected === 0
                    ? '선택된 카테고리가 없습니다.'
                    : `총 ${totalSelected}건 선택 · 현재 중분류 목록 ${visibleSubSelected}건`;
        }
        const subPageCheckbox = document.getElementById('categorySelectSubPage');
        if (subPageCheckbox) {
            subPageCheckbox.checked = visibleSubNoSet.size > 0 && visibleSubSelected === visibleSubNoSet.size;
        }
    },

    setSubCategoryMeta(message) {
        document.getElementById('subCategoryMetaText').textContent = message;
    },

    resetFilters() {
        document.getElementById('categoryKeyword').value = '';
        document.getElementById('categoryIsActiveFilter').value = '';
        document.getElementById('categoryPageSize').value = '10';
        this.state.page = 0;
        this.state.selectedParentNo = null;
        this.state.selectedParentName = '';
        this.state.depth2List = [];
        this.renderDepth2Empty();
        this.getDepth1List();
    },

    goPage(page) {
        this.state.page = page;
        this.getDepth1List();
    },

    async openModal(depth, item) {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 카테고리 등록 및 수정이 불가능합니다.', '알림', 'warning');
            return;
        }
        document.getElementById('categoryForm').reset();
        document.getElementById('categoryNo').value = '';
        document.getElementById('depth').value = depth;
        document.getElementById('parentNo').value = depth === 1 ? '0' : this.state.selectedParentNo;

        const parentWrapper = document.getElementById('parentNameWrapper');
        if (depth === 2) {
            parentWrapper.style.display = 'block';
            document.getElementById('parentDisplay').innerText = this.state.selectedParentName;
        } else {
            parentWrapper.style.display = 'none';
        }

        if (item) {
            document.getElementById('categoryNo').value = item.categoryNo;
            document.getElementById('categoryName').value = item.name;
            document.getElementById('isCategoryActive').value = item.isActive;
            document.getElementById('categoryModalTitle').innerText = '카테고리 수정';
        } else {
            document.getElementById('categoryModalTitle').innerText = depth === 1 ? '대분류 등록' : '중분류 등록';
        }

        this.modal.show();
    },

    async saveCategory() {
        if (this.saveInFlight) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 카테고리 저장이 불가능합니다.', '알림', 'warning');
            return;
        }
        const name = document.getElementById('categoryName').value;
        if (!name) {
            await CommonJS.alert('카테고리명을 입력하세요.', '알림', 'warning');
            return;
        }

        const data = {
            categoryNo: document.getElementById('categoryNo').value || null,
            parentNo: document.getElementById('parentNo').value,
            name: name,
            depth: document.getElementById('depth').value,
            isActive: document.getElementById('isCategoryActive').value
        };

        try {
            this.saveInFlight = true;
            CommonJS.setButtonDisabled(document.getElementById('btnSaveCategory'), true, '저장 중입니다.');
            const res = await fetch('/api/admin/categories/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '저장 중 오류가 발생했습니다.'));

            this.modal.hide();
            await this.getDepth1List();
            if (Number(data.depth) === 2 && this.state.selectedParentNo) {
                await this.getDepth2List(this.state.selectedParentNo, this.state.selectedParentName);
            }
            if (Number(data.depth) === 1 && this.state.selectedParentNo === Number(data.categoryNo)) {
                await this.getDepth2List(this.state.selectedParentNo, this.state.selectedParentName);
            }
            await CommonJS.alert('성공적으로 저장되었습니다.', '성공', 'success');
        } catch (err) {
            await CommonJS.alert(err.message || '저장 중 오류가 발생했습니다.', '오류', 'error');
        } finally {
            this.saveInFlight = false;
            this.applyOperationPolicy(this.operationPolicy);
        }
    },

    async deleteCategory(no) {
        if (this.deleteInFlight.has(no)) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 카테고리 삭제가 불가능합니다.', '알림', 'warning');
            return;
        }
        const confirm = await CommonJS.confirm('정말 삭제하시겠습니까? (하위 항목이 있는 경우 삭제되지 않을 수 있습니다)');
        if (!confirm) return;

        try {
            this.deleteInFlight.add(no);
            const res = await fetch(`/api/admin/categories/delete?no=${no}`, { method: 'DELETE' });
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '삭제 중 오류가 발생했습니다.'));
            await this.getDepth1List();
            if (this.state.selectedParentNo) {
                await this.getDepth2List(this.state.selectedParentNo, this.state.selectedParentName);
            }
            await CommonJS.alert('삭제되었습니다.', '성공', 'success');
        } catch (err) {
            await CommonJS.alert(err.message || '삭제 중 오류가 발생했습니다.', '오류', 'error');
        } finally {
            this.deleteInFlight.delete(no);
        }
    },

    async applyBulkOperation() {
        if (this.bulkInFlight) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 카테고리 상태 변경이 불가능합니다.', '알림', 'warning');
            return;
        }
        if (this.selectedCategoryNos.size === 0) {
            await CommonJS.alert('일괄 적용할 카테고리를 선택하세요.', '알림', 'warning');
            return;
        }
        const isActive = document.getElementById('bulkCategoryIsActive').value;
        if (!isActive) {
            await CommonJS.alert('변경할 상태를 선택하세요.', '알림', 'warning');
            return;
        }

        try {
            this.bulkInFlight = true;
            const res = await fetch('/api/admin/categories/bulk-operate', {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    categoryNos: Array.from(this.selectedCategoryNos),
                    isActive: isActive
                })
            });
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '카테고리 일괄 상태 변경에 실패했습니다.'));
            const result = await res.json();
            await this.getDepth1List();
            if (this.state.selectedParentNo) {
                await this.getDepth2List(this.state.selectedParentNo, this.state.selectedParentName);
            }
            await CommonJS.alert(`요청 ${result.requestedCount}건 중 ${result.updatedCount}건 변경, ${result.unchangedCount}건 동일 상태입니다.`, '성공', 'success');
        } catch (err) {
            await CommonJS.alert(err.message || '카테고리 일괄 상태 변경에 실패했습니다.', '오류', 'error');
        } finally {
            this.bulkInFlight = false;
        }
    },

    async applyBulkDelete() {
        if (this.bulkInFlight) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 카테고리 삭제가 불가능합니다.', '알림', 'warning');
            return;
        }
        if (this.selectedCategoryNos.size === 0) {
            await CommonJS.alert('일괄 삭제할 카테고리를 선택하세요.', '알림', 'warning');
            return;
        }
        const confirmed = await CommonJS.confirm(`선택한 카테고리 ${this.selectedCategoryNos.size}건을 삭제하시겠습니까?`);
        if (!confirmed) {
            return;
        }

        try {
            this.bulkInFlight = true;
            const res = await fetch('/api/admin/categories/bulk-delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    categoryNos: Array.from(this.selectedCategoryNos)
                })
            });
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '카테고리 일괄 삭제에 실패했습니다.'));
            const result = await res.json();
            this.clearSelection();
            await this.getDepth1List();
            if (this.state.selectedParentNo) {
                await this.getDepth2List(this.state.selectedParentNo, this.state.selectedParentName);
            }
            await CommonJS.alert(`요청 ${result.requestedCount}건 중 ${result.deletedCount}건 삭제, ${result.blockedCount}건 하위/상품 연관으로 유지, ${result.missingCount}건 미존재입니다.`, '성공', 'success');
        } catch (err) {
            await CommonJS.alert(err.message || '카테고리 일괄 삭제에 실패했습니다.', '오류', 'error');
        } finally {
            this.bulkInFlight = false;
        }
    },

    _updateStateFromInputs() {
        this.state.keyword = CommonJS.normalizeOptionalText(document.getElementById('categoryKeyword').value) || '';
        this.state.isActive = document.getElementById('categoryIsActiveFilter').value || '';
        this.state.size = Number(document.getElementById('categoryPageSize').value || 10);
    },

    restoreSelectedParent() {
        if (!this.state.selectedParentNo) {
            this.renderDepth2Empty();
            return;
        }
        const matchedParent = this.state.depth1List.find((item) => item.categoryNo === this.state.selectedParentNo);
        if (!matchedParent) {
            this.state.selectedParentNo = null;
            this.state.selectedParentName = '';
            this.renderDepth2Empty();
            history.replaceState(null, '', `${window.location.pathname}?${this.buildParams().toString()}`);
            return;
        }
        this.getDepth2List(matchedParent.categoryNo, matchedParent.name);
    },

    renderDepth2Empty() {
        const wrapper = document.getElementById('depth2TableWrapper');
        const emptyMsg = document.getElementById('depth2EmptyMessage');
        const tbody = document.getElementById('depth2ListBody');
        const parentName = document.getElementById('parentCategoryName');
        if (wrapper) {
            wrapper.classList.add('d-none');
        }
        if (emptyMsg) {
            emptyMsg.classList.remove('d-none');
        }
        if (tbody) {
            tbody.innerHTML = '';
        }
        if (parentName) {
            parentName.innerText = '> 대분류를 선택하세요';
        }
        this.setSubCategoryMeta('선택된 대분류가 없습니다.');
    }
};

document.addEventListener('DOMContentLoaded', () => CategoryList.init());
