package com.section.admin.log.res;

final class AdminLogTargetLinkSupport {

    private AdminLogTargetLinkSupport() {
    }

    static String resolveTargetLabel(String actionType, Long targetId) {
        if (targetId == null) {
            return "-";
        }

        if (actionType == null || actionType.isBlank()) {
            return "대상 #" + targetId;
        }

        if (actionType.startsWith("PRODUCT_")) {
            return "상품 #" + targetId;
        }
        if (actionType.startsWith("ORDER_")) {
            return "주문 #" + targetId;
        }
        if (actionType.startsWith("CONTENT_")) {
            return "게시글 #" + targetId;
        }
        if (actionType.startsWith("BANNER_")) {
            return "배너 #" + targetId;
        }
        if (actionType.startsWith("CATEGORY_")) {
            return "카테고리 #" + targetId;
        }
        if (actionType.startsWith("BRAND_")) {
            return "브랜드 #" + targetId;
        }
        if (actionType.startsWith("MEMBER_")) {
            return "회원 #" + targetId;
        }
        if (actionType.startsWith("NOTICE_")) {
            return "운영 공지 #" + targetId;
        }
        if (actionType.startsWith("TASK_")) {
            return "운영 작업 #" + targetId;
        }
        return "대상 #" + targetId;
    }

    static String resolveTargetPath(String actionType, Long targetId) {
        if (targetId == null || actionType == null || actionType.isBlank()) {
            return null;
        }

        if (actionType.startsWith("PRODUCT_")) {
            return "/admin/products/history?productNo=" + targetId;
        }
        if (actionType.startsWith("ORDER_")) {
            return "/admin/orders/history?orderNo=" + targetId;
        }
        if (actionType.startsWith("CONTENT_")) {
            return "/admin/content/get?id=" + targetId + "&boardType=NOTICE";
        }
        if (actionType.startsWith("BANNER_")) {
            return "/admin/banner/list";
        }
        if (actionType.startsWith("CATEGORY_")) {
            return "/admin/category/list";
        }
        if (actionType.startsWith("BRAND_")) {
            return "/admin/brand/list";
        }
        if (actionType.startsWith("MEMBER_")) {
            return "/admin/members";
        }
        if (actionType.startsWith("NOTICE_")) {
            return "/admin/settings/notices?noticeNo=" + targetId;
        }
        if (actionType.startsWith("TASK_")) {
            return "/admin/settings/tasks?taskNo=" + targetId;
        }
        return null;
    }
}
