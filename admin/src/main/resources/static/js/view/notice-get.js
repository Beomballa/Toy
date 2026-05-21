const NoticeDetailPage = {
    initialized: false,
    state: {
        noticeNo: null,
        returnTo: '/admin/settings/notices'
    },

    init() {
        if (this.initialized) return;
        this.initialized = true;
        this.readBootstrapState();
        this.bindEvents();
        this.loadDetail();
    },

    readBootstrapState() {
        const bootstrapState = window.noticeDetailBootstrap || {};
        this.state.noticeNo = Number(bootstrapState.noticeNo || 0);
        this.state.returnTo = bootstrapState.returnTo || '/admin/settings/notices';
        const breadcrumbLink = document.getElementById('noticeDetailBreadcrumbLink');
        if (breadcrumbLink) {
            breadcrumbLink.href = this.state.returnTo;
        }
    },

    bindEvents() {
        document.getElementById('btnBackToNoticeList')?.addEventListener('click', () => {
            window.location.href = this.state.returnTo;
        });
    },

    async loadDetail() {
        if (!this.state.noticeNo) {
            this.renderError('운영 공지 번호가 없습니다.');
            return;
        }

        try {
            const response = await fetch(`/api/admin/settings/notices/${this.state.noticeNo}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 공지 상세를 불러오지 못했습니다.'));
            }
            const data = await response.json();
            this.renderDetail(data);
        } catch (error) {
            this.renderError(error.message);
        }
    },

    renderDetail(data) {
        document.getElementById('noticeDetailTitle').textContent = data.title || '-';
        document.getElementById('noticeDetailStatus').innerHTML = this.renderStatusBadge(data.displayStatus);
        document.getElementById('noticeDetailIsActive').textContent = data.isActive === 'Y' ? '활성' : '비활성';
        document.getElementById('noticeDetailIsPinned').textContent = data.isPinned === 'Y' ? '고정' : '일반';
        document.getElementById('noticeDetailStartDtm').textContent = data.startDtm || '-';
        document.getElementById('noticeDetailEndDtm').textContent = data.endDtm || '-';
        document.getElementById('noticeDetailContent').innerHTML = this.escapeHtml(data.content || '-').replace(/\n/g, '<br>');
        document.getElementById('noticeDetailCrtDtm').textContent = data.crtDtm || '-';
        document.getElementById('noticeDetailMeta').textContent = `운영 공지 #${data.noticeNo}`;
        document.getElementById('noticeDetailSummary').textContent = `${data.displayStatus} · ${data.isPinned === 'Y' ? '고정 공지' : '일반 공지'}`;
        const historyPath = `${data.historyPath}&returnTo=${encodeURIComponent(window.location.pathname + window.location.search)}`;
        document.getElementById('btnNoticeDetailHistory').href = historyPath;
        document.getElementById('btnNoticeDetailHistoryMore').href = historyPath;
        document.getElementById('btnNoticeDetailLog').href = data.activityLogPath;
        this.renderRecentHistories(data.recentHistories || []);

        const metaEl = document.getElementById('noticeDetailStateMeta');
        if (metaEl) {
            metaEl.dataset.detailState = 'ready';
            metaEl.dataset.stateMessage = '';
            metaEl.dataset.noticeNo = String(data.noticeNo || '');
            metaEl.dataset.displayStatus = data.displayStatus || '';
        }
    },

    renderError(message) {
        document.getElementById('noticeDetailTitle').textContent = message;
        document.getElementById('noticeDetailMeta').textContent = '상세 확인 불가';
        document.getElementById('noticeDetailSummary').textContent = '운영 공지 상세 조회에 실패했습니다.';
        const historyList = document.getElementById('noticeDetailRecentHistoryList');
        if (historyList) {
            historyList.innerHTML = `<div class="text-danger small">${this.escapeHtml(message)}</div>`;
        }
        const metaEl = document.getElementById('noticeDetailStateMeta');
        if (metaEl) {
            metaEl.dataset.detailState = 'error';
            metaEl.dataset.stateMessage = message;
        }
        const historyMetaEl = document.getElementById('noticeDetailHistoryStateMeta');
        if (historyMetaEl) {
            historyMetaEl.dataset.listState = 'error';
            historyMetaEl.dataset.stateMessage = message;
            historyMetaEl.dataset.visibleCount = '0';
        }
    },

    renderRecentHistories(items) {
        const listEl = document.getElementById('noticeDetailRecentHistoryList');
        const metaEl = document.getElementById('noticeDetailHistoryStateMeta');
        if (!listEl) {
            return;
        }

        if (!items.length) {
            listEl.innerHTML = '<div class="text-muted small">최근 이력이 없습니다.</div>';
            if (metaEl) {
                metaEl.dataset.listState = 'empty';
                metaEl.dataset.stateMessage = '최근 이력이 없습니다.';
                metaEl.dataset.visibleCount = '0';
            }
            return;
        }

        listEl.innerHTML = items.map((item) => `
            <div class="list-group-item px-0">
                <div class="d-flex justify-content-between align-items-start gap-3">
                    <div>
                        <div class="fw-semibold">${this.escapeHtml(item.actionLabel)}</div>
                        <div class="small text-muted">${this.escapeHtml(item.adminName || '-')} · ${this.escapeHtml(item.actionDtm || '-')}</div>
                    </div>
                    <div class="d-flex gap-2">
                        <a class="btn btn-sm btn-outline-dark" href="${item.activityLogPath}">활동 로그</a>
                        <a class="btn btn-sm btn-outline-secondary" href="${item.historyPath}">이력</a>
                    </div>
                </div>
            </div>
        `).join('');

        if (metaEl) {
            metaEl.dataset.listState = 'ready';
            metaEl.dataset.stateMessage = '';
            metaEl.dataset.visibleCount = String(items.length);
        }
    },

    renderStatusBadge(displayStatus) {
        const status = displayStatus || '-';
        const badgeClass = status === '노출중'
            ? 'badge-y'
            : status === '예약'
                ? 'text-bg-warning'
                : 'badge-n';
        return `<span class="badge rounded-pill ${badgeClass}">${this.escapeHtml(status)}</span>`;
    },

    escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }
};

document.addEventListener('DOMContentLoaded', () => NoticeDetailPage.init());
