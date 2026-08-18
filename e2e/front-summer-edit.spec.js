import { expect, test } from "@playwright/test";

const product = (id, name) => ({
  id,
  name,
  brand: "NOREN",
  category: "셔츠",
  price: 129000,
  thumbnailUrl: "/images/product-placeholder.svg"
});

const pageResponse = (products) => ({
  products,
  pagination: {
    page: 0,
    size: 8,
    totalElements: products.length,
    totalPages: products.length ? 1 : 0,
    first: true,
    last: true
  }
});

test("여름 에디트는 미디어를 제어하고 안전한 상품 카드로 연결한다", async ({ page }) => {
  await page.route("**/api/front/products?**", route => route.fulfill({
    json: pageResponse([product(1, "여름 셔츠"), product(2, "여름 팬츠")])
  }));

  await page.goto("/front/events/summer-edit");
  await expect(page.locator(".summer-product-card")).toHaveCount(2);
  await expect(page.locator("#summerFrameStatus")).toHaveText("01 / 03");
  await page.locator("#summerNextFrame").click();
  await expect(page.locator("#summerFrameStatus")).toHaveText("02 / 03");
  await expect(page.locator("#summerHeroMedia img.is-active")).toHaveCount(1);
  await page.locator("#summerMediaToggle").click();
  await expect(page.locator("#summerMediaToggle")).toHaveAttribute("aria-pressed", "true");
  await expect(page.locator("#summerMediaToggle")).toHaveText("Play");
  await expect(page.locator(".summer-product-card a").first()).toHaveAttribute("href", "/front/products/1");
});

test("여름 에디트는 상품 응답 실패를 노출하고 재시도하며 모바일 폭을 유지한다", async ({ page }) => {
  let attempts = 0;
  await page.route("**/api/front/products?**", route => {
    attempts += 1;
    if (attempts === 1) return route.fulfill({ status: 503, json: { message: "temporary" } });
    return route.fulfill({ json: pageResponse([product(3, "재시도 셔츠")]) });
  });

  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/front/events/summer-edit");
  await expect(page.locator("#summerProductRetry")).toBeVisible();
  await page.locator("#summerProductRetry").click();
  await expect(page.locator(".summer-product-card")).toHaveCount(1);
  await expect(page.locator("#summerProductGrid")).toHaveAttribute("aria-busy", "false");
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy();
  for (const selector of [".summer-hero", ".summer-benefits", ".summer-products__grid"]) {
    const box = await page.locator(selector).boundingBox();
    expect(box.x).toBeGreaterThanOrEqual(0);
    expect(box.x + box.width).toBeLessThanOrEqual(390);
  }
});
