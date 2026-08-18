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
