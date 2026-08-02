import { expect, test } from "@playwright/test";

test("공통 상단은 저장 상품을 정규화하고 같은 화면 변경을 즉시 반영한다", async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem("front-bookmark-products", JSON.stringify([
      { id: 1, name: "정상" }, { id: "1", name: "중복" }, { id: 0 }, null
    ]));
    localStorage.setItem("front-compare-products", JSON.stringify([
      { id: 1 }, { id: 2 }, { id: 3 }, { id: 4 }
    ]));
  });

  await page.goto("/front/support");
  await expect(page.locator('[data-store-shell-count="bookmark"]')).toHaveText("1");
  await expect(page.locator('[data-store-shell-count="compare"]')).toHaveText("3");

  await page.evaluate(() => window.StorefrontState.write("bookmark", [{ id: 5 }, { id: 5 }, { id: -1 }]));
  await expect(page.locator('[data-store-shell-count="bookmark"]')).toHaveText("1");
  expect(await page.evaluate(() => window.StorefrontState.write("unknown-key", [{ id: 9 }]))).toBe(false);
});

test("공통 검색은 모달 상태와 포커스를 관리하고 검색어를 정리한다", async ({ page }) => {
  await page.goto("/front/support");
  const open = page.locator("[data-store-shell-search-open]");
  await open.click();
  await expect(open).toHaveAttribute("aria-expanded", "true");
  await expect(page.locator("[data-store-shell-search]" )).toBeVisible();
  await expect(page.locator("[data-store-shell-search-input]")).toBeFocused();

  await page.locator("[data-store-shell-search-close]").focus();
  await page.keyboard.press("Shift+Tab");
  await expect(page.locator(".store-shell__search-links a").last()).toBeFocused();

  await page.locator("[data-store-shell-search-input]").fill("  나이키   에어포스  ");
  await page.locator("[data-store-shell-search-form]").evaluate((form) => form.requestSubmit());
  await expect(page).toHaveURL(/keyword=%EB%82%98%EC%9D%B4%ED%82%A4+%EC%97%90%EC%96%B4%ED%8F%AC%EC%8A%A4|keyword=%EB%82%98%EC%9D%B4%ED%82%A4%20%EC%97%90%EC%96%B4%ED%8F%AC%EC%8A%A4/);
});
