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
    depth1RequestId: 0,
    depth2RequestId: 0,
    depth1ItemsByNo: new Map(),
    depth2ItemsByNo: new Map(),
    selectedCategoryNos: new Set(),
    toggleInFlight: new Set(),
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
            document.querySelectorAll('[data-role="toggle-category-active"]').forEach((button) => {
                CommonJS.setButtonDisabled(button, disabled, reason);
            });
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
                const categoryNo = this.normalizeOptionalPositiveNumber(checkbox.dataset.categoryNo);
                if (categoryNo == null) {
                    return;
                }
                this.toggleSelection(categoryNo, checkbox.checked);
                return;
            }

            const editRootButton = event.target.closest('[data-role="edit-root-category"]');
            if (editRootButton) {
                event.stopPropagation();
                const categoryNo = this.normalizeOptionalPositiveNumber(editRootButton.dataset.categoryNo);
                const category = categoryNo == null ? null : this.depth1ItemsByNo.get(categoryNo);
                if (!category) {
                    void CommonJS.alert('수정할 카테고리 정보를 읽을 수 없습니다.', '알림', 'warning');
                    return;
                }
                this.openModal(1, category);
                return;
            }

            const toggleRootButton = event.target.closest('[data-role="toggle-category-active"]');
            if (toggleRootButton) {
                event.stopPropagation();
                const categoryNo = this.normalizeOptionalPositiveNumber(toggleRootButton.dataset.categoryNo);
                const nextActive = this.normalizeYnFilterValue(toggleRootButton.dataset.nextActive);
                if (categoryNo == null || !nextActive) {
                    void CommonJS.alert('변경할 카테고리 상태 정보가 올바르지 않습니다.', '알림', 'warning');
                    return;
                }
                this.toggleActive(categoryNo, nextActive);
                return;
            }

            const parentItem = event.target.closest('[data-role="select-parent"]');
            if (parentItem) {
                const parentNo = this.normalizeOptionalPositiveNumber(parentItem.dataset.parentNo);
                const parent = parentNo == null ? null : this.depth1ItemsByNo.get(parentNo);
                if (!parent) {
                    void CommonJS.alert('유효하지 않은 상위 카테고리 번호입니다.', '알림', 'warning');
                    return;
                }
                this.getDepth2List(parentNo, parent.name);
            }
        });
        document.getElementById('depth2ListBody')?.addEventListener('click', (event) => {
            const checkbox = event.target.closest('[data-role="select-sub-category"]');
            if (checkbox) {
                const categoryNo = this.normalizeOptionalPositiveNumber(checkbox.dataset.categoryNo);
                if (categoryNo == null) {
                    return;
                }
                this.toggleSelection(categoryNo, checkbox.checked);
                return;
            }

            const editSubButton = event.target.closest('[data-role="edit-sub-category"]');
            if (editSubButton) {
                const categoryNo = this.normalizeOptionalPositiveNumber(editSubButton.dataset.categoryNo);
                const category = categoryNo == null ? null : this.depth2ItemsByNo.get(categoryNo);
                if (!category) {
                    void CommonJS.alert('수정할 카테고리 정보를 읽을 수 없습니다.', '알림', 'warning');
                    return;
                }
                this.openModal(2, category);
                return;
            }

            const deleteSubButton = event.target.closest('[data-role="delete-sub-category"]');
            if (deleteSubButton) {
                const categoryNo = this.normalizeOptionalPositiveNumber(deleteSubButton.dataset.categoryNo);
                if (categoryNo == null) {
                    void CommonJS.alert('유효하지 않은 카테고리 번호입니다.', '알림', 'warning');
                    return;
                }
                this.deleteCategory(categoryNo);
                return;
            }

            const toggleSubButton = event.target.closest('[data-role="toggle-category-active"]');
            if (toggleSubButton) {
                const categoryNo = this.normalizeOptionalPositiveNumber(toggleSubButton.dataset.categoryNo);
                const nextActive = this.normalizeYnFilterValue(toggleSubButton.dataset.nextActive);
                if (categoryNo == null || !nextActive) {
                    void CommonJS.alert('변경할 카테고리 상태 정보가 올바르지 않습니다.', '알림', 'warning');
                    return;
                }
                this.toggleActive(categoryNo, nextActive);
            }
        });
        window.addEventListener('popstate', () => {
            this.readStateFromUrl();
            this.getDepth1List();
        });
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        this.state.page = this.normalizePage(params.get('page'));
        this.state.size = this.normalizePageSize(params.get('size'));
        this.state.keyword = CommonJS.normalizeOptionalText(params.get('keyword')) || '';
        this.state.isActive = this.normalizeYnFilterValue(params.get('isActive'));
        this.state.source = params.get('source') || '';
        this.state.returnTo = CommonJS.normalizeAdminReturnPath(params.get('returnTo'), '');
        this.state.selectedParentNo = this.normalizeOptionalPositiveNumber(params.get('parentNo'));
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
        if (this.state.isActive && ['Y', 'N'].includes(this.state.isActive)) params.set('isActive', this.state.isActive);
        if (this.state.selectedParentNo) params.set('parentNo', String(this.state.selectedParentNo));
        if (this.state.source) params.set('source', this.state.source);
        if (this.state.returnTo) params.set('returnTo', this.state.returnTo);
        return params;
    },

    async getDepth1List() {
        const requestId = ++this.depth1RequestId;
        this.depth2RequestId++;
        try {
            this._updateStateFromInputs();
            if (!this.validateState()) {
                return;
            }
            const params = this.buildParams();
            history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
            this.setMetaText('카테고리 목록을 불러오는 중입니다...');
            this.setFilterMeta('적용 필터를 계산하는 중입니다...');
            this.setResultMeta('결과 메타를 계산하는 중입니다...');
            this.setPageMeta('페이지 메타를 계산하는 중입니다...');
            this.setListStateMeta('loading', '카테고리 목록을 불러오는 중입니다.', 0, 0, '');
            const res = await fetch(`/api/admin/categories/list?${params.toString()}`);
            if (!res.ok) {
                throw new Error(await CommonJS.extractErrorMessage(res, '카테고리 목록을 불러오지 못했습니다.'));
            }
            const data = await res.json();
            if (requestId !== this.depth1RequestId) {
                return;
            }
            this.state.depth1List = data.items || [];
            this.depth1ItemsByNo = this.buildCategoryMap(this.state.depth1List);
            this.renderDepth1();
            this.renderDepth1Meta(data);
            this.renderPagination(data);
            this.restoreSelectedParent();
        } catch (err) {
            if (requestId !== this.depth1RequestId) {
                return;
            }
            console.error('1차 카테고리 로드 실패:', err);
            this.setMetaText('카테고리 목록을 불러오지 못했습니다.');
            this.setFilterMeta('카테고리 목록을 불러오지 못했습니다.');
            this.setResultMeta('결과 메타 확인 불가');
            this.setPageMeta('페이지 메타 확인 불가');
            document.getElementById('depth1Body').innerHTML = `
                <div class="product-empty-state py-5">
                    <div class="product-empty-state__icon text-danger">
                        <i class="fas fa-triangle-exclamation"></i>
                    </div>
                    <strong>카테고리 목록을 불러오지 못했습니다.</strong>
                    <p>${this.escapeHtml(err.message || '잠시 후 다시 시도해 주세요.')}</p>
                </div>
            `;
            document.getElementById('categoryPagination').innerHTML = '';
            this.setListStateMeta('error', '카테고리 목록을 불러오지 못했습니다.', 0, 0, '');
        }
    },

    async getDepth2List(parentNo, parentName) {
        if (!this.isValidCategoryNo(parentNo)) {
            await CommonJS.alert('유효하지 않은 상위 카테고리 번호입니다.', '알림', 'warning');
            return;
        }
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

        const requestId = ++this.depth2RequestId;
        try {
            const res = await fetch(`/api/admin/categories/sub?parentNo=${parentNo}`);
            if (!res.ok) {
                throw new Error(await CommonJS.extractErrorMessage(res, '하위 카테고리를 불러오지 못했습니다.'));
            }
            const data = await res.json();
            if (requestId !== this.depth2RequestId || this.state.selectedParentNo !== parentNo) {
                return;
            }
            this.state.depth2List = Array.isArray(data) ? data : [];
            this.depth2ItemsByNo = this.buildCategoryMap(this.state.depth2List);
            this.renderDepth2();
            this.setSubCategoryMeta(`선택된 대분류 ${parentName} · 하위 카테고리 ${this.state.depth2List.length}건`);
        } catch (err) {
            if (requestId !== this.depth2RequestId || this.state.selectedParentNo !== parentNo) {
                return;
            }
            console.error('2차 카테고리 로드 실패:', err);
            this.state.depth2List = [];
            this.depth2ItemsByNo.clear();
            const tbody = document.getElementById('depth2ListBody');
            if (tbody) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="5" class="py-5">
                            <div class="product-empty-state">
                                <div class="product-empty-state__icon text-danger">
                                    <i class="fas fa-triangle-exclamation"></i>
                                </div>
                                <strong>하위 카테고리를 불러오지 못했습니다.</strong>
                                <p>${this.escapeHtml(err.message || '잠시 후 다시 시도해 주세요.')}</p>
                            </div>
                        </td>
                    </tr>
                `;
            }
            this.setSubCategoryMeta('하위 카테고리 메타 확인 불가');
        }
    },

    renderDepth1() {
        const body = document.getElementById('depth1Body');
        if (!this.state.depth1List || this.state.depth1List.length === 0) {
            body.innerHTML = `
                <div class="product-empty-state py-5">
                    <div class="product-empty-state__icon">
                        <i class="fas fa-folder-open"></i>
                    </div>
                    <strong>등록된 카테고리가 없습니다.</strong>
                    <p>현재 조건에 맞는 대분류가 없어 새 카테고리를 바로 추가할 수 있습니다.</p>
                </div>`;
            this.setListStateMeta('empty', '등록된 카테고리가 없습니다.', 0, 0, '');
            this.updateSelectionMeta();
            return;
        }

        body.innerHTML = this.state.depth1List.map(item => {
            const categoryNo = this.normalizeOptionalPositiveNumber(item.categoryNo);
            const name = this.escapeHtml(item.name || '-');
            const isActive = item.isActive === 'Y';
            return `
            <div class="category-item ${this.state.selectedParentNo === categoryNo ? 'active' : ''}"
                 data-role="select-parent" data-parent-no="${categoryNo || ''}">
                <div class="category-item__top">
                    <div class="category-item__title-wrap">
                        <div class="form-check mb-2">
                            <input class="form-check-input" type="checkbox" data-role="select-root-category" data-category-no="${categoryNo || ''}" ${this.selectedCategoryNos.has(categoryNo) ? 'checked' : ''}>
                        </div>
                        <div class="category-item__eyebrow">ROOT CATEGORY</div>
                        <div class="category-item__title">${name}</div>
                    </div>
                    <button class="btn btn-xs btn-link category-item__edit" data-role="edit-root-category" data-category-no="${categoryNo || ''}" aria-label="${name} 수정">
                        <i class="fas fa-pen"></i>
                    </button>
                </div>
                <div class="category-item__bottom">
                    <div class="category-item__meta">
                        <span class="category-item__code">#${categoryNo || '-'}</span>
                        <span class="category-item__hint">선택 시 중분류 목록을 오른쪽에서 확인합니다.</span>
                    </div>
                    <div class="d-flex align-items-center gap-2">
                        <span class="badge rounded-pill ${isActive ? 'badge-y' : 'badge-n'}">
                            ${isActive ? '사용중' : '중지'}
                        </span>
                        <button class="btn btn-sm btn-outline-dark"
                                data-role="toggle-category-active"
                                data-category-no="${categoryNo || ''}"
                                data-next-active="${isActive ? 'N' : 'Y'}">${isActive ? '중지' : '활성'}</button>
                    </div>
                </div>
            </div>
        `;
        }).join('');
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
            tbody.innerHTML = `
                <tr>
                    <td colspan="5" class="py-5">
                        <div class="product-empty-state">
                            <div class="product-empty-state__icon">
                                <i class="fas fa-folder-tree"></i>
                            </div>
                            <strong>하위 카테고리가 없습니다.</strong>
                            <p>선택한 대분류 아래에 아직 등록된 중분류가 없어 바로 추가할 수 있습니다.</p>
                        </div>
                    </td>
                </tr>`;
            this.updateSelectionMeta();
            return;
        }

        tbody.innerHTML = this.state.depth2List.map(item => {
            const categoryNo = this.normalizeOptionalPositiveNumber(item.categoryNo);
            const name = this.escapeHtml(item.name || '-');
            const isActive = item.isActive === 'Y';
            return `
            <tr>
                <td class="ps-4">
                    <input type="checkbox" data-role="select-sub-category" data-category-no="${categoryNo || ''}" ${this.selectedCategoryNos.has(categoryNo) ? 'checked' : ''}>
                </td>
                <td class="ps-4 text-muted small">${categoryNo || '-'}</td>
                <td class="fw-bold">${name}</td>
                <td class="text-center">
                    <span class="badge rounded-pill ${isActive ? 'badge-y' : 'badge-n'}">
                        ${isActive ? '사용중' : '중지'}
                    </span>
                </td>
                <td class="text-end pe-4">
                    <button class="btn btn-sm btn-outline-primary me-1" data-role="edit-sub-category" data-category-no="${categoryNo || ''}">수정</button>
                    <button class="btn btn-sm btn-outline-dark me-1"
                            data-role="toggle-category-active"
                            data-category-no="${categoryNo || ''}"
                            data-next-active="${isActive ? 'N' : 'Y'}">${isActive ? '중지' : '활성'}</button>
                    <button class="btn btn-sm btn-outline-danger" data-role="delete-sub-category" data-category-no="${categoryNo || ''}">삭제</button>
                </td>
            </tr>
        `;
        }).join('');
        this.updateSelectionMeta();
    },

    renderDepth1Meta(data) {
        this.setMetaText(data.resultMeta?.resultLabel || `${this.state.depth1List.length}건`);
        this.setFilterMeta(`필터 ${data.resultMeta?.appliedFilterCount ?? 0}개`);
        this.setResultMeta(data.resultMeta?.querySignature || '대분류 기준');
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
            button.addEventListener('click', () => this.goPage(this.normalizePage(button.dataset.page)));
        });
    },

    async exportList() {
        if (this.exportInFlight) {
            return;
        }
        this._updateStateFromInputs();
        if (!this.validateState()) {
            return;
        }
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

    setMetaText(message) {
        document.getElementById('categoryMetaText').textContent = message;
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
            const categoryNo = this.normalizeOptionalPositiveNumber(checkbox.dataset.categoryNo);
            if (categoryNo == null) {
                checkbox.checked = false;
                return;
            }
            checkbox.checked = checked;
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
                .map((checkbox) => this.normalizeOptionalPositiveNumber(checkbox.dataset.categoryNo))
                .filter((categoryNo) => categoryNo != null);
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
        if (!Number.isInteger(page) || page < 0) {
            void CommonJS.alert('이동할 페이지 정보가 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        this.state.page = page;
        this.getDepth1List();
    },

    async openModal(depth, item) {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 카테고리 등록 및 수정이 불가능합니다.', '알림', 'warning');
            return;
        }
        if (!this.isValidCategoryDepth(depth)) {
            await CommonJS.alert('유효하지 않은 카테고리 깊이입니다.', '알림', 'warning');
            return;
        }
        if (depth === 2 && !item && !this.isValidCategoryNo(this.state.selectedParentNo)) {
            await CommonJS.alert('중분류를 추가할 대분류를 먼저 선택하세요.', '알림', 'warning');
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
        const categoryNo = document.getElementById('categoryNo').value;
        const parentNo = document.getElementById('parentNo').value;
        const depth = Number(document.getElementById('depth').value);
        const name = CommonJS.normalizeOptionalText(document.getElementById('categoryName').value);
        if (!name) {
            await CommonJS.alert('카테고리명을 입력하세요.', '알림', 'warning');
            return;
        }
        if (categoryNo && !this.isValidCategoryNo(Number(categoryNo))) {
            await CommonJS.alert('유효하지 않은 카테고리 번호입니다.', '알림', 'warning');
            return;
        }
        if (!this.isValidCategoryDepth(depth)) {
            await CommonJS.alert('유효하지 않은 카테고리 깊이입니다.', '알림', 'warning');
            return;
        }
        if (depth === 2 && !this.isValidCategoryNo(Number(parentNo))) {
            await CommonJS.alert('중분류는 유효한 상위 카테고리가 필요합니다.', '알림', 'warning');
            return;
        }

        const data = {
            categoryNo: categoryNo ? Number(categoryNo) : null,
            parentNo: parentNo ? Number(parentNo) : null,
            name: name,
            depth: depth,
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
        if (!this.isValidCategoryNo(no)) {
            await CommonJS.alert('유효하지 않은 카테고리 번호입니다.', '알림', 'warning');
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

    async toggleActive(no, isActive) {
        if (this.toggleInFlight.has(no)) {
            return;
        }
        if (!this.isValidCategoryNo(no)) {
            await CommonJS.alert('유효하지 않은 카테고리 번호입니다.', '알림', 'warning');
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 카테고리 상태 변경이 불가능합니다.', '알림', 'warning');
            return;
        }

        try {
            this.toggleInFlight.add(no);
            const res = await fetch(`/api/admin/categories/active/${no}`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ isActive })
            });
            if (!res.ok) {
                throw new Error(await CommonJS.extractErrorMessage(res, '카테고리 상태 변경에 실패했습니다.'));
            }
            await this.getDepth1List();
            await CommonJS.alert(`카테고리 상태를 ${isActive === 'Y' ? '사용' : '중지'}로 변경했습니다.`, '성공', 'success');
        } catch (err) {
            await CommonJS.alert(err.message || '카테고리 상태 변경에 실패했습니다.', '오류', 'error');
        } finally {
            this.toggleInFlight.delete(no);
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
        this.state.isActive = this.normalizeYnFilterValue(document.getElementById('categoryIsActiveFilter').value);
        this.state.size = this.normalizePageSize(document.getElementById('categoryPageSize').value);
    },

    restoreSelectedParent() {
        if (!this.state.selectedParentNo) {
            this.renderDepth2Empty();
            return;
        }
        const matchedParent = this.depth1ItemsByNo.get(this.state.selectedParentNo);
        if (!matchedParent) {
            this.state.selectedParentNo = null;
            this.state.selectedParentName = '';
            this.renderDepth2Empty();
            history.replaceState(null, '', `${window.location.pathname}?${this.buildParams().toString()}`);
            return;
        }
        this.getDepth2List(matchedParent.categoryNo, matchedParent.name);
    },

    isValidCategoryNo(categoryNo) {
        return Number.isInteger(Number(categoryNo)) && Number(categoryNo) > 0;
    },

    isValidCategoryDepth(depth) {
        return Number(depth) === 1 || Number(depth) === 2;
    },

    normalizeYnFilterValue(value) {
        return ['Y', 'N'].includes(value) ? value : '';
    },

    validateState() {
        if (this.state.keyword && this.state.keyword.length > 100) {
            void CommonJS.alert('검색어는 100자 이하로 입력하세요.', '알림', 'warning');
            return false;
        }
        if (this.state.isActive && !this.normalizeYnFilterValue(this.state.isActive)) {
            void CommonJS.alert('활성 상태 필터 값이 올바르지 않습니다.', '알림', 'warning');
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
    },

    normalizeOptionalPositiveNumber(value) {
        const parsed = Number(value);
        return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
    },

    buildCategoryMap(items) {
        return new Map(
            (items || [])
                .map((item) => [this.normalizeOptionalPositiveNumber(item.categoryNo), item])
                .filter(([categoryNo]) => categoryNo != null)
        );
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
        this.depth2ItemsByNo.clear();
        if (parentName) {
            parentName.innerText = '> 대분류를 선택하세요';
        }
        this.setSubCategoryMeta('선택된 대분류가 없습니다.');
    }
};

document.addEventListener('DOMContentLoaded', () => CategoryList.init());
