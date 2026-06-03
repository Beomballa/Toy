package com.section.common.system.repository;

import com.section.common.system.entity.AdminOperationTaskComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminOperationTaskCommentRepository extends JpaRepository<AdminOperationTaskComment, Long>, CustomAdminOperationTaskCommentRepository {
    void deleteByTaskNoIn(java.util.Collection<Long> taskNos);
}
