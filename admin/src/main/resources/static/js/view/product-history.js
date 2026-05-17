const ProductHistoryPage = {
    initialized: false,
    state: {
        page: 0,
        size: 20
    },

    init() {
        if (this.initialized) return;
        this.initialized = true;
        this.bindEvents();
        this.readStateFromUrl();
        this.loadHistory();
    },

    bindEvents() {
        document.getElementById('btnSearchHistory')?.addEventListener('click', () => {
            this.state.page = 0;
            this.loadHistory();
        });
        document.getElementById('historyPageSize')?.addEventListener('change', () => {
            this.state.page = 0;
            this.state.size = Number(document.getElementById('historyPageSize')?.value || 20);
            this.loadHistory();
        });
        document.getElementById('historyKeyword')?.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                this.state.page = 0;
                this.loadHistory();
            }
        });
        document.getElementById('historyActorKeyword')?.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                this.state.page = 0;
                this.loadHistory();
            }
        });
        document.getElementById('historyOrderType')?.addEventListener('change', () => {
            this.state.page = 0;
            this.loadHistory();
        });
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        document.getElementById('historyProductNo').value = params.get('productNo') || '';
        document.getElementById('historyActionType').value = params.get('actionType') || '';
        document.getElementById('historyStartDate').value = params.get('startDate') || '';
        document.getElementById('historyEndDate').value = params.get('endDate') || '';
        document.getElementById('historyKeyword').value = params.get('keyword') || '';
        document.getElementById('historyActorKeyword').value = params.get('actorKeyword') || '';
        document.getElementById('historyOrderType').value = params.get('orderType') || 'latest';
        this.state.page = Number(params.get('page') || 0);
        this.state.size = Number(params.get('size') || 20);
        document.getElementById('historyPageSize').value = String(this.state.size);
    },

    buildParams() {
        const params = new URLSearchParams();
        const productNo = document.getElementById('historyProductNo').value.trim();
        const actionType = document.getElementById('historyActionType').value;
        const startDate = document.getElementById('historyStartDate').value;
        const endDate = document.getElementById('historyEndDate').value;
        const keyword = CommonJS.normalizeOptionalText(document.getElementById('historyKeyword').value);
        const actorKeyword = CommonJS.normalizeOptionalText(document.getElementById('historyActorKeyword').value);
        const orderType = document.getElementById('historyOrderType').value || 'latest';

        if (productNo) params.set('productNo', productNo);
        if (actionType) params.set('actionType', actionType);
        if (startDate) params.set('startDate', startDate);
        if (endDate) params.set('endDate', endDate);
        if (keyword) params.set('keyword', keyword);
        if (actorKeyword) params.set('actorKeyword', actorKeyword);
        if (orderType && orderType !== 'latest') params.set('orderType', orderType);
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
            this.renderPagination(data);
        } catch (error) {
            this.renderError(error.message);
        }
    },

    renderList(items) {
        const tbody = document.getElementById('productHistoryBody');
        const returnTo = encodeURIComponent(window.location.pathname + window.location.search);
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
                    ${item.relatedProductNo ? `
                        <div class="small">
                            <a class="text-decoration-none" href="/admin/products/get?no=${item.relatedProductNo}&returnTo=${returnTo}">
                                ${item.relatedProductLabel} #${item.relatedProductNo}
                            </a>
                        </div>
                    ` : ''}
                    ${item.activityLogPath ? `
                        <div class="small">
                            <a class="text-decoration-none" href="${item.activityLogPath}">
                                ${item.activityLogLabel || '활동 로그 보기'}
                            </a>
                        </div>
                    ` : ''}
                    <div class="text-muted small">상태 ${item.statusSnapshot || '-'} · 옵션 ${item.optionCount}개 · 재고 ${item.totalStock}개</div>
                </td>
                <td>${item.actorName}${item.actorNo ? ` <span class="text-muted small">(#${item.actorNo})</span>` : ''}</td>
                <td class="text-muted small">${item.totalStock} / ${item.optionCount}</td>
                <td class="text-end pe-4 small text-muted">${item.actionDtm}</td>
            </tr>
        `).join('');
    },

    renderMeta(data) {
        this.setMetaText(data.pageInfoLabel || `${data.rangeStart}-${data.rangeEnd} / ${data.totalElements}건`);
    },

    renderPagination(data) {
        const pagination = document.getElementById('historyPagination');
        if (!pagination) {
            return;
        }

        if (!data.totalPages) {
            pagination.innerHTML = '';
            return;
        }

        let html = '';
        for (let i = 0; i < data.totalPages; i += 1) {
            html += `
                <li class="page-item ${i === data.currentPage ? 'active' : ''}">
                    <a class="page-link" href="javascript:void(0);" onclick="ProductHistoryPage.goPage(${i})">${i + 1}</a>
                </li>
            `;
        }
        pagination.innerHTML = html;
    },

    renderError(message) {
        document.getElementById('productHistoryBody').innerHTML =
            `<tr><td colspan="7" class="text-center py-5 text-danger">${message}</td></tr>`;
        this.setMetaText('이력 조회 실패');
        document.getElementById('historyPagination').innerHTML = '';
    },

    setMetaText(message) {
        document.getElementById('historyMetaText').textContent = message;
    },

    goPage(page) {
        this.state.page = page;
        this.loadHistory();
    }
};

document.addEventListener('DOMContentLoaded', () => ProductHistoryPage.init());
