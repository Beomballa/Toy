package com.section.admin.task.service;

import com.section.admin.task.req.AdminOperationTaskWorkloadListRequest;
import com.section.admin.task.res.AdminOperationTaskWorkloadDetailResponse;
import com.section.admin.task.res.AdminOperationTaskWorkloadListResponse;
import com.section.admin.log.res.AdminLogListResponse;
import com.section.admin.log.service.AdminLogService;
import com.section.common.system.dto.AdminOperationTaskWorkloadCommentSummaryDto;
import com.section.common.system.dto.AdminOperationTaskWorkloadDto;
import com.section.common.system.dto.AdminOperationTaskWorkloadListQuery;
import com.section.common.system.dto.AdminOperationTaskWorkloadSummaryDto;
import com.section.common.system.dto.AdminOperationTaskListResDto;
import com.section.common.system.repository.AdminOperationTaskCommentRepository;
import com.section.common.system.repository.AdminOperationTaskRepository;
import com.section.common.system.repository.AdminUserRepository;
import com.section.common.system.entity.AdminUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOperationTaskWorkloadServiceTest {

    @Mock
    private AdminOperationTaskRepository adminOperationTaskRepository;
    @Mock
    private AdminOperationTaskCommentRepository adminOperationTaskCommentRepository;
    @Mock
    private AdminUserRepository adminUserRepository;
    @Mock
    private AdminLogService adminLogService;

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
        latestComment.setCrtDtm(LocalDateTime.of(2026, 5, 25, 10, 0));
        when(adminOperationTaskCommentRepository.getLatestCommentsByAssigneeAdminNos(any()))
                .thenReturn(List.of(latestComment));

        AdminOperationTaskWorkloadListResponse response = adminOperationTaskWorkloadService.getWorkloadList(request);

        assertEquals(1, response.items().size());
        assertEquals("운영자", response.items().get(0).assigneeAdminName());
        assertEquals("정산 점검", response.items().get(0).latestCommentTaskTitle());
        assertEquals("우선 확인 필요", response.items().get(0).latestCommentContent());
        assertEquals("/admin/settings/tasks?assigneeAdminNo=7&keyword=%EC%A0%95%EC%82%B0&priority=HIGH", response.items().get(0).targetPath());
        assertEquals("/admin/settings/tasks?assigneeAdminNo=7&keyword=%EC%A0%95%EC%82%B0&priority=HIGH&overdueOnly=Y", response.items().get(0).overduePath());
        assertEquals(6L, response.summary().assignedTaskCount());
        assertEquals("검색 결과 1명", response.resultMeta().resultLabel());
    }

    @Test
    @DisplayName("운영 작업 워크로드 CSV 내보내기는 필터 결과를 파일 바이트로 만든다")
    void exportWorkloadListCsvReturnsCsvBytes() {
        AdminOperationTaskWorkloadListRequest request = new AdminOperationTaskWorkloadListRequest();
        request.setKeyword("정산");
        request.setPriority("HIGH");
        request.setOverdueOnly("Y");

        when(adminOperationTaskRepository.getTaskWorkloadPage(any(AdminOperationTaskWorkloadListQuery.class), eq(PageRequest.of(0, 1000)), any(LocalDate.class)))
                .thenReturn(new PageImpl<>(
                        List.of(new AdminOperationTaskWorkloadDto(7L, "운영자", 6L, 2L, 3L, 1L)),
                        PageRequest.of(0, 1000),
                        1
                ));
        when(adminOperationTaskRepository.getTaskWorkloadSummary(any(AdminOperationTaskWorkloadListQuery.class), any(LocalDate.class)))
                .thenReturn(new AdminOperationTaskWorkloadSummaryDto(1L, 6L, 1L, 0L));
        AdminOperationTaskWorkloadCommentSummaryDto latestComment = new AdminOperationTaskWorkloadCommentSummaryDto();
        latestComment.setAssigneeAdminNo(7L);
        latestComment.setTaskNo(11L);
        latestComment.setTaskTitle("정산 점검");
        latestComment.setAdminNo(3L);
        latestComment.setAdminName("관리자");
        latestComment.setContent("우선 확인 필요");
        latestComment.setCrtDtm(LocalDateTime.of(2026, 5, 25, 10, 0));
        when(adminOperationTaskCommentRepository.getLatestCommentsByAssigneeAdminNos(List.of(7L)))
                .thenReturn(List.of(latestComment));

        String csv = new String(adminOperationTaskWorkloadService.exportWorkloadListCsv(request), StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"조회조건\",\"기한 초과 우선 · 진행중 우선 · 검색=정산 · 우선순위=높음 · 기한 초과만\""));
        assertTrue(csv.contains("\"운영자\""));
        assertTrue(csv.contains("\"정산 점검\""));
        assertTrue(csv.contains("\"우선 확인 필요\""));
    }

    @Test
    @DisplayName("운영 작업 워크로드 상세는 최근 작업 메모 활동을 함께 반환한다")
    void getWorkloadDetailReturnsRecentContexts() {
        when(adminUserRepository.findById(7L))
                .thenReturn(java.util.Optional.of(AdminUser.builder().adminNo(7L).name("운영자").loginId("ops").password("pw").build()));
        when(adminOperationTaskRepository.getTaskWorkload(7L, LocalDate.now()))
                .thenReturn(new AdminOperationTaskWorkloadDto(7L, "운영자", 6L, 2L, 3L, 1L));

        AdminOperationTaskListResDto task = new AdminOperationTaskListResDto();
        task.setTaskNo(11L);
        task.setTitle("정산 점검");
        task.setStatus("IN_PROGRESS");
        task.setPriority("HIGH");
        task.setAssigneeAdminNo(7L);
        task.setAssigneeAdminName("운영자");
        task.setDueDate(LocalDate.of(2026, 5, 26));
        when(adminOperationTaskRepository.getRecentTasksByAssigneeAdminNo(7L, 5)).thenReturn(List.of(task));
        when(adminOperationTaskRepository.getOverdueTasksByAssigneeAdminNo(7L, LocalDate.now(), 5)).thenReturn(List.of(task));

        AdminOperationTaskWorkloadCommentSummaryDto comment = new AdminOperationTaskWorkloadCommentSummaryDto();
        comment.setAssigneeAdminNo(7L);
        comment.setTaskNo(11L);
        comment.setTaskTitle("정산 점검");
        comment.setAdminNo(3L);
        comment.setAdminName("관리자");
        comment.setContent("우선 확인 필요");
        comment.setCrtDtm(LocalDateTime.of(2026, 5, 25, 11, 0));
        when(adminOperationTaskCommentRepository.getRecentCommentsByAssigneeAdminNo(7L, 5)).thenReturn(List.of(comment));

        AdminLogListResponse.Item logItem = new AdminLogListResponse.Item(
                15L, 7L, "운영자", "TASK_UPDATE", 11L, "운영 작업 #11", "/admin/settings/tasks/get?no=11", "127.0.0.1", "2026-05-25 12:00"
        );
        when(adminLogService.getLogList(any(), any(PageRequest.class)))
                .thenReturn(new AdminLogListResponse(
                        List.of(logItem),
                        1L, 1, 0, 5, 1L, 1L, "1-1 / 1건 · 1페이지",
                        new AdminLogListResponse.Summary(1, 1, 0, 1, 0, 1),
                        new AdminLogListResponse.AppliedQuery(7L, "TASK_", null, null, null),
                        new AdminLogListResponse.ResultMeta("검색 결과 1건", "1-1 / 1건 · 1페이지", 2, "1-1 · 작업=TASK_")
                ));

        AdminOperationTaskWorkloadDetailResponse response = adminOperationTaskWorkloadService.getWorkloadDetail(7L, "/admin/settings/tasks/workloads?keyword=%EC%A0%95%EC%82%B0");

        assertEquals("운영자", response.assigneeAdminName());
        assertEquals(
                "/admin/settings/tasks?assigneeAdminNo=7&returnTo=%2Fadmin%2Fsettings%2Ftasks%2Fworkloads%2Fget%3FadminNo%3D7%26returnTo%3D%252Fadmin%252Fsettings%252Ftasks%252Fworkloads%253Fkeyword%253D%2525EC%2525A0%252595%2525EC%252582%2525B0&source=task-workload-detail",
                response.targetPath()
        );
        assertEquals(
                "/admin/settings/tasks?assigneeAdminNo=7&status=TODO&returnTo=%2Fadmin%2Fsettings%2Ftasks%2Fworkloads%2Fget%3FadminNo%3D7%26returnTo%3D%252Fadmin%252Fsettings%252Ftasks%252Fworkloads%253Fkeyword%253D%2525EC%2525A0%252595%2525EC%252582%2525B0&source=task-workload-detail",
                response.todoPath()
        );
        assertEquals(1, response.recentTasks().size());
        assertEquals("정산 점검", response.recentTasks().get(0).title());
        assertEquals(
                "/admin/settings/tasks/get?no=11&returnTo=%2Fadmin%2Fsettings%2Ftasks%2Fworkloads%2Fget%3FadminNo%3D7%26returnTo%3D%252Fadmin%252Fsettings%252Ftasks%252Fworkloads%253Fkeyword%253D%2525EC%2525A0%252595%2525EC%252582%2525B0",
                response.recentTasks().get(0).taskPath()
        );
        assertEquals(1, response.overdueTasks().size());
        assertEquals(1, response.recentComments().size());
        assertEquals("우선 확인 필요", response.recentComments().get(0).content());
        assertEquals(1, response.recentHistories().size());
        assertEquals("작업 수정", response.recentHistories().get(0).actionLabel());
    }

    @Test
    @DisplayName("운영 작업 워크로드 상세는 댓글 수정과 일괄 삭제 라벨을 노출한다")
    void getWorkloadDetailMapsAdditionalHistoryLabels() {
        when(adminUserRepository.findById(7L))
                .thenReturn(java.util.Optional.of(AdminUser.builder().adminNo(7L).name("운영자").loginId("ops").password("pw").build()));
        when(adminOperationTaskRepository.getTaskWorkload(7L, LocalDate.now()))
                .thenReturn(new AdminOperationTaskWorkloadDto(7L, "운영자", 0L, 0L, 0L, 0L));
        when(adminOperationTaskRepository.getRecentTasksByAssigneeAdminNo(7L, 5)).thenReturn(List.of());
        when(adminOperationTaskRepository.getOverdueTasksByAssigneeAdminNo(7L, LocalDate.now(), 5)).thenReturn(List.of());
        when(adminOperationTaskCommentRepository.getRecentCommentsByAssigneeAdminNo(7L, 5)).thenReturn(List.of());
        when(adminLogService.getLogList(any(), any(PageRequest.class)))
                .thenReturn(new AdminLogListResponse(
                        List.of(
                                new AdminLogListResponse.Item(15L, 7L, "운영자", "TASK_COMMENT_UPDATE", 11L, "운영 작업 #11", "/admin/settings/tasks/get?no=11", "127.0.0.1", "2026-05-25 12:00"),
                                new AdminLogListResponse.Item(16L, 7L, "운영자", "TASK_BULK_DELETE", 12L, "운영 작업 #12", "/admin/settings/tasks/get?no=12", "127.0.0.1", "2026-05-25 12:10")
                        ),
                        2L, 1, 0, 5, 1L, 2L, "1-2 / 2건 · 1페이지",
                        new AdminLogListResponse.Summary(2, 2, 0, 2, 0, 2),
                        new AdminLogListResponse.AppliedQuery(7L, "TASK_", null, null, null),
                        new AdminLogListResponse.ResultMeta("검색 결과 2건", "1-2 / 2건 · 1페이지", 2, "1-2 · 작업=TASK_")
                ));

        AdminOperationTaskWorkloadDetailResponse response = adminOperationTaskWorkloadService.getWorkloadDetail(7L, "/admin/settings/tasks/workloads");

        assertEquals("댓글 수정", response.recentHistories().get(0).actionLabel());
        assertEquals("일괄 삭제", response.recentHistories().get(1).actionLabel());
    }
}
