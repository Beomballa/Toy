(() => {
    "use strict";

    const page = document.body.dataset.commercePage;
    const elements = {
        list: document.getElementById("commerceCartList"),
        count: document.getElementById("commerceItemCount"),
        subtotal: document.getElementById("commerceSubtotal"),
        total: document.getElementById("commerceTotal"),
        totalQuantity: document.getElementById("commerceTotalQuantity"),
        stockSummary: document.getElementById("commerceStockSummary"),
        clearCart: document.getElementById("clearCartButton"),
        checkoutLink: document.getElementById("commerceCheckoutLink"),
        form: document.getElementById("checkoutForm"),
        submit: document.getElementById("submitOrderButton"),
        sameBuyer: document.getElementById("sameBuyerCheck"),
        complete: document.getElementById("orderComplete"),
        orderNumber: document.getElementById("completedOrderNumber"),
        orderAmount: document.getElementById("completedOrderAmount"),
        orderLink: document.getElementById("completedOrderLink"),
        deliveryPreset: document.getElementById("deliveryRequestPreset"),
        toast: document.getElementById("commerceToast")
    };
    let cart = { items: [], itemCount: 0, totalQuantity: 0, totalAmount: 0 };
    let toastTimer = null;
    let memoryCartToken = null;
    let submitting = false;
    let cartMutating = false;

    function cartToken() {
        const key = "grade-stock-cart-token";
        try {
            let token = window.localStorage.getItem(key);
            if (!token) {
                token = createCartToken();
                window.localStorage.setItem(key, token);
            }
            return token;
        } catch (ignored) {
            memoryCartToken ||= createCartToken();
            return memoryCartToken;
        }
    }

    function createCartToken() {
        return window.crypto?.randomUUID?.() || `cart-${Date.now()}-${Math.random().toString(16).slice(2)}`;
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

    function fallbackImage() {
        return "/images/product-placeholder.svg";
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
        setListBusy(true);
        syncCheckoutAvailability(false);
        try {
            cart = await request("/api/front/cart");
            renderCart();
        } catch (error) {
            showToast(error.message);
            renderUnavailable();
        } finally {
            setListBusy(false);
        }
    }

    function renderCart() {
        elements.count.textContent = `${cart.itemCount}개 상품 · 총 ${cart.totalQuantity}개`;
        elements.subtotal.textContent = formatPrice(cart.totalAmount);
        elements.total.textContent = formatPrice(cart.totalAmount);
        if (elements.totalQuantity) elements.totalQuantity.textContent = `${cart.totalQuantity}개`;
        const lowStockCount = cart.items.filter((item) => item.stock - item.quantity <= 3).length;
        if (elements.stockSummary) {
            elements.stockSummary.textContent = lowStockCount
                ? `${lowStockCount}개 옵션의 남은 재고가 3개 이하입니다.`
                : "현재 담긴 옵션은 재고가 안정적입니다.";
            elements.stockSummary.classList.toggle("is-alert", lowStockCount > 0);
        }
        syncCheckoutAvailability(cart.items.length > 0);
        if (!cart.items.length) {
            renderEmpty("장바구니가 비어 있습니다.");
            return;
        }
        if (page === "checkout" && elements.form) {
            elements.form.hidden = false;
        }
        elements.list.innerHTML = cart.items.map((item) => `
            <article class="commerce-item" data-cart-item="${item.itemId}">
                <a href="/front/products/${item.productId}"><img src="${escapeMarkup(item.thumbnailUrl || fallbackImage(item.productName))}" alt="${escapeMarkup(item.productName)}"></a>
                <div class="commerce-item__copy">
                    <h2><a href="/front/products/${item.productId}">${escapeMarkup(item.productName)}</a></h2>
                    <p>옵션 ${escapeMarkup(item.optionName)} · 재고 ${item.stock}개 <span class="commerce-stock-badge ${item.stock - item.quantity <= 3 ? "is-low" : ""}">${item.stock - item.quantity <= 3 ? "품절 임박" : "재고 안정"}</span></p>
                    <strong>${formatPrice(item.unitPrice)}</strong>
                    ${page === "cart" ? `<div class="commerce-item__control"><button type="button" data-quantity="-1" aria-label="수량 줄이기" ${item.quantity <= 1 ? "disabled" : ""}>−</button><span aria-label="현재 수량 ${item.quantity}개">${item.quantity}</span><button type="button" data-quantity="1" aria-label="수량 늘리기" ${item.quantity >= item.stock || item.quantity >= 20 ? "disabled" : ""}>＋</button><button class="commerce-item__remove" type="button" data-remove>삭제</button></div>` : ""}
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

    function renderUnavailable() {
        elements.list.innerHTML = `
            <div class="commerce-empty commerce-empty--error">
                <strong>장바구니를 불러오지 못했습니다.</strong>
                <p>네트워크 상태를 확인한 후 다시 시도해주세요.</p>
                <button type="button" data-cart-retry>다시 시도</button>
            </div>`;
        if (page === "checkout" && elements.form) {
            elements.form.hidden = true;
        }
    }

    function syncCheckoutAvailability(available) {
        if (elements.clearCart) {
            elements.clearCart.disabled = !available || cartMutating;
        }
        elements.checkoutLink?.classList.toggle("is-disabled", !available);
        elements.checkoutLink?.setAttribute("aria-disabled", String(!available));
        if (elements.submit) {
            elements.submit.disabled = !available || submitting;
        }
    }

    function setListBusy(busy) {
        elements.list?.setAttribute("aria-busy", String(busy));
    }

    async function changeItem(itemId, nextQuantity) {
        if (cartMutating) return;
        cartMutating = true;
        syncCheckoutAvailability(false);
        setListBusy(true);
        try {
            cart = await request(`/api/front/cart/items/${itemId}`, {
                method: "PATCH",
                body: JSON.stringify({ quantity: nextQuantity })
            });
            renderCart();
        } catch (error) {
            showToast(error.message);
        } finally {
            cartMutating = false;
            setListBusy(false);
            syncCheckoutAvailability(cart.items.length > 0);
        }
    }

    async function removeItem(itemId) {
        if (cartMutating) return;
        cartMutating = true;
        syncCheckoutAvailability(false);
        setListBusy(true);
        try {
            cart = await request(`/api/front/cart/items/${itemId}`, { method: "DELETE" });
            renderCart();
            showToast("상품을 장바구니에서 삭제했습니다.");
        } catch (error) {
            showToast(error.message);
        } finally {
            cartMutating = false;
            setListBusy(false);
            syncCheckoutAvailability(cart.items.length > 0);
        }
    }

    async function clearCart() {
        if (cartMutating || !cart.items.length || !window.confirm("장바구니의 모든 상품을 삭제할까요?")) return;
        cartMutating = true;
        syncCheckoutAvailability(false);
        setListBusy(true);
        try {
            cart = await request("/api/front/cart/items", { method: "DELETE" });
            renderCart();
            showToast("장바구니를 비웠습니다.");
        } catch (error) {
            showToast(error.message);
        } finally {
            cartMutating = false;
            setListBusy(false);
            syncCheckoutAvailability(cart.items.length > 0);
        }
    }

    elements.list?.addEventListener("click", (event) => {
        if (event.target.closest("[data-cart-retry]")) {
            loadCart();
            return;
        }
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
        syncBuyerToRecipient();
    });

    elements.clearCart?.addEventListener("click", clearCart);

    function syncBuyerToRecipient() {
        if (!elements.sameBuyer?.checked || !elements.form) return;
        elements.form.elements.recipientName.value = elements.form.elements.buyerName.value;
        elements.form.elements.recipientPhone.value = elements.form.elements.buyerPhone.value;
    }

    function formatPhoneInput(input) {
        const digits = input.value.replace(/\D/g, "").slice(0, 11);
        input.value = digits.length <= 3
            ? digits
            : digits.length <= 7
                ? `${digits.slice(0, 3)}-${digits.slice(3)}`
                : `${digits.slice(0, 3)}-${digits.slice(3, digits.length - 4)}-${digits.slice(-4)}`;
    }

    elements.form?.querySelectorAll('input[inputmode="tel"]').forEach((input) => {
        input.addEventListener("input", () => {
            formatPhoneInput(input);
            if (input.name === "buyerPhone") syncBuyerToRecipient();
        });
    });
    elements.form?.elements.buyerName?.addEventListener("input", syncBuyerToRecipient);
    elements.form?.elements.postalCode?.addEventListener("input", (event) => {
        event.target.value = event.target.value.replace(/\D/g, "").slice(0, 10);
    });
    elements.form?.querySelectorAll("[data-field-counter]").forEach((counter) => {
        const input = elements.form.elements[counter.dataset.fieldCounter];
        const update = () => {
            counter.textContent = `${input.value.length} / ${input.maxLength}`;
        };
        input.addEventListener("input", update);
        update();
    });
    elements.deliveryPreset?.addEventListener("change", () => {
        if (!elements.deliveryPreset.value) return;
        const input = elements.form.elements.deliveryRequest;
        input.value = elements.deliveryPreset.value;
        input.dispatchEvent(new Event("input"));
        input.focus();
    });

    elements.form?.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (submitting || !cart.items.length || !elements.form.reportValidity()) return;
        submitting = true;
        elements.submit.disabled = true;
        elements.submit.setAttribute("aria-busy", "true");
        elements.submit.querySelector("span").textContent = "주문 접수 중";
        const body = Object.fromEntries(new FormData(elements.form).entries());
        try {
            const order = await request("/api/front/orders", {
                method: "POST",
                body: JSON.stringify(body)
            });
            elements.orderNumber.textContent = order.orderNumber;
            elements.orderAmount.textContent = `${formatPrice(order.totalAmount)} · 주문 접수`;
            const buyerPhone = String(body.buyerPhone || "");
            try {
                window.sessionStorage.setItem("grade-stock-last-order", JSON.stringify({
                    orderNumber: order.orderNumber,
                    phone: buyerPhone
                }));
            } catch (ignored) {
                // 저장소 접근이 제한돼도 서버에서 완료된 주문 결과는 그대로 표시한다.
            }
            if (elements.orderLink) {
                elements.orderLink.href = `/front/orders/${encodeURIComponent(order.orderNumber)}`;
            }
            elements.complete.hidden = false;
        } catch (error) {
            showToast(error.message);
            elements.submit.disabled = false;
            await loadCart();
        } finally {
            submitting = false;
            elements.submit.removeAttribute("aria-busy");
            elements.submit.querySelector("span").textContent = "주문 접수하기";
            syncCheckoutAvailability(cart.items.length > 0);
        }
    });

    loadCart();
})();
