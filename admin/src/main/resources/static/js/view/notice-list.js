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
        noticeNo: ''
    },

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
        this.getList();
    },

    async applyOperationPolicy(settings = null) {
        try {
            this.operationPolicy = settings || await CommonJS.fetchSystemSettings();
            const disabled = CommonJS.isAdminWriteBlocked(this.operationPolicy);
            const reason = CommonJS.getAdminWriteBlockedReason('운영 공지 등록 및 수정');
            CommonJS.setButtonDisabled(document.getElementById('btnNewNotice'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnSaveNotice'), disabled, reason);
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
    },

    bindEvents() {
        document.getElementById('btnNewNotice')?.addEventListener('click', () => this.openModal());
        document.getElementById('btnSaveNotice')?.addEventListener('click', () => this.saveNotice());
        document.getElementById('btnSearchNotice')?.addEventListener('click', () => this.getList());
        document.getElementById('btnResetNotice')?.addEventListener('click', () => this.resetFilters());
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
        this.state.noticeNo = params.get('noticeNo') || '';
        document.getElementById('noticeKeyword').value = this.state.keyword;
        document.getElementById('noticeIsActiveFilter').value = this.state.isActive;
        document.getElementById('noticeIsPinnedFilter').value = this.state.isPinned;
        document.getElementById('noticePageSize').value = String(this.state.size);
    },

    updateStateFromInputs() {
        this.state.keyword = document.getElementById('noticeKeyword').value.trim();
        this.state.isActive = document.getElementById('noticeIsActiveFilter').value;
        this.state.isPinned = document.getElementById('noticeIsPinnedFilter').value;
        this.state.size = Number(document.getElementById('noticePageSize').value || 10);
    },

    buildParams() {
        const params = new URLSearchParams();
        params.set('page', String(this.state.page));
        params.set('size', String(this.state.size));
        if (this.state.keyword) params.set('keyword', this.state.keyword);
        if (this.state.isActive) params.set('isActive', this.state.isActive);
        if (this.state.isPinned) params.set('isPinned', this.state.isPinned);
        if (this.state.noticeNo) params.set('noticeNo', this.state.noticeNo);
        return params;
    },

    async getList() {
        try {
            this.updateStateFromInputs();
            const params = this.buildParams();
            history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
            this.setFilterMeta('적용 필터를 계산하는 중입니다...');
            this.setResultMeta('결과 메타를 계산하는 중입니다...');
            this.setPageMeta('페이지 메타를 계산하는 중입니다...');
            this.setListStateMeta('loading', '운영 공지를 불러오는 중입니다.', 0, 0, '');

            const res = await fetch(`/api/admin/settings/notices/list?${params.toString()}`);
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '운영 공지 목록을 불러오지 못했습니다.'));

            const data = await res.json();
            this.renderList(data.items || []);
            this.renderMeta(data);
            this.renderPagination(data);
            await this.openDeepLinkedNoticeIfNeeded(data.items || []);
        } catch (err) {
            document.getElementById('noticeMetaText').textContent = err.message;
            this.setFilterMeta(err.message);
            this.setResultMeta('결과 메타 확인 불가');
            this.setPageMeta('페이지 메타 확인 불가');
            document.getElementById('noticeListBody').innerHTML = `<tr><td colspan="6" class="text-center py-5 text-danger">${err.message}</td></tr>`;
            document.getElementById('noticePagination').innerHTML = '';
            this.setListStateMeta('error', err.message, 0, 0, '');
        }
    },

    renderList(items) {
        const tbody = document.getElementById('noticeListBody');
        if (!tbody) return;

        if (!items || items.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center py-5 text-muted">등록된 운영 공지가 없습니다.</td></tr>';
            this.setListStateMeta('empty', '등록된 운영 공지가 없습니다.', 0, 0, '');
            return;
        }

        tbody.innerHTML = items.map((item) => `
            <tr>
                <td class="ps-4 text-muted small">${item.noticeNo}</td>
                <td>
                    <div class="d-flex align-items-center gap-2 mb-1">
                        ${item.isPinned === 'Y' ? '<span class="badge text-bg-danger">고정</span>' : ''}
                        <span class="fw-bold text-dark">${this.escapeHtml(item.title)}</span>
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
                    <button class="btn btn-sm btn-outline-dark me-1" data-role="toggle-notice" data-notice-no="${item.noticeNo}" data-next-active="${item.isActive === 'Y' ? 'N' : 'Y'}">${item.isActive === 'Y' ? '비활성' : '활성'}</button>
                    <button class="btn btn-sm btn-outline-danger" data-role="delete-notice" data-notice-no="${item.noticeNo}">삭제</button>
                </td>
            </tr>
        `).join('');

        this.setListStateMeta('ready', '', items.length, null, null);
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
        document.getElementById('noticeMetaText').textContent = data.resultMeta?.resultLabel || `${data.totalElements || 0}건 조회`;
        this.setFilterMeta(`필터 ${data.resultMeta?.appliedFilterCount ?? 0}개 · ${data.resultMeta?.querySignature || '고정 우선 최신순'}`);
        this.setResultMeta(data.resultMeta?.resultLabel || '결과 메타 없음');
        this.setPageMeta(data.resultMeta?.pageInfoLabel || '페이지 메타 없음');
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
            button.addEventListener('click', () => this.goPage(Number(button.dataset.page)));
        });
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
        document.getElementById('noticePageSize').value = '10';
        this.state.page = 0;
        this.state.noticeNo = '';
        this.getList();
    },

    goPage(page) {
        this.state.page = page;
        this.getList();
    },

    openModal() {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 공지 등록 및 수정'), '알림', 'warning');
            return;
        }
        document.getElementById('noticeForm').reset();
        document.getElementById('noticeNo').value = '';
        document.getElementById('noticeIsActive').value = 'Y';
        document.getElementById('noticeIsPinned').value = 'N';
        document.getElementById('noticeModalTitle').innerText = '운영 공지 등록';
        this.modal.show();
    },

    openEditModal(item) {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 공지 등록 및 수정'), '알림', 'warning');
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
            const res = await fetch('/api/admin/settings/notices/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData)
            });
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '운영 공지를 저장하지 못했습니다.'));

            await CommonJS.alert('운영 공지가 저장되었습니다.', '성공', 'success');
            this.modal.hide();
            this.getList();
        } catch (err) {
            await CommonJS.alert(err.message, '오류', 'error');
        }
    },

    async toggleActive(noticeNo, isActive) {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 공지 상태 변경'), '알림', 'warning');
            return;
        }
        try {
            const res = await fetch(`/api/admin/settings/notices/active/${noticeNo}?isActive=${isActive}`, {
                method: 'PATCH'
            });
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '공지 상태를 변경하지 못했습니다.'));
            this.getList();
        } catch (err) {
            await CommonJS.alert(err.message, '오류', 'error');
        }
    },

    async deleteNotice(noticeNo) {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 공지 삭제'), '알림', 'warning');
            return;
        }
        const confirmed = await CommonJS.confirm('운영 공지를 삭제하시겠습니까?', '삭제 확인');
        if (!confirmed) return;

        try {
            const res = await fetch(`/api/admin/settings/notices/delete?no=${noticeNo}`, {
                method: 'DELETE'
            });
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '운영 공지를 삭제하지 못했습니다.'));
            await CommonJS.alert('운영 공지가 삭제되었습니다.', '성공', 'success');
            this.getList();
        } catch (err) {
            await CommonJS.alert(err.message, '오류', 'error');
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
