const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync(require('node:path').join(__dirname, '../../main/resources/static/js/view/order-claims.js'), 'utf8');

async function renderPage(page, totalPages, patchResponse, responseOverrides = {}) {
    const nodes = new Map();
    const requests = [];
    const document = { getElementById(id) {
        if (!nodes.has(id)) nodes.set(id, { innerHTML: '', textContent: '', value: '', events: {}, addEventListener(name, handler) { this.events[name] = handler; } });
        return nodes.get(id);
    } };
    vm.runInNewContext(source, {
        document, URLSearchParams,
        location: { search: `?page=${page}`, pathname: '/admin/orders/claims' },
        history: { replaceState() {} },
        bootstrap: { Modal: class { show() {} hide() { document.getElementById('claimActionModal').events['hidden.bs.modal'](); } } },
        CommonJS: { alert: async () => {}, extractErrorMessage: async () => 'failed' },
        fetch: async (url, options) => {
            if (options?.method === 'PATCH') return patchResponse(url, options);
            requests.push(url);
            const requested = Number(new URL(url, 'http://localhost').searchParams.get('page'));
            return { ok: true, json: async () => ({ claims: [], page: requested, totalPages, totalElements: totalPages * 20, ...responseOverrides }) };
        }
    });
    await new Promise(resolve => setImmediate(resolve));
    return { html: document.getElementById('claimPagination').innerHTML, requests, nodes };
}

function openAction(nodes, claimNo = '42') {
    nodes.get('claimTableBody').events.click({ target: { closest: () => ({ dataset: { claimNo, claimAction: 'APPROVED' } }) } });
    nodes.get('claimActionMemo').value = '승인 안내';
}

test('pending action blocks repeated submits, closing and target changes', async () => {
    let finish;
    const writes = [];
    const { nodes } = await renderPage(0, 1, (url, options) => {
        writes.push(JSON.parse(options.body));
        return new Promise(resolve => { finish = resolve; });
    });
    openAction(nodes);
    const submit = () => nodes.get('claimActionForm').events.submit({ preventDefault() {} });
    const pending = submit();
    assert.equal(nodes.get('claimActionSubmitButton').disabled, true);
    await submit();
    openAction(nodes, '99');
    let prevented = false;
    nodes.get('claimActionModal').events['hide.bs.modal']({ preventDefault() { prevented = true; } });
    assert.equal(prevented, true);
    assert.equal(writes.length, 1);
    assert.equal(writes[0].claimNo, 42);
    finish({ ok: true });
    await pending;
    await submit();
    assert.equal(writes.length, 1);
    assert.equal(nodes.get('claimActionSubmitButton').disabled, false);
});

test('failed action unlocks the form and preserves its target for retry', async () => {
    let attempts = 0;
    const { nodes } = await renderPage(0, 1, async () => ({ ok: ++attempts > 1 }));
    openAction(nodes);
    const submit = () => nodes.get('claimActionForm').events.submit({ preventDefault() {} });
    await submit();
    assert.equal(nodes.get('claimActionSubmitButton').disabled, false);
    await submit();
    assert.equal(attempts, 2);
});

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

test('invalid claim identifiers cannot become links or action attributes', async () => {
    for (const field of ['claimNo', 'orderNo', 'memberNo']) {
        const claim = { claimNo: 1, orderNo: 2, memberNo: 3, status: 'REQUESTED', [field]: '\"><img src=x onerror=alert(1)>' };
        const { nodes, html } = await renderPage(0, 1, undefined, { claims: [claim] });
        assert.match(nodes.get('claimTableBody').innerHTML, /요청 목록 정보가 올바르지 않습니다/);
        assert.doesNotMatch(nodes.get('claimTableBody').innerHTML, /<img|data-claim-action|href=/);
        assert.equal(nodes.get('claimTotalCount').textContent, '-');
        assert.equal(html, '');
    }
});

test('valid claim renders actions while escaping customer text', async () => {
    const { nodes } = await renderPage(0, 1, undefined, { claims: [{
        claimNo: 42, orderNo: 23, memberNo: 7, status: 'REQUESTED',
        reason: '<img src=x onerror=alert(1)>', orderNumber: 'GS&23'
    }] });
    const html = nodes.get('claimTableBody').innerHTML;
    assert.match(html, /href="\/admin\/orders\/get\?no=23"/);
    assert.match(html, /data-claim-no="42"/);
    assert.match(html, /&lt;img src=x onerror=alert\(1\)&gt;/);
    assert.match(html, /GS&amp;23/);
    assert.doesNotMatch(html, /<img/);
});

test('mismatched page and invalid count are rejected instead of showing stale metadata', async () => {
    for (const response of [{ page: 3 }, { totalElements: -1 }, { claims: null }]) {
        const { nodes } = await renderPage(0, 4, undefined, response);
        assert.match(nodes.get('claimResultMeta').textContent, /조회하지 못했습니다/);
        assert.equal(nodes.get('claimTotalCount').textContent, '-');
    }
});
