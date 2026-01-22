var DashBoardListJS = {
    init: function () {
        this.get7DaysPopularContent();
    },

    get7DaysPopularContent: function () {
        const self = this;

        axios.post('/api/content/recent/list')
            .then(res => {
                const response = res.data; // 전체 JSON 응답

                // resultCode 확인 및 데이터 존재 여부 체크
                // JSON 구조상 실제 리스트는 response.data 에 들어있음
                if (response.resultCode === "200" && response.data && response.data.length > 0) {
                    self.renderPopularList(response.data);
                } else {
                    self.renderEmptyState();
                }
            })
            .catch(error => {
                console.error('인기 게시물 조회 실패:', error);
                self.renderEmptyState();
            });
    },

    renderPopularList: function (dataList) {
        const listContainer = document.getElementById("popular-post-list");
        const noDataMsg = document.getElementById("no-data-message");

        // 화면 전환: 리스트 보이기, 빈 화면 메시지 숨기기
        if (noDataMsg) noDataMsg.classList.add("d-none");
        if (listContainer) {
            listContainer.classList.remove("d-none");
            listContainer.innerHTML = ''; // 초기화
        }

        let htmlContent = '';

        dataList.forEach(item => {
            // 1. 내용 미리보기 처리 (HTML 태그 제거 및 줄바꿈을 공백으로)
            // item.content가 null이면 빈 문자열 처리
            const rawContent = item.content || '';
            const contentPreview = rawContent.replace(/<[^>]*>?/g, '').replace(/\n/g, ' ');

            // 2. 공개/비공개 배지 스타일 결정
            // 데이터가 '공개' 또는 '비공개' 텍스트로 옴
            const isPublic = item.viewYn === '공개';
            const badgeClass = isPublic ? 'bg-success-subtle text-success' : 'bg-secondary-subtle text-secondary';

            // 3. 카드 HTML 생성
            htmlContent += `
                <div class="col">
                    <div class="card h-100 border-0 shadow-sm hover-card" onclick="location.href='/content/edit?no=${item.no}'" style="cursor: pointer; transition: transform 0.2s;">
                        <div class="card-body">
                            <div class="d-flex justify-content-between align-items-start mb-2">
                                <h6 class="card-title fw-bold text-truncate mb-0" style="max-width: 75%;" title="${item.title}">
                                    ${item.title || '제목 없음'}
                                </h6>
                                <span class="badge ${badgeClass} rounded-pill" style="font-size: 0.75rem;">
                                    ${item.viewYn}
                                </span>
                            </div>
                            
                            <p class="card-text text-muted small mb-3" style="
                                display: -webkit-box;
                                -webkit-line-clamp: 2;
                                -webkit-box-orient: vertical;
                                overflow: hidden;
                                height: 2.8em;
                                line-height: 1.4em;
                            ">
                                ${contentPreview || '작성된 내용이 없습니다.'}
                            </p>
                            
                            <div class="d-flex justify-content-between align-items-center pt-2 border-top mt-auto">
                                <small class="text-muted">
                                    <i class="bi bi-calendar-check me-1"></i> ${item.uptDtm ? item.uptDtm.split(' ')[0] : '-'}
                                </small>
                                <small class="text-primary fw-semibold" style="font-size: 0.8rem;">
                                    자세히 보기 <i class="bi bi-chevron-right"></i>
                                </small>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        });

        listContainer.innerHTML = htmlContent;
    },

    renderEmptyState: function () {
        const listContainer = document.getElementById("popular-post-list");
        const noDataMsg = document.getElementById("no-data-message");

        if (listContainer) listContainer.classList.add("d-none");
        if (noDataMsg) noDataMsg.classList.remove("d-none");
    }
};