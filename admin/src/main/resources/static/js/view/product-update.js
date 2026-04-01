const ProductUpdate = {
    optionCount: 0,
    productNo: null,
    brands: [],
    categories: [],

    init(brands, categories) {
        this.brands = brands || [];
        this.categories = categories || [];

        // URL에서 상품 번호(no) 추출
        const urlParams = new URLSearchParams(window.location.search);
        this.productNo = urlParams.get('no');

        if (!this.productNo) {
            alert('상품 번호가 유효하지 않습니다.');
            history.back();
            return;
        }

        // 초기 렌더링 및 데이터 로드
        this.renderSelects();
        this.loadProductData();
        this.bindEvents();

        document.getElementById("main-logo").addEventListener("click", () => {
            window.location.href = "/admin/products";
        });
    },

    renderSelects() {
        const brandSelect = document.getElementById('brandNo');
        this.brands.forEach(brand => {
            const option = document.createElement('option');
            option.value = brand.brandNo;
            option.textContent = brand.nameKo;
            brandSelect.appendChild(option);
        });

        const categorySelect = document.getElementById('categoryNo');
        this.categories.forEach(category => {
            const option = document.createElement('option');
            option.value = category.categoryNo;
            option.textContent = category.name;
            categorySelect.appendChild(option);
        });
    },

    bindEvents() {
        document.getElementById('btnUpdate').addEventListener('click', () => this.updateForm());
        document.getElementById('btnAddOption').addEventListener('click', () => this.addOption());

        // 실시간 미리보기 이벤트 바인딩 (product-set.js와 유사)
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
            alert('데이터 로드 중 오류가 발생했습니다.');
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
        document.getElementById('status').value = data.status || 'ACTIVE';

        // 옵션 데이터 렌더링
        if (data.options && data.options.length > 0) {
            const optionList = document.getElementById('optionList');
            optionList.innerHTML = '';
            data.options.forEach(opt => {
                this.addOption(opt.optionName, opt.stockQty);
            });
        } else {
            this.showEmptyOptionMessage();
        }
    },

    addOption(name = '', qty = 0) {
        this.optionCount++;
        const optionId = this.optionCount;

        const optionHtml = `
            <div class="option-item mb-2" data-option-id="${optionId}">
                <div class="row g-2 align-items-center">
                    <div class="col-sm-6">
                        <div class="input-group">
                            <span class="input-group-text bg-light text-muted small">사이즈</span>
                            <input type="text" class="form-control option-name" placeholder="예: 250" value="${name}" required>
                        </div>
                    </div>
                    <div class="col-sm-5">
                        <div class="input-group">
                            <span class="input-group-text bg-light text-muted small">수량</span>
                            <input type="number" class="form-control option-cnt" placeholder="0" min="0" value="${qty}" required>
                            <span class="input-group-text bg-light text-muted small">개</span>
                        </div>
                    </div>
                    <div class="col-sm-1 text-end">
                        <button type="button" class="btn btn-outline-danger btn-remove-option w-100" data-option-id="${optionId}">
                            <i class="fas fa-times"></i>
                        </button>
                    </div>
                </div>
            </div>
        `;

        const optionList = document.getElementById('optionList');
        if (this.optionCount === 1 && !name) {
             // 수동 추가 시 첫 번째면 비우기
             optionList.innerHTML = '';
        } else if (optionList.querySelector('.alert-info')) {
             optionList.innerHTML = '';
        }

        optionList.insertAdjacentHTML('beforeend', optionHtml);

        const removeBtn = optionList.querySelector(`[data-option-id="${optionId}"].btn-remove-option`);
        removeBtn.addEventListener('click', (e) => {
            const id = e.currentTarget.getAttribute('data-option-id');
            document.querySelector(`.option-item[data-option-id="${id}"]`).remove();
            if (document.querySelectorAll('.option-item').length === 0) {
                this.showEmptyOptionMessage();
                this.optionCount = 0;
            }
        });
    },

    showEmptyOptionMessage() {
        document.getElementById('optionList').innerHTML = `
            <div class="alert alert-info mb-0">
                <i class="fas fa-info-circle me-2"></i>
                상품 옵션을 추가해주세요.
            </div>
        `;
    },

    updatePreview() {
        const categorySelect = document.getElementById('categoryNo');
        document.getElementById('previewCategory').textContent = categorySelect.options[categorySelect.selectedIndex]?.text || '-';

        const brandSelect = document.getElementById('brandNo');
        document.getElementById('previewBrand').textContent = brandSelect.options[brandSelect.selectedIndex]?.text || '-';

        document.getElementById('previewName').textContent = document.getElementById('nameKo').value || '-';
        document.getElementById('previewModel').textContent = document.getElementById('modelNum').value || '-';

        const price = document.getElementById('releasePrice').value;
        document.getElementById('previewPrice').textContent = price ? parseInt(price).toLocaleString() + '원' : '-';

        const url = document.getElementById('thumbnailUrl').value;
        document.getElementById('previewImage').src = url || 'https://via.placeholder.com/300x300?text=No+Image';
    },

    async updateForm() {
        const categoryNo = document.getElementById('categoryNo').value;
        const brandNo = document.getElementById('brandNo').value;
        const nameKo = document.getElementById('nameKo').value;
        const releasePrice = document.getElementById('releasePrice').value;

        if (!categoryNo || !brandNo || !nameKo || !releasePrice) {
            alert('필수 항목을 모두 입력해주세요.');
            return;
        }

        const options = [];
        document.querySelectorAll('.option-item').forEach(item => {
            const optionName = item.querySelector('.option-name').value.trim();
            const stockCnt = parseInt(item.querySelector('.option-cnt').value);
            if (optionName) {
                options.push({ optionName, stockCnt });
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
            status: document.getElementById('status').value,
            options: options
        };

        try {
            // 수정 API 호출 (POST /product/update 가 있다고 가정하거나 새로 만들어야 함)
            const response = await fetch('/api/admin/product/update', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (response.ok) {
                alert('상품 정보가 수정되었습니다.');
                window.location.href = `/product/get?no=${this.productNo}`;
            } else {
                const err = await response.json();
                alert('수정 실패: ' + (err.message || '알 수 없는 오류'));
            }
        } catch (error) {
            console.error('Update Error:', error);
            alert('수정 중 오류가 발생했습니다.');
        }
    }
};