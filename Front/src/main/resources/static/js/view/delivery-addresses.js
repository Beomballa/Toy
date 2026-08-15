(() => {
    "use strict";

    const MAX_ADDRESS_COUNT = 20;
    const form = document.getElementById("deliveryAddressForm");
    const addressIdInput = document.getElementById("deliveryAddressId");
    const formSection = document.querySelector(".address-create");
    const formStatus = document.getElementById("deliveryAddressFormStatus");
    const formTitle = document.getElementById("addressFormTitle");
    const submitButton = document.getElementById("deliveryAddressSubmit");
    const resetButton = document.getElementById("deliveryAddressFormReset");
    const list = document.getElementById("deliveryAddressList");
    const toast = document.getElementById("deliveryAddressToast");
    const state = { addresses: [], saving: false, mutating: false };
    let toastTimer;

    function safe(value) {
        return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;").replaceAll("'", "&#39;");
    }

    function cleanText(value, maxLength, field) {
        if (typeof value !== "string") throw new Error(`${field} 정보가 올바르지 않습니다.`);
        const normalized = value.replace(/[\u0000-\u001f\u007f]/g, " ").replace(/\s+/g, " ").trim();
        if (!normalized || normalized.length > maxLength) throw new Error(`${field} 정보가 올바르지 않습니다.`);
        return normalized;
    }

    function optionalText(value, maxLength) {
        if (value == null) return "";
        if (typeof value !== "string") throw new Error("배송지 정보가 올바르지 않습니다.");
        return value.replace(/[\u0000-\u001f\u007f]/g, " ").replace(/\s+/g, " ").trim().slice(0, maxLength);
    }

    function normalizePhone(value) {
        const digits = String(value ?? "").replace(/\D/g, "").slice(0, 11);
        return digits.length <= 3 ? digits : digits.length <= 7
            ? `${digits.slice(0, 3)}-${digits.slice(3)}`
            : `${digits.slice(0, 3)}-${digits.slice(3, -4)}-${digits.slice(-4)}`;
    }

    function normalizeAddresses(payload) {
        if (!Array.isArray(payload) || payload.length > MAX_ADDRESS_COUNT) throw new Error("배송지 목록 응답이 올바르지 않습니다.");
        const ids = new Set();
        let defaultCount = 0;
        return payload.map((address) => {
            const id = Number(address?.id);
            if (!Number.isSafeInteger(id) || id <= 0 || ids.has(id)) throw new Error("배송지 번호가 올바르지 않습니다.");
            ids.add(id);
            const defaultAddress = address.defaultAddress === true;
            if (defaultAddress) defaultCount += 1;
            if (defaultCount > 1) throw new Error("기본 배송지 정보가 올바르지 않습니다.");
            return {
                id,
                addressName: cleanText(address.addressName, 40, "배송지 이름"),
                recipientName: cleanText(address.recipientName, 50, "받는 분"),
                recipientPhone: cleanText(address.recipientPhone, 20, "연락처"),
                postalCode: cleanText(address.postalCode, 10, "우편번호"),
                address1: cleanText(address.address1, 200, "주소"),
                address2: optionalText(address.address2, 200),
                defaultAddress
            };
        });
    }

    function message(value) {
        toast.textContent = value;
        toast.hidden = false;
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => { toast.hidden = true; }, 4200);
    }

    async function request(path, options = {}) {
        const response = await fetch(path, { ...options, headers: { Accept: "application/json", ...(options.headers || {}) } });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok) {
            const fallback = response.status === 401 ? "로그인 후 배송지를 관리할 수 있습니다." : "배송지 요청을 처리하지 못했습니다.";
            const error = new Error(typeof payload.message === "string" && payload.message.trim() ? payload.message.slice(0, 200) : fallback);
            error.status = response.status;
            throw error;
        }
        return payload;
    }

    function render() {
        document.getElementById("deliveryAddressCount").textContent = `${state.addresses.length}개`;
        list.setAttribute("aria-busy", "false");
        list.innerHTML = state.addresses.length ? state.addresses.map(address => `
            <article class="address-card ${address.defaultAddress ? "is-default" : ""}">
                <h3><span>${safe(address.addressName)}</span>${address.defaultAddress ? "<b>기본 배송지</b>" : ""}</h3>
                <p><strong>${safe(address.recipientName)}</strong> · ${safe(normalizePhone(address.recipientPhone))}</p>
                <p>(${safe(address.postalCode)}) ${safe(address.address1)} ${safe(address.address2)}</p>
                <div class="address-card__actions"><button type="button" data-address-edit="${address.id}">수정</button>${address.defaultAddress ? "" : `<button type="button" data-address-default="${address.id}">기본으로 설정</button>`}<button type="button" data-address-delete="${address.id}">삭제</button></div>
            </article>`).join("") : '<div class="address-empty"><strong>저장한 배송지가 없습니다.</strong><p>주문에 사용할 첫 배송지를 등록해보세요.</p><button type="button" data-address-focus-form>배송지 등록하기</button></div>';
    }

    function renderLoadError(error) {
        list.setAttribute("aria-busy", "false");
        const action = error.status === 401
            ? '<a href="/front/login?next=/front/my/addresses">로그인</a>'
            : '<button type="button" data-address-retry>다시 시도</button>';
        list.innerHTML = `<div class="address-empty"><strong>배송지를 불러오지 못했습니다.</strong><p>${safe(error.message)}</p>${action}</div>`;
    }

    async function load() {
        list.setAttribute("aria-busy", "true");
        list.innerHTML = '<div class="address-empty">배송지를 불러오는 중입니다.</div>';
        try {
            state.addresses = normalizeAddresses(await request("/api/front/member/delivery-addresses"));
            render();
        } catch (error) {
            renderLoadError(error);
        }
    }

    function clearStatus() {
        formStatus.hidden = true;
        formStatus.textContent = "";
    }

    function clearFieldError(name) {
        const input = form.elements[name];
        const target = form.querySelector(`[data-address-error="${name}"]`);
        input?.removeAttribute("aria-invalid");
        if (target) {
            target.textContent = "";
            target.hidden = true;
        }
    }

    function fieldError(name, value) {
        const input = form.elements[name];
        const target = form.querySelector(`[data-address-error="${name}"]`);
        if (!input || !target) return;
        input.setAttribute("aria-invalid", "true");
        target.textContent = value;
        target.hidden = false;
    }

    function validateForm() {
        clearStatus();
        form.querySelectorAll("[data-address-error]").forEach(target => { target.hidden = true; target.textContent = ""; });
        form.querySelectorAll("[aria-invalid]").forEach(input => input.removeAttribute("aria-invalid"));
        for (const input of form.querySelectorAll("input[required]")) {
            if (input.value.trim()) continue;
            fieldError(input.name, `${input.labels?.[0]?.textContent.trim() || "필수 항목"}을 입력해 주세요.`);
            input.focus();
            return false;
        }
        if (!/^\d{10,11}$/.test(form.elements.recipientPhone.value.replace(/\D/g, ""))) {
            fieldError("recipientPhone", "연락처는 숫자 10~11자리로 입력해 주세요.");
            form.elements.recipientPhone.focus();
            return false;
        }
        if (!/^\d{5,10}$/.test(form.elements.postalCode.value)) {
            fieldError("postalCode", "우편번호는 숫자 5~10자리로 입력해 주세요.");
            form.elements.postalCode.focus();
            return false;
        }
        return true;
    }

    function reset(focus = false) {
        form.reset();
        addressIdInput.value = "";
        clearStatus();
        form.querySelectorAll("[data-address-error]").forEach(target => { target.hidden = true; target.textContent = ""; });
        form.querySelectorAll("[aria-invalid]").forEach(input => input.removeAttribute("aria-invalid"));
        formTitle.textContent = "새 배송지 등록";
        submitButton.textContent = "배송지 저장";
        resetButton.textContent = "입력 초기화";
        formSection.classList.remove("is-editing");
        if (focus) form.elements.addressName.focus();
    }

    function beginEdit(id) {
        const address = state.addresses.find(item => item.id === id);
        if (!address || state.mutating) return;
        addressIdInput.value = String(address.id);
        ["addressName", "recipientName", "recipientPhone", "postalCode", "address1", "address2"].forEach(name => { form.elements[name].value = address[name] || ""; });
        form.elements.defaultAddress.checked = address.defaultAddress;
        formTitle.textContent = `${address.addressName} 수정`;
        submitButton.textContent = "배송지 수정";
        resetButton.textContent = "수정 취소";
        formSection.classList.add("is-editing");
        formSection.scrollIntoView({ behavior: matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth", block: "start" });
        form.elements.addressName.focus({ preventScroll: true });
    }

    function setSaving(value) {
        state.saving = value;
        form.querySelectorAll("input, button").forEach(element => { element.disabled = value; });
        form.setAttribute("aria-busy", String(value));
        submitButton.textContent = value ? submitButton.dataset.pendingLabel : addressIdInput.value ? "배송지 수정" : "배송지 저장";
    }

    async function mutateAddress(path, method, successMessage) {
        if (state.mutating) return;
        state.mutating = true;
        list.setAttribute("aria-busy", "true");
        list.querySelectorAll("button").forEach(button => { button.disabled = true; });
        try {
            state.addresses = normalizeAddresses(await request(path, { method }));
            render();
            message(successMessage);
        } catch (error) {
            message(error.message);
            render();
        } finally {
            state.mutating = false;
            list.setAttribute("aria-busy", "false");
        }
    }

    form.elements.recipientPhone.addEventListener("input", () => {
        form.elements.recipientPhone.value = normalizePhone(form.elements.recipientPhone.value);
        clearFieldError("recipientPhone");
    });
    form.elements.postalCode.addEventListener("input", () => {
        form.elements.postalCode.value = form.elements.postalCode.value.replace(/\D/g, "").slice(0, 10);
        clearFieldError("postalCode");
    });
    form.querySelectorAll("input").forEach(input => input.addEventListener("input", () => clearFieldError(input.name)));
    resetButton.addEventListener("click", () => reset(true));
    form.addEventListener("submit", async event => {
        event.preventDefault();
        if (state.saving || !validateForm()) return;
        const id = addressIdInput.value;
        const body = {
            addressName: form.elements.addressName.value.trim(),
            recipientName: form.elements.recipientName.value.trim(),
            recipientPhone: form.elements.recipientPhone.value,
            postalCode: form.elements.postalCode.value,
            address1: form.elements.address1.value.trim(),
            address2: form.elements.address2.value.trim(),
            defaultAddress: form.elements.defaultAddress.checked
        };
        setSaving(true);
        try {
            const path = id ? `/api/front/member/delivery-addresses/${encodeURIComponent(id)}` : "/api/front/member/delivery-addresses";
            state.addresses = normalizeAddresses(await request(path, { method: id ? "PUT" : "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) }));
            reset();
            render();
            message(id ? "배송지를 수정했습니다." : "배송지를 저장했습니다.");
            document.getElementById("deliveryAddressListTitle").setAttribute("tabindex", "-1");
            document.getElementById("deliveryAddressListTitle").focus();
        } catch (error) {
            formStatus.textContent = error.message;
            formStatus.hidden = false;
        } finally {
            setSaving(false);
        }
    });

    list.addEventListener("click", event => {
        if (event.target.closest("[data-address-retry]")) return load();
        if (event.target.closest("[data-address-focus-form]")) return form.elements.addressName.focus();
        const edit = event.target.closest("[data-address-edit]");
        if (edit) return beginEdit(Number(edit.dataset.addressEdit));
        const defaultButton = event.target.closest("[data-address-default]");
        if (defaultButton) return mutateAddress(`/api/front/member/delivery-addresses/${encodeURIComponent(defaultButton.dataset.addressDefault)}/default`, "PUT", "기본 배송지를 변경했습니다.");
        const deleteButton = event.target.closest("[data-address-delete]");
        if (!deleteButton || !confirm("이 배송지를 삭제할까요?")) return;
        mutateAddress(`/api/front/member/delivery-addresses/${encodeURIComponent(deleteButton.dataset.addressDelete)}`, "DELETE", "배송지를 삭제했습니다.");
    });
    addEventListener("keydown", event => {
        if (event.key === "Escape" && addressIdInput.value && !state.saving) reset(true);
    });
    load();
})();
