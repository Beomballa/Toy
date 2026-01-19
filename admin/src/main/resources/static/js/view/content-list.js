var ContentListJS = {
    currentPage : 0,
    pageSize : 10,
    pager : undefined,

    init : function () {
        const self = this;

        // 새 글 등록 이벤트
        document.getElementById("newContentBtn").addEventListener("click", function (el){
            ContentListJS.setNewContent();
        });

        // 검색 폼 이벤트
        document.getElementById("searchForm").addEventListener("submit", function (e){
            e.preventDefault();
            ContentListJS.getListInfo(0);
        })

        self.getListInfo(0);
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

    getListInfo : function (pageNum = 0) {
        const self = this;
        self.currentPage = pageNum;

        const keyword = document.getElementById("searchKeyword").value;
        const typeSelect = document.getElementById("searchKeywordType");
        const keywordType = typeSelect.value ? typeSelect.value : "A";

        const reqData = {
            langCode: "KO",
            page: pageNum,
            pageSize: self.pageSize,
            searchKeyword : keyword,
            searchKeywordType : keywordType
        }
        axios.post('/api/content/list', reqData)
            .then(r => {
                const res = r.data

                if(res.resultCode === "200") {
                    self.renderList(res);       // 리스트 그리기 분리
                    self.renderPagination(res.totalCount); // 페이징 그리기
                }

            })
            .catch(error => {
                console.error('리스트 조회 중 에러 발생:', error);
                Swal.fire('오류 발생', '리스트 조회 간 문제가 발생했습니다.', 'error');
            });
    },
    renderList : function (res) {
        const listContainer = document.getElementById("document-list");

        // 1. 기존 내용을 비우고, Bootstrap의 'row' 구조 생성
        listContainer.innerHTML = '<div class="row row-cols-1 g-3" id="document-grid"></div>';
        const grid = document.getElementById("document-grid");

        const data = res.data;

        // 2. 등록된 게시글이 없을 경우
        if(res.resultMsg === 'N') {
            grid.innerHTML = '<div class="col text-center py-5 text-muted">작성한 문서가 없습니다.</div>';
            return;
        }

        // 3. 검색 결과가 없을 경우 처리
        if (!data || data.length === 0) {
            grid.innerHTML = '<div class="col text-center py-5 text-muted">검색 결과가 없습니다.</div>';
            return;
        }

        let htmlContent = '';
        data.forEach(item => {
            htmlContent += `
                        <div class="col">
                            <div class="card shadow-sm h-100">
                                <div class="card-body position-relative"> <!-- position-relative 필수 -->
                                <h5 class="card-title mb-2">
                                    <a href="/content/edit?no=${item.no}" class="stretched-link text-decoration-none text-dark fw-semibold">${item.title}</a>
                                </h5>
                                <p class="card-text text-truncate text-muted" style="max-width: 90%;">
                                    <span>${item.content}</span>
                                </p>
                            </div>
                            <div class="card-footer d-flex justify-content-between align-items-center small text-muted">
                                <span>${item.uptDtm}</span>
                                <span class="badge bg-secondary">${item.viewYn}</span>
                            </div>
                        </div>
                    </div>
                `
        })
        grid.innerHTML = htmlContent;
    },

    renderPagination : function (totalCount) {
        const self = this;
        const pageWrapper = document.getElementById("pagination-wrapper");

        if(!pageWrapper) return;

        const totalPage = Math.ceil(totalCount / self.pageSize);

        // 페이지의 갯수가 1개뿐이면 숨김
        if(totalPage <= 1) {
            pageWrapper.innerHTML = '';
            return;
        }

        let html = `<ul class="pagination pagination-sm">`;

        // 이전
        const prevDisabled = self.currentPage === 0 ? 'disabled' : '';
        html += `<li class="page-item ${prevDisabled}">
                    <a class="page-link" href="javascript:void(0)" onclick="ContentListJS.movePage(${self.currentPage - 1}); return false;">이전</a>
                 </li>`;

        // 페이지 번호(전체 출력)
        for(let i=0; i<totalPage; i++) {
            const activeClass = self.currentPage === i ? 'active' : '';
            html += `<li class="page-item ${activeClass}">
                        <a class="page-link" href="javascript:void(0)" onclick="ContentListJS.movePage(${i}); return false;">${i+1}</a>
                     </li>`;
        }

        const nextDisabled = self.currentPage === totalPage - 1 ? 'disabled' : '';
        html += `<li class="page-item ${nextDisabled}">
                    <a class="page-link" href="javascript:void(0)" onclick="ContentListJS.movePage(${self.currentPage + 1}); return false;">다음</a>
                 </li>`
        html += `</ul>`;
        pageWrapper.innerHTML = html;

    },

    movePage : function (pageNum) {
        if(pageNum < 0) return;

        this.getListInfo(pageNum);
    }
}