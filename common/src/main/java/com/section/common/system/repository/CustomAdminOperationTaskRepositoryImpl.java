package com.section.common.system.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.system.dto.AdminOperationTaskListQuery;
import com.section.common.system.dto.AdminOperationTaskListResDto;
import com.section.common.system.dto.AdminOperationTaskSummaryDto;
import com.section.common.system.entity.AdminOperationTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static com.section.common.system.entity.QAdminOperationTask.adminOperationTask;
import static com.section.common.system.entity.QAdminUser.adminUser;

public class CustomAdminOperationTaskRepositoryImpl implements CustomAdminOperationTaskRepository {

    private final JPAQueryFactory queryFactory;

    public CustomAdminOperationTaskRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<AdminOperationTaskListResDto> getTaskList(AdminOperationTaskListQuery query, Pageable pageable) {
        List<AdminOperationTaskListResDto> content = queryFactory
                .select(Projections.bean(
                        AdminOperationTaskListResDto.class,
                        adminOperationTask.taskNo,
                        adminOperationTask.title,
                        adminOperationTask.description,
                        adminOperationTask.status,
                        adminOperationTask.priority,
                        adminOperationTask.assigneeAdminNo,
                        adminUser.name.as("assigneeAdminName"),
                        adminOperationTask.dueDate,
                        adminOperationTask.isPinned,
                        adminOperationTask.crtDtm
                ))
                .from(adminOperationTask)
                .leftJoin(adminUser).on(adminOperationTask.assigneeAdminNo.eq(adminUser.adminNo))
                .where(
                        keywordLike(query.keyword()),
                        statusEq(query.status()),
                        priorityEq(query.priority()),
                        assigneeEq(query.assigneeAdminNo()),
                        overdue(query.overdueOnly(), LocalDate.now())
                )
                .orderBy(
                        adminOperationTask.isPinned.desc(),
                        adminOperationTask.dueDate.asc().nullsLast(),
                        adminOperationTask.taskNo.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(adminOperationTask.count())
                .from(adminOperationTask)
                .where(
                        keywordLike(query.keyword()),
                        statusEq(query.status()),
                        priorityEq(query.priority()),
                        assigneeEq(query.assigneeAdminNo()),
                        overdue(query.overdueOnly(), LocalDate.now())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    @Override
    public AdminOperationTaskSummaryDto getTaskSummary(AdminOperationTaskListQuery query, LocalDate today) {
        AdminOperationTaskListQuery statsQuery = query.toStatsQuery();
        return new AdminOperationTaskSummaryDto(
                countBy(statsQuery, null, null, today),
                countBy(statsQuery, "TODO", null, today),
                countBy(statsQuery, "IN_PROGRESS", null, today),
                countBy(statsQuery, null, "Y", today)
        );
    }

    @Override
    public List<AdminOperationTask> getDashboardTasks(LocalDate today, int limit) {
        return queryFactory
                .selectFrom(adminOperationTask)
                .where(
                        adminOperationTask.status.in("TODO", "IN_PROGRESS")
                )
                .orderBy(
                        // 대시보드에서는 고정, 기한 임박, 최근 생성 순서로 정렬합니다.
                        adminOperationTask.isPinned.desc(),
                        adminOperationTask.dueDate.asc().nullsLast(),
                        adminOperationTask.taskNo.desc()
                )
                .limit(limit)
                .fetch();
    }

    private long countBy(AdminOperationTaskListQuery query, String status, String overdueOnly, LocalDate today) {
        Long count = queryFactory
                .select(adminOperationTask.count())
                .from(adminOperationTask)
                .where(
                        keywordLike(query.keyword()),
                        statusEq(status != null ? status : query.status()),
                        priorityEq(query.priority()),
                        assigneeEq(query.assigneeAdminNo()),
                        overdue(overdueOnly, today)
                )
                .fetchOne();
        return count == null ? 0L : count;
    }

    private BooleanExpression keywordLike(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return adminOperationTask.title.containsIgnoreCase(keyword.trim())
                .or(adminOperationTask.description.isNotNull().and(adminOperationTask.description.containsIgnoreCase(keyword.trim())));
    }

    private BooleanExpression statusEq(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return adminOperationTask.status.eq(status.trim().toUpperCase());
    }

    private BooleanExpression priorityEq(String priority) {
        if (priority == null || priority.isBlank()) {
            return null;
        }
        return adminOperationTask.priority.eq(priority.trim().toUpperCase());
    }

    private BooleanExpression assigneeEq(Long assigneeAdminNo) {
        return assigneeAdminNo == null ? null : adminOperationTask.assigneeAdminNo.eq(assigneeAdminNo);
    }

    private BooleanExpression overdue(String overdueOnly, LocalDate today) {
        if (!"Y".equalsIgnoreCase(overdueOnly)) {
            return null;
        }
        return adminOperationTask.dueDate.isNotNull()
                .and(adminOperationTask.dueDate.lt(today))
                .and(adminOperationTask.status.ne("DONE"));
    }
}
