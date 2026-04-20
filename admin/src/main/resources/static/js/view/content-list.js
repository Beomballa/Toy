const ContentList = {
    state: {
        page: 0,
        size: 9,
        boardType: new URLSearchParams(window.location.search).get('boardType') || 'NOTICE'
    },

    init() {
        this.setInitialTab();
        this.bindEvents();
        this.getList();
    },

    setInitialTab() {
        document.querySelectorAll('.nav-link[data-board-type]').forEach(el => {
            if (el.dataset.boardType === this.state.boardType) {
                el.classList.add('active');
            } else {
                el.classList.remove('active');
            }
        });
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
                
                // URL 파라미터 업데이트 (새로고침 없이)
                const newUrl = `${window.location.pathname}?boardType=${this.state.boardType}`;
                window.history.pushState({ path: newUrl }, '', newUrl);
                
                this.getList();
            });
        });

        // 새 글 작성 버튼
        document.getElementById('btnNewContent')?.addEventListener('click', () => {
            location.href = `/admin/content/edit?boardType=${this.state.boardType}`;
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
            this.renderList(data.items);
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
            grid.innerHTML = `
                <div class="col-12 text-center py-5">
                    <div class="mb-3 text-muted"><i class="fas fa-folder-open fa-3x opacity-25"></i></div>
                    <div class="text-muted">등록된 콘텐츠가 없습니다.</div>
                </div>`;
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
