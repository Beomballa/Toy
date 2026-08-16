import { expect, test } from "@playwright/test";

test("주문 문의 컨텍스트는 필터 이동에도 유지되고 원래 회원 주문으로 돌아간다", async ({ page }) => {
  await page.addInitScript(() => {
    sessionStorage.setItem("noren-support-order-context", JSON.stringify({
      orderNumber: "GSMEMBER00000",
      statusLabel: "배송 준비",
      memberOrder: true
    }));
  });
  await page.goto("/front/support?topic=ORDER&context=order");

  await expect(page.locator("#supportOrderContextCard")).toBeVisible();
  await expect(page.locator("#supportOrderContext")).toContainText("GSMEMBER00000");
  await expect(page.locator("#supportOrderContextLink")).toHaveAttribute("href", "/front/orders/GSMEMBER00000?member=true");
  await page.locator('[data-support-topic="SHOPPING"]').click();
  await expect(page).toHaveURL(/context=order/);
  await expect(page.locator("#supportOrderContextCard")).toBeVisible();

  await page.locator("#supportOrderContextClearButton").click();
  await expect(page.locator("#supportOrderContextCard")).toBeHidden();
  await expect(page).not.toHaveURL(/context=order/);
  expect(await page.evaluate(() => sessionStorage.getItem("noren-support-order-context"))).toBeNull();
});

test("FAQ 직접 링크는 해당 답변을 펼치고 공유 가능한 짧은 식별자를 유지한다", async ({ page }) => {
  await page.goto("/front/support?view=faq&topic=ORDER&faq=order-cancel");

  const faq = page.locator('[data-faq-id="order-cancel"]');
  await expect(faq.locator("[aria-expanded]")).toHaveAttribute("aria-expanded", "true");
  await expect(faq.locator(".support-faq__answer")).toBeVisible();
  await expect(faq).toContainText("출고 전 상태");
  await expect(page).toHaveURL(/faq=order-cancel/);

  await faq.locator("[aria-expanded]").click();
  await expect(page).not.toHaveURL(/faq=order-cancel/);
  await expect(faq.locator(".support-faq__answer")).toBeHidden();
});

test("고객지원 검색은 모바일에서도 지우기 제어와 화면 경계를 유지한다", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/front/support");
  await page.locator("#supportKeyword").fill("주문");

  await expect(page.locator("#supportSearchClearButton")).toBeVisible();
  await page.locator("#supportSearchClearButton").click();
  await expect(page.locator("#supportKeyword")).toHaveValue("");
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy();
  for (const selector of [".support-hero", ".support-quick-grid", ".support-layout", ".support-contact"]) {
    const box = await page.locator(selector).boundingBox();
    expect(box.x).toBeGreaterThanOrEqual(0);
    expect(box.x + box.width).toBeLessThanOrEqual(390);
  }
});
