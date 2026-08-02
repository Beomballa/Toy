import { expect, test } from "@playwright/test";

test("MY 활동은 변조된 저장 항목을 제외하고 전체 활동을 안전하게 초기화한다", async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem("front-recent-viewed-products", JSON.stringify([
      {
        id: 12,
        name: "정상 저장 상품",
        brand: "Grade",
        model: "GS-12",
        price: 75000,
        stock: 3,
        thumbnailUrl: "/missing-image.jpg"
      },
      {
        id: '13" onmouseover="window.__storageInjected=true',
        name: "변조 저장 상품",
        brand: "Unsafe"
      }
    ]));
    localStorage.setItem("front-bookmark-products", JSON.stringify([{ id: 14, name: "관심 상품" }]));
  });

  await page.goto("/front/my?tab=recent");

  await expect(page.locator(".my-card")).toHaveCount(1);
  await expect(page.locator(".my-card h2")).toHaveText("정상 저장 상품");
  await expect(page.locator("body")).not.toContainText("변조 저장 상품");
  await expect(page.locator(".my-card__visual img")).toHaveAttribute("src", "/images/product-placeholder.svg");
  expect(await page.evaluate(() => window.__storageInjected)).toBeUndefined();

  page.once("dialog", dialog => dialog.accept());
  await page.locator("#myResetAllButton").click();

  await expect(page.locator(".my-empty")).toBeVisible();
  await expect(page.locator("#myTotalCount")).toHaveText("0");
  await expect(page.locator("#myToast")).toContainText("모든 쇼핑 활동을 초기화했습니다.");
  const remaining = await page.evaluate(() => [
    "front-recent-viewed-products",
    "front-bookmark-products",
    "front-compare-products",
    "front-hidden-products"
  ].filter(key => localStorage.getItem(key) !== null));
  expect(remaining).toEqual([]);
});

test("MY 활동은 중복 상품과 위험한 이미지·금액 값을 정규화한다", async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem("front-bookmark-products", JSON.stringify([
      { id: 21, name: "안전 확인", brand: "GS", price: -5000, stock: "invalid", thumbnailUrl: "javascript:alert(1)" },
      { id: "21", name: "중복 상품", price: 999999 }
    ]));
  });

  await page.goto("/front/my?tab=wishlist");
  await expect(page.locator(".my-card")).toHaveCount(1);
  await expect(page.locator(".my-card h2")).toHaveText("안전 확인");
  await expect(page.locator(".my-card__visual img")).toHaveAttribute("src", "/images/product-placeholder.svg");
  await expect(page.locator(".my-card__price strong")).toHaveText("0원");
  await expect(page.locator("body")).not.toContainText("중복 상품");
});
