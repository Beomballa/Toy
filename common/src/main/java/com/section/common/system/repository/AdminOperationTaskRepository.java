package com.section.common.system.repository;

import com.section.common.system.entity.AdminOperationTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AdminOperationTaskRepository extends JpaRepository<AdminOperationTask, Long>, CustomAdminOperationTaskRepository {

    Optional<AdminOperationTask> findBySourceTypeAndSourceId(String sourceType, Long sourceId);

    List<AdminOperationTask> findAllBySourceTypeAndSourceIdIn(String sourceType, Collection<Long> sourceIds);
}
