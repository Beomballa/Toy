(() => {
    "use strict";

    const loginTab = document.querySelector("#memberLoginTab");
    const signUpTab = document.querySelector("#memberSignUpTab");
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

    function switchMode(mode, focus = true) {
        const signUp = mode === "SIGNUP";
        loginTab.setAttribute("aria-selected", String(!signUp));
        signUpTab.setAttribute("aria-selected", String(signUp));
        loginForm.hidden = signUp;
        signUpForm.hidden = !signUp;
        title.textContent = signUp ? "처음 오셨나요?" : "다시 만나 반갑습니다.";
        description.textContent = signUp ? "기본 정보로 NOREN 탐색을 시작하세요." : "가입한 이메일로 로그인하세요.";
        clearStatus();
        if (focus) {
            (signUp ? signUpForm : loginForm).querySelector("input")?.focus();
        }
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

    function setPending(form, value) {
        pending = value;
        form.querySelectorAll("input, button").forEach((element) => {
            element.disabled = value;
        });
        form.setAttribute("aria-busy", String(value));
    }

    async function request(path, payload) {
        const response = await fetch(path, {
            method: payload === undefined ? "GET" : "POST",
            headers: payload === undefined ? undefined : { "Content-Type": "application/json" },
            body: payload === undefined ? undefined : JSON.stringify(payload)
        });
        const data = await response.json().catch(() => null);
        if (!response.ok) {
            throw new Error(data?.message || "요청을 처리하지 못했습니다.");
        }
        return data;
    }

    function renderMember(member) {
        const authenticated = member?.authenticated === true;
        anonymousPanel.hidden = authenticated;
        signedPanel.hidden = !authenticated;
        if (!authenticated) return;
        signedName.textContent = `${member.nickname || member.name || "회원"}님`;
        signedEmail.textContent = member.email || "";
        signedName.focus?.();
    }

    function safeNextPath() {
        const next = new URLSearchParams(window.location.search).get("next");
        return next && next.startsWith("/front") && !next.startsWith("//") ? next : "/front/my";
    }

    loginForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (pending || !loginForm.reportValidity()) return;
        clearStatus();
        const form = new FormData(loginForm);
        setPending(loginForm, true);
        try {
            await request("/api/front/auth/login", {
                email: String(form.get("email") || "").trim(),
                password: String(form.get("password") || "")
            });
            window.location.assign(safeNextPath());
        } catch (error) {
            showStatus(error.message);
        } finally {
            setPending(loginForm, false);
        }
    });

    signUpForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (pending || !signUpForm.reportValidity()) return;
        clearStatus();
        const form = new FormData(signUpForm);
        const password = String(form.get("password") || "");
        if (password !== String(form.get("passwordConfirm") || "")) {
            showStatus("비밀번호 확인이 일치하지 않습니다.");
            signUpForm.elements.passwordConfirm.focus();
            return;
        }
        if (!/[A-Za-z]/.test(password) || !/\d/.test(password)) {
            showStatus("비밀번호는 영문과 숫자를 모두 포함해야 합니다.");
            signUpForm.elements.password.focus();
            return;
        }
        setPending(signUpForm, true);
        try {
            await request("/api/front/auth/signup", {
                email: String(form.get("email") || "").trim(),
                password,
                name: String(form.get("name") || "").trim(),
                nickname: String(form.get("nickname") || "").trim()
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

    loginTab.addEventListener("click", () => switchMode("LOGIN"));
    signUpTab.addEventListener("click", () => switchMode("SIGNUP"));
    request("/api/front/auth/me").then(renderMember).catch(() => renderMember(null));
})();
