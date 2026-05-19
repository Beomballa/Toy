package com.section.common.system.support;

import java.util.Optional;

public final class AdminRequestContext {

    private static final ThreadLocal<Long> CURRENT_ADMIN_NO = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_IP_ADDRESS = new ThreadLocal<>();

    private AdminRequestContext() {
    }

    public static void setCurrentAdminNo(Long adminNo) {
        CURRENT_ADMIN_NO.set(adminNo);
    }

    public static Optional<Long> getCurrentAdminNo() {
        return Optional.ofNullable(CURRENT_ADMIN_NO.get());
    }

    public static void setCurrentIpAddress(String ipAddress) {
        CURRENT_IP_ADDRESS.set(ipAddress);
    }

    public static Optional<String> getCurrentIpAddress() {
        return Optional.ofNullable(CURRENT_IP_ADDRESS.get());
    }

    public static void clear() {
        CURRENT_ADMIN_NO.remove();
        CURRENT_IP_ADDRESS.remove();
    }
}
