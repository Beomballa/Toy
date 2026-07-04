const MemberListPage = {
    initialized: false,
    modal: null,
    selectedMember: null,
    operationPolicy: null,
    detailActionInFlight: false,
    detailLoadInFlight: false,
    exportInFlight: false,
    state: {
        page: 0,
        size: 20,
        source: '',
        returnTo: ''
    },

    init() {
        if (this.initialized) return;
        this.initialized = true;
        this.modal = new bootstrap.Modal(document.getElementById('memberDetailModal'));
        this.bindEvents();
        this.readStateFromUrl();
        CommonJS.bindMainLogoNavigation(this.state.returnTo || '/admin/members');
        CommonJS.renderSourceContextNotice({ noticeId: 'memberSourceContextNotice', source: this.state.source });
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
        document.getElementById('btnExportMember')?.addEventListener('click', () => this.exportList());
        document.getElementById('btnResetMember')?.addEventListener('click', () => this.resetFilters());
        document.getElementById('memberPageSize')?.addEventListener('change', (event) => {
            this.state.size = Number(event.target.value || 20);
            this.state.page = 0;
            this.getList();
        });
        document.getElementById('memberMasterYn')?.addEventListener('change', () => {
            this.state.page = 0;
            this.getList();
        });
        document.getElementById('memberDelYn')?.addEventListener('change', () => {
            this.state.page = 0;
            this.getList();
        });
        document.getElementById('memberInitYn')?.addEventListener('change', () => {
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
        document.querySelectorAll('[data-summary-filter]').forEach((button) => {
            button.addEventListener('click', () => this.applySummaryFilter(button.dataset.summaryFilter));
        });
        document.getElementById('btnToggleMasterYn')?.addEventListener('click', () => this.toggleMemberStatus('master'));
        document.getElementById('btnToggleMemberStatus')?.addEventListener('click', () => this.toggleMemberStatus('deleted'));
        document.getElementById('memberDetailModal')?.addEventListener('hidden.bs.modal', () => {
            this.selectedMember = null;
            this.detailActionInFlight = false;
            this.setDetailActionState(false);
        });
        window.addEventListener('popstate', () => {
            this.readStateFromUrl();
            this.getList();
        });
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        document.getElementById('memberKeyword').value = params.get('keyword') || '';
        document.getElementById('memberMasterYn').value = params.get('masterYn') || '';
        document.getElementById('memberDelYn').value = params.get('delYn') || '';
        document.getElementById('memberInitYn').value = params.get('initYn') || '';
        this.state.page = this.normalizePage(params.get('page'));
        this.state.size = this.normalizePageSize(params.get('size'));
        this.state.source = params.get('source') || '';
        this.state.returnTo = params.get('returnTo') || '';
        document.getElementById('memberPageSize').value = String(this.state.size);
        CommonJS.bindMainLogoNavigation(this.state.returnTo || '/admin/members');
        CommonJS.renderSourceContextNotice({ noticeId: 'memberSourceContextNotice', source: this.state.source });
    },

    buildParams() {
        const params = new URLSearchParams();
        const keyword = CommonJS.normalizeOptionalText(document.getElementById('memberKeyword').value);
        const masterYn = document.getElementById('memberMasterYn').value;
        const delYn = document.getElementById('memberDelYn').value;
        const initYn = document.getElementById('memberInitYn').value;
        if (keyword) params.set('keyword', keyword);
        if (masterYn) params.set('masterYn', masterYn);
        if (delYn) params.set('delYn', delYn);
        if (initYn) params.set('initYn', initYn);
        params.set('page', String(this.state.page));
        params.set('size', String(this.state.size));
        if (this.state.source) params.set('source', this.state.source);
        if (this.state.returnTo) params.set('returnTo', this.state.returnTo);
        return params;
    },

    async getList() {
        this.validateState();
        const params = this.buildParams();
        history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
        this.setMetaText('회원 목록을 불러오는 중입니다...');
        this.setFilterMetaText('적용 필터를 계산하는 중입니다...');
        this.setResultMetaText('결과 메타를 계산하는 중입니다...');
        this.setPageMetaText('페이지 메타를 계산하는 중입니다...');
        this.renderPagination(0, 0);
        this.renderLoadingState();
        try {
            const [listRes, summaryRes] = await Promise.all([
                fetch(`/api/admin/members/list?${params.toString()}`),
                fetch(`/api/admin/members/summary?${params.toString()}`)
            ]);
            const res = listRes;
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '회원 목록을 불러오지 못했습니다.'));
            if (!summaryRes.ok) throw new Error(await CommonJS.extractErrorMessage(summaryRes, '회원 요약을 불러오지 못했습니다.'));
            const data = await res.json();
            const summary = await summaryRes.json();
            this.renderList(data.items || []);
            this.renderMeta(data);
            this.renderSummary(summary);
            this.renderPagination(data.currentPage ?? 0, data.totalPages ?? 0);
        } catch (err) {
            const tbody = document.getElementById('memberListBody');
            if (tbody) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="7" class="py-5">
                            <div class="product-empty-state">
                                <div class="product-empty-state__icon text-danger">
                                    <i class="fa-solid fa-triangle-exclamation"></i>
                                </div>
                                <strong>회원 목록을 불러오지 못했습니다.</strong>
                                <p>${this.escapeHtml(err.message)}</p>
                            </div>
                        </td>
                    </tr>
                `;
            }
            this.setMetaText('회원 목록 조회 실패');
            this.setFilterMetaText(err.message);
            this.setResultMetaText('결과 메타 확인 불가');
            this.setPageMetaText('페이지 메타 확인 불가');
            this.setPaginationSummary('페이지 정보를 불러오지 못했습니다.');
            this.renderSummary(null);
        }
    },

    renderSummary(summary) {
        const data = summary || {};
        document.getElementById('memberSummaryTotal').textContent = this.formatCount(data.totalCount);
        document.getElementById('memberSummaryMaster').textContent = this.formatCount(data.masterCount);
        document.getElementById('memberSummaryDeleted').textContent = this.formatCount(data.deletedCount);
        document.getElementById('memberSummaryTempPassword').textContent = this.formatCount(data.tempPasswordCount);
        document.querySelectorAll('[data-summary-filter]').forEach((button) => {
            const active = this.isSummaryFilterActive(button.dataset.summaryFilter);
            button.classList.toggle('border-dark', active);
            button.classList.toggle('shadow', active);
        });
    },

    renderList(items) {
        const tbody = document.getElementById('memberListBody');
        if (!items.length) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="py-5">
                        <div class="product-empty-state">
                            <i class="fas fa-users-slash product-empty-state-icon"></i>
                            <strong>조건에 맞는 회원이 없습니다.</strong>
                            <p>${this.buildEmptyStateMessage()}</p>
                        </div>
                    </td>
                </tr>
            `;
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
            button.addEventListener('click', () => {
                const memberId = this.normalizeOptionalPositiveNumber(button.dataset.memberId);
                if (memberId == null) {
                    void CommonJS.alert('유효한 회원 번호를 확인할 수 없습니다.', '알림', 'warning');
                    return;
                }
                this.openDetail(memberId);
            });
        });
    },

    renderMeta(data) {
        const resultMeta = data.resultMeta || null;
        this.setMetaText(resultMeta?.resultLabel || `${data.rangeStart}-${data.rangeEnd} / ${data.totalElements}명`);
        this.setFilterMetaText(
            resultMeta
                ? `필터 ${resultMeta.appliedFilterCount}개`
                : '필터 0개'
        );
        this.setResultMetaText(this.resolveQuerySignature(resultMeta?.querySignature));
        this.setPageMetaText(resultMeta?.pageInfoLabel || `${data.rangeStart}-${data.rangeEnd} / ${data.totalElements}명`);
        this.setPaginationSummary(`페이지 크기 ${data.pageSize ?? this.state.size} · ${resultMeta?.pageInfoLabel || '페이지 정보 없음'}`);
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
                this.goPage(targetPage);
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
        document.getElementById('memberInitYn').value = '';
        document.getElementById('memberPageSize').value = '20';
        this.state.page = 0;
        this.state.size = 20;
        this.getList();
    },

    renderLoadingState() {
        const tbody = document.getElementById('memberListBody');
        if (!tbody) {
            return;
        }
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="py-5">
                    <div class="product-loading-state">
                        <div class="spinner-border spinner-border-sm text-primary" role="status" aria-hidden="true"></div>
                        <strong>회원 목록을 불러오는 중입니다.</strong>
                        <p>현재 필터 조건에 맞는 회원 데이터를 조회하고 있습니다.</p>
                    </div>
                </td>
            </tr>
        `;
    },

    buildEmptyStateMessage() {
        const parts = [];
        const keyword = CommonJS.normalizeOptionalText(document.getElementById('memberKeyword').value);
        const masterYn = document.getElementById('memberMasterYn').value;
        const delYn = document.getElementById('memberDelYn').value;
        const initYn = document.getElementById('memberInitYn').value;

        if (keyword) {
            parts.push(`검색어 "${keyword}"`);
        }
        if (masterYn) {
            parts.push(`권한 ${masterYn === 'Y' ? '마스터' : '일반'}`);
        }
        if (delYn) {
            parts.push(`상태 ${delYn === 'Y' ? '탈퇴' : '정상'}`);
        }
        if (initYn) {
            parts.push(`비밀번호 ${initYn === 'Y' ? '임시 비밀번호' : '정상'}`);
        }

        if (!parts.length) {
            return '등록된 회원이 아직 없거나, 현재 페이지에 표시할 데이터가 없습니다.';
        }

        return `${parts.join(', ')} 조건에 맞는 회원이 없습니다.`;
    },

    applySummaryFilter(filterType) {
        if (filterType === 'MASTER') {
            document.getElementById('memberMasterYn').value = 'Y';
            document.getElementById('memberDelYn').value = '';
            document.getElementById('memberInitYn').value = '';
        } else if (filterType === 'DELETED') {
            document.getElementById('memberMasterYn').value = '';
            document.getElementById('memberDelYn').value = 'Y';
            document.getElementById('memberInitYn').value = '';
        } else if (filterType === 'TEMP_PASSWORD') {
            document.getElementById('memberMasterYn').value = '';
            document.getElementById('memberDelYn').value = '';
            document.getElementById('memberInitYn').value = 'Y';
        } else {
            document.getElementById('memberMasterYn').value = '';
            document.getElementById('memberDelYn').value = '';
            document.getElementById('memberInitYn').value = '';
        }
        this.state.page = 0;
        this.getList();
    },

    isSummaryFilterActive(filterType) {
        const masterYn = document.getElementById('memberMasterYn').value;
        const delYn = document.getElementById('memberDelYn').value;
        const initYn = document.getElementById('memberInitYn').value;
        if (filterType === 'MASTER') {
            return masterYn === 'Y' && !delYn && !initYn;
        }
        if (filterType === 'DELETED') {
            return delYn === 'Y' && !masterYn && !initYn;
        }
        if (filterType === 'TEMP_PASSWORD') {
            return initYn === 'Y' && !masterYn && !delYn;
        }
        return !masterYn && !delYn && !initYn;
    },

    async exportList() {
        if (this.exportInFlight) {
            return;
        }
        try {
            this.exportInFlight = true;
            CommonJS.setButtonDisabled(document.getElementById('btnExportMember'), true, '내보내는 중입니다.');
            this.validateState();
            const params = this.buildParams();
            params.delete('page');
            params.delete('size');
            await CommonJS.downloadFile(`/api/admin/members/export?${params.toString()}`, 'members.csv');
        } catch (error) {
            await CommonJS.alert(error.message || '회원 CSV를 내보내지 못했습니다.', '오류', 'error');
        } finally {
            this.exportInFlight = false;
            CommonJS.setButtonDisabled(document.getElementById('btnExportMember'), false);
        }
    },

    async openDetail(memberId) {
        if (!this.isPositiveNumber(memberId)) {
            await CommonJS.alert('유효한 회원 번호를 확인할 수 없습니다.', '알림', 'warning');
            return;
        }
        if (this.detailLoadInFlight) {
            return;
        }
        this.setDetailLoadingState('회원 상세를 불러오는 중입니다.', '프로필, 권한, 상태 정보를 정리하고 있습니다.');
        this.modal.show();
        try {
            this.detailLoadInFlight = true;
            const data = await this.fetchMemberDetail(memberId);
            this.renderDetail(data);
        } catch (err) {
            document.getElementById('memberDetailBody').innerHTML = `
                <div class="product-empty-state py-4">
                    <div class="product-empty-state__icon text-danger">
                        <i class="fa-solid fa-triangle-exclamation"></i>
                    </div>
                    <strong>회원 상세를 불러오지 못했습니다.</strong>
                    <p>${this.escapeHtml(err.message)}</p>
                </div>
            `;
            this.setDetailActionState(true);
        } finally {
            this.detailLoadInFlight = false;
        }
    },

    async fetchMemberDetail(memberId) {
        if (!this.isPositiveNumber(memberId)) {
            throw new Error('유효한 회원 번호를 확인할 수 없습니다.');
        }
        const res = await fetch(`/api/admin/members/get?id=${memberId}`);
        if (!res.ok) {
            throw new Error(await CommonJS.extractErrorMessage(res, '회원 상세를 불러오지 못했습니다.'));
        }
        const data = await res.json();
        this.selectedMember = data;
        return data;
    },

    setDetailLoadingState(title, description) {
        document.getElementById('memberDetailBody').innerHTML = `
            <div class="product-loading-state py-4">
                <div class="spinner-border spinner-border-sm text-primary" role="status" aria-hidden="true"></div>
                <strong>${this.escapeHtml(title)}</strong>
                <p>${this.escapeHtml(description)}</p>
            </div>
        `;
        this.setDetailActionState(true);
    },

    renderDetail(data) {
        document.getElementById('memberDetailBody').innerHTML = `
            <div class="member-detail-layout">
                <section class="member-detail-hero">
                    ${this.renderMemberAvatar(data)}
                    <div class="member-detail-summary">
                        <div class="member-detail-summary-top">
                            <div class="member-detail-summary-title">
                                <h3 class="member-detail-name">${this.escapeHtml(data.name || '이름 미등록')}</h3>
                                <div class="member-detail-email">${this.escapeHtml(data.email || '-')}</div>
                            </div>
                            <div class="member-detail-badges">
                                ${this.renderRoleBadge(data.masterYn)}
                                ${this.renderStatusBadge(data.delYn)}
                            </div>
                        </div>
                        <div class="member-detail-meta-row">
                            <span class="member-detail-chip">회원번호 ${this.escapeHtml(String(data.id ?? '-'))}</span>
                            <span class="member-detail-chip">가입 ${this.escapeHtml(data.crtDtm || '-')}</span>
                        </div>
                    </div>
                </section>

                <section class="member-detail-grid">
                    ${this.renderDetailCard('닉네임', data.nickname || '미등록')}
                    ${this.renderDetailCard('초기화 여부', this.formatInitStatus(data.initYn))}
                    ${this.renderDetailCard('임시 비밀번호 발급', this.formatTempPasswordIssuedAt(data.initYn, data.tmpPwIssueDtm))}
                    ${this.renderDetailCard('권한 상태', this.formatRoleText(data.masterYn), '현재 운영 권한 상태입니다.')}
                    ${this.renderDetailCard('계정 상태', this.formatMemberStatus(data.delYn), '탈퇴 처리 여부를 포함합니다.')}
                </section>

                <div class="member-detail-note">
                    회원 상태와 권한 변경은 즉시 반영됩니다. 유지보수 모드에서는 하단 액션 버튼이 비활성화됩니다.
                </div>
            </div>
        `;
        this.bindDetailAvatarFallback(data);
        document.getElementById('btnToggleMasterYn').textContent = data.masterYn === 'Y' ? '마스터 해제' : '마스터 지정';
        document.getElementById('btnToggleMemberStatus').textContent = data.delYn === 'Y' ? '회원 복구' : '탈퇴 처리';
        const disabled = !!(this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy));
        const reason = '유지보수 모드에서는 회원 상태 변경이 불가능합니다.';
        this.setDetailActionState(this.detailActionInFlight || disabled, disabled ? reason : '');
    },

    renderMemberAvatar(data) {
        const name = data.name || data.nickname || data.email || '회원';
        if (data.profileImgPath) {
            return `
                <div class="member-detail-avatar-shell">
                    <img src="${this.escapeHtml(data.profileImgPath)}" alt="${this.escapeHtml(name)}" class="member-detail-avatar-img" id="memberDetailAvatarImage">
                </div>
            `;
        }
        return `<div class="member-detail-avatar">${this.escapeHtml(this.getInitials(name))}</div>`;
    },

    bindDetailAvatarFallback(data) {
        const avatarImage = document.getElementById('memberDetailAvatarImage');
        if (!avatarImage) {
            return;
        }
        const name = data.name || data.nickname || data.email || '회원';
        avatarImage.addEventListener('error', () => {
            avatarImage.outerHTML = `<div class="member-detail-avatar">${this.escapeHtml(this.getInitials(name))}</div>`;
        }, { once: true });
    },

    renderRoleBadge(masterYn) {
        const isMaster = masterYn === 'Y';
        return `<span class="badge ${isMaster ? 'badge-master' : 'badge-user'}">${isMaster ? '마스터 권한' : '일반 회원'}</span>`;
    },

    renderStatusBadge(delYn) {
        const deleted = delYn === 'Y';
        return `<span class="badge ${deleted ? 'badge-deleted' : 'badge-normal'}">${deleted ? '탈퇴 처리' : '정상 이용'}</span>`;
    },

    renderDetailCard(label, value, description = '') {
        return `
            <article class="member-detail-card">
                <div class="member-detail-label">${this.escapeHtml(label)}</div>
                <div class="member-detail-value">${this.escapeHtml(value)}</div>
                ${description ? `<div class="member-detail-value member-detail-value--muted">${this.escapeHtml(description)}</div>` : ''}
            </article>
        `;
    },

    formatRoleText(masterYn) {
        return masterYn === 'Y' ? '마스터 관리자 권한' : '일반 회원 권한';
    },

    formatMemberStatus(delYn) {
        return delYn === 'Y' ? '탈퇴 처리 상태' : '정상 활성 상태';
    },

    formatInitStatus(initYn) {
        return initYn === 'Y' ? '초기 비밀번호 상태' : '일반 로그인 상태';
    },

    formatTempPasswordIssuedAt(initYn, tmpPwIssueDtm) {
        if (initYn !== 'Y') {
            return '발급 이력 없음';
        }
        return tmpPwIssueDtm && tmpPwIssueDtm !== '-' ? tmpPwIssueDtm : '발급 시각 미기록';
    },

    getInitials(value) {
        const normalized = String(value || '').trim();
        if (!normalized) {
            return 'U';
        }
        const parts = normalized.split(/\s+/).filter(Boolean);
        if (parts.length >= 2) {
            return parts.slice(0, 2).map((part) => part.charAt(0).toUpperCase()).join('');
        }
        return normalized.slice(0, 2).toUpperCase();
    },

    escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    },

    async toggleMemberStatus(type) {
        if (!this.selectedMember || this.detailActionInFlight) {
            return;
        }
        if (!this.isPositiveNumber(Number(this.selectedMember.id))) {
            await CommonJS.alert('유효한 회원 번호를 확인할 수 없습니다.', '알림', 'warning');
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
            this.detailActionInFlight = true;
            this.setDetailActionState(true, '상태 변경을 처리하는 중입니다.');
            const res = await fetch(`/api/admin/members/status/${this.selectedMember.id}`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '회원 상태를 변경하지 못했습니다.'));
            const refreshed = await this.fetchMemberDetail(this.selectedMember.id);
            this.renderDetail(refreshed);
            await this.getList();
            await CommonJS.alert('회원 상태가 변경되었습니다.', '성공', 'success');
            this.modal.hide();
        } catch (err) {
            await CommonJS.alert(err.message, '오류', 'error');
            if (this.selectedMember) {
                this.renderDetail(this.selectedMember);
            }
        } finally {
            this.detailActionInFlight = false;
            if (this.selectedMember) {
                this.renderDetail(this.selectedMember);
            } else {
                this.setDetailActionState(false);
            }
        }
    },

    setDetailActionState(disabled, reason = '') {
        CommonJS.setButtonDisabled(document.getElementById('btnToggleMasterYn'), disabled, reason);
        CommonJS.setButtonDisabled(document.getElementById('btnToggleMemberStatus'), disabled, reason);
    },

    setMetaText(message) {
        document.getElementById('memberMetaText').textContent = message;
    },

    setFilterMetaText(message) {
        document.getElementById('memberFilterMeta').textContent = message;
    },

    setResultMetaText(message) {
        document.getElementById('memberResultMeta').textContent = message;
    },

    setPageMetaText(message) {
        document.getElementById('memberPageMeta').textContent = message;
    },

    goPage(page) {
        if (!Number.isInteger(page) || page < 0) {
            void CommonJS.alert('이동할 페이지 정보가 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        this.state.page = page;
        this.getList();
    },

    validateState() {
        const keyword = CommonJS.normalizeOptionalText(document.getElementById('memberKeyword').value) || '';
        const masterYn = document.getElementById('memberMasterYn').value;
        const delYn = document.getElementById('memberDelYn').value;
        const initYn = document.getElementById('memberInitYn').value;
        this.state.size = this.normalizePageSize(document.getElementById('memberPageSize')?.value);

        if (keyword.length > 100) {
            throw new Error('검색어는 100자 이하로 입력하세요.');
        }
        if (masterYn && !this.isValidYn(masterYn)) {
            throw new Error('마스터 여부 필터 값이 올바르지 않습니다.');
        }
        if (delYn && !this.isValidYn(delYn)) {
            throw new Error('탈퇴 여부 필터 값이 올바르지 않습니다.');
        }
        if (initYn && !this.isValidYn(initYn)) {
            throw new Error('초기 비밀번호 여부 필터 값이 올바르지 않습니다.');
        }
    },

    normalizePage(value) {
        const page = Number(value);
        return Number.isInteger(page) && page >= 0 ? page : 0;
    },

    normalizePageSize(value) {
        const size = Number(value);
        return Number.isInteger(size) && size > 0 ? size : 20;
    },

    normalizeOptionalPositiveNumber(value) {
        if (value == null || value === '') {
            return null;
        }
        const number = Number(value);
        return this.isPositiveNumber(number) ? number : null;
    },

    isPositiveNumber(value) {
        return Number.isInteger(value) && value > 0;
    },

    isValidYn(value) {
        return value === 'Y' || value === 'N';
    },

    setPaginationSummary(message) {
        document.getElementById('memberPaginationSummary').textContent = message;
    },

    formatCount(value) {
        return Number(value || 0).toLocaleString('ko-KR');
    },

    resolveQuerySignature(signature) {
        if (!signature) {
            return '최신 가입순';
        }

        return signature
            .replace('권한=Y', '권한=마스터')
            .replace('권한=N', '권한=일반')
            .replace('상태=Y', '상태=탈퇴')
            .replace('상태=N', '상태=정상');
    }
};

document.addEventListener('DOMContentLoaded', () => MemberListPage.init());
