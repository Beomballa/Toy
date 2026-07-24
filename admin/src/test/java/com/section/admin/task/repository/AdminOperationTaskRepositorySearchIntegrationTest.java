package com.section.admin.task.repository;

import com.section.admin.AdminToyApplication;
import com.section.common.system.dto.AdminOperationTaskListQuery;
import com.section.common.system.dto.AdminOperationTaskListResDto;
import com.section.common.system.dto.AdminOperationTaskWorkloadListQuery;
import com.section.common.system.dto.AdminOperationTaskWorkloadDto;
import com.section.common.system.entity.AdminOperationTask;
import com.section.common.system.entity.AdminOperationTaskComment;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminOperationTaskCommentRepository;
import com.section.common.system.repository.AdminOperationTaskRepository;
import com.section.common.system.repository.AdminUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = AdminToyApplication.class)
@ActiveProfiles("local")
@Transactional
class AdminOperationTaskRepositorySearchIntegrationTest {

    @Autowired
    private AdminOperationTaskRepository adminOperationTaskRepository;

    @Autowired
    private AdminOperationTaskCommentRepository adminOperationTaskCommentRepository;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Test
    @DisplayName("운영 작업은 출처 유형과 ID로 연결 작업을 조회한다")
    void findsTaskBySource() {
        AdminOperationTask saved = adminOperationTaskRepository.saveAndFlush(AdminOperationTask.builder()
                .title("콘텐츠 개선")
                .status("TODO")
                .priority("HIGH")
                .isPinned("Y")
                .sourceType("CONTENT_PERFORMANCE")
                .sourceId(31001L)
                .build());

        AdminOperationTask found = adminOperationTaskRepository
                .findBySourceTypeAndSourceId("CONTENT_PERFORMANCE", 31001L)
                .orElseThrow();

        assertEquals(saved.getTaskNo(), found.getTaskNo());
    }

    @Test
    @DisplayName("같은 출처의 운영 작업은 DB 유니크 제약으로 중복 저장되지 않는다")
    void rejectsDuplicatedTaskSource() {
        adminOperationTaskRepository.saveAndFlush(AdminOperationTask.builder()
                .title("첫 번째 콘텐츠 개선")
                .status("TODO")
                .priority("HIGH")
                .isPinned("Y")
                .sourceType("CONTENT_PERFORMANCE")
                .sourceId(31002L)
                .build());

        assertThrows(DataIntegrityViolationException.class, () ->
                adminOperationTaskRepository.saveAndFlush(AdminOperationTask.builder()
                        .title("중복 콘텐츠 개선")
                        .status("TODO")
                        .priority("HIGH")
                        .isPinned("Y")
                        .sourceType("CONTENT_PERFORMANCE")
                        .sourceId(31002L)
                        .build())
        );
    }

    @Test
    @DisplayName("운영 작업 목록 검색은 공백으로 구분된 여러 키워드를 모두 만족하는 작업만 찾는다")
    void getTaskListMatchesAllKeywordTokens() {
        AdminUser assignee = adminUserRepository.save(AdminUser.builder()
                .loginId("ops-token")
                .password("pw")
                .name("정산담당")
                .build());
        AdminOperationTask matchedTask = adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("정산 점검")
                .description("송장 지연 확인")
                .status("TODO")
                .priority("HIGH")
                .assigneeAdminNo(assignee.getAdminNo())
                .isPinned("N")
                .build());
        adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("정산 점검")
                .description("일반 확인")
                .status("TODO")
                .priority("LOW")
                .assigneeAdminNo(assignee.getAdminNo())
                .isPinned("N")
                .build());
        adminOperationTaskCommentRepository.save(comment(matchedTask.getTaskNo(), "지연 사유 메모", assignee.getAdminNo()));

        Page<?> matched = adminOperationTaskRepository.getTaskList(
                new AdminOperationTaskListQuery("정산 지연", null, null, null, null, null, null, null, null, null, null, "PINNED_DUE", null, null),
                PageRequest.of(0, 10)
        );

        assertEquals(1, matched.getTotalElements());
    }

    @Test
    @DisplayName("운영 작업 목록 검색은 담당자명도 함께 찾는다")
    void getTaskListMatchesAssigneeName() {
        AdminUser assignee = adminUserRepository.save(AdminUser.builder()
                .loginId("ops-assignee")
                .password("pw")
                .name("정산담당")
                .build());
        adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("정산 배치 검수")
                .description("야간 배치 점검")
                .status("TODO")
                .priority("HIGH")
                .assigneeAdminNo(assignee.getAdminNo())
                .dueDate(LocalDate.of(2026, 6, 22))
                .isPinned("N")
                .build());

        Page<?> matched = adminOperationTaskRepository.getTaskList(
                new AdminOperationTaskListQuery("정산담당", null, null, null, null, null, null, null, null, null, null, "PINNED_DUE", null, null),
                PageRequest.of(0, 10)
        );

        assertEquals(1, matched.getTotalElements());
    }

    @Test
    @DisplayName("운영 작업 목록 최근 메모 정렬은 마지막 메모가 최신인 작업을 먼저 노출한다")
    void getTaskListSortsByLatestComment() {
        AdminOperationTask firstTask = adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("주문 검수")
                .description("출고 검수")
                .status("TODO")
                .priority("HIGH")
                .isPinned("N")
                .build());
        AdminOperationTask secondTask = adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("정산 마감")
                .description("정산 확인")
                .status("IN_PROGRESS")
                .priority("MEDIUM")
                .isPinned("N")
                .build());

        adminOperationTaskCommentRepository.save(comment(firstTask.getTaskNo(), "먼저 달린 메모", null, LocalDateTime.of(2026, 6, 3, 9, 0)));
        adminOperationTaskCommentRepository.save(comment(secondTask.getTaskNo(), "나중에 달린 메모", null, LocalDateTime.of(2026, 6, 4, 18, 30)));

        Page<?> sorted = adminOperationTaskRepository.getTaskList(
                new AdminOperationTaskListQuery("달린 메모", null, null, null, null, null, null, null, null, null, null, "LATEST_COMMENT_DESC", null, null),
                PageRequest.of(0, 10)
        );

        List<Long> sortedTaskNos = sorted.getContent().stream()
                .map(item -> ((AdminOperationTaskListResDto) item).getTaskNo())
                .toList();
        assertEquals(true, sortedTaskNos.size() >= 2);
        assertIterableEquals(List.of(secondTask.getTaskNo(), firstTask.getTaskNo()), sortedTaskNos.subList(0, 2));
    }

    @Test
    @DisplayName("운영 작업 목록은 메모 없는 작업만 필터링할 수 있다")
    void getTaskListFiltersTasksWithoutComments() {
        AdminOperationTask withoutComment = adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("무메모 작업")
                .description("메모 없음")
                .status("TODO")
                .priority("MEDIUM")
                .isPinned("N")
                .build());
        AdminOperationTask withComment = adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("메모 작업")
                .description("메모 있음")
                .status("TODO")
                .priority("MEDIUM")
                .isPinned("N")
                .build());
        adminOperationTaskCommentRepository.save(comment(withComment.getTaskNo(), "메모 있음", null));

        Page<?> matched = adminOperationTaskRepository.getTaskList(
                new AdminOperationTaskListQuery(null, null, null, null, null, null, null, null, "N", null, null, "PINNED_DUE", null, null),
                PageRequest.of(0, 10)
        );

        List<Long> matchedTaskNos = matched.getContent().stream()
                .map(item -> ((AdminOperationTaskListResDto) item).getTaskNo())
                .toList();
        assertTrue(matchedTaskNos.contains(withoutComment.getTaskNo()));
        assertTrue(matchedTaskNos.stream().noneMatch(taskNo -> taskNo.equals(withComment.getTaskNo())));
    }

    @Test
    @DisplayName("운영 작업 목록은 지정한 일수 안에 마감되는 작업만 조회한다")
    void getTaskListFiltersTasksDueWithinDays() {
        LocalDate today = LocalDate.now();
        AdminOperationTask nearDueTask = adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("임박 작업")
                .description("3일 안에 마감")
                .status("IN_PROGRESS")
                .priority("HIGH")
                .dueDate(today.plusDays(3))
                .isPinned("N")
                .build());
        adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("여유 작업")
                .description("10일 뒤 마감")
                .status("IN_PROGRESS")
                .priority("HIGH")
                .dueDate(today.plusDays(10))
                .isPinned("N")
                .build());
        adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("완료 임박 작업")
                .description("완료 상태")
                .status("DONE")
                .priority("LOW")
                .dueDate(today.plusDays(2))
                .isPinned("N")
                .build());

        Page<?> matched = adminOperationTaskRepository.getTaskList(
                new AdminOperationTaskListQuery(null, null, null, null, null, null, null, null, null, 7, null, "PINNED_DUE", null, null),
                PageRequest.of(0, 10)
        );

        List<Long> matchedTaskNos = matched.getContent().stream()
                .map(item -> ((AdminOperationTaskListResDto) item).getTaskNo())
                .toList();
        assertEquals(List.of(nearDueTask.getTaskNo()), matchedTaskNos);
    }

    @Test
    @DisplayName("운영 작업 목록 메모 수 정렬은 메모가 많은 작업을 먼저 노출한다")
    void getTaskListSortsByCommentCount() {
        AdminOperationTask firstTask = adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("메모 한 건")
                .description("적은 메모")
                .status("TODO")
                .priority("HIGH")
                .isPinned("N")
                .build());
        AdminOperationTask secondTask = adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("메모 세 건")
                .description("많은 메모")
                .status("TODO")
                .priority("HIGH")
                .isPinned("N")
                .build());

        adminOperationTaskCommentRepository.save(comment(firstTask.getTaskNo(), "첫 메모", null));
        adminOperationTaskCommentRepository.save(comment(secondTask.getTaskNo(), "둘째 메모", null));
        adminOperationTaskCommentRepository.save(comment(secondTask.getTaskNo(), "셋째 메모", null));
        adminOperationTaskCommentRepository.save(comment(secondTask.getTaskNo(), "넷째 메모", null));

        Page<?> sorted = adminOperationTaskRepository.getTaskList(
                new AdminOperationTaskListQuery("메모", null, null, null, null, null, null, null, null, null, null, "COMMENT_COUNT_DESC", null, null),
                PageRequest.of(0, 10)
        );

        List<Long> sortedTaskNos = sorted.getContent().stream()
                .map(item -> ((AdminOperationTaskListResDto) item).getTaskNo())
                .toList();
        assertEquals(true, sortedTaskNos.size() >= 2);
        assertIterableEquals(List.of(secondTask.getTaskNo(), firstTask.getTaskNo()), sortedTaskNos.subList(0, 2));
    }

    @Test
    @DisplayName("운영 작업 목록 검색은 작업 번호가 주어지면 정확히 해당 작업만 조회한다")
    void getTaskListMatchesExactTaskNo() {
        AdminOperationTask targetTask = adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("정확 조회 대상")
                .description("작업 번호 테스트")
                .status("TODO")
                .priority("HIGH")
                .isPinned("N")
                .build());
        adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("비교 대상")
                .description("다른 작업")
                .status("TODO")
                .priority("LOW")
                .isPinned("N")
                .build());

        Page<?> matched = adminOperationTaskRepository.getTaskList(
                new AdminOperationTaskListQuery(null, targetTask.getTaskNo(), null, null, null, null, null, null, null, null, null, "PINNED_DUE", null, null),
                PageRequest.of(0, 10)
        );

        assertEquals(1L, matched.getTotalElements());
        assertEquals(targetTask.getTaskNo(), ((AdminOperationTaskListResDto) matched.getContent().getFirst()).getTaskNo());
    }

    @Test
    @DisplayName("운영 작업 워크로드 검색은 담당자명과 메모 키워드도 함께 집계한다")
    void getTaskWorkloadPageMatchesAssigneeNameAndCommentKeyword() {
        AdminUser assignee = adminUserRepository.save(AdminUser.builder()
                .loginId("ops-workload")
                .password("pw")
                .name("정산운영")
                .build());
        AdminOperationTask task = adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("주간 정산 점검")
                .description("정산 배치 확인")
                .status("IN_PROGRESS")
                .priority("HIGH")
                .assigneeAdminNo(assignee.getAdminNo())
                .dueDate(LocalDate.of(2026, 6, 21))
                .isPinned("Y")
                .build());
        adminOperationTaskCommentRepository.save(comment(task.getTaskNo(), "송장 지연 이슈 확인", assignee.getAdminNo()));

        Page<?> assigneeMatched = adminOperationTaskRepository.getTaskWorkloadPage(
                new AdminOperationTaskWorkloadListQuery("정산운영", null, null, null),
                PageRequest.of(0, 10),
                LocalDate.of(2026, 6, 10)
        );
        Page<?> commentMatched = adminOperationTaskRepository.getTaskWorkloadPage(
                new AdminOperationTaskWorkloadListQuery("송장 지연", null, null, null),
                PageRequest.of(0, 10),
                LocalDate.of(2026, 6, 10)
        );

        assertEquals(1, assigneeMatched.getTotalElements());
        assertEquals(1, commentMatched.getTotalElements());
    }

    @Test
    @DisplayName("운영 작업 워크로드 검색은 여러 키워드를 모두 만족하는 담당자만 집계한다")
    void getTaskWorkloadPageMatchesAllKeywordTokens() {
        AdminUser assignee = adminUserRepository.save(AdminUser.builder()
                .loginId("ops-workload-token")
                .password("pw")
                .name("정산운영")
                .build());
        AdminOperationTask task = adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("정산 배치")
                .description("송장 지연 처리")
                .status("IN_PROGRESS")
                .priority("HIGH")
                .assigneeAdminNo(assignee.getAdminNo())
                .isPinned("Y")
                .build());
        adminOperationTaskCommentRepository.save(comment(task.getTaskNo(), "지연 재확인 메모", assignee.getAdminNo()));
        adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("정산 점검")
                .description("일반 처리")
                .status("IN_PROGRESS")
                .priority("MEDIUM")
                .isPinned("N")
                .build());

        Page<?> matched = adminOperationTaskRepository.getTaskWorkloadPage(
                new AdminOperationTaskWorkloadListQuery("정산 지연", null, null, null),
                PageRequest.of(0, 10),
                LocalDate.of(2026, 6, 10)
        );

        assertEquals(1, matched.getTotalElements());
    }

    @Test
    @DisplayName("운영 작업 담당자 추천은 활성 관리자만 반환한다")
    void getTaskAssignmentRecommendationsExcludesSuspendedAdmins() {
        AdminUser activeAdmin = adminUserRepository.save(AdminUser.builder()
                .loginId("active-rec")
                .password("pw")
                .name("활성 담당자")
                .role("ROLE_ADMIN")
                .status("ACTIVE")
                .build());
        adminUserRepository.save(AdminUser.builder()
                .loginId("suspended-rec")
                .password("pw")
                .name("정지 담당자")
                .role("ROLE_ADMIN")
                .status("SUSPENDED")
                .build());
        adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("활성 관리자 작업")
                .description("추천 대상")
                .status("TODO")
                .priority("HIGH")
                .assigneeAdminNo(activeAdmin.getAdminNo())
                .isPinned("N")
                .build());

        var result = adminOperationTaskRepository.getTaskAssignmentRecommendations(LocalDate.of(2026, 6, 10), null, 10);

        assertTrue(result.stream().anyMatch(item -> "활성 담당자".equals(item.adminName())));
        assertTrue(result.stream().noneMatch(item -> "정지 담당자".equals(item.adminName())));
    }

    @Test
    @DisplayName("운영 작업 워크로드 정렬은 총 작업 많은 순 기준으로 담당자를 먼저 노출한다")
    void getTaskWorkloadPageSortsByTotalTaskCount() {
        AdminUser heavyAssignee = adminUserRepository.save(AdminUser.builder()
                .loginId("ops-heavy")
                .password("pw")
                .name("작업 많은 담당자")
                .build());
        AdminUser lightAssignee = adminUserRepository.save(AdminUser.builder()
                .loginId("ops-light")
                .password("pw")
                .name("작업 적은 담당자")
                .build());

        adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("정산 1")
                .description("정산 작업")
                .status("TODO")
                .priority("HIGH")
                .assigneeAdminNo(heavyAssignee.getAdminNo())
                .isPinned("N")
                .build());
        adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("정산 2")
                .description("정산 작업")
                .status("IN_PROGRESS")
                .priority("MEDIUM")
                .assigneeAdminNo(heavyAssignee.getAdminNo())
                .isPinned("N")
                .build());
        adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("정산 3")
                .description("정산 작업")
                .status("DONE")
                .priority("LOW")
                .assigneeAdminNo(heavyAssignee.getAdminNo())
                .isPinned("N")
                .build());

        adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("정산 단건")
                .description("정산 작업")
                .status("TODO")
                .priority("HIGH")
                .assigneeAdminNo(lightAssignee.getAdminNo())
                .isPinned("N")
                .build());

        Page<?> sorted = adminOperationTaskRepository.getTaskWorkloadPage(
                new AdminOperationTaskWorkloadListQuery("정산", null, null, "TOTAL_DESC"),
                PageRequest.of(0, 10),
                LocalDate.of(2026, 6, 10)
        );

        List<Long> adminNos = sorted.getContent().stream()
                .map(item -> ((AdminOperationTaskWorkloadDto) item).assigneeAdminNo())
                .toList();
        assertIterableEquals(List.of(heavyAssignee.getAdminNo(), lightAssignee.getAdminNo()), adminNos.subList(0, 2));
    }

    private AdminOperationTaskComment comment(Long taskNo, String content, Long crtNo) {
        return comment(taskNo, content, crtNo, null);
    }

    private AdminOperationTaskComment comment(Long taskNo, String content, Long crtNo, LocalDateTime crtDtm) {
        AdminOperationTaskComment comment = AdminOperationTaskComment.builder()
                .taskNo(taskNo)
                .content(content)
                .build();
        setBaseEntityField(comment, "crtNo", crtNo);
        if (crtDtm != null) {
            setBaseEntityField(comment, "crtDtm", crtDtm);
        }
        return comment;
    }

    private void setBaseEntityField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }
}
