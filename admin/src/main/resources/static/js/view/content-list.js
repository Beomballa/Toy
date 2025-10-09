var ContentListJS = {
    pagerKo : undefined,
    pagerEn : undefined,

    init : function () {
        document.getElementById("newContentBtn").addEventListener("click", function (el){
            ContentListJS.setNewContent();
        });
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

}