import { expect, test } from "@playwright/test";

const detail = (id) => ({
  id,
  name: `비교 상품 ${id}`,
  brand: "Grade",
  model: `GS-${id}`,
  category: "스니커즈",
  price: 100000 + id * 1000,
  priceLabel: `${100 + id},000원`,
  stock: 10 + id,
  stockStatus: "재고 안정",
  options: [{ id: id * 10, name: "260", stock: 10 + id, additionalPrice: 0 }]
});

test("비교 화면은 상품 ID와 옵션 재고가 불일치한 응답을 표시하지 않는다", async ({ page }) => {
  let attempts = 0;
  await page.route("**/api/front/products/*", async (route) => {
    const requestedId = Number(new URL(route.request().url()).pathname.split("/").pop());
    const response = detail(attempts++ < 2 ? requestedId + 100 : requestedId);
    if (attempts > 2) response.thumbnailUrl = "javascript:alert(1)";
    await route.fulfill({ json: response });
  });

  await page.goto("/front/compare?ids=1,2");
  await expect(page.locator("#comparisonEmptyDescription")).toContainText("2개 상품 정보를 불러오지 못했습니다");
  await expect(page.locator(".comparison-product-head")).toHaveCount(0);

  await page.locator("#comparisonEmptyRetryButton").click();
  await expect(page.locator(".comparison-product-head")).toHaveCount(2);
  await expect(page.locator(".comparison-product-head img").first()).toHaveAttribute("src", "/images/product-placeholder.svg");
});

test("비교 상품 조회 실패를 가짜 0원 상품으로 표시하지 않고 재시도한다", async ({ page }) => {
  let secondAttempts = 0;
  await page.route("**/api/front/products/*", async (route) => {
    const id = Number(new URL(route.request().url()).pathname.split("/").pop());
    if (id === 2 && secondAttempts++ === 0) {
      await route.fulfill({ status: 503, json: { message: "temporary" } });
      return;
    }
    await route.fulfill({ json: detail(id) });
  });

  await page.goto("/front/compare?ids=1,2");
  await expect(page.locator("#comparisonEmpty")).toBeVisible();
  await expect(page.locator("#comparisonEmptyDescription")).toContainText("1개 상품 정보를 불러오지 못했습니다");
  await expect(page.locator("#comparisonEmpty")).not.toContainText("0원");

  await page.locator("#comparisonEmptyRetryButton").click();
  await expect(page.locator("#comparisonWorkspace")).toBeVisible();
  await expect(page.locator(".comparison-product-head")).toHaveCount(2);
  await expect(page.locator("#comparisonResultText")).toContainText("2개 상품");
  await expect(page.locator("#comparisonRefreshButton")).not.toHaveAttribute("aria-busy");
});

test("비교 제어와 긴 추천 문구는 320px 화면에서 겹치지 않는다", async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 844 });
  await page.route("**/api/front/products/*", async route => {
    const id = Number(new URL(route.request().url()).pathname.split("/").pop());
    await route.fulfill({ json: {
      ...detail(id),
      name: `긴 상품명 반응형 비교 제어 영역 검증용 상품 ${id}`,
      brand: "NOREN-LONG-BRAND-NAME"
    } });
  });

  await page.goto("/front/compare?ids=1,2");
  for (const selector of [".comparison-toolbar", ".comparison-toolbar__actions", ".comparison-heading", ".comparison-heading > div:last-child", ".comparison-recommendation"]) {
    const box = await page.locator(selector).boundingBox();
    expect(box.x).toBeGreaterThanOrEqual(0);
    expect(box.x + box.width).toBeLessThanOrEqual(320);
  }
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy();
});
