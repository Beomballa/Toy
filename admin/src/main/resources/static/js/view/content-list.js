var ContentListJS = {
    pagerKo : undefined,
    pagerEn : undefined,

    init : function () {
        document.getElementById("newContentBtn").addEventListener("click", function (el){
            ContentListJS.setNewContent();
        });

        document.getElementById("searchForm").addEventListener("submit", function (e){
            e.preventDefault();
            ContentListJS.getListInfo();
        })
    },

    setNewContent : function () {
        axios.post('/api/content/set')
            .then(res => {
                if(res.data.resultCode === "200") {
                    Swal.fire('등록 완료!', '성공적으로 등록되었습니다.', 'success')
                        .then(() => {
                            location.reload();
                        });
                }
            })
            .catch(error => {
                console.error('저장 중 에러 발생:', error);
                Swal.fire('오류 발생', '문서 생성중 문제가 발생했습니다.', 'error');
            });
    },

    getListInfo : function () {
        const reqData = {
            langCode: "KO",
            page: 0,
            pageSize: 10,
            searchKeyword : document.getElementById("searchKeyword").value,
        }

        axios.post('/api/content/list', reqData)
            .then(res => {
                if(res.data.resultCode === "200") {
                    ContentListJS.renderList(res.data.data, res.data.totalCount);
                }
            })
            .catch(error => {
                console.error('리스트 조회 중 에러 발생:', error);
                Swal.fire('오류 발생', '리스트 조회 간 문제가 발생했습니다.', 'error');
            });
    },

    // HTML 템플릿 함수
    renderList : function (posts) {
        const documentList = document.getElementById("document-list");

        if (!posts || posts.length === 0) {
            documentList.innerHTML = `
                <div class="alert alert-info mt-4 text-center" role="alert">
                    등록된 게시물이 없습니다.
                </div>
            `;
            return;
        }

        const html = `
            <div class="row row-cols-1 g-3">
                ${posts.map(post => this.renderPostCard(post)).join('')}
            </div>
        `;

        documentList.innerHTML = html;
    },

    // 개별 카드 렌더링 (재사용 가능)
    renderPostCard : function (post) {
        // XSS 방지를 위한 이스케이프
        const title = this.escapeHtml(post.title || '제목 없음');
        const content = this.escapeHtml(post.content || '');
        const uptDtm = this.escapeHtml(post.uptDtm || '');
        const viewYn = this.escapeHtml(post.viewYn || '');

        return `
            <div class="col">
                <div class="card shadow-sm h-100">
                    <div class="card-body position-relative">
                        <h5 class="card-title mb-2">
                            <a href="/content/edit?docNo=${post.docNo}"
                               class="stretched-link text-decoration-none text-dark fw-semibold">
                                ${title}
                            </a>
                        </h5>
                        <p class="card-text text-truncate text-muted" style="max-width: 90%;">
                            <span>${content}</span>
                        </p>
                    </div>
                    <div class="card-footer d-flex justify-content-between align-items-center small text-muted">
                        <span>${uptDtm}</span>
                        <span class="badge bg-secondary">${viewYn}</span>
                    </div>
                </div>
            </div>
        `;
    },

    // XSS 방지를 위한 HTML 이스케이프
    escapeHtml : function (text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}