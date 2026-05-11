package com.section.admin.settings.res;

public record AdminSystemSettingResponse(
        boolean maintenanceMode,
        boolean communityWriteEnabled,
        boolean orderExportEnabled,
        long lowStockDefaultThreshold
) {
}
