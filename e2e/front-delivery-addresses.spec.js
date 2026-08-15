import { expect, test } from "@playwright/test";

function address(id = 1, overrides = {}) {
  return {
    id,
    addressName: "집",
    recipientName: "홍길동",
    recipientPhone: "01012345678",
    postalCode: "06236",
    address1: "서울시 강남구 테헤란로 1",
    address2: "101호",
    defaultAddress: true,
    ...overrides
  };
}

test("배송지 폼은 필드 오류를 안내하고 정규화된 입력만 저장한다", async ({ page }) => {
  let savedBody;
  await page.route("**/api/front/member/delivery-addresses", async route => {
    if (route.request().method() === "GET") return route.fulfill({ json: [] });
    savedBody = route.request().postDataJSON();
    return route.fulfill({ json: [address()] });
  });
  await page.goto("/front/my/addresses");

  await page.locator("#addressName").fill("집");
  await page.locator("#recipientName").fill("홍길동");
  await page.locator("#recipientPhone").fill("123");
  await page.locator("#postalCode").fill("1234");
  await page.locator("#addressLine1").fill("서울시 강남구");
  await page.locator("#deliveryAddressSubmit").click();
  await expect(page.locator("#recipientPhone")).toHaveAttribute("aria-invalid", "true");
  await expect(page.locator("#recipientPhoneError")).toContainText("10~11자리");

  await page.locator("#recipientPhone").fill("01012345678");
  await page.locator("#postalCode").fill("06236");
  await page.locator("#deliveryAddressSubmit").click();
  await expect(page.locator(".address-card")).toHaveCount(1);
  await expect(page.locator("#deliveryAddressCount")).toHaveText("1개");
  expect(savedBody).toMatchObject({ recipientPhone: "010-1234-5678", postalCode: "06236", address1: "서울시 강남구" });
});

test("배송지 수정은 편집 상태를 표시하고 Escape로 안전하게 취소한다", async ({ page }) => {
  await page.route("**/api/front/member/delivery-addresses", route => route.fulfill({ json: [address()] }));
  await page.goto("/front/my/addresses");

  await page.locator('[data-address-edit="1"]').click();
  await expect(page.locator(".address-create")).toHaveClass(/is-editing/);
  await expect(page.locator("#addressFormTitle")).toContainText("집 수정");
  await expect(page.locator("#deliveryAddressFormReset")).toHaveText("수정 취소");
  await page.keyboard.press("Escape");
  await expect(page.locator(".address-create")).not.toHaveClass(/is-editing/);
  await expect(page.locator("#addressName")).toHaveValue("");
});

test("배송지 화면은 비정상 응답을 렌더링하지 않고 모바일 경계를 유지한다", async ({ page }) => {
  await page.route("**/api/front/member/delivery-addresses", route => route.fulfill({ json: [address(-1)] }));
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/front/my/addresses");

  await expect(page.locator(".address-card")).toHaveCount(0);
  await expect(page.locator(".address-empty")).toContainText("불러오지 못했습니다");
  await expect(page.locator("[data-address-retry]")).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy();
  for (const selector of [".address-hero", ".address-create", ".address-list-section"]) {
    const box = await page.locator(selector).boundingBox();
    expect(box.x).toBeGreaterThanOrEqual(0);
    expect(box.x + box.width).toBeLessThanOrEqual(390);
  }
});
