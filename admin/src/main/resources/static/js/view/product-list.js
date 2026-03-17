const ProductList = {

    init() {
        ProductList.bindEvents();

        document.getElementById('new-product').addEventListener('click', function () {
            window.location.href = '/product/set';
        })
    },

    bindEvents() {
        const animateElements = document.querySelectorAll('.animate-in');
        animateElements.forEach((el, index) => {
            setTimeout(() => {
                el.style.opacity = '0';
                el.style.animation = `fadeInUp 0.6s ease forwards ${index * 0.1}s`;
            }, 100);
        });
    },
}