const ContentDetail = {
    initialized: false,
    isDeleting: false,
    state: {
        id: null,
        boardType: 'NOTICE',
        returnTo: null,
        source: '',
        data: null
    },
    operationPolicy: null,
    operateInFlight: false,

    async init() {
        if (this.initialized) return;
        this.initialized = true;
        const params = new URLSearchParams(window.location.search);
        this.state.id = this.normalizeContentId(window.initialContentDetail?.id || params.get('id'));
        this.state.boardType = ContentBoardConfig.normalizeBoardType(
            window.initialContentDetail?.boardType || params.get('boardType')
        );
        this.state.returnTo = CommonJS.normalizeOptionalText(window.initialContentDetail?.returnTo || params.get('returnTo'));
        this.state.source = CommonJS.normalizeOptionalText(window.initialContentDetail?.source || params.get('source')) || '';

        if (!this.isValidContentId(this.state.id)) {
            await CommonJS.alert('문서 번호가 올바르지 않습니다.', '오류', 'error');
            window.location.href = this.getListPath();
            return;
        }

        this.applyBoardMeta(this.state.boardType);
        CommonJS.renderSourceContextNotice({ noticeId: 'contentDetailSourceContextNotice', source: this.state.source });
        CommonJS.bindMainLogoNavigation(this.getListPath());
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
            if (!this.isValidContentId(this.state.id)) {
                await CommonJS.alert('문서 번호가 올바르지 않습니다.', '알림', 'warning');
                return;
            }
            const returnToQuery = this.state.returnTo ? `&returnTo=${encodeURIComponent(this.state.returnTo)}` : '';
            const sourceQuery = this.state.source ? `&source=${encodeURIComponent(this.state.source)}` : '';
            window.location.href = `/admin/content/edit?id=${this.state.id}&boardType=${this.state.boardType}${sourceQuery}${returnToQuery}`;
        });

        document.getElementById('btnDeleteContent')?.addEventListener('click', () => {
            this.deleteContent();
        });
        document.getElementById('btnOpenContentProduct')?.addEventListener('click', () => this.openLinkedProduct());
        document.getElementById('btnToggleContentStatus')?.addEventListener('click', () => this.toggleContentStatus());
        document.getElementById('btnToggleContentPublic')?.addEventListener('click', () => this.toggleContentPublic());
        document.getElementById('btnToggleContentPinned')?.addEventListener('click', () => this.toggleContentPinned());
    },

    async applyOperationPolicy(settings = null) {
        try {
            this.operationPolicy = settings || await CommonJS.fetchSystemSettings();
            const disabled = CommonJS.isCommunityWriteBlocked(this.operationPolicy);
            const reason = CommonJS.getCommunityWriteBlockedReason(this.operationPolicy, '커뮤니티 수정 및 삭제');

            CommonJS.setButtonDisabled(document.getElementById('btnOpenContentProduct'), false);
            CommonJS.setButtonDisabled(document.getElementById('btnToggleContentStatus'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnToggleContentPublic'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnToggleContentPinned'), disabled, reason);
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
                throw new Error(await CommonJS.extractErrorMessage(response, '상세 내용을 불러오는 중 오류가 발생했습니다.'));
            }

            const data = await response.json();
            this.state.data = data;
            this.state.boardType = ContentBoardConfig.normalizeBoardType(data.boardType || this.state.boardType);
            this.applyBoardMeta(this.state.boardType);
            CommonJS.bindMainLogoNavigation(this.getListPath());
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
        this.renderBodyValue(data.content);
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
        this.setButtonText('btnOpenContentProduct', data.productNo ? `상품 #${data.productNo}` : '연결 상품 없음');
        const nextStatusLabel = data.status === 'PUBLISHED' ? '임시저장' : '게시';
        const nextPublicLabel = data.publicYn === 'Y' ? '비공개' : '공개';
        const nextPinnedLabel = data.pinnedYn === 'Y' ? '고정 해제' : '고정';
        this.setButtonText('btnToggleContentStatus', nextStatusLabel);
        this.setButtonText('btnToggleContentPublic', nextPublicLabel);
        this.setButtonText('btnToggleContentPinned', nextPinnedLabel);
    },

    async openLinkedProduct() {
        const productNo = this.normalizeOptionalProductNo(this.state.data?.productNo);
        if (!productNo) {
            await CommonJS.alert('연결된 상품이 없습니다.', '알림', 'info');
            return;
        }
        const returnTo = encodeURIComponent(window.location.pathname + window.location.search);
        const sourceQuery = this.state.source ? `&source=${encodeURIComponent(this.state.source)}` : '';
        window.location.href = `/admin/products/get?no=${productNo}&returnTo=${returnTo}${sourceQuery}`;
    },

    async toggleContentStatus() {
        const current = this.state.data;
        await this.operateContent({ status: current?.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED' }, '게시 상태');
    },

    async toggleContentPublic() {
        const current = this.state.data;
        await this.operateContent({ publicYn: current?.publicYn === 'Y' ? 'N' : 'Y' }, '공개 상태');
    },

    async toggleContentPinned() {
        const current = this.state.data;
        await this.operateContent({ pinnedYn: current?.pinnedYn === 'Y' ? 'N' : 'Y' }, '고정 상태');
    },

    async operateContent(payload, label) {
        if (this.operateInFlight) return;
        if (this.operationPolicy && CommonJS.isCommunityWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getCommunityWriteBlockedReason(this.operationPolicy, `${label} 변경`), '알림', 'warning');
            return;
        }
        if (!this.isValidContentId(this.state.id)) {
            await CommonJS.alert('문서 번호가 올바르지 않습니다.', '알림', 'warning');
            return;
        }

        try {
            this.operateInFlight = true;
            this.setOperateButtonsDisabled(true);
            const response = await fetch(`/api/admin/content/${this.state.id}/operate`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, `${label} 변경 중 오류가 발생했습니다.`));
            }
            await this.loadDetail();
            await CommonJS.alert(`${label}가 변경되었습니다.`, '성공', 'success');
        } catch (error) {
            console.error('콘텐츠 빠른 변경 실패:', error);
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.operateInFlight = false;
            this.setOperateButtonsDisabled(false);
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    applyBoardMeta(boardType) {
        const normalizedBoardType = ContentBoardConfig.normalizeBoardType(boardType);
        const meta = ContentBoardConfig.getMeta(normalizedBoardType);
        this.setText('contentDetailBadge', meta.badge);
        this.setText('contentDetailBreadcrumb', meta.detail.pageTitle);
        this.setText('contentBoardLabel', meta.detail.label);

        const listLink = document.getElementById('contentListBreadcrumbLink');
        const backButton = document.getElementById('btnBackToList');
        const returnContext = this.state.returnTo
            ? CommonJS.getReturnContext(this.state.returnTo, meta.detail.listTitle)
            : null;
        if (listLink) {
            listLink.textContent = returnContext?.label || meta.detail.listTitle;
            listLink.href = this.getListPath();
        }
        if (backButton) {
            backButton.innerHTML = `<i class="fas fa-list me-2"></i>${returnContext?.buttonLabel || `${meta.detail.listTitle}로`}`;
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
                throw new Error(await CommonJS.extractErrorMessage(response, '삭제 처리 중 오류가 발생했습니다.'));
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

    renderBodyValue(content) {
        const bodyEl = document.getElementById('contentBodyValue');
        if (!bodyEl) {
            return;
        }
        if (!content) {
            bodyEl.innerHTML = `
                <div class="product-empty-state py-4">
                    <i class="fas fa-file-lines product-empty-state-icon"></i>
                    <strong>등록된 본문이 없습니다.</strong>
                    <p>아직 게시글 본문이 비어 있거나 저장되지 않았습니다.</p>
                </div>
            `;
            return;
        }
        bodyEl.textContent = content;
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
    },

    setOperateButtonsDisabled(disabled) {
        ['btnToggleContentStatus', 'btnToggleContentPublic', 'btnToggleContentPinned'].forEach((id) => {
            const button = document.getElementById(id);
            if (button) {
                button.disabled = disabled;
            }
        });
    },

    setButtonText(id, text) {
        const button = document.getElementById(id);
        if (button) {
            button.textContent = text;
        }
    },

    isValidContentId(id) {
        return /^\d+$/.test(String(id || '')) && Number(id) > 0;
    },

    normalizeContentId(id) {
        return this.isValidContentId(id) ? String(Number(id)) : null;
    },

    normalizeOptionalProductNo(productNo) {
        return /^\d+$/.test(String(productNo || '')) && Number(productNo) > 0
            ? String(Number(productNo))
            : null;
    }
};
