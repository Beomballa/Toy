package com.section.admin.settings.service;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.system.entity.AdminSystemSetting;
import com.section.common.system.repository.AdminSystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOperationPolicyService {

    static final String KEY_MAINTENANCE_MODE = "SYSTEM_MAINTENANCE_MODE";
    static final String KEY_COMMUNITY_WRITE = "COMMUNITY_WRITE_ENABLED";
    static final String KEY_ORDER_EXPORT = "ORDER_EXPORT_ENABLED";

    private final AdminSystemSettingRepository adminSystemSettingRepository;

    public void assertAdminWriteAllowed() {
        if (isEnabled(KEY_MAINTENANCE_MODE, false)) {
            throw new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE);
        }
    }

    public void assertCommunityWriteAllowed() {
        assertAdminWriteAllowed();
        if (!isEnabled(KEY_COMMUNITY_WRITE, true)) {
            throw new BusinessException(ErrorCode.ADMIN_FEATURE_DISABLED);
        }
    }

    public void assertOrderExportAllowed() {
        if (!isEnabled(KEY_ORDER_EXPORT, true)) {
            throw new BusinessException(ErrorCode.ADMIN_FEATURE_DISABLED);
        }
    }

    private boolean isEnabled(String key, boolean defaultValue) {
        Map<String, AdminSystemSetting> settings = adminSystemSettingRepository.findAllBySettingKeyIn(List.of(key))
                .stream()
                .collect(Collectors.toMap(AdminSystemSetting::getSettingKey, Function.identity()));
        AdminSystemSetting setting = settings.get(key);
        return setting == null ? defaultValue : Boolean.parseBoolean(setting.getSettingValue());
    }
}
