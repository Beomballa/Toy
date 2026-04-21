package com.section.common.system.repository;

import com.section.common.system.entity.AdminActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActivityLogRepository extends JpaRepository<AdminActivityLog, Long> {
    Page<AdminActivityLog> findAllByOrderByActionDtmDesc(Pageable pageable);
}
