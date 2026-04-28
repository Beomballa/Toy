const ContentList = {
    state: {
        page: 0,
        size: 9,
        boardType: ContentBoardConfig.normalizeBoardType(
            window.initialContentBoardType || new URLSearchParams(window.location.search).get('boardType')
        ),
        keyword: new URLSearchParams(window.location.search).get('keyword') || ''
    },

    init() {
        this.syncSearchField();
        this.setInitialTab();
        this.updateSidebarActive();
        this.updatePageMeta();
        this.bindEvents();
        this.getList();
    },

    setInitialTab() {
        document.querySelectorAll('.content-board-tab[data-board-type]').forEach(el => {
            if (el.dataset.boardType === this.state.boardType) {
                el.classList.add('active');
            } else {
                el.classList.remove('active');
            }
        });
    },

    updateSidebarActive() {
        document.querySelectorAll('.nav-link[data-community-nav]').forEach(el => {
            const boardType = el.dataset.communityNav;
            if (boardType === this.state.boardType) {
                el.classList.add('active');
            } else {
                el.classList.remove('active');
            }
        });
    },

    bindEvents() {
        // 보드 타입 탭 클릭
        document.querySelectorAll('.content-board-tab[data-board-type]').forEach(el => {
            el.addEventListener('click', (e) => {
                e.preventDefault();
                document.querySelectorAll('.content-board-tab[data-board-type]').forEach(link => link.classList.remove('active'));
                el.classList.add('active');
                this.state.boardType = el.dataset.boardType;
                this.state.page = 0;

                this.pushState();
                this.updateSidebarActive();
                this.updatePageMeta();
                this.getList();
            });
        });

        window.addEventListener('popstate', () => {
            const params = new URLSearchParams(window.location.search);
            // 히스토리 이동 시 URL이 현재 게시판 상태의 기준이 된다.
            this.state.boardType = ContentBoardConfig.normalizeBoardType(params.get('boardType'));
            this.state.keyword = params.get('keyword') || '';
            this.state.page = 0;
            this.syncSearchField();
            this.setInitialTab();
            this.updateSidebarActive();
            this.updatePageMeta();
            this.getList();
        });

        // 새 글 작성 버튼
        document.getElementById('btnNewContent')?.addEventListener('click', () => {
            location.href = `/admin/content/edit?boardType=${this.state.boardType}`;
        });

        document.getElementById('contentSearchForm')?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.state.keyword = document.getElementById('contentSearchKeyword')?.value.trim() || '';
            this.state.page = 0;
            this.pushState();
            this.getList();
        });

        document.getElementById('btnResetContentSearch')?.addEventListener('click', () => {
            this.state.keyword = '';
            this.state.page = 0;
            this.syncSearchField();
            this.pushState();
            this.getList();
        });
    },

    updatePageMeta() {
        const meta = ContentBoardConfig.getMeta(this.state.boardType).list;
        const badge = ContentBoardConfig.getMeta(this.state.boardType).badge;
        const titleEl = document.getElementById('contentPageTitle');
        const breadcrumbEl = document.getElementById('contentBreadcrumb');
        const createLabelEl = document.getElementById('contentCreateLabel');
        const badgeEl = document.getElementById('contentBoardBadge');
        const descEl = document.getElementById('contentBoardDescription');

        if (titleEl) titleEl.textContent = meta.pageTitle;
        if (breadcrumbEl) breadcrumbEl.textContent = meta.breadcrumb;
        if (createLabelEl) createLabelEl.textContent = meta.createLabel;
        if (badgeEl) badgeEl.textContent = badge;
        if (descEl) descEl.textContent = meta.description;
    },

    async getList() {
        const params = new URLSearchParams({
            page: this.state.page,
            size: this.state.size,
            boardType: this.state.boardType
        });
        if (this.state.keyword) {
            params.set('keyword', this.state.keyword);
        }

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
                    <div class="card h-100 content-board-card">
                    <div class="card-body content-board-card-body">
                        <div class="content-board-card-top">
                            <span class="content-board-card-badge">${ContentBoardConfig.escapeHtml(this.getBoardLabel(item.boardType))}</span>
                            <span class="content-board-card-views"><i class="far fa-eye me-1"></i>${item.viewCnt}</span>
                        </div>
                        <a class="content-board-card-link" href="/admin/content/get?id=${item.id}&boardType=${item.boardType}">
                            <h5 class="card-title content-board-card-title text-line-clamp-2">${ContentBoardConfig.escapeHtml(item.title || '제목 없음')}</h5>
                        </a>
                        <p class="content-board-card-copy">${ContentBoardConfig.escapeHtml(item.contentPreview || '내용 미리보기가 없습니다.')}</p>
                    </div>
                    <div class="card-footer content-board-card-footer">
                        <span class="content-board-card-date">${item.crtDtm}</span>
                        <div class="content-board-card-actions">
                            <button class="btn btn-sm btn-light" onclick="location.href='/admin/content/get?id=${item.id}&boardType=${item.boardType}'">상세</button>
                            <button class="btn btn-sm btn-outline-primary" onclick="location.href='/admin/content/edit?id=${item.id}&boardType=${item.boardType}'">수정</button>
                        </div>
                    </div>
                </div>
            </div>
        `).join('');
    },

    getBoardLabel(boardType) {
        const meta = ContentBoardConfig.getMeta(boardType).list;
        return meta ? meta.boardLabel : boardType;
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
    },

    pushState() {
        const params = new URLSearchParams({ boardType: this.state.boardType });
        if (this.state.keyword) {
            params.set('keyword', this.state.keyword);
        }
        const newUrl = `${window.location.pathname}?${params.toString()}`;
        window.history.pushState({ path: newUrl }, '', newUrl);
    },

    syncSearchField() {
        const searchInput = document.getElementById('contentSearchKeyword');
        if (searchInput) {
            searchInput.value = this.state.keyword;
        }
    }
};

document.addEventListener('DOMContentLoaded', () => ContentList.init());
