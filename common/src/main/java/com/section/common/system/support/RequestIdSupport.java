package com.section.common.system.support;

import java.util.UUID;
import java.util.regex.Pattern;

public final class RequestIdSupport {

    public static final String HEADER_NAME = "X-Request-Id";
    public static final String MDC_KEY = "requestId";
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{8,64}");

    private RequestIdSupport() {
    }

    public static String resolve(String candidate) {
        if (candidate != null) {
            String normalized = candidate.trim();
            if (SAFE_REQUEST_ID.matcher(normalized).matches()) {
                return normalized;
            }
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
