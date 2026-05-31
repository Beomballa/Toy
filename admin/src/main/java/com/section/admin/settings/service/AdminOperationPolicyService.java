package com.section.admin.settings.service;

import com.section.admin.settings.support.AdminSettingDefinition;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.system.entity.AdminSystemSetting;
import com.section.common.system.repository.AdminSystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Map<String, AdminSystemSetting> settings = loadPolicySettings();
        if (isEnabled(settings, AdminSettingDefinition.MAINTENANCE_MODE)) {
            throw new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE);
        }
    }

    public void assertCommunityWriteAllowed() {
        Map<String, AdminSystemSetting> settings = loadPolicySettings();
        if (isEnabled(settings, AdminSettingDefinition.MAINTENANCE_MODE)) {
            throw new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE);
        }
        if (!isEnabled(settings, AdminSettingDefinition.COMMUNITY_WRITE_ENABLED)) {
            throw new BusinessException(ErrorCode.ADMIN_FEATURE_DISABLED);
        }
    }

    public void assertOrderExportAllowed() {
        Map<String, AdminSystemSetting> settings = loadPolicySettings();
        if (!isEnabled(settings, AdminSettingDefinition.ORDER_EXPORT_ENABLED)) {
            throw new BusinessException(ErrorCode.ADMIN_FEATURE_DISABLED);
        }
    }

    private Map<String, AdminSystemSetting> loadPolicySettings() {
        return adminSystemSettingRepository.findAllBySettingKeyIn(
                        java.util.List.of(KEY_MAINTENANCE_MODE, KEY_COMMUNITY_WRITE, KEY_ORDER_EXPORT)
                )
                .stream()
                .collect(Collectors.toMap(AdminSystemSetting::getSettingKey, Function.identity()));
    }

    private boolean isEnabled(Map<String, AdminSystemSetting> settings, AdminSettingDefinition definition) {
        AdminSystemSetting setting = settings.get(definition.key());
        return definition.parseBoolean(setting == null ? definition.defaultValue() : setting.getSettingValue());
    }
}
