const NoticeDetailPage = {
    initialized: false,
    modal: null,
    operationPolicy: null,
    isSavingDetail: false,
    isTogglingActive: false,
    isDeletingNotice: false,
    state: {
        noticeNo: null,
        returnTo: '/admin/settings/notices',
        source: '',
        currentDetail: null
    },

    init() {
        if (this.initialized) return;
        this.initialized = true;
        const modalEl = document.getElementById('noticeDetailEditModal');
        if (modalEl) {
            this.modal = new bootstrap.Modal(modalEl);
        }
        this.readBootstrapState();
        this.bindEvents();
        this.applyOperationPolicy();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));
        this.loadDetail();
    },

    readBootstrapState() {
        const bootstrapState = window.noticeDetailBootstrap || {};
        this.state.noticeNo = Number(bootstrapState.noticeNo || 0);
        this.state.returnTo = bootstrapState.returnTo || '/admin/settings/notices';
        this.state.source = bootstrapState.source || '';
        this.syncReturnLinks();
        this.syncReturnContextMeta();
        CommonJS.renderSourceContextNotice({ noticeId: 'noticeDetailSourceContextNotice', source: this.state.source });
    },

    bindEvents() {
        document.getElementById('btnBackToNoticeList')?.addEventListener('click', () => {
            window.location.href = this.state.returnTo;
        });
        document.getElementById('btnNoticeDetailEdit')?.addEventListener('click', () => this.openEditModal());
        document.getElementById('btnNoticeDetailSave')?.addEventListener('click', () => this.saveDetail());
        document.getElementById('btnNoticeDetailToggleActive')?.addEventListener('click', () => this.toggleActive());
        document.getElementById('btnNoticeDetailDelete')?.addEventListener('click', () => this.deleteNotice());
        document.getElementById('noticeDetailActionNoticeClose')?.addEventListener('click', () => this.hideLastActionNotice(true));
    },

    async applyOperationPolicy(settings = null) {
        try {
            this.operationPolicy = settings || await CommonJS.fetchSystemSettings();
            const disabled = CommonJS.isAdminWriteBlocked(this.operationPolicy);
            const reason = CommonJS.getAdminWriteBlockedReason('운영 공지 수정 및 삭제');
            CommonJS.setButtonDisabled(document.getElementById('btnNoticeDetailEdit'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnNoticeDetailToggleActive'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnNoticeDetailDelete'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnNoticeDetailSave'), disabled, reason);
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
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
            this.state.currentDetail = data;
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
        const historyPath = this.buildHistoryPathFromBase(data.historyPath);
        document.getElementById('btnNoticeDetailHistory').href = historyPath;
        document.getElementById('btnNoticeDetailHistoryMore').href = historyPath;
        document.getElementById('btnNoticeDetailLog').href = this.buildLogPathFromBase(data.activityLogPath);
        document.getElementById('btnNoticeDetailToggleActive').textContent = data.isActive === 'Y' ? '비활성' : '활성';
        this.renderRecentHistories(data.recentHistories || []);

        const metaEl = document.getElementById('noticeDetailStateMeta');
        if (metaEl) {
            metaEl.dataset.detailState = 'ready';
            metaEl.dataset.stateMessage = '';
            metaEl.dataset.noticeNo = String(data.noticeNo || '');
            metaEl.dataset.displayStatus = data.displayStatus || '';
            metaEl.dataset.returnTo = this.state.returnTo || '/admin/settings/notices';
            metaEl.dataset.returnContext = this.resolveReturnContext();
            metaEl.dataset.sourceContext = this.state.source || '';
            if (!metaEl.dataset.lastAction) {
                metaEl.dataset.lastAction = '';
                metaEl.dataset.lastActionSource = '';
                metaEl.dataset.lastActionStatus = '';
                metaEl.dataset.lastActionHistoryPath = '';
                metaEl.dataset.lastActionLogPath = '';
            }
        }
        CommonJS.renderSourceContextNotice({ noticeId: 'noticeDetailSourceContextNotice', source: this.state.source });
        this.renderLastActionNotice();
    },

    async openEditModal() {
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 공지 수정 및 삭제'), '알림', 'warning');
            return;
        }
        const detail = this.state.currentDetail;
        if (!detail) {
            return;
        }
        document.getElementById('noticeDetailEditTitle').value = detail.title || '';
        document.getElementById('noticeDetailEditContent').value = detail.content || '';
        document.getElementById('noticeDetailEditIsActive').value = detail.isActive || 'Y';
        document.getElementById('noticeDetailEditIsPinned').value = detail.isPinned || 'N';
        document.getElementById('noticeDetailEditStartDtm').value = this.toDateTimeLocalValue(detail.startDtm);
        document.getElementById('noticeDetailEditEndDtm').value = this.toDateTimeLocalValue(detail.endDtm);
        this.modal?.show();
    },

    async saveDetail() {
        if (this.isSavingDetail) return;
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 공지 수정 및 삭제'), '알림', 'warning');
            return;
        }
        const detail = this.state.currentDetail;
        if (!detail) {
            return;
        }

        const payload = {
            noticeNo: detail.noticeNo,
            title: document.getElementById('noticeDetailEditTitle').value.trim(),
            content: document.getElementById('noticeDetailEditContent').value.trim(),
            isActive: document.getElementById('noticeDetailEditIsActive').value,
            isPinned: document.getElementById('noticeDetailEditIsPinned').value,
            startDtm: this.toNullableDateTime(document.getElementById('noticeDetailEditStartDtm').value),
            endDtm: this.toNullableDateTime(document.getElementById('noticeDetailEditEndDtm').value)
        };

        if (!payload.title || !payload.content) {
            await CommonJS.alert('공지 제목과 내용을 입력하세요.', '알림', 'warning');
            return;
        }

        try {
            this.isSavingDetail = true;
            this.setBusyButton(document.getElementById('btnNoticeDetailSave'), true, '저장 중...');
            const response = await fetch('/api/admin/settings/notices/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 공지를 저장하지 못했습니다.'));
            }
            this.setLastActionMeta('save-detail', 'success', '상세 수정');
            this.modal?.hide();
            await this.loadDetail();
            await CommonJS.alert('운영 공지가 저장되었습니다.', '성공', 'success');
        } catch (error) {
            this.setLastActionMeta('save-detail', 'error', '상세 수정');
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isSavingDetail = false;
            this.setBusyButton(document.getElementById('btnNoticeDetailSave'), false);
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    async toggleActive() {
        if (this.isTogglingActive) return;
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 공지 수정 및 삭제'), '알림', 'warning');
            return;
        }
        const detail = this.state.currentDetail;
        if (!detail) {
            return;
        }

        const nextActive = detail.isActive === 'Y' ? 'N' : 'Y';
        try {
            this.isTogglingActive = true;
            this.setBusyButton(document.getElementById('btnNoticeDetailToggleActive'), true, '처리 중...');
            const response = await fetch(`/api/admin/settings/notices/active/${detail.noticeNo}?isActive=${nextActive}`, {
                method: 'PATCH'
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '공지 상태를 변경하지 못했습니다.'));
            }
            this.setLastActionMeta('toggle-active', 'success', '상세 상태 변경');
            await this.loadDetail();
            await CommonJS.alert('운영 공지 상태가 변경되었습니다.', '성공', 'success');
        } catch (error) {
            this.setLastActionMeta('toggle-active', 'error', '상세 상태 변경');
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isTogglingActive = false;
            this.setBusyButton(document.getElementById('btnNoticeDetailToggleActive'), false);
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    async deleteNotice() {
        if (this.isDeletingNotice) return;
        if (this.operationPolicy && CommonJS.isAdminWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(CommonJS.getAdminWriteBlockedReason('운영 공지 수정 및 삭제'), '알림', 'warning');
            return;
        }
        const detail = this.state.currentDetail;
        if (!detail) {
            return;
        }

        const confirmed = await CommonJS.confirm('운영 공지를 삭제하시겠습니까?', '삭제 확인');
        if (!confirmed) {
            return;
        }

        try {
            this.isDeletingNotice = true;
            this.setBusyButton(document.getElementById('btnNoticeDetailDelete'), true, '삭제 중...');
            const response = await fetch(`/api/admin/settings/notices/delete?no=${detail.noticeNo}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '운영 공지를 삭제하지 못했습니다.'));
            }
            this.setLastActionMeta('delete-notice', 'success', '상세 삭제');
            await CommonJS.alert('운영 공지가 삭제되었습니다.', '성공', 'success');
            window.location.href = this.state.returnTo;
        } catch (error) {
            this.setLastActionMeta('delete-notice', 'error', '상세 삭제');
            await CommonJS.alert(error.message, '오류', 'error');
        } finally {
            this.isDeletingNotice = false;
            this.setBusyButton(document.getElementById('btnNoticeDetailDelete'), false);
            await this.applyOperationPolicy(this.operationPolicy);
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
            metaEl.dataset.returnTo = this.state.returnTo || '/admin/settings/notices';
            metaEl.dataset.returnContext = this.resolveReturnContext();
            metaEl.dataset.sourceContext = this.state.source || '';
        }
        this.renderLastActionNotice();
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
                        <a class="btn btn-sm btn-outline-dark" href="${this.buildLogPathFromBase(item.activityLogPath)}">활동 로그</a>
                        <a class="btn btn-sm btn-outline-secondary" href="${this.buildHistoryPath(item.historyPath)}">이력</a>
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

    syncReturnContextMeta() {
        const metaEl = document.getElementById('noticeDetailStateMeta');
        if (!metaEl) return;
        metaEl.dataset.returnTo = this.state.returnTo || '/admin/settings/notices';
        metaEl.dataset.returnContext = this.resolveReturnContext();
        metaEl.dataset.sourceContext = this.state.source || '';
    },

    syncReturnLinks() {
        const returnContext = CommonJS.getReturnContext(this.state.returnTo, '운영 공지');
        const breadcrumbLink = document.getElementById('noticeDetailBreadcrumbLink');
        if (breadcrumbLink) {
            breadcrumbLink.href = this.state.returnTo;
            breadcrumbLink.textContent = returnContext.label;
        }
        const backButton = document.getElementById('btnBackToNoticeList');
        if (backButton) {
            backButton.textContent = `${returnContext.label}로 돌아가기`;
        }
    },

    resolveReturnContext() {
        const returnTo = this.state.returnTo || '';
        if (returnTo.includes('/notices/history')) return 'notice-history';
        if (returnTo.includes('/admin/settings/notices')) return 'notice-list';
        if (returnTo.includes('/admin')) return 'dashboard-or-notice';
        return 'unknown';
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

    toNullableDateTime(value) {
        return value ? `${value}:00` : null;
    },

    toDateTimeLocalValue(value) {
        if (!value || value === '-') {
            return '';
        }
        return value.substring(0, 16);
    },

    setLastActionMeta(action, status, sourceLabel = '운영 공지 상세') {
        const metaEl = document.getElementById('noticeDetailStateMeta');
        if (!metaEl) return;
        metaEl.dataset.lastAction = action || '';
        metaEl.dataset.lastActionSource = sourceLabel || '운영 공지 상세';
        metaEl.dataset.lastActionStatus = status || '';
        metaEl.dataset.lastActionHistoryPath = this.buildHistoryPath();
        metaEl.dataset.lastActionLogPath = this.buildLogPathFromBase(this.state.currentDetail?.activityLogPath);
        this.renderLastActionNotice();
    },

    renderLastActionNotice() {
        const metaEl = document.getElementById('noticeDetailStateMeta');
        const noticeEl = document.getElementById('noticeDetailActionNotice');
        const noticeTextEl = document.getElementById('noticeDetailActionNoticeText');
        const noticeActionsEl = document.getElementById('noticeDetailActionNoticeActions');
        if (!metaEl || !noticeEl || !noticeTextEl || !noticeActionsEl) return;

        const action = metaEl.dataset.lastAction || '';
        const source = metaEl.dataset.lastActionSource || '';
        const status = metaEl.dataset.lastActionStatus || '';
        const historyPath = metaEl.dataset.lastActionHistoryPath || '';
        const logPath = metaEl.dataset.lastActionLogPath || '';

        if (!action || !status) {
            this.hideLastActionNotice(false);
            return;
        }

        const templates = {
            'save-detail:success': '운영 공지 수정 내용을 반영했습니다.',
            'save-detail:error': '운영 공지 수정에 실패했습니다.',
            'toggle-active:success': '운영 공지 상태를 변경했습니다.',
            'toggle-active:error': '운영 공지 상태 변경에 실패했습니다.',
            'delete-notice:success': '운영 공지 삭제를 반영했습니다.',
            'delete-notice:error': '운영 공지 삭제에 실패했습니다.'
        };
        const variants = {
            'save-detail:success': 'alert-success',
            'toggle-active:success': 'alert-primary',
            'delete-notice:success': 'alert-warning',
            'save-detail:error': 'alert-danger',
            'toggle-active:error': 'alert-danger',
            'delete-notice:error': 'alert-danger'
        };

        const sourceMessage = source ? `${source}에서 실행` : '운영 공지 상세에서 실행';
        const message = templates[`${action}:${status}`] || '조치 결과를 확인해 주세요.';
        const variantClass = variants[`${action}:${status}`] || (status === 'success' ? 'alert-success' : 'alert-danger');
        CommonJS.renderActionNotice({
            noticeId: 'noticeDetailActionNotice',
            textId: 'noticeDetailActionNoticeText',
            actionsId: 'noticeDetailActionNoticeActions',
            action,
            status,
            variantClass,
            message: `${sourceMessage} · ${message}`,
            actionsHtml: [
            historyPath ? `<a class="btn btn-sm btn-outline-secondary" href="${historyPath}">이력</a>` : '',
            logPath ? `<a class="btn btn-sm btn-outline-secondary" href="${logPath}">활동 로그</a>` : ''
            ].join('')
        });
    },

    hideLastActionNotice(clearMeta = false) {
        CommonJS.hideActionNotice({
            noticeId: 'noticeDetailActionNotice',
            textId: 'noticeDetailActionNoticeText',
            actionsId: 'noticeDetailActionNoticeActions',
            metaId: 'noticeDetailStateMeta',
            clearMeta,
            metaKeys: [
                'lastAction',
                'lastActionSource',
                'lastActionStatus',
                'lastActionHistoryPath',
                'lastActionLogPath'
            ]
        });
    },

    buildHistoryPath() {
        return this.buildHistoryPathFromBase(this.state.currentDetail?.historyPath);
    },

    buildHistoryPathFromBase(basePath) {
        if (!basePath) return '';
        const [path, rawQuery = ''] = basePath.split('?');
        const params = new URLSearchParams(rawQuery);
        params.set('returnTo', window.location.pathname + window.location.search);
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        return `${path}?${params.toString()}`;
    },

    buildLogPathFromBase(basePath) {
        if (!basePath) return '';
        const [path, rawQuery = ''] = basePath.split('?');
        const params = new URLSearchParams(rawQuery);
        params.set('returnTo', window.location.pathname + window.location.search);
        if (this.state.source) {
            params.set('source', this.state.source);
        }
        return `${path}?${params.toString()}`;
    },

    escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
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

document.addEventListener('DOMContentLoaded', () => NoticeDetailPage.init());
