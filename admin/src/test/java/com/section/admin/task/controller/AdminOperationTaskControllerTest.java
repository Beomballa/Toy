package com.section.admin.task.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminOperationTaskControllerTest {

    private final AdminOperationTaskController controller = new AdminOperationTaskController();

    @Test
    @DisplayName("운영 작업 화면은 전용 뷰를 반환한다")
    void taskListReturnsView() {
        assertEquals("views/task-list", controller.taskList());
    }

    @Test
    @DisplayName("운영 작업 이력 화면은 전용 뷰를 반환한다")
    void taskHistoryReturnsView() {
        assertEquals("views/task-history", controller.taskHistory());
    }

    @Test
    @DisplayName("담당자 워크로드 상세 화면은 관리자 번호와 기본 복귀 경로를 모델에 담는다")
    void taskWorkloadDetailReturnsViewWithDefaultReturnPath() {
        Model model = new ExtendedModelMap();

        String viewName = controller.taskWorkloadDetail(7L, null, model);

        assertEquals("views/task-workload-get", viewName);
        assertEquals(7L, model.getAttribute("adminNo"));
        assertEquals("/admin/settings/tasks/workloads", model.getAttribute("returnTo"));
    }

    @Test
    @DisplayName("운영 작업 상세 화면은 작업 번호와 복귀 경로를 모델에 담는다")
    void taskDetailReturnsViewWithModel() {
        Model model = new ExtendedModelMap();

        String viewName = controller.taskDetail(5L, "/admin/settings/tasks?page=1", model);

        assertEquals("views/task-get", viewName);
        assertEquals(5L, model.getAttribute("taskNo"));
        assertEquals("/admin/settings/tasks?page=1", model.getAttribute("returnTo"));
    }
}
