package com.section.common.system.repository;

import com.section.common.system.entity.AdminOperationTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminOperationTaskRepository extends JpaRepository<AdminOperationTask, Long>, CustomAdminOperationTaskRepository {
}
