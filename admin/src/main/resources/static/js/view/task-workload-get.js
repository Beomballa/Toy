const TaskWorkloadDetail = {
    initialized: false,
    bootstrap: window.taskWorkloadDetailBootstrap || {},

    init() {
        if (this.initialized) return;
        this.initialized = true;
        this.loadDetail();
    },

    async loadDetail() {
        try {
            const adminNo = Number(this.bootstrap.adminNo || 0);
            if (!adminNo) {
                throw new Error('담당자 번호가 올바르지 않습니다.');
            }
            const response = await fetch(`/api/admin/settings/tasks/workloads/${adminNo}`);
            if (!response.ok) {
                throw new Error(await CommonJS.extractErrorMessage(response, '담당자 워크로드 상세를 불러오지 못했습니다.'));
            }
            const data = await response.json();
            this.renderDetail(data);
        } catch (error) {
            document.getElementById('taskWorkloadDetailTitle').textContent = error.message;
            document.getElementById('workloadDetailMetaText').textContent = '상세 메타 확인 불가';
            document.getElementById('workloadRecentTasksBody').innerHTML = `<div class="text-danger small">${this.escapeHtml(error.message)}</div>`;
            document.getElementById('workloadRecentCommentsBody').innerHTML = '<div class="text-muted small">최근 메모를 확인할 수 없습니다.</div>';
            document.getElementById('workloadRecentHistoriesBody').innerHTML = '<div class="text-muted small">최근 활동을 확인할 수 없습니다.</div>';
        }
    },

    renderDetail(data) {
        document.getElementById('taskWorkloadDetailTitle').textContent = `${data.assigneeAdminName} 워크로드 상세`;
        document.getElementById('workloadDetailAssigneeName').textContent = `${data.assigneeAdminName} · 관리자 #${data.assigneeAdminNo}`;
        document.getElementById('workloadDetailMetaText').textContent = `최근 작업 ${Number(data.summary?.totalCount || 0).toLocaleString()}건 기준`;

        document.getElementById('workloadDetailTotalCount').textContent = Number(data.summary?.totalCount || 0).toLocaleString();
        document.getElementById('workloadDetailTodoCount').textContent = Number(data.summary?.todoCount || 0).toLocaleString();
        document.getElementById('workloadDetailInProgressCount').textContent = Number(data.summary?.inProgressCount || 0).toLocaleString();
        document.getElementById('workloadDetailOverdueCount').textContent = Number(data.summary?.overdueCount || 0).toLocaleString();

        document.getElementById('workloadDetailTaskListButton').href = data.targetPath || '#';
        document.getElementById('workloadDetailOverdueButton').href = data.overduePath || '#';
        document.getElementById('workloadDetailLogButton').href = data.activityLogPath || '#';

        this.renderRecentTasks(data.recentTasks || []);
        this.renderRecentComments(data.recentComments || []);
        this.renderRecentHistories(data.recentHistories || []);
    },

    renderRecentTasks(items) {
        const body = document.getElementById('workloadRecentTasksBody');
        if (!body) return;
        if (!items.length) {
            body.innerHTML = '<div class="text-muted small">최근 작업이 없습니다.</div>';
            return;
        }
        body.innerHTML = items.map((item) => `
            <div class="border rounded-3 p-3 mb-3">
                <div class="fw-bold mb-1"><a class="text-decoration-none" href="${item.taskPath}">${this.escapeHtml(item.title)}</a></div>
                <div class="small text-muted">${this.escapeHtml(item.statusLabel)} · ${this.escapeHtml(item.priorityLabel)} · ${this.escapeHtml(item.dueState)}</div>
                <div class="mt-2">
                    <a class="btn btn-sm btn-outline-secondary" href="${item.historyPath}">이력</a>
                </div>
            </div>
        `).join('');
    },

    renderRecentComments(items) {
        const body = document.getElementById('workloadRecentCommentsBody');
        if (!body) return;
        if (!items.length) {
            body.innerHTML = '<div class="text-muted small">최근 메모가 없습니다.</div>';
            return;
        }
        body.innerHTML = items.map((item) => `
            <div class="border rounded-3 p-3 mb-3">
                <div class="fw-bold mb-1"><a class="text-decoration-none" href="${item.taskPath}">${this.escapeHtml(item.taskTitle)}</a></div>
                <div class="small text-muted">${this.escapeHtml(item.adminName)} · ${this.escapeHtml(item.commentDtm)}</div>
                <div class="small text-dark mt-2">${this.escapeHtml(item.content)}</div>
            </div>
        `).join('');
    },

    renderRecentHistories(items) {
        const body = document.getElementById('workloadRecentHistoriesBody');
        if (!body) return;
        if (!items.length) {
            body.innerHTML = '<div class="text-muted small">최근 활동이 없습니다.</div>';
            return;
        }
        body.innerHTML = items.map((item) => `
            <div class="border rounded-3 p-3 mb-3">
                <div class="fw-bold mb-1">${this.escapeHtml(item.actionLabel)}</div>
                <div class="small text-muted">${this.escapeHtml(item.adminName)} · ${this.escapeHtml(item.actionDtm)}</div>
                <div class="small text-dark mt-2">
                    ${item.taskPath ? `<a class="text-decoration-none" href="${item.taskPath}">${this.escapeHtml(item.taskLabel || '관련 작업 보기')}</a>` : this.escapeHtml(item.taskLabel || '-')}
                </div>
            </div>
        `).join('');
    },

    escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }
};
