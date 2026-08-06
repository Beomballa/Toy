(() => {
    "use strict";

    const form = document.getElementById("orderLookupForm");
    const result = document.getElementById("orderResult");
    const error = document.getElementById("orderLookupError");
    const toast = document.getElementById("commerceToast");
    const initialOrderNumber = document.body.dataset.orderNumber || "";
    const isMemberOrder = new URLSearchParams(location.search).get("member") === "true";
    const cancelDialog = document.getElementById("memberOrderCancelDialog");
    const cancelForm = document.getElementById("memberOrderCancelForm");
    let currentOrder = null;
    let toastTimer = null;
    let lookupController = null;
    let lookupSequence = 0;

    const ORDER_NUMBER_PATTERN = /^GS[A-Z0-9]{10,40}$/;
    const STATUS_STEPS = Object.freeze({
        ORDERED: 1,
        PAID: 2,
        PREPARING: 3,
        SHIPPED: 4,
        DELIVERED: 5,
        CANCELLED: 0
    });
    const STATUS_LABELS = Object.freeze({
        ORDERED: "주문 접수",
        PAID: "결제 확인",
        PREPARING: "배송 준비",
        SHIPPED: "배송 중",
        DELIVERED: "배송 완료",
        CANCELLED: "주문 취소"
    });

    function escapeMarkup(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#39;");
    }

    function formatPrice(value) {
        return `${value.toLocaleString("ko-KR")}원`;
    }

    function fallbackImage() {
        return "/images/product-placeholder.svg";
    }

    function requiredText(value, maxLength, fieldName) {
        if (typeof value !== "string") throw new Error(`${fieldName} 정보가 올바르지 않습니다.`);
        const normalized = value.replace(/[\u0000-\u001f\u007f]/g, " ").replace(/\s+/g, " ").trim();
        if (!normalized || normalized.length > maxLength) {
            throw new Error(`${fieldName} 정보가 올바르지 않습니다.`);
        }
        return normalized;
    }

    function optionalText(value, maxLength) {
        if (value == null) return "";
        if (typeof value !== "string") throw new Error("주문 문구가 올바르지 않습니다.");
        const normalized = value.replace(/[\u0000-\u001f\u007f]/g, " ").replace(/\s+/g, " ").trim();
        return normalized && normalized.length <= maxLength ? normalized : "";
    }

    function safeInteger(value, fieldName, minimum = 0, maximum = Number.MAX_SAFE_INTEGER) {
        if (!Number.isSafeInteger(value) || value < minimum || value > maximum) {
            throw new Error(`${fieldName} 정보가 올바르지 않습니다.`);
        }
        return value;
    }

    function normalizeImageSource(value) {
        const normalized = optionalText(value, 500);
        if (/^\/(?!\/)/.test(normalized) || /^https?:\/\//i.test(normalized)) {
            return normalized;
        }
        return fallbackImage();
    }

    function showToast(message) {
        toast.textContent = message;
        toast.hidden = false;
        window.clearTimeout(toastTimer);
        toastTimer = window.setTimeout(() => {
            toast.hidden = true;
        }, 2400);
    }

    function formatPhone(value) {
        const digits = String(value || "").replace(/\D/g, "").slice(0, 11);
        if (digits.length <= 3) return digits;
        if (digits.length <= 7) return `${digits.slice(0, 3)}-${digits.slice(3)}`;
        return `${digits.slice(0, 3)}-${digits.slice(3, digits.length - 4)}-${digits.slice(-4)}`;
    }

    function readRecentOrder() {
        try {
            const stored = JSON.parse(window.sessionStorage.getItem("grade-stock-last-order") || "{}");
            return normalizeLookupInput(stored.orderNumber, stored.phone);
        } catch (ignored) {
            try {
                window.sessionStorage.removeItem("grade-stock-last-order");
            } catch (storageError) {
                // 저장소 접근이 제한돼도 주문 조회 화면은 계속 사용할 수 있다.
            }
            return {};
        }
    }

    function normalizeLookupInput(orderNumberValue, phoneValue) {
        if (typeof orderNumberValue !== "string" || typeof phoneValue !== "string") throw new Error("주문 조회 정보가 올바르지 않습니다.");
        const orderNumber = orderNumberValue.trim().toUpperCase();
        const rawPhone = phoneValue.trim();
        const phone = rawPhone.replace(/\D/g, "");
        if (!ORDER_NUMBER_PATTERN.test(orderNumber)) {
            throw new Error("주문번호 형식을 확인해주세요.");
        }
        if (!/^[0-9() -]+$/.test(rawPhone) || !/^\d{10,11}$/.test(phone)) {
            throw new Error("주문자 연락처 형식을 확인해주세요.");
        }
        return { orderNumber, phone };
    }

    function normalizeDelivery(value) {
        if (value == null) return null;
        if (typeof value !== "object" || Array.isArray(value)) {
            throw new Error("배송지 정보가 올바르지 않습니다.");
        }
        return {
            recipientName: requiredText(value.recipientName, 50, "받는 분"),
            recipientPhone: requiredText(value.recipientPhone, 30, "연락처"),
            postalCode: requiredText(value.postalCode, 12, "우편번호"),
            address1: requiredText(value.address1, 200, "주소"),
            address2: optionalText(value.address2, 200),
            deliveryRequest: optionalText(value.deliveryRequest, 200)
        };
    }

    function normalizeOrderResponse(value, expectedOrderNumber) {
        if (!value || typeof value !== "object" || Array.isArray(value)) {
            throw new Error("주문 조회 응답이 올바르지 않습니다.");
        }
        const orderNumber = requiredText(value.orderNumber, 42, "주문번호").toUpperCase();
        if (!ORDER_NUMBER_PATTERN.test(orderNumber) || orderNumber !== expectedOrderNumber) {
            throw new Error("조회한 주문과 응답 정보가 일치하지 않습니다.");
        }
        const status = requiredText(value.status, 20, "주문 상태").toUpperCase();
        if (!Object.hasOwn(STATUS_STEPS, status)) {
            throw new Error("지원하지 않는 주문 상태입니다.");
        }
        if (value.statusLabel !== STATUS_LABELS[status] || value.statusStep !== STATUS_STEPS[status]) {
            throw new Error("주문 상태 표시 정보가 일치하지 않습니다.");
        }
        if (!Array.isArray(value.items) || value.items.length === 0 || value.items.length > 100) {
            throw new Error("주문 상품 정보가 올바르지 않습니다.");
        }
        const items = value.items.map((item) => {
            const unitPrice = safeInteger(item?.unitPrice, "상품 가격", 0, 1000000000);
            const quantity = safeInteger(item?.quantity, "상품 수량", 1, 20);
            const lineAmount = safeInteger(item?.lineAmount, "상품 합계", 0, 2000000000);
            if (unitPrice * quantity !== lineAmount) {
                throw new Error("주문 상품 합계가 올바르지 않습니다.");
            }
            return {
                productId: safeInteger(item?.productId, "상품 번호", 1),
                productName: requiredText(item?.productName, 200, "상품명"),
                thumbnailUrl: normalizeImageSource(item?.thumbnailUrl),
                unitPrice,
                quantity,
                lineAmount
            };
        });
        const totalAmount = safeInteger(value.totalAmount, "총 주문 금액", 0, 2000000000);
        if (items.reduce((sum, item) => sum + item.lineAmount, 0) !== totalAmount) {
            throw new Error("총 주문 금액이 상품 합계와 일치하지 않습니다.");
        }
        const rawHistory = Array.isArray(value.statusHistory) ? value.statusHistory : [];
        if (rawHistory.length === 0 || rawHistory.length > 20) {
            throw new Error("주문 처리 이력이 올바르지 않습니다.");
        }
        const statusHistory = rawHistory.map((event) => {
            const eventStatus = requiredText(event?.status, 20, "처리 상태").toUpperCase();
            if (!Object.hasOwn(STATUS_LABELS, eventStatus)) {
                throw new Error("주문 처리 이력이 올바르지 않습니다.");
            }
            const changedAt = requiredText(event?.changedAt, 30, "처리 일시");
            if (!/^\d{4}[.-]\d{2}[.-]\d{2} \d{2}:\d{2}$/.test(changedAt)
                || event.statusLabel !== STATUS_LABELS[eventStatus]) throw new Error("주문 처리 이력이 올바르지 않습니다.");
            return {
                status: eventStatus,
                statusLabel: STATUS_LABELS[eventStatus],
                changedAt
            };
        });
        if (statusHistory[0].status !== status
            || statusHistory.some((event, index) => index > 0 && statusHistory[index - 1].changedAt < event.changedAt)) {
            throw new Error("주문 처리 이력 순서가 올바르지 않습니다.");
        }
        const orderedAt = requiredText(value.orderedAt, 30, "주문 일시");
        if (!/^\d{4}[.-]\d{2}[.-]\d{2} \d{2}:\d{2}$/.test(orderedAt)) throw new Error("주문 일시가 올바르지 않습니다.");
        const deliveryCompany = optionalText(value.deliveryCompany, 50);
        const trackingNumber = optionalText(value.trackingNumber, 80);
        if (Boolean(deliveryCompany) !== Boolean(trackingNumber)) throw new Error("배송 추적 정보가 올바르지 않습니다.");
        return {
            orderNumber,
            buyerName: requiredText(value.buyerName, 50, "주문자"),
            totalAmount,
            status,
            statusLabel: STATUS_LABELS[status],
            statusStep: STATUS_STEPS[status],
            orderedAt,
            deliveryCompany,
            trackingNumber,
            delivery: normalizeDelivery(value.delivery),
            items,
            statusHistory
        };
    }

    async function copyText(value, successMessage) {
        if (!value) return;
        try {
            await navigator.clipboard.writeText(value);
            showToast(successMessage);
        } catch (ignored) {
            showToast("복사하지 못했습니다. 값을 직접 선택해주세요.");
        }
    }

    async function lookup(orderNumber, phone) {
        lookupController?.abort();
        lookupController = new AbortController();
        const activeRequest = ++lookupSequence;
        clearOrderResult();
        error.hidden = true;
        setLookupBusy(true);
        try {
            const response = await fetch("/api/front/orders/lookup", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ orderNumber, phone }),
                signal: lookupController.signal
            });
            const payload = await response.json().catch(() => ({}));
            if (!response.ok) {
                const fallback = response.status === 429
                    ? "조회 요청이 많습니다. 5분 후 다시 시도해주세요."
                    : "주문 정보를 확인할 수 없습니다.";
                throw new Error(typeof payload.message === "string" && payload.message.trim() ? payload.message.slice(0, 200) : fallback);
            }
            if (activeRequest !== lookupSequence) return;
            const order = normalizeOrderResponse(payload, orderNumber);
            render(order);
            try {
                window.history.replaceState(null, "", `/front/orders/${encodeURIComponent(order.orderNumber)}`);
            } catch (ignored) {
                // URL 갱신이 제한된 환경에서도 조회 결과는 유지한다.
            }
        } catch (requestError) {
            if (requestError.name === "AbortError" || activeRequest !== lookupSequence) return;
            clearOrderResult();
            error.textContent = requestError.message;
            error.hidden = false;
        } finally {
            if (activeRequest === lookupSequence) {
                setLookupBusy(false);
            }
        }
    }

    async function lookupMemberOrder(orderNumber) {
        lookupController?.abort();
        lookupController = new AbortController();
        const activeRequest = ++lookupSequence;
        clearOrderResult();
        error.hidden = true;
        setLookupBusy(true);
        try {
            const response = await fetch(`/api/front/member/orders/${encodeURIComponent(orderNumber)}`, {
                headers: { Accept: "application/json" }, signal: lookupController.signal
            });
            const payload = await response.json().catch(() => ({}));
            if (!response.ok) {
                const fallback = response.status === 401 ? "로그인 후 주문 내역을 확인할 수 있습니다." : "주문 정보를 확인할 수 없습니다.";
                throw new Error(typeof payload.message === "string" && payload.message.trim() ? payload.message.slice(0, 200) : fallback);
            }
            if (activeRequest !== lookupSequence) return;
            render(normalizeOrderResponse(payload, orderNumber));
        } catch (requestError) {
            if (requestError.name === "AbortError" || activeRequest !== lookupSequence) return;
            clearOrderResult();
            error.textContent = requestError.message;
            error.hidden = false;
        } finally {
            if (activeRequest === lookupSequence) setLookupBusy(false);
        }
    }

    async function cancelMemberOrder() {
        if (!currentOrder || !isMemberOrder) return;
        const submitButton = document.getElementById("memberOrderCancelSubmitButton");
        submitButton.disabled = true;
        try {
            const reason = document.getElementById("memberOrderCancelReason").value.trim().slice(0, 200);
            const response = await fetch(`/api/front/member/orders/${encodeURIComponent(currentOrder.orderNumber)}/cancel`, {
                method: "POST", headers: { "Content-Type": "application/json", Accept: "application/json" },
                body: JSON.stringify({ reason })
            });
            const payload = await response.json().catch(() => ({}));
            if (!response.ok) throw new Error(typeof payload.message === "string" ? payload.message : "주문을 취소하지 못했습니다.");
            const order = normalizeOrderResponse(payload, currentOrder.orderNumber);
            cancelDialog.close();
            render(order);
            showToast("주문을 취소하고 재고를 복구했습니다.");
        } catch (requestError) {
            showToast(requestError.message || "주문을 취소하지 못했습니다.");
        } finally {
            submitButton.disabled = false;
        }
    }

    function setLookupBusy(busy) {
        const button = form.querySelector("button");
        button.disabled = busy;
        button.toggleAttribute("aria-busy", busy);
        button.querySelector("span").textContent = busy ? "조회 중" : "주문 조회";
    }

    function clearOrderResult() {
        currentOrder = null;
        result.hidden = true;
        document.getElementById("orderResultNumber").textContent = "";
        document.getElementById("orderResultDate").textContent = "";
        document.getElementById("orderResultStatus").textContent = "";
        document.getElementById("orderTotalAmount").textContent = "";
        document.getElementById("orderItemSummary").textContent = "";
        document.getElementById("orderItems").replaceChildren();
        document.getElementById("orderDelivery").replaceChildren();
        document.getElementById("orderHistory").replaceChildren();
        document.getElementById("orderTracking").hidden = true;
        document.getElementById("orderTrackingText").textContent = "";
    }

    function render(order) {
        currentOrder = order;
        document.getElementById("orderResultNumber").textContent = order.orderNumber;
        document.getElementById("orderResultDate").textContent = `${order.orderedAt} · 주문자 ${order.buyerName}`;
        document.getElementById("orderResultStatus").textContent = order.statusLabel;
        document.getElementById("memberOrderCancelButton").hidden = !isMemberOrder
            || !["ORDERED", "PAID", "PREPARING"].includes(order.status);
        document.getElementById("orderTotalAmount").textContent = formatPrice(order.totalAmount);
        const totalQuantity = order.items.reduce((sum, item) => sum + Number(item.quantity || 0), 0);
        document.getElementById("orderItemSummary").textContent = `${order.items.length}개 상품 · 총 ${totalQuantity}개`;
        document.querySelectorAll("#orderProgress li").forEach((item, index) => {
            item.classList.toggle("is-complete", order.statusStep > 0 && index + 1 <= order.statusStep);
            item.classList.toggle("is-current", index + 1 === order.statusStep);
        });
        document.getElementById("orderItems").innerHTML = order.items.map((item) => `
            <article class="order-item">
                <a href="/front/products/${item.productId}"><img src="${escapeMarkup(item.thumbnailUrl || fallbackImage())}" alt="${escapeMarkup(item.productName)}" data-order-product-image></a>
                <div><strong>${escapeMarkup(item.productName)}</strong><span>${formatPrice(item.unitPrice)} · ${item.quantity}개</span></div>
                <b>${formatPrice(item.lineAmount)}</b>
            </article>
        `).join("");
        const delivery = order.delivery;
        document.getElementById("orderDelivery").innerHTML = delivery ? `
            <div><dt>받는 분</dt><dd>${escapeMarkup(delivery.recipientName)}</dd></div>
            <div><dt>연락처</dt><dd>${escapeMarkup(delivery.recipientPhone)}</dd></div>
            <div><dt>주소</dt><dd>(${escapeMarkup(delivery.postalCode)}) ${escapeMarkup(delivery.address1)} ${escapeMarkup(delivery.address2 || "")}</dd></div>
            <div><dt>요청사항</dt><dd>${escapeMarkup(delivery.deliveryRequest || "-")}</dd></div>
        ` : "<div><dt>배송지</dt><dd>등록된 배송지가 없습니다.</dd></div>";
        const tracking = document.getElementById("orderTracking");
        tracking.hidden = !order.trackingNumber;
        document.getElementById("orderTrackingText").textContent = order.trackingNumber
            ? `${order.deliveryCompany || "택배사 확인"} · ${order.trackingNumber}`
            : "";
        document.getElementById("orderHistory").innerHTML = order.statusHistory.map((event) => `
            <li><strong>${escapeMarkup(event.statusLabel)}</strong><span>${escapeMarkup(event.changedAt)}</span></li>
        `).join("");
        document.querySelectorAll("[data-order-product-image]").forEach((image) => {
            image.addEventListener("error", () => {
                if (image.dataset.fallbackApplied === "true") return;
                image.dataset.fallbackApplied = "true";
                image.src = fallbackImage();
            }, { once: true });
        });
        result.hidden = false;
        result.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    form.addEventListener("submit", (event) => {
        event.preventDefault();
        if (!form.reportValidity()) return;
        const values = new FormData(form);
        try {
            const input = normalizeLookupInput(values.get("orderNumber"), values.get("phone"));
            form.elements.orderNumber.value = input.orderNumber;
            form.elements.phone.value = formatPhone(input.phone);
            lookup(input.orderNumber, input.phone);
        } catch (inputError) {
            clearOrderResult();
            error.textContent = inputError.message;
            error.hidden = false;
        }
    });

    form.elements.orderNumber.addEventListener("input", (event) => {
        event.target.value = event.target.value.toUpperCase().replace(/[^A-Z0-9]/g, "");
    });
    form.elements.phone.addEventListener("input", (event) => {
        event.target.value = formatPhone(event.target.value);
    });
    document.getElementById("clearOrderLookupButton").addEventListener("click", () => {
        lookupController?.abort();
        lookupSequence += 1;
        setLookupBusy(false);
        form.reset();
        clearOrderResult();
        error.hidden = true;
        try {
            window.history.replaceState(null, "", "/front/orders");
        } catch (ignored) {
            // URL 갱신이 제한된 환경에서도 입력과 조회 결과는 초기화한다.
        }
        form.elements.orderNumber.focus();
    });
    document.getElementById("loadRecentOrderButton").addEventListener("click", () => {
        const recent = readRecentOrder();
        if (!recent.orderNumber || !recent.phone) {
            showToast("최근 주문 정보가 없습니다.");
            return;
        }
        form.elements.orderNumber.value = recent.orderNumber;
        form.elements.phone.value = formatPhone(recent.phone);
        lookup(recent.orderNumber, recent.phone);
    });
    document.getElementById("copyOrderNumberButton").addEventListener("click", () => {
        copyText(currentOrder?.orderNumber, "주문번호를 복사했습니다.");
    });
    document.getElementById("copyTrackingButton").addEventListener("click", () => {
        copyText(currentOrder?.trackingNumber, "송장번호를 복사했습니다.");
    });
    document.getElementById("printOrderButton").addEventListener("click", () => {
        if (!currentOrder) {
            showToast("먼저 주문을 조회해주세요.");
            return;
        }
        window.print();
    });
    document.getElementById("memberOrderCancelButton").addEventListener("click", () => {
        document.getElementById("memberOrderCancelReason").value = "";
        cancelDialog.showModal();
        document.getElementById("memberOrderCancelReason").focus();
    });
    document.getElementById("memberOrderCancelCloseButton").addEventListener("click", () => cancelDialog.close());
    cancelForm.addEventListener("submit", (event) => { event.preventDefault(); cancelMemberOrder(); });

    if (initialOrderNumber) {
        if (isMemberOrder) {
            lookupMemberOrder(initialOrderNumber);
            return;
        }
        const recent = readRecentOrder();
        if (recent.orderNumber === initialOrderNumber && recent.phone) {
            form.elements.phone.value = formatPhone(recent.phone);
            lookup(initialOrderNumber, recent.phone);
        }
    }
})();
