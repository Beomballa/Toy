const ContentEdit = {
    boardMeta: {
        NOTICE: {
            title: '공지 작성',
            saveLabel: '공지 저장',
            boardName: '공지',
            badge: 'NOTICE',
            description: '운영 공지와 서비스 안내를 명확하게 전달하는 문서를 작성합니다.',
            sideNote: '공지 게시판은 모든 운영자와 사용자가 가장 먼저 확인하는 정보성 영역입니다.',
            listPath: '/admin/content/list?boardType=NOTICE'
        },
        STYLE: {
            title: '스타일 피드 작성',
            saveLabel: '피드 저장',
            boardName: '스타일 피드',
            badge: 'STYLE',
            description: '룩북, 착용 이미지, 큐레이션 성격의 콘텐츠를 피드 형식으로 정리합니다.',
            sideNote: '스타일 피드는 시각적인 흐름이 중요하므로 제목과 첫 문장의 완성도가 특히 중요합니다.',
            listPath: '/admin/content/list?boardType=STYLE'
        },
        DISCUSS: {
            title: '종목 토론 작성',
            saveLabel: '토론 저장',
            boardName: '종목 토론방',
            badge: 'DISCUSS',
            description: '상품 이슈, 시세 흐름, 관심 포인트를 토론형 문맥에 맞춰 작성합니다.',
            sideNote: '토론형 게시판은 질문형 제목이나 핵심 이슈가 먼저 드러나는 문장이 더 잘 읽힙니다.',
            listPath: '/admin/content/list?boardType=DISCUSS'
        },
        QNA: {
            title: '문의 작성',
            saveLabel: '문의 저장',
            boardName: '문의사항',
            badge: 'QNA',
            description: '사용자 문의와 답변 관리에 적합한 형태로 내용을 정리합니다.',
            sideNote: '문의 게시판은 요약 제목과 본문 내 맥락 분리가 잘 되어야 후속 대응이 쉽습니다.',
            listPath: '/admin/content/list?boardType=QNA'
        }
    },
    debounceTimer: null,
    statusTimer: null,
    isSaving: false,
    initialData: {
        title: '',
        content: '',
        boardType: ''
    },

    init() {
        this.id = document.getElementById('contentId').value;
        this.initialBoardType = document.getElementById('initialBoardType')?.value || 'NOTICE';
        const boardTypeSelect = document.getElementById('boardType');

        if (boardTypeSelect) {
            boardTypeSelect.value = this.initialBoardType;
        }

        this.bindEvents();
        this.applyBoardMeta(boardTypeSelect?.value || this.initialBoardType);
        
        if (this.id) {
            this.getDetail();
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
        const inputs = [document.getElementById('title'), document.getElementById('content'), document.getElementById('boardType')];
        inputs.slice(0, 2).forEach(el => {
            el?.addEventListener('input', () => {
                this.scheduleAutoSave();
            });
        });

        document.getElementById('boardType')?.addEventListener('change', (e) => {
            this.applyBoardMeta(e.target.value);
            this.scheduleAutoSave();
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
            
            this.initialData = {
                title: data.title,
                content: data.content,
                boardType: data.boardType
            };
            this.applyBoardMeta(data.boardType || 'NOTICE');
        } catch (err) {
            console.error('콘텐츠 로드 실패:', err);
            CommonJS.alert('내용을 불러오는 중 오류가 발생했습니다.', '오류', 'error');
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
        const meta = this.boardMeta[boardType] || this.boardMeta.NOTICE;
        const titleEl = document.getElementById('contentEditTitle');
        const saveBtn = document.getElementById('btnSave');
        const saveBtnLabel = document.getElementById('btnSaveLabel');
        const breadcrumbEl = document.getElementById('contentEditBreadcrumb');
        const badgeEl = document.getElementById('contentBoardBadge');
        const descEl = document.getElementById('contentEditDescription');
        const boardNameEl = document.getElementById('contentBoardName');
        const sideNoteEl = document.getElementById('contentSideNote');
        const listBreadcrumbEl = document.getElementById('contentListBreadcrumb');

        if (titleEl) titleEl.textContent = this.id ? meta.title.replace('작성', '수정') : meta.title;
        if (breadcrumbEl) breadcrumbEl.textContent = this.id ? meta.title.replace('작성', '수정') : meta.title;
        if (saveBtn) saveBtn.setAttribute('aria-label', meta.saveLabel);
        if (saveBtnLabel) saveBtnLabel.textContent = meta.saveLabel;
        if (badgeEl) badgeEl.textContent = meta.badge;
        if (descEl) descEl.textContent = meta.description;
        if (boardNameEl) boardNameEl.textContent = meta.boardName;
        if (sideNoteEl) sideNoteEl.textContent = meta.sideNote;
        if (listBreadcrumbEl) {
            listBreadcrumbEl.textContent = meta.boardName;
            listBreadcrumbEl.href = meta.listPath;
        }
    },

    getListPath() {
        const boardType = document.getElementById('boardType')?.value || this.initialBoardType || 'NOTICE';
        return (this.boardMeta[boardType] || this.boardMeta.NOTICE).listPath;
    },

    async saveContent(isAutoSave) {
        if (this.isSaving) return;

        const title = document.getElementById('title').value;
        const content = document.getElementById('content').value;
        const boardType = document.getElementById('boardType').value;

        if (!title.trim()) {
            if (!isAutoSave) CommonJS.alert('제목을 입력하세요.', '알림', 'warning');
            return;
        }

        // 변경 사항 확인
        if (isAutoSave && 
            title === this.initialData.title && 
            content === this.initialData.content && 
            boardType === this.initialData.boardType) {
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
                    boardType: boardType
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

            this.initialData = { title, content, boardType };
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
        const confirm = await CommonJS.confirm('정말 삭제하시겠습니까?');
        if (!confirm) return;

        try {
            const res = await fetch(`/api/admin/content/delete?id=${this.id}`, {
                method: 'DELETE'
            });

            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            await CommonJS.alert('삭제되었습니다.', '성공', 'success');
            location.href = this.getListPath();
        } catch (err) {
            console.error('삭제 실패:', err);
            CommonJS.alert('삭제 중 오류가 발생했습니다.', '오류', 'error');
        }
    }
};
