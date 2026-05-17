const MemberListPage = {
    initialized: false,
    modal: null,
    selectedMember: null,
    operationPolicy: null,
    state: {
        page: 0,
        size: 20
    },

    init() {
        if (this.initialized) return;
        this.initialized = true;
        this.modal = new bootstrap.Modal(document.getElementById('memberDetailModal'));
        this.bindEvents();
        this.readStateFromUrl();
        this.applyOperationPolicy();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));
        this.getList();
    },

    async applyOperationPolicy(settings = null) {
        try {
            this.operationPolicy = settings || await CommonJS.fetchSystemSettings();
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
    },

    bindEvents() {
        document.getElementById('btnSearchMember')?.addEventListener('click', () => {
            this.state.page = 0;
            this.getList();
        });
        document.getElementById('btnResetMember')?.addEventListener('click', () => this.resetFilters());
        document.getElementById('memberPageSize')?.addEventListener('change', (event) => {
            this.state.size = Number(event.target.value || 20);
            this.state.page = 0;
            this.getList();
        });
        document.getElementById('memberKeyword')?.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                this.state.page = 0;
                this.getList();
            }
        });
        document.getElementById('btnToggleMasterYn')?.addEventListener('click', () => this.toggleMemberStatus('master'));
        document.getElementById('btnToggleMemberStatus')?.addEventListener('click', () => this.toggleMemberStatus('deleted'));
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        document.getElementById('memberKeyword').value = params.get('keyword') || '';
        document.getElementById('memberMasterYn').value = params.get('masterYn') || '';
        document.getElementById('memberDelYn').value = params.get('delYn') || '';
        this.state.page = Number(params.get('page') || 0);
        this.state.size = Number(params.get('size') || 20);
        document.getElementById('memberPageSize').value = String(this.state.size);
    },

    buildParams() {
        const params = new URLSearchParams();
        const keyword = CommonJS.normalizeOptionalText(document.getElementById('memberKeyword').value);
        const masterYn = document.getElementById('memberMasterYn').value;
        const delYn = document.getElementById('memberDelYn').value;
        if (keyword) params.set('keyword', keyword);
        if (masterYn) params.set('masterYn', masterYn);
        if (delYn) params.set('delYn', delYn);
        params.set('page', String(this.state.page));
        params.set('size', String(this.state.size));
        return params;
    },

    async getList() {
        const params = this.buildParams();
        history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
        this.setMetaText('데이터를 불러오는 중입니다...');
        this.setFilterMetaText('적용 필터를 계산하는 중입니다...');
        this.setPageMetaText('페이지 메타를 계산하는 중입니다...');
        this.renderPagination(0, 0);
        try {
            const res = await fetch(`/api/admin/members/list?${params.toString()}`);
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '회원 목록을 불러오지 못했습니다.'));
            const data = await res.json();
            this.renderList(data.items || []);
            this.renderMeta(data);
            this.renderPagination(data.currentPage ?? 0, data.totalPages ?? 0);
        } catch (err) {
            document.getElementById('memberListBody').innerHTML =
                `<tr><td colspan="7" class="text-center py-5 text-danger">${err.message}</td></tr>`;
            this.setMetaText('회원 목록 조회 실패');
            this.setFilterMetaText(err.message);
            this.setPageMetaText('페이지 메타 확인 불가');
            this.setPaginationSummary('페이지 정보를 불러오지 못했습니다.');
        }
    },

    renderList(items) {
        const tbody = document.getElementById('memberListBody');
        if (!items.length) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center py-5 text-muted">등록된 회원이 없습니다.</td></tr>';
            return;
        }
        tbody.innerHTML = items.map(item => `
            <tr>
                <td class="ps-4 text-muted small">${item.id}</td>
                <td>
                    <div class="fw-bold text-dark">${item.name || 'Unknown'}</div>
                    <div class="text-muted small">${item.nickname || '-'}</div>
                </td>
                <td>${item.email || '-'}</td>
                <td class="text-center">
                    <span class="badge ${item.masterYn === 'Y' ? 'badge-master' : 'badge-user'}">
                        ${item.masterYn === 'Y' ? '마스터' : '일반회원'}
                    </span>
                </td>
                <td class="text-center text-muted small">${item.crtDtm || '-'}</td>
                <td class="text-center">
                    <button type="button" class="btn btn-sm btn-outline-dark" data-role="open-member-detail" data-member-id="${item.id}">상세</button>
                </td>
                <td class="text-end pe-4">
                    <span class="badge ${item.delYn === 'N' ? 'badge-normal' : 'badge-deleted'}">
                        ${item.delYn === 'N' ? '정상' : '탈퇴'}
                    </span>
                </td>
            </tr>
        `).join('');
        tbody.querySelectorAll('[data-role="open-member-detail"]').forEach((button) => {
            button.addEventListener('click', () => this.openDetail(Number(button.dataset.memberId)));
        });
    },

    renderMeta(data) {
        this.setMetaText(data.resultMeta?.resultLabel || `${data.rangeStart}-${data.rangeEnd} / ${data.totalElements}명`);
        this.setFilterMetaText(`필터 ${data.resultMeta?.appliedFilterCount ?? 0}개 · ${data.resultMeta?.querySignature || '최신 가입순'}`);
        this.setPageMetaText(data.resultMeta?.pageInfoLabel || `${data.rangeStart}-${data.rangeEnd} / ${data.totalElements}명`);
        this.setPaginationSummary(`페이지 크기 ${data.pageSize ?? this.state.size} · ${data.resultMeta?.pageInfoLabel || '페이지 정보 없음'}`);
    },

    renderPagination(currentPage, totalPages) {
        const pagination = document.getElementById('memberPagination');
        if (!pagination) {
            return;
        }
        if (!totalPages || totalPages <= 1) {
            pagination.innerHTML = '';
            return;
        }
        const items = [];
        items.push(this.paginationItem('이전', currentPage - 1, currentPage <= 0));
        for (let page = 0; page < totalPages; page += 1) {
            items.push(this.paginationItem(String(page + 1), page, false, page === currentPage));
        }
        items.push(this.paginationItem('다음', currentPage + 1, currentPage >= totalPages - 1));
        pagination.innerHTML = items.join('');
        pagination.querySelectorAll('[data-page]').forEach((link) => {
            link.addEventListener('click', (event) => {
                event.preventDefault();
                const targetPage = Number(link.dataset.page);
                if (Number.isNaN(targetPage) || targetPage === this.state.page) {
                    return;
                }
                this.state.page = targetPage;
                this.getList();
            });
        });
    },

    paginationItem(label, page, disabled, active = false) {
        return `
            <li class="page-item ${disabled ? 'disabled' : ''} ${active ? 'active' : ''}">
                <a class="page-link" href="#" data-page="${page}">${label}</a>
            </li>
        `;
    },

    resetFilters() {
        document.getElementById('memberKeyword').value = '';
        document.getElementById('memberMasterYn').value = '';
        document.getElementById('memberDelYn').value = '';
        document.getElementById('memberPageSize').value = '20';
        this.state.page = 0;
        this.state.size = 20;
        this.getList();
    },

    async openDetail(memberId) {
        document.getElementById('memberDetailBody').textContent = '데이터를 불러오는 중입니다...';
        this.modal.show();
        try {
            const res = await fetch(`/api/admin/members/get?id=${memberId}`);
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '회원 상세를 불러오지 못했습니다.'));
            const data = await res.json();
            this.selectedMember = data;
            this.renderDetail(data);
        } catch (err) {
            document.getElementById('memberDetailBody').innerHTML = `<div class="text-danger">${err.message}</div>`;
        }
    },

    renderDetail(data) {
        document.getElementById('memberDetailBody').innerHTML = `
            <div class="mb-2"><strong>ID</strong> ${data.id}</div>
            <div class="mb-2"><strong>이메일</strong> ${data.email}</div>
            <div class="mb-2"><strong>이름</strong> ${data.name}</div>
            <div class="mb-2"><strong>닉네임</strong> ${data.nickname || '-'}</div>
            <div class="mb-2"><strong>권한</strong> ${data.masterYn}</div>
            <div class="mb-2"><strong>상태</strong> ${data.delYn === 'Y' ? '탈퇴' : '정상'}</div>
            <div class="mb-2"><strong>초기화 여부</strong> ${data.initYn || '-'}</div>
            <div><strong>가입일시</strong> ${data.crtDtm}</div>
        `;
        document.getElementById('btnToggleMasterYn').textContent = data.masterYn === 'Y' ? '마스터 해제' : '마스터 지정';
        document.getElementById('btnToggleMemberStatus').textContent = data.delYn === 'Y' ? '회원 복구' : '탈퇴 처리';
        const disabled = !!(this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy));
        const reason = '유지보수 모드에서는 회원 상태 변경이 불가능합니다.';
        CommonJS.setButtonDisabled(document.getElementById('btnToggleMasterYn'), disabled, reason);
        CommonJS.setButtonDisabled(document.getElementById('btnToggleMemberStatus'), disabled, reason);
    },

    async toggleMemberStatus(type) {
        if (!this.selectedMember) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert('유지보수 모드에서는 회원 상태 변경이 불가능합니다.', '알림', 'warning');
            return;
        }
        const payload = {
            masterMember: type === 'master' ? this.selectedMember.masterYn !== 'Y' : this.selectedMember.masterYn === 'Y',
            deleted: type === 'deleted' ? this.selectedMember.delYn !== 'Y' : this.selectedMember.delYn === 'Y'
        };
        try {
            const res = await fetch(`/api/admin/members/status/${this.selectedMember.id}`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '회원 상태를 변경하지 못했습니다.'));
            await CommonJS.alert('회원 상태가 변경되었습니다.', '성공', 'success');
            await this.openDetail(this.selectedMember.id);
            await this.getList();
        } catch (err) {
            CommonJS.alert(err.message, '오류', 'error');
        }
    },

    setMetaText(message) {
        document.getElementById('memberMetaText').textContent = message;
    },

    setFilterMetaText(message) {
        document.getElementById('memberFilterMeta').textContent = message;
    },

    setPageMetaText(message) {
        document.getElementById('memberPageMeta').textContent = message;
    },

    setPaginationSummary(message) {
        document.getElementById('memberPaginationSummary').textContent = message;
    }
};

document.addEventListener('DOMContentLoaded', () => MemberListPage.init());
