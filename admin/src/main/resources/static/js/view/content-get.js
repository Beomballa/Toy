const ContentDetail = {
    initialized: false,
    isDeleting: false,
    state: {
        id: null,
        boardType: 'NOTICE',
        returnTo: null,
        source: '',
        data: null,
        reactionRangeDays: 30,
        reactionInsight: null
    },
    operationPolicy: null,
    operateInFlight: false,
    reactionRequestId: 0,

    async init() {
        if (this.initialized) return;
        this.initialized = true;
        const params = new URLSearchParams(window.location.search);
        this.state.id = this.normalizeContentId(window.initialContentDetail?.id || params.get('id'));
        this.state.boardType = ContentBoardConfig.normalizeBoardType(
            window.initialContentDetail?.boardType || params.get('boardType')
        );
        this.state.returnTo = CommonJS.normalizeAdminReturnPath(
            window.initialContentDetail?.returnTo || params.get('returnTo'),
            ''
        );
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
        const detailLoaded = await this.loadDetail();
        if (detailLoaded) {
            await this.loadReactionInsight();
        }
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
        document.querySelectorAll('[data-content-reaction-range]').forEach((button) => {
            button.addEventListener('click', () => {
                const rangeDays = Number(button.dataset.contentReactionRange);
                if (![7, 30, 90].includes(rangeDays) || rangeDays === this.state.reactionRangeDays) return;
                this.state.reactionRangeDays = rangeDays;
                this.syncReactionRangeButtons();
                this.loadReactionInsight();
            });
        });
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
            return true;
        } catch (error) {
            console.error('콘텐츠 상세 로드 실패:', error);
            await CommonJS.alert('상세 내용을 불러오는 중 오류가 발생했습니다.', '오류', 'error');
            window.location.href = this.getListPath();
            return false;
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
        const analysisLink = document.getElementById('contentReactionDetailListLink');
        if (analysisLink) {
            analysisLink.href = `/admin/content/list?boardType=${encodeURIComponent(this.state.boardType)}&source=content-reaction-detail`;
        }
    },

    async loadReactionInsight() {
        const requestId = ++this.reactionRequestId;
        this.renderReactionInsightLoading();
        try {
            const response = await fetch(
                `/api/admin/content/${this.state.id}/reactions?days=${this.state.reactionRangeDays}`,
                { headers: { Accept: 'application/json' } }
            );
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '반응 인사이트를 불러오지 못했습니다.'));
            }
            const insight = await response.json();
            if (requestId !== this.reactionRequestId) return;
            this.state.reactionInsight = insight;
            this.renderReactionInsight();
        } catch (error) {
            if (requestId !== this.reactionRequestId) return;
            this.state.reactionInsight = null;
            this.renderReactionInsightError();
            console.error('콘텐츠 반응 인사이트 조회 실패:', error);
        }
    },

    renderReactionInsightLoading() {
        this.setText('contentReactionDetailStatus', `최근 ${this.state.reactionRangeDays}일 반응 활동을 집계하고 있습니다.`);
        this.setText('contentReactionDetailTotal', '-');
        this.setText('contentReactionDetailHelpful', '-');
        this.setText('contentReactionDetailNotHelpful', '-');
        this.setText('contentReactionDetailRate', '-');
        this.setText('contentReactionDetailRecent', '-');
        this.setText('contentReactionDetailRangeLabel', `최근 ${this.state.reactionRangeDays}일`);
        this.renderReactionInsightTrend([]);
        this.syncReactionRangeButtons();
    },

    renderReactionInsight() {
        const insight = this.state.reactionInsight || {};
        this.setText(
            'contentReactionDetailStatus',
            `${insight.startDate || '-'} ~ ${insight.endDate || '-'} · 현재 누계와 최근 활동을 함께 표시합니다.`
        );
        this.setText('contentReactionDetailTotal', `${this.formatNumber(insight.totalCount)}건`);
        this.setText('contentReactionDetailHelpful', `${this.formatNumber(insight.helpfulCount)}건`);
        this.setText('contentReactionDetailNotHelpful', `${this.formatNumber(insight.notHelpfulCount)}건`);
        this.setText('contentReactionDetailRate', `${this.formatNumber(insight.helpfulRate)}%`);
        this.setText('contentReactionDetailRecent', `${this.formatNumber(insight.recentActivityCount)}건`);
        this.setText('contentReactionDetailRangeLabel', `최근 ${insight.rangeDays || this.state.reactionRangeDays}일`);
        this.renderReactionInsightTrend(Array.isArray(insight.trend) ? insight.trend : []);
        this.renderReactionDecision(insight);
        this.syncReactionRangeButtons();
    },

    renderReactionInsightTrend(trend) {
        const target = document.getElementById('contentReactionDetailTrend');
        if (!target) return;
        if (!trend.length) {
            target.innerHTML = '<div class="content-view-empty">표시할 반응 활동이 없습니다.</div>';
            return;
        }
        const maxValue = Math.max(1, ...trend.map((item) => Number(item.totalCount) || 0));
        target.innerHTML = trend.map((item, index) => {
            const helpful = Number(item.helpfulCount) || 0;
            const notHelpful = Number(item.notHelpfulCount) || 0;
            const helpfulHeight = Math.max(helpful > 0 ? 8 : 2, Math.round(helpful / maxValue * 100));
            const notHelpfulHeight = Math.max(notHelpful > 0 ? 8 : 2, Math.round(notHelpful / maxValue * 100));
            const showLabel = trend.length <= 7 || index === 0 || index === trend.length - 1
                || index % (trend.length > 30 ? 15 : 5) === 0;
            return `
                <div class="content-view-chart__column"
                     title="${ContentBoardConfig.escapeHtml(item.date)} · 도움됨 ${this.formatNumber(helpful)}건 · 개선 필요 ${this.formatNumber(notHelpful)}건">
                    <div class="content-view-chart__value">${item.totalCount ? this.formatNumber(item.totalCount) : ''}</div>
                    <div class="content-view-chart__bars">
                        <span class="content-view-chart__bar content-reaction-bar--helpful" style="height:${helpfulHeight}%"></span>
                        <span class="content-view-chart__bar content-reaction-bar--not-helpful" style="height:${notHelpfulHeight}%"></span>
                    </div>
                    <span class="content-view-chart__date">${showLabel ? this.formatShortDate(item.date) : ''}</span>
                </div>
            `;
        }).join('');
    },

    renderReactionDecision(insight) {
        const target = document.getElementById('contentReactionDetailDecision');
        if (!target) return;
        const status = ['HEALTHY', 'IMPROVEMENT_REQUIRED', 'NO_FEEDBACK'].includes(insight.status)
            ? insight.status
            : 'NO_FEEDBACK';
        const labels = {
            HEALTHY: '안정',
            IMPROVEMENT_REQUIRED: '보완 검토',
            NO_FEEDBACK: '반응 대기'
        };
        target.dataset.status = status;
        const strong = target.querySelector('strong');
        const message = target.querySelector('p');
        if (strong) strong.textContent = labels[status];
        if (message) message.textContent = insight.statusMessage || '반응 데이터를 더 수집해 주세요.';
    },

    renderReactionInsightError() {
        this.setText('contentReactionDetailStatus', '반응 인사이트를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');
        this.renderReactionInsightTrend([]);
        this.renderReactionDecision({
            status: 'NO_FEEDBACK',
            statusMessage: '반응 분석 연결 상태를 확인해 주세요.'
        });
    },

    syncReactionRangeButtons() {
        document.querySelectorAll('[data-content-reaction-range]').forEach((button) => {
            const active = Number(button.dataset.contentReactionRange) === this.state.reactionRangeDays;
            button.classList.toggle('is-active', active);
            button.setAttribute('aria-pressed', String(active));
        });
    },

    formatNumber(value) {
        const number = Number(value);
        return Number.isFinite(number) ? number.toLocaleString('ko-KR') : '0';
    },

    formatShortDate(value) {
        const parts = String(value || '').split('-');
        return parts.length === 3 ? `${Number(parts[1])}/${Number(parts[2])}` : '';
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
