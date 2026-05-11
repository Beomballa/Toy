package com.section.admin.settings.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AdminSystemSettingSaveRequest(
        @NotNull Boolean maintenanceMode,
        @NotNull Boolean communityWriteEnabled,
        @NotNull Boolean orderExportEnabled,
        @NotNull @Positive Long lowStockDefaultThreshold
) {
}
