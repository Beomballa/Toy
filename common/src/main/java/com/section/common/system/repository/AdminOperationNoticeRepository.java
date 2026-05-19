package com.section.common.system.repository;

import com.section.common.system.entity.AdminOperationNotice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminOperationNoticeRepository extends JpaRepository<AdminOperationNotice, Long>, CustomAdminOperationNoticeRepository {
}
