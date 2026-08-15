import { expect, test } from "@playwright/test";

test.beforeEach(async ({ page }) => {
  await page.route("**/api/front/member/orders**", route => route.fulfill({
    status: 200,
    contentType: "application/json",
    body: JSON.stringify({ items: [], statusSummaries: [], hasNext: false })
  }));
  await page.route("**/api/front/member/reviews**", route => route.fulfill({
    status: 200,
    contentType: "application/json",
    body: JSON.stringify({ reviews: [], totalCount: 0, hasNext: false })
  }));
});

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

  await page.locator(".my-management summary").click();
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

test("MY 활동은 상품 보드를 먼저 보여주고 선택 작업과 관리 기능을 단계적으로 노출한다", async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem("front-recent-viewed-products", JSON.stringify([
      { id: 31, name: "첫 상품", brand: "NOREN", price: 120000, stock: 7 },
      { id: 32, name: "두 번째 상품", brand: "NOREN", price: 180000, stock: 30 }
    ]));
  });
  await page.goto("/front/my?tab=recent");

  const boardBeforeOrders = await page.evaluate(() => {
    const board = document.querySelector(".my-board");
    const orders = document.querySelector(".my-orders");
    return Boolean(board.compareDocumentPosition(orders) & Node.DOCUMENT_POSITION_FOLLOWING);
  });
  expect(boardBeforeOrders).toBeTruthy();
  await expect(page.locator(".my-management")).not.toHaveAttribute("open", "");
  await expect(page.locator("#myExportButton")).not.toBeVisible();
  await expect(page.locator("#mySelectionBar")).not.toBeVisible();

  await page.locator('[data-select-id="31"]').check();
  await expect(page.locator("#mySelectionBar")).toBeVisible();
  await expect(page.locator("#mySelectionText")).toContainText("1개 선택");
  await page.locator(".my-management summary").click();
  await expect(page.locator("#myExportButton")).toBeVisible();
});

test("MY 활동 탭은 키보드 전환과 모바일 경계를 유지한다", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/front/my?tab=recent");

  await page.locator('[data-tab="recent"]').focus();
  await page.keyboard.press("ArrowRight");
  await expect(page.locator('[data-tab="wishlist"]')).toHaveAttribute("aria-selected", "true");
  await expect(page).toHaveURL(/tab=wishlist/);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy();
  for (const selector of [".my-toolbar", ".my-command", ".my-orders", ".my-reviews"]) {
    const box = await page.locator(selector).boundingBox();
    expect(box.x).toBeGreaterThanOrEqual(0);
    expect(box.x + box.width).toBeLessThanOrEqual(390);
  }
});
