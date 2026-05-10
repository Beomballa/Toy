// /js/common.js
let CommonJS = {
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
        document.getElementById("main-logo")?.addEventListener("click", function (el){
            window.location.href = "/product/list";
        });
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
    }
}

document.addEventListener('DOMContentLoaded', function () {
    if (typeof CommonJS !== 'undefined' && CommonJS && typeof CommonJS.init === 'function') {
        CommonJS.init();
    }
});
