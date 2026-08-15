import { expect, test } from "@playwright/test";

const cartResponse = (quantity = 1) => ({
  items: [{
    itemId: 7,
    productId: 12,
    optionId: 31,
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

test("주문 접수는 중복 요청을 차단하고 완료 후 개인정보와 장바구니 상태를 비운다", async ({ page }) => {
  let orderRequestCount = 0;

  await page.route("**/api/front/cart", async (route) => {
    await route.fulfill({ json: cartResponse() });
  });
  await page.route("**/api/front/orders", async (route) => {
    orderRequestCount += 1;
    await new Promise((resolve) => setTimeout(resolve, 150));
    await route.fulfill({
      json: {
        orderId: 91,
        orderNumber: "GS202607270001",
        totalAmount: 75000,
        status: "ORDERED"
      }
    });
  });

  await page.goto("/front/checkout");
  await expect(page.locator(".commerce-item")).toHaveCount(1);

  await page.locator('[name="buyerName"]').fill("테스트 주문자");
  await page.locator('[name="buyerPhone"]').fill("01012345678");
  await page.locator("#sameBuyerCheck").check();
  await page.locator('[name="postalCode"]').fill("06236");
  await page.locator('[name="address1"]').fill("서울시 강남구 테스트로");
  await page.locator('[name="address2"]').fill("101호");

  await page.locator("#submitOrderButton").evaluate((button) => {
    button.click();
    button.click();
  });

  await expect(page.locator("#orderComplete")).toBeVisible();
  await expect(page.locator("#completedOrderNumber")).toHaveText("GS202607270001");
  expect(orderRequestCount).toBe(1);
  await expect(page.locator("#checkoutForm")).toBeHidden();
  await expect(page.locator("#submitOrderButton")).toBeDisabled();
  await expect(page.locator('[name="buyerName"]')).toHaveValue("");
  await expect(page.locator('[name="buyerPhone"]')).toHaveValue("");
  await expect(page.locator('[name="address1"]')).toHaveValue("");
  await expect(page.locator('[name="address2"]')).toHaveValue("");
  await expect(page.locator("#completedOrderTitle")).toBeFocused();

  const savedOrder = await page.evaluate(() => JSON.parse(sessionStorage.getItem("grade-stock-last-order")));
  expect(savedOrder).toEqual({
    orderNumber: "GS202607270001",
    phone: "010-1234-5678"
  });
});

test("장바구니 합계가 응답 품목과 다르면 주문 진입을 차단한다", async ({ page }) => {
  await page.route("**/api/front/cart", async (route) => {
    await route.fulfill({ json: { ...cartResponse(), totalAmount: 1 } });
  });

  await page.goto("/front/cart");
  await expect(page.locator("[data-cart-retry]")).toBeVisible();
  await expect(page.locator("#commerceCheckoutLink")).toHaveAttribute("aria-disabled", "true");
});

test("장바구니와 주문서는 공통 폭과 모바일 입력 구조를 유지한다", async ({ page }, testInfo) => {
  await page.route("**/api/front/cart", async (route) => route.fulfill({ json: cartResponse(2) }));
  await page.route("**/api/front/member/delivery-addresses", async (route) => route.fulfill({ json: [] }));

  await page.goto("/front/cart");
  await expect(page.locator(".commerce-item")).toHaveCount(1);
  const cartLayout = await page.evaluate(() => {
    const shell = document.querySelector(".commerce-shell").getBoundingClientRect();
    const item = document.querySelector(".commerce-item").getBoundingClientRect();
    const summary = document.querySelector(".commerce-summary").getBoundingClientRect();
    return {
      overflow: document.documentElement.scrollWidth - document.documentElement.clientWidth,
      shellWidth: shell.width,
      itemWidth: item.width,
      summaryWidth: summary.width,
      clearHidden: document.querySelector("#clearCartButton").hidden
    };
  });
  expect(cartLayout.overflow).toBe(0);
  expect(cartLayout.itemWidth).toBeGreaterThan(0);
  expect(cartLayout.summaryWidth).toBeGreaterThan(0);
  expect(cartLayout.clearHidden).toBe(false);

  await page.goto("/front/checkout");
  await expect(page.locator(".commerce-item")).toHaveCount(1);
  const checkoutLayout = await page.evaluate(() => {
    const fields = [...document.querySelectorAll(".form-grid input, .form-grid select")].map((field) => field.getBoundingClientRect());
    const summary = document.querySelector(".commerce-summary").getBoundingClientRect();
    return {
      overflow: document.documentElement.scrollWidth - document.documentElement.clientWidth,
      minFieldWidth: Math.min(...fields.map((field) => field.width)),
      maxFieldRight: Math.max(...fields.map((field) => field.right)),
      summaryWidth: summary.width,
      viewportWidth: document.documentElement.clientWidth
    };
  });
  expect(checkoutLayout.overflow).toBe(0);
  expect(checkoutLayout.minFieldWidth).toBeGreaterThan(0);
  expect(checkoutLayout.maxFieldRight).toBeLessThanOrEqual(checkoutLayout.viewportWidth);
  expect(checkoutLayout.summaryWidth).toBeGreaterThan(0);

  if (testInfo.project.name === "desktop-chromium") {
    expect(cartLayout.shellWidth).toBeGreaterThan(900);
  } else {
    expect(cartLayout.shellWidth).toBeLessThan(500);
  }
});
