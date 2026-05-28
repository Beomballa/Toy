package com.section.admin.settings.support;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum AdminSettingDefinition {
    MAINTENANCE_MODE("SYSTEM_MAINTENANCE_MODE", "유지보수 모드", "false", ValueType.BOOLEAN, "관리자 서비스 유지보수 모드"),
    COMMUNITY_WRITE_ENABLED("COMMUNITY_WRITE_ENABLED", "커뮤니티 작성 허용", "true", ValueType.BOOLEAN, "커뮤니티 글쓰기 허용"),
    ORDER_EXPORT_ENABLED("ORDER_EXPORT_ENABLED", "주문 Export 허용", "true", ValueType.BOOLEAN, "주문 export 허용"),
    LOW_STOCK_DEFAULT_THRESHOLD("LOW_STOCK_DEFAULT_THRESHOLD", "기본 저재고 임계값", "100", ValueType.LONG, "저재고 기본 임계값");

    private static final Map<String, AdminSettingDefinition> BY_KEY = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(AdminSettingDefinition::key, Function.identity()));

    private final String key;
    private final String label;
    private final String defaultValue;
    private final ValueType valueType;
    private final String description;

    AdminSettingDefinition(String key, String label, String defaultValue, ValueType valueType, String description) {
        this.key = key;
        this.label = label;
        this.defaultValue = defaultValue;
        this.valueType = valueType;
        this.description = description;
    }

    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    public String defaultValue() {
        return defaultValue;
    }

    public String description() {
        return description;
    }

    public boolean parseBoolean(String value) {
        return Boolean.parseBoolean(normalizeStoredValue(value));
    }

    public long parseLong(String value) {
        return Long.parseLong(normalizeStoredValue(value));
    }

    public String normalizeStoredValue(String value) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    public String formatValue(String value) {
        String normalized = normalizeStoredValue(value);
        return switch (valueType) {
            case BOOLEAN -> Boolean.parseBoolean(normalized) ? "활성" : "비활성";
            case LONG -> normalized;
        };
    }

    public String buildChangeSummary(String beforeValue, String afterValue) {
        return label + "이(가) " + formatValue(beforeValue) + "에서 " + formatValue(afterValue) + "(으)로 변경되었습니다.";
    }

    public static List<String> keys() {
        return Arrays.stream(values()).map(AdminSettingDefinition::key).toList();
    }

    public static AdminSettingDefinition fromKey(String key) {
        AdminSettingDefinition definition = BY_KEY.get(key);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown setting key: " + key);
        }
        return definition;
    }

    private enum ValueType {
        BOOLEAN,
        LONG
    }
}
