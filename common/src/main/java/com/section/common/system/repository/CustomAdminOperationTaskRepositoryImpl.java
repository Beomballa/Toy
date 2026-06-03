package com.section.common.system.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.system.dto.AdminOperationTaskListQuery;
import com.section.common.system.dto.AdminOperationTaskListResDto;
import com.section.common.system.dto.AdminOperationTaskAssigneeRecommendationDto;
import com.section.common.system.dto.AdminOperationTaskSummaryDto;
import com.section.common.system.dto.AdminOperationTaskWorkloadListQuery;
import com.section.common.system.dto.AdminOperationTaskWorkloadSummaryDto;
import com.section.common.system.dto.AdminOperationTaskWorkloadDto;
import com.section.common.system.entity.AdminOperationTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.section.common.system.entity.QAdminOperationTask.adminOperationTask;
import static com.section.common.system.entity.QAdminOperationTaskComment.adminOperationTaskComment;
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
                        isPinnedEq(query.isPinned()),
                        unassigned(query.unassignedOnly()),
                        commented(query.commentedOnly()),
                        dueStateEq(query.dueState(), LocalDate.now()),
                        overdue(query.overdueOnly(), LocalDate.now()),
                        dueDateOnOrAfter(query.dueDateFrom()),
                        dueDateOnOrBefore(query.dueDateTo())
                )
                .orderBy(resolveOrderSpecifiers(query.sortBy()))
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
                        isPinnedEq(query.isPinned()),
                        unassigned(query.unassignedOnly()),
                        commented(query.commentedOnly()),
                        dueStateEq(query.dueState(), LocalDate.now()),
                        overdue(query.overdueOnly(), LocalDate.now()),
                        dueDateOnOrAfter(query.dueDateFrom()),
                        dueDateOnOrBefore(query.dueDateTo())
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
                countBy(statsQuery, null, "Y", today),
                countUnassigned(statsQuery, today)
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

    @Override
    public List<AdminOperationTask> getDashboardUnassignedTasks(LocalDate today, int limit) {
        return queryFactory
                .selectFrom(adminOperationTask)
                .where(
                        adminOperationTask.assigneeAdminNo.isNull(),
                        adminOperationTask.status.ne("DONE")
                )
                .orderBy(
                        adminOperationTask.isPinned.desc(),
                        adminOperationTask.dueDate.asc().nullsLast(),
                        adminOperationTask.taskNo.desc()
                )
                .limit(limit)
                .fetch();
    }

    @Override
    public List<AdminOperationTaskWorkloadDto> getDashboardTaskWorkloads(LocalDate today, int limit) {
        return queryFactory
                .select(Projections.constructor(
                        AdminOperationTaskWorkloadDto.class,
                        adminOperationTask.assigneeAdminNo,
                        adminUser.name,
                        adminOperationTask.count(),
                        sumTodoCount(),
                        sumInProgressCount(),
                        sumOverdueCount(today)
                ))
                .from(adminOperationTask)
                .leftJoin(adminUser).on(adminOperationTask.assigneeAdminNo.eq(adminUser.adminNo))
                .where(adminOperationTask.assigneeAdminNo.isNotNull())
                .groupBy(adminOperationTask.assigneeAdminNo, adminUser.name)
                .orderBy(
                        sumOverdueCount(today).desc(),
                        sumInProgressCount().desc(),
                        sumTodoCount().desc(),
                        adminOperationTask.count().desc(),
                        adminOperationTask.assigneeAdminNo.asc()
                )
                .limit(limit)
                .fetch();
    }

    @Override
    public AdminOperationTaskWorkloadDto getTaskWorkload(Long assigneeAdminNo, LocalDate today) {
        return queryFactory
                .select(Projections.constructor(
                        AdminOperationTaskWorkloadDto.class,
                        adminOperationTask.assigneeAdminNo,
                        adminUser.name,
                        adminOperationTask.count(),
                        sumTodoCount(),
                        sumInProgressCount(),
                        sumOverdueCount(today)
                ))
                .from(adminOperationTask)
                .leftJoin(adminUser).on(adminOperationTask.assigneeAdminNo.eq(adminUser.adminNo))
                .where(adminOperationTask.assigneeAdminNo.eq(assigneeAdminNo))
                .groupBy(adminOperationTask.assigneeAdminNo, adminUser.name)
                .fetchOne();
    }

    @Override
    public List<AdminOperationTaskListResDto> getRecentTasksByAssigneeAdminNo(Long assigneeAdminNo, int limit) {
        return queryFactory
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
                .where(adminOperationTask.assigneeAdminNo.eq(assigneeAdminNo))
                .orderBy(
                        adminOperationTask.isPinned.desc(),
                        adminOperationTask.dueDate.asc().nullsLast(),
                        adminOperationTask.taskNo.desc()
                )
                .limit(limit)
                .fetch();
    }

    @Override
    public List<AdminOperationTaskListResDto> getOverdueTasksByAssigneeAdminNo(Long assigneeAdminNo, LocalDate today, int limit) {
        return queryFactory
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
                        adminOperationTask.assigneeAdminNo.eq(assigneeAdminNo),
                        adminOperationTask.dueDate.isNotNull(),
                        adminOperationTask.dueDate.lt(today),
                        adminOperationTask.status.ne("DONE")
                )
                .orderBy(
                        adminOperationTask.dueDate.asc(),
                        adminOperationTask.isPinned.desc(),
                        adminOperationTask.taskNo.desc()
                )
                .limit(limit)
                .fetch();
    }

    @Override
    public List<AdminOperationTaskAssigneeRecommendationDto> getTaskAssignmentRecommendations(LocalDate today, Long excludeAdminNo, int limit) {
        return queryFactory
                .select(Projections.constructor(
                        AdminOperationTaskAssigneeRecommendationDto.class,
                        adminUser.adminNo,
                        adminUser.name,
                        adminOperationTask.count(),
                        sumInProgressCount(),
                        sumOverdueCount(today)
                ))
                .from(adminUser)
                .leftJoin(adminOperationTask).on(adminOperationTask.assigneeAdminNo.eq(adminUser.adminNo))
                .where(adminNoNe(excludeAdminNo))
                .groupBy(adminUser.adminNo, adminUser.name)
                .orderBy(
                        sumOverdueCount(today).asc(),
                        sumInProgressCount().asc(),
                        adminOperationTask.count().asc(),
                        adminUser.adminNo.asc()
                )
                .limit(limit)
                .fetch();
    }

    @Override
    public Page<AdminOperationTaskWorkloadDto> getTaskWorkloadPage(AdminOperationTaskWorkloadListQuery query, Pageable pageable, LocalDate today) {
        List<AdminOperationTaskWorkloadDto> content = queryFactory
                .select(Projections.constructor(
                        AdminOperationTaskWorkloadDto.class,
                        adminOperationTask.assigneeAdminNo,
                        adminUser.name,
                        adminOperationTask.count(),
                        sumTodoCount(),
                        sumInProgressCount(),
                        sumOverdueCount(today)
                ))
                .from(adminOperationTask)
                .leftJoin(adminUser).on(adminOperationTask.assigneeAdminNo.eq(adminUser.adminNo))
                .where(
                        adminOperationTask.assigneeAdminNo.isNotNull(),
                        keywordLike(query.keyword()),
                        priorityEq(query.priority()),
                        overdue(query.overdueOnly(), today)
                )
                .groupBy(adminOperationTask.assigneeAdminNo, adminUser.name)
                .orderBy(
                        sumOverdueCount(today).desc(),
                        sumInProgressCount().desc(),
                        sumTodoCount().desc(),
                        adminOperationTask.count().desc(),
                        adminOperationTask.assigneeAdminNo.asc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(adminOperationTask.assigneeAdminNo.countDistinct())
                .from(adminOperationTask)
                .where(
                        adminOperationTask.assigneeAdminNo.isNotNull(),
                        keywordLike(query.keyword()),
                        priorityEq(query.priority()),
                        overdue(query.overdueOnly(), today)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    @Override
    public AdminOperationTaskWorkloadSummaryDto getTaskWorkloadSummary(AdminOperationTaskWorkloadListQuery query, LocalDate today) {
        return new AdminOperationTaskWorkloadSummaryDto(
                countDistinctAssignees(query, today),
                countAssignedTasks(query, today),
                countOverdueTasks(query, today),
                countUnassignedTasks(query, today)
        );
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
                        isPinnedEq(query.isPinned()),
                        unassigned(query.unassignedOnly()),
                        commented(query.commentedOnly()),
                        dueStateEq(query.dueState(), today),
                        overdue(overdueOnly, today),
                        dueDateOnOrAfter(query.dueDateFrom()),
                        dueDateOnOrBefore(query.dueDateTo())
                )
                .fetchOne();
        return count == null ? 0L : count;
    }

    private BooleanExpression adminNoNe(Long adminNo) {
        return adminNo == null ? null : adminUser.adminNo.ne(adminNo);
    }

    private long countUnassigned(AdminOperationTaskListQuery query, LocalDate today) {
        Long count = queryFactory
                .select(adminOperationTask.count())
                .from(adminOperationTask)
                .where(
                        keywordLike(query.keyword()),
                        statusEq(query.status()),
                        priorityEq(query.priority()),
                        isPinnedEq(query.isPinned()),
                        commented(query.commentedOnly()),
                        dueStateEq(query.dueState(), today),
                        adminOperationTask.assigneeAdminNo.isNull(),
                        overdue(query.overdueOnly(), today),
                        dueDateOnOrAfter(query.dueDateFrom()),
                        dueDateOnOrBefore(query.dueDateTo())
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

    private BooleanExpression isPinnedEq(String isPinned) {
        if (isPinned == null || isPinned.isBlank()) {
            return null;
        }
        return adminOperationTask.isPinned.eq(isPinned.trim().toUpperCase());
    }

    private BooleanExpression unassigned(String unassignedOnly) {
        if (!"Y".equalsIgnoreCase(unassignedOnly)) {
            return null;
        }
        return adminOperationTask.assigneeAdminNo.isNull();
    }

    private BooleanExpression commented(String commentedOnly) {
        if (!"Y".equalsIgnoreCase(commentedOnly)) {
            return null;
        }
        return JPAExpressions.selectOne()
                .from(adminOperationTaskComment)
                .where(adminOperationTaskComment.taskNo.eq(adminOperationTask.taskNo))
                .exists();
    }

    private BooleanExpression overdue(String overdueOnly, LocalDate today) {
        if (!"Y".equalsIgnoreCase(overdueOnly)) {
            return null;
        }
        return adminOperationTask.dueDate.isNotNull()
                .and(adminOperationTask.dueDate.lt(today))
                .and(adminOperationTask.status.ne("DONE"));
    }

    private BooleanExpression dueStateEq(String dueState, LocalDate today) {
        if (dueState == null || dueState.isBlank()) {
            return null;
        }
        return switch (dueState.trim().toUpperCase()) {
            case "OVERDUE" -> adminOperationTask.dueDate.isNotNull()
                    .and(adminOperationTask.dueDate.lt(today))
                    .and(adminOperationTask.status.ne("DONE"));
            case "TODAY" -> adminOperationTask.dueDate.eq(today)
                    .and(adminOperationTask.status.ne("DONE"));
            case "UPCOMING" -> adminOperationTask.dueDate.isNotNull()
                    .and(adminOperationTask.dueDate.gt(today))
                    .and(adminOperationTask.status.ne("DONE"));
            case "NO_DUE" -> adminOperationTask.dueDate.isNull();
            default -> null;
        };
    }

    private BooleanExpression dueDateOnOrAfter(LocalDate dueDateFrom) {
        return dueDateFrom == null ? null : adminOperationTask.dueDate.goe(dueDateFrom);
    }

    private BooleanExpression dueDateOnOrBefore(LocalDate dueDateTo) {
        return dueDateTo == null ? null : adminOperationTask.dueDate.loe(dueDateTo);
    }

    private OrderSpecifier<?>[] resolveOrderSpecifiers(String sortBy) {
        String normalizedSort = sortBy == null || sortBy.isBlank() ? "PINNED_DUE" : sortBy.trim().toUpperCase();
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        switch (normalizedSort) {
            case "DUE_DATE_DESC" -> {
                orderSpecifiers.add(adminOperationTask.dueDate.desc().nullsLast());
                orderSpecifiers.add(adminOperationTask.isPinned.desc());
            }
            case "PRIORITY_DESC" -> {
                NumberExpression<Integer> priorityRank = new CaseBuilder()
                        .when(adminOperationTask.priority.eq("HIGH")).then(0)
                        .when(adminOperationTask.priority.eq("MEDIUM")).then(1)
                        .otherwise(2);
                orderSpecifiers.add(priorityRank.asc());
                orderSpecifiers.add(adminOperationTask.isPinned.desc());
                orderSpecifiers.add(adminOperationTask.dueDate.asc().nullsLast());
            }
            case "CREATED_DESC" -> {
                orderSpecifiers.add(adminOperationTask.crtDtm.desc());
                orderSpecifiers.add(adminOperationTask.isPinned.desc());
            }
            default -> {
                orderSpecifiers.add(adminOperationTask.isPinned.desc());
                orderSpecifiers.add(adminOperationTask.dueDate.asc().nullsLast());
            }
        }

        orderSpecifiers.add(adminOperationTask.taskNo.desc());
        return orderSpecifiers.toArray(new OrderSpecifier[0]);
    }

    private NumberExpression<Long> sumTodoCount() {
        NumberExpression<Long> todoCase = new CaseBuilder()
                .when(adminOperationTask.status.eq("TODO")).then(1L)
                .otherwise(0L);
        return Expressions.numberTemplate(Long.class, "sum({0})", todoCase);
    }

    private NumberExpression<Long> sumInProgressCount() {
        NumberExpression<Long> inProgressCase = new CaseBuilder()
                .when(adminOperationTask.status.eq("IN_PROGRESS")).then(1L)
                .otherwise(0L);
        return Expressions.numberTemplate(Long.class, "sum({0})", inProgressCase);
    }

    private NumberExpression<Long> sumOverdueCount(LocalDate today) {
        NumberExpression<Long> overdueCase = new CaseBuilder()
                .when(
                        adminOperationTask.dueDate.isNotNull()
                                .and(adminOperationTask.dueDate.lt(today))
                                .and(adminOperationTask.status.ne("DONE"))
                ).then(1L)
                .otherwise(0L);
        // 집계 화면은 상태별 카운트를 DB group-by 단계에서 끝내야 대시보드/워크로드 화면에서 메모리 재계산이 없습니다.
        return Expressions.numberTemplate(Long.class, "sum({0})", overdueCase);
    }

    private long countDistinctAssignees(AdminOperationTaskWorkloadListQuery query, LocalDate today) {
        Long count = queryFactory
                .select(adminOperationTask.assigneeAdminNo.countDistinct())
                .from(adminOperationTask)
                .where(
                        adminOperationTask.assigneeAdminNo.isNotNull(),
                        keywordLike(query.keyword()),
                        priorityEq(query.priority()),
                        overdue(query.overdueOnly(), today)
                )
                .fetchOne();
        return count == null ? 0L : count;
    }

    private long countAssignedTasks(AdminOperationTaskWorkloadListQuery query, LocalDate today) {
        Long count = queryFactory
                .select(adminOperationTask.count())
                .from(adminOperationTask)
                .where(
                        adminOperationTask.assigneeAdminNo.isNotNull(),
                        keywordLike(query.keyword()),
                        priorityEq(query.priority()),
                        overdue(query.overdueOnly(), today)
                )
                .fetchOne();
        return count == null ? 0L : count;
    }

    private long countOverdueTasks(AdminOperationTaskWorkloadListQuery query, LocalDate today) {
        Long count = queryFactory
                .select(adminOperationTask.count())
                .from(adminOperationTask)
                .where(
                        adminOperationTask.assigneeAdminNo.isNotNull(),
                        keywordLike(query.keyword()),
                        priorityEq(query.priority()),
                        adminOperationTask.dueDate.isNotNull(),
                        adminOperationTask.dueDate.lt(today),
                        adminOperationTask.status.ne("DONE")
                )
                .fetchOne();
        return count == null ? 0L : count;
    }

    private long countUnassignedTasks(AdminOperationTaskWorkloadListQuery query, LocalDate today) {
        Long count = queryFactory
                .select(adminOperationTask.count())
                .from(adminOperationTask)
                .where(
                        adminOperationTask.assigneeAdminNo.isNull(),
                        keywordLike(query.keyword()),
                        priorityEq(query.priority()),
                        overdue(query.overdueOnly(), today)
                )
                .fetchOne();
        return count == null ? 0L : count;
    }
}
