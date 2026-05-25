package com.section.admin.task.service;

import com.section.admin.task.req.AdminOperationTaskWorkloadListRequest;
import com.section.admin.task.res.AdminOperationTaskWorkloadListResponse;
import com.section.common.system.dto.AdminOperationTaskWorkloadCommentSummaryDto;
import com.section.common.system.dto.AdminOperationTaskWorkloadDto;
import com.section.common.system.dto.AdminOperationTaskWorkloadListQuery;
import com.section.common.system.dto.AdminOperationTaskWorkloadSummaryDto;
import com.section.common.system.repository.AdminOperationTaskCommentRepository;
import com.section.common.system.repository.AdminOperationTaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOperationTaskWorkloadServiceTest {

    @Mock
    private AdminOperationTaskRepository adminOperationTaskRepository;
    @Mock
    private AdminOperationTaskCommentRepository adminOperationTaskCommentRepository;

    @InjectMocks
    private AdminOperationTaskWorkloadService adminOperationTaskWorkloadService;

    @Test
    @DisplayName("운영 작업 워크로드 목록은 페이지 응답과 요약 메타를 반환한다")
    void getWorkloadListReturnsPagedResponse() {
        AdminOperationTaskWorkloadListRequest request = new AdminOperationTaskWorkloadListRequest();
        request.setKeyword("정산");
        request.setPriority("HIGH");

        when(adminOperationTaskRepository.getTaskWorkloadPage(any(AdminOperationTaskWorkloadListQuery.class), any(PageRequest.class), any(LocalDate.class)))
                .thenReturn(new PageImpl<>(
                        List.of(new AdminOperationTaskWorkloadDto(7L, "운영자", 6L, 2L, 3L, 1L)),
                        PageRequest.of(0, 10),
                        1
                ));
        when(adminOperationTaskRepository.getTaskWorkloadSummary(any(AdminOperationTaskWorkloadListQuery.class), any(LocalDate.class)))
                .thenReturn(new AdminOperationTaskWorkloadSummaryDto(1L, 6L, 1L, 2L));
        AdminOperationTaskWorkloadCommentSummaryDto latestComment = new AdminOperationTaskWorkloadCommentSummaryDto();
        latestComment.setAssigneeAdminNo(7L);
        latestComment.setTaskNo(11L);
        latestComment.setTaskTitle("정산 점검");
        latestComment.setAdminNo(3L);
        latestComment.setAdminName("관리자");
        latestComment.setContent("우선 확인 필요");
        latestComment.setCrtDtm(java.time.LocalDateTime.of(2026, 5, 25, 10, 0));
        when(adminOperationTaskCommentRepository.getLatestCommentsByAssigneeAdminNos(any()))
                .thenReturn(List.of(latestComment));

        AdminOperationTaskWorkloadListResponse response = adminOperationTaskWorkloadService.getWorkloadList(request);

        assertEquals(1, response.items().size());
        assertEquals("운영자", response.items().get(0).assigneeAdminName());
        assertEquals("정산 점검", response.items().get(0).latestCommentTaskTitle());
        assertEquals("우선 확인 필요", response.items().get(0).latestCommentContent());
        assertEquals(6L, response.summary().assignedTaskCount());
        assertEquals("검색 결과 1명", response.resultMeta().resultLabel());
    }
}
