const ContentEdit = {
    initialized: false,
    debounceTimer: null,
    statusTimer: null,
    isSaving: false,
    isDeleting: false,
    operationPolicy: null,
    detailRequestId: 0,
    source: '',
    initialData: {
        title: '',
        content: '',
        boardType: '',
        productNo: '',
        status: 'DRAFT',
        publicYn: 'Y',
        pinnedYn: 'N'
    },

    async init() {
        if (this.initialized) return;
        this.initialized = true;
        const rawContentId = String(document.getElementById('contentId').value || '').trim();
        this.id = this.normalizeContentId(rawContentId);
        this.initialBoardType = ContentBoardConfig.normalizeBoardType(
            document.getElementById('initialBoardType')?.value
        );
        this.returnTo = CommonJS.normalizeAdminReturnPath(document.getElementById('contentReturnTo')?.value, '');
        this.source = CommonJS.normalizeOptionalText(document.getElementById('contentSource')?.value) || '';
        const boardTypeSelect = document.getElementById('boardType');

        if (boardTypeSelect) {
            boardTypeSelect.value = this.initialBoardType;
        }
        document.getElementById('status').value = this.normalizeStatusValue(this.initialData.status);
        document.getElementById('publicYn').value = this.normalizeYnValue(this.initialData.publicYn, 'Y');
        document.getElementById('pinnedYn').value = this.normalizeYnValue(this.initialData.pinnedYn, 'N');

        this.applyBoardMeta(boardTypeSelect?.value || this.initialBoardType);
        CommonJS.renderSourceContextNotice({ noticeId: 'contentEditSourceContextNotice', source: this.source });
        this.syncVisibilitySummary();
        this.syncProductSummary();
        await this.applyOperationPolicy();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));
        
        if (rawContentId && !this.id) {
            await CommonJS.alert('유효하지 않은 콘텐츠 번호입니다.', '오류', 'error');
            location.href = this.getListPath();
            return;
        }
        if (this.id && !await this.getDetail()) return;
        this.bindEvents();
    },

    bindEvents() {
        document.getElementById('contentEditForm')?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.saveContent(false);
        });

        // 삭제 버튼
        document.getElementById('btnDelete')?.addEventListener('click', () => {
            this.deleteContent();
        });

        // 자동 저장 (입력 시)
        const textInputs = [document.getElementById('title'), document.getElementById('content'), document.getElementById('productNo')];
        textInputs.forEach(el => {
            el?.addEventListener('input', () => {
                if (el.id === 'productNo') {
                    this.syncProductSummary();
                }
                this.scheduleAutoSave();
            });
        });

        ['boardType', 'status', 'publicYn', 'pinnedYn'].forEach((id) => {
            document.getElementById(id)?.addEventListener('change', (e) => {
                if (id === 'boardType') {
                    this.applyBoardMeta(e.target.value);
                }
                this.syncVisibilitySummary();
                this.scheduleAutoSave();
            });
        });

        document.getElementById('btnBackToList')?.addEventListener('click', () => {
            location.href = this.getListPath();
        });
    },

    async getDetail() {
        const requestId = ++this.detailRequestId;
        if (!this.isValidContentId(this.id)) {
            await CommonJS.alert('유효하지 않은 콘텐츠 번호입니다.', '알림', 'warning');
            location.href = this.getListPath();
            return false;
        }
        this.setEditorLoading(true);
        try {
            const res = await fetch(`/api/admin/content/get?id=${this.id}`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            const data = this.normalizeContentDetail(await res.json());
            if (requestId !== this.detailRequestId) return;
            if (!data) {
                throw new Error('요청한 콘텐츠와 상세 응답 정보가 일치하지 않습니다.');
            }
            document.getElementById('title').value = data.title;
            document.getElementById('content').value = data.content;
            document.getElementById('boardType').value = data.boardType;
            document.getElementById('productNo').value = data.productNo;
            document.getElementById('status').value = data.status;
            document.getElementById('publicYn').value = data.publicYn;
            document.getElementById('pinnedYn').value = data.pinnedYn;
            
            this.initialData = {
                title: data.title,
                content: data.content,
                boardType: data.boardType,
                productNo: data.productNo,
                status: data.status,
                publicYn: data.publicYn,
                pinnedYn: data.pinnedYn
            };
            this.applyBoardMeta(this.initialData.boardType);
            this.syncVisibilitySummary();
            this.syncProductSummary();
            this.setEditorLoading(false);
            await this.applyOperationPolicy(this.operationPolicy);
            return true;
        } catch (err) {
            if (requestId !== this.detailRequestId) return;
            console.error('콘텐츠 로드 실패:', err);
            await CommonJS.alert('내용을 불러오는 중 오류가 발생했습니다.', '오류', 'error');
            location.href = this.getListPath();
            return false;
        }
    },

    scheduleAutoSave() {
        clearTimeout(this.debounceTimer);
        this.setStatus('자동 저장 대기 중...');
        this.debounceTimer = setTimeout(() => {
            this.saveContent(true);
        }, 5000);
    },

    setStatus(message, clearAfterMs = 0) {
        const statusEl = document.getElementById('saveStatus');
        if (!statusEl) return;

        clearTimeout(this.statusTimer);
        statusEl.textContent = message;

        if (clearAfterMs > 0) {
            this.statusTimer = setTimeout(() => {
                statusEl.textContent = '';
            }, clearAfterMs);
        }
    },

    applyBoardMeta(boardType) {
        const normalizedBoardType = ContentBoardConfig.normalizeBoardType(boardType);
        const meta = ContentBoardConfig.getMeta(normalizedBoardType);
        const titleEl = document.getElementById('contentEditTitle');
        const saveBtn = document.getElementById('btnSave');
        const saveBtnLabel = document.getElementById('btnSaveLabel');
        const breadcrumbEl = document.getElementById('contentEditBreadcrumb');
        const badgeEl = document.getElementById('contentBoardBadge');
        const descEl = document.getElementById('contentEditDescription');
        const boardNameEl = document.getElementById('contentBoardName');
        const sideNoteEl = document.getElementById('contentSideNote');
        const listBreadcrumbEl = document.getElementById('contentListBreadcrumb');
        const backButton = document.getElementById('btnBackToList');
        const returnContext = this.returnTo
            ? CommonJS.getReturnContext(this.returnTo, meta.edit.boardName)
            : null;

        const pageTitle = this.id ? meta.edit.title.replace('작성', '수정') : meta.edit.title;

        if (titleEl) titleEl.textContent = pageTitle;
        if (breadcrumbEl) breadcrumbEl.textContent = pageTitle;
        if (saveBtn) saveBtn.setAttribute('aria-label', meta.edit.saveLabel);
        if (saveBtnLabel) saveBtnLabel.textContent = meta.edit.saveLabel;
        if (badgeEl) badgeEl.textContent = meta.badge;
        if (descEl) descEl.textContent = meta.edit.description;
        if (boardNameEl) boardNameEl.textContent = meta.edit.boardName;
        if (sideNoteEl) sideNoteEl.textContent = meta.edit.sideNote;
        if (listBreadcrumbEl) {
            listBreadcrumbEl.textContent = returnContext?.label || meta.edit.boardName;
            listBreadcrumbEl.href = this.getListPath();
        }
        if (backButton) {
            backButton.replaceChildren();
            const icon = document.createElement('i');
            icon.className = 'fas fa-list me-2';
            icon.setAttribute('aria-hidden', 'true');
            backButton.append(icon, document.createTextNode(returnContext?.buttonLabel || `${meta.edit.boardName}로`));
        }
    },

    getListPath() {
        if (this.returnTo) {
            return this.returnTo;
        }
        const boardType = document.getElementById('boardType')?.value || this.initialBoardType;
        return ContentBoardConfig.getListPath(boardType);
    },

    async applyOperationPolicy(settings = null) {
        try {
            this.operationPolicy = settings || await CommonJS.fetchSystemSettings();
            const disabled = CommonJS.isCommunityWriteBlocked(this.operationPolicy);
            const reason = CommonJS.getCommunityWriteBlockedReason(this.operationPolicy, '커뮤니티 저장 및 삭제');
            CommonJS.setButtonDisabled(document.getElementById('btnSave'), disabled, reason);
            CommonJS.setButtonDisabled(document.getElementById('btnDelete'), disabled, reason);
            if (disabled) {
                this.setStatus(reason);
            }
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
    },

    async saveContent(isAutoSave) {
        if (this.isSaving || this.isDeleting) return;
        if (!isAutoSave) clearTimeout(this.debounceTimer);
        if (this.operationPolicy && CommonJS.isCommunityWriteBlocked(this.operationPolicy)) {
            if (!isAutoSave) {
                const message = CommonJS.getCommunityWriteBlockedReason(this.operationPolicy, '커뮤니티 저장');
                await CommonJS.alert(message, '알림', 'warning');
            }
            return;
        }

        const title = document.getElementById('title').value;
        const content = document.getElementById('content').value;
        const boardType = document.getElementById('boardType').value;
        const productNo = document.getElementById('productNo').value.trim();
        const status = document.getElementById('status').value;
        const publicYn = document.getElementById('publicYn').value;
        const pinnedYn = document.getElementById('pinnedYn').value;
        const normalizedTitle = CommonJS.normalizeRequiredText(title);
        const normalizedContent = CommonJS.normalizeRequiredText(content);
        const parsedProductNo = this.parseProductNo(productNo);
        const normalizedProductNo = parsedProductNo ? String(parsedProductNo) : '';

        if (!normalizedTitle) {
            if (!isAutoSave) await CommonJS.alert('제목을 입력하세요.', '알림', 'warning');
            return;
        }
        if (normalizedTitle.length > 200) {
            if (!isAutoSave) await CommonJS.alert('제목은 200자 이내로 입력하세요.', '알림', 'warning');
            return;
        }
        if (!normalizedContent || normalizedContent.length > 10000) {
            this.setStatus('본문은 필수이며 10,000자 이내로 입력해야 합니다.', 4000);
            if (!isAutoSave) await CommonJS.alert('본문은 필수이며 10,000자 이내로 입력하세요.', '알림', 'warning');
            return;
        }
        if (productNo && parsedProductNo == null) {
            this.setStatus('상품 번호는 1 이상의 숫자만 입력할 수 있습니다.', 4000);
            if (!isAutoSave) {
                await CommonJS.alert('상품 번호는 1 이상의 숫자만 입력하세요.', '알림', 'warning');
            }
            return;
        }
        if (!this.isValidBoardType(boardType) || !this.isValidStatus(status) || !this.isValidYn(publicYn) || !this.isValidYn(pinnedYn)) {
            this.setStatus('유효하지 않은 공개 상태 또는 게시 상태입니다.', 4000);
            if (!isAutoSave) {
                await CommonJS.alert('유효하지 않은 공개 상태 또는 게시 상태입니다.', '알림', 'warning');
            }
            return;
        }

        // 자동 저장은 실제 변경이 생긴 경우에만 보내서 불필요한 저장 요청을 줄인다.
        if (this.isSameAsInitial(normalizedTitle, normalizedContent, boardType, normalizedProductNo, status, publicYn, pinnedYn)) {
            if (!isAutoSave) {
                await CommonJS.alert('변경된 내용이 없습니다.', '알림', 'info');
            }
            return;
        }
        if (isAutoSave && 
            normalizedTitle === this.initialData.title && 
            normalizedContent === this.initialData.content && 
            boardType === this.initialData.boardType &&
            normalizedProductNo === this.initialData.productNo &&
            status === this.initialData.status &&
            publicYn === this.initialData.publicYn &&
            pinnedYn === this.initialData.pinnedYn) {
            return;
        }

        this.isSaving = true;
        this.setSaveDisabled(true, isAutoSave ? '자동 저장 중...' : '저장 중...');
        this.setStatus(isAutoSave ? '자동 저장 중...' : '저장 중...');

        try {
            const res = await fetch('/api/admin/content/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    id: this.id || null,
                    title: normalizedTitle,
                    content: normalizedContent,
                    boardType: boardType,
                    productNo: parsedProductNo,
                    status: status,
                    publicYn: publicYn,
                    pinnedYn: pinnedYn
                })
            });

            if (!res.ok) {
                throw new Error(await CommonJS.extractErrorMessage(res, '콘텐츠 저장 중 오류가 발생했습니다.'));
            }

            const saved = await res.json();
            const savedId = this.normalizeContentId(saved?.id);
            if (!savedId) {
                throw new Error('저장된 콘텐츠 번호를 확인할 수 없습니다.');
            }
            if (this.id && savedId !== this.id) {
                throw new Error('저장 요청과 응답의 콘텐츠 번호가 일치하지 않습니다.');
            }
            if (savedId) {
                this.id = savedId;
                document.getElementById('contentId').value = String(savedId);
                const url = new URL(window.location.href);
                url.searchParams.set('id', String(savedId));
                url.searchParams.set('boardType', boardType);
                window.history.replaceState({}, '', url);
            }

            this.initialData = { title: normalizedTitle, content: normalizedContent, boardType, productNo: normalizedProductNo, status, publicYn, pinnedYn };
            this.setStatus(isAutoSave ? '임시 저장되었습니다.' : '저장되었습니다.', 3000);

            if (isAutoSave && this.hasUnsavedFormChanges()) {
                this.scheduleAutoSave();
            }

            if (!isAutoSave) {
                await CommonJS.alert('성공적으로 저장되었습니다.', '성공', 'success');
                location.href = this.getListPath();
            }
        } catch (err) {
            console.error('저장 실패:', err);
            this.setStatus('저장에 실패했습니다.', 4000);
            if (!isAutoSave) {
                await CommonJS.alert('저장 중 오류가 발생했습니다.', '오류', 'error');
            }
        } finally {
            this.isSaving = false;
            this.setSaveDisabled(false);
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    async deleteContent() {
        if (this.isDeleting || this.isSaving) {
            return;
        }
        if (!this.isValidContentId(this.id)) {
            await CommonJS.alert('삭제할 콘텐츠 번호가 올바르지 않습니다.', '알림', 'warning');
            return;
        }
        if (this.operationPolicy && CommonJS.isCommunityWriteBlocked(this.operationPolicy)) {
            await CommonJS.alert(
                CommonJS.getCommunityWriteBlockedReason(this.operationPolicy, '커뮤니티 삭제'),
                '알림',
                'warning'
            );
            return;
        }
        const confirm = await CommonJS.confirm('정말 삭제하시겠습니까?');
        if (!confirm) return;
        clearTimeout(this.debounceTimer);

        try {
            this.isDeleting = true;
            this.setDeleteDisabled(true);
            const res = await fetch(`/api/admin/content/delete?id=${this.id}`, {
                method: 'DELETE'
            });

            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            await CommonJS.alert('삭제되었습니다.', '성공', 'success');
            location.href = this.getListPath();
        } catch (err) {
            console.error('삭제 실패:', err);
            await CommonJS.alert('삭제 중 오류가 발생했습니다.', '오류', 'error');
        } finally {
            this.isDeleting = false;
            this.setDeleteDisabled(false);
            await this.applyOperationPolicy(this.operationPolicy);
        }
    },

    setDeleteDisabled(disabled) {
        const deleteButton = document.getElementById('btnDelete');
        if (!deleteButton) return;
        if (disabled) {
            if (!deleteButton.dataset.originalText) {
                deleteButton.dataset.originalText = deleteButton.textContent;
            }
            deleteButton.disabled = true;
            deleteButton.textContent = '삭제 중...';
            return;
        }
        deleteButton.disabled = false;
        if (deleteButton.dataset.originalText) {
            deleteButton.textContent = deleteButton.dataset.originalText;
            delete deleteButton.dataset.originalText;
        }
    },

    setSaveDisabled(disabled, label = '저장 중...') {
        const saveButton = document.getElementById('btnSave');
        const saveLabel = document.getElementById('btnSaveLabel');
        if (!saveButton) {
            return;
        }
        if (disabled) {
            if (!saveButton.dataset.originalText) {
                saveButton.dataset.originalText = saveLabel?.textContent || saveButton.textContent;
            }
            saveButton.disabled = true;
            if (saveLabel) {
                saveLabel.textContent = label;
            } else {
                saveButton.textContent = label;
            }
            return;
        }
        saveButton.disabled = false;
        const originalText = saveButton.dataset.originalText || '저장';
        if (saveLabel) {
            saveLabel.textContent = originalText;
        } else {
            saveButton.textContent = originalText;
        }
        if (saveButton.dataset.originalText) {
            delete saveButton.dataset.originalText;
        }
    },

    parseProductNo(rawValue) {
        if (!rawValue) {
            return null;
        }
        if (!/^\d+$/.test(rawValue)) {
            return null;
        }
        const parsed = Number(rawValue);
        return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : null;
    },

    normalizeContentId(rawValue) {
        const parsed = Number(rawValue);
        return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : null;
    },

    isValidContentId(contentId) {
        return Number.isSafeInteger(Number(contentId)) && Number(contentId) > 0;
    },

    isValidStatus(status) {
        return status === 'DRAFT' || status === 'PUBLISHED';
    },

    isValidBoardType(boardType) {
        return Object.prototype.hasOwnProperty.call(ContentBoardConfig.BOARD_META, boardType);
    },

    isValidYn(value) {
        return value === 'Y' || value === 'N';
    },

    normalizeStatusValue(status) {
        return this.isValidStatus(status) ? status : 'DRAFT';
    },

    normalizeYnValue(value, fallback = 'Y') {
        return this.isValidYn(value) ? value : fallback;
    },

    isSameAsInitial(title, content, boardType, productNo, status, publicYn, pinnedYn) {
        return title === this.initialData.title
            && content === this.initialData.content
            && boardType === this.initialData.boardType
            && productNo === this.initialData.productNo
            && status === this.initialData.status
            && publicYn === this.initialData.publicYn
            && pinnedYn === this.initialData.pinnedYn;
    },

    normalizeContentDetail(data) {
        if (!data || this.normalizeContentId(data.id) !== this.id) return null;
        const title = CommonJS.normalizeRequiredText(data.title);
        const content = CommonJS.normalizeRequiredText(data.content);
        const productNo = data.productNo == null ? '' : String(this.parseProductNo(String(data.productNo)) || '');
        if (!title || title.length > 200 || !content || content.length > 10000) return null;
        if (!this.isValidBoardType(data.boardType) || !this.isValidStatus(data.status)) return null;
        if (!this.isValidYn(data.publicYn) || !this.isValidYn(data.pinnedYn)) return null;
        if (data.productNo != null && !productNo) return null;
        return { ...data, id: this.id, title, content, productNo, boardType: data.boardType, status: data.status, publicYn: data.publicYn, pinnedYn: data.pinnedYn };
    },

    hasUnsavedFormChanges() {
        const title = CommonJS.normalizeRequiredText(document.getElementById('title')?.value);
        const content = CommonJS.normalizeRequiredText(document.getElementById('content')?.value);
        const boardType = document.getElementById('boardType')?.value || '';
        const rawProductNo = document.getElementById('productNo')?.value.trim() || '';
        const parsedProductNo = this.parseProductNo(rawProductNo);
        const productNo = parsedProductNo ? String(parsedProductNo) : rawProductNo;
        return !this.isSameAsInitial(
            title,
            content,
            boardType,
            productNo,
            document.getElementById('status')?.value || '',
            document.getElementById('publicYn')?.value || '',
            document.getElementById('pinnedYn')?.value || ''
        );
    },

    setEditorLoading(loading) {
        document.querySelectorAll('#contentEditForm input, #contentEditForm textarea, #contentEditForm select, #contentEditForm button')
            .forEach((element) => {
                if (element.type !== 'hidden') element.disabled = loading;
            });
        this.setStatus(loading ? '콘텐츠를 불러오는 중...' : '');
    },

    syncVisibilitySummary() {
        const summaryEl = document.getElementById('contentVisibilitySummary');
        if (!summaryEl) return;

        const statusLabel = (document.getElementById('status')?.value || 'DRAFT') === 'PUBLISHED' ? '게시중' : '임시저장';
        const publicLabel = (document.getElementById('publicYn')?.value || 'Y') === 'Y' ? '공개' : '비공개';
        const pinnedLabel = (document.getElementById('pinnedYn')?.value || 'N') === 'Y' ? '고정글' : '일반글';
        summaryEl.textContent = `${statusLabel} · ${publicLabel} · ${pinnedLabel}`;
    },

    syncProductSummary() {
        const productSummaryEl = document.getElementById('contentProductSummary');
        if (!productSummaryEl) return;
        const productNo = document.getElementById('productNo')?.value.trim();
        productSummaryEl.textContent = productNo ? `상품 #${productNo}` : '미연결';
    }
};
