const ContentList = {
    boardMeta: {
        NOTICE: { pageTitle: '콘텐츠 관리', breadcrumb: '콘텐츠 관리', createLabel: '새 공지 작성', badge: 'NOTICE', description: '운영 공지와 서비스 안내 문서를 관리합니다.', boardLabel: '공지' },
        STYLE: { pageTitle: '스타일 피드', breadcrumb: '스타일 피드', createLabel: '새 스타일 피드 작성', badge: 'STYLE', description: '룩북, 착용 이미지, 큐레이션 피드를 관리합니다.', boardLabel: '스타일' },
        DISCUSS: { pageTitle: '종목 토론방', breadcrumb: '종목 토론방', createLabel: '새 토론 작성', badge: 'DISCUSS', description: '상품별 이슈와 시세 흐름을 다루는 토론 게시글을 관리합니다.', boardLabel: '토론' },
        QNA: { pageTitle: '문의사항', breadcrumb: '문의사항', createLabel: '새 문의 작성', badge: 'QNA', description: '사용자 문의와 응답이 필요한 게시글을 관리합니다.', boardLabel: '문의' }
    },
    state: {
        page: 0,
        size: 9,
        boardType: window.initialContentBoardType || new URLSearchParams(window.location.search).get('boardType') || 'NOTICE'
    },

    init() {
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
                
                // URL 파라미터 업데이트 (새로고침 없이)
                const newUrl = `${window.location.pathname}?boardType=${this.state.boardType}`;
                window.history.pushState({ path: newUrl }, '', newUrl);
                this.updateSidebarActive();
                this.updatePageMeta();
                this.getList();
            });
        });

        window.addEventListener('popstate', () => {
            const params = new URLSearchParams(window.location.search);
            this.state.boardType = params.get('boardType') || 'NOTICE';
            this.state.page = 0;
            this.setInitialTab();
            this.updateSidebarActive();
            this.updatePageMeta();
            this.getList();
        });

        // 새 글 작성 버튼
        document.getElementById('btnNewContent')?.addEventListener('click', () => {
            location.href = `/admin/content/edit?boardType=${this.state.boardType}`;
        });
    },

    updatePageMeta() {
        const meta = this.boardMeta[this.state.boardType] || this.boardMeta.NOTICE;
        const titleEl = document.getElementById('contentPageTitle');
        const breadcrumbEl = document.getElementById('contentBreadcrumb');
        const createLabelEl = document.getElementById('contentCreateLabel');
        const badgeEl = document.getElementById('contentBoardBadge');
        const descEl = document.getElementById('contentBoardDescription');

        if (titleEl) titleEl.textContent = meta.pageTitle;
        if (breadcrumbEl) breadcrumbEl.textContent = meta.breadcrumb;
        if (createLabelEl) createLabelEl.textContent = meta.createLabel;
        if (badgeEl) badgeEl.textContent = meta.badge;
        if (descEl) descEl.textContent = meta.description;
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
                <div class="card h-100 content-board-card">
                    <div class="card-body content-board-card-body">
                        <div class="content-board-card-top">
                            <span class="content-board-card-badge">${this.escapeHtml(this.getBoardLabel(item.boardType))}</span>
                            <span class="content-board-card-views"><i class="far fa-eye me-1"></i>${item.viewCnt}</span>
                        </div>
                        <a class="content-board-card-link" href="/admin/content/get?id=${item.id}&boardType=${item.boardType}">
                            <h5 class="card-title content-board-card-title text-line-clamp-2">${this.escapeHtml(item.title || '제목 없음')}</h5>
                        </a>
                        <p class="content-board-card-copy">${this.escapeHtml(item.contentPreview || '내용 미리보기가 없습니다.')}</p>
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
        const meta = this.boardMeta[boardType];
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

    escapeHtml(value) {
        return String(value)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }
};

document.addEventListener('DOMContentLoaded', () => ContentList.init());
