import { expect, test } from "@playwright/test";

const product = (id, name) => ({
  id,
  name,
  brand: "Grade",
  category: "스니커즈",
  modelNumber: `GS-${id}`,
  price: 129000,
  priceLabel: "129,000원",
  stock: 12,
  stockStatus: "재고 안정",
  thumbnailUrl: "/images/product-placeholder.svg"
});

const pageResponse = (products) => ({
  products,
  pagination: {
    page: 0,
    size: 20,
    totalElements: products.length,
    totalPages: products.length ? 1 : 0,
    first: true,
    last: true
  }
});

const multiPageResponse = (products, page) => ({
  ...pageResponse(products),
  pagination: { page, size: 20, totalElements: 40, totalPages: 2, first: page === 0, last: page === 1 }
});

test("컬렉션은 최신 검색 응답만 표시하고 실패한 요청을 재시도한다", async ({ page }) => {
  let retryCount = 0;

  await page.route("**/api/front/products?**", async (route) => {
    const keyword = new URL(route.request().url()).searchParams.get("keyword");
    if (keyword === "slow") {
      await new Promise((resolve) => setTimeout(resolve, 250));
      await route.fulfill({ json: pageResponse([product(1, "느린 검색 상품")]) });
      return;
    }
    if (keyword === "retry") {
      retryCount += 1;
      if (retryCount === 1) {
        await route.fulfill({ status: 503, json: { message: "temporary" } });
        return;
      }
      await route.fulfill({ json: pageResponse([product(3, "재시도 성공 상품")]) });
      return;
    }
    await route.fulfill({
      json: pageResponse([product(2, keyword === "fast" ? "빠른 검색 상품" : "기본 상품")])
    });
  });

  await page.goto("/front/collections/new");
  const search = page.locator("#collectionSearchInput");

  await search.fill("slow");
  await page.locator("#collectionSearchForm").evaluate((form) => form.requestSubmit());
  await search.fill("fast");
  await page.locator("#collectionSearchForm").evaluate((form) => form.requestSubmit());

  await expect(page.locator(".collection-product h2")).toHaveText("빠른 검색 상품");
  await expect(page.locator(".collection-grid")).not.toContainText("느린 검색 상품");

  await search.fill("retry");
  await page.locator("#collectionSearchForm").evaluate((form) => form.requestSubmit());
  await expect(page.locator("[data-collection-retry]")).toBeVisible();
  await page.locator("[data-collection-retry]").click();

  await expect(page.locator(".collection-product h2")).toHaveText("재시도 성공 상품");
  await expect(page.locator("#collectionGrid")).toHaveAttribute("aria-busy", "false");

  const layout = await page.evaluate(() => {
    const viewportWidth = document.documentElement.clientWidth;
    const toolbar = document.querySelector("#collectionSearchForm").getBoundingClientRect();
    const grid = document.querySelector("#collectionGrid").getBoundingClientRect();
    return {
      bodyOverflow: document.documentElement.scrollWidth - viewportWidth,
      toolbarWidth: toolbar.width,
      toolbarLeft: toolbar.left,
      toolbarRight: toolbar.right,
      gridWidth: grid.width,
      viewportWidth
    };
  });

  expect(layout.bodyOverflow).toBe(0);
  expect(layout.toolbarWidth).toBeGreaterThan(0);
  expect(layout.toolbarLeft).toBeGreaterThanOrEqual(0);
  expect(layout.toolbarRight).toBeLessThanOrEqual(layout.viewportWidth);
  expect(layout.gridWidth).toBeGreaterThan(0);
});

test("컬렉션 상세 필터는 URL과 요청 조건을 동기화하고 개별 조건을 해제한다", async ({ page }) => {
  const requests = [];
  await page.route("**/api/front/products?**", async (route) => {
    requests.push(new URL(route.request().url()));
    await route.fulfill({ json: pageResponse([product(10, "필터 상품")]) });
  });

  await page.goto("/front/collections/new");
  await page.locator("#collectionFilterButton").click();
  await expect(page.locator("#collectionFilterDialog")).toBeVisible();
  await page.locator("#collectionBrandInput").fill("나이키");
  await page.locator("#collectionCategoryInput").fill("스니커즈");
  await page.locator("#collectionStockSelect").selectOption("STABLE");
  await page.locator("#collectionPriceBandSelect").selectOption("UNDER_200");
  await page.locator("#collectionLowStockThreshold").selectOption("10");
  await page.locator("#collectionFeaturedOnly").check();
  await page.locator("#collectionFilterForm").evaluate((form) => form.requestSubmit());

  await expect(page.locator("#collectionFilterDialog")).toBeHidden();
  await expect(page.locator("#collectionFilterCount")).toHaveText("6");
  await expect(page).toHaveURL(/brand=%EB%82%98%EC%9D%B4%ED%82%A4/);
  await expect(page).toHaveURL(/featuredOnly=true/);
  const lastRequest = requests.at(-1).searchParams;
  expect(lastRequest.get("brand")).toBe("나이키");
  expect(lastRequest.get("category")).toBe("스니커즈");
  expect(lastRequest.get("stock")).toBe("STABLE");
  expect(lastRequest.get("priceBand")).toBe("UNDER_200");
  expect(lastRequest.get("lowStockThreshold")).toBe("10");
  expect(lastRequest.get("featuredOnly")).toBe("true");

  await page.locator('[data-filter-reset="brand"]').click();
  await expect(page.locator("#collectionFilterCount")).toHaveText("5");
  await expect(page).not.toHaveURL(/brand=/);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy();
});

test("컬렉션 빠른 보기는 상세 옵션을 표시하고 닫은 뒤 카드로 포커스를 돌린다", async ({ page }) => {
  await page.route("**/api/front/products?**", route => route.fulfill({ json: pageResponse([product(21, "빠른 보기 상품")]) }));
  await page.route("**/api/front/products/21", route => route.fulfill({ json: {
    ...product(21, "빠른 보기 상품"),
    headline: "빠른 확인",
    options: [{ id: 1, name: "260", stock: 3, additionalPrice: 10000 }]
  } }));

  await page.goto("/front/collections/new");
  const trigger = page.locator('[data-quick-view-id="21"]');
  await trigger.click();
  await expect(page.locator("#collectionQuickView")).toBeVisible();
  await expect(page.locator("#collectionQuickViewContent")).toContainText("260 · 3개 · +10,000원");
  await page.locator("#collectionQuickViewClose").click();
  await expect(page.locator("#collectionQuickView")).toBeHidden();
  await expect(trigger).toBeFocused();
});

test("컬렉션 빠른 보기는 상세 조회 실패 후 모달 안에서 재시도한다", async ({ page }) => {
  let requests = 0;
  await page.route("**/api/front/products?**", route => route.fulfill({ json: pageResponse([product(22, "재시도 상품")]) }));
  await page.route("**/api/front/products/22", route => {
    requests += 1;
    return requests === 1
      ? route.fulfill({ status: 503, json: { message: "temporary" } })
      : route.fulfill({ json: { ...product(22, "재시도 상품"), options: [] } });
  });
  await page.goto("/front/collections/new");
  await page.locator('[data-quick-view-id="22"]').click();
  await expect(page.locator("[data-quick-view-retry]")).toBeVisible();
  await page.locator("[data-quick-view-retry]").click();
  await expect(page.locator("#collectionQuickViewContent")).toContainText("등록된 옵션이 없습니다");
  expect(requests).toBe(2);
});

test("열린 빠른 보기에서 다른 상품을 선택하면 최신 상품 정보로 교체한다", async ({ page }) => {
  await page.route("**/api/front/products?**", route => route.fulfill({ json: pageResponse([
    product(31, "첫 번째 빠른 보기 상품"),
    product(32, "두 번째 빠른 보기 상품")
  ]) }));
  await page.route("**/api/front/products/31", route => route.fulfill({ json: { ...product(31, "첫 번째 빠른 보기 상품"), options: [] } }));
  await page.route("**/api/front/products/32", route => route.fulfill({ json: { ...product(32, "두 번째 빠른 보기 상품"), options: [] } }));

  await page.goto("/front/collections/new");
  await page.locator('[data-quick-view-id="31"]').click();
  await expect(page.locator("#collectionQuickViewContent")).toContainText("첫 번째 빠른 보기 상품");
  // dialog의 inert 처리 밖에서, 위임된 클릭 핸들러의 요청 교체 경로를 검증한다.
  await page.locator('[data-quick-view-id="32"]').dispatchEvent("click");

  await expect(page.locator("#collectionQuickView")).toBeVisible();
  await expect(page.locator("#collectionQuickViewContent")).toContainText("두 번째 빠른 보기 상품");
  await expect(page.locator("#collectionQuickViewContent")).not.toContainText("첫 번째 빠른 보기 상품");
});

test("컬렉션은 페이지 선택으로 서버 20개 단위 페이지를 이동한다", async ({ page }) => {
  const requestedPages = [];
  await page.route("**/api/front/products?**", route => {
    const requestPage = Number(new URL(route.request().url()).searchParams.get("page"));
    requestedPages.push(requestPage);
    return route.fulfill({ json: multiPageResponse([product(requestPage + 41, `${requestPage + 1} 페이지 상품`)], requestPage) });
  });
  await page.goto("/front/collections/new");
  await expect(page.locator("#collectionPageSelect")).toHaveValue("0");
  await page.locator("#collectionPageSelect").selectOption("1");
  await expect(page.locator(".collection-product h2")).toHaveText("2 페이지 상품");
  expect(requestedPages).toEqual([0, 1]);
});

test("컬렉션은 처음과 마지막 페이지로 바로 이동한다", async ({ page }) => {
  const requestedPages = [];
  await page.route("**/api/front/products?**", route => {
    const requestPage = Number(new URL(route.request().url()).searchParams.get("page"));
    requestedPages.push(requestPage);
    return route.fulfill({ json: { ...multiPageResponse([product(requestPage + 51, `${requestPage + 1} 페이지 상품`)], requestPage), pagination: { page: requestPage, size: 20, totalElements: 60, totalPages: 3, first: requestPage === 0, last: requestPage === 2 } } });
  });
  await page.goto("/front/collections/new");
  await page.locator("#collectionLastButton").click();
  await expect(page.locator("#collectionPageSelect")).toHaveValue("2");
  await page.locator("#collectionFirstButton").click();
  await expect(page.locator("#collectionPageSelect")).toHaveValue("0");
  expect(requestedPages).toEqual([0, 2, 0]);
});

test("컬렉션 카드는 동일한 행동 영역 폭으로 뷰포트 안에 정렬한다", async ({ page }) => {
  await page.route("**/api/front/products?**", route => route.fulfill({ json: pageResponse([
    product(61, "한 줄 상품"),
    product(62, "두 줄로 길어지는 컬렉션 상품 이름"),
    product(63, "정렬 확인 상품"),
    product(64, "모바일 확인 상품")
  ]) }));

  await page.goto("/front/collections/new");
  await expect(page.locator(".collection-product")).toHaveCount(4);

  const layout = await page.evaluate(() => {
    const viewportWidth = document.documentElement.clientWidth;
    const cards = [...document.querySelectorAll(".collection-product")];
    const actionRows = cards.map((card) => {
      const actions = [...card.querySelectorAll(".collection-product__actions > *")]
        .map((action) => action.getBoundingClientRect());
      return {
        left: card.getBoundingClientRect().left,
        right: card.getBoundingClientRect().right,
        actionWidths: actions.map((action) => action.width),
        actionRight: actions.map((action) => action.right)
      };
    });
    return {
      bodyOverflow: document.documentElement.scrollWidth - viewportWidth,
      viewportWidth,
      actionRows
    };
  });

  expect(layout.bodyOverflow).toBe(0);
  for (const row of layout.actionRows) {
    expect(row.left).toBeGreaterThanOrEqual(0);
    expect(row.right).toBeLessThanOrEqual(layout.viewportWidth);
    expect(row.actionWidths).toHaveLength(2);
    expect(Math.abs(row.actionWidths[0] - row.actionWidths[1])).toBeLessThanOrEqual(1);
    expect(Math.max(...row.actionRight)).toBeLessThanOrEqual(row.right + 1);
  }
});
