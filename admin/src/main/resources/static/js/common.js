// /js/common.js
let CommonJS = {

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
        try {
            const error = await response.json();
            return error?.message || fallbackMessage;
        } catch (e) {
            return fallbackMessage;
        }
    }
}

document.addEventListener('DOMContentLoaded', function () {
    if (typeof CommonJS !== 'undefined' && CommonJS && typeof CommonJS.init === 'function') {
        CommonJS.init();
    }
});
