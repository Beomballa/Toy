const OrderList = {
    state: {
        page: Number(new URLSearchParams(window.location.search).get('page') || 0),
        size: Number(new URLSearchParams(window.location.search).get('size') || 10),
        status: new URLSearchParams(window.location.search).get('status') || '',
        startDate: new URLSearchParams(window.location.search).get('startDate') || '',
        endDate: new URLSearchParams(window.location.search).get('endDate') || '',
        searchKeyword: new URLSearchParams(window.location.search).get('searchKeyword') || ''
    },

    init() {
        this.syncFilterFields();
        this.bindEvents();
        this.getList();
    },

    bindEvents() {
        // 필터 적용 버튼
        document.getElementById('btnFilter')?.addEventListener('click', () => {
            this.state.page = 0;
            this.captureFilterState();
            this.pushState();
            this.getList();
        });

        // 검색 조건은 URL에 남겨서 새로고침/뒤로가기 때도 같은 문맥을 유지한다.
        document.getElementById('searchKeyword')?.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.state.page = 0;
                this.captureFilterState();
                this.pushState();
                this.getList();
            }
        });

        window.addEventListener('popstate', () => {
            const params = new URLSearchParams(window.location.search);
            this.state.page = Number(params.get('page') || 0);
            this.state.size = Number(params.get('size') || 10);
            this.state.status = params.get('status') || '';
            this.state.startDate = params.get('startDate') || '';
            this.state.endDate = params.get('endDate') || '';
            this.state.searchKeyword = params.get('searchKeyword') || '';
            this.syncFilterFields();
            this.getList();
        });
    },

    async getList() {
        this.captureFilterState();
        const params = new URLSearchParams({
            page: this.state.page,
            size: this.state.size,
            status: this.state.status,
            startDate: this.state.startDate,
            endDate: this.state.endDate,
            searchKeyword: this.state.searchKeyword
        });

        try {
            const res = await fetch(`/api/admin/orders/list?${params}`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            const data = await res.json();
            this.renderList(data.orders);
            this.renderPagination(data);
        } catch (err) {
            console.error('주문 목록 로드 실패:', err);
            CommonJS.alert('데이터를 불러오는 중 오류가 발생했습니다.', '오류', 'error');
        }
    },

    renderList(items) {
        const tbody = document.getElementById('orderListTableBody');
        if (!tbody) return;

        if (!items || items.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center py-5 text-muted">주문 내역이 없습니다.</td></tr>';
            return;
        }

        tbody.innerHTML = items.map(item => {
            let statusClass = 'bg-secondary';
            switch(item.statusCode) {
                case 'PAID': statusClass = 'badge-paid'; break;
                case 'SHIPPED': statusClass = 'badge-shipped'; break;
                case 'DELIVERED': statusClass = 'badge-delivered'; break;
                case 'CANCELLED': statusClass = 'badge-cancelled'; break;
                case 'ORDERED': statusClass = 'badge-ordered'; break;
            }

            return `
                <tr>
                    <td class="ps-4"><span class="order-id">${item.orderNum}</span></td>
                    <td>${item.orderDt}</td>
                    <td>
                        <div class="buyer-info">
                            <div class="fw-bold">${item.buyerName}</div>
                            <div class="text-muted small">${item.buyerPhone}</div>
                        </div>
                    </td>
                    <td>${item.productSummary}</td>
                    <td><strong>${item.totalAmount}</strong></td>
                    <td><span class="badge ${statusClass}">${item.statusDesc}</span></td>
                    <td class="text-end pe-4">
                        <button type="button" class="btn btn-sm btn-outline-secondary" onclick="location.href='${this.buildDetailUrl(item.orderNo)}'">상세보기</button>
                    </td>
                </tr>
            `;
        }).join('');
    },

    renderPagination(data) {
        const { totalPages, currentPage: curr, totalElements } = data;
        const pagination = document.getElementById('pagination');
        if (!pagination) return;

        let html = '';
        for (let i = 0; i < totalPages; i++) {
            html += `
                <li class="page-item ${i === curr ? 'active' : ''}">
                    <a class="page-link" href="javascript:void(0);" onclick="OrderList.goPage(${i})">${i + 1}</a>
                </li>`;
        }
        pagination.innerHTML = html;

        const infoEl = document.getElementById('pageInfoText');
        if (infoEl) {
            infoEl.textContent = `Showing page ${curr + 1} of ${totalPages} (Total ${totalElements.toLocaleString()} entries)`;
        }
    },

    goPage(page) {
        this.state.page = page;
        this.pushState();
        this.getList();
    },

    captureFilterState() {
        this.state.status = document.getElementById('orderStatus')?.value || '';
        this.state.startDate = document.getElementById('startDate')?.value || '';
        this.state.endDate = document.getElementById('endDate')?.value || '';
        this.state.searchKeyword = document.getElementById('searchKeyword')?.value || '';
    },

    syncFilterFields() {
        const statusEl = document.getElementById('orderStatus');
        const startDateEl = document.getElementById('startDate');
        const endDateEl = document.getElementById('endDate');
        const searchKeywordEl = document.getElementById('searchKeyword');

        if (statusEl) statusEl.value = this.state.status;
        if (startDateEl) startDateEl.value = this.state.startDate;
        if (endDateEl) endDateEl.value = this.state.endDate;
        if (searchKeywordEl) searchKeywordEl.value = this.state.searchKeyword;
    },

    pushState() {
        const newUrl = `${window.location.pathname}?${this.buildQueryString()}`;
        window.history.pushState({ path: newUrl }, '', newUrl);
    },

    buildDetailUrl(orderNo) {
        const returnTo = encodeURIComponent(`${window.location.pathname}?${this.buildQueryString()}`);
        return `/admin/orders/get?no=${orderNo}&returnTo=${returnTo}`;
    },

    buildQueryString() {
        const params = new URLSearchParams({
            page: this.state.page,
            size: this.state.size
        });

        if (this.state.status) params.set('status', this.state.status);
        if (this.state.startDate) params.set('startDate', this.state.startDate);
        if (this.state.endDate) params.set('endDate', this.state.endDate);
        if (this.state.searchKeyword) params.set('searchKeyword', this.state.searchKeyword);

        return params.toString();
    }
};

document.addEventListener('DOMContentLoaded', () => OrderList.init());
