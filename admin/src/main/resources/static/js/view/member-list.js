const MemberListPage = {
    modal: null,
    selectedMember: null,
    state: {
        page: 0,
        size: 20
    },

    init() {
        this.modal = new bootstrap.Modal(document.getElementById('memberDetailModal'));
        this.bindEvents();
        this.readStateFromUrl();
        this.getList();
    },

    bindEvents() {
        document.getElementById('btnSearchMember')?.addEventListener('click', () => {
            this.state.page = 0;
            this.getList();
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
        try {
            const res = await fetch(`/api/admin/members/list?${params.toString()}`);
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '회원 목록을 불러오지 못했습니다.'));
            const data = await res.json();
            this.renderList(data.items || []);
            this.setMetaText(`${data.rangeStart}-${data.rangeEnd} / ${data.totalElements}건 · ${data.totalPages}페이지`);
        } catch (err) {
            document.getElementById('memberListBody').innerHTML =
                `<tr><td colspan="7" class="text-center py-5 text-danger">${err.message}</td></tr>`;
            this.setMetaText('회원 목록 조회 실패');
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
                    <button type="button" class="btn btn-sm btn-outline-dark" onclick="MemberListPage.openDetail(${item.id})">상세</button>
                </td>
                <td class="text-end pe-4">
                    <span class="badge ${item.delYn === 'N' ? 'badge-normal' : 'badge-deleted'}">
                        ${item.delYn === 'N' ? '정상' : '탈퇴'}
                    </span>
                </td>
            </tr>
        `).join('');
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
    },

    async toggleMemberStatus(type) {
        if (!this.selectedMember) {
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
    }
};

document.addEventListener('DOMContentLoaded', () => MemberListPage.init());
