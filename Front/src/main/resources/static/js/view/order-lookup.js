(() => {
    "use strict";

    const form = document.getElementById("orderLookupForm");
    const result = document.getElementById("orderResult");
    const error = document.getElementById("orderLookupError");
    const initialOrderNumber = document.body.dataset.orderNumber || "";

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

    async function lookup(orderNumber, phone) {
        error.hidden = true;
        const button = form.querySelector("button");
        button.disabled = true;
        try {
            const response = await fetch(`/api/front/orders/${encodeURIComponent(orderNumber)}?phone=${encodeURIComponent(phone)}`);
            const payload = await response.json().catch(() => ({}));
            if (!response.ok) {
                throw new Error(payload.message || "주문 정보를 확인할 수 없습니다.");
            }
            render(payload);
            window.history.replaceState(null, "", `/front/orders/${encodeURIComponent(payload.orderNumber)}`);
        } catch (requestError) {
            result.hidden = true;
            error.textContent = requestError.message;
            error.hidden = false;
        } finally {
            button.disabled = false;
        }
    }

    function render(order) {
        document.getElementById("orderResultNumber").textContent = order.orderNumber;
        document.getElementById("orderResultDate").textContent = `${order.orderedAt} · 주문자 ${order.buyerName}`;
        document.getElementById("orderResultStatus").textContent = order.statusLabel;
        document.getElementById("orderTotalAmount").textContent = formatPrice(order.totalAmount);
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
        tracking.textContent = order.trackingNumber
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
        lookup(String(values.get("orderNumber") || "").trim(), String(values.get("phone") || "").trim());
    });

    if (initialOrderNumber) {
        try {
            const recent = JSON.parse(window.sessionStorage.getItem("grade-stock-last-order") || "{}");
            if (recent.orderNumber === initialOrderNumber && recent.phone) {
                form.elements.phone.value = recent.phone;
                lookup(initialOrderNumber, recent.phone);
            }
        } catch (ignored) {
            // 세션 저장소를 사용할 수 없어도 직접 조회 폼은 정상 동작한다.
        }
    }
})();
