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
