package com.section.admin.settings.service;

import com.section.admin.settings.req.AdminSystemSettingSaveRequest;
import com.section.admin.settings.res.AdminSystemSettingResponse;
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
public class AdminSettingsService {

    private static final String KEY_MAINTENANCE_MODE = "SYSTEM_MAINTENANCE_MODE";
    private static final String KEY_COMMUNITY_WRITE = "COMMUNITY_WRITE_ENABLED";
    private static final String KEY_ORDER_EXPORT = "ORDER_EXPORT_ENABLED";
    private static final String KEY_LOW_STOCK_THRESHOLD = "LOW_STOCK_DEFAULT_THRESHOLD";

    private final AdminSystemSettingRepository adminSystemSettingRepository;

    public AdminSystemSettingResponse getSystemSettings() {
        Map<String, AdminSystemSetting> settings = adminSystemSettingRepository.findAllBySettingKeyIn(List.of(
                KEY_MAINTENANCE_MODE,
                KEY_COMMUNITY_WRITE,
                KEY_ORDER_EXPORT,
                KEY_LOW_STOCK_THRESHOLD
        )).stream().collect(Collectors.toMap(AdminSystemSetting::getSettingKey, Function.identity()));

        return new AdminSystemSettingResponse(
                readBoolean(settings, KEY_MAINTENANCE_MODE, false),
                readBoolean(settings, KEY_COMMUNITY_WRITE, true),
                readBoolean(settings, KEY_ORDER_EXPORT, true),
                readLong(settings, KEY_LOW_STOCK_THRESHOLD, 100L)
        );
    }

    @Transactional
    public void saveSystemSettings(AdminSystemSettingSaveRequest req) {
        if (req.lowStockDefaultThreshold() == null || req.lowStockDefaultThreshold() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        upsert(KEY_MAINTENANCE_MODE, String.valueOf(req.maintenanceMode()), "관리자 서비스 유지보수 모드");
        upsert(KEY_COMMUNITY_WRITE, String.valueOf(req.communityWriteEnabled()), "커뮤니티 글쓰기 허용");
        upsert(KEY_ORDER_EXPORT, String.valueOf(req.orderExportEnabled()), "주문 export 허용");
        upsert(KEY_LOW_STOCK_THRESHOLD, String.valueOf(req.lowStockDefaultThreshold()), "저재고 기본 임계값");
    }

    private boolean readBoolean(Map<String, AdminSystemSetting> settings, String key, boolean defaultValue) {
        AdminSystemSetting setting = settings.get(key);
        return setting == null ? defaultValue : Boolean.parseBoolean(setting.getSettingValue());
    }

    private long readLong(Map<String, AdminSystemSetting> settings, String key, long defaultValue) {
        AdminSystemSetting setting = settings.get(key);
        if (setting == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(setting.getSettingValue());
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void upsert(String key, String value, String description) {
        AdminSystemSetting setting = adminSystemSettingRepository.findBySettingKey(key)
                .orElseGet(() -> AdminSystemSetting.builder()
                        .settingKey(key)
                        .settingValue(value)
                        .description(description)
                        .build());
        setting.updateValue(value);
        adminSystemSettingRepository.save(setting);
    }
}
