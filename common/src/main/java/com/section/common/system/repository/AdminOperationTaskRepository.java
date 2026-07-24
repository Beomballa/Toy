package com.section.common.system.repository;

import com.section.common.system.entity.AdminOperationTask;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AdminOperationTaskRepository extends JpaRepository<AdminOperationTask, Long>, CustomAdminOperationTaskRepository {

    Optional<AdminOperationTask> findBySourceTypeAndSourceId(String sourceType, Long sourceId);

    List<AdminOperationTask> findAllBySourceTypeAndSourceIdIn(String sourceType, Collection<Long> sourceIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select task
              from AdminOperationTask task
             where task.taskNo in :taskNos
             order by task.taskNo asc
            """)
    List<AdminOperationTask> findAllByTaskNoInForUpdate(@Param("taskNos") Collection<Long> taskNos);
}
