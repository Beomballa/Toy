(() => {
    "use strict";

    const loginTab = document.querySelector("#memberLoginTab");
    const signUpTab = document.querySelector("#memberSignUpTab");
    const tabs = [loginTab, signUpTab];
    const loginForm = document.querySelector("#memberLoginForm");
    const signUpForm = document.querySelector("#memberSignUpForm");
    const status = document.querySelector("#memberAuthStatus");
    const title = document.querySelector("#memberAuthTitle");
    const description = document.querySelector("#memberAuthDescription");
    const anonymousPanel = document.querySelector("#memberAnonymousPanel");
    const signedPanel = document.querySelector("#memberSignedPanel");
    const signedName = document.querySelector("#memberSignedName");
    const signedEmail = document.querySelector("#memberSignedEmail");
    const logoutButton = document.querySelector("#memberLogoutButton");
    let pending = false;

    function switchMode(mode, focus = true, updateUrl = true) {
        const signUp = mode === "SIGNUP";
        tabs.forEach((tab, index) => {
            const selected = signUp ? index === 1 : index === 0;
            tab.setAttribute("aria-selected", String(selected));
            tab.tabIndex = selected ? 0 : -1;
        });
        loginForm.hidden = signUp;
        signUpForm.hidden = !signUp;
        title.textContent = signUp ? "처음 오셨나요?" : "다시 만나 반갑습니다.";
        description.textContent = signUp ? "기본 정보로 NOREN 탐색을 시작하세요." : "가입한 이메일로 로그인하세요.";
        clearStatus();
        clearFieldErrors(signUp ? loginForm : signUpForm);
        if (updateUrl) syncModeQuery(signUp);
        if (focus) (signUp ? signUpForm : loginForm).querySelector("input")?.focus();
    }

    function syncModeQuery(signUp) {
        const url = new URL(window.location.href);
        if (signUp) url.searchParams.set("mode", "signup");
        else url.searchParams.delete("mode");
        history.replaceState(null, "", `${url.pathname}${url.search}${url.hash}`);
    }

    function showStatus(message, success = false) {
        status.textContent = message;
        status.classList.toggle("is-success", success);
        status.hidden = false;
    }

    function clearStatus() {
        status.hidden = true;
        status.textContent = "";
        status.classList.remove("is-success");
    }

    function fieldError(form, name, message) {
        const input = form.elements[name];
        const error = form.querySelector(`[data-field-error="${name}"]`);
        if (!input || !error) return;
        input.setAttribute("aria-invalid", "true");
        error.textContent = message;
        error.hidden = false;
    }

    function clearFieldError(form, name) {
        const input = form.elements[name];
        const error = form.querySelector(`[data-field-error="${name}"]`);
        input?.removeAttribute("aria-invalid");
        if (error) {
            error.textContent = "";
            error.hidden = true;
        }
    }

    function clearFieldErrors(form) {
        form.querySelectorAll("[data-field-error]").forEach((error) => {
            error.textContent = "";
            error.hidden = true;
        });
        form.querySelectorAll("[aria-invalid]").forEach((input) => input.removeAttribute("aria-invalid"));
    }

    function validateForm(form) {
        clearFieldErrors(form);
        for (const input of form.querySelectorAll("input[required]")) {
            if (input.checkValidity()) continue;
            const message = input.validity.valueMissing
                ? `${input.labels?.[0]?.textContent.trim() || "필수 항목"}을 입력해 주세요.`
                : input.validity.typeMismatch ? "올바른 이메일 형식으로 입력해 주세요."
                    : `최소 ${input.minLength}자 이상 입력해 주세요.`;
            fieldError(form, input.name, message);
            input.focus();
            return false;
        }
        return true;
    }

    function setPending(form, value) {
        pending = value;
        form.querySelectorAll("input, button").forEach((element) => { element.disabled = value; });
        const submit = form.querySelector("[type=submit]");
        if (submit) {
            if (value) {
                submit.dataset.idleLabel = submit.textContent;
                submit.textContent = submit.dataset.pendingLabel;
            } else if (submit.dataset.idleLabel) {
                submit.textContent = submit.dataset.idleLabel;
            }
        }
        form.setAttribute("aria-busy", String(value));
    }

    async function request(path, payload) {
        const response = await fetch(path, {
            method: payload === undefined ? "GET" : "POST",
            headers: payload === undefined ? { Accept: "application/json" } : { Accept: "application/json", "Content-Type": "application/json" },
            body: payload === undefined ? undefined : JSON.stringify(payload)
        });
        const data = await response.json().catch(() => null);
        if (!response.ok) throw new Error(data?.message || "요청을 처리하지 못했습니다.");
        return data;
    }

    function renderMember(member) {
        const authenticated = member?.authenticated === true;
        anonymousPanel.hidden = authenticated;
        signedPanel.hidden = !authenticated;
        if (!authenticated) return;
        signedName.textContent = `${member.nickname || member.name || "회원"}님`;
        signedEmail.textContent = member.email || "";
        signedName.setAttribute("tabindex", "-1");
        signedName.focus();
    }

    function safeNextPath() {
        const next = new URLSearchParams(window.location.search).get("next");
        return next && next.startsWith("/front") && !next.startsWith("//") ? next : "/front/my";
    }

    function normalizeEmail(value) {
        return String(value || "").trim().toLocaleLowerCase("en-US");
    }

    function updatePasswordRules() {
        const password = signUpForm.elements.password.value;
        const checks = { length: password.length >= 8, letter: /[A-Za-z]/.test(password), number: /\d/.test(password) };
        Object.entries(checks).forEach(([rule, valid]) => {
            document.querySelector(`[data-password-rule="${rule}"]`)?.classList.toggle("is-valid", valid);
        });
        clearFieldError(signUpForm, "password");
        validatePasswordConfirm(false);
    }

    function validatePasswordConfirm(showEmpty = true) {
        const confirmation = signUpForm.elements.passwordConfirm;
        clearFieldError(signUpForm, "passwordConfirm");
        if (!confirmation.value && !showEmpty) return true;
        if (confirmation.value !== signUpForm.elements.password.value) {
            fieldError(signUpForm, "passwordConfirm", "비밀번호가 일치하지 않습니다.");
            return false;
        }
        return true;
    }

    function togglePassword(button) {
        const input = document.getElementById(button.dataset.passwordToggle);
        if (!input) return;
        const visible = input.type === "text";
        input.type = visible ? "password" : "text";
        button.textContent = visible ? "보기" : "숨기기";
        button.setAttribute("aria-label", visible ? "비밀번호 표시" : "비밀번호 숨기기");
        input.focus();
    }

    function handleCapsLock(event) {
        const note = event.currentTarget.closest(".member-auth__field")?.querySelector("[data-caps-note]");
        if (note) note.hidden = !event.getModifierState?.("CapsLock");
    }

    loginForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (pending || !validateForm(loginForm)) return;
        clearStatus();
        const form = new FormData(loginForm);
        setPending(loginForm, true);
        try {
            await request("/api/front/auth/login", { email: normalizeEmail(form.get("email")), password: String(form.get("password") || "") });
            window.location.assign(safeNextPath());
        } catch (error) {
            showStatus(error.message);
            loginForm.elements.password.focus();
        } finally {
            setPending(loginForm, false);
        }
    });

    signUpForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (pending || !validateForm(signUpForm)) return;
        clearStatus();
        const form = new FormData(signUpForm);
        const password = String(form.get("password") || "");
        if (!/[A-Za-z]/.test(password) || !/\d/.test(password)) {
            fieldError(signUpForm, "password", "비밀번호는 영문과 숫자를 모두 포함해야 합니다.");
            signUpForm.elements.password.focus();
            return;
        }
        if (!validatePasswordConfirm()) {
            signUpForm.elements.passwordConfirm.focus();
            return;
        }
        setPending(signUpForm, true);
        try {
            await request("/api/front/auth/signup", {
                email: normalizeEmail(form.get("email")), password,
                name: String(form.get("name") || "").trim(), nickname: String(form.get("nickname") || "").trim()
            });
            window.location.assign(safeNextPath());
        } catch (error) {
            showStatus(error.message);
        } finally {
            setPending(signUpForm, false);
        }
    });

    logoutButton.addEventListener("click", async () => {
        if (pending) return;
        pending = true;
        logoutButton.disabled = true;
        try {
            await request("/api/front/auth/logout", {});
            window.StorefrontState?.forgetSession();
            renderMember(null);
            switchMode("LOGIN", false);
            showStatus("로그아웃되었습니다.", true);
        } catch (error) {
            showStatus(error.message);
        } finally {
            pending = false;
            logoutButton.disabled = false;
        }
    });

    tabs.forEach((tab, index) => {
        tab.addEventListener("click", () => switchMode(index === 0 ? "LOGIN" : "SIGNUP"));
        tab.addEventListener("keydown", (event) => {
            if (!["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key)) return;
            event.preventDefault();
            const targetIndex = event.key === "Home" ? 0 : event.key === "End" ? tabs.length - 1
                : event.key === "ArrowRight" ? (index + 1) % tabs.length : (index - 1 + tabs.length) % tabs.length;
            tabs[targetIndex].focus();
            switchMode(targetIndex === 0 ? "LOGIN" : "SIGNUP", false);
        });
    });
    document.querySelectorAll("[data-password-toggle]").forEach((button) => button.addEventListener("click", () => togglePassword(button)));
    document.querySelectorAll("input[type=password]").forEach((input) => {
        input.addEventListener("keydown", handleCapsLock);
        input.addEventListener("keyup", handleCapsLock);
        input.addEventListener("blur", () => {
            const note = input.closest(".member-auth__field")?.querySelector("[data-caps-note]");
            if (note) note.hidden = true;
        });
    });
    document.querySelectorAll(".member-auth__form input").forEach((input) => input.addEventListener("input", () => clearFieldError(input.form, input.name)));
    signUpForm.elements.password.addEventListener("input", updatePasswordRules);
    signUpForm.elements.passwordConfirm.addEventListener("input", () => validatePasswordConfirm(false));

    switchMode(new URLSearchParams(window.location.search).get("mode")?.toLowerCase() === "signup" ? "SIGNUP" : "LOGIN", false, false);
    request("/api/front/auth/me").then(renderMember).catch(() => renderMember(null));
})();
