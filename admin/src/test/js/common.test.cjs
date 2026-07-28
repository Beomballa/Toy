const test = require("node:test");
const assert = require("node:assert/strict");

global.window = {
    location: {
        origin: "https://admin.grade-stock.test"
    }
};

const CommonJS = require("../../main/resources/static/js/common.js");

test("normalizeAdminReturnPath keeps only same-origin admin paths", () => {
    assert.equal(
        CommonJS.normalizeAdminReturnPath("/admin/orders/list?page=2#result", "/admin"),
        "/admin/orders/list?page=2#result"
    );
    assert.equal(
        CommonJS.normalizeAdminReturnPath("https://admin.grade-stock.test/admin/products", "/admin"),
        "/admin/products"
    );
});

test("normalizeAdminReturnPath rejects executable, external, and non-admin paths", () => {
    const fallback = "/admin/dashboard";

    assert.equal(CommonJS.normalizeAdminReturnPath("javascript:alert(1)", fallback), fallback);
    assert.equal(CommonJS.normalizeAdminReturnPath("//evil.example/admin", fallback), fallback);
    assert.equal(CommonJS.normalizeAdminReturnPath("/front/products/1", fallback), fallback);
    assert.equal(CommonJS.normalizeAdminReturnPath("/admin\\evil", fallback), fallback);
    assert.equal(CommonJS.normalizeAdminReturnPath("/admin/\u0000evil", fallback), fallback);
});
