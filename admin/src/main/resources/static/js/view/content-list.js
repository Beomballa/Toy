const ContentList = {
    initialized: false,
    exportInFlight: false,
    viewAnalyticsExportInFlight: false,
    reactionAnalyticsExportInFlight: false,
    performanceAnalyticsExportInFlight: false,
    performanceBulkTaskInFlight: false,
    performanceResolveInFlight: false,
    performanceAssignInFlight: false,
    performanceTaskInFlightIds: new Set(),
    performanceUnlinkedActionCount: 0,
    performanceRecoverableTaskCount: 0,
    performanceUnassignedTaskCount: 0,
    performanceAssignmentRecommendationCount: 0,
    operationPolicy: null,
    actionInFlightIds: new Set(),
    dailyStats: null,
    viewAnalytics: null,
    reactionAnalytics: null,
    reactionDataQuality: null,
    performanceAnalytics: null,
    viewDataQuality: null,
    viewAnalyticsRequestId: 0,
    reactionAnalyticsRequestId: 0,
    performanceAnalyticsRequestId: 0,
    viewAnalyticsLoading: false,
    state: {
        page: 0,
        size: 9,
        boardType: ContentBoardConfig.normalizeBoardType(window.initialContentBoardType),
        keyword: '',
        status: '',
        publicYn: '',
        startDate: '',
        endDate: '',
        pinnedOnly: false,
        productLinked: '',
        productNo: '',
        viewRangeDays: 7,
        source: '',
        returnTo: '',
        selectedIds: new Set(),
        currentPageIds: [],
        lastBulkResultMessage: '아직 일괄 적용 결과가 없습니다.'
    },

    init() {
        if (this.initialized) return;
        this.initialized = true;
        this.normalizeStateFromUrl();
        this.syncSearchField();
        this.setInitialTab();
        this.updateSidebarActive();
        this.updatePageMeta();
        CommonJS.bindMainLogoNavigation(this.state.returnTo || '/admin/content/list');
        CommonJS.renderSourceContextNotice({ noticeId: 'contentListSourceContextNotice', source: this.state.source });
        this.bindEvents();
        this.applyOperationPolicy();
        this.getDailyStats();
        this.getViewAnalytics();
        this.getReactionAnalytics();
        this.getReactionDataQuality();
        this.getPerformanceAnalytics();
        this.getViewDataQuality();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));
        this.getList();
    },

    setInitialTab() {
        document.querySelectorAll('.content-board-tab[data-board-type]').forEach(el => {
            const boardType = ContentBoardConfig.normalizeBoardType(el.dataset.boardType);
            if (boardType === this.state.boardType) {
                el.classList.add('active');
            } else {
                el.classList.remove('active');
            }
        });
    },

    updateSidebarActive() {
        document.querySelectorAll('.nav-link[data-community-nav]').forEach(el => {
            const boardType = ContentBoardConfig.normalizeBoardType(el.dataset.communityNav);
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
                const boardType = ContentBoardConfig.normalizeBoardType(el.dataset.boardType);
                if (boardType !== el.dataset.boardType) {
                    void CommonJS.alert('게시판 정보가 올바르지 않습니다.', '알림', 'warning');
                    return;
                }
                document.querySelectorAll('.content-board-tab[data-board-type]').forEach(link => link.classList.remove('active'));
                el.classList.add('active');
                this.state.boardType = boardType;
                this.state.page = 0;

                this.pushState();
                this.updateSidebarActive();
                this.updatePageMeta();
                this.renderDailyStats();
                this.getViewAnalytics();
                this.getReactionAnalytics();
                this.getPerformanceAnalytics();
                this.getList();
            });
        });

        window.addEventListener('popstate', () => {
            // 히스토리 이동 시 URL이 현재 게시판 상태의 기준이 된다.
            this.normalizeStateFromUrl();
            this.syncSearchField();
            this.setInitialTab();
            this.updateSidebarActive();
            this.updatePageMeta();
            this.renderDailyStats();
            this.getViewAnalytics();
            this.getReactionAnalytics();
            this.getPerformanceAnalytics();
            CommonJS.bindMainLogoNavigation(this.state.returnTo || '/admin/content/list');
            CommonJS.renderSourceContextNotice({ noticeId: 'contentListSourceContextNotice', source: this.state.source });
            this.getList();
        });

        // 새 글 작성 버튼
        document.getElementById('btnNewContent')?.addEventListener('click', () => {
            location.href = `/admin/content/edit?boardType=${this.state.boardType}&source=content-list&returnTo=${encodeURIComponent(this.getCurrentLocation())}`;
        });
        document.getElementById('btnExportContentCsv')?.addEventListener('click', () => this.exportCsv());
        document.getElementById('btnBulkDeleteContent')?.addEventListener('click', () => this.applyBulkDelete());
        document.getElementById('btnRefreshViewAnalytics')?.addEventListener('click', () => {
            this.getViewAnalytics();
            this.getViewDataQuality();
        });
        document.getElementById('btnExportViewAnalytics')?.addEventListener('click', () => this.exportViewAnalytics());
        document.getElementById('btnRefreshReactionAnalytics')?.addEventListener('click', () => this.getReactionAnalytics());
        document.getElementById('btnRefreshReactionAnalytics')?.addEventListener('click', () => this.getReactionDataQuality());
        document.getElementById('btnExportReactionAnalytics')?.addEventListener('click', () => this.exportReactionAnalytics());
        document.getElementById('btnRefreshPerformanceAnalytics')?.addEventListener('click', () => this.getPerformanceAnalytics());
        document.getElementById('btnExportPerformanceAnalytics')?.addEventListener('click', () => this.exportPerformanceAnalytics());
        document.getElementById('btnCreatePerformanceTasks')?.addEventListener('click', () => this.createPerformanceTasks());
        document.getElementById('btnResolvePerformanceTasks')?.addEventListener('click', () => this.resolvePerformanceTasks());
        document.getElementById('btnAssignPerformanceTasks')?.addEventListener('click', () => this.assignPerformanceTasks());
        document.getElementById('contentPerformancePriorityList')?.addEventListener('click', (event) => {
            const button = event.target.closest('[data-role="create-performance-task"]');
            if (!button) return;
            this.createPerformanceTask(
                Number(button.dataset.documentId),
                button.dataset.boardType
            );
        });
        document.querySelectorAll('[data-view-range]').forEach((button) => {
            button.addEventListener('click', () => {
                const rangeDays = Number(button.dataset.viewRange);
                if (![7, 14, 30].includes(rangeDays) || rangeDays === this.state.viewRangeDays) {
                    return;
                }
                this.state.viewRangeDays = rangeDays;
                this.syncViewAnalyticsPeriod();
                this.getViewAnalytics();
                this.getReactionAnalytics();
                this.getPerformanceAnalytics();
            });
        });

        document.getElementById('contentSearchForm')?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.state.keyword = CommonJS.normalizeOptionalText(document.getElementById('contentSearchKeyword')?.value) || '';
            this.state.status = this.normalizeStatusValue(document.getElementById('contentStatusFilter')?.value);
            this.state.publicYn = this.normalizeYnFilterValue(document.getElementById('contentPublicFilter')?.value);
            this.state.startDate = document.getElementById('contentStartDate')?.value || '';
            this.state.endDate = document.getElementById('contentEndDate')?.value || '';
            this.state.pinnedOnly = document.getElementById('contentPinnedOnly')?.checked || false;
            this.state.productLinked = this.normalizeYnFilterValue(document.getElementById('contentProductLinkedFilter')?.value);
            this.state.productNo = this.normalizeOptionalPositiveNumber(document.getElementById('contentProductNoFilter')?.value.trim() || '');
            this.state.page = 0;
            if (!this.validateState()) {
                return;
            }
            this.pushState();
            this.getList();
        });

        document.getElementById('btnResetContentSearch')?.addEventListener('click', () => {
            this.state.keyword = '';
            this.state.status = '';
            this.state.publicYn = '';
            this.state.startDate = '';
            this.state.endDate = '';
            this.state.pinnedOnly = false;
            this.state.productLinked = '';
            this.state.productNo = '';
            this.state.page = 0;
            this.syncSearchField();
            this.pushState();
            this.getList();
        });

        document.getElementById('btnApplyBulkOperate')?.addEventListener('click', () => this.applyBulkOperate());
        document.getElementById('btnClearSelection')?.addEventListener('click', () => {
            this.state.selectedIds.clear();
            this.syncSelectionState();
        });
        document.getElementById('btnSelectCurrentPage')?.addEventListener('click', () => this.updateCurrentPageSelection(true));
        document.getElementById('btnDeselectCurrentPage')?.addEventListener('click', () => this.updateCurrentPageSelection(false));
        document.getElementById('contentSelectAllOnPage')?.addEventListener('change', (event) => {
            this.updateCurrentPageSelection(event.target.checked);
        });

        document.getElementById('pagination')?.addEventListener('click', (event) => {
            const pageButton = event.target.closest('[data-role="go-content-page"]');
            if (!pageButton) {
                return;
            }
            this.goPage(this.normalizePage(pageButton.dataset.page));
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

    async getDailyStats() {
        try {
            const response = await fetch('/api/admin/content/stats/daily', {
                headers: { Accept: 'application/json' }
            });
            if (!response.ok) {
                throw new Error('문서 일일 통계를 불러오지 못했습니다.');
            }
            this.dailyStats = await response.json();
            this.renderDailyStats();
        } catch (error) {
            this.dailyStats = null;
            this.setText('contentDailyStatsStatus', '조회 실패');
            console.error('문서 일일 통계 조회 실패:', error);
        }
    },

    renderDailyStats() {
        const items = Array.isArray(this.dailyStats?.items) ? this.dailyStats.items : [];
        const total = items.find((item) => item.scope === 'TOTAL');
        const board = items.find((item) => item.scope === this.state.boardType);
        const hasSnapshot = Boolean(this.dailyStats?.snapshotDate && total);

        this.setText('contentDailyStatsDate', hasSnapshot ? `${this.dailyStats.snapshotDate} 기준` : '집계 대기');
        this.setText('contentDailyStatsBoard', board ? `${this.formatNumber(board.totalCount)}건` : '-');
        this.setText('contentDailyStatsTotal', total ? `${this.formatNumber(total.totalCount)}건` : '-');
        this.setText('contentDailyStatsViews', total ? `${this.formatNumber(total.totalViewCount)}회` : '-');
        this.setText('contentDailyStatsStatus', hasSnapshot ? `완료 · ${this.dailyStats.aggregatedAt}` : '스냅샷 없음');
    },

    async getViewAnalytics() {
        const requestId = ++this.viewAnalyticsRequestId;
        const params = new URLSearchParams({
            boardType: this.state.boardType,
            days: String(this.state.viewRangeDays)
        });
        this.viewAnalyticsLoading = true;
        this.syncViewAnalyticsPeriod();
        this.renderViewAnalyticsLoading();

        try {
            const response = await fetch(`/api/admin/content/stats/views?${params}`, {
                headers: { Accept: 'application/json' }
            });
            if (!response.ok) {
                throw new Error('프론트 조회 분석을 불러오지 못했습니다.');
            }
            const analytics = await response.json();
            if (requestId !== this.viewAnalyticsRequestId) {
                return;
            }
            this.viewAnalytics = analytics;
            this.renderViewAnalytics();
        } catch (error) {
            if (requestId !== this.viewAnalyticsRequestId) {
                return;
            }
            this.viewAnalytics = null;
            this.renderViewAnalyticsError();
            console.error('프론트 조회 분석 실패:', error);
        } finally {
            if (requestId === this.viewAnalyticsRequestId) {
                this.viewAnalyticsLoading = false;
                this.syncViewAnalyticsPeriod();
            }
        }
    },

    async getViewDataQuality() {
        this.renderViewDataQualityLoading();
        try {
            const response = await fetch('/api/admin/content/stats/views/quality', {
                headers: { Accept: 'application/json' }
            });
            if (!response.ok) {
                throw new Error('조회 이벤트 품질 정보를 불러오지 못했습니다.');
            }
            this.viewDataQuality = await response.json();
            this.renderViewDataQuality();
        } catch (error) {
            this.viewDataQuality = null;
            this.renderViewDataQualityError();
            console.error('조회 이벤트 품질 조회 실패:', error);
        }
    },

    renderViewDataQualityLoading() {
        const status = document.getElementById('contentViewQualityStatus');
        if (status) {
            status.textContent = '점검 중';
            status.className = 'content-view-quality__badge';
        }
        this.setText('contentViewQualityValid', '-');
        this.setText('contentViewQualityOrphan', '-');
        this.setText('contentViewQualityRange', '-');
    },

    renderViewDataQuality() {
        const quality = this.viewDataQuality || {};
        const healthy = quality.status === 'HEALTHY';
        const status = document.getElementById('contentViewQualityStatus');
        if (status) {
            status.textContent = healthy ? '정상' : '정리 필요';
            status.className = `content-view-quality__badge ${healthy ? 'is-healthy' : 'is-warning'}`;
        }
        this.setText('contentViewQualityValid', `${this.formatNumber(quality.validEventCount)}건`);
        this.setText('contentViewQualityOrphan', `${this.formatNumber(quality.orphanEventCount)}건`);
        this.setText(
            'contentViewQualityRange',
            quality.oldestViewedDate && quality.latestViewedDate
                ? `${quality.oldestViewedDate} ~ ${quality.latestViewedDate}`
                : '수집 데이터 없음'
        );
    },

    renderViewDataQualityError() {
        const status = document.getElementById('contentViewQualityStatus');
        if (status) {
            status.textContent = '조회 실패';
            status.className = 'content-view-quality__badge is-error';
        }
        this.setText('contentViewQualityValid', '-');
        this.setText('contentViewQualityOrphan', '-');
        this.setText('contentViewQualityRange', '연결 확인 필요');
    },

    syncViewAnalyticsPeriod() {
        document.querySelectorAll('[data-view-range]').forEach((button) => {
            const active = Number(button.dataset.viewRange) === this.state.viewRangeDays;
            button.classList.toggle('is-active', active);
            button.setAttribute('aria-pressed', String(active));
            button.disabled = this.viewAnalyticsLoading && active;
        });
        this.setText('contentViewRangeLabel', `최근 ${this.state.viewRangeDays}일`);
    },

    renderViewAnalyticsLoading() {
        this.setText('contentViewAnalyticsStatus', `${this.state.boardType} 게시판의 최근 ${this.state.viewRangeDays}일 조회를 집계하고 있습니다.`);
        this.setText('contentViewTotalViews', '-');
        this.setText('contentViewChangeRate', '직전 기간 비교 -');
        this.setText('contentViewUniqueVisitors', '-');
        this.setText('contentViewContentCount', '-');
        this.setText('contentViewAverage', '-');
        this.renderViewTrend([]);
        this.renderViewTopContents([]);
    },

    renderViewAnalytics() {
        const analytics = this.viewAnalytics;
        const summary = analytics?.summary || {};
        const trend = Array.isArray(analytics?.trend) ? analytics.trend : [];
        const topContents = Array.isArray(analytics?.topContents) ? analytics.topContents : [];
        const generatedAt = analytics?.generatedAt || '집계 시각 없음';

        this.setText(
            'contentViewAnalyticsStatus',
            `${analytics?.startDate || '-'} ~ ${analytics?.endDate || '-'} · ${generatedAt} 갱신`
        );
        this.setText('contentViewTotalViews', `${this.formatNumber(summary.totalViews)}회`);
        this.setText('contentViewUniqueVisitors', `${this.formatNumber(summary.uniqueVisitors)}명`);
        this.setText('contentViewContentCount', `${this.formatNumber(summary.viewedContentCount)}건`);
        this.setText('contentViewAverage', `${this.formatDecimal(summary.averageViewsPerContent)}회`);
        this.renderViewChangeRate(summary.viewChangeRate, summary.previousViews);
        this.renderViewTrend(trend);
        this.renderViewTopContents(topContents);
        this.syncViewAnalyticsPeriod();
    },

    renderViewChangeRate(rateValue, previousViews) {
        const rate = Number(rateValue);
        const safeRate = Number.isFinite(rate) ? rate : 0;
        const prefix = safeRate > 0 ? '+' : '';
        const element = document.getElementById('contentViewChangeRate');
        if (!element) return;
        element.textContent = `직전 ${this.formatNumber(previousViews)}회 대비 ${prefix}${safeRate}%`;
        element.classList.toggle('is-positive', safeRate > 0);
        element.classList.toggle('is-negative', safeRate < 0);
    },

    renderViewTrend(trend) {
        const target = document.getElementById('contentViewTrend');
        if (!target) return;
        if (!trend.length) {
            target.innerHTML = '<div class="content-view-empty">표시할 조회 추이가 없습니다.</div>';
            return;
        }

        const maxValue = Math.max(1, ...trend.map((item) => Number(item.viewCount) || 0));
        target.innerHTML = trend.map((item, index) => {
            const views = Number(item.viewCount) || 0;
            const visitors = Number(item.uniqueVisitors) || 0;
            const viewHeight = Math.max(views > 0 ? 8 : 2, Math.round(views / maxValue * 100));
            const visitorHeight = Math.max(visitors > 0 ? 6 : 2, Math.round(visitors / maxValue * 100));
            const showLabel = trend.length <= 14 || index === 0 || index === trend.length - 1 || index % 5 === 0;
            return `
                <div class="content-view-chart__column" title="${ContentBoardConfig.escapeHtml(item.date)} · 조회 ${this.formatNumber(views)}회 · 방문자 ${this.formatNumber(visitors)}명">
                    <div class="content-view-chart__value">${views ? this.formatNumber(views) : ''}</div>
                    <div class="content-view-chart__bars">
                        <span class="content-view-chart__bar content-view-chart__bar--views" style="height:${viewHeight}%"></span>
                        <span class="content-view-chart__bar content-view-chart__bar--visitors" style="height:${visitorHeight}%"></span>
                    </div>
                    <span class="content-view-chart__date">${showLabel ? this.formatShortDate(item.date) : ''}</span>
                </div>
            `;
        }).join('');
    },

    renderViewTopContents(items) {
        const target = document.getElementById('contentViewTopContents');
        if (!target) return;
        if (!items.length) {
            target.innerHTML = '<li class="content-view-empty">선택한 기간에 조회된 콘텐츠가 없습니다.</li>';
            return;
        }
        target.innerHTML = items.map((item, index) => `
            <li class="content-view-ranking__item">
                <span class="content-view-ranking__rank">${index + 1}</span>
                <a href="/admin/content/get?id=${encodeURIComponent(item.documentId)}&boardType=${encodeURIComponent(item.boardType)}&source=content-list&returnTo=${encodeURIComponent(this.getCurrentLocation())}"
                   class="content-view-ranking__content">
                    <strong>${ContentBoardConfig.escapeHtml(item.title || '제목 없음')}</strong>
                    <span>${ContentBoardConfig.escapeHtml(item.boardType)} · 방문자 ${this.formatNumber(item.uniqueVisitors)}명</span>
                </a>
                <strong class="content-view-ranking__views">${this.formatNumber(item.viewCount)}회</strong>
            </li>
        `).join('');
    },

    renderViewAnalyticsError() {
        this.setText('contentViewAnalyticsStatus', '조회 분석을 불러오지 못했습니다. 새로고침으로 다시 시도해 주세요.');
        this.renderViewTrend([]);
        const target = document.getElementById('contentViewTopContents');
        if (target) {
            target.innerHTML = '<li class="content-view-empty content-view-empty--error">조회 분석 연결을 확인해 주세요.</li>';
        }
        this.syncViewAnalyticsPeriod();
    },

    async exportViewAnalytics() {
        if (this.viewAnalyticsExportInFlight) {
            return;
        }
        const button = document.getElementById('btnExportViewAnalytics');
        const params = new URLSearchParams({
            boardType: this.state.boardType,
            days: String(this.state.viewRangeDays)
        });
        try {
            this.viewAnalyticsExportInFlight = true;
            CommonJS.setButtonDisabled(button, true, '내보내는 중입니다.');
            await CommonJS.downloadFile(
                `/api/admin/content/stats/views/export?${params}`,
                `content-view-analytics-${this.state.boardType}-${this.state.viewRangeDays}d.csv`
            );
        } catch (error) {
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.viewAnalyticsExportInFlight = false;
            CommonJS.setButtonDisabled(button, false);
        }
    },

    async getReactionAnalytics() {
        const requestId = ++this.reactionAnalyticsRequestId;
        const params = new URLSearchParams({
            boardType: this.state.boardType,
            days: String(this.state.viewRangeDays)
        });
        this.renderReactionAnalyticsLoading();
        try {
            const response = await fetch(`/api/admin/content/stats/reactions?${params}`, {
                headers: { Accept: 'application/json' }
            });
            if (!response.ok) {
                throw new Error('독자 반응 분석을 불러오지 못했습니다.');
            }
            const analytics = await response.json();
            if (requestId !== this.reactionAnalyticsRequestId) return;
            this.reactionAnalytics = analytics;
            this.renderReactionAnalytics();
        } catch (error) {
            if (requestId !== this.reactionAnalyticsRequestId) return;
            this.reactionAnalytics = null;
            this.renderReactionAnalyticsError();
            console.error('독자 반응 분석 실패:', error);
        }
    },

    async getReactionDataQuality() {
        this.renderReactionDataQualityLoading();
        try {
            const response = await fetch('/api/admin/content/stats/reactions/quality', {
                headers: { Accept: 'application/json' }
            });
            if (!response.ok) throw new Error('반응 데이터 품질 정보를 불러오지 못했습니다.');
            this.reactionDataQuality = await response.json();
            this.renderReactionDataQuality();
        } catch (error) {
            this.reactionDataQuality = null;
            this.renderReactionDataQualityError();
            console.error('반응 데이터 품질 조회 실패:', error);
        }
    },

    renderReactionDataQualityLoading() {
        const status = document.getElementById('contentReactionQualityStatus');
        if (status) {
            status.textContent = '점검 중';
            status.className = 'content-view-quality__badge';
        }
        this.setText('contentReactionQualityValid', '-');
        this.setText('contentReactionQualityOrphan', '-');
        this.setText('contentReactionQualityRange', '-');
    },

    renderReactionDataQuality() {
        const quality = this.reactionDataQuality || {};
        const healthy = quality.status === 'HEALTHY';
        const status = document.getElementById('contentReactionQualityStatus');
        if (status) {
            status.textContent = healthy ? '정상' : '정리 필요';
            status.className = `content-view-quality__badge ${healthy ? 'is-healthy' : 'is-warning'}`;
        }
        this.setText('contentReactionQualityValid', `${this.formatNumber(quality.validCount)}건`);
        this.setText('contentReactionQualityOrphan', `${this.formatNumber(quality.orphanCount)}건`);
        this.setText(
            'contentReactionQualityRange',
            quality.oldestReactedAt && quality.latestReactedAt
                ? `${quality.oldestReactedAt} ~ ${quality.latestReactedAt}`
                : '수집 데이터 없음'
        );
    },

    renderReactionDataQualityError() {
        const status = document.getElementById('contentReactionQualityStatus');
        if (status) {
            status.textContent = '조회 실패';
            status.className = 'content-view-quality__badge is-error';
        }
        this.setText('contentReactionQualityValid', '-');
        this.setText('contentReactionQualityOrphan', '-');
        this.setText('contentReactionQualityRange', '연결 확인 필요');
    },

    renderReactionAnalyticsLoading() {
        this.setText(
            'contentReactionAnalyticsStatus',
            `${this.state.boardType} 게시판의 최근 ${this.state.viewRangeDays}일 반응을 집계하고 있습니다.`
        );
        this.setText('contentReactionRangeLabel', `최근 ${this.state.viewRangeDays}일`);
        this.setText('contentReactionTotal', '-');
        this.setText('contentReactionVisitorCount', '참여 방문자 -');
        this.setText('contentReactionHelpful', '-');
        this.setText('contentReactionNotHelpful', '-');
        this.setText('contentReactionHelpfulRate', '-');
        this.setText('contentReactionContentCount', '-');
        this.renderReactionTrend([]);
        this.renderReactionContents('contentReactionTopContents', [], '표시할 반응 상위가 없습니다.');
        this.renderReactionContents('contentReactionImprovementContents', [], '표시할 개선 후보가 없습니다.');
    },

    renderReactionAnalytics() {
        const analytics = this.reactionAnalytics || {};
        const summary = analytics.summary || {};
        this.setText(
            'contentReactionAnalyticsStatus',
            `${analytics.startDate || '-'} ~ ${analytics.endDate || '-'} · ${analytics.metricBasis || '현재 반응 기준'} · ${analytics.generatedAt || '-'} 갱신`
        );
        this.setText('contentReactionRangeLabel', `최근 ${analytics.rangeDays || this.state.viewRangeDays}일`);
        this.setText('contentReactionTotal', `${this.formatNumber(summary.totalCount)}건`);
        this.setText('contentReactionVisitorCount', `참여 방문자 ${this.formatNumber(summary.uniqueVisitors)}명`);
        this.setText('contentReactionHelpful', `${this.formatNumber(summary.helpfulCount)}건`);
        this.setText('contentReactionNotHelpful', `${this.formatNumber(summary.notHelpfulCount)}건`);
        this.setText('contentReactionHelpfulRate', `${this.formatNumber(summary.helpfulRate)}%`);
        this.setText('contentReactionContentCount', `${this.formatNumber(summary.evaluatedContentCount)}건`);
        this.renderReactionTrend(Array.isArray(analytics.trend) ? analytics.trend : []);
        this.renderReactionContents(
            'contentReactionTopContents',
            Array.isArray(analytics.topContents) ? analytics.topContents : [],
            '선택한 기간에 반응이 남은 콘텐츠가 없습니다.'
        );
        this.renderReactionContents(
            'contentReactionImprovementContents',
            Array.isArray(analytics.improvementContents) ? analytics.improvementContents : [],
            '선택한 기간에 개선 필요 반응이 없습니다.'
        );
    },

    renderReactionTrend(trend) {
        const target = document.getElementById('contentReactionTrend');
        if (!target) return;
        if (!trend.length) {
            target.innerHTML = '<div class="content-view-empty">표시할 반응 추이가 없습니다.</div>';
            return;
        }
        const maxValue = Math.max(1, ...trend.map((item) => Number(item.totalCount) || 0));
        target.innerHTML = trend.map((item, index) => {
            const helpful = Number(item.helpfulCount) || 0;
            const notHelpful = Number(item.notHelpfulCount) || 0;
            const helpfulHeight = Math.max(helpful > 0 ? 8 : 2, Math.round(helpful / maxValue * 100));
            const notHelpfulHeight = Math.max(notHelpful > 0 ? 8 : 2, Math.round(notHelpful / maxValue * 100));
            const showLabel = trend.length <= 14 || index === 0 || index === trend.length - 1 || index % 5 === 0;
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

    renderReactionContents(targetId, items, emptyMessage) {
        const target = document.getElementById(targetId);
        if (!target) return;
        if (!items.length) {
            target.innerHTML = `<li class="content-view-empty">${ContentBoardConfig.escapeHtml(emptyMessage)}</li>`;
            return;
        }
        target.innerHTML = items.map((item, index) => `
            <li class="content-view-ranking__item">
                <span class="content-view-ranking__rank">${index + 1}</span>
                <a href="/admin/content/get?id=${encodeURIComponent(item.documentId)}&boardType=${encodeURIComponent(item.boardType)}&source=content-list&returnTo=${encodeURIComponent(this.getCurrentLocation())}"
                   class="content-view-ranking__content">
                    <strong>${ContentBoardConfig.escapeHtml(item.title || '제목 없음')}</strong>
                    <span>${ContentBoardConfig.escapeHtml(item.boardType)} · 도움 ${this.formatNumber(item.helpfulCount)} · 개선 ${this.formatNumber(item.notHelpfulCount)}</span>
                </a>
                <strong class="content-view-ranking__views">${this.formatNumber(item.helpfulRate)}%</strong>
            </li>
        `).join('');
    },

    renderReactionAnalyticsError() {
        this.setText('contentReactionAnalyticsStatus', '독자 반응 분석을 불러오지 못했습니다. 연결 상태를 확인해 주세요.');
        this.renderReactionTrend([]);
        this.renderReactionContents('contentReactionTopContents', [], '반응 분석 연결을 확인해 주세요.');
        this.renderReactionContents('contentReactionImprovementContents', [], '반응 분석 연결을 확인해 주세요.');
    },

    async exportReactionAnalytics() {
        if (this.reactionAnalyticsExportInFlight) return;
        const button = document.getElementById('btnExportReactionAnalytics');
        const params = new URLSearchParams({
            boardType: this.state.boardType,
            days: String(this.state.viewRangeDays)
        });
        try {
            this.reactionAnalyticsExportInFlight = true;
            CommonJS.setButtonDisabled(button, true, '내보내는 중입니다.');
            await CommonJS.downloadFile(
                `/api/admin/content/stats/reactions/export?${params}`,
                `content-reaction-analytics-${this.state.boardType}-${this.state.viewRangeDays}d.csv`
            );
        } catch (error) {
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.reactionAnalyticsExportInFlight = false;
            CommonJS.setButtonDisabled(button, false);
        }
    },

    async getPerformanceAnalytics() {
        const requestId = ++this.performanceAnalyticsRequestId;
        const params = new URLSearchParams({
            boardType: this.state.boardType,
            days: String(this.state.viewRangeDays)
        });
        this.renderPerformanceAnalyticsLoading();
        try {
            const response = await fetch(`/api/admin/content/stats/performance?${params}`, {
                headers: { Accept: 'application/json' }
            });
            if (!response.ok) throw new Error('콘텐츠 효과 분석을 불러오지 못했습니다.');
            const analytics = await response.json();
            if (requestId !== this.performanceAnalyticsRequestId) return;
            this.performanceAnalytics = analytics;
            this.renderPerformanceAnalytics();
        } catch (error) {
            if (requestId !== this.performanceAnalyticsRequestId) return;
            this.performanceAnalytics = null;
            this.renderPerformanceAnalyticsError();
            console.error('콘텐츠 효과 분석 실패:', error);
        }
    },

    renderPerformanceAnalyticsLoading() {
        this.setText(
            'contentPerformanceAnalyticsStatus',
            `${this.state.boardType} 게시판의 최근 ${this.state.viewRangeDays}일 효과를 계산하고 있습니다.`
        );
        this.setText('contentPerformanceRangeLabel', `최근 ${this.state.viewRangeDays}일`);
        this.setText('contentPerformanceViews', '-');
        this.setText('contentPerformanceReactions', '-');
        this.setText('contentPerformanceHelpfulRate', '-');
        this.setText('contentPerformanceCoverage', '-');
        this.setText('contentPerformanceContentCount', '-');
        this.setText('contentPerformanceActionCount', '-');
        this.setText('contentPerformanceLinkedCount', '-');
        this.setText('contentPerformanceUnlinkedCount', '-');
        this.setText('contentPerformanceOpenTaskCount', '-');
        this.setText('contentPerformanceOverdueTaskCount', '-');
        this.setText('contentPerformanceRecoverableTaskCount', '-');
        this.setText('contentPerformanceUnassignedTaskCount', '-');
        this.performanceUnlinkedActionCount = 0;
        this.performanceRecoverableTaskCount = 0;
        this.performanceUnassignedTaskCount = 0;
        this.performanceAssignmentRecommendationCount = 0;
        this.syncPerformanceBulkTaskButton();
        this.syncPerformanceResolveButton();
        this.syncPerformanceAssignButton();
        this.renderPerformanceAssignment([]);
        this.renderPerformancePriorityList([]);
    },

    renderPerformanceAnalytics() {
        const analytics = this.performanceAnalytics || {};
        const summary = analytics.summary || {};
        this.setText(
            'contentPerformanceAnalyticsStatus',
            `${analytics.startDate || '-'} ~ ${analytics.endDate || '-'} · ${analytics.generatedAt || '-'} 갱신`
        );
        this.setText('contentPerformanceRangeLabel', `최근 ${analytics.rangeDays || this.state.viewRangeDays}일`);
        this.setText('contentPerformanceViews', `${this.formatNumber(summary.totalViews)}회`);
        this.setText('contentPerformanceReactions', `${this.formatNumber(summary.totalReactions)}건`);
        this.setText('contentPerformanceHelpfulRate', `${this.formatNumber(summary.helpfulRate)}%`);
        this.setText('contentPerformanceCoverage', `${this.formatNumber(summary.reactionCoverageRate)}%`);
        this.setText('contentPerformanceContentCount', `${this.formatNumber(summary.analyzedContentCount)}건`);
        this.setText('contentPerformanceActionCount', `${this.formatNumber(summary.actionRequiredCount)}건`);
        this.setText('contentPerformanceLinkedCount', `${this.formatNumber(summary.linkedActionCount)}건`);
        this.setText('contentPerformanceUnlinkedCount', `${this.formatNumber(summary.unlinkedActionCount)}건`);
        this.setText('contentPerformanceOpenTaskCount', `${this.formatNumber(summary.openTaskCount)}건`);
        this.setText('contentPerformanceOverdueTaskCount', `${this.formatNumber(summary.overdueTaskCount)}건`);
        this.setText('contentPerformanceRecoverableTaskCount', `${this.formatNumber(summary.recoverableTaskCount)}건`);
        this.setText('contentPerformanceUnassignedTaskCount', `${this.formatNumber(summary.unassignedTaskCount)}건`);
        this.performanceUnlinkedActionCount = Number(summary.unlinkedActionCount) || 0;
        this.performanceRecoverableTaskCount = Number(summary.recoverableTaskCount) || 0;
        this.performanceUnassignedTaskCount = Number(summary.unassignedTaskCount) || 0;
        const recommendations = Array.isArray(analytics.assignmentRecommendations)
            ? analytics.assignmentRecommendations
            : [];
        this.performanceAssignmentRecommendationCount = recommendations.length;
        this.syncPerformanceBulkTaskButton();
        this.syncPerformanceResolveButton();
        this.syncPerformanceAssignButton();
        this.renderPerformanceAssignment(recommendations);
        this.renderPerformancePriorityList(
            Array.isArray(analytics.priorityContents) ? analytics.priorityContents : []
        );
    },

    renderPerformancePriorityList(items) {
        const target = document.getElementById('contentPerformancePriorityList');
        if (!target) return;
        if (!items.length) {
            target.innerHTML = '<div class="content-view-empty">선택한 기간에 분석할 콘텐츠 신호가 없습니다.</div>';
            return;
        }
        const labels = {
            IMPROVEMENT_REQUIRED: '보완 필요',
            FEEDBACK_NEEDED: '반응 확보',
            LOW_SIGNAL: '추가 관찰',
            HEALTHY: '안정'
        };
        target.innerHTML = items.map((item, index) => {
            const status = labels[item.status] ? item.status : 'LOW_SIGNAL';
            const actionRequired = ['IMPROVEMENT_REQUIRED', 'FEEDBACK_NEEDED'].includes(status);
            const taskAction = item.operationTaskNo && item.operationTaskPath
                ? `<a class="content-performance-item__task-link"
                      href="${ContentBoardConfig.escapeHtml(item.operationTaskPath)}">
                       작업 #${this.formatNumber(item.operationTaskNo)} 보기
                   </a>`
                : actionRequired
                    ? `<button type="button"
                               class="content-performance-item__task-button"
                               data-role="create-performance-task"
                               data-document-id="${Number(item.documentId)}"
                               data-board-type="${ContentBoardConfig.escapeHtml(item.boardType)}">
                           작업 생성
                       </button>`
                    : '';
            const taskMeta = item.operationTaskNo
                ? `<span class="content-performance-item__task-meta${item.operationTaskOverdue ? ' is-overdue' : ''}${item.operationTaskRecoverable ? ' is-recoverable' : ''}">
                       ${ContentBoardConfig.escapeHtml(item.operationTaskStatusLabel || item.operationTaskStatus || '연결')}
                       ${item.operationTaskDueDate ? ` · ${ContentBoardConfig.escapeHtml(item.operationTaskDueDate)}` : ''}
                       ${item.operationTaskAssigneeAdminNo ? ` · 관리자 #${this.formatNumber(item.operationTaskAssigneeAdminNo)}` : ' · 미배정'}
                       ${item.operationTaskOverdue ? ' · 연체' : ''}
                       ${item.operationTaskRecoverable ? ' · 회복 확인' : ''}
                   </span>`
                : '';
            return `
                <article class="content-performance-item" data-status="${status}">
                    <span class="content-performance-item__rank">${index + 1}</span>
                    <div class="content-performance-item__main">
                        <div class="content-performance-item__title">
                            <span>${ContentBoardConfig.escapeHtml(item.boardType)}</span>
                            <a href="/admin/content/get?id=${encodeURIComponent(item.documentId)}&boardType=${encodeURIComponent(item.boardType)}&source=content-performance&returnTo=${encodeURIComponent(this.getCurrentLocation())}">
                                ${ContentBoardConfig.escapeHtml(item.title || '제목 없음')}
                            </a>
                        </div>
                        <p>${ContentBoardConfig.escapeHtml(item.statusMessage || '')}</p>
                    </div>
                    <dl class="content-performance-item__metrics">
                        <div><dt>조회</dt><dd>${this.formatNumber(item.viewCount)}</dd></div>
                        <div><dt>반응</dt><dd>${this.formatNumber(item.reactionCount)}</dd></div>
                        <div><dt>도움</dt><dd>${this.formatNumber(item.helpfulRate)}%</dd></div>
                        <div><dt>확보</dt><dd>${this.formatNumber(item.reactionCoverageRate)}%</dd></div>
                    </dl>
                    <div class="content-performance-item__decision">
                        <div>
                            <span>${labels[status]}</span>
                            <strong>${this.formatNumber(item.priorityScore)}점</strong>
                        </div>
                        ${taskMeta}
                        ${taskAction}
                    </div>
                </article>
            `;
        }).join('');
        this.applyOperationPolicy();
    },

    renderPerformanceAnalyticsError() {
        this.setText('contentPerformanceAnalyticsStatus', '콘텐츠 효과 분석을 불러오지 못했습니다. 연결 상태를 확인해 주세요.');
        const target = document.getElementById('contentPerformancePriorityList');
        if (target) {
            target.innerHTML = '<div class="content-view-empty content-view-empty--error">효과 분석 연결을 확인해 주세요.</div>';
        }
        this.performanceUnlinkedActionCount = 0;
        this.performanceRecoverableTaskCount = 0;
        this.performanceUnassignedTaskCount = 0;
        this.performanceAssignmentRecommendationCount = 0;
        this.syncPerformanceBulkTaskButton();
        this.syncPerformanceResolveButton();
        this.syncPerformanceAssignButton();
        this.renderPerformanceAssignment([]);
    },

    async exportPerformanceAnalytics() {
        if (this.performanceAnalyticsExportInFlight) return;
        const button = document.getElementById('btnExportPerformanceAnalytics');
        const params = new URLSearchParams({
            boardType: this.state.boardType,
            days: String(this.state.viewRangeDays)
        });
        try {
            this.performanceAnalyticsExportInFlight = true;
            CommonJS.setButtonDisabled(button, true, '내보내는 중입니다.');
            await CommonJS.downloadFile(
                `/api/admin/content/stats/performance/export?${params}`,
                `content-performance-${this.state.boardType}-${this.state.viewRangeDays}d.csv`
            );
        } catch (error) {
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.performanceAnalyticsExportInFlight = false;
            CommonJS.setButtonDisabled(button, false);
        }
    },

    async createPerformanceTask(documentId, boardType) {
        if (!Number.isInteger(documentId) || documentId <= 0 || this.performanceTaskInFlightIds.has(documentId)) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(
                CommonJS.getAdminWriteBlockedReason('콘텐츠 개선 작업 생성'),
                '알림',
                'warning'
            );
            return;
        }
        const confirmed = await CommonJS.confirm(
            `콘텐츠 #${documentId}의 현재 효과 분석 결과로 운영 작업을 생성하시겠습니까?`,
            '콘텐츠 개선 작업 생성',
            'info'
        );
        if (!confirmed) return;

        const button = document.querySelector(
            `[data-role="create-performance-task"][data-document-id="${documentId}"]`
        );
        const params = new URLSearchParams({
            boardType: ContentBoardConfig.normalizeBoardType(boardType),
            days: String(this.state.viewRangeDays)
        });
        try {
            this.performanceTaskInFlightIds.add(documentId);
            CommonJS.setButtonDisabled(button, true, '작업을 생성하고 있습니다.');
            const response = await fetch(`/api/admin/content/${documentId}/performance-task?${params}`, {
                method: 'POST',
                headers: { Accept: 'application/json' }
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 작업 생성에 실패했습니다.'));
            }
            const result = await response.json();
            await CommonJS.alert(
                `${result.message || '운영 작업을 연결했습니다.'}<br>작업 #${this.formatNumber(result.taskNo)} · ${ContentBoardConfig.escapeHtml(result.priority || '-')}`,
                result.created ? '작업 생성 완료' : '기존 작업 확인',
                'success'
            );
            await this.getPerformanceAnalytics();
        } catch (error) {
            await CommonJS.alert(ContentBoardConfig.escapeHtml(error.message), '오류', 'error');
        } finally {
            this.performanceTaskInFlightIds.delete(documentId);
            CommonJS.setButtonDisabled(button, false);
        }
    },

    async createPerformanceTasks() {
        if (this.performanceBulkTaskInFlight || this.performanceUnlinkedActionCount <= 0) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(
                CommonJS.getAdminWriteBlockedReason('콘텐츠 개선 작업 일괄 생성'),
                '알림',
                'warning'
            );
            return;
        }
        const confirmed = await CommonJS.confirm(
            `현재 우선순위의 미연결 조치 ${this.formatNumber(this.performanceUnlinkedActionCount)}건을 운영 작업으로 생성하시겠습니까?`,
            '개선 작업 일괄 생성',
            'warning'
        );
        if (!confirmed) return;

        const params = new URLSearchParams({
            boardType: this.state.boardType,
            days: String(this.state.viewRangeDays)
        });
        try {
            this.performanceBulkTaskInFlight = true;
            this.syncPerformanceBulkTaskButton();
            const response = await fetch(`/api/admin/content/stats/performance/tasks?${params}`, {
                method: 'POST',
                headers: { Accept: 'application/json' }
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 작업 일괄 생성에 실패했습니다.'));
            }
            const result = await response.json();
            await CommonJS.alert(
                `${ContentBoardConfig.escapeHtml(result.message || '일괄 생성을 완료했습니다.')}<br>요청 ${this.formatNumber(result.requestedCount)}건 · 신규 ${this.formatNumber(result.createdCount)}건 · 기존 ${this.formatNumber(result.existingCount)}건`,
                '개선 작업 일괄 생성 완료',
                'success'
            );
            await this.getPerformanceAnalytics();
        } catch (error) {
            await CommonJS.alert(ContentBoardConfig.escapeHtml(error.message), '오류', 'error');
        } finally {
            this.performanceBulkTaskInFlight = false;
            this.syncPerformanceBulkTaskButton();
        }
    },

    syncPerformanceBulkTaskButton() {
        const button = document.getElementById('btnCreatePerformanceTasks');
        const label = document.getElementById('btnCreatePerformanceTasksLabel');
        if (label) {
            label.textContent = this.performanceBulkTaskInFlight
                ? '작업 생성 중'
                : `미연결 ${this.formatNumber(this.performanceUnlinkedActionCount)}건 작업화`;
        }
        const policyBlocked = !!(this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy));
        const disabled = this.performanceBulkTaskInFlight
            || this.performanceUnlinkedActionCount <= 0
            || policyBlocked;
        const reason = policyBlocked
            ? CommonJS.getAdminWriteBlockedReason('콘텐츠 개선 작업 일괄 생성')
            : this.performanceUnlinkedActionCount <= 0
                ? '현재 미연결 조치 대상이 없습니다.'
                : '';
        CommonJS.setButtonDisabled(button, disabled, reason);
    },

    async resolvePerformanceTasks() {
        if (this.performanceResolveInFlight || this.performanceRecoverableTaskCount <= 0) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(
                CommonJS.getAdminWriteBlockedReason('성과 회복 작업 일괄 완료'),
                '알림',
                'warning'
            );
            return;
        }
        const confirmed = await CommonJS.confirm(
            `성과가 안정 상태로 회복된 연결 작업 ${this.formatNumber(this.performanceRecoverableTaskCount)}건을 완료 처리하시겠습니까?`,
            '성과 회복 작업 완료',
            'warning'
        );
        if (!confirmed) return;

        const params = new URLSearchParams({
            boardType: this.state.boardType,
            days: String(this.state.viewRangeDays)
        });
        try {
            this.performanceResolveInFlight = true;
            this.syncPerformanceResolveButton();
            const response = await fetch(`/api/admin/content/stats/performance/tasks/resolve?${params}`, {
                method: 'POST',
                headers: { Accept: 'application/json' }
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '성과 회복 작업 완료에 실패했습니다.'));
            }
            const result = await response.json();
            await CommonJS.alert(
                `${ContentBoardConfig.escapeHtml(result.message || '회복 작업 처리를 완료했습니다.')}<br>요청 ${this.formatNumber(result.requestedCount)}건 · 완료 ${this.formatNumber(result.completedCount)}건 · 기존 완료 ${this.formatNumber(result.alreadyCompletedCount)}건`,
                '성과 회복 작업 완료',
                'success'
            );
            await this.getPerformanceAnalytics();
        } catch (error) {
            await CommonJS.alert(ContentBoardConfig.escapeHtml(error.message), '오류', 'error');
        } finally {
            this.performanceResolveInFlight = false;
            this.syncPerformanceResolveButton();
        }
    },

    syncPerformanceResolveButton() {
        const button = document.getElementById('btnResolvePerformanceTasks');
        const label = document.getElementById('btnResolvePerformanceTasksLabel');
        if (label) {
            label.textContent = this.performanceResolveInFlight
                ? '완료 처리 중'
                : `회복 ${this.formatNumber(this.performanceRecoverableTaskCount)}건 완료`;
        }
        const policyBlocked = !!(this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy));
        const disabled = this.performanceResolveInFlight
            || this.performanceRecoverableTaskCount <= 0
            || policyBlocked;
        const reason = policyBlocked
            ? CommonJS.getAdminWriteBlockedReason('성과 회복 작업 일괄 완료')
            : this.performanceRecoverableTaskCount <= 0
                ? '현재 성과가 회복된 진행 작업이 없습니다.'
                : '';
        CommonJS.setButtonDisabled(button, disabled, reason);
    },

    renderPerformanceAssignment(items) {
        const target = document.getElementById('contentPerformanceAssignment');
        if (!target) return;
        if (!items.length) {
            target.innerHTML = `<div class="content-view-empty">${
                this.performanceUnassignedTaskCount > 0
                    ? '현재 배정 가능한 활성 관리자가 없습니다.'
                    : '미배정 콘텐츠 개선 작업이 없습니다.'
            }</div>`;
            return;
        }
        target.innerHTML = items.map((item, index) => `
            <article class="content-performance-assignment__item">
                <span>${index + 1}순위</span>
                <strong>${ContentBoardConfig.escapeHtml(item.adminName || `관리자 #${item.adminNo}`)}</strong>
                <small>${ContentBoardConfig.escapeHtml(item.reasonLabel || '')}</small>
                <dl>
                    <div><dt>전체</dt><dd>${this.formatNumber(item.totalCount)}</dd></div>
                    <div><dt>진행</dt><dd>${this.formatNumber(item.inProgressCount)}</dd></div>
                    <div><dt>연체</dt><dd>${this.formatNumber(item.overdueCount)}</dd></div>
                </dl>
            </article>
        `).join('');
    },

    async assignPerformanceTasks() {
        if (this.performanceAssignInFlight
            || this.performanceUnassignedTaskCount <= 0
            || this.performanceAssignmentRecommendationCount <= 0) {
            return;
        }
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(
                CommonJS.getAdminWriteBlockedReason('콘텐츠 개선 작업 일괄 배정'),
                '알림',
                'warning'
            );
            return;
        }
        const confirmed = await CommonJS.confirm(
            `미배정 콘텐츠 개선 작업 ${this.formatNumber(this.performanceUnassignedTaskCount)}건을 현재 워크로드가 낮은 담당자에게 분산 배정하시겠습니까?`,
            '추천 담당자 일괄 배정',
            'warning'
        );
        if (!confirmed) return;

        const params = new URLSearchParams({
            boardType: this.state.boardType,
            days: String(this.state.viewRangeDays)
        });
        try {
            this.performanceAssignInFlight = true;
            this.syncPerformanceAssignButton();
            const response = await fetch(`/api/admin/content/stats/performance/tasks/assign?${params}`, {
                method: 'POST',
                headers: { Accept: 'application/json' }
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '콘텐츠 개선 작업 배정에 실패했습니다.'));
            }
            const result = await response.json();
            await CommonJS.alert(
                `${ContentBoardConfig.escapeHtml(result.message || '작업 배정을 완료했습니다.')}<br>요청 ${this.formatNumber(result.requestedCount)}건 · 배정 ${this.formatNumber(result.assignedCount)}건 · 기존 배정 ${this.formatNumber(result.alreadyAssignedCount)}건`,
                '추천 담당자 배정 완료',
                'success'
            );
            await this.getPerformanceAnalytics();
        } catch (error) {
            await CommonJS.alert(ContentBoardConfig.escapeHtml(error.message), '오류', 'error');
        } finally {
            this.performanceAssignInFlight = false;
            this.syncPerformanceAssignButton();
        }
    },

    syncPerformanceAssignButton() {
        const button = document.getElementById('btnAssignPerformanceTasks');
        const label = document.getElementById('btnAssignPerformanceTasksLabel');
        if (label) {
            label.textContent = this.performanceAssignInFlight
                ? '담당자 배정 중'
                : `미배정 ${this.formatNumber(this.performanceUnassignedTaskCount)}건 배정`;
        }
        const policyBlocked = !!(this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy));
        const noRecommendation = this.performanceAssignmentRecommendationCount <= 0;
        const disabled = this.performanceAssignInFlight
            || this.performanceUnassignedTaskCount <= 0
            || noRecommendation
            || policyBlocked;
        const reason = policyBlocked
            ? CommonJS.getAdminWriteBlockedReason('콘텐츠 개선 작업 일괄 배정')
            : this.performanceUnassignedTaskCount <= 0
                ? '현재 미배정 콘텐츠 개선 작업이 없습니다.'
                : noRecommendation
                    ? '배정 가능한 활성 관리자가 없습니다.'
                    : '';
        CommonJS.setButtonDisabled(button, disabled, reason);
    },

    formatDecimal(value) {
        const number = Number(value);
        return Number.isFinite(number)
            ? number.toLocaleString('ko-KR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })
            : '0.0';
    },

    formatShortDate(value) {
        const parts = String(value || '').split('-');
        return parts.length === 3 ? `${Number(parts[1])}/${Number(parts[2])}` : '';
    },

    setText(id, value) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value;
        }
    },

    formatNumber(value) {
        const number = Number(value);
        return Number.isFinite(number) ? number.toLocaleString('ko-KR') : '0';
    },

    async applyOperationPolicy(settings = null) {
        const createButton = document.getElementById('btnNewContent');
        const bulkButton = document.getElementById('btnApplyBulkOperate');
        const bulkDeleteButton = document.getElementById('btnBulkDeleteContent');
        try {
            const resolvedSettings = settings || await CommonJS.fetchSystemSettings();
            this.operationPolicy = resolvedSettings;
            const disabled = CommonJS.isCommunityWriteBlocked(resolvedSettings);
            const reason = CommonJS.getCommunityWriteBlockedReason(resolvedSettings, '커뮤니티 작성');
            CommonJS.setButtonDisabled(createButton, disabled, reason);
            CommonJS.setButtonDisabled(bulkButton, disabled, reason);
            CommonJS.setButtonDisabled(bulkDeleteButton, disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnSelectCurrentPage'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnDeselectCurrentPage'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('contentSelectAllOnPage'), disabled, reason);
            document.querySelectorAll('[data-role="content-edit"]').forEach((button) => {
                CommonJS.setButtonDisabled(button, disabled, reason);
            });
            document.querySelectorAll('[data-role="content-status-toggle"], [data-role="content-visibility-toggle"], [data-role="content-delete"]').forEach((button) => {
                CommonJS.setButtonDisabled(button, disabled, reason);
            });
            const taskDisabled = CommonJS.isAdminWriteBlocked(resolvedSettings);
            const taskReason = CommonJS.getAdminWriteBlockedReason('콘텐츠 개선 작업 생성');
            document.querySelectorAll('[data-role="create-performance-task"]').forEach((button) => {
                CommonJS.setButtonDisabled(button, taskDisabled, taskReason);
            });
            this.syncPerformanceBulkTaskButton();
            this.syncPerformanceResolveButton();
            this.syncPerformanceAssignButton();
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
    },

    async getList() {
        if (!this.validateState()) {
            return;
        }
        const params = new URLSearchParams({
            page: this.state.page,
            size: String(this.state.size),
            boardType: this.state.boardType
        });
        if (this.state.keyword) {
            params.set('keyword', this.state.keyword);
        }
        if (this.state.status) {
            params.set('status', this.state.status);
        }
        if (this.state.publicYn) {
            params.set('publicYn', this.state.publicYn);
        }
        if (this.state.startDate) {
            params.set('startDate', this.state.startDate);
        }
        if (this.state.endDate) {
            params.set('endDate', this.state.endDate);
        }
        if (this.state.pinnedOnly) {
            params.set('pinnedOnly', 'true');
        }
        if (this.state.productLinked) {
            params.set('productLinked', this.state.productLinked);
        }
        if (this.state.productNo) {
            params.set('productNo', this.state.productNo);
        }

        try {
            this.renderLoadingState();
            const [listResponse, summaryResponse] = await Promise.all([
                fetch(`/api/admin/content/list?${params}`),
                fetch(`/api/admin/content/summary?${params}`)
            ]);
            if (!listResponse.ok) throw new Error(`HTTP ${listResponse.status}`);
            if (!summaryResponse.ok) throw new Error(`HTTP ${summaryResponse.status}`);

            const data = await listResponse.json();
            const summary = await summaryResponse.json();
            this.renderList(data.items);
            this.renderPagination(data);
            this.renderSummary(summary);
        } catch (err) {
            console.error('콘텐츠 목록 로드 실패:', err);
            this.renderListError(err.message || '목록을 불러오는 중 오류가 발생했습니다.');
            this.renderSummary(null);
            CommonJS.alert('목록을 불러오는 중 오류가 발생했습니다.', '오류', 'error');
        }
    },

    renderSummary(summary) {
        if (!summary) {
            const totalEl = document.getElementById('contentSummaryTotal');
            const viewsEl = document.getElementById('contentSummaryViews');
            const publishEl = document.getElementById('contentSummaryPublish');
            const visibilityEl = document.getElementById('contentSummaryVisibility');
            const optionsEl = document.getElementById('contentSummaryOptions');
            if (totalEl) totalEl.textContent = '0건';
            if (viewsEl) viewsEl.textContent = '조회수 합계 0';
            if (publishEl) publishEl.textContent = '게시중 0 · 임시저장 0';
            if (visibilityEl) visibilityEl.textContent = '공개 0 · 비공개 0';
            if (optionsEl) optionsEl.textContent = '고정 0 · 상품연결 0';
            return;
        }
        const totalCount = Number(summary.totalCount || 0).toLocaleString();
        const totalViewCount = Number(summary.totalViewCount || 0).toLocaleString();
        const publishedCount = Number(summary.publishedCount || 0).toLocaleString();
        const draftCount = Number(summary.draftCount || 0).toLocaleString();
        const publicCount = Number(summary.publicCount || 0).toLocaleString();
        const privateCount = Number(summary.privateCount || 0).toLocaleString();
        const pinnedCount = Number(summary.pinnedCount || 0).toLocaleString();
        const linkedCount = Number(summary.linkedCount || 0).toLocaleString();

        const totalEl = document.getElementById('contentSummaryTotal');
        if (totalEl) totalEl.textContent = `${totalCount}건`;
        const viewsEl = document.getElementById('contentSummaryViews');
        if (viewsEl) viewsEl.textContent = `조회수 합계 ${totalViewCount}`;
        const publishEl = document.getElementById('contentSummaryPublish');
        if (publishEl) publishEl.textContent = `게시중 ${publishedCount} · 임시저장 ${draftCount}`;
        const visibilityEl = document.getElementById('contentSummaryVisibility');
        if (visibilityEl) visibilityEl.textContent = `공개 ${publicCount} · 비공개 ${privateCount}`;
        const optionsEl = document.getElementById('contentSummaryOptions');
        if (optionsEl) optionsEl.textContent = `고정 ${pinnedCount} · 상품연결 ${linkedCount}`;
    },

    renderList(items) {
        const grid = document.getElementById('contentGrid');
        if (!grid) return;
        this.state.currentPageIds = (items || []).map((item) => item.id);

        if (!items || items.length === 0) {
            grid.innerHTML = `
                <div class="col-12">
                    <div class="product-empty-state py-5">
                        <i class="fas fa-folder-open product-empty-state-icon"></i>
                        <strong>등록된 콘텐츠가 없습니다.</strong>
                        <p>${this.buildEmptyStateMessage()}</p>
                    </div>
                </div>`;
            this.syncSelectionState();
            return;
        }

        grid.innerHTML = items.map(item => `
            <div class="col-md-4 mb-4">
                    <div class="card h-100 content-board-card">
                    <div class="card-body content-board-card-body">
                        <div class="content-board-card-top">
                            <div class="d-flex gap-2 align-items-center flex-wrap">
                                <label class="form-check mb-0">
                                    <input class="form-check-input content-select-checkbox" type="checkbox" data-content-id="${item.id}" ${this.state.selectedIds.has(item.id) ? 'checked' : ''}>
                                </label>
                                <span class="content-board-card-badge">${ContentBoardConfig.escapeHtml(this.getBoardLabel(item.boardType))}</span>
                                ${item.pinnedYn === 'Y' ? '<span class="badge bg-dark">고정</span>' : ''}
                                <span class="badge ${item.status === 'PUBLISHED' ? 'bg-success-subtle text-success-emphasis' : 'bg-secondary-subtle text-secondary-emphasis'}">${item.status === 'PUBLISHED' ? '게시중' : '임시저장'}</span>
                                <span class="badge ${item.publicYn === 'Y' ? 'bg-primary-subtle text-primary-emphasis' : 'bg-warning-subtle text-warning-emphasis'}">${item.publicYn === 'Y' ? '공개' : '비공개'}</span>
                                <span class="badge bg-light text-dark border">${item.productNo ? `상품 #${item.productNo}` : '미연결'}</span>
                            </div>
                            <span class="content-board-card-views"><i class="far fa-eye me-1"></i>${item.viewCnt}</span>
                        </div>
                        <a class="content-board-card-link" href="/admin/content/get?id=${item.id}&boardType=${item.boardType}&source=content-list&returnTo=${encodeURIComponent(this.getCurrentLocation())}">
                            <h5 class="card-title content-board-card-title text-line-clamp-2">${ContentBoardConfig.escapeHtml(item.title || '제목 없음')}</h5>
                        </a>
                        <p class="content-board-card-copy">${ContentBoardConfig.escapeHtml(item.contentPreview || '내용 미리보기가 없습니다.')}</p>
                    </div>
                    <div class="card-footer content-board-card-footer">
                        <span class="content-board-card-date">${item.crtDtm}</span>
                        <div class="content-board-card-actions">
                            <button class="btn btn-sm btn-light" data-role="content-detail" data-content-id="${item.id}" data-board-type="${item.boardType}">상세</button>
                            <button class="btn btn-sm btn-outline-dark"
                                    data-role="content-status-toggle"
                                    data-content-id="${item.id}"
                                    data-next-status="${item.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED'}">${item.status === 'PUBLISHED' ? '임시저장' : '게시'}</button>
                            <button class="btn btn-sm btn-outline-secondary"
                                    data-role="content-visibility-toggle"
                                    data-content-id="${item.id}"
                                    data-next-public-yn="${item.publicYn === 'Y' ? 'N' : 'Y'}">${item.publicYn === 'Y' ? '비공개' : '공개'}</button>
                            <button class="btn btn-sm btn-outline-primary" data-role="content-edit" data-content-id="${item.id}" data-board-type="${item.boardType}">수정</button>
                            <button class="btn btn-sm btn-outline-danger" data-role="content-delete" data-content-id="${item.id}">삭제</button>
                        </div>
                    </div>
                </div>
            </div>
        `).join('');
        this.bindSelectionEvents();
        this.syncSelectionState();
        this.bindRowActions();
        this.applyOperationPolicy();
    },

    renderLoadingState() {
        const grid = document.getElementById('contentGrid');
        if (!grid) return;
        grid.innerHTML = `
            <div class="col-12">
                <div class="product-loading-state py-5">
                    <div class="spinner-border spinner-border-sm text-primary" role="status" aria-hidden="true"></div>
                    <strong>콘텐츠 목록을 불러오는 중입니다.</strong>
                    <p>선택한 게시판과 검색 조건에 맞는 게시글을 정리하고 있습니다.</p>
                </div>
            </div>
        `;
    },

    renderListError(message) {
        const grid = document.getElementById('contentGrid');
        const pagination = document.getElementById('pagination');
        if (grid) {
            grid.innerHTML = `
                <div class="col-12">
                    <div class="product-empty-state py-5">
                        <i class="fas fa-triangle-exclamation product-empty-state-icon"></i>
                        <strong>콘텐츠 목록을 불러오지 못했습니다.</strong>
                        <p>${ContentBoardConfig.escapeHtml(message)}</p>
                    </div>
                </div>
            `;
        }
        if (pagination) {
            pagination.innerHTML = '';
        }
    },

    buildEmptyStateMessage() {
        const parts = [];
        if (this.state.keyword) parts.push(`검색어 "${this.state.keyword}"`);
        if (this.state.status) parts.push(`상태 ${this.state.status === 'PUBLISHED' ? '게시중' : '임시저장'}`);
        if (this.state.publicYn) parts.push(`공개 ${this.state.publicYn === 'Y' ? '공개' : '비공개'}`);
        if (this.state.startDate || this.state.endDate) parts.push(`기간 ${this.state.startDate || '전체'} ~ ${this.state.endDate || '전체'}`);
        if (this.state.pinnedOnly) parts.push('고정글만');
        if (this.state.productLinked) parts.push(`상품 연결 ${this.state.productLinked === 'Y' ? '연결됨' : '미연결'}`);
        if (this.state.productNo) parts.push(`상품번호 ${this.state.productNo}`);

        if (!parts.length) {
            return '아직 등록된 게시글이 없거나 현재 페이지에 표시할 결과가 없습니다.';
        }

        return `${parts.join(', ')} 조건에 맞는 콘텐츠가 없습니다.`;
    },

    getBoardLabel(boardType) {
        const meta = ContentBoardConfig.getMeta(boardType).list;
        return meta ? meta.boardLabel : boardType;
    },

    renderPagination(data) {
        const totalPages = Number(data.totalPages || 0);
        const curr = Number(data.currentPage || 0);
        const pagination = document.getElementById('pagination');
        if (!pagination) return;

        if (totalPages <= 1) {
            pagination.innerHTML = '';
            return;
        }

        let html = '';
        for (let i = 0; i < totalPages; i++) {
            html += `
                <li class="page-item ${i === curr ? 'active' : ''}">
                    <button type="button" class="page-link" data-role="go-content-page" data-page="${i}">${i + 1}</button>
                </li>`;
        }
        pagination.innerHTML = html;
    },

    goPage(page) {
        if (!Number.isInteger(page) || page < 0) {
            void CommonJS.alert('이동할 페이지 정보가 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        this.state.page = page;
        this.pushState();
        this.getList();
    },

    pushState() {
        const params = this.buildQueryParams();
        const newUrl = `${window.location.pathname}?${params.toString()}`;
        window.history.pushState({ path: newUrl }, '', newUrl);
    },

    buildQueryParams() {
        const params = new URLSearchParams({ boardType: this.state.boardType });
        params.set('page', String(this.state.page));
        params.set('size', String(this.state.size));
        if (this.state.keyword) {
            params.set('keyword', this.state.keyword);
        }
        if (this.normalizeStatusValue(this.state.status)) {
            params.set('status', this.state.status);
        }
        if (this.normalizeYnFilterValue(this.state.publicYn)) {
            params.set('publicYn', this.state.publicYn);
        }
        if (this.state.startDate) {
            params.set('startDate', this.state.startDate);
        }
        if (this.state.endDate) {
            params.set('endDate', this.state.endDate);
        }
        if (this.state.pinnedOnly) {
            params.set('pinnedOnly', 'true');
        }
        if (this.normalizeYnFilterValue(this.state.productLinked)) {
            params.set('productLinked', this.state.productLinked);
        }
        if (this.state.productNo) {
            params.set('productNo', this.state.productNo);
        }
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        if (this.state.returnTo) {
            params.set('returnTo', this.state.returnTo);
        }
        return params;
    },

    syncSearchField() {
        const searchInput = document.getElementById('contentSearchKeyword');
        if (searchInput) {
            searchInput.value = this.state.keyword;
        }
        const statusInput = document.getElementById('contentStatusFilter');
        if (statusInput) {
            statusInput.value = this.state.status;
        }
        const publicInput = document.getElementById('contentPublicFilter');
        if (publicInput) {
            publicInput.value = this.state.publicYn;
        }
        const startDateInput = document.getElementById('contentStartDate');
        if (startDateInput) {
            startDateInput.value = this.state.startDate;
        }
        const endDateInput = document.getElementById('contentEndDate');
        if (endDateInput) {
            endDateInput.value = this.state.endDate;
        }
        const pinnedInput = document.getElementById('contentPinnedOnly');
        if (pinnedInput) {
            pinnedInput.checked = this.state.pinnedOnly;
        }
        const productLinkedInput = document.getElementById('contentProductLinkedFilter');
        if (productLinkedInput) {
            productLinkedInput.value = this.state.productLinked;
        }
        const productNoInput = document.getElementById('contentProductNoFilter');
        if (productNoInput) {
            productNoInput.value = this.state.productNo;
        }
    },

    bindSelectionEvents() {
        document.querySelectorAll('.content-select-checkbox').forEach((checkbox) => {
            checkbox.addEventListener('change', () => {
                const id = this.normalizeNumericId(checkbox.dataset.contentId);
                if (id == null) {
                    checkbox.checked = false;
                    return;
                }
                if (checkbox.checked) {
                    this.state.selectedIds.add(id);
                } else {
                    this.state.selectedIds.delete(id);
                }
                this.syncSelectionState();
            });
        });
    },

    bindRowActions() {
        document.querySelectorAll('[data-role="content-detail"]').forEach((button) => {
            button.addEventListener('click', () => {
                const contentId = this.normalizeNumericId(button.dataset.contentId);
                const boardType = ContentBoardConfig.normalizeBoardType(button.dataset.boardType);
                if (contentId == null) {
                    void CommonJS.alert('콘텐츠 번호가 올바르지 않습니다.', '알림', 'warning');
                    return;
                }
                location.href = `/admin/content/get?id=${contentId}&boardType=${boardType}&source=content-list&returnTo=${encodeURIComponent(this.getCurrentLocation())}`;
            });
        });
        document.querySelectorAll('[data-role="content-edit"]').forEach((button) => {
            button.addEventListener('click', async () => {
                const settings = await CommonJS.fetchSystemSettings();
                if (CommonJS.isCommunityWriteBlocked(settings)) {
                    await CommonJS.alert(CommonJS.getCommunityWriteBlockedReason(settings, '커뮤니티 수정'), '알림', 'warning');
                    return;
                }
                const contentId = this.normalizeNumericId(button.dataset.contentId);
                const boardType = ContentBoardConfig.normalizeBoardType(button.dataset.boardType);
                if (contentId == null) {
                    await CommonJS.alert('콘텐츠 번호가 올바르지 않습니다.', '알림', 'warning');
                    return;
                }
                location.href = `/admin/content/edit?id=${contentId}&boardType=${boardType}&source=content-list&returnTo=${encodeURIComponent(this.getCurrentLocation())}`;
            });
        });
        document.querySelectorAll('[data-role="content-status-toggle"]').forEach((button) => {
            button.addEventListener('click', () => {
                const contentId = this.normalizeNumericId(button.dataset.contentId);
                const nextStatus = this.normalizeBulkStatusValue(button.dataset.nextStatus);
                if (contentId == null || !nextStatus) {
                    void CommonJS.alert('변경할 게시 상태 정보가 올바르지 않습니다.', '알림', 'warning');
                    return;
                }
                this.applyQuickOperate(contentId, { status: nextStatus });
            });
        });
        document.querySelectorAll('[data-role="content-visibility-toggle"]').forEach((button) => {
            button.addEventListener('click', () => {
                const contentId = this.normalizeNumericId(button.dataset.contentId);
                const nextPublicYn = this.normalizeBulkYnActionValue(button.dataset.nextPublicYn);
                if (contentId == null || !nextPublicYn) {
                    void CommonJS.alert('변경할 공개 상태 정보가 올바르지 않습니다.', '알림', 'warning');
                    return;
                }
                this.applyQuickOperate(contentId, { publicYn: nextPublicYn });
            });
        });
        document.querySelectorAll('[data-role="content-delete"]').forEach((button) => {
            button.addEventListener('click', () => {
                const contentId = this.normalizeNumericId(button.dataset.contentId);
                if (contentId == null) {
                    void CommonJS.alert('삭제할 콘텐츠 번호가 올바르지 않습니다.', '알림', 'warning');
                    return;
                }
                this.deleteSingleContent(contentId);
            });
        });
    },

    getCurrentLocation() {
        return `${window.location.pathname}${window.location.search}`;
    },

    syncSelectionState() {
        const meta = document.getElementById('contentSelectionMeta');
        const selectedOnPageCount = this.state.currentPageIds.filter((id) => this.state.selectedIds.has(id)).length;
        if (meta) {
            meta.textContent = `전체 선택 ${this.state.selectedIds.size}건 · 현재 페이지 ${selectedOnPageCount}/${this.state.currentPageIds.length || 0}건`;
        }
        const selectAllOnPage = document.getElementById('contentSelectAllOnPage');
        if (selectAllOnPage) {
            const hasCurrentPageItems = this.state.currentPageIds.length > 0;
            selectAllOnPage.checked = hasCurrentPageItems && selectedOnPageCount === this.state.currentPageIds.length;
            selectAllOnPage.indeterminate = hasCurrentPageItems && selectedOnPageCount > 0 && selectedOnPageCount < this.state.currentPageIds.length;
        }
        const resultMeta = document.getElementById('contentBulkResultMeta');
        if (resultMeta) {
            resultMeta.textContent = this.state.lastBulkResultMessage;
        }
    },

    updateCurrentPageSelection(checked) {
        // 선택 집합은 페이지 이동 후에도 유지해서, 여러 페이지를 넘겨가며 일괄 적용할 수 있게 둔다.
        this.state.currentPageIds.forEach((id) => {
            if (this.normalizeNumericId(id) == null) {
                return;
            }
            if (checked) {
                this.state.selectedIds.add(id);
            } else {
                this.state.selectedIds.delete(id);
            }
        });
        document.querySelectorAll('.content-select-checkbox').forEach((checkbox) => {
            checkbox.checked = checked;
        });
        this.syncSelectionState();
    },

    async applyBulkOperate() {
        if (!this.state.selectedIds.size) {
            await CommonJS.alert('일괄 적용할 게시글을 선택하세요.', '알림', 'warning');
            return;
        }

        const payload = {
            ids: Array.from(this.state.selectedIds),
            status: this.normalizeBulkStatusValue(document.getElementById('contentBulkStatus')?.value),
            publicYn: this.normalizeBulkYnActionValue(document.getElementById('contentBulkPublicYn')?.value),
            pinnedYn: this.normalizeBulkYnActionValue(document.getElementById('contentBulkPinnedYn')?.value)
        };

        if (!payload.status && !payload.publicYn && !payload.pinnedYn) {
            await CommonJS.alert('변경할 항목을 하나 이상 선택하세요.', '알림', 'warning');
            return;
        }

        const confirmed = await CommonJS.confirm(`선택한 게시글 ${this.state.selectedIds.size}건에 일괄 적용하시겠습니까?`);
        if (!confirmed) {
            return;
        }

        const response = await fetch('/api/admin/content/bulk-operate', {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            await CommonJS.alert(await CommonJS.extractErrorMessage(response, '일괄 적용에 실패했습니다.'), '오류', 'error');
            return;
        }

        const result = await response.json();
        if (result.updatedCount > 0) {
            this.state.selectedIds.clear();
            this.state.lastBulkResultMessage = `선택 ${result.requestedCount}건 중 ${result.updatedCount}건을 변경했습니다. ${result.unchangedCount}건은 기존 상태를 유지했고, 누락 ${result.missingCount}건은 건너뛰었으며, 적용 후 선택을 해제했습니다.`;
        } else {
            this.state.lastBulkResultMessage = `선택 ${result.requestedCount}건 중 변경할 항목이 없습니다. 동일 상태 ${result.unchangedCount}건, 누락 ${result.missingCount}건이며 선택은 유지됩니다.`;
        }
        this.syncSelectionState();
        await CommonJS.alert(this.state.lastBulkResultMessage, '성공', 'success');
        this.getList();
    },

    async applyBulkDelete() {
        if (!this.state.selectedIds.size) {
            await CommonJS.alert('삭제할 게시글을 선택하세요.', '알림', 'warning');
            return;
        }

        const confirmed = await CommonJS.confirm(`선택한 게시글 ${this.state.selectedIds.size}건을 삭제하시겠습니까?`);
        if (!confirmed) {
            return;
        }

        const response = await fetch('/api/admin/content/bulk-delete', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ ids: Array.from(this.state.selectedIds) })
        });

        if (!response.ok) {
            await CommonJS.alert(await CommonJS.extractErrorMessage(response, '일괄 삭제에 실패했습니다.'), '오류', 'error');
            return;
        }

        const result = await response.json();
        this.state.selectedIds.clear();
        this.state.lastBulkResultMessage = `선택 ${result.requestedCount}건 중 ${result.deletedCount}건을 삭제했습니다. 누락 ${result.missingCount}건은 이미 삭제되었거나 찾을 수 없습니다.`;
        this.syncSelectionState();
        await CommonJS.alert(this.state.lastBulkResultMessage, '성공', 'success');
        this.getList();
    },

    async exportCsv() {
        if (this.exportInFlight) {
            return;
        }
        const button = document.getElementById('btnExportContentCsv');
        try {
            if (!this.validateState()) {
                return;
            }
            this.exportInFlight = true;
            CommonJS.setButtonDisabled(button, true, '내보내는 중입니다.');
            await CommonJS.downloadFile(`/api/admin/content/export?${this.buildQueryParams().toString()}`, `contents-${this.state.boardType}.csv`);
        } catch (error) {
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.exportInFlight = false;
            CommonJS.setButtonDisabled(button, false);
        }
    },

    async applyQuickOperate(contentId, payload) {
        if (this.actionInFlightIds.has(contentId)) {
            return;
        }
        const settings = await CommonJS.fetchSystemSettings();
        if (CommonJS.isCommunityWriteBlocked(settings)) {
            await CommonJS.alert(CommonJS.getCommunityWriteBlockedReason(settings, '커뮤니티 수정'), '알림', 'warning');
            return;
        }

        this.actionInFlightIds.add(contentId);
        try {
            const response = await fetch(`/api/admin/content/${contentId}/operate`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });
            if (!response.ok) {
                await CommonJS.alert(await CommonJS.extractErrorMessage(response, '콘텐츠 상태 변경에 실패했습니다.'), '오류', 'error');
                return;
            }

            const result = await response.json();
            this.state.lastBulkResultMessage = `콘텐츠 ${contentId}번에 빠른 운영 액션을 적용했습니다. 변경 ${result.updatedCount}건, 유지 ${result.unchangedCount}건입니다.`;
            this.syncSelectionState();
            this.getList();
        } finally {
            this.actionInFlightIds.delete(contentId);
        }
    },

    async deleteSingleContent(contentId) {
        if (this.actionInFlightIds.has(contentId)) {
            return;
        }
        const settings = await CommonJS.fetchSystemSettings();
        if (CommonJS.isCommunityWriteBlocked(settings)) {
            await CommonJS.alert(CommonJS.getCommunityWriteBlockedReason(settings, '커뮤니티 삭제'), '알림', 'warning');
            return;
        }

        const confirmed = await CommonJS.confirm(`콘텐츠 ${contentId}번을 삭제하시겠습니까?`);
        if (!confirmed) {
            return;
        }

        this.actionInFlightIds.add(contentId);
        try {
            const response = await fetch(`/api/admin/content/delete?id=${contentId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                await CommonJS.alert(await CommonJS.extractErrorMessage(response, '콘텐츠 삭제에 실패했습니다.'), '오류', 'error');
                return;
            }

            this.state.selectedIds.delete(contentId);
            this.state.lastBulkResultMessage = `콘텐츠 ${contentId}번을 삭제했습니다.`;
            this.syncSelectionState();
            this.getList();
        } finally {
            this.actionInFlightIds.delete(contentId);
        }
    },

    validateState() {
        if (this.state.startDate && this.state.endDate && this.state.startDate > this.state.endDate) {
            void CommonJS.alert('시작일은 종료일보다 늦을 수 없습니다.', '알림', 'warning');
            return false;
        }
        if (this.state.keyword && this.state.keyword.length > 100) {
            void CommonJS.alert('검색어는 100자 이하로 입력하세요.', '알림', 'warning');
            return false;
        }
        if (this.state.productNo && !this.isPositiveNumber(this.state.productNo)) {
            void CommonJS.alert('상품 번호는 1 이상의 숫자만 입력할 수 있습니다.', '알림', 'warning');
            return false;
        }
        if (this.state.status !== this.normalizeStatusValue(this.state.status)) {
            void CommonJS.alert('게시 상태 값이 올바르지 않습니다.', '알림', 'warning');
            return false;
        }
        if (this.state.publicYn !== this.normalizeYnFilterValue(this.state.publicYn)) {
            void CommonJS.alert('공개 여부 값이 올바르지 않습니다.', '알림', 'warning');
            return false;
        }
        if (this.state.productLinked !== this.normalizeYnFilterValue(this.state.productLinked)) {
            void CommonJS.alert('상품 연결 필터 값이 올바르지 않습니다.', '알림', 'warning');
            return false;
        }

        return true;
    },

    normalizeStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        this.state.boardType = ContentBoardConfig.normalizeBoardType(params.get('boardType') || window.initialContentBoardType);
        this.state.keyword = CommonJS.normalizeOptionalText(params.get('keyword')) || '';
        const status = this.normalizeStatusValue(params.get('status'));
        const publicYn = this.normalizeYnFilterValue(params.get('publicYn'));
        this.state.startDate = params.get('startDate') || '';
        this.state.endDate = params.get('endDate') || '';
        this.state.pinnedOnly = params.get('pinnedOnly') === 'true';
        const productLinked = this.normalizeYnFilterValue(params.get('productLinked'));
        this.state.source = params.get('source') || '';
        this.state.returnTo = CommonJS.normalizeAdminReturnPath(params.get('returnTo'), '');
        this.state.page = this.normalizePage(params.get('page'));
        this.state.size = this.normalizePageSize(params.get('size'));
        this.state.productNo = this.normalizeOptionalPositiveNumber(params.get('productNo'));
        this.state.status = status;
        this.state.publicYn = publicYn;
        this.state.productLinked = productLinked;
    },

    normalizePage(page) {
        const parsed = Number(page);
        return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0;
    },

    normalizePageSize(size) {
        const parsed = Number(size);
        return Number.isInteger(parsed) && parsed > 0 ? parsed : 9;
    },

    normalizeStatusValue(value) {
        return ['', 'DRAFT', 'PUBLISHED'].includes(value) ? value : '';
    },

    normalizeYnFilterValue(value) {
        return ['', 'Y', 'N'].includes(value) ? value : '';
    },

    normalizeBulkStatusValue(value) {
        return ['PUBLISHED', 'DRAFT'].includes(value) ? value : null;
    },

    normalizeBulkYnActionValue(value) {
        return ['Y', 'N'].includes(value) ? value : null;
    },

    normalizeOptionalPositiveNumber(value) {
        return this.isPositiveNumber(value) ? String(Number(value)) : '';
    },

    normalizeNumericId(value) {
        return this.isPositiveNumber(value) ? Number(value) : null;
    },

    isPositiveNumber(value) {
        return /^\d+$/.test(String(value || '')) && Number(value) > 0;
    }
};

document.addEventListener('DOMContentLoaded', () => ContentList.init());
