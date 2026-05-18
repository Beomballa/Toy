const BannerList = {
    initialized: false,
    modal: null,
    state: {
        page: 0,
        size: 10,
        keyword: '',
        isActive: '',
    },
    operationPolicy: null,

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
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
    },

    bindEvents() {
        document.getElementById('btnNewBanner')?.addEventListener('click', () => {
            this.openModal();
        });

        document.getElementById('btnSaveBanner')?.addEventListener('click', () => {
            this.saveBanner();
        });

        document.getElementById('btnSearchBanner')?.addEventListener('click', () => this.getList());
        document.getElementById('btnResetBanner')?.addEventListener('click', () => this.resetFilters());
        document.getElementById('bannerPageSize')?.addEventListener('change', () => {
            this.state.page = 0;
            this._updateStateFromInputs();
            this.getList();
        });
        document.getElementById('bannerListBody')?.addEventListener('click', (event) => {
            const editButton = event.target.closest('[data-role="edit-banner"]');
            if (editButton) {
                this.openEditModal(JSON.parse(editButton.dataset.banner));
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
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        this.state.page = Number(params.get('page') || 0);
        this.state.size = Number(params.get('size') || 10);
        this.state.keyword = params.get('keyword') || '';
        this.state.isActive = params.get('isActive') || '';
        document.getElementById('bannerKeyword').value = this.state.keyword;
        document.getElementById('bannerIsActiveFilter').value = this.state.isActive;
        document.getElementById('bannerPageSize').value = String(this.state.size);
    },

    buildParams() {
        const params = new URLSearchParams();
        params.set('page', String(this.state.page));
        params.set('size', String(this.state.size));
        if (this.state.keyword) params.set('keyword', this.state.keyword);
        if (this.state.isActive) params.set('isActive', this.state.isActive);
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
            this.setListStateMeta('loading', '배너 목록을 불러오는 중입니다.', 0, 0, '');
            const res = await fetch(`/api/admin/banners/list?${params.toString()}`);
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '배너 목록을 불러오지 못했습니다.'));
            const data = await res.json();
            this.renderList(data.items || []);
            this.renderMeta(data);
            this.renderPagination(data);
        } catch (err) {
            document.getElementById('bannerMetaText').textContent = err.message;
            this.setFilterMeta(err.message);
            this.setResultMeta('결과 메타 확인 불가');
            this.setPageMeta('페이지 메타 확인 불가');
            document.getElementById('bannerListBody').innerHTML =
                `<tr><td colspan="6" class="text-center py-5 text-danger">${err.message}</td></tr>`;
            document.getElementById('bannerPagination').innerHTML = '';
            this.setListStateMeta('error', err.message, 0, 0, '');
        }
    },

    renderList(items) {
        const tbody = document.getElementById('bannerListBody');
        if (!tbody) return;

        if (!items || items.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center py-5 text-muted">등록된 배너가 없습니다.</td></tr>';
            this.setListStateMeta('empty', '등록된 배너가 없습니다.', 0, 0, '');
            return;
        }

        tbody.innerHTML = items.map(item => `
            <tr>
                <td class="ps-4 text-center fw-bold">${item.sortOrder}</td>
                <td>
                    <img src="${item.imageUrl}" class="banner-preview-img" alt="banner" 
                         onerror="CommonJS.handleImageError(this)">
                </td>
                <td>
                    <div class="fw-bold text-dark">${item.title}</div>
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

    resetFilters() {
        document.getElementById('bannerKeyword').value = '';
        document.getElementById('bannerIsActiveFilter').value = '';
        document.getElementById('bannerPageSize').value = '10';
        this.state.page = 0;
        this.getList();
    },

    goPage(page) {
        this.state.page = page;
        this.getList();
    },

    openModal() {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            CommonJS.alert('유지보수 모드에서는 배너 등록이 불가능합니다.', '알림', 'warning');
            return;
        }
        document.getElementById('bannerForm').reset();
        document.getElementById('bannerNo').value = '';
        document.getElementById('bannerModalTitle').innerText = '신규 배너 등록';
        this.modal.show();
    },

    openEditModal(item) {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            CommonJS.alert('유지보수 모드에서는 배너 수정이 불가능합니다.', '알림', 'warning');
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
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 배너 저장이 불가능합니다.', '알림', 'warning');
            return;
        }
        const formData = {
            bannerNo: document.getElementById('bannerNo').value || null,
            title: document.getElementById('title').value,
            imageUrl: document.getElementById('imageUrl').value,
            targetUrl: document.getElementById('targetUrl').value,
            startDtm: document.getElementById('startDtm').value,
            endDtm: document.getElementById('endDtm').value,
            sortOrder: document.getElementById('sortOrder').value,
            isActive: document.getElementById('isActive').value
        };

        if (!formData.title || !formData.imageUrl || !formData.startDtm || !formData.endDtm) {
            CommonJS.alert('필수 항목을 모두 입력하세요.', '알림', 'warning');
            return;
        }

        try {
            const res = await fetch('/api/admin/banners/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData)
            });

            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '저장 중 오류가 발생했습니다.'));

            await CommonJS.alert('성공적으로 저장되었습니다.', '성공', 'success');
            this.modal.hide();
            this.getList();
        } catch (err) {
            CommonJS.alert(err.message, '오류', 'error');
        }
    },

    async toggleActive(no, isActive) {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 배너 상태 변경이 불가능합니다.', '알림', 'warning');
            return;
        }
        try {
            const res = await fetch(`/api/admin/banners/active/${no}?isActive=${isActive}`, { method: 'PATCH' });
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '상태 변경 중 오류가 발생했습니다.'));
            await CommonJS.alert('배너 상태가 변경되었습니다.', '성공', 'success');
            this.getList();
        } catch (err) {
            CommonJS.alert(err.message, '오류', 'error');
        }
    },

    async deleteBanner(no) {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 배너 삭제가 불가능합니다.', '알림', 'warning');
            return;
        }
        const confirm = await CommonJS.confirm('배너를 삭제하시겠습니까?');
        if (!confirm) return;

        try {
            const res = await fetch(`/api/admin/banners/delete?no=${no}`, { method: 'DELETE' });
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '삭제 중 오류가 발생했습니다.'));
            await CommonJS.alert('삭제되었습니다.', '성공', 'success');
            this.getList();
        } catch (err) {
            CommonJS.alert(err.message, '오류', 'error');
        }
    },

    _updateStateFromInputs() {
        this.state.keyword = CommonJS.normalizeOptionalText(document.getElementById('bannerKeyword').value) || '';
        this.state.isActive = document.getElementById('bannerIsActiveFilter').value || '';
        this.state.size = Number(document.getElementById('bannerPageSize').value || 10);
    }
};

document.addEventListener('DOMContentLoaded', () => BannerList.init());
