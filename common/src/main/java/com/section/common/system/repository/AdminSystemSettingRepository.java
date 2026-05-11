package com.section.common.system.repository;

import com.section.common.system.entity.AdminSystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminSystemSettingRepository extends JpaRepository<AdminSystemSetting, Long> {

    Optional<AdminSystemSetting> findBySettingKey(String settingKey);

    List<AdminSystemSetting> findAllBySettingKeyIn(List<String> settingKeys);
}
