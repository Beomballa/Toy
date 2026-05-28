package com.section.admin.settings.service;

import com.section.admin.settings.req.AdminSystemSettingSaveRequest;
import com.section.admin.settings.req.AdminSystemSettingHistoryListRequest;
import com.section.admin.settings.res.AdminSystemSettingHistoryListResponse;
import com.section.admin.settings.res.AdminSystemSettingResponse;
import com.section.admin.settings.support.AdminSettingDefinition;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.system.entity.AdminSystemSetting;
import com.section.common.system.entity.AdminSystemSettingHistory;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.dto.AdminSystemSettingHistoryListResDto;
import com.section.common.system.dto.AdminSystemSettingHistorySummaryDto;
import com.section.common.system.repository.AdminSystemSettingRepository;
import com.section.common.system.repository.AdminSystemSettingHistoryRepository;
import com.section.common.system.repository.AdminUserRepository;
import com.section.common.system.support.AdminRequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    private final AdminSystemSettingRepository adminSystemSettingRepository;
    private final AdminSystemSettingHistoryRepository adminSystemSettingHistoryRepository;
    private final AdminUserRepository adminUserRepository;

    public AdminSystemSettingResponse getSystemSettings() {
        Map<String, AdminSystemSetting> settings = loadSettingMap();

        return new AdminSystemSettingResponse(
                readBoolean(settings, AdminSettingDefinition.MAINTENANCE_MODE),
                readBoolean(settings, AdminSettingDefinition.COMMUNITY_WRITE_ENABLED),
                readBoolean(settings, AdminSettingDefinition.ORDER_EXPORT_ENABLED),
                readLong(settings, AdminSettingDefinition.LOW_STOCK_DEFAULT_THRESHOLD)
        );
    }

    public long getLowStockDefaultThreshold() {
        return getSystemSettings().lowStockDefaultThreshold();
    }

    public AdminSystemSettingHistoryListResponse getSystemSettingHistory(
            AdminSystemSettingHistoryListRequest req,
            Integer page,
            Integer size
    ) {
        var query = req.toQuery();
        Page<AdminSystemSettingHistoryListResDto> historyPage = adminSystemSettingHistoryRepository.getHistoryList(
                query,
                PageRequest.of(req.normalizedPage(page), req.normalizedSize(size))
        );
        AdminSystemSettingHistorySummaryDto summary = adminSystemSettingHistoryRepository.getHistorySummary(query);

        Map<Long, String> adminNameMap = adminUserRepository.findAllById(
                historyPage.getContent().stream()
                        .map(AdminSystemSettingHistoryListResDto::getCrtNo)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList()
        ).stream().collect(Collectors.toMap(AdminUser::getAdminNo, AdminUser::getName));

        return AdminSystemSettingHistoryListResponse.from(historyPage, adminNameMap, query, summary);
    }

    @Transactional
    public void saveSystemSettings(AdminSystemSettingSaveRequest req) {
        if (req.lowStockDefaultThreshold() == null || req.lowStockDefaultThreshold() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Map<String, AdminSystemSetting> settings = loadSettingMap();
        List<SettingChange> changes = List.of(
                SettingChange.of(AdminSettingDefinition.MAINTENANCE_MODE, String.valueOf(req.maintenanceMode()), settings),
                SettingChange.of(AdminSettingDefinition.COMMUNITY_WRITE_ENABLED, String.valueOf(req.communityWriteEnabled()), settings),
                SettingChange.of(AdminSettingDefinition.ORDER_EXPORT_ENABLED, String.valueOf(req.orderExportEnabled()), settings),
                SettingChange.of(AdminSettingDefinition.LOW_STOCK_DEFAULT_THRESHOLD, String.valueOf(req.lowStockDefaultThreshold()), settings)
        ).stream().filter(SettingChange::isChanged).toList();

        if (changes.isEmpty()) {
            return;
        }

        String changedIpAddress = AdminRequestContext.getCurrentIpAddress().orElse("127.0.0.1");
        for (SettingChange change : changes) {
            upsert(change.definition(), change.afterValue(), settings.get(change.definition().key()));
        }
        adminSystemSettingHistoryRepository.saveAll(changes.stream()
                .map(change -> AdminSystemSettingHistory.builder()
                        .settingKey(change.definition().key())
                        .settingName(change.definition().label())
                        .beforeValue(change.beforeValue())
                        .afterValue(change.afterValue())
                        .changeSummary(change.definition().buildChangeSummary(change.beforeValue(), change.afterValue()))
                        .changedIpAddress(changedIpAddress)
                        .build())
                .toList());
    }

    private Map<String, AdminSystemSetting> loadSettingMap() {
        return adminSystemSettingRepository.findAllBySettingKeyIn(AdminSettingDefinition.keys()).stream()
                .collect(Collectors.toMap(AdminSystemSetting::getSettingKey, Function.identity()));
    }

    private boolean readBoolean(Map<String, AdminSystemSetting> settings, AdminSettingDefinition definition) {
        AdminSystemSetting setting = settings.get(definition.key());
        return definition.parseBoolean(setting == null ? definition.defaultValue() : setting.getSettingValue());
    }

    private long readLong(Map<String, AdminSystemSetting> settings, AdminSettingDefinition definition) {
        AdminSystemSetting setting = settings.get(definition.key());
        try {
            return definition.parseLong(setting == null ? definition.defaultValue() : setting.getSettingValue());
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void upsert(AdminSettingDefinition definition, String value, AdminSystemSetting currentSetting) {
        AdminSystemSetting setting = currentSetting == null
                ? AdminSystemSetting.builder()
                        .settingKey(definition.key())
                        .settingValue(value)
                        .description(definition.description())
                        .build()
                : currentSetting;
        setting.updateValue(value);
        adminSystemSettingRepository.save(setting);
    }

    private record SettingChange(AdminSettingDefinition definition, String beforeValue, String afterValue) {

        private static SettingChange of(
                AdminSettingDefinition definition,
                String requestedValue,
                Map<String, AdminSystemSetting> settings
        ) {
            AdminSystemSetting setting = settings.get(definition.key());
            String beforeValue = setting == null ? definition.defaultValue() : definition.normalizeStoredValue(setting.getSettingValue());
            String afterValue = definition.normalizeStoredValue(requestedValue);
            return new SettingChange(definition, beforeValue, afterValue);
        }

        private boolean isChanged() {
            return !beforeValue.equals(afterValue);
        }
    }
}
