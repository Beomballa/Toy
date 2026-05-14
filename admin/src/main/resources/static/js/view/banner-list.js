const BannerList = {
    modal: null,
    state: {},
    operationPolicy: null,

    init() {
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
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        document.getElementById('bannerKeyword').value = params.get('keyword') || '';
        document.getElementById('bannerIsActiveFilter').value = params.get('isActive') || '';
    },

    buildParams() {
        const params = new URLSearchParams();
        const keyword = CommonJS.normalizeOptionalText(document.getElementById('bannerKeyword').value);
        const isActive = document.getElementById('bannerIsActiveFilter').value;
        if (keyword) params.set('keyword', keyword);
        if (isActive) params.set('isActive', isActive);
        return params;
    },

    async getList() {
        try {
            const params = this.buildParams();
            history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
            const res = await fetch(`/api/admin/banners/list?${params.toString()}`);
            if (!res.ok) throw new Error(await CommonJS.extractErrorMessage(res, '배너 목록을 불러오지 못했습니다.'));
            const data = await res.json();
            this.renderList(data.items || []);
            document.getElementById('bannerMetaText').textContent = `${(data.items || []).length}건 조회`;
        } catch (err) {
            document.getElementById('bannerMetaText').textContent = err.message;
            document.getElementById('bannerListBody').innerHTML =
                `<tr><td colspan="6" class="text-center py-5 text-danger">${err.message}</td></tr>`;
        }
    },

    renderList(items) {
        const tbody = document.getElementById('bannerListBody');
        if (!tbody) return;

        if (!items || items.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center py-5 text-muted">등록된 배너가 없습니다.</td></tr>';
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
                    <button class="btn btn-sm btn-outline-primary me-1" onclick="BannerList.openEditModal(${JSON.stringify(item).replace(/"/g, '&quot;')})">수정</button>
                    <button class="btn btn-sm btn-outline-dark me-1" onclick="BannerList.toggleActive(${item.bannerNo}, '${item.isActive === 'Y' ? 'N' : 'Y'}')">${item.isActive === 'Y' ? '중지' : '활성'}</button>
                    <button class="btn btn-sm btn-outline-danger" onclick="BannerList.deleteBanner(${item.bannerNo})">삭제</button>
                </td>
            </tr>
        `).join('');
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
    }
};

document.addEventListener('DOMContentLoaded', () => BannerList.init());
