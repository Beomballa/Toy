const BrandList = {
    modal: null,
    operationPolicy: null,

    init() {
        const modalEl = document.getElementById('brandModal');
        if (modalEl) {
            this.modal = new bootstrap.Modal(modalEl);
        } else {
            console.error('브랜드 모달 엘리먼트를 찾을 수 없습니다.');
        }
        this.bindEvents();
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
    },

    async getList() {
        try {
            const res = await fetch('/api/admin/brands/list');
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            this.renderList(data);
        } catch (err) {
            console.error('브랜드 목록 로드 실패:', err);
        }
    },

    renderList(items) {
        const tbody = document.getElementById('brandListBody');
        if (!tbody) return;

        if (!items || items.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center py-5 text-muted">등록된 브랜드가 없습니다.</td></tr>';
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
                    <button class="btn btn-sm btn-outline-primary me-1" onclick="BrandList.openModal(${item.brandNo})">수정</button>
                    <button class="btn btn-sm btn-outline-danger" onclick="BrandList.deleteBrand(${item.brandNo})">삭제</button>
                </td>
            </tr>
        `).join('');
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
    }
};

document.addEventListener('DOMContentLoaded', () => BrandList.init());
