import { expect, test } from "@playwright/test";

const orderResponse = (orderNumber, recipientName) => ({
  orderNumber,
  buyerName: "홍**",
  totalAmount: 75000,
  status: "ORDERED",
  statusLabel: "주문 접수",
  statusStep: 1,
  orderedAt: "2026.07.27 10:00",
  deliveryCompany: null,
  trackingNumber: null,
  delivery: {
    recipientName,
    recipientPhone: "010-****-5678",
    postalCode: "06236",
    address1: "서울시 강남구 테스트로",
    address2: "101호",
    deliveryRequest: "문 앞"
  },
  items: [{
    productId: 12,
    productName: "반스 올드스쿨 블랙",
    unitPrice: 75000,
    quantity: 1,
    lineAmount: 75000,
    thumbnailUrl: null
  }],
  statusHistory: [{ status: "ORDERED", statusLabel: "주문 접수", changedAt: "2026.07.27 10:00" }]
});

test("주문조회는 최신 응답만 표시하고 초기화 시 개인정보 DOM을 제거한다", async ({ page }) => {
  await page.route("**/api/front/orders/lookup", async (route) => {
    const body = route.request().postDataJSON();
    if (body.orderNumber === "GSSLOW000000") {
      await new Promise((resolve) => setTimeout(resolve, 250));
      await route.fulfill({ json: orderResponse("GSSLOW000000", "느린수령인") });
      return;
    }
    await route.fulfill({ json: orderResponse("GSFAST000000", "최신수령인") });
  });

  await page.goto("/front/orders");
  const orderNumber = page.locator('[name="orderNumber"]');
  const phone = page.locator('[name="phone"]');

  await orderNumber.fill("GSSLOW000000");
  await phone.fill("010-1234-5678");
  await page.locator("#orderLookupForm").evaluate((form) => form.requestSubmit());
  await orderNumber.fill("GSFAST000000");
  await page.locator("#orderLookupForm").evaluate((form) => form.requestSubmit());

  await expect(page.locator("#orderResultNumber")).toHaveText("GSFAST000000");
  await expect(page.locator("#orderDelivery")).toContainText("최신수령인");
  await expect(page.locator("body")).not.toContainText("느린수령인");

  await page.locator("#clearOrderLookupButton").click();
  await expect(page.locator("#orderResult")).toBeHidden();
  await expect(page.locator("#orderDelivery")).toBeEmpty();
  await expect(page.locator("body")).not.toContainText("최신수령인");
  await expect(page).toHaveURL(/\/front\/orders$/);
});

test("주문조회는 주문번호와 금액이 불일치한 응답을 표시하지 않는다", async ({ page }) => {
  await page.route("**/api/front/orders/lookup", async (route) => {
    const response = orderResponse("GSOTHER00000", "노출금지");
    response.totalAmount = 1;
    await route.fulfill({ json: response });
  });

  await page.goto("/front/orders");
  await page.locator('[name="orderNumber"]').fill("GSCHECK000000");
  await page.locator('[name="phone"]').fill("010-1234-5678");
  await page.locator("#orderLookupForm").evaluate((form) => form.requestSubmit());

  await expect(page.locator("#orderLookupError")).toContainText("일치하지 않습니다");
  await expect(page.locator("#orderResult")).toBeHidden();
  await expect(page.locator("body")).not.toContainText("노출금지");
});

test("주문조회는 현재 상태와 최신 이력이 다른 응답을 거부하고 재조회한다", async ({ page }) => {
  let attempts = 0;
  await page.route("**/api/front/orders/lookup", async (route) => {
    const body = route.request().postDataJSON();
    const response = orderResponse(body.orderNumber, "정상수령인");
    if (attempts++ === 0) response.statusHistory[0].status = "SHIPPED";
    await route.fulfill({ json: response });
  });

  await page.goto("/front/orders");
  await page.locator('[name="orderNumber"]').fill("GSHISTORY0000");
  await page.locator('[name="phone"]').fill("010-1234-5678");
  await page.locator("#orderLookupForm").evaluate((form) => form.requestSubmit());
  await expect(page.locator("#orderLookupError")).toContainText("처리 이력");
  await expect(page.locator("#orderResult")).toBeHidden();

  await page.locator("#orderLookupForm").evaluate((form) => form.requestSubmit());
  await expect(page.locator("#orderResultNumber")).toHaveText("GSHISTORY0000");
  await expect(page.locator("#orderDelivery")).toContainText("정상수령인");
});
