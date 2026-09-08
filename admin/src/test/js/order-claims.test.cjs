const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync(require('node:path').join(__dirname, '../../main/resources/static/js/view/order-claims.js'), 'utf8');

async function renderPage(page, totalPages) {
    const nodes = new Map();
    const requests = [];
    const document = { getElementById(id) {
        if (!nodes.has(id)) nodes.set(id, { innerHTML: '', textContent: '', value: '', addEventListener() {} });
        return nodes.get(id);
    } };
    vm.runInNewContext(source, {
        document, URLSearchParams,
        location: { search: `?page=${page}`, pathname: '/admin/orders/claims' },
        history: { replaceState() {} },
        bootstrap: { Modal: class {} },
        CommonJS: {},
        fetch: async url => {
            requests.push(url);
            const requested = Number(new URL(url, 'http://localhost').searchParams.get('page'));
            return { ok: true, json: async () => ({ claims: [], page: requested, totalPages, totalElements: totalPages * 20 }) };
        }
    });
    await new Promise(resolve => setImmediate(resolve));
    return { html: document.getElementById('claimPagination').innerHTML, requests };
}

test('later claim pages remain reachable and expose the current page', async () => {
    const { html } = await renderPage(7, 12);
    assert.match(html, /data-claim-page="8"[^>]*>다음/);
    assert.match(html, /data-claim-page="11"[^>]*>마지막/);
    assert.match(html, /aria-current="page">8/);
});

test('last claim page disables forward navigation', async () => {
    const { html } = await renderPage(11, 12);
    assert.match(html, /data-claim-page="12" disabled[^>]*>다음/);
    assert.match(html, /data-claim-page="11" disabled[^>]*>마지막/);
});

test('a removed final page reloads the last existing page', async () => {
    const { requests, html } = await renderPage(8, 3);
    assert.equal(requests.length, 2);
    assert.match(requests[1], /page=2$/);
    assert.match(html, /aria-current="page">3/);
});
