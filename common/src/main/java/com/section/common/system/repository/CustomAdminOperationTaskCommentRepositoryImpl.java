package com.section.common.system.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.system.dto.AdminOperationTaskCommentResDto;

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
}
