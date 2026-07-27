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
  options: [{ id: id * 10, name: "260", stock: 5, additionalPrice: 0 }]
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
