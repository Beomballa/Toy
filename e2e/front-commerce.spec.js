import { expect, test } from "@playwright/test";

const cartResponse = (quantity = 1) => ({
  items: [{
    itemId: 7,
    productId: 12,
    productName: "반스 올드스쿨 블랙",
    optionName: "260",
    unitPrice: 75000,
    quantity,
    lineAmount: 75000 * quantity,
    stock: 10,
    thumbnailUrl: null
  }],
  itemCount: 1,
  totalQuantity: quantity,
  totalAmount: 75000 * quantity
});

test("장바구니 조회 실패를 재시도하고 중복 수량 요청을 차단한다", async ({ page }) => {
  let loadCount = 0;
  let patchCount = 0;

  await page.route("**/api/front/cart", async (route) => {
    loadCount += 1;
    if (loadCount === 1) {
      await route.fulfill({ status: 503, json: { message: "temporary" } });
      return;
    }
    await route.fulfill({ json: cartResponse() });
  });
  await page.route("**/api/front/cart/items/7", async (route) => {
    patchCount += 1;
    await new Promise((resolve) => setTimeout(resolve, 150));
    await route.fulfill({ json: cartResponse(2) });
  });

  await page.goto("/front/cart");
  await expect(page.locator("[data-cart-retry]")).toBeVisible();
  await expect(page.locator("#commerceCheckoutLink")).toHaveAttribute("aria-disabled", "true");

  await page.locator("[data-cart-retry]").click();
  await expect(page.locator(".commerce-item")).toHaveCount(1);
  await expect(page.locator("#commerceCheckoutLink")).toHaveAttribute("aria-disabled", "false");

  const increaseButton = page.locator('[data-quantity="1"]');
  await increaseButton.evaluate((button) => {
    button.click();
    button.click();
  });
  await expect(page.locator(".commerce-item__control span")).toContainText("2");
  expect(patchCount).toBe(1);
  await expect(page.locator("#commerceCartList")).toHaveAttribute("aria-busy", "false");
});
