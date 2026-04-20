const CategoryList = {
    modal: null,
    state: {
        selectedParentNo: null,
        selectedParentName: '',
        depth1List: [],
        depth2List: []
    },

    init() {
        const modalEl = document.getElementById('categoryModal');
        if (modalEl) {
            this.modal = new bootstrap.Modal(modalEl);
        } else {
            console.error('카테고리 모달 엘리먼트를 찾을 수 없습니다.');
        }
        this.bindEvents();
        this.getDepth1List();
    },

    bindEvents() {
        document.getElementById('btnSaveCategory')?.addEventListener('click', () => {
            this.saveCategory();
        });
    },

    async getDepth1List() {
        try {
            const res = await fetch('/api/admin/categories/list?depth=1');
            const data = await res.json();
            this.state.depth1List = data;
            this.renderDepth1();
        } catch (err) {
            console.error('1차 카테고리 로드 실패:', err);
        }
    },

    async getDepth2List(parentNo, parentName) {
        this.state.selectedParentNo = parentNo;
        this.state.selectedParentName = parentName;
        
        document.getElementById('parentCategoryName').innerText = `> ${parentName}`;
        document.getElementById('btnNewSubCategory').disabled = false;

        try {
            const res = await fetch(`/api/admin/categories/sub?parentNo=${parentNo}`);
            const data = await res.json();
            this.state.depth2List = data;
            this.renderDepth2();
        } catch (err) {
            console.error('2차 카테고리 로드 실패:', err);
        }
    },

    renderDepth1() {
        const body = document.getElementById('depth1Body');
        if (!this.state.depth1List || this.state.depth1List.length === 0) {
            body.innerHTML = '<div class="text-center py-5 text-muted">등록된 카테고리가 없습니다.</div>';
            return;
        }

        body.innerHTML = this.state.depth1List.map(item => `
            <div class="category-item d-flex justify-content-between align-items-center ${this.state.selectedParentNo === item.categoryNo ? 'active' : ''}" 
                 onclick="CategoryList.getDepth2List(${item.categoryNo}, '${item.name}')">
                <span>${item.name}</span>
                <div class="d-flex align-items-center gap-2">
                    <span class="badge rounded-pill ${item.isActive === 'Y' ? 'badge-y' : 'badge-n'}">
                        ${item.isActive === 'Y' ? '사용중' : '중지'}
                    </span>
                    <button class="btn btn-xs btn-link p-0 text-muted" onclick="event.stopPropagation(); CategoryList.openModal(1, ${JSON.stringify(item).replace(/"/g, '&quot;')})">
                        <i class="fas fa-edit"></i>
                    </button>
                </div>
            </div>
        `).join('');
    },

    renderDepth2() {
        const wrapper = document.getElementById('depth2TableWrapper');
        const emptyMsg = document.getElementById('depth2EmptyMessage');
        const tbody = document.getElementById('depth2ListBody');

        emptyMsg.classList.add('d-none');
        wrapper.classList.remove('d-none');

        if (!this.state.depth2List || this.state.depth2List.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center py-5 text-muted">하위 카테고리가 없습니다.</td></tr>';
            return;
        }

        tbody.innerHTML = this.state.depth2List.map(item => `
            <tr>
                <td class="ps-4 text-muted small">${item.categoryNo}</td>
                <td class="fw-bold">${item.name}</td>
                <td class="text-center">
                    <span class="badge rounded-pill ${item.isActive === 'Y' ? 'badge-y' : 'badge-n'}">
                        ${item.isActive === 'Y' ? '사용중' : '중지'}
                    </span>
                </td>
                <td class="text-end pe-4">
                    <button class="btn btn-sm btn-outline-primary me-1" onclick="CategoryList.openModal(2, ${JSON.stringify(item).replace(/"/g, '&quot;')})">수정</button>
                    <button class="btn btn-sm btn-outline-danger" onclick="CategoryList.deleteCategory(${item.categoryNo})">삭제</button>
                </td>
            </tr>
        `).join('');
    },

    openModal(depth, item) {
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
        const name = document.getElementById('categoryName').value;
        if (!name) {
            CommonJS.alert('카테고리명을 입력하세요.', '알림', 'warning');
            return;
        }

        const data = {
            categoryNo: document.getElementById('categoryNo').value || null,
            parentNo: document.getElementById('parentNo').value,
            name: name,
            depth: document.getElementById('depth').value,
            isActive: document.getElementById('isCategoryActive').value
        };

        try {
            const res = await fetch('/api/admin/categories/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (!res.ok) throw new Error();

            CommonJS.alert('성공적으로 저장되었습니다.', '성공', 'success', () => {
                this.modal.hide();
                this.getDepth1List();
                if (data.depth == 2 || this.state.selectedParentNo == data.categoryNo) {
                    this.getDepth2List(this.state.selectedParentNo, this.state.selectedParentName);
                }
            });
        } catch (err) {
            CommonJS.alert('저장 중 오류가 발생했습니다.', '오류', 'error');
        }
    },

    async deleteCategory(no) {
        const confirm = await CommonJS.confirm('정말 삭제하시겠습니까? (하위 항목이 있는 경우 삭제되지 않을 수 있습니다)');
        if (!confirm) return;

        try {
            const res = await fetch(`/api/admin/categories/delete?no=${no}`, { method: 'DELETE' });
            if (!res.ok) throw new Error();
            CommonJS.alert('삭제되었습니다.', '성공', 'success', () => {
                this.getDepth1List();
                if (this.state.selectedParentNo) {
                    this.getDepth2List(this.state.selectedParentNo, this.state.selectedParentName);
                }
            });
        } catch (err) {
            CommonJS.alert('삭제 중 오류가 발생했습니다.', '오류', 'error');
        }
    }
};

document.addEventListener('DOMContentLoaded', () => CategoryList.init());
