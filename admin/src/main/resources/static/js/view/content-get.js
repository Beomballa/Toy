const ContentDetail = {
    boardMeta: {
        NOTICE: { pageTitle: '공지 상세', listTitle: '콘텐츠 관리', label: '공지사항', listPath: '/admin/content/list' },
        STYLE: { pageTitle: '스타일 피드 상세', listTitle: '스타일 피드', label: '스타일 피드', listPath: '/admin/content/list?boardType=STYLE' },
        DISCUSS: { pageTitle: '종목 토론 상세', listTitle: '종목 토론방', label: '종목 토론방', listPath: '/admin/content/list?boardType=DISCUSS' },
        QNA: { pageTitle: '문의 상세', listTitle: '문의사항', label: '문의사항', listPath: '/admin/content/list?boardType=QNA' }
    },
    state: {
        id: null,
        boardType: 'NOTICE',
        data: null
    },

    init() {
        const params = new URLSearchParams(window.location.search);
        this.state.id = window.initialContentDetail?.id || params.get('id');
        this.state.boardType = window.initialContentDetail?.boardType || params.get('boardType') || 'NOTICE';

        if (!this.state.id) {
            CommonJS.alert('문서 번호가 올바르지 않습니다.', '오류', 'error').then(() => {
                window.location.href = this.getListPath();
            });
            return;
        }

        this.applyBoardMeta(this.state.boardType);
        this.bindEvents();
        this.loadDetail();
    },

    bindEvents() {
        document.getElementById('btnBackToList')?.addEventListener('click', () => {
            window.location.href = this.getListPath();
        });

        document.getElementById('btnEditContent')?.addEventListener('click', () => {
            window.location.href = `/admin/content/edit?id=${this.state.id}&boardType=${this.state.boardType}`;
        });

        document.getElementById('btnDeleteContent')?.addEventListener('click', () => {
            this.deleteContent();
        });
    },

    async loadDetail() {
        try {
            const response = await fetch(`/api/admin/content/read?id=${this.state.id}`);
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const data = await response.json();
            this.state.data = data;
            this.state.boardType = data.boardType || this.state.boardType;
            this.applyBoardMeta(this.state.boardType);
            this.renderDetail(data);
        } catch (error) {
            console.error('콘텐츠 상세 로드 실패:', error);
            await CommonJS.alert('상세 내용을 불러오는 중 오류가 발생했습니다.', '오류', 'error');
            window.location.href = this.getListPath();
        }
    },

    renderDetail(data) {
        this.setText('contentDetailTitle', data.title || '제목 없음');
        this.setText('contentTitleValue', data.title || '제목 없음');
        this.setText('contentBodyValue', data.content || '등록된 본문이 없습니다.');
        this.setText('contentIdValue', data.id || '-');
        this.setText('contentProductNoValue', data.productNo || '-');
        this.setText('contentCreatedValue', data.crtDtm || '-');
        this.setText('contentUpdatedValue', data.uptDtm || '-');
        this.setText('contentViewsValue', `${(data.viewCnt ?? 0).toLocaleString()}회`);
        this.setText('contentCreatedMeta', `등록 ${data.crtDtm || '-'}`);
        this.setText('contentUpdatedMeta', `수정 ${data.uptDtm || '-'}`);
        this.setText('contentViewsMeta', `조회 ${(data.viewCnt ?? 0).toLocaleString()}회`);
    },

    applyBoardMeta(boardType) {
        const meta = this.boardMeta[boardType] || this.boardMeta.NOTICE;
        this.setText('contentDetailBadge', boardType);
        this.setText('contentDetailBreadcrumb', meta.pageTitle);
        this.setText('contentBoardLabel', meta.label);

        const listLink = document.getElementById('contentListBreadcrumbLink');
        if (listLink) {
            listLink.textContent = meta.listTitle;
            listLink.href = meta.listPath;
        }
    },

    async deleteContent() {
        const isConfirm = await CommonJS.confirm('정말로 이 게시글을 삭제하시겠습니까?', '콘텐츠 삭제 확인', 'error');
        if (!isConfirm) return;

        try {
            const response = await fetch(`/api/admin/content/delete?id=${this.state.id}`, {
                method: 'DELETE'
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            await CommonJS.alert('삭제되었습니다.', '성공', 'success');
            window.location.href = this.getListPath();
        } catch (error) {
            console.error('콘텐츠 삭제 실패:', error);
            await CommonJS.alert('삭제 처리 중 오류가 발생했습니다.', '오류', 'error');
        }
    },

    getListPath() {
        return (this.boardMeta[this.state.boardType] || this.boardMeta.NOTICE).listPath;
    },

    setText(id, value) {
        const el = document.getElementById(id);
        if (el) {
            el.textContent = value;
        }
    }
};
