package com.section.admin.task.repository;

import com.section.admin.AdminToyApplication;
import com.section.common.system.dto.AdminOperationTaskListQuery;
import com.section.common.system.dto.AdminOperationTaskListResDto;
import com.section.common.system.dto.AdminOperationTaskWorkloadListQuery;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
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
    @DisplayName("운영 작업 목록 검색은 메모 내용과 메모 작성자명도 함께 찾는다")
    void getTaskListMatchesCommentContentAndAuthorName() {
        AdminUser commentAuthor = adminUserRepository.save(AdminUser.builder()
                .loginId("ops-comment")
                .password("pw")
                .name("메모담당")
                .build());
        AdminOperationTask targetTask = adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("배너 점검")
                .description("메인 배너 운영")
                .status("TODO")
                .priority("HIGH")
                .dueDate(LocalDate.of(2026, 6, 20))
                .isPinned("N")
                .build());
        AdminOperationTask otherTask = adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title("카테고리 정리")
                .description("카테고리 운영")
                .status("TODO")
                .priority("LOW")
                .isPinned("N")
                .build());

        adminOperationTaskCommentRepository.save(comment(targetTask.getTaskNo(), "긴급 점검 메모", commentAuthor.getAdminNo()));
        adminOperationTaskCommentRepository.save(comment(otherTask.getTaskNo(), "일반 메모", null));

        Page<?> contentMatched = adminOperationTaskRepository.getTaskList(
                new AdminOperationTaskListQuery("긴급", null, null, null, null, null, null, null, null, "PINNED_DUE", null, null),
                PageRequest.of(0, 10)
        );
        Page<?> authorMatched = adminOperationTaskRepository.getTaskList(
                new AdminOperationTaskListQuery("메모담당", null, null, null, null, null, null, null, null, "PINNED_DUE", null, null),
                PageRequest.of(0, 10)
        );

        assertEquals(1, contentMatched.getTotalElements());
        assertEquals(1, authorMatched.getTotalElements());
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
                new AdminOperationTaskListQuery("정산 지연", null, null, null, null, null, null, null, null, "PINNED_DUE", null, null),
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
                new AdminOperationTaskListQuery("정산담당", null, null, null, null, null, null, null, null, "PINNED_DUE", null, null),
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
                new AdminOperationTaskListQuery(null, null, null, null, null, null, null, null, null, "LATEST_COMMENT_DESC", null, null),
                PageRequest.of(0, 10)
        );

        assertIterableEquals(
                List.of(secondTask.getTaskNo(), firstTask.getTaskNo()),
                sorted.getContent().stream()
                        .map(item -> ((AdminOperationTaskListResDto) item).getTaskNo())
                        .toList()
        );
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
                new AdminOperationTaskWorkloadListQuery("정산운영", null, null),
                PageRequest.of(0, 10),
                LocalDate.of(2026, 6, 10)
        );
        Page<?> commentMatched = adminOperationTaskRepository.getTaskWorkloadPage(
                new AdminOperationTaskWorkloadListQuery("송장 지연", null, null),
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
                new AdminOperationTaskWorkloadListQuery("정산 지연", null, null),
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

        assertEquals(1, result.size());
        assertTrue(result.stream().allMatch(item -> "활성 담당자".equals(item.adminName())));
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
