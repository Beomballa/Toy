import { expect, test } from "@playwright/test";

test.beforeEach(async ({ page }) => {
  await page.route("**/api/front/auth/me", route => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ authenticated: true, email: "member@example.com", name: "기존 회원", nickname: "노렌" }) }));
  await page.route("**/api/front/member/orders**", route => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ items: [], statusSummaries: [], hasNext: false }) }));
  await page.route("**/api/front/member/reviews**", route => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ reviews: [], totalCount: 0, hasNext: false }) }));
});

test("MY 페이지는 기본정보를 정규화해 저장하고 최신 응답으로 갱신한다", async ({ page }) => {
  let request;
  await page.route("**/api/front/auth/profile", route => {
    request = route.request().postDataJSON();
    return route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ authenticated: true, email: "member@example.com", name: "새 회원", nickname: "새노렌" }) });
  });
  await page.goto("/front/my");
  await expect(page.locator("#memberProfileEmail")).toHaveValue("member@example.com");
  await page.locator("#memberProfileName").fill(" 새 회원 ");
  await page.locator("#memberProfileNickname").fill(" 새노렌 ");
  await page.locator("#memberProfileSubmitButton").click();
  await expect(page.locator("#memberProfileStatus")).toContainText("저장했습니다");
  expect(request).toEqual({ name: "새 회원", nickname: "새노렌" });
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

test("MY 페이지는 각 비밀번호 입력값을 독립적으로 표시하거나 숨긴다", async ({ page }) => {
  await page.goto("/front/my");
  const current = page.locator("#memberCurrentPassword");
  const button = page.locator('[data-password-toggle="memberCurrentPassword"]');

  await expect(current).toHaveAttribute("type", "password");
  await button.click();
  await expect(current).toHaveAttribute("type", "text");
  await expect(button).toHaveText("숨기기");
  await button.click();
  await expect(current).toHaveAttribute("type", "password");
  await expect(button).toHaveText("보기");
});

test("MY 페이지는 Caps Lock 상태를 비밀번호 필드에 연결해 안내한다", async ({ page }) => {
  await page.goto("/front/my");
  const input = page.locator("#memberNewPassword");
  const note = page.locator("#memberNewPasswordCaps");

  await input.focus();
  await page.evaluate(() => {
    const event = new KeyboardEvent("keydown", { bubbles: true, key: "A" });
    Object.defineProperty(event, "getModifierState", { value: key => key === "CapsLock" });
    document.querySelector("#memberNewPassword")?.dispatchEvent(event);
  });
  await expect(note).toBeVisible();
  await input.blur();
  await expect(note).toBeHidden();
});
