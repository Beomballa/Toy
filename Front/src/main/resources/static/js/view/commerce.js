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
        savedAddress: document.getElementById("savedDeliveryAddress"),
        toast: document.getElementById("commerceToast")
    };
    let cart = { items: [], itemCount: 0, totalQuantity: 0, totalAmount: 0 };
    let toastTimer = null;
    let memoryCartToken = null;
    let submitting = false;
    let cartMutating = false;
    let cartRequestSequence = 0;
    let savedAddresses = [];

    function cartToken() {
        const key = "grade-stock-cart-token";
        try {
            let token = window.localStorage.getItem(key);
            if (!isValidCartToken(token)) {
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

    function isValidCartToken(value) {
        return /^[A-Za-z0-9-]{16,80}$/.test(String(value || ""));
    }

    function formatPrice(value) {
        const normalized = normalizeNonNegativeInteger(value);
        return `${normalized == null ? 0 : normalized.toLocaleString("ko-KR")}원`;
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
        const activeRequest = ++cartRequestSequence;
        setListBusy(true);
        syncCheckoutAvailability(false);
        try {
            const nextCart = normalizeCart(await request("/api/front/cart"));
            if (activeRequest !== cartRequestSequence) return;
            if (!nextCart) throw new Error("장바구니 응답이 올바르지 않습니다.");
            cart = nextCart;
            renderCart();
        } catch (error) {
            if (activeRequest !== cartRequestSequence) return;
            cart = emptyCart();
            showToast(error.message);
            renderUnavailable();
        } finally {
            if (activeRequest === cartRequestSequence) setListBusy(false);
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
        const normalizedItemId = normalizePositiveInteger(itemId);
        const normalizedQuantity = normalizePositiveInteger(nextQuantity);
        if (!normalizedItemId || !normalizedQuantity || normalizedQuantity > 20) return;
        cartMutating = true;
        const activeRequest = ++cartRequestSequence;
        syncCheckoutAvailability(false);
        setListBusy(true);
        try {
            const nextCart = normalizeCart(await request(`/api/front/cart/items/${normalizedItemId}`, {
                method: "PATCH",
                body: JSON.stringify({ quantity: normalizedQuantity })
            }));
            if (activeRequest !== cartRequestSequence) return;
            if (!nextCart) throw new Error("장바구니 변경 응답이 올바르지 않습니다.");
            cart = nextCart;
            renderCart();
        } catch (error) {
            showToast(error.message);
            await loadCart();
        } finally {
            cartMutating = false;
            setListBusy(false);
            syncCheckoutAvailability(cart.items.length > 0);
        }
    }

    async function removeItem(itemId) {
        if (cartMutating) return;
        const normalizedItemId = normalizePositiveInteger(itemId);
        if (!normalizedItemId) return;
        cartMutating = true;
        const activeRequest = ++cartRequestSequence;
        syncCheckoutAvailability(false);
        setListBusy(true);
        try {
            const nextCart = normalizeCart(await request(`/api/front/cart/items/${normalizedItemId}`, { method: "DELETE" }));
            if (activeRequest !== cartRequestSequence) return;
            if (!nextCart) throw new Error("장바구니 삭제 응답이 올바르지 않습니다.");
            cart = nextCart;
            renderCart();
            showToast("상품을 장바구니에서 삭제했습니다.");
        } catch (error) {
            showToast(error.message);
            await loadCart();
        } finally {
            cartMutating = false;
            setListBusy(false);
            syncCheckoutAvailability(cart.items.length > 0);
        }
    }

    async function clearCart() {
        if (cartMutating || !cart.items.length || !window.confirm("장바구니의 모든 상품을 삭제할까요?")) return;
        cartMutating = true;
        const activeRequest = ++cartRequestSequence;
        syncCheckoutAvailability(false);
        setListBusy(true);
        try {
            const nextCart = normalizeCart(await request("/api/front/cart/items", { method: "DELETE" }));
            if (activeRequest !== cartRequestSequence) return;
            if (!nextCart || nextCart.items.length) throw new Error("장바구니 초기화 응답이 올바르지 않습니다.");
            cart = nextCart;
            renderCart();
            showToast("장바구니를 비웠습니다.");
        } catch (error) {
            showToast(error.message);
            await loadCart();
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

    function showCompletedOrder(order, buyerPhone) {
        elements.orderNumber.textContent = order.orderNumber;
        elements.orderAmount.textContent = `${formatPrice(order.totalAmount)} · 주문 접수`;
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

        // 완료된 주문서의 개인정보와 이전 장바구니 상태를 브라우저에 남기지 않는다.
        elements.form.reset();
        cart = emptyCart();
        renderCart();
        elements.complete.hidden = false;
        document.getElementById("completedOrderTitle")?.focus();
    }

    function formatPhoneInput(input) {
        const digits = input.value.replace(/\D/g, "").slice(0, 11);
        input.value = digits.length <= 3
            ? digits
            : digits.length <= 7
                ? `${digits.slice(0, 3)}-${digits.slice(3)}`
                : `${digits.slice(0, 3)}-${digits.slice(3, digits.length - 4)}-${digits.slice(-4)}`;
    }
    async function loadSavedAddresses() {
        if (!elements.savedAddress) return;
        try {
            const response = await fetch("/api/front/member/delivery-addresses", { headers: { Accept: "application/json" } });
            if (!response.ok) return;
            savedAddresses = await response.json();
            if (!Array.isArray(savedAddresses)) return;
            elements.savedAddress.insertAdjacentHTML("beforeend", savedAddresses.map(address => `<option value="${Number(address.id)}">${escapeMarkup(address.addressName)}${address.defaultAddress ? " · 기본" : ""}</option>`).join(""));
            const primary = savedAddresses.find(address => address.defaultAddress);
            if (primary) { elements.savedAddress.value = String(primary.id); applySavedAddress(primary); }
        } catch (_) { /* 비회원과 네트워크 실패는 직접 입력을 유지한다. */ }
    }
    function applySavedAddress(address) { ["recipientName","recipientPhone","postalCode","address1","address2"].forEach(name => { const input=elements.form?.elements[name]; if (input) input.value=address[name] || ""; }); }

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
    elements.savedAddress?.addEventListener("change", () => { const address=savedAddresses.find(item=>String(item.id)===elements.savedAddress.value); if(address) applySavedAddress(address); });

    elements.form?.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (submitting || !cart.items.length || !elements.form.reportValidity()) return;
        const body = Object.fromEntries(new FormData(elements.form).entries());
        const normalizedBody = normalizeCheckoutPayload(body);
        if (!normalizedBody) {
            showToast("주문자와 배송지 입력값을 다시 확인해주세요.");
            return;
        }
        submitting = true;
        elements.submit.disabled = true;
        elements.submit.setAttribute("aria-busy", "true");
        elements.submit.querySelector("span").textContent = "주문 접수 중";
        const expectedTotalAmount = cart.totalAmount;
        try {
            const order = normalizeOrderCreateResponse(await request("/api/front/orders", {
                method: "POST",
                body: JSON.stringify(normalizedBody)
            }), expectedTotalAmount);
            if (!order) throw new Error("주문 완료 응답이 올바르지 않습니다.");
            const buyerPhone = normalizedBody.buyerPhone;
            showCompletedOrder(order, buyerPhone);
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
    loadSavedAddresses();

    function emptyCart() {
        return { items: [], itemCount: 0, totalQuantity: 0, totalAmount: 0 };
    }

    function normalizePositiveInteger(value) {
        const text = String(value ?? "").trim();
        if (!/^\d+$/.test(text)) return null;
        const parsed = Number(text);
        return Number.isSafeInteger(parsed) && parsed > 0 && parsed <= 2147483647 ? parsed : null;
    }

    function normalizeNonNegativeInteger(value) {
        const text = String(value ?? "").trim();
        if (!/^\d+$/.test(text)) return null;
        const parsed = Number(text);
        return Number.isSafeInteger(parsed) && parsed <= 2147483647 ? parsed : null;
    }

    function normalizeImageSource(value) {
        const text = String(value || "").trim();
        if (!text) return fallbackImage();
        if (text.startsWith("/") && !text.startsWith("//")) return text;
        try {
            const url = new URL(text, window.location.origin);
            return ["http:", "https:"].includes(url.protocol) ? url.href : fallbackImage();
        } catch (ignored) {
            return fallbackImage();
        }
    }

    function normalizeCart(payload) {
        if (!payload || !Array.isArray(payload.items)) return null;
        const seenItemIds = new Set();
        const seenOptionIds = new Set();
        const items = payload.items.map((item) => {
            const itemId = normalizePositiveInteger(item?.itemId);
            const productId = normalizePositiveInteger(item?.productId);
            const optionId = normalizePositiveInteger(item?.optionId);
            const unitPrice = normalizeNonNegativeInteger(item?.unitPrice);
            const quantity = normalizePositiveInteger(item?.quantity);
            const stock = normalizeNonNegativeInteger(item?.stock);
            const lineAmount = normalizeNonNegativeInteger(item?.lineAmount);
            const productName = String(item?.productName || "").trim();
            const optionName = String(item?.optionName || "").trim();
            if (!itemId || !productId || !optionId || unitPrice == null || !quantity || stock == null || lineAmount == null) return null;
            if (!productName || !optionName || quantity > 20 || quantity > stock || lineAmount !== unitPrice * quantity) return null;
            if (seenItemIds.has(itemId) || seenOptionIds.has(optionId)) return null;
            seenItemIds.add(itemId);
            seenOptionIds.add(optionId);
            return { ...item, itemId, productId, optionId, unitPrice, quantity, stock, lineAmount, productName, optionName, thumbnailUrl: normalizeImageSource(item.thumbnailUrl) };
        });
        if (items.some((item) => !item)) return null;
        const itemCount = normalizeNonNegativeInteger(payload.itemCount);
        const totalQuantity = normalizeNonNegativeInteger(payload.totalQuantity);
        const totalAmount = normalizeNonNegativeInteger(payload.totalAmount);
        const calculatedQuantity = items.reduce((sum, item) => sum + item.quantity, 0);
        const calculatedAmount = items.reduce((sum, item) => sum + item.lineAmount, 0);
        if (itemCount !== items.length || totalQuantity !== calculatedQuantity || totalAmount !== calculatedAmount) return null;
        return { items, itemCount, totalQuantity, totalAmount };
    }

    function normalizeCheckoutPayload(payload) {
        const normalizeRequired = (value, max) => {
            const text = String(value || "").trim().replace(/\s+/g, " ");
            return text && text.length <= max ? text : null;
        };
        const normalizeOptional = (value, max) => {
            const text = String(value || "").trim().replace(/\s+/g, " ");
            return text.length <= max ? text : null;
        };
        const buyerName = normalizeRequired(payload?.buyerName, 50);
        const recipientName = normalizeRequired(payload?.recipientName, 50);
        const buyerPhone = String(payload?.buyerPhone || "").trim();
        const recipientPhone = String(payload?.recipientPhone || "").trim();
        const postalCode = normalizeRequired(payload?.postalCode, 10);
        const address1 = normalizeRequired(payload?.address1, 200);
        const address2 = normalizeOptional(payload?.address2, 200);
        const deliveryRequest = normalizeOptional(payload?.deliveryRequest, 200);
        const phonePattern = /^[0-9()\-\s]{10,20}$/;
        const normalizedPhoneLength = (value) => value.replace(/\D/g, "").length;
        if (!buyerName || !recipientName || !postalCode || !address1 || address2 == null || deliveryRequest == null) return null;
        if (!phonePattern.test(buyerPhone) || !phonePattern.test(recipientPhone)) return null;
        if (![10, 11].includes(normalizedPhoneLength(buyerPhone)) || ![10, 11].includes(normalizedPhoneLength(recipientPhone))) return null;
        return { buyerName, buyerPhone, recipientName, recipientPhone, postalCode, address1, address2, deliveryRequest };
    }

    function normalizeOrderCreateResponse(order, expectedTotalAmount) {
        const orderId = normalizePositiveInteger(order?.orderId);
        const totalAmount = normalizeNonNegativeInteger(order?.totalAmount);
        const orderNumber = String(order?.orderNumber || "").trim();
        if (!orderId || !/^GS[A-Z0-9]{10,40}$/.test(orderNumber) || totalAmount !== expectedTotalAmount || order?.status !== "ORDERED") return null;
        return { orderId, orderNumber, totalAmount, status: "ORDERED" };
    }

    loadCart();
})();
