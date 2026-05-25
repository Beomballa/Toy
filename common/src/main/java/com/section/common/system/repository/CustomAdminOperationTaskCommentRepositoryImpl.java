package com.section.common.system.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.system.dto.AdminOperationTaskCommentResDto;
import com.section.common.system.dto.AdminOperationTaskCommentSummaryDto;

import java.util.List;

import static com.section.common.system.entity.QAdminOperationTaskComment.adminOperationTaskComment;
import static com.section.common.system.entity.QAdminUser.adminUser;

public class CustomAdminOperationTaskCommentRepositoryImpl implements CustomAdminOperationTaskCommentRepository {

    private final JPAQueryFactory queryFactory;

    public CustomAdminOperationTaskCommentRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<AdminOperationTaskCommentResDto> getTaskComments(Long taskNo, int limit) {
        return queryFactory
                .select(Projections.bean(
                        AdminOperationTaskCommentResDto.class,
                        adminOperationTaskComment.commentNo,
                        adminOperationTaskComment.taskNo,
                        adminOperationTaskComment.crtNo.as("adminNo"),
                        adminUser.name.as("adminName"),
                        adminOperationTaskComment.content,
                        adminOperationTaskComment.crtDtm
                ))
                .from(adminOperationTaskComment)
                .leftJoin(adminUser).on(adminOperationTaskComment.crtNo.eq(adminUser.adminNo))
                .where(adminOperationTaskComment.taskNo.eq(taskNo))
                // 상세 화면 최근 메모는 최신순으로 고정해 작업 문맥을 빨리 읽게 합니다.
                .orderBy(adminOperationTaskComment.commentNo.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<AdminOperationTaskCommentSummaryDto> getLatestCommentsByTaskNos(List<Long> taskNos) {
        if (taskNos == null || taskNos.isEmpty()) {
            return List.of();
        }

        com.section.common.system.entity.QAdminOperationTaskComment latestComment =
                new com.section.common.system.entity.QAdminOperationTaskComment("latestComment");

        return queryFactory
                .select(Projections.bean(
                        AdminOperationTaskCommentSummaryDto.class,
                        adminOperationTaskComment.taskNo,
                        adminOperationTaskComment.commentNo,
                        adminOperationTaskComment.crtNo.as("adminNo"),
                        adminUser.name.as("adminName"),
                        adminOperationTaskComment.content,
                        adminOperationTaskComment.crtDtm
                ))
                .from(adminOperationTaskComment)
                .leftJoin(adminUser).on(adminOperationTaskComment.crtNo.eq(adminUser.adminNo))
                .where(
                        adminOperationTaskComment.taskNo.in(taskNos),
                        // 대시보드용 요약은 task별 최신 메모 1건만 필요하므로 max(comment_no) correlated subquery로 잘라냅니다.
                        adminOperationTaskComment.commentNo.eq(
                                JPAExpressions.select(latestComment.commentNo.max())
                                        .from(latestComment)
                                        .where(latestComment.taskNo.eq(adminOperationTaskComment.taskNo))
                        )
                )
                .orderBy(adminOperationTaskComment.commentNo.desc())
                .fetch();
    }
}
