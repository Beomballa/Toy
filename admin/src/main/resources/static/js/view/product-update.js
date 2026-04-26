const ProductUpdate = {
    optionCount: 0,
    productNo: null,

    init() {
        const urlParams = new URLSearchParams(window.location.search);
        this.productNo = urlParams.get('no');

        if (!this.productNo) {
            CommonJS.alert('상품 번호가 유효하지 않습니다.', '오류', 'error').then(() => history.back());
            return;
        }

        this.loadProductData();
        this.bindEvents();

        document.getElementById("main-logo")?.addEventListener("click", () => {
            window.location.href = "/admin/products";
        });
    },

    bindEvents() {
        document.getElementById('btnUpdate').addEventListener('click', () => this.updateForm());
        document.getElementById('btnAddOption').addEventListener('click', () => this.addOption());

        const previewIds = ['categoryNo', 'brandNo', 'nameKo', 'modelNum', 'releasePrice', 'thumbnailUrl'];
        previewIds.forEach(id => {
            const el = document.getElementById(id);
            if (!el) return;
            const eventType = el.tagName === 'SELECT' ? 'change' : 'input';
            el.addEventListener(eventType, () => this.updatePreview());
        });
    },

    async loadProductData() {
        try {
            const response = await fetch(`/api/admin/product/get?no=${this.productNo}`);
            if (!response.ok) throw new Error('상품 정보를 불러오는데 실패했습니다.');

            const data = await response.json();
            this.fillForm(data);
            this.updatePreview();
        } catch (error) {
            console.error('Data Load Error:', error);
            await CommonJS.alert('데이터 로드 중 오류가 발생했습니다.', '오류', 'error');
        }
    },

    fillForm(data) {
        document.getElementById('productNo').value = data.productNo;
        document.getElementById('categoryNo').value = data.categoryNo || '';
        document.getElementById('brandNo').value = data.brandNo || '';
        document.getElementById('nameKo').value = data.productName || '';
        document.getElementById('modelNum').value = data.productModel || '';
        document.getElementById('releasePrice').value = data.releasePrice || 0;
        document.getElementById('releaseDt').value = data.releaseDt || '';
        document.getElementById('thumbnailUrl').value = data.thumbnailUrl || '';
        document.getElementById('productStatus').value = data.status || 'ACTIVE';

        const tbody = document.getElementById('optionTableBody');
        tbody.innerHTML = '';
        if (data.options && data.options.length > 0) {
            data.options.forEach(opt => {
                this.addOption(opt.optionName, opt.stockQty, opt.additionalPrice);
            });
        } else {
            this.showEmptyOptionMessage();
        }
    },

    addOption(name = '', qty = 0, addPrice = 0) {
        this.optionCount++;
        const optionId = this.optionCount;
        const lowStockClass = qty < 10 ? 'text-danger fw-bold' : '';

        const optionHtml = `
            <tr class="option-item" data-option-id="${optionId}">
                <td>
                    <input type="text" class="form-control form-control-sm option-name" placeholder="예: 250" value="${name}" required>
                </td>
                <td>
                    <div class="input-group input-group-sm">
                        <input type="number" class="form-control option-price" placeholder="0" min="0" value="${addPrice}" required>
                        <span class="input-group-text">원</span>
                    </div>
                </td>
                <td>
                    <div class="input-group input-group-sm">
                        <input type="number" class="form-control option-cnt ${lowStockClass}" placeholder="0" min="0" value="${qty}" required>
                        <span class="input-group-text">개</span>
                    </div>
                </td>
                <td class="text-end">
                    <button type="button" class="btn btn-sm btn-outline-danger btn-remove-option" data-option-id="${optionId}">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            </tr>
        `;

        const tbody = document.getElementById('optionTableBody');
        if (tbody.querySelector('.alert-info')) {
             tbody.innerHTML = '';
        }
        tbody.insertAdjacentHTML('beforeend', optionHtml);

        const row = tbody.querySelector(`tr[data-option-id="${optionId}"]`);
        row.querySelector('.option-cnt').addEventListener('input', (e) => {
            if (parseInt(e.target.value) < 10) e.target.classList.add('text-danger', 'fw-bold');
            else e.target.classList.remove('text-danger', 'fw-bold');
        });

        row.querySelector('.btn-remove-option').addEventListener('click', () => {
            row.remove();
            if (document.querySelectorAll('.option-item').length === 0) {
                this.showEmptyOptionMessage();
                this.optionCount = 0;
            }
        });
    },

    showEmptyOptionMessage() {
        document.getElementById('optionTableBody').innerHTML = `
            <tr><td colspan="4" class="text-center py-4 text-muted"><i class="fas fa-info-circle me-2"></i>상품 옵션을 추가해주세요.</td></tr>
        `;
    },

    updatePreview() {
        const categorySelect = document.getElementById('categoryNo');
        const brandSelect = document.getElementById('brandNo');
        const price = document.getElementById('releasePrice').value;
        document.getElementById('previewPrice').textContent = price ? parseInt(price).toLocaleString() + '원' : '-';
        document.getElementById('previewCategory').textContent = categorySelect.options[categorySelect.selectedIndex]?.text || '-';
        document.getElementById('previewBrand').textContent = brandSelect.options[brandSelect.selectedIndex]?.text || '-';
        document.getElementById('previewName').textContent = document.getElementById('nameKo').value || '-';
        document.getElementById('previewModel').textContent = document.getElementById('modelNum').value || '-';
        document.getElementById('previewStatus').textContent = document.getElementById('productStatus').value || 'ACTIVE';

        const url = document.getElementById('thumbnailUrl').value;
        const previewImage = document.getElementById('previewImage');
        const previewText = document.getElementById('previewText');

        if (url) {
            previewImage.src = url;
            previewImage.classList.remove('d-none');
            previewText.classList.add('d-none');
        } else {
            previewImage.src = '';
            previewImage.classList.add('d-none');
            previewText.classList.remove('d-none');
        }
    },

    async updateForm() {
        const categoryNo = document.getElementById('categoryNo').value;
        const brandNo = document.getElementById('brandNo').value;
        const nameKo = document.getElementById('nameKo').value;
        const releasePrice = document.getElementById('releasePrice').value;

        if (!categoryNo || !brandNo || !nameKo || !releasePrice) {
            await CommonJS.alert('필수 항목을 모두 입력해주세요.', '알림', 'warning');
            return;
        }

        const isConfirm = await CommonJS.confirm('상품 정보를 수정하시겠습니까?', '상품 수정 확인');
        if (!isConfirm) return;

        const options = [];
        document.querySelectorAll('.option-item').forEach(item => {
            const optionName = item.querySelector('.option-name').value.trim();
            const stockCnt = parseInt(item.querySelector('.option-cnt').value);
            const additionalPrice = parseInt(item.querySelector('.option-price').value) || 0;
            if (optionName) {
                options.push({ optionName, stockCnt, additionalPrice });
            }
        });

        const data = {
            productNo: parseInt(this.productNo),
            categoryNo: parseInt(categoryNo),
            brandNo: parseInt(brandNo),
            nameKo: nameKo,
            modelNum: document.getElementById('modelNum').value || null,
            releasePrice: parseInt(releasePrice),
            releaseDt: document.getElementById('releaseDt').value || null,
            thumbnailUrl: document.getElementById('thumbnailUrl').value || null,
            status: document.getElementById('productStatus').value,
            options: options
        };

        try {
            const response = await fetch('/api/admin/product/update', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (response.ok) {
                await CommonJS.alert('상품 정보가 성공적으로 수정되었습니다.', '성공', 'success');
                window.location.href = `/admin/products/get?no=${this.productNo}`;
            } else {
                const err = await response.json();
                await CommonJS.alert('수정 실패: ' + (err.message || '알 수 없는 오류'), '오류', 'error');
            }
        } catch (error) {
            console.error('Update Error:', error);
            await CommonJS.alert('수정 중 오류가 발생했습니다.', '오류', 'error');
        }
    }
};
