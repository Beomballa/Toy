package com.section.common.system.dto;

public record AdminSystemSettingHistorySummaryDto(
        long totalCount,
        long todayCount,
        long maintenanceCount,
        long communityCount,
        long orderExportCount,
        long lowStockThresholdCount,
        long currentValueCount,
        long outdatedValueCount
) {
}
