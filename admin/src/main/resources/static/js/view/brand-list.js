const BrandList = {
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
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
    },

    bindEvents() {
        document.getElementById('btnNewBrand')?.addEventListener('click', () => {
            this.openModal();
        });

        document.getElementById('btnSaveBrand')?.addEventListener('click', () => {
            this.saveBrand();
        });

        document.getElementById('btnSearchBrand')?.addEventListener('click', () => this.getList());
        document.getElementById('btnResetBrand')?.addEventListener('click', () => this.resetFilters());
        document.getElementById('brandPageSize')?.addEventListener('change', () => {
            this.state.page = 0;
            this._updateStateFromInputs();
            this.getList();
        });
        document.getElementById('brandListBody')?.addEventListener('click', (event) => {
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
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        this.state.page = Number(params.get('page') || 0);
        this.state.size = Number(params.get('size') || 10);
        this.state.keyword = params.get('keyword') || '';
        this.state.isActive = params.get('isActive') || '';
        document.getElementById('brandKeyword').value = this.state.keyword;
        document.getElementById('brandIsActiveFilter').value = this.state.isActive;
        document.getElementById('brandPageSize').value = String(this.state.size);
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
            this.setListStateMeta('loading', '브랜드 목록을 불러오는 중입니다.', 0, 0, '');
            const res = await fetch(`/api/admin/brands/list?${params.toString()}`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            this.renderList(data.items || []);
            this.renderMeta(data);
            this.renderPagination(data);
        } catch (err) {
            console.error('브랜드 목록 로드 실패:', err);
            document.getElementById('brandMetaText').textContent = '브랜드 목록 조회 실패';
            this.setFilterMeta('브랜드 목록을 불러오지 못했습니다.');
            this.setResultMeta('결과 메타 확인 불가');
            this.setPageMeta('페이지 메타 확인 불가');
            document.getElementById('brandPagination').innerHTML = '';
            this.setListStateMeta('error', '브랜드 목록 조회 실패', 0, 0, '');
        }
    },

    renderList(items) {
        const tbody = document.getElementById('brandListBody');
        if (!tbody) return;

        if (!items || items.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center py-5 text-muted">등록된 브랜드가 없습니다.</td></tr>';
            this.setListStateMeta('empty', '등록된 브랜드가 없습니다.', 0, 0, '');
            return;
        }

        tbody.innerHTML = items.map(item => `
            <tr>
                <td class="ps-4 text-muted">${item.brandNo}</td>
                <td>
                    <div class="brand-logo-wrapper">
                        <img src="${item.logoUrl || ''}" class="brand-logo-img" alt="${item.nameKo}" 
                             onerror="CommonJS.handleImageError(this, '${item.nameKo}')">
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
        this.setListStateMeta('ready', '', items.length, null, null);
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

    resetFilters() {
        document.getElementById('brandKeyword').value = '';
        document.getElementById('brandIsActiveFilter').value = '';
        document.getElementById('brandPageSize').value = '10';
        this.state.page = 0;
        this.getList();
    },

    goPage(page) {
        this.state.page = page;
        this.getList();
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
                const data = await res.json();
                document.getElementById('brandNo').value = data.brandNo;
                document.getElementById('nameKo').value = data.nameKo;
                document.getElementById('nameEn').value = data.nameEn || '';
                document.getElementById('logoUrl').value = data.logoUrl || '';
                document.getElementById('isActive').value = data.isActive || 'Y';
                document.getElementById('brandModalTitle').innerText = '브랜드 정보 수정';
            } catch (err) {
                console.error('브랜드 정보 로드 실패:', err);
                return;
            }
        }
        this.modal.show();
    },

    async saveBrand() {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 브랜드 저장이 불가능합니다.', '알림', 'warning');
            return;
        }
        const brandNo = document.getElementById('brandNo').value;
        const nameKo = document.getElementById('nameKo').value;
        if (!nameKo) {
            CommonJS.alert('브랜드명을 입력하세요.', '알림', 'warning');
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
            const res = await fetch('/api/admin/brands/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (!res.ok) throw new Error();

            CommonJS.alert('성공적으로 저장되었습니다.', '성공', 'success', () => {
                this.modal.hide();
                this.getList();
            });
        } catch (err) {
            CommonJS.alert('저장 중 오류가 발생했습니다.', '오류', 'error');
        }
    },

    async deleteBrand(brandNo) {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 브랜드 삭제가 불가능합니다.', '알림', 'warning');
            return;
        }
        const confirm = await CommonJS.confirm('정말 삭제하시겠습니까?');
        if (!confirm) return;

        try {
            const res = await fetch(`/api/admin/brands/delete?no=${brandNo}`, { method: 'DELETE' });
            if (!res.ok) throw new Error();
            CommonJS.alert('삭제되었습니다.', '성공', 'success', () => this.getList());
        } catch (err) {
            CommonJS.alert('삭제 중 오류가 발생했습니다. (연관된 상품이 있을 수 있습니다)', '오류', 'error');
        }
    },

    _updateStateFromInputs() {
        this.state.keyword = CommonJS.normalizeOptionalText(document.getElementById('brandKeyword').value) || '';
        this.state.isActive = document.getElementById('brandIsActiveFilter').value || '';
        this.state.size = Number(document.getElementById('brandPageSize').value || 10);
    }
};

document.addEventListener('DOMContentLoaded', () => BrandList.init());
