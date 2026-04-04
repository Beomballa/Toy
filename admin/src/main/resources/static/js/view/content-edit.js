var ContentEditJS = {
    
    debounceTimer : null,
    
    initialTitle : "",
    initialContent : "",

    titleEl : null,
    contentEl : null,
    
    init : function() {

        this.titleEl = document.getElementById("title");
        this.contentEl = document.getElementById("content");

        this.initialTitle = this.titleEl.value;
        this.initialContent = this.contentEl.value;
        
        [this.titleEl, this.contentEl].forEach(input => {
            input.addEventListener("keyup", () => {
                clearTimeout(this.debounceTimer);
                
                this.debounceTimer = setTimeout(() => {
                    this.saveContent(true); // 자동 저장
                }, 5000)
            })
        });

        // Title에서 엔터 키 방지 및 저장 실행
        this.titleEl.addEventListener("keydown", (e) => {
            if (e.key === "Enter") {
                e.preventDefault(); // 1. 폼 제출(새로고침)을 방지
                // showConfirmAlert(
                //         '등록하시겠습니까?',
                //         '작성한 내용이 서버에 저장됩니다.',
                //         () => {
                //             ContentEditJS.saveContent(false); // 수동 저장
                //         }
                //     );
            }
        });

        // 폼 자체의 submit 이벤트 방지
        document.getElementById("documentEditForm").addEventListener("submit", function(e) {
            e.preventDefault();
        });

        ContentEditJS.updateViewCnt();
        
        // document.getElementById("submitBtn").addEventListener("click", function (el) {
        //     el.preventDefault();
        //     // 바로 saveContent를 호출하는 대신, 공통 확인 창 함수를 먼저 호출
        //
        // })
    },

    saveContent : function (isAutoSave) {

        const currentTitle = this.titleEl.value;
        const currentContent = this.contentEl.value;
        const docNo = document.getElementById("docNo").value;

        if (isAutoSave && currentTitle === this.initialTitle && currentContent === this.initialContent) {
            console.log("변경된 내용이 없어 자동저장을 하지 않습니다.");
            return;
        }

        const reqData = {
            docNo : docNo,
            title : currentTitle,
            content : currentContent
        };

        axios.post('/api/content/save', reqData)
            .then(response => {
                this.initialTitle = currentTitle;
                this.initialContent = currentContent;

                const saveStatus = document.getElementById("saveStatus");
                setTimeout(() => { saveStatus.textContent = '';}, 2000);

                if(!isAutoSave) {
                    Swal.fire('등록 완료!', '성공적으로 등록되었습니다.', 'success');
                }
            })
            .catch(error => {
                console.error('저장 중 에러 발생:', error);
                Swal.fire('오류 발생', '등록 중 문제가 발생했습니다.', 'error');
            });
    },

    updateViewCnt : function () {
        const docNo = document.getElementById("docNo").value;

        const reqData = {
            docNo : docNo,
        };

        axios.post('/api/content/update/cnt', reqData)
            .then(response => {
                // this.initialTitle = currentTitle;
                // this.initialContent = currentContent;
                //
                // const saveStatus = document.getElementById("saveStatus");
                // setTimeout(() => { saveStatus.textContent = '';}, 2000);
                //
                // if(!isAutoSave) {
                //     Swal.fire('등록 완료!', '성공적으로 등록되었습니다.', 'success');
                // }
            })
            .catch(error => {
                Swal.fire('오류 발생', '조회수 증가중 문제가 발생했습니다.', 'error');
            });
    }
}