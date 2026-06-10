package com.section.admin.task.support;

public final class AdminTaskLinkSupport {

    private AdminTaskLinkSupport() {
    }

    public static String buildListOpenPath(Long taskNo, String returnTo, String source) {
        if (taskNo == null) {
            return "/admin/settings/tasks";
        }

        StringBuilder builder = new StringBuilder("/admin/settings/tasks")
                .append("?taskNo=").append(taskNo)
                .append("&openTaskNo=").append(taskNo)
                .append("&focusTaskNo=").append(taskNo);
        appendQuery(builder, "returnTo", returnTo);
        appendQuery(builder, "source", source);
        return builder.toString();
    }

    private static void appendQuery(StringBuilder builder, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append("&")
                .append(key)
                .append("=")
                .append(java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8));
    }
}
