const BannerList = {
    modal: null,

    init() {
        const modalEl = document.getElementById('bannerModal');
        if (modalEl) {
            this.modal = new bootstrap.Modal(modalEl);
        }
        this.bindEvents();
        this.getList();
    },

    bindEvents() {
        document.getElementById('btnNewBanner')?.addEventListener('click', () => {
            this.openModal();
        });

        document.getElementById('btnSaveBanner')?.addEventListener('click', () => {
            this.saveBanner();
        });
    },

    async getList() {
        try {
            const res = await fetch('/api/admin/banners/list');
            if (!res.ok) throw new Error();
            const data = await res.json();
            this.renderList(data);
        } catch (err) {
            console.error('배너 목록 로드 실패:', err);
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
                        ${item.isActive === 'Y' ? '사용중' : '중지'}
                    </span>
                </td>
                <td class="text-end pe-4">
                    <button class="btn btn-sm btn-outline-primary me-1" onclick="BannerList.openEditModal(${JSON.stringify(item).replace(/"/g, '&quot;')})">수정</button>
                    <button class="btn btn-sm btn-outline-danger" onclick="BannerList.deleteBanner(${item.bannerNo})">삭제</button>
                </td>
            </tr>
        `).join('');
    },

    openModal() {
        document.getElementById('bannerForm').reset();
        document.getElementById('bannerNo').value = '';
        document.getElementById('bannerModalTitle').innerText = '신규 배너 등록';
        this.modal.show();
    },

    openEditModal(item) {
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
        const formData = {
            bannerNo: document.getElementById('bannerNo').value || null,
            title: document.getElementById('title').value,
            imageUrl: document.getElementById('imageUrl').value,
            targetUrl: document.getElementById('targetUrl').value,
            startDtm: document.getElementById('startDtm').value,
            endDtm: document.getElementById('endDtm').value,
            sortOrder: document.getElementById('sortOrder').value,
            isActive: document.getElementById('isActive').value,
            crtAdminNo: 1 // 테스트용
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

            if (!res.ok) throw new Error();

            CommonJS.alert('성공적으로 저장되었습니다.', '성공', 'success', () => {
                this.modal.hide();
                this.getList();
            });
        } catch (err) {
            CommonJS.alert('저장 중 오류가 발생했습니다.', '오류', 'error');
        }
    },

    async deleteBanner(no) {
        const confirm = await CommonJS.confirm('배너를 삭제하시겠습니까?');
        if (!confirm) return;

        try {
            const res = await fetch(`/api/admin/banners/delete?no=${no}`, { method: 'DELETE' });
            if (!res.ok) throw new Error();
            CommonJS.alert('삭제되었습니다.', '성공', 'success', () => this.getList());
        } catch (err) {
            CommonJS.alert('삭제 중 오류가 발생했습니다.', '오류', 'error');
        }
    }
};

document.addEventListener('DOMContentLoaded', () => BannerList.init());
