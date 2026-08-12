(() => {
    "use strict";
    const form = document.getElementById("deliveryAddressForm");
    const list = document.getElementById("deliveryAddressList");
    const toast = document.getElementById("deliveryAddressToast");
    const state = { addresses: [], saving: false };
    let toastTimer;

    const safe = (value) => String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;").replaceAll("'", "&#39;");
    const message = (value) => { toast.textContent = value; toast.hidden = false; clearTimeout(toastTimer); toastTimer = setTimeout(() => { toast.hidden = true; }, 2600); };
    const normalizePhone = (value) => { const digits = String(value ?? "").replace(/\D/g, "").slice(0, 11); return digits.length <= 3 ? digits : digits.length <= 7 ? `${digits.slice(0, 3)}-${digits.slice(3)}` : `${digits.slice(0, 3)}-${digits.slice(3, -4)}-${digits.slice(-4)}`; };

    async function request(path, options = {}) {
        const response = await fetch(path, { ...options, headers: { Accept: "application/json", ...(options.headers || {}) } });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(typeof payload.message === "string" ? payload.message : "배송지 요청을 처리하지 못했습니다.");
        return payload;
    }

    function render() {
        document.getElementById("deliveryAddressCount").textContent = `${state.addresses.length}개`;
        list.setAttribute("aria-busy", "false");
        list.innerHTML = state.addresses.length ? state.addresses.map(address => `<article class="address-card ${address.defaultAddress ? "is-default" : ""}"><h3>${safe(address.addressName)}${address.defaultAddress ? "<span>기본 배송지</span>" : ""}</h3><p><strong>${safe(address.recipientName)}</strong> · ${safe(normalizePhone(address.recipientPhone))}</p><p>(${safe(address.postalCode)}) ${safe(address.address1)} ${safe(address.address2 || "")}</p><div class="address-card__actions"><button type="button" data-address-edit="${Number(address.id)}">수정</button>${address.defaultAddress ? "" : `<button type="button" data-address-default="${Number(address.id)}">기본으로 설정</button>`}<button type="button" data-address-delete="${Number(address.id)}">삭제</button></div></article>`).join("") : '<div class="address-empty">저장한 배송지가 없습니다. 주문에 사용할 배송지를 등록해보세요.</div>';
    }

    async function load() {
        list.setAttribute("aria-busy", "true");
        try { const payload = await request("/api/front/member/delivery-addresses"); state.addresses = Array.isArray(payload) ? payload : []; render(); }
        catch (error) { list.setAttribute("aria-busy", "false"); list.innerHTML = `<div class="address-empty">${safe(error.message)}</div>`; }
    }

    function reset() {
        form.reset(); form.elements.id.value = "";
        document.getElementById("addressFormTitle").textContent = "새 배송지 등록";
        document.getElementById("deliveryAddressSubmit").textContent = "배송지 저장";
    }

    function beginEdit(id) {
        const address = state.addresses.find(item => Number(item.id) === id);
        if (!address) return;
        form.elements.id.value = String(address.id);
        ["addressName", "recipientName", "recipientPhone", "postalCode", "address1", "address2"].forEach(name => { form.elements[name].value = address[name] || ""; });
        form.elements.defaultAddress.checked = Boolean(address.defaultAddress);
        document.getElementById("addressFormTitle").textContent = `${address.addressName} 수정`;
        document.getElementById("deliveryAddressSubmit").textContent = "배송지 수정";
        form.scrollIntoView({ behavior: "smooth", block: "start" }); form.elements.addressName.focus();
    }

    form.elements.recipientPhone.addEventListener("input", () => { form.elements.recipientPhone.value = normalizePhone(form.elements.recipientPhone.value); });
    form.elements.postalCode.addEventListener("input", () => { form.elements.postalCode.value = form.elements.postalCode.value.replace(/\D/g, "").slice(0, 10); });
    document.getElementById("deliveryAddressFormReset").addEventListener("click", reset);
    form.addEventListener("submit", async event => {
        event.preventDefault(); if (state.saving || !form.reportValidity()) return;
        const id = form.elements.id.value; const body = Object.fromEntries(new FormData(form).entries()); body.defaultAddress = form.elements.defaultAddress.checked;
        state.saving = true; const submit = document.getElementById("deliveryAddressSubmit"); submit.disabled = true;
        try { state.addresses = await request(id ? `/api/front/member/delivery-addresses/${encodeURIComponent(id)}` : "/api/front/member/delivery-addresses", { method: id ? "PUT" : "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) }); reset(); render(); message(id ? "배송지를 수정했습니다." : "배송지를 저장했습니다."); }
        catch (error) { message(error.message); } finally { state.saving = false; submit.disabled = false; }
    });
    list.addEventListener("click", async event => {
        const edit = event.target.closest("[data-address-edit]"); if (edit) return beginEdit(Number(edit.dataset.addressEdit));
        const defaultButton = event.target.closest("[data-address-default]"); const deleteButton = event.target.closest("[data-address-delete]");
        const path = defaultButton ? `/api/front/member/delivery-addresses/${defaultButton.dataset.addressDefault}/default` : deleteButton ? `/api/front/member/delivery-addresses/${deleteButton.dataset.addressDelete}` : null;
        if (!path || (deleteButton && !confirm("이 배송지를 삭제할까요?"))) return;
        try { state.addresses = await request(path, { method: defaultButton ? "PUT" : "DELETE" }); render(); message(defaultButton ? "기본 배송지를 변경했습니다." : "배송지를 삭제했습니다."); } catch (error) { message(error.message); }
    });
    load();
})();
