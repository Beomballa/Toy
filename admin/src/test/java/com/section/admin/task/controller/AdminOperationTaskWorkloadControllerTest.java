package com.section.admin.task.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminOperationTaskWorkloadControllerTest {

    private final AdminOperationTaskController controller = new AdminOperationTaskController();

    @Test
    @DisplayName("운영 작업 워크로드 화면은 전용 뷰를 반환한다")
    void taskWorkloadsReturnsView() {
        assertEquals("views/task-workload-list", controller.taskWorkloads());
    }
}
