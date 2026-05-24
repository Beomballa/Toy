package com.section.common.system.repository;

import com.section.common.system.dto.AdminOperationTaskListQuery;
import com.section.common.system.dto.AdminOperationTaskListResDto;
import com.section.common.system.dto.AdminOperationTaskSummaryDto;
import com.section.common.system.dto.AdminOperationTaskWorkloadDto;
import com.section.common.system.entity.AdminOperationTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface CustomAdminOperationTaskRepository {
    Page<AdminOperationTaskListResDto> getTaskList(AdminOperationTaskListQuery query, Pageable pageable);
    AdminOperationTaskSummaryDto getTaskSummary(AdminOperationTaskListQuery query, LocalDate today);
    List<AdminOperationTask> getDashboardTasks(LocalDate today, int limit);
    List<AdminOperationTaskWorkloadDto> getDashboardTaskWorkloads(LocalDate today, int limit);
}
