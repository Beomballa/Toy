(() => {
    "use strict";

    const page = document.body.dataset.commercePage;
    const elements = {
        list: document.getElementById("commerceCartList"),
        count: document.getElementById("commerceItemCount"),
        subtotal: document.getElementById("commerceSubtotal"),
        total: document.getElementById("commerceTotal"),
        checkoutLink: document.getElementById("commerceCheckoutLink"),
        form: document.getElementById("checkoutForm"),
        submit: document.getElementById("submitOrderButton"),
        sameBuyer: document.getElementById("sameBuyerCheck"),
        complete: document.getElementById("orderComplete"),
        orderNumber: document.getElementById("completedOrderNumber"),
        orderAmount: document.getElementById("completedOrderAmount"),
        orderLink: document.getElementById("completedOrderLink"),
        toast: document.getElementById("commerceToast")
    };
    let cart = { items: [], itemCount: 0, totalQuantity: 0, totalAmount: 0 };
    let toastTimer = null;

    function cartToken() {
        const key = "grade-stock-cart-token";
        let token = window.localStorage.getItem(key);
        if (!token) {
            token = window.crypto?.randomUUID?.() || `cart-${Date.now()}-${Math.random().toString(16).slice(2)}`;
            window.localStorage.setItem(key, token);
        }
        return token;
    }

    function formatPrice(value) {
        return `${Number(value || 0).toLocaleString("ko-KR")}원`;
    }

    function escapeMarkup(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#39;");
    }

    function fallbackImage(name) {
        return `https://placehold.co/240x240/f4f4f4/999?text=${encodeURIComponent(String(name || "GS").slice(0, 12))}`;
    }

    function showToast(message) {
        if (!elements.toast) return;
        elements.toast.textContent = message;
        elements.toast.hidden = false;
        window.clearTimeout(toastTimer);
        toastTimer = window.setTimeout(() => {
            elements.toast.hidden = true;
        }, 2800);
    }

    async function request(path, options = {}) {
        const response = await fetch(path, {
            ...options,
            headers: {
                "Content-Type": "application/json",
                "X-Cart-Token": cartToken(),
                ...(options.headers || {})
            }
        });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(payload.message || "요청을 처리하지 못했습니다.");
        }
        return payload;
    }

    async function loadCart() {
        try {
            cart = await request("/api/front/cart");
            renderCart();
        } catch (error) {
            showToast(error.message);
            renderEmpty("장바구니를 불러오지 못했습니다.");
        }
    }

    function renderCart() {
        elements.count.textContent = `${cart.itemCount}개 상품 · 총 ${cart.totalQuantity}개`;
        elements.subtotal.textContent = formatPrice(cart.totalAmount);
        elements.total.textContent = formatPrice(cart.totalAmount);
        elements.checkoutLink?.classList.toggle("is-disabled", !cart.items.length);
        if (elements.submit) elements.submit.disabled = !cart.items.length;
        if (!cart.items.length) {
            renderEmpty("장바구니가 비어 있습니다.");
            return;
        }
        elements.list.innerHTML = cart.items.map((item) => `
            <article class="commerce-item" data-cart-item="${item.itemId}">
                <a href="/front/products/${item.productId}"><img src="${escapeMarkup(item.thumbnailUrl || fallbackImage(item.productName))}" alt="${escapeMarkup(item.productName)}"></a>
                <div class="commerce-item__copy">
                    <h2><a href="/front/products/${item.productId}">${escapeMarkup(item.productName)}</a></h2>
                    <p>옵션 ${escapeMarkup(item.optionName)} · 재고 ${item.stock}개</p>
                    <strong>${formatPrice(item.unitPrice)}</strong>
                    ${page === "cart" ? `<div class="commerce-item__control"><button type="button" data-quantity="-1" aria-label="수량 줄이기">−</button><span>${item.quantity}</span><button type="button" data-quantity="1" aria-label="수량 늘리기" ${item.quantity >= item.stock || item.quantity >= 20 ? "disabled" : ""}>＋</button><button class="commerce-item__remove" type="button" data-remove>삭제</button></div>` : ""}
                </div>
                <div class="commerce-item__amount"><strong>${formatPrice(item.lineAmount)}</strong><span>${item.quantity}개</span></div>
            </article>
        `).join("");
    }

    function renderEmpty(message) {
        elements.list.innerHTML = `<div class="commerce-empty"><strong>${escapeMarkup(message)}</strong><p>상품 상세에서 옵션과 수량을 선택해 담아주세요.</p><a href="/front">상품 둘러보기</a></div>`;
        if (page === "checkout" && elements.form) {
            elements.form.hidden = true;
        }
    }

    async function changeItem(itemId, nextQuantity) {
        try {
            cart = await request(`/api/front/cart/items/${itemId}`, {
                method: "PATCH",
                body: JSON.stringify({ quantity: nextQuantity })
            });
            renderCart();
        } catch (error) {
            showToast(error.message);
        }
    }

    async function removeItem(itemId) {
        try {
            cart = await request(`/api/front/cart/items/${itemId}`, { method: "DELETE" });
            renderCart();
            showToast("상품을 장바구니에서 삭제했습니다.");
        } catch (error) {
            showToast(error.message);
        }
    }

    elements.list?.addEventListener("click", (event) => {
        const article = event.target.closest("[data-cart-item]");
        if (!article) return;
        const item = cart.items.find((candidate) => Number(candidate.itemId) === Number(article.dataset.cartItem));
        if (!item) return;
        const quantityButton = event.target.closest("[data-quantity]");
        if (quantityButton) {
            changeItem(item.itemId, item.quantity + Number(quantityButton.dataset.quantity));
            return;
        }
        if (event.target.closest("[data-remove]")) {
            removeItem(item.itemId);
        }
    });

    elements.sameBuyer?.addEventListener("change", () => {
        if (!elements.sameBuyer.checked || !elements.form) return;
        elements.form.elements.recipientName.value = elements.form.elements.buyerName.value;
        elements.form.elements.recipientPhone.value = elements.form.elements.buyerPhone.value;
    });

    elements.form?.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!cart.items.length || !elements.form.reportValidity()) return;
        elements.submit.disabled = true;
        const body = Object.fromEntries(new FormData(elements.form).entries());
        try {
            const order = await request("/api/front/orders", {
                method: "POST",
                body: JSON.stringify(body)
            });
            elements.orderNumber.textContent = order.orderNumber;
            elements.orderAmount.textContent = `${formatPrice(order.totalAmount)} · 주문 접수`;
            const buyerPhone = String(body.buyerPhone || "");
            window.sessionStorage.setItem("grade-stock-last-order", JSON.stringify({
                orderNumber: order.orderNumber,
                phone: buyerPhone
            }));
            if (elements.orderLink) {
                elements.orderLink.href = `/front/orders/${encodeURIComponent(order.orderNumber)}`;
            }
            elements.complete.hidden = false;
        } catch (error) {
            showToast(error.message);
            elements.submit.disabled = false;
            await loadCart();
        }
    });

    loadCart();
})();
