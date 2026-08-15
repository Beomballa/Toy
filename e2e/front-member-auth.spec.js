import { expect, test } from "@playwright/test";

test("회원가입은 URL 진입과 비밀번호 보조 상태를 제공한다", async ({ page }) => {
  await page.route("**/api/front/auth/me", route => route.fulfill({ status: 200, contentType: "application/json", body: '{"authenticated":false}' }));
  await page.goto("/front/login?mode=signup&next=/front/cart");

  await expect(page.locator("#memberSignUpTab")).toHaveAttribute("aria-selected", "true");
  await expect(page.locator("#memberSignUpForm")).toBeVisible();
  await page.locator("#memberSignUpPassword").fill("noren123");
  await expect(page.locator("[data-password-rule].is-valid")).toHaveCount(3);

  await page.locator("#memberSignUpPasswordConfirm").fill("different");
  await expect(page.locator("#memberSignUpPasswordConfirm")).toHaveAttribute("aria-invalid", "true");
  await expect(page.locator("#memberSignUpPasswordConfirmError")).toContainText("일치하지 않습니다");

  await page.locator('[data-password-toggle="memberSignUpPassword"]').click();
  await expect(page.locator("#memberSignUpPassword")).toHaveAttribute("type", "text");
  await expect(page.locator('[data-password-toggle="memberSignUpPassword"]')).toHaveText("숨기기");
});

test("인증 탭은 키보드로 전환되고 모바일에서 화면을 넘지 않는다", async ({ page }) => {
  await page.route("**/api/front/auth/me", route => route.fulfill({ status: 200, contentType: "application/json", body: '{"authenticated":false}' }));
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/front/login");

  await page.locator("#memberLoginTab").focus();
  await page.keyboard.press("ArrowRight");
  await expect(page.locator("#memberSignUpTab")).toHaveAttribute("aria-selected", "true");
  await expect(page).toHaveURL(/mode=signup/);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy();
  const panel = await page.locator(".member-auth__panel").boundingBox();
  expect(panel.x).toBeGreaterThanOrEqual(0);
  expect(panel.x + panel.width).toBeLessThanOrEqual(390);
});
