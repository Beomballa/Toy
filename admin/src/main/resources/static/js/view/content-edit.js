const ContentEdit = {
    debounceTimer: null,
    initialData: {
        title: '',
        content: '',
        boardType: ''
    },

    init() {
        this.id = document.getElementById('contentId').value;
        this.bindEvents();
        
        if (this.id) {
            this.getDetail();
        }
    },

    bindEvents() {
        // 수동 저장 버튼
        document.getElementById('btnSave')?.addEventListener('click', () => {
            this.saveContent(false);
        });

        // 삭제 버튼
        document.getElementById('btnDelete')?.addEventListener('click', () => {
            this.deleteContent();
        });

        // 자동 저장 (입력 시)
        const inputs = [document.getElementById('title'), document.getElementById('content'), document.getElementById('boardType')];
        inputs.forEach(el => {
            el?.addEventListener('input', () => {
                clearTimeout(this.debounceTimer);
                this.debounceTimer = setTimeout(() => {
                    this.saveContent(true);
                }, 5000); // 5초간 입력 없으면 자동 저장
            });
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
        } catch (err) {
            console.error('콘텐츠 로드 실패:', err);
            CommonJS.alert('내용을 불러오는 중 오류가 발생했습니다.', '오류', 'error');
        }
    },

    async saveContent(isAutoSave) {
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

        const statusEl = document.getElementById('saveStatus');
        if (statusEl) statusEl.textContent = isAutoSave ? '자동 저장 중...' : '저장 중...';

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

            this.initialData = { title, content, boardType };
            if (statusEl) {
                statusEl.textContent = '모든 변경 사항이 저장되었습니다.';
                setTimeout(() => statusEl.textContent = '', 3000);
            }

            if (!isAutoSave) {
                CommonJS.alert('성공적으로 저장되었습니다.', '성공', 'success', () => {
                    location.href = '/admin/content/list';
                });
            }
        } catch (err) {
            console.error('저장 실패:', err);
            if (!isAutoSave) CommonJS.alert('저장 중 오류가 발생했습니다.', '오류', 'error');
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

            CommonJS.alert('삭제되었습니다.', '성공', 'success', () => {
                location.href = '/admin/content/list';
            });
        } catch (err) {
            console.error('삭제 실패:', err);
            CommonJS.alert('삭제 중 오류가 발생했습니다.', '오류', 'error');
        }
    }
};
