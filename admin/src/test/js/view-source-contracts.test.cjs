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
