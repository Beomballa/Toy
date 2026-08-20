(() => {
    "use strict";

    const KEYS = {
        recent: "front-recent-viewed-products",
        wishlist: "front-bookmark-products",
        compare: "front-compare-products",
        hidden: "front-hidden-products"
    };
    const labels = { recent: "최근 본 상품", wishlist: "관심 상품", compare: "비교 상품", hidden: "숨긴 상품" };
    const LIMITS = { recent: 12, wishlist: 24, compare: 3, hidden: 12 };
    const PLACEHOLDER = "/images/product-placeholder.svg";
    const savedView = (() => {
        try { return localStorage.getItem("front-my-view") === "list" ? "list" : "grid"; }
        catch (_) { return "grid"; }
    })();
    const state = { tab: new URLSearchParams(location.search).get("tab") || "recent", keyword: "", stock: "ALL", sort: "RECENT", view: savedView, selected: new Set() };
    if (!KEYS[state.tab]) state.tab = "recent";
    const el = {
        grid: document.getElementById("myProductGrid"), search: document.getElementById("mySearchInput"),
        stock: document.getElementById("myStockFilter"), sort: document.getElementById("mySortSelect"),
        selection: document.getElementById("mySelectionBar"), selectionText: document.getElementById("mySelectionText"),
        toast: document.getElementById("myToast")
    };
    let toastTimer;
    let memberOrderPage = 0;
    let memberOrdersLoaded = false;
    let memberOrderStatus = "ALL";
    let memberOrderSequence = 0;
    let memberReviewPage = 0;
    let memberReviewsLoaded = false;
    let memberReviewSequence = 0;

    function read(tab = state.tab) {
        try {
            const parsed = window.StorefrontState
                ? window.StorefrontState.read(KEYS[tab])
                : JSON.parse(localStorage.getItem(KEYS[tab]) || "[]");
            return normalizeProducts(parsed, LIMITS[tab]);
        } catch (_) { return []; }
    }

    function normalizeProducts(value, limit) {
        if (!Array.isArray(value)) return [];
        const ids = new Set();
        return value.flatMap(item => {
            const id = Number(item?.id);
            if (!Number.isSafeInteger(id) || id <= 0 || ids.has(id)) return [];
            ids.add(id);
            const name = cleanText(item.name || item.headline, 200);
            return [{
                id,
                brand: cleanText(item.brand, 100),
                name,
                headline: cleanText(item.headline, 200),
                model: cleanText(item.model, 100),
                category: cleanText(item.category, 100),
                price: nonNegativeInteger(item.price),
                stock: nonNegativeInteger(item.stock),
                thumbnailUrl: safeImage(item.thumbnailUrl)
            }];
        }).slice(0, limit);
    }

    function cleanText(value, maxLength) {
        return String(value ?? "").trim().replace(/\s+/g, " ").slice(0, maxLength);
    }

    function nonNegativeInteger(value) {
        const number = Number(value ?? 0);
        return Number.isSafeInteger(number) && number >= 0 ? number : 0;
    }

    function safeImage(value) {
        const image = cleanText(value, 500);
        return /^\/(?!\/)/.test(image) || /^https?:\/\//i.test(image) ? image : PLACEHOLDER;
    }
    function write(tab, items) {
        if (window.StorefrontState) {
            if (!window.StorefrontState.write(KEYS[tab], items)) {
                toast("브라우저 저장소를 사용할 수 없습니다.");
                return false;
            }
            return true;
        }
        try {
            localStorage.setItem(KEYS[tab], JSON.stringify(items));
            return true;
        } catch (_) {
            toast("브라우저 저장소를 사용할 수 없습니다.");
            return false;
        }
    }
    function safe(value) {
        return String(value ?? "").replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;").replaceAll("\"","&quot;").replaceAll("'","&#39;");
    }
    function price(value) { return `${Number(value || 0).toLocaleString("ko-KR")}원`; }
    function image(item) { return item.thumbnailUrl || PLACEHOLDER; }
    function toast(message) {
        el.toast.textContent = message; el.toast.hidden = false; clearTimeout(toastTimer);
        toastTimer = setTimeout(() => { el.toast.hidden = true; }, 4200);
    }
    async function loadMemberOrders(reset = false) {
        const target = document.getElementById("memberOrdersList");
        const moreButton = document.getElementById("memberOrdersMoreButton");
        if (reset) {
            memberOrderPage = 0;
            memberOrdersLoaded = false;
            memberOrderSequence += 1;
            target.innerHTML = '<p class="my-order__empty">주문 내역을 불러오는 중입니다.</p>';
        }
        if (memberOrdersLoaded && !reset) return;
        const requestSequence = memberOrderSequence;
        target.setAttribute("aria-busy", "true");
        moreButton.disabled = true;
        try {
            const response = await fetch(`/api/front/member/orders?page=${memberOrderPage}&status=${encodeURIComponent(memberOrderStatus)}`, { headers: { Accept: "application/json" } });
            if (response.status === 401) {
                target.innerHTML = '<p class="my-order__empty">로그인 후 주문 내역을 확인할 수 있습니다. <a href="/front/login">로그인</a></p>';
                moreButton.hidden = true;
                memberOrdersLoaded = true;
                return;
            }
            if (!response.ok) throw new Error("주문 내역을 불러오지 못했습니다.");
            const payload = await response.json();
            if (requestSequence !== memberOrderSequence) return;
            renderMemberOrderSummary(payload.statusSummaries);
            const orders = Array.isArray(payload.items) ? payload.items : [];
            if (memberOrderPage === 0) target.replaceChildren();
            if (!orders.length && memberOrderPage === 0) {
                target.innerHTML = '<p class="my-order__empty">최근 주문이 없습니다. 상품을 둘러보고 첫 주문을 시작해보세요.</p>';
            } else {
                target.insertAdjacentHTML("beforeend", orders.map(order => `<a class="my-order" href="/front/orders/${encodeURIComponent(order.orderNumber)}?member=true"><span class="my-order__copy"><strong>${safe(order.productName)}</strong><span>${safe(order.orderedAt)} · ${Math.max(0, Number(order.itemCount) || 0)}개 상품</span></span><b class="my-order__amount">${price(order.totalAmount)}</b><em class="my-order__status">${safe(order.statusLabel)}</em></a>`).join(""));
            }
            memberOrderPage += 1;
            memberOrdersLoaded = !payload.hasNext;
            moreButton.hidden = memberOrdersLoaded;
        } catch (_) {
            if (requestSequence !== memberOrderSequence) return;
            if (memberOrderPage === 0) target.innerHTML = '<p class="my-order__empty">주문 내역을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.</p>';
            toast("주문 내역을 불러오지 못했습니다.");
        } finally {
            if (requestSequence === memberOrderSequence) target.setAttribute("aria-busy", "false");
            moreButton.disabled = false;
        }
    }
    function renderMemberOrderSummary(summaries) {
        const target = document.getElementById("memberOrdersSummary");
        const rows = Array.isArray(summaries) ? summaries : [];
        const total = rows.reduce((sum, item) => sum + Math.max(0, Number(item?.count) || 0), 0);
        target.innerHTML = [`<button type="button" data-member-order-status="ALL" class="${memberOrderStatus === "ALL" ? "is-active" : ""}">전체 ${total}</button>`, ...rows.map(item => `<button type="button" data-member-order-status="${safe(item.status)}" class="${memberOrderStatus === item.status ? "is-active" : ""}">${safe(item.label)} ${Math.max(0, Number(item.count) || 0)}</button>`)].join("");
    }
    async function loadMemberReviews(reset = false) {
        const target = document.getElementById("memberReviewsList");
        const moreButton = document.getElementById("memberReviewsMoreButton");
        if (reset) {
            memberReviewPage = 0;
            memberReviewsLoaded = false;
            memberReviewSequence += 1;
            target.innerHTML = '<p class="my-review__empty">작성한 후기를 불러오는 중입니다.</p>';
        }
        if (memberReviewsLoaded && !reset) return;
        const requestSequence = memberReviewSequence;
        target.setAttribute("aria-busy", "true");
        moreButton.disabled = true;
        try {
            const response = await fetch(`/api/front/member/reviews?page=${memberReviewPage}`, { headers: { Accept: "application/json" } });
            if (response.status === 401) {
                target.innerHTML = '<p class="my-review__empty">로그인 후 작성한 후기를 확인할 수 있습니다.</p>';
                moreButton.hidden = true;
                memberReviewsLoaded = true;
                return;
            }
            if (!response.ok) throw new Error("작성한 후기를 불러오지 못했습니다.");
            const payload = await response.json();
            if (requestSequence !== memberReviewSequence) return;
            const reviews = Array.isArray(payload.reviews) ? payload.reviews : [];
            if (memberReviewPage === 0) target.replaceChildren();
            if (!reviews.length && memberReviewPage === 0) {
                target.innerHTML = '<p class="my-review__empty">아직 작성한 후기가 없습니다. 배송 완료 주문의 상품에서 후기를 남겨보세요.</p>';
            } else {
                target.insertAdjacentHTML("beforeend", reviews.map(review => `<article class="my-review"><a href="/front/products/${Math.max(0, Number(review.productId) || 0)}#detailReviews"><img src="${safe(safeImage(review.thumbnailUrl))}" alt="${safe(review.productName)}"><span><strong>${safe(review.productBrand)} · ${safe(review.productName)}</strong><em>${"★".repeat(Math.min(5, Math.max(1, Number(review.rating) || 1)))}${"☆".repeat(5 - Math.min(5, Math.max(1, Number(review.rating) || 1)))}</em><p>${safe(review.content)}</p><small>${safe(review.createdDate)}</small></span></a><button type="button" data-delete-review-id="${Math.max(0, Number(review.id) || 0)}">삭제</button></article>`).join(""));
            }
            memberReviewPage += 1;
            memberReviewsLoaded = !payload.hasNext;
            moreButton.hidden = memberReviewsLoaded;
            document.getElementById("memberReviewCount").textContent = String(Math.max(0, Number(payload.totalCount) || 0));
        } catch (_) {
            if (requestSequence !== memberReviewSequence) return;
            if (memberReviewPage === 0) target.innerHTML = '<p class="my-review__empty">작성한 후기를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.</p>';
        } finally {
            if (requestSequence === memberReviewSequence) target.setAttribute("aria-busy", "false");
            moreButton.disabled = false;
        }
    }
    function filtered() {
        const keyword = state.keyword.toLowerCase();
        const items = read().filter(item => {
            const text = [item.brand,item.name,item.headline,item.model].join(" ").toLowerCase();
            const stock = Number(item.stock || 0);
            const stockMatch = state.stock === "ALL" || (state.stock === "AVAILABLE" && stock > 0)
                || (state.stock === "LOW" && stock > 0 && stock <= 20) || (state.stock === "SOLD_OUT" && stock <= 0);
            return (!keyword || text.includes(keyword)) && stockMatch;
        });
        return items.sort((a,b) => state.sort === "PRICE_LOW" ? Number(a.price||0)-Number(b.price||0)
            : state.sort === "PRICE_HIGH" ? Number(b.price||0)-Number(a.price||0)
            : state.sort === "STOCK_LOW" ? Number(a.stock||0)-Number(b.stock||0)
            : state.sort === "NAME" ? String(a.name||a.headline||"").localeCompare(String(b.name||b.headline||""),"ko") : 0);
    }
    function syncCounts() {
        const all = [...new Map(Object.keys(KEYS).flatMap(tab => read(tab)).map(item => [item.id, item])).values()];
        document.getElementById("myTotalCount").textContent = all.length;
        document.getElementById("myAveragePrice").textContent = price(all.length ? Math.round(all.reduce((s,i)=>s+Number(i.price||0),0)/all.length) : 0);
        document.getElementById("myLowStockCount").textContent = all.filter(i=>Number(i.stock||0)<=20).length;
        document.getElementById("myTotalStock").textContent = all.reduce((s,i)=>s+Number(i.stock||0),0).toLocaleString("ko-KR");
        Object.keys(KEYS).forEach(tab => document.getElementById(`my${tab[0].toUpperCase()+tab.slice(1)}Count`).textContent = read(tab).length);
    }
    function syncSelection(items) {
        [...state.selected].forEach(id => { if (!read().some(item=>Number(item.id)===id)) state.selected.delete(id); });
        el.selection.hidden = !state.selected.size;
        el.selectionText.textContent = `${state.selected.size}개 선택 · 합계 ${price(items.filter(i=>state.selected.has(Number(i.id))).reduce((s,i)=>s+Number(i.price||0),0))}`;
        ["myMoveWishlistButton","myMoveCompareButton","myDeleteSelectedButton"].forEach(id => document.getElementById(id).disabled = !state.selected.size);
    }
    function render() {
        const items = filtered();
        document.querySelectorAll("[data-tab]").forEach(button => {
            const active = button.dataset.tab === state.tab;
            button.classList.toggle("is-active", active);
            button.setAttribute("aria-selected", String(active));
            button.tabIndex = active ? 0 : -1;
        });
        document.getElementById("myResultTitle").textContent = `${items.length}개 상품`;
        document.getElementById("myResultDescription").textContent = `${labels[state.tab]}에서 현재 조건에 맞는 상품입니다.`;
        el.grid.classList.toggle("is-list",state.view==="list");
        document.querySelectorAll("[data-view]").forEach(button => button.setAttribute("aria-pressed", String(button.dataset.view === state.view)));
        document.getElementById("mySearchClearButton").hidden = !state.keyword;
        const emptyActions = {
            recent: ["상품을 둘러보면 최근 확인한 상품이 여기에 표시됩니다.", "/front/collections/recommended", "상품 둘러보기"],
            wishlist: ["상품의 관심 버튼을 눌러 나만의 목록을 만들어보세요.", "/front/collections/recommended", "관심 상품 찾기"],
            compare: ["상품을 최대 3개까지 담아 가격과 재고를 비교할 수 있습니다.", "/front/compare", "비교 화면 열기"],
            hidden: ["숨긴 상품이 없습니다. 현재 모든 상품이 탐색 결과에 표시됩니다.", "/front/collections/recommended", "상품 둘러보기"]
        };
        const emptyAction = emptyActions[state.tab];
        el.grid.innerHTML = items.length ? items.map(item => `
          <article class="my-card" data-product-id="${item.id}">
            <label class="my-card__check"><input type="checkbox" data-select-id="${item.id}" ${state.selected.has(Number(item.id))?"checked":""} aria-label="${safe(item.name||item.headline)} 선택"></label>
            <a class="my-card__visual" href="/front/products/${item.id}"><img src="${safe(image(item))}" alt="${safe(item.name||item.headline)}"></a>
            <div class="my-card__copy"><div class="my-card__brand"><span>${safe(item.brand||"NOREN")}</span><span>${Number(item.stock||0)<=0?"품절":Number(item.stock||0)<=20?"재고주의":"재고안정"}</span></div><h2>${safe(item.name||item.headline||`상품 ${item.id}`)}</h2><p>${safe(item.model||item.category||"상품 정보 확인")}</p><div class="my-card__price"><strong>${price(item.price)}</strong><span>재고 ${Number(item.stock||0)}개</span></div></div>
            <div class="my-card__actions"><a href="/front/products/${item.id}">상세 보기</a><button type="button" data-remove-id="${item.id}">목록에서 삭제</button></div>
          </article>`).join("") : `<div class="my-empty"><strong>${labels[state.tab]}이 없습니다.</strong><p>${emptyAction[0]}</p><a href="${emptyAction[1]}">${emptyAction[2]}</a></div>`;
        bindImageFallbacks();
        syncCounts(); syncSelection(items);
        document.getElementById("mySelectAllButton").disabled = !items.length;
        document.getElementById("mySelectAllButton").textContent = items.length && items.every(item => state.selected.has(Number(item.id))) ? "전체 해제" : "전체 선택";
        document.getElementById("myClearTabButton").disabled = !read().length;
        document.getElementById("myExportButton").disabled = !items.length;
        document.getElementById("myCopySummaryButton").disabled = !items.length;
        const url = new URL(location.href);
        url.searchParams.set("tab", state.tab);
        history.replaceState(null, "", `${url.pathname}${url.search}`);
    }
    function bindImageFallbacks() {
        el.grid.querySelectorAll(".my-card__visual img").forEach(img => {
            img.addEventListener("error", () => {
                if (!img.src.endsWith("/images/product-placeholder.svg")) {
                    img.src = "/images/product-placeholder.svg";
                }
            }, { once: true });
        });
    }
    function remove(ids) {
        const set = new Set(ids.map(Number));
        if (write(state.tab,read().filter(item=>!set.has(Number(item.id))))) {
            state.selected.clear();
            render();
        }
    }
    function move(target) {
        const source = read().filter(item=>state.selected.has(Number(item.id)));
        const limit = LIMITS[target];
        const merged = source.concat(read(target)).filter((item,index,all)=>all.findIndex(x=>Number(x.id)===Number(item.id))===index).slice(0,limit);
        if (write(target,merged)) {
            toast(`${source.length}개 상품을 ${labels[target]}에 반영했습니다.`);
            syncCounts();
        }
    }
    function resetAllActivity() {
        const removed = Object.keys(KEYS).map(tab => {
            if (window.StorefrontState) {
                return window.StorefrontState.remove(KEYS[tab]);
            }
            try {
                localStorage.removeItem(KEYS[tab]);
                return true;
            } catch (_) {
                return false;
            }
        }).every(Boolean);
        if (!removed) {
            toast("브라우저 저장소를 초기화하지 못했습니다.");
            return;
        }
        state.selected.clear();
        render();
        toast("모든 쇼핑 활동을 초기화했습니다.");
    }
    function downloadCsv() {
        const rows = [["상품번호","브랜드","상품명","모델","가격","재고"],...filtered().map(i=>[i.id,i.brand,i.name||i.headline,i.model,i.price,i.stock])];
        const csv = "\ufeff"+rows.map(row=>row.map(csvCell).join(",")).join("\n");
        const a=document.createElement("a"); a.href=URL.createObjectURL(new Blob([csv],{type:"text/csv;charset=utf-8"})); a.download=`noren-${state.tab}.csv`; a.click(); URL.revokeObjectURL(a.href);
    }

    function csvCell(value) {
        const text = String(value ?? "");
        const safeText = /^[=+\-@]/.test(text) ? `'${text}` : text;
        return `\"${safeText.replaceAll("\"", "\"\"")}\"`;
    }

    const tabButtons = [...document.querySelectorAll("[data-tab]")];
    tabButtons.forEach((button, index) => {
        button.addEventListener("click", () => { state.tab = button.dataset.tab; state.selected.clear(); render(); });
        button.addEventListener("keydown", event => {
            if (!["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key)) return;
            event.preventDefault();
            const targetIndex = event.key === "Home" ? 0 : event.key === "End" ? tabButtons.length - 1
                : event.key === "ArrowRight" ? (index + 1) % tabButtons.length : (index - 1 + tabButtons.length) % tabButtons.length;
            tabButtons[targetIndex].focus();
            tabButtons[targetIndex].click();
        });
    });
    document.querySelectorAll("[data-view]").forEach(button=>button.addEventListener("click",()=>{
        state.view=button.dataset.view;
        try { localStorage.setItem("front-my-view", state.view); } catch (_) { toast("보기 설정을 저장하지 못했습니다."); }
        render();
    }));
    el.search.addEventListener("input",()=>{state.keyword=cleanText(el.search.value, 100).toLocaleLowerCase("ko-KR");render();});
    el.stock.addEventListener("change",()=>{state.stock=el.stock.value;render();});
    el.sort.addEventListener("change",()=>{state.sort=el.sort.value;render();});
    document.getElementById("mySearchClearButton").addEventListener("click",()=>{el.search.value="";state.keyword="";render();el.search.focus();});
    el.grid.addEventListener("change",event=>{const box=event.target.closest("[data-select-id]");if(!box)return;const id=Number(box.dataset.selectId);box.checked?state.selected.add(id):state.selected.delete(id);syncSelection(filtered());});
    el.grid.addEventListener("click",event=>{const button=event.target.closest("[data-remove-id]");if(button)remove([button.dataset.removeId]);});
    document.getElementById("mySelectAllButton").addEventListener("click",()=>{const ids=filtered().map(i=>Number(i.id));const all=ids.every(id=>state.selected.has(id));ids.forEach(id=>all?state.selected.delete(id):state.selected.add(id));render();});
    document.getElementById("myClearSelectionButton").addEventListener("click",()=>{state.selected.clear();render();});
    document.getElementById("myDeleteSelectedButton").addEventListener("click",()=>{if(confirm("선택 상품을 현재 목록에서 삭제할까요?"))remove([...state.selected]);});
    document.getElementById("myMoveWishlistButton").addEventListener("click",()=>move("wishlist"));
    document.getElementById("myMoveCompareButton").addEventListener("click",()=>move("compare"));
    document.getElementById("myClearTabButton").addEventListener("click",()=>{if(confirm(`${labels[state.tab]} 전체를 비울까요?`) && write(state.tab,[])){state.selected.clear();render();}});
    document.getElementById("myResetAllButton").addEventListener("click",()=>{if(confirm("모든 쇼핑 활동을 초기화할까요?"))resetAllActivity();});
    document.getElementById("myExportButton").addEventListener("click",downloadCsv);
    document.getElementById("myCopySummaryButton").addEventListener("click",async()=>{const text=`${labels[state.tab]} ${filtered().length}개 · 평균가 ${document.getElementById("myAveragePrice").textContent}`;try{await navigator.clipboard.writeText(text);toast("쇼핑 활동 요약을 복사했습니다.");}catch(_){toast("요약을 복사하지 못했습니다.");}});
    document.getElementById("memberOrdersMoreButton").addEventListener("click", () => loadMemberOrders());
    document.getElementById("memberReviewsMoreButton").addEventListener("click", () => loadMemberReviews());
    document.getElementById("memberReviewsList").addEventListener("click", async event => {
        const button = event.target.closest("[data-delete-review-id]");
        if (!button || !confirm("작성한 후기를 삭제할까요?")) return;
        button.disabled = true;
        try {
            const response = await fetch(`/api/front/member/reviews/${encodeURIComponent(button.dataset.deleteReviewId)}`, { method: "DELETE" });
            if (!response.ok) {
                const error = await response.json().catch(() => null);
                throw new Error(error?.message || "후기를 삭제하지 못했습니다.");
            }
            await loadMemberReviews(true);
            toast("작성한 후기를 삭제했습니다.");
        } catch (error) {
            toast(error.message || "후기를 삭제하지 못했습니다. 잠시 후 다시 시도해주세요.");
            button.disabled = false;
        }
    });
    document.getElementById("memberOrderStatusFilter").addEventListener("change", event => {
        memberOrderStatus = event.target.value;
        loadMemberOrders(true);
    });
    document.getElementById("memberOrdersSummary").addEventListener("click", event => {
        const button = event.target.closest("[data-member-order-status]");
        if (!button) return;
        memberOrderStatus = button.dataset.memberOrderStatus;
        document.getElementById("memberOrderStatusFilter").value = memberOrderStatus;
        loadMemberOrders(true);
    });
    document.querySelectorAll("[data-password-toggle]").forEach(button => button.addEventListener("click", () => {
        const input = document.getElementById(button.dataset.passwordToggle);
        if (!input) return;
        const visible = input.type === "text";
        input.type = visible ? "password" : "text";
        button.textContent = visible ? "보기" : "숨기기";
        const label = button.dataset.passwordLabel || "비밀번호";
        button.setAttribute("aria-label", visible ? `${label} 표시` : `${label} 숨기기`);
        input.focus();
    }));
    document.querySelectorAll("#memberPasswordForm input[type='password']").forEach(input => {
        input.addEventListener("keydown", event => {
            const note = input.closest("label")?.querySelector("[data-caps-note]");
            if (note) note.hidden = !event.getModifierState?.("CapsLock");
        });
        input.addEventListener("blur", () => {
            const note = input.closest("label")?.querySelector("[data-caps-note]");
            if (note) note.hidden = true;
        });
    });
    document.getElementById("memberPasswordForm").addEventListener("submit", async event => {
        event.preventDefault();
        const form = event.currentTarget;
        const currentPassword = document.getElementById("memberCurrentPassword").value;
        const newPassword = document.getElementById("memberNewPassword").value;
        const confirmPassword = document.getElementById("memberConfirmPassword").value;
        const status = document.getElementById("memberPasswordStatus");
        const submitButton = document.getElementById("memberPasswordSubmitButton");
        const showPasswordStatus = (message, type = "") => {
            status.textContent = message;
            status.className = type ? `is-${type}` : "";
        };
        if (!currentPassword || !newPassword || !confirmPassword) {
            showPasswordStatus("모든 비밀번호를 입력해 주세요.", "error");
            return;
        }
        if (!/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/.test(newPassword)) {
            showPasswordStatus("새 비밀번호는 영문과 숫자를 포함한 8자 이상이어야 합니다.", "error");
            return;
        }
        if (currentPassword === newPassword) {
            showPasswordStatus("새 비밀번호는 현재 비밀번호와 다르게 입력해 주세요.", "error");
            return;
        }
        if (newPassword !== confirmPassword) {
            showPasswordStatus("새 비밀번호 확인이 일치하지 않습니다.", "error");
            return;
        }
        submitButton.disabled = true;
        showPasswordStatus("비밀번호를 변경하고 있습니다.");
        try {
            const response = await fetch("/api/front/auth/password", {
                method: "POST",
                headers: { "Content-Type": "application/json", Accept: "application/json" },
                body: JSON.stringify({ currentPassword, newPassword })
            });
            const payload = await response.json().catch(() => null);
            if (!response.ok) throw new Error(payload?.message || "비밀번호를 변경하지 못했습니다.");
            form.reset();
            showPasswordStatus("비밀번호를 변경했습니다. 현재 기기에서 계속 이용할 수 있습니다.", "success");
            toast("비밀번호를 변경했습니다.");
        } catch (error) {
            showPasswordStatus(error.message || "비밀번호를 변경하지 못했습니다. 잠시 후 다시 시도해 주세요.", "error");
        } finally {
            submitButton.disabled = false;
        }
    });
    addEventListener("storage",event=>{if(Object.values(KEYS).includes(event.key))render();});
    document.addEventListener("storefront:state-ready", render);
    addEventListener("keydown",event=>{
        const editable = event.target instanceof HTMLInputElement || event.target instanceof HTMLTextAreaElement || event.target instanceof HTMLSelectElement;
        if(event.key==="/" && !editable){event.preventDefault();el.search.focus();}
        if(event.key==="Escape" && state.keyword){el.search.value="";state.keyword="";render();}
    });
    render();
    loadMemberOrders();
    loadMemberReviews();
})();
