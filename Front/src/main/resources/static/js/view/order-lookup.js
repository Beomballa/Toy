(() => {
    "use strict";

    const form = document.getElementById("orderLookupForm");
    const result = document.getElementById("orderResult");
    const error = document.getElementById("orderLookupError");
    const toast = document.getElementById("commerceToast");
    const initialOrderNumber = document.body.dataset.orderNumber || "";
    let currentOrder = null;
    let toastTimer = null;

    function escapeMarkup(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#39;");
    }

    function formatPrice(value) {
        return `${Number(value || 0).toLocaleString("ko-KR")}원`;
    }

    function fallbackImage(name) {
        return `https://placehold.co/180x180/f4f4f4/999?text=${encodeURIComponent(String(name || "GS").slice(0, 10))}`;
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
            return JSON.parse(window.sessionStorage.getItem("grade-stock-last-order") || "{}");
        } catch (ignored) {
            return {};
        }
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
        error.hidden = true;
        const button = form.querySelector("button");
        button.disabled = true;
        button.setAttribute("aria-busy", "true");
        button.querySelector("span").textContent = "조회 중";
        try {
            const response = await fetch("/api/front/orders/lookup", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ orderNumber, phone })
            });
            const payload = await response.json().catch(() => ({}));
            if (!response.ok) {
                const fallback = response.status === 429
                    ? "조회 요청이 많습니다. 5분 후 다시 시도해주세요."
                    : "주문 정보를 확인할 수 없습니다.";
                throw new Error(payload.message || fallback);
            }
            render(payload);
            try {
                window.history.replaceState(null, "", `/front/orders/${encodeURIComponent(payload.orderNumber)}`);
            } catch (ignored) {
                // URL 갱신이 제한된 환경에서도 조회 결과는 유지한다.
            }
        } catch (requestError) {
            result.hidden = true;
            error.textContent = requestError.message;
            error.hidden = false;
        } finally {
            button.disabled = false;
            button.removeAttribute("aria-busy");
            button.querySelector("span").textContent = "주문 조회";
        }
    }

    function render(order) {
        currentOrder = order;
        document.getElementById("orderResultNumber").textContent = order.orderNumber;
        document.getElementById("orderResultDate").textContent = `${order.orderedAt} · 주문자 ${order.buyerName}`;
        document.getElementById("orderResultStatus").textContent = order.statusLabel;
        document.getElementById("orderTotalAmount").textContent = formatPrice(order.totalAmount);
        const totalQuantity = order.items.reduce((sum, item) => sum + Number(item.quantity || 0), 0);
        document.getElementById("orderItemSummary").textContent = `${order.items.length}개 상품 · 총 ${totalQuantity}개`;
        document.querySelectorAll("#orderProgress li").forEach((item, index) => {
            item.classList.toggle("is-complete", order.statusStep > 0 && index + 1 <= order.statusStep);
            item.classList.toggle("is-current", index + 1 === order.statusStep);
        });
        document.getElementById("orderItems").innerHTML = order.items.map((item) => `
            <article class="order-item">
                <a href="/front/products/${item.productId}"><img src="${escapeMarkup(item.thumbnailUrl || fallbackImage(item.productName))}" alt="${escapeMarkup(item.productName)}"></a>
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
        result.hidden = false;
        result.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    form.addEventListener("submit", (event) => {
        event.preventDefault();
        if (!form.reportValidity()) return;
        const values = new FormData(form);
        const orderNumber = String(values.get("orderNumber") || "").trim().toUpperCase();
        form.elements.orderNumber.value = orderNumber;
        lookup(orderNumber, String(values.get("phone") || "").trim());
    });

    form.elements.orderNumber.addEventListener("input", (event) => {
        event.target.value = event.target.value.toUpperCase().replace(/[^A-Z0-9]/g, "");
    });
    form.elements.phone.addEventListener("input", (event) => {
        event.target.value = formatPhone(event.target.value);
    });
    document.getElementById("clearOrderLookupButton").addEventListener("click", () => {
        form.reset();
        if (initialOrderNumber) form.elements.orderNumber.value = initialOrderNumber;
        currentOrder = null;
        result.hidden = true;
        error.hidden = true;
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
    document.getElementById("printOrderButton").addEventListener("click", () => window.print());

    if (initialOrderNumber) {
        const recent = readRecentOrder();
        if (recent.orderNumber === initialOrderNumber && recent.phone) {
            form.elements.phone.value = formatPhone(recent.phone);
            lookup(initialOrderNumber, recent.phone);
        }
    }
})();
