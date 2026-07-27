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
});
