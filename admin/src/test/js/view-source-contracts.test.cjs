const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

function readViewScript(fileName) {
    return fs.readFileSync(
        path.resolve(__dirname, "../../main/resources/static/js/view", fileName),
        "utf8"
    );
}

test("category controls run before the parent-card click handler", () => {
    const source = readViewScript("category-list.js");

    assert.ok(source.indexOf("const editRootButton") < source.indexOf("const parentItem"));
    assert.ok(source.indexOf("const toggleRootButton") < source.indexOf("const parentItem"));
});

test("category rows use number-based lookup and reject stale list responses", () => {
    const source = readViewScript("category-list.js");

    assert.equal(source.includes("data-category='"), false);
    assert.equal(source.includes("JSON.stringify(item)"), false);
    assert.match(source, /requestId !== this\.depth1RequestId/);
    assert.match(source, /requestId !== this\.depth2RequestId/);
});

test("order filtering accepts a new request and ignores an older response", () => {
    const source = readViewScript("order-list.js");

    assert.equal(source.includes("if (this.isLoading)"), false);
    assert.match(source, /const requestId = \+\+this\.listRequestId/);
    assert.match(source, /requestId !== this\.listRequestId/);
});

test("member list and detail discard responses from an older screen state", () => {
    const source = readViewScript("member-list.js");

    assert.match(source, /const requestId = \+\+this\.listRequestId/);
    assert.match(source, /const requestId = \+\+this\.detailRequestId/);
    assert.match(source, /requestId !== this\.listRequestId/);
    assert.match(source, /requestId !== this\.detailRequestId/);
});

test("banner and activity log lists keep only the latest filter response", () => {
    for (const fileName of ["banner-list.js", "admin-logs.js"]) {
        const source = readViewScript(fileName);
        assert.match(source, /const requestId = \+\+this\.listRequestId/);
        assert.match(source, /requestId !== this\.listRequestId/);
    }
});

test("notice rows use number-based lookup, safe paths, and latest responses", () => {
    const source = readViewScript("notice-list.js");

    assert.equal(source.includes("data-notice='"), false);
    assert.equal(source.includes("parseNoticeDataset"), false);
    assert.match(source, /CommonJS\.normalizeAdminReturnPath/);
    assert.match(source, /requestId !== this\.listRequestId/);
});

test("task rows use the server HOLD code and number-based lookup", () => {
    const source = readViewScript("task-list.js");

    assert.equal(source.includes("ON_HOLD"), false);
    assert.equal(source.includes("data-task='"), false);
    assert.equal(source.includes("parseTaskDataset"), false);
    assert.match(source, /requestId !== this\.listRequestId/);
    assert.match(source, /CommonJS\.normalizeAdminReturnPath/);
});

test("task workload accepts newer filters and validates server paths", () => {
    const source = readViewScript("task-workload-list.js");

    assert.equal(source.includes("if (this.isLoading)"), false);
    assert.match(source, /requestId !== this\.listRequestId/);
    assert.match(source, /CommonJS\.normalizeAdminReturnPath/);
    assert.match(source, /this\.escapeHtml\(this\.buildEmptyStateMessage\(\)\)/);
});

test("product front display validates filters and keeps the latest response", () => {
    const source = readViewScript("product-front-display.js");

    assert.match(source, /requestId !== this\.listRequestId/);
    assert.match(source, /Number\.isInteger\(parsed\)/);
    assert.match(source, /\['', 'ACTIVE', 'HIDDEN', 'SOLD_OUT'\]/);
    assert.equal(source.includes("backButton.innerHTML"), false);
});

test("task workload detail rejects stale responses and unsafe identifiers", () => {
    const source = readViewScript("task-workload-get.js");

    assert.match(source, /requestId !== this\.detailRequestId/);
    assert.match(source, /requestId !== this\.reassignRequestId/);
    assert.match(source, /CommonJS\.normalizeAdminReturnPath\(basePath, ''\)/);
    assert.match(source, /safeOptions\.flatMap/);
    assert.equal(source.includes("returnButton.innerHTML"), false);
});

test("task and notice histories keep latest responses and escape log fields", () => {
    const taskSource = readViewScript("task-history.js");
    const noticeSource = readViewScript("notice-history.js");

    for (const source of [taskSource, noticeSource]) {
        assert.match(source, /requestId !== this\.listRequestId/);
        assert.match(source, /requestId !== this\.detailRequestId/);
        assert.match(source, /CommonJS\.normalizeAdminReturnPath\(basePath, ''\)/);
        assert.match(source, /this\.escapeHtml\(data\.ipAddress/);
        assert.match(source, /this\.normalizeOptionalPositiveNumber\(item\.logNo\)/);
    }
});

test("product and order histories validate rows and keep the latest response", () => {
    const productSource = readViewScript("product-history.js");
    const orderSource = readViewScript("order-history.js");

    for (const source of [productSource, orderSource]) {
        assert.match(source, /requestId !== this\.listRequestId/);
        assert.match(source, /Array\.isArray\(data\.items\)/);
        assert.match(source, /this\.escapeHtml\(this\.buildEmptyStateMessage\(\)\)/);
        assert.match(source, /this\.normalizeOptionalPositiveNumber\(item\.historyNo\)/);
    }
    assert.match(productSource, /CommonJS\.normalizeAdminReturnPath\(basePath, ''\)/);
    assert.match(productSource, /this\.escapeHtml\(item\.summary/);
    assert.match(orderSource, /this\.normalizeOptionalPositiveNumber\(item\.orderNo\)/);
});

test("notice and task details validate navigation and latest responses", () => {
    const noticeSource = readViewScript("notice-get.js");
    const taskSource = readViewScript("task-get.js");

    for (const source of [noticeSource, taskSource]) {
        assert.match(source, /requestId !== this\.detailRequestId/);
        assert.match(source, /CommonJS\.normalizeAdminReturnPath\(basePath, ''\)/);
        assert.match(source, /syncNavigationLink\(elementId, path\)/);
    }
    assert.match(taskSource, /CommonJS\.normalizeAdminReturnPath\(data\.sourcePath, ''\)/);
    assert.match(taskSource, /options\.flatMap/);
    assert.match(taskSource, /this\.normalizePositiveNumber\(item\.commentNo\)/);
    assert.match(noticeSource, /this\.buildHistoryPathFromBase\(item\.historyPath\)/);
});

test("order detail validates the latest response, items, and histories", () => {
    const source = readViewScript("order-get.js");

    assert.match(source, /requestId !== this\.detailRequestId/);
    assert.match(source, /this\.normalizeOrderNo\(data\.orderNo \|\| this\.orderNo\) !== this\.orderNo/);
    assert.match(source, /Array\.isArray\(data\.histories\)/);
    assert.match(source, /this\.normalizeOrderNo\(item\.productNo\)/);
    assert.match(source, /this\.normalizePositiveInteger\(item\.count\)/);
    assert.match(source, /this\.normalizeOrderNo\(history\.historyNo\)/);
});

test("product create and update validate async data and partial saves", () => {
    const createSource = readViewScript("product-set.js");
    const updateSource = readViewScript("product-update.js");

    assert.match(createSource, /requestId !== this\.rankGuideRequestId/);
    assert.match(createSource, /this\.normalizePositiveNumber\(result\.productNo\)/);
    assert.match(createSource, /this\.navigateToDetail\(productNo\)/);
    assert.match(createSource, /CommonJS\.normalizeImageSource\(e\.target\.value\)/);
    assert.equal(createSource.includes("backButton.innerHTML"), false);

    assert.match(updateSource, /requestId !== this\.productRequestId/);
    assert.match(updateSource, /requestId !== this\.displayRequestId/);
    assert.match(updateSource, /requestId !== this\.rankGuideRequestId/);
    assert.match(updateSource, /Array\.isArray\(data\.options\)/);
    assert.match(updateSource, /CommonJS\.normalizeImageSource/);
});

test("content detail and edit validate document identity and latest responses", () => {
    const detailSource = readViewScript("content-get.js");
    const editSource = readViewScript("content-edit.js");

    for (const source of [detailSource, editSource]) {
        assert.match(source, /requestId !== this\.detailRequestId/);
        assert.equal(source.includes("backButton.innerHTML"), false);
    }
    assert.match(detailSource, /this\.normalizeContentId\(data\.id\) !== this\.state\.id/);
    assert.match(detailSource, /this\.formatNumber\(data\.viewCnt\)/);
    assert.match(detailSource, /this\.normalizeOptionalProductNo\(data\.productNo\)/);
    assert.match(editSource, /this\.normalizeContentId\(saved\?\.id\)/);
    assert.match(editSource, /savedId !== this\.id/);
});

test("dashboard validates stats, identifiers, output, and navigation", () => {
    const source = readViewScript("dashboard/dashboard-list.js");

    assert.match(source, /requestId !== this\.statsRequestId/);
    assert.match(source, /Array\.isArray\(data\.recentOrders\)/);
    assert.match(source, /CommonJS\.normalizeAdminReturnPath\(action\?\.detailPath, ''\)/);
    assert.match(source, /CommonJS\.normalizeAdminReturnPath\(basePath, ''\)/);
    assert.match(source, /this\.normalizePositiveId\(order\.orderNo\)/);
    assert.match(source, /this\.normalizePositiveId\(product\.productNo\)/);
    assert.match(source, /this\.escapeHtml\(order\.statusDesc/);
});

test("content list validates latest results, cards, and task paths", () => {
    const source = readViewScript("content-list.js");

    assert.match(source, /requestId !== this\.listRequestId/);
    assert.match(source, /Array\.isArray\(data\.items\)/);
    assert.match(source, /this\.normalizeNumericId\(item\.documentId\)/);
    assert.match(source, /CommonJS\.normalizeAdminReturnPath\(item\.operationTaskPath, ''\)/);
    assert.match(source, /this\.normalizeNumericId\(item\.id\)/);
    assert.match(source, /ContentBoardConfig\.normalizeBoardType\(item\.boardType\)/);
    assert.match(source, /this\.normalizeNumericId\(item\.productNo\)/);
    assert.match(source, /this\.formatNumber\(item\.viewCnt\)/);
    assert.match(source, /ContentBoardConfig\.escapeHtml\(item\.crtDtm/);
    assert.match(source, /ContentBoardConfig\.escapeHtml\(this\.buildEmptyStateMessage\(\)\)/);
});

test("product detail validates async sections, identifiers, and output", () => {
    const source = readViewScript("product-get.js");

    assert.match(source, /requestId !== this\.detailRequestId/);
    assert.match(source, /requestId !== this\.displayRequestId/);
    assert.match(source, /requestId !== this\.historyRequestId/);
    assert.match(source, /this\.normalizeProductNo\(data\.productNo\) !== this\.productNo/);
    assert.match(source, /this\.normalizeProductNo\(data\.productNo\)/);
    assert.match(source, /Array\.isArray\(data\.options\)/);
    assert.match(source, /Array\.isArray\(payload\)/);
    assert.match(source, /this\.normalizeProductNo\(history\.actorNo\)/);
    assert.match(source, /this\.formatCount\(history\.optionCount\)/);
    assert.equal(source.includes("backButton.innerHTML"), false);
});

test("product list validates rows, metadata, output, and reset navigation", () => {
    const source = readViewScript("product-list.js");

    assert.match(source, /Array\.isArray\(data\.products\)/);
    assert.match(source, /Array\.isArray\(items\)/);
    assert.match(source, /this\._normalizeOptionalPositiveNumber\(item\?\.productNo\)/);
    assert.match(source, /this\._normalizeNonNegativeInteger\(data\?\.totalPages\)/);
    assert.match(source, /this\._formatCurrency\(item\.releasePrice\)/);
    assert.match(source, /this\._formatCount\(item\.totalStock\)/);
    assert.match(source, /CommonJS\.escapeHtml\(message\)/);
    assert.match(source, /source: this\.state\.source/);
    assert.match(source, /returnTo: this\.state\.returnTo/);
    assert.match(source, /const clonedProductNo = this\._normalizeOptionalPositiveNumber/);
    assert.match(source, /const safeLabel = CommonJS\.escapeHtml/);
});
