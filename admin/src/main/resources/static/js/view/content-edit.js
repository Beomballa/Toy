const ContentEdit = {
    initialized: false,
    debounceTimer: null,
    statusTimer: null,
    isSaving: false,
    isDeleting: false,
    operationPolicy: null,
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
        this.id = document.getElementById('contentId').value;
        this.initialBoardType = ContentBoardConfig.normalizeBoardType(
            document.getElementById('initialBoardType')?.value
        );
        this.returnTo = document.getElementById('contentReturnTo')?.value || '';
        this.source = document.getElementById('contentSource')?.value || '';
        const boardTypeSelect = document.getElementById('boardType');

        if (boardTypeSelect) {
            boardTypeSelect.value = this.initialBoardType;
        }
        document.getElementById('status').value = this.initialData.status;
        document.getElementById('publicYn').value = this.initialData.publicYn;
        document.getElementById('pinnedYn').value = this.initialData.pinnedYn;

        this.bindEvents();
        this.applyBoardMeta(boardTypeSelect?.value || this.initialBoardType);
        CommonJS.renderSourceContextNotice({ noticeId: 'contentEditSourceContextNotice', source: this.source });
        this.syncVisibilitySummary();
        this.syncProductSummary();
        this.applyOperationPolicy();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));
        
        if (this.id) {
            await this.getDetail();
        }
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
        try {
            const res = await fetch(`/api/admin/content/get?id=${this.id}`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            const data = await res.json();
            document.getElementById('title').value = data.title || '';
            document.getElementById('content').value = data.content || '';
            document.getElementById('boardType').value = data.boardType || 'NOTICE';
            document.getElementById('productNo').value = data.productNo || '';
            document.getElementById('status').value = data.status || 'DRAFT';
            document.getElementById('publicYn').value = data.publicYn || 'Y';
            document.getElementById('pinnedYn').value = data.pinnedYn || 'N';
            
            this.initialData = {
                title: data.title,
                content: data.content,
                boardType: data.boardType,
                productNo: data.productNo ? String(data.productNo) : '',
                status: data.status || 'DRAFT',
                publicYn: data.publicYn || 'Y',
                pinnedYn: data.pinnedYn || 'N'
            };
            this.applyBoardMeta(data.boardType || 'NOTICE');
            this.syncVisibilitySummary();
            this.syncProductSummary();
        } catch (err) {
            console.error('콘텐츠 로드 실패:', err);
            await CommonJS.alert('내용을 불러오는 중 오류가 발생했습니다.', '오류', 'error');
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
            listBreadcrumbEl.textContent = meta.edit.boardName;
            listBreadcrumbEl.href = meta.listPath;
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
        if (this.isSaving) return;
        if (this.operationPolicy && CommonJS.isCommunityWriteBlocked(this.operationPolicy)) {
            if (!isAutoSave) {
                const message = CommonJS.getCommunityWriteBlockedReason(this.operationPolicy, '커뮤니티 저장');
                await CommonJS.alert(message, '알림', 'warning');
            }
            return;
        }

        const title = document.getElementById('title').value;
        const content = document.getElementById('content').value;
        const boardType = ContentBoardConfig.normalizeBoardType(document.getElementById('boardType').value);
        const productNo = document.getElementById('productNo').value.trim();
        const status = document.getElementById('status').value || 'DRAFT';
        const publicYn = document.getElementById('publicYn').value || 'Y';
        const pinnedYn = document.getElementById('pinnedYn').value || 'N';

        if (!title.trim()) {
            if (!isAutoSave) await CommonJS.alert('제목을 입력하세요.', '알림', 'warning');
            return;
        }

        // 자동 저장은 실제 변경이 생긴 경우에만 보내서 불필요한 저장 요청을 줄인다.
        if (isAutoSave && 
            title === this.initialData.title && 
            content === this.initialData.content && 
            boardType === this.initialData.boardType &&
            productNo === this.initialData.productNo &&
            status === this.initialData.status &&
            publicYn === this.initialData.publicYn &&
            pinnedYn === this.initialData.pinnedYn) {
            return;
        }

        this.isSaving = true;
        this.setStatus(isAutoSave ? '자동 저장 중...' : '저장 중...');

        try {
            const res = await fetch('/api/admin/content/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    id: this.id || null,
                    title: title,
                    content: content,
                    boardType: boardType,
                    productNo: productNo ? Number(productNo) : null,
                    status: status,
                    publicYn: publicYn,
                    pinnedYn: pinnedYn
                })
            });

            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            const saved = await res.json();
            if (saved?.id) {
                this.id = saved.id;
                document.getElementById('contentId').value = saved.id;
                const url = new URL(window.location.href);
                url.searchParams.set('id', saved.id);
                url.searchParams.set('boardType', boardType);
                window.history.replaceState({}, '', url);
            }

            this.initialData = { title, content, boardType, productNo, status, publicYn, pinnedYn };
            this.setStatus(isAutoSave ? '임시 저장되었습니다.' : '저장되었습니다.', 3000);

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
        }
    },

    async deleteContent() {
        if (this.isDeleting) {
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
