const OrderList = {
    state: {
        page: 0,
        size: 10
    },

    init() {
        this.bindEvents();
        this.getList();
    },

    bindEvents() {
        // 필터 적용 버튼
        document.getElementById('btnFilter')?.addEventListener('click', () => {
            this.state.page = 0;
            this.getList();
        });

        // 엔터키 검색
        document.getElementById('searchKeyword')?.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.state.page = 0;
                this.getList();
            }
        });
    },

    async getList() {
        const params = new URLSearchParams({
            page: this.state.page,
            size: this.state.size,
            status: document.getElementById('orderStatus').value,
            startDate: document.getElementById('startDate').value,
            endDate: document.getElementById('endDate').value,
            searchKeyword: document.getElementById('searchKeyword').value
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
                        <button type="button" class="btn btn-sm btn-outline-secondary" onclick="location.href='/admin/orders/get?no=${item.orderNo}'">상세보기</button>
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
        this.getList();
    }
};

document.addEventListener('DOMContentLoaded', () => OrderList.init());
