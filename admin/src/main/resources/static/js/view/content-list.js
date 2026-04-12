const ContentList = {
    state: {
        page: 0,
        size: 9,
        boardType: 'NOTICE'
    },

    init() {
        this.bindEvents();
        this.getList();
    },

    bindEvents() {
        // 보드 타입 탭 클릭
        document.querySelectorAll('.nav-link[data-board-type]').forEach(el => {
            el.addEventListener('click', (e) => {
                e.preventDefault();
                document.querySelectorAll('.nav-link[data-board-type]').forEach(link => link.classList.remove('active'));
                el.classList.add('active');
                this.state.boardType = el.dataset.boardType;
                this.state.page = 0;
                this.getList();
            });
        });

        // 새 글 작성 버튼
        document.getElementById('btnNewContent')?.addEventListener('click', () => {
            location.href = '/admin/content/edit';
        });
    },

    async getList() {
        const params = new URLSearchParams({
            page: this.state.page,
            size: this.state.size,
            boardType: this.state.boardType
        });

        try {
            const res = await fetch(`/api/admin/content/list?${params}`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            const data = await res.json();
            this.renderList(data.contents);
            this.renderPagination(data);
        } catch (err) {
            console.error('콘텐츠 목록 로드 실패:', err);
            CommonJS.alert('목록을 불러오는 중 오류가 발생했습니다.', '오류', 'error');
        }
    },

    renderList(items) {
        const grid = document.getElementById('contentGrid');
        if (!grid) return;

        if (!items || items.length === 0) {
            grid.innerHTML = '<div class="col-12 text-center py-5 text-muted">등록된 게시물이 없습니다.</div>';
            return;
        }

        grid.innerHTML = items.map(item => `
            <div class="col-md-4 mb-4">
                <div class="card h-100 shadow-sm hover-card">
                    <div class="card-body">
                        <div class="d-flex justify-content-between mb-2">
                            <span class="badge bg-light text-primary">${item.boardType}</span>
                            <span class="text-muted small"><i class="far fa-eye me-1"></i>${item.viewCnt}</span>
                        </div>
                        <h5 class="card-title fw-bold text-truncate">${item.title}</h5>
                        <p class="card-text text-muted small text-line-clamp-2">클릭하여 상세 내용을 확인하고 수정하세요.</p>
                    </div>
                    <div class="card-footer bg-white border-top-0 d-flex justify-content-between align-items-center pb-3">
                        <span class="text-muted small">${item.crtDtm}</span>
                        <button class="btn btn-sm btn-outline-primary" onclick="location.href='/admin/content/edit?id=${item.id}'">수정</button>
                    </div>
                </div>
            </div>
        `).join('');
    },

    renderPagination(data) {
        const { totalPages, currentPage: curr } = data;
        const pagination = document.getElementById('pagination');
        if (!pagination) return;

        let html = '';
        for (let i = 0; i < totalPages; i++) {
            html += `
                <li class="page-item ${i === curr ? 'active' : ''}">
                    <a class="page-link" href="javascript:void(0);" onclick="ContentList.goPage(${i})">${i + 1}</a>
                </li>`;
        }
        pagination.innerHTML = html;
    },

    goPage(page) {
        this.state.page = page;
        this.getList();
    }
};

document.addEventListener('DOMContentLoaded', () => ContentList.init());
