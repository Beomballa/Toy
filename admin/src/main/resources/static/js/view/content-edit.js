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
        })
        
        document.getElementById("submitBtn").addEventListener("click", function (el) {
            el.preventDefault();
            // 바로 saveContent를 호출하는 대신, 공통 확인 창 함수를 먼저 호출합니다.
            showConfirmAlert(
                '등록하시겠습니까?',
                '작성한 내용이 서버에 저장됩니다.',
                () => {
                    ContentEditJS.saveContent(false); // 수동 저장
                }
            );
        })
    },

    saveContent : function (isAutoSave) {

        const currentTitle = this.titleEl.value;
        const currentContent = this.contentEl.value;
        const docNo = document.getElementById("docNo").value;

        if(isAutoSave && currentTitle === this.initialTitle || currentContent === this.initialContent) {
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
                console.log('저장 성공!', response.data);
                this.initialTitle = currentTitle;
                this.initialContent = currentContent;
                //
                // this.titleEl.value = this.initialTitle;
                // this.contentEl.value = this.initialContent;

                const saveStatus = document.getElementById("saveStatus");
                saveStatus.textContent = isAutoSave ? "자동저장" : "수동저장";
                setTimeout(() => { saveStatus.textContent = '';}, 2000);
                // window.location.href = "/content/list"

                if(!isAutoSave) {
                    Swal.fire('등록 완료!', '성공적으로 등록되었습니다.', 'success');
                }
            })
            .catch(error => {
                console.error('저장 중 에러 발생:', error);
                Swal.fire('오류 발생', '등록 중 문제가 발생했습니다.', 'error');
            });
    }
}