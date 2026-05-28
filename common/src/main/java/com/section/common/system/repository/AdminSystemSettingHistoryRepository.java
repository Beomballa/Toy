package com.section.common.system.repository;

import com.section.common.system.entity.AdminSystemSettingHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminSystemSettingHistoryRepository
        extends JpaRepository<AdminSystemSettingHistory, Long>, CustomAdminSystemSettingHistoryRepository {
}
