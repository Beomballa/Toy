package com.section.admin.content.support;

public final class ContentPreviewSanitizer {
    private ContentPreviewSanitizer() {
    }

    public static String sanitize(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        String withoutCodeBlocks = content
                .replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ");
        String withoutTags = withoutCodeBlocks.replaceAll("(?is)<[^>]+>", " ");
        String decoded = withoutTags
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
        return decoded.replaceAll("\\s+", " ").trim();
    }
}
