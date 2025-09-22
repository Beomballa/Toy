var ContentSetJS = {
    init : function() {
        document.getElementById("submitBtn").addEventListener("click", function (el) {
            el.preventDefault();
            // 바로 setContent를 호출하는 대신, 공통 확인 창 함수를 먼저 호출합니다.
            showConfirmAlert(
                '등록하시겠습니까?',
                '작성한 내용이 서버에 저장됩니다.',
                function() {
                    // '예' 버튼을 눌렀을 때 실행될 함수로 setContent를 지정합니다.
                    ContentSetJS.setContent();
                }
            );
        })

    },

    setContent : function () {
        const title = document.getElementById("title").value;
        const content = document.getElementById("content").value;

        const reqData = {
            title : title,
            content : content
        };

        axios.post('/api/content/save', reqData)
            .then(response => {
                console.log('저장 성공!', response.data);
                Swal.fire('등록 완료!', '성공적으로 등록되었습니다.', 'success');
                window.location.href = "/content/list"
            })
            .catch(error => {
                console.error('저장 중 에러 발생:', error);
                Swal.fire('오류 발생', '등록 중 문제가 발생했습니다.', 'error');
            });
    }
}