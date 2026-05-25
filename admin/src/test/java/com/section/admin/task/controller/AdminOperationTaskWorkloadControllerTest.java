package com.section.admin.task.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminOperationTaskWorkloadControllerTest {

    private final AdminOperationTaskController controller = new AdminOperationTaskController();

    @Test
    @DisplayName("운영 작업 워크로드 화면은 전용 뷰를 반환한다")
    void taskWorkloadsReturnsView() {
        assertEquals("views/task-workload-list", controller.taskWorkloads());
    }

    @Test
    @DisplayName("운영 작업 워크로드 상세 화면은 부트스트랩 값을 모델에 담는다")
    void taskWorkloadDetailReturnsView() {
        Model model = new ExtendedModelMap();

        String viewName = controller.taskWorkloadDetail(7L, "/admin/settings/tasks/workloads", model);

        assertEquals("views/task-workload-get", viewName);
        assertEquals(7L, model.getAttribute("adminNo"));
        assertEquals("/admin/settings/tasks/workloads", model.getAttribute("returnTo"));
    }

    @Test
    @DisplayName("운영 작업 워크로드 상세 화면은 복귀 경로가 없으면 기본값을 사용한다")
    void taskWorkloadDetailUsesDefaultReturnTo() {
        Model model = new ExtendedModelMap();

        controller.taskWorkloadDetail(8L, null, model);

        assertEquals("/admin/settings/tasks/workloads", model.getAttribute("returnTo"));
    }
}
