const OrderList = {
    maxDateRangeDays: 92,
    maxKeywordLength: 50,
    state: null,
    operationPolicy: null,

    init() {
        this.state = this.readStateFromUrl();
        this.syncFilterFields();
        this.bindEvents();
        this.applyOperationPolicy();
        window.addEventListener(CommonJS.systemSettingsEventName, (event) => this.applyOperationPolicy(event.detail));
        this.getList();
    },

    bindEvents() {
        // 필터 적용 버튼
        document.getElementById('btnFilter')?.addEventListener('click', () => {
            this.state.page = 0;
            this.captureFilterState();
            if (!this.validateDateRange()) {
                return;
            }
            this.pushState();
            this.getList();
        });

        document.getElementById('btnResetFilter')?.addEventListener('click', () => {
            this.resetFilters();
            this.pushState();
            this.getList();
        });

        document.getElementById('pageSize')?.addEventListener('change', () => {
            this.state.page = 0;
            this.state.size = Number(document.getElementById('pageSize')?.value || 10);
            this.pushState();
            this.getList();
        });

        document.getElementById('btnExportOrders')?.addEventListener('click', () => {
            if (this.operationPolicy && CommonJS.isOrderExportBlocked(this.operationPolicy)) {
                CommonJS.alert('현재 설정에서 주문 CSV 내보내기 기능이 비활성화되어 있습니다.', '알림', 'warning');
                return;
            }
            this.captureFilterState();
            if (!this.validateDateRange()) {
                return;
            }
            window.location.href = `/api/admin/orders/export?${this.buildStateParams().toString()}`;
        });

        document.querySelectorAll('[data-date-preset]').forEach((button) => {
            button.addEventListener('click', () => {
                this.applyDatePreset(Number(button.dataset.datePreset || 0));
                this.pushState();
                this.getList();
            });
        });

        // 검색 조건은 URL에 남겨서 새로고침/뒤로가기 때도 같은 문맥을 유지한다.
        document.getElementById('searchKeyword')?.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.state.page = 0;
                this.captureFilterState();
                if (!this.validateDateRange()) {
                    return;
                }
                this.pushState();
                this.getList();
            }
        });

        window.addEventListener('popstate', () => {
            this.state = this.readStateFromUrl();
            this.syncFilterFields();
            this.getList();
        });
    },

    async applyOperationPolicy(settings = null) {
        const exportButton = document.getElementById('btnExportOrders');
        try {
            this.operationPolicy = settings || await CommonJS.fetchSystemSettings();
            CommonJS.setButtonDisabled(
                exportButton,
                CommonJS.isOrderExportBlocked(this.operationPolicy),
                '현재 설정에서 주문 CSV 내보내기 기능이 비활성화되어 있습니다.'
            );
        } catch (error) {
            console.error('운영 설정 로드 실패:', error);
        }
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
            if (!res.ok) {
                throw new Error(await CommonJS.extractErrorMessage(res, '데이터를 불러오는 중 오류가 발생했습니다.'));
            }

            const data = await res.json();
            this.renderList(data.orders);
            this.renderPagination(data);
        } catch (err) {
            console.error('주문 목록 로드 실패:', err);
            CommonJS.alert(err.message || '데이터를 불러오는 중 오류가 발생했습니다.', '오류', 'error');
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
            const statusMeta = CommonJS.getOrderStatusMeta(item.statusCode);

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
                    <td><span class="badge ${statusMeta.badgeClass}">${item.statusDesc}</span></td>
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

        const totalCountEl = document.getElementById('totalElementsCount');
        if (totalCountEl) {
            totalCountEl.textContent = `전체 ${Number(totalElements || 0).toLocaleString()}건`;
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
        const rawKeyword = document.getElementById('searchKeyword')?.value || '';
        this.state.searchKeyword = rawKeyword.trim().replace(/\s+/g, ' ');
    },

    syncFilterFields() {
        const statusEl = document.getElementById('orderStatus');
        const startDateEl = document.getElementById('startDate');
        const endDateEl = document.getElementById('endDate');
        const searchKeywordEl = document.getElementById('searchKeyword');
        const pageSizeEl = document.getElementById('pageSize');

        if (statusEl) statusEl.value = this.state.status;
        if (startDateEl) startDateEl.value = this.state.startDate;
        if (endDateEl) endDateEl.value = this.state.endDate;
        if (searchKeywordEl) searchKeywordEl.value = this.state.searchKeyword;
        if (pageSizeEl) pageSizeEl.value = String(this.state.size);
    },

    resetFilters() {
        this.state.page = 0;
        this.state.status = '';
        this.state.startDate = '';
        this.state.endDate = '';
        this.state.searchKeyword = '';
        this.syncFilterFields();
    },

    applyDatePreset(days) {
        const today = new Date();
        const endDate = this.formatDate(today);
        const startDate = new Date(today);

        startDate.setDate(today.getDate() - Math.max(days - 1, 0));

        this.state.page = 0;
        this.state.startDate = this.formatDate(startDate);
        this.state.endDate = endDate;
        this.syncFilterFields();
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
        return this.buildStateParams().toString();
    },

    validateDateRange() {
        if (this.state.searchKeyword && this.state.searchKeyword.length > this.maxKeywordLength) {
            CommonJS.alert(`검색어는 ${this.maxKeywordLength}자 이내로 입력할 수 있습니다.`, '알림', 'warning');
            return false;
        }

        if (!this.state.startDate || !this.state.endDate) {
            return true;
        }

        if (this.state.startDate > this.state.endDate) {
            CommonJS.alert('시작일은 종료일보다 늦을 수 없습니다.', '알림', 'warning');
            return false;
        }

        const startDate = new Date(`${this.state.startDate}T00:00:00`);
        const endDate = new Date(`${this.state.endDate}T00:00:00`);
        const diffDays = Math.floor((endDate - startDate) / (1000 * 60 * 60 * 24));

        if (diffDays > this.maxDateRangeDays) {
            CommonJS.alert(`조회 기간은 ${this.maxDateRangeDays + 1}일 이내로만 설정할 수 있습니다.`, '알림', 'warning');
            return false;
        }

        return true;
    },

    formatDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    },

    readStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        return {
            page: Number(params.get('page') || 0),
            size: Number(params.get('size') || 10),
            status: params.get('status') || '',
            startDate: params.get('startDate') || '',
            endDate: params.get('endDate') || '',
            searchKeyword: params.get('searchKeyword') || ''
        };
    },

    buildStateParams() {
        // URL state를 한 곳에서만 조립해야 필터 항목이 늘어나도 popstate와 상세 복귀가 같이 유지됩니다.
        const params = new URLSearchParams({
            page: this.state.page,
            size: this.state.size
        });

        if (this.state.status) params.set('status', this.state.status);
        if (this.state.startDate) params.set('startDate', this.state.startDate);
        if (this.state.endDate) params.set('endDate', this.state.endDate);
        if (this.state.searchKeyword) params.set('searchKeyword', this.state.searchKeyword);

        return params;
    }
};

document.addEventListener('DOMContentLoaded', () => OrderList.init());
