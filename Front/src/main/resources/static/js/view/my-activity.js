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
    const state = { tab: new URLSearchParams(location.search).get("tab") || "recent", keyword: "", stock: "ALL", sort: "RECENT", view: "grid", selected: new Set() };
    if (!KEYS[state.tab]) state.tab = "recent";
    const el = {
        grid: document.getElementById("myProductGrid"), search: document.getElementById("mySearchInput"),
        stock: document.getElementById("myStockFilter"), sort: document.getElementById("mySortSelect"),
        selection: document.getElementById("mySelectionBar"), selectionText: document.getElementById("mySelectionText"),
        toast: document.getElementById("myToast")
    };
    let toastTimer;

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
        toastTimer = setTimeout(() => { el.toast.hidden = true; }, 2400);
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
        const all = Object.keys(KEYS).flatMap(tab => read(tab));
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
        document.querySelectorAll("[data-tab]").forEach(button => { button.classList.toggle("is-active",button.dataset.tab===state.tab); button.setAttribute("aria-current",button.dataset.tab===state.tab?"page":"false"); });
        document.getElementById("myResultTitle").textContent = `${items.length}개 상품`;
        document.getElementById("myResultDescription").textContent = `${labels[state.tab]}에서 현재 조건에 맞는 상품입니다.`;
        el.grid.classList.toggle("is-list",state.view==="list");
        el.grid.innerHTML = items.length ? items.map(item => `
          <article class="my-card" data-product-id="${item.id}">
            <label class="my-card__check"><input type="checkbox" data-select-id="${item.id}" ${state.selected.has(Number(item.id))?"checked":""} aria-label="${safe(item.name||item.headline)} 선택"></label>
            <a class="my-card__visual" href="/front/products/${item.id}"><img src="${safe(image(item))}" alt="${safe(item.name||item.headline)}"></a>
            <div class="my-card__copy"><div class="my-card__brand"><span>${safe(item.brand||"NOREN")}</span><span>${Number(item.stock||0)<=0?"품절":Number(item.stock||0)<=20?"재고주의":"재고안정"}</span></div><h2>${safe(item.name||item.headline||`상품 ${item.id}`)}</h2><p>${safe(item.model||item.category||"상품 정보 확인")}</p><div class="my-card__price"><strong>${price(item.price)}</strong><span>재고 ${Number(item.stock||0)}개</span></div></div>
            <div class="my-card__actions"><a href="/front/products/${item.id}">상세 보기</a><button type="button" data-remove-id="${item.id}">목록에서 삭제</button></div>
          </article>`).join("") : `<div class="my-empty"><strong>${labels[state.tab]}이 없습니다.</strong><p>검색 조건을 바꾸거나 상품을 둘러보고 활동을 시작해보세요.</p><a href="/front#catalog">상품 둘러보기</a></div>`;
        bindImageFallbacks();
        syncCounts(); syncSelection(items);
        document.getElementById("mySelectAllButton").disabled = !items.length;
        document.getElementById("myClearTabButton").disabled = !read().length;
        history.replaceState(null,"",`/front/my?tab=${state.tab}`);
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
        const a=document.createElement("a"); a.href=URL.createObjectURL(new Blob([csv],{type:"text/csv;charset=utf-8"})); a.download=`grade-stock-${state.tab}.csv`; a.click(); URL.revokeObjectURL(a.href);
    }

    function csvCell(value) {
        const text = String(value ?? "");
        const safeText = /^[=+\-@]/.test(text) ? `'${text}` : text;
        return `\"${safeText.replaceAll("\"", "\"\"")}\"`;
    }

    document.querySelectorAll("[data-tab]").forEach(button=>button.addEventListener("click",()=>{state.tab=button.dataset.tab;state.selected.clear();render();}));
    document.querySelectorAll("[data-view]").forEach(button=>button.addEventListener("click",()=>{state.view=button.dataset.view;document.querySelectorAll("[data-view]").forEach(b=>b.setAttribute("aria-pressed",String(b===button)));render();}));
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
    addEventListener("storage",event=>{if(Object.values(KEYS).includes(event.key))render();});
    addEventListener("keydown",event=>{if(event.key==="/"&&document.activeElement!==el.search){event.preventDefault();el.search.focus();}if(event.key==="Escape"&&state.keyword){el.search.value="";state.keyword="";render();}});
    render();
})();
