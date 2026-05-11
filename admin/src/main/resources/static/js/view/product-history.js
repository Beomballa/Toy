const ProductHistoryPage = {
    state: {
        page: 0,
        size: 20
    },

    init() {
        this.bindEvents();
        this.readStateFromUrl();
        this.loadHistory();
    },

    bindEvents() {
        document.getElementById('btnSearchHistory')?.addEventListener('click', () => {
            this.state.page = 0;
            this.loadHistory();
        });
        document.getElementById('historyKeyword')?.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                this.state.page = 0;
                this.loadHistory();
            }
        });
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        document.getElementById('historyProductNo').value = params.get('productNo') || '';
        document.getElementById('historyActionType').value = params.get('actionType') || '';
        document.getElementById('historyStartDate').value = params.get('startDate') || '';
        document.getElementById('historyEndDate').value = params.get('endDate') || '';
        document.getElementById('historyKeyword').value = params.get('keyword') || '';
        this.state.page = Number(params.get('page') || 0);
        this.state.size = Number(params.get('size') || 20);
    },

    buildParams() {
        const params = new URLSearchParams();
        const productNo = document.getElementById('historyProductNo').value.trim();
        const actionType = document.getElementById('historyActionType').value;
        const startDate = document.getElementById('historyStartDate').value;
        const endDate = document.getElementById('historyEndDate').value;
        const keyword = CommonJS.normalizeOptionalText(document.getElementById('historyKeyword').value);

        if (productNo) params.set('productNo', productNo);
        if (actionType) params.set('actionType', actionType);
        if (startDate) params.set('startDate', startDate);
        if (endDate) params.set('endDate', endDate);
        if (keyword) params.set('keyword', keyword);
        params.set('page', String(this.state.page));
        params.set('size', String(this.state.size));
        return params;
    },

    async loadHistory() {
        const params = this.buildParams();
        history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
        this.setMetaText('데이터를 불러오는 중입니다...');

        try {
            const response = await fetch(`/api/admin/product/history/list?${params.toString()}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '변경 이력을 불러오지 못했습니다.'));
            }
            const data = await response.json();
            this.renderList(data.items || []);
            this.renderMeta(data);
        } catch (error) {
            this.renderError(error.message);
        }
    },

    renderList(items) {
        const tbody = document.getElementById('productHistoryBody');
        if (!items.length) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center py-5 text-muted">조회된 변경 이력이 없습니다.</td></tr>';
            return;
        }

        tbody.innerHTML = items.map(item => `
            <tr>
                <td class="ps-4 text-muted small">${item.historyNo}</td>
                <td><a class="text-decoration-none fw-bold" href="/admin/products/get?no=${item.productNo}&returnTo=${encodeURIComponent(window.location.pathname + window.location.search)}">${item.productNo}</a></td>
                <td><span class="badge bg-dark">${item.actionLabel}</span></td>
                <td>
                    <div class="fw-semibold">${item.summary}</div>
                    <div class="text-muted small">상태 ${item.statusSnapshot || '-'} · 옵션 ${item.optionCount}개 · 재고 ${item.totalStock}개</div>
                </td>
                <td>${item.actorName}${item.actorNo ? ` <span class="text-muted small">(#${item.actorNo})</span>` : ''}</td>
                <td class="text-muted small">${item.totalStock} / ${item.optionCount}</td>
                <td class="text-end pe-4 small text-muted">${item.actionDtm}</td>
            </tr>
        `).join('');
    },

    renderMeta(data) {
        this.setMetaText(`${data.rangeStart}-${data.rangeEnd} / ${data.totalElements}건 · ${data.totalPages}페이지`);
    },

    renderError(message) {
        document.getElementById('productHistoryBody').innerHTML =
            `<tr><td colspan="7" class="text-center py-5 text-danger">${message}</td></tr>`;
        this.setMetaText('이력 조회 실패');
    },

    setMetaText(message) {
        document.getElementById('historyMetaText').textContent = message;
    }
};

document.addEventListener('DOMContentLoaded', () => ProductHistoryPage.init());
