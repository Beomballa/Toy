// /js/common.js
let CommonJS = {
    initialized: false,
    systemSettingsCache: null,
    systemSettingsEventName: 'admin-system-settings-updated',
    actionNoticeTimers: {},
    orderStatusMeta: {
        ORDERED: { badgeClass: 'badge-ordered', canCancel: true, showDeliveryInput: false, showDeliveryInfo: false, showCompleteDelivery: false },
        PAID: { badgeClass: 'badge-paid', canCancel: true, showDeliveryInput: true, showDeliveryInfo: false, showCompleteDelivery: false },
        PREPARING: { badgeClass: 'badge-preparing', canCancel: false, showDeliveryInput: false, showDeliveryInfo: false, showCompleteDelivery: false },
        SHIPPED: { badgeClass: 'badge-shipped', canCancel: false, showDeliveryInput: false, showDeliveryInfo: true, showCompleteDelivery: true },
        DELIVERED: { badgeClass: 'badge-delivered', canCancel: false, showDeliveryInput: false, showDeliveryInfo: true, showCompleteDelivery: false },
        CANCELLED: { badgeClass: 'badge-cancelled', canCancel: false, showDeliveryInput: false, showDeliveryInfo: false, showCompleteDelivery: false }
    },
    productStatusMeta: {
        ACTIVE: { badgeClass: 'badge-active' },
        HIDDEN: { badgeClass: 'bg-secondary' },
        SOLD_OUT: { badgeClass: 'bg-dark' },
        DELETE: { badgeClass: 'bg-danger' }
    },

    init: function () {
        if (this.initialized) {
            return;
        }
        this.initialized = true;
        this.bindMainLogoNavigation('/admin');
        this.renderOperationPolicyBanner();
    },

    bindMainLogoNavigation: function (targetUrl) {
        const logo = document.getElementById('main-logo');
        if (!logo) return;
        // 로고 이동은 화면별 문맥이 달라 addEventListener 누적보다 onclick 재할당이 안전합니다.
        logo.onclick = () => {
            window.location.href = targetUrl;
        };
    },

    /**
     * 커스텀 알림창 (Alert)
     */
    alert: function(message, title = '알림', type = 'info') {
        return new Promise((resolve) => {
            const icons = {
                info: '<i class="fas fa-info-circle"></i>',
                success: '<i class="fas fa-check-circle"></i>',
                error: '<i class="fas fa-times-circle"></i>',
                warning: '<i class="fas fa-exclamation-triangle"></i>'
            };

            const modalHtml = `
                <div class="custom-modal-overlay" id="customAlertOverlay">
                    <div class="custom-modal">
                        <div class="custom-modal-header">
                            <div class="custom-modal-icon ${type}">${icons[type] || icons.info}</div>
                            <div class="custom-modal-title">${title}</div>
                        </div>
                        <div class="custom-modal-body">${message}</div>
                        <div class="custom-modal-footer">
                            <button type="button" class="custom-btn custom-btn-primary" id="customAlertBtn">확인</button>
                        </div>
                    </div>
                </div>
            `;

            document.body.insertAdjacentHTML('beforeend', modalHtml);
            const overlay = document.getElementById('customAlertOverlay');
            
            // display: flex 먼저 적용 후 opacity 애니메이션
            overlay.style.display = 'flex';
            setTimeout(() => overlay.classList.add('show'), 10);

            const btn = document.getElementById('customAlertBtn');
            btn.focus({ preventScroll: true });

            const handleClose = () => {
                overlay.classList.remove('show');
                setTimeout(() => {
                    overlay.remove();
                    resolve();
                }, 200);
            };

            btn.addEventListener('click', handleClose);
            overlay.addEventListener('keyup', (e) => {
                if (e.key === 'Enter') handleClose();
            });
        });
    },

    /**
     * 커스텀 확인창 (Confirm)
     */
    confirm: function(message, title = '확인', type = 'warning') {
        return new Promise((resolve) => {
            const icons = {
                info: '<i class="fas fa-info-circle"></i>',
                success: '<i class="fas fa-check-circle"></i>',
                error: '<i class="fas fa-times-circle"></i>',
                warning: '<i class="fas fa-question-circle"></i>'
            };

            const modalHtml = `
                <div class="custom-modal-overlay" id="customConfirmOverlay">
                    <div class="custom-modal">
                        <div class="custom-modal-header">
                            <div class="custom-modal-icon ${type}">${icons[type] || icons.info}</div>
                            <div class="custom-modal-title">${title}</div>
                        </div>
                        <div class="custom-modal-body">${message}</div>
                        <div class="custom-modal-footer">
                            <button type="button" class="custom-btn custom-btn-secondary" id="customConfirmCancelBtn">취소</button>
                            <button type="button" class="custom-btn custom-btn-primary" id="customConfirmOkBtn">확인</button>
                        </div>
                    </div>
                </div>
            `;

            document.body.insertAdjacentHTML('beforeend', modalHtml);
            const overlay = document.getElementById('customConfirmOverlay');
            
            overlay.style.display = 'flex';
            setTimeout(() => overlay.classList.add('show'), 10);

            const okBtn = document.getElementById('customConfirmOkBtn');
            const cancelBtn = document.getElementById('customConfirmCancelBtn');
            
            okBtn.focus({ preventScroll: true });

            const close = (result) => {
                overlay.classList.remove('show');
                setTimeout(() => {
                    overlay.remove();
                    resolve(result);
                }, 200);
            };

            okBtn.addEventListener('click', () => close(true));
            cancelBtn.addEventListener('click', () => close(false));

            overlay.addEventListener('keyup', (e) => {
                if (e.key === 'Enter') okBtn.click();
                if (e.key === 'Escape') cancelBtn.click();
            });
        });
    },

    /**
     * 이미지 로드 실패 시 대체 처리
     */
    handleImageError: function(img, fallbackText = '') {
        img.onerror = null; // 무한 루프 방지
        img.style.display = 'none';
        
        // 부모 요소에 대체 텍스트/아이콘 추가
        const parent = img.parentElement;
        if (parent && !parent.querySelector('.img-fallback')) {
            const fallback = document.createElement('div');
            fallback.className = 'img-fallback';
            fallback.innerHTML = fallbackText ? `<span>${fallbackText.substring(0,1)}</span>` : '<i class="fas fa-image"></i>';
            parent.appendChild(fallback);
        }
    },

    buildImageSearchUrl: function(productName = '', modelNum = '', brandName = '') {
        const query = [brandName, productName, modelNum]
            .filter(Boolean)
            .join(' ')
            .trim();

        return `https://www.google.com/search?tbm=isch&q=${encodeURIComponent(query)}`;
    },

    openImageSearch: function(productName = '', modelNum = '', brandName = '') {
        const url = this.buildImageSearchUrl(productName, modelNum, brandName);
        window.open(url, '_blank', 'noopener,noreferrer');
    },

    extractErrorMessage: async function(response, fallbackMessage = '오류가 발생했습니다.') {
        const error = await this.extractError(response);
        return error.message || fallbackMessage;
    },

    extractError: async function(response) {
        try {
            const error = await response.json();
            return {
                code: error?.code || '',
                message: error?.message || ''
            };
        } catch (e) {
            return {
                code: '',
                message: ''
            };
        }
    },

    getOrderStatusMeta: function(statusCode) {
        return this.orderStatusMeta[statusCode] || {
            badgeClass: 'bg-secondary',
            canCancel: false,
            showDeliveryInput: false,
            showDeliveryInfo: false,
            showCompleteDelivery: false
        };
    },

    getProductStatusMeta: function(statusCode) {
        return this.productStatusMeta[statusCode] || {
            badgeClass: 'bg-secondary'
        };
    },

    getReturnContext: function(returnTo, fallbackLabel = '목록') {
        if (!returnTo) {
            return { label: fallbackLabel, buttonLabel: `${fallbackLabel}으로` };
        }

        if (returnTo.includes('/admin/dashboard')) {
            return { label: '대시보드', buttonLabel: '대시보드로' };
        }

        if (returnTo.includes('/admin/orders')) {
            return { label: '주문 관리', buttonLabel: '주문 관리로' };
        }

        if (returnTo.includes('/admin/products')) {
            return { label: '상품 관리', buttonLabel: '상품 관리로' };
        }

        return { label: fallbackLabel, buttonLabel: `${fallbackLabel}으로` };
    },

    normalizeRequiredText: function(value) {
        return (value || '').trim().replaceAll(/\s+/g, ' ');
    },

    normalizeOptionalText: function(value) {
        const normalized = this.normalizeRequiredText(value);
        return normalized ? normalized : null;
    },

    renderListMeta: function(config = {}) {
        const {
            metaTextId,
            filterMetaId,
            resultMetaId,
            pageMetaId,
            resultLabel = '',
            filterCount = 0,
            querySignature = '',
            pageInfoLabel = '',
            filterPrefix = '필터',
            defaultResultText = '결과 메타 없음',
            defaultPageText = '페이지 메타 없음'
        } = config;

        const metaTextEl = metaTextId ? document.getElementById(metaTextId) : null;
        const filterMetaEl = filterMetaId ? document.getElementById(filterMetaId) : null;
        const resultMetaEl = resultMetaId ? document.getElementById(resultMetaId) : null;
        const pageMetaEl = pageMetaId ? document.getElementById(pageMetaId) : null;

        if (metaTextEl) {
            metaTextEl.textContent = resultLabel || defaultResultText;
        }
        if (filterMetaEl) {
            const filterText = querySignature
                ? `${filterPrefix} ${filterCount}개 · ${querySignature}`
                : `${filterPrefix} ${filterCount}개`;
            filterMetaEl.textContent = filterText;
        }
        if (resultMetaEl) {
            resultMetaEl.textContent = resultLabel || defaultResultText;
        }
        if (pageMetaEl) {
            pageMetaEl.textContent = pageInfoLabel || defaultPageText;
        }
    },

    describeTaskSourceContext: function(source) {
        const mapping = {
            'dashboard-task-title': '대시보드 운영 작업 제목에서 진입',
            'dashboard-task-manage': '대시보드 운영 작업 관리 버튼에서 진입',
            'dashboard-task-history': '대시보드 운영 작업 이력 버튼에서 진입',
            'dashboard-task-activity-log': '대시보드 운영 작업 활동 로그 버튼에서 진입',
            'dashboard-unassigned-title': '대시보드 미지정 작업 제목에서 진입',
            'dashboard-unassigned-detail': '대시보드 미지정 작업 상세 버튼에서 진입',
            'dashboard-unassigned-history': '대시보드 미지정 작업 이력 버튼에서 진입',
            'dashboard-unassigned-activity-log': '대시보드 미지정 작업 활동 로그 버튼에서 진입',
            'dashboard-workload-summary': '대시보드 워크로드 요약에서 진입',
            'dashboard-workload-assigned': '대시보드 배정 작업 카드에서 진입',
            'dashboard-workload-overdue-summary': '대시보드 기한 초과 카드에서 진입',
            'dashboard-workload-unassigned-summary': '대시보드 미지정 작업 카드에서 진입',
            'dashboard-workload-assignee': '대시보드 담당자 워크로드에서 진입',
            'dashboard-workload-task-list': '대시보드 담당 작업 버튼에서 진입',
            'dashboard-workload-overdue': '대시보드 기한 초과 작업 버튼에서 진입'
        };
        return mapping[source] || '';
    },

    describeNoticeSourceContext: function(source) {
        const mapping = {
            'dashboard-notice-title': '대시보드 운영 공지 제목에서 진입',
            'dashboard-notice-manage': '대시보드 운영 공지 관리 버튼에서 진입',
            'dashboard-notice-history': '대시보드 운영 공지 이력 버튼에서 진입',
            'dashboard-notice-live': '대시보드 노출중 공지 버튼에서 진입',
            'dashboard-notice-pinned': '대시보드 고정 공지 버튼에서 진입',
            'dashboard-notice-list': '대시보드 전체 공지 버튼에서 진입'
        };
        return mapping[source] || '';
    },

    renderSourceContextNotice: function(config = {}) {
        const { noticeId, source } = config;
        const noticeEl = noticeId ? document.getElementById(noticeId) : null;
        if (!noticeEl) return;
        const message = this.describeTaskSourceContext(source) || this.describeNoticeSourceContext(source);
        if (!message) {
            noticeEl.classList.add('d-none');
            noticeEl.textContent = '';
            noticeEl.dataset.sourceContext = '';
            return;
        }
        noticeEl.classList.remove('d-none');
        noticeEl.textContent = message;
        noticeEl.dataset.sourceContext = source || '';
    },

    renderActionNotice: function(config = {}) {
        const {
            noticeId,
            textId,
            actionsId,
            action = '',
            status = '',
            variantClass = '',
            message = '',
            actionsHtml = '',
            successDurationMs = 5000
        } = config;
        const noticeEl = noticeId ? document.getElementById(noticeId) : null;
        const noticeTextEl = textId ? document.getElementById(textId) : null;
        const noticeActionsEl = actionsId ? document.getElementById(actionsId) : null;
        if (!noticeEl || !noticeTextEl || !noticeActionsEl) {
            return;
        }

        noticeEl.classList.remove('d-none', 'alert-success', 'alert-danger', 'alert-warning', 'alert-primary');
        if (variantClass) {
            noticeEl.classList.add(variantClass);
        }
        noticeTextEl.textContent = message;
        noticeActionsEl.innerHTML = actionsHtml;
        noticeEl.dataset.visible = 'Y';
        noticeEl.dataset.action = action;
        noticeEl.dataset.status = status;

        this.clearActionNoticeHide(noticeId);
        if (status === 'success') {
            this.actionNoticeTimers[noticeId] = window.setTimeout(() => {
                this.hideActionNotice({ noticeId, textId, actionsId });
            }, successDurationMs);
        }
    },

    clearActionNoticeHide: function(noticeId) {
        if (!noticeId || !this.actionNoticeTimers[noticeId]) {
            return;
        }
        window.clearTimeout(this.actionNoticeTimers[noticeId]);
        delete this.actionNoticeTimers[noticeId];
    },

    hideActionNotice: function(config = {}) {
        const {
            noticeId,
            textId,
            actionsId,
            metaId,
            clearMeta = false,
            metaKeys = []
        } = config;
        const noticeEl = noticeId ? document.getElementById(noticeId) : null;
        const noticeTextEl = textId ? document.getElementById(textId) : null;
        const noticeActionsEl = actionsId ? document.getElementById(actionsId) : null;
        const metaEl = metaId ? document.getElementById(metaId) : null;

        this.clearActionNoticeHide(noticeId);

        if (noticeEl) {
            noticeEl.classList.add('d-none');
            noticeEl.classList.remove('alert-success', 'alert-danger', 'alert-warning', 'alert-primary');
            noticeEl.dataset.visible = 'N';
            noticeEl.dataset.action = '';
            noticeEl.dataset.status = '';
        }
        if (noticeTextEl) {
            noticeTextEl.textContent = '';
        }
        if (noticeActionsEl) {
            noticeActionsEl.innerHTML = '';
        }

        if (!clearMeta || !metaEl) {
            return;
        }
        metaKeys.forEach((key) => {
            metaEl.dataset[key] = '';
        });
    },

    fetchSystemSettings: async function(forceRefresh = false) {
        if (!forceRefresh && this.systemSettingsCache) {
            return this.systemSettingsCache;
        }

        const response = await fetch('/api/admin/settings/system');
        if (!response.ok) {
            throw new Error(await this.extractErrorMessage(response, '운영 설정을 불러오지 못했습니다.'));
        }

        return this.setSystemSettingsCache(await response.json());
    },

    setSystemSettingsCache: function(settings) {
        this.systemSettingsCache = settings;
        window.dispatchEvent(new CustomEvent(this.systemSettingsEventName, { detail: settings }));
        this.renderOperationPolicyBanner(settings);
        return settings;
    },

    isAdminWriteBlocked: function(settings) {
        return !!settings?.maintenanceMode;
    },

    isCommunityWriteBlocked: function(settings) {
        return this.isAdminWriteBlocked(settings) || settings?.communityWriteEnabled === false;
    },

    isOrderExportBlocked: function(settings) {
        return settings?.orderExportEnabled === false;
    },

    getAdminWriteBlockedReason: function(actionLabel = '관리 작업') {
        return `유지보수 모드에서는 ${actionLabel}이 불가능합니다.`;
    },

    getCommunityWriteBlockedReason: function(settings, actionLabel = '커뮤니티 작성') {
        if (this.isAdminWriteBlocked(settings)) {
            return this.getAdminWriteBlockedReason(actionLabel);
        }
        return `현재 설정에서 ${actionLabel} 기능이 비활성화되어 있습니다.`;
    },

    getOrderExportBlockedReason: function() {
        return '현재 설정에서 주문 CSV 내보내기 기능이 비활성화되어 있습니다.';
    },

    setButtonDisabled: function(button, disabled, reason = '') {
        if (!button) return;
        button.disabled = disabled;
        button.dataset.policyDisabled = String(!!disabled);
        if (disabled && reason) {
            button.title = reason;
            button.dataset.policyDisabledReason = reason;
        } else {
            button.removeAttribute('title');
            delete button.dataset.policyDisabledReason;
        }
    },

    renderOperationPolicyBanner: async function(settings = null) {
        const host = document.querySelector('main.main-content, main#main-content');
        if (!host) {
            return;
        }

        try {
            const resolvedSettings = settings || await this.fetchSystemSettings();
            const banner = document.getElementById('operationPolicyBanner') || document.createElement('div');
            banner.id = 'operationPolicyBanner';
            banner.className = 'operation-policy-banner';
            const items = this.buildOperationPolicyBannerItems(resolvedSettings);
            banner.dataset.maintenanceMode = String(!!resolvedSettings.maintenanceMode);
            banner.dataset.communityWriteEnabled = String(resolvedSettings.communityWriteEnabled !== false);
            banner.dataset.orderExportEnabled = String(resolvedSettings.orderExportEnabled !== false);
            banner.dataset.lowStockDefaultThreshold = String(resolvedSettings.lowStockDefaultThreshold || 100);
            banner.innerHTML = `
                <div class="operation-policy-banner__title">
                    <i class="fas fa-shield-alt"></i>
                    <span>현재 운영 정책</span>
                </div>
                <div class="operation-policy-banner__items">
                    ${items.map((item) => `<span class="operation-policy-banner__item">${item}</span>`).join('')}
                </div>
            `;
            if (!banner.isConnected) {
                host.insertAdjacentElement('afterbegin', banner);
            }
        } catch (error) {
            console.error('운영 정책 배너 로드 실패:', error);
        }
    },

    buildOperationPolicyBannerItems: function(settings) {
        const items = [];
        if (settings.maintenanceMode) {
            items.push('유지보수 모드 활성화');
        }
        if (settings.communityWriteEnabled === false) {
            items.push('커뮤니티 작성 비활성화');
        }
        if (settings.orderExportEnabled === false) {
            items.push('주문 CSV 비활성화');
        }

        // 운영 기본값은 화면 해석 비용을 줄이는 정보라, 경고성 상태가 아니어도 같이 보여줍니다.
        items.push(`기본 저재고 기준 ${settings.lowStockDefaultThreshold || 100}개 미만`);
        return items;
    }
}

document.addEventListener('DOMContentLoaded', function () {
    if (typeof CommonJS !== 'undefined' && CommonJS && typeof CommonJS.init === 'function') {
        CommonJS.init();
    }
});
