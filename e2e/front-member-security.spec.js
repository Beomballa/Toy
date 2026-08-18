import { expect, test } from "@playwright/test";

test.beforeEach(async ({ page }) => {
  await page.route("**/api/front/member/orders**", route => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ items: [], statusSummaries: [], hasNext: false }) }));
  await page.route("**/api/front/member/reviews**", route => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ reviews: [], totalCount: 0, hasNext: false }) }));
});

test("MY 페이지는 비밀번호 확인 뒤 한 번의 요청으로 변경한다", async ({ page }) => {
  let requests = 0;
  let payload;
  await page.route("**/api/front/auth/password", async route => {
    requests += 1;
    payload = route.request().postDataJSON();
    await new Promise(resolve => setTimeout(resolve, 100));
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ authenticated: true }) });
  });
  await page.goto("/front/my");

  await page.locator("#memberCurrentPassword").fill("noren1234");
  await page.locator("#memberNewPassword").fill("renew1234");
  await page.locator("#memberConfirmPassword").fill("renew1234");
  await page.locator("#memberPasswordSubmitButton").dblclick();

  await expect(page.locator("#memberPasswordStatus")).toContainText("비밀번호를 변경했습니다");
  expect(requests).toBe(1);
  expect(payload).toEqual({ currentPassword: "noren1234", newPassword: "renew1234" });
  await expect(page.locator("#memberCurrentPassword")).toHaveValue("");
});

test("MY 페이지는 일치하지 않는 새 비밀번호를 요청 전에 안내하고 모바일 가로 경계를 유지한다", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  let requested = false;
  await page.route("**/api/front/auth/password", route => { requested = true; return route.abort(); });
  await page.goto("/front/my");

  await page.locator("#memberCurrentPassword").fill("noren1234");
  await page.locator("#memberNewPassword").fill("renew1234");
  await page.locator("#memberConfirmPassword").fill("different1234");
  await page.locator("#memberPasswordSubmitButton").click();

  await expect(page.locator("#memberPasswordStatus")).toContainText("일치하지 않습니다");
  expect(requested).toBeFalsy();
  const box = await page.locator(".my-security").boundingBox();
  expect(box.x).toBeGreaterThanOrEqual(0);
  expect(box.x + box.width).toBeLessThanOrEqual(390);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy();
});
