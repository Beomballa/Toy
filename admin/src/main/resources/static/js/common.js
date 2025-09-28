// /js/common.js
let CommonJS = {

    init: function () {
        document.getElementById("main-logo").addEventListener("click", function (el){
            window.location.href = "/content/list";
        });
    },

    /**
     * SweetAlert2 확인 창을 띄우는 공통 함수
     * @param {string} title - 확인 창의 제목
     * @param {string} text - 확인 창의 내용
     * @param {function} onConfirm - '예' 버튼을 눌렀을 때 실행될 콜백 함수
     */
    showConfirmAlert: function (title, text, onConfirm) {
        Swal.fire({
            title: title,
            text: text,
            icon: 'question',

            showCancelButton: true,
            confirmButtonColor: '#3085d6',
            cancelButtonColor: '#d33',
            confirmButtonText: '예',
            cancelButtonText: '아니요',

        }).then((result) => {
            // '예' 버튼을 눌렀을 경우, 전달받은 콜백 함수를 실행
            if (result.isConfirmed) {
                onConfirm();
            }
        });
    }
}
document.addEventListener('DOMContentLoaded', function () {
    if (typeof CommonJS !== 'undefined' && CommonJS && typeof CommonJS.init === 'function') {
        CommonJS.init();
    }
});