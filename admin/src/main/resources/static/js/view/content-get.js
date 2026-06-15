const ContentDetail = {
    initialized: false,
    isDeleting: false,
    state: {
        id: null,
        boardType: 'NOTICE',
        returnTo: null,
        data: null
    },
    operationPolicy: null,

    async init() {
        if (this.initialized) return;
        this.initialized = true;
        const params = new URLSearchParams(window.location.search);
        this.state.id = window.initialContentDetail?.id || params.get('id');
        this.state.boardType = ContentBoardConfig.normalizeBoardType(
            window.initialContentDetail?.boardType || params.get('boardType')
        );
        this.state.returnTo = window.initialContentDetail?.returnTo || params.get('returnTo');

        if (!this.state.id) {
            await CommonJS.alert('문서 번호가 올바르지 않습니다.', '오류', 'error');
            window.location.href = this.getListPath();
            return;
        }

        this.applyBoardMeta(this.state.boardType);
        this.bindEvents();
        this.applyOperationPolicy();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));
        await this.loadDetail();
    },

    bindEvents() {
        document.getElementById('btnBackToList')?.addEventListener('click', () => {
            window.location.href = this.getListPath();
        });

        document.getElementById('btnEditContent')?.addEventListener('click', async () => {
            if (this.operationPolicy && CommonJS.isCommunityWriteBlocked(this.operationPolicy)) {
                await CommonJS.alert(CommonJS.getCommunityWriteBlockedReason(this.operationPolicy, '커뮤니티 수정'), '알림', 'warning');
                return;
            }
            const returnToQuery = this.state.returnTo ? `&returnTo=${encodeURIComponent(this.state.returnTo)}` : '';
            window.location.href = `/admin/content/edit?id=${this.state.id}&boardType=${this.state.boardType}${returnToQuery}`;
        });

        document.getElementById('btnDeleteContent')?.addEventListener('click', () => {
            this.deleteContent();
        });
    },

    async applyOperationPolicy(settings = null) {
        try {
            this.operationPolicy = settings || await CommonJS.fetchSystemSettings();
            const disabled = CommonJS.isCommunityWriteBlocked(this.operationPolicy);
            const reason = CommonJS.getCommunityWriteBlockedReason(this.operationPolicy, '커뮤니티 수정 및 삭제');

            CommonJS.setButtonDisabled(document.getElementById('btnEditContent'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnDeleteContent'), disabled, reason);
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
    },

    async loadDetail() {
        try {
            const response = await fetch(`/api/admin/content/get?id=${this.state.id}`);
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
        this.setText('contentStatusValue', data.status === 'PUBLISHED' ? '게시중' : '임시저장');
        this.setText('contentPublicValue', data.publicYn === 'Y' ? '공개' : '비공개');
        this.setText('contentPinnedValue', data.pinnedYn === 'Y' ? '고정글' : '일반글');
        this.setText('contentCreatedValue', data.crtDtm || '-');
        this.setText('contentUpdatedValue', data.uptDtm || '-');
        this.setText('contentViewsValue', `${(data.viewCnt ?? 0).toLocaleString()}회`);
        this.setText('contentCreatedMeta', `등록 ${data.crtDtm || '-'}`);
        this.setText('contentUpdatedMeta', `수정 ${data.uptDtm || '-'}`);
        this.setText('contentViewsMeta', `조회 ${(data.viewCnt ?? 0).toLocaleString()}회`);
    },

    applyBoardMeta(boardType) {
        const normalizedBoardType = ContentBoardConfig.normalizeBoardType(boardType);
        const meta = ContentBoardConfig.getMeta(normalizedBoardType);
        this.setText('contentDetailBadge', meta.badge);
        this.setText('contentDetailBreadcrumb', meta.detail.pageTitle);
        this.setText('contentBoardLabel', meta.detail.label);

        const listLink = document.getElementById('contentListBreadcrumbLink');
        if (listLink) {
            listLink.textContent = meta.detail.listTitle;
            listLink.href = meta.listPath;
        }
    },

    async deleteContent() {
        if (this.isDeleting) return;
        if (this.operationPolicy && CommonJS.isCommunityWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getCommunityWriteBlockedReason(this.operationPolicy, '커뮤니티 삭제'), '알림', 'warning');
            return;
        }

        const isConfirm = await CommonJS.confirm('정말로 이 게시글을 삭제하시겠습니까?', '콘텐츠 삭제 확인', 'error');
        if (!isConfirm) return;

        try {
            this.isDeleting = true;
            this.setBusyButton(document.getElementById('btnDeleteContent'), true, '삭제 중...');
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
        } finally {
            this.isDeleting = false;
            this.setBusyButton(document.getElementById('btnDeleteContent'), false);
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    getListPath() {
        return this.state.returnTo || ContentBoardConfig.getListPath(this.state.boardType);
    },

    setText(id, value) {
        const el = document.getElementById(id);
        if (el) {
            el.textContent = value;
        }
    },

    setBusyButton(button, isBusy, busyText = '처리 중...') {
        if (!button) return;
        if (isBusy) {
            if (!button.dataset.originalText) {
                button.dataset.originalText = button.textContent;
            }
            button.disabled = true;
            button.textContent = busyText;
            return;
        }
        button.disabled = false;
        if (button.dataset.originalText) {
            button.textContent = button.dataset.originalText;
            delete button.dataset.originalText;
        }
    }
};
