import { expect, test } from "@playwright/test";

const productDetail = (id = 12) => ({
  id,
  brand: "반스",
  category: "스니커즈",
  name: "반스 올드스쿨 블랙",
  headline: "오늘의 셀렉션 012",
  model: "VN000D3HY28",
  price: 75000,
  stock: 5,
  createdDate: "2026-04-20",
  description: "상품 상세 테스트",
  mood: "daily essential",
  featured: true,
  featuredRank: 12,
  stockStatus: "재고 안정",
  priceLabel: "75,000원",
  thumbnailUrl: "javascript:alert(1)",
  options: [{ id: 31, name: "260", stock: 5, additionalPrice: 5000 }],
  relatedProducts: [{
    id: 11,
    brand: "컨버스",
    category: "스니커즈",
    name: "척 70",
    reason: "같은 카테고리",
    model: "162050C",
    price: 99000,
    stock: 3,
    stockStatus: "재고 긴장",
    priceLabel: "99,000원",
    thumbnailUrl: null
  }]
});

test("상품 상세는 요청 ID가 다른 응답을 거부하고 재시도 후 안전하게 표시한다", async ({ page }) => {
  let attempts = 0;
  await page.route("**/api/front/products/12", async (route) => {
    await route.fulfill({ json: productDetail(attempts++ === 0 ? 13 : 12) });
  });

  await page.goto("/front/products/12");
  await expect(page.locator("#detailTitle")).toHaveText("상품 상세를 불러오지 못했습니다.");
  await page.locator("#detailRetryButton").click();
  await expect(page.locator("#detailTitle")).toHaveText("오늘의 셀렉션 012");
  await expect(page.locator("#detailProductVisual img")).toHaveAttribute("src", "/images/product-placeholder.svg");
});

test("상품 상세는 옵션 추가금을 계산하고 불일치한 장바구니 합계를 거부한다", async ({ page }) => {
  await page.route("**/api/front/products/12", async (route) => route.fulfill({ json: productDetail() }));
  await page.route("**/api/front/cart/items", async (route) => route.fulfill({ json: {
    items: [{ productId: 12, optionId: 31, quantity: 1, unitPrice: 75000, lineAmount: 75000 }],
    totalQuantity: 1,
    totalAmount: 75000
  } }));

  await page.goto("/front/products/12");
  await page.locator('[data-detail-option="260"]').click();
  await expect(page.locator("#detailUnitPrice")).toHaveText("80,000원");
  await page.locator("#detailAddCartButton").click();
  await expect(page.locator(".toast.is-warning")).toContainText("장바구니 합계가 요청한 상품과 일치하지 않습니다");
});

test("상품 상세는 선택한 옵션을 다시 눌러도 선택과 구매 수량을 유지한다", async ({ page }) => {
  let submittedBody;
  await page.route("**/api/front/products/12", route => route.fulfill({ json: productDetail() }));
  await page.route("**/api/front/cart/items", async (route) => {
    submittedBody = route.request().postDataJSON();
    await route.fulfill({ json: {
      items: [{ productId: 12, optionId: 31, quantity: 1, unitPrice: 80000, lineAmount: 80000 }],
      totalQuantity: 1,
      totalAmount: 80000
    } });
  });

  await page.goto("/front/products/12");
  const option = page.locator('[data-detail-option="260"]');
  await option.click();
  await option.click();

  await expect(option).toHaveAttribute("aria-checked", "true");
  await expect(page.locator("#detailPurchaseEstimate")).toBeVisible();
  await page.locator("#detailAddCartButton").click();
  await expect.poll(() => submittedBody).toEqual({ productId: 12, optionId: 31, quantity: 1 });
  await expect(page.locator(".toast").last()).toContainText("장바구니에 담았습니다");
  expect(submittedBody).toEqual({ productId: 12, optionId: 31, quantity: 1 });
});

test("상품 상세 핵심 영역은 화면 폭 안에서 정렬된다", async ({ page }) => {
  await page.route("**/api/front/products/12", async (route) => route.fulfill({ json: productDetail() }));
  await page.goto("/front/products/12");
  await expect(page.locator("#detailTitle")).toHaveText("오늘의 셀렉션 012");

    const layout = await page.evaluate(() => {
    const viewportWidth = document.documentElement.clientWidth;
    const signals = document.querySelector("#detailSignalList").getBoundingClientRect();
    const returnLink = document.querySelector("#backToCatalogLink").getBoundingClientRect();
      const hero = document.querySelector("#detailHero").getBoundingClientRect();
      const desktopActions = document.querySelector(".detail-actions");
      const mobileActions = document.querySelector("#detailMobileActions");
      const mobileBounds = mobileActions.getBoundingClientRect();
      const optionGrid = document.querySelector("#detailOptionGrid");
      const insights = document.querySelector(".detail-option-insights");
      return {
      bodyOverflow: document.documentElement.scrollWidth - viewportWidth,
      signalWidth: signals.width,
      signalLeft: signals.left,
      signalRight: signals.right,
      returnWidth: returnLink.width,
      returnRight: returnLink.right,
      heroWidth: hero.width,
      viewportWidth,
      desktopActionsDisplay: getComputedStyle(desktopActions).display,
      mobileActionsDisplay: getComputedStyle(mobileActions).display,
      mobileActionsRight: mobileBounds.right,
      mobileActionsBottom: mobileBounds.bottom,
      viewportHeight: window.innerHeight,
      insightsOpen: insights.open,
      optionGridBeforeInsights: Boolean(optionGrid.compareDocumentPosition(insights) & Node.DOCUMENT_POSITION_FOLLOWING)
    };
  });

  expect(layout.bodyOverflow).toBe(0);
  expect(layout.heroWidth).toBeGreaterThan(0);
  expect(layout.signalWidth).toBeGreaterThan(0);
  expect(layout.signalLeft).toBeGreaterThanOrEqual(0);
  expect(layout.signalRight).toBeLessThanOrEqual(layout.viewportWidth);
  expect(layout.returnWidth).toBeGreaterThan(0);
  expect(layout.returnRight).toBeLessThanOrEqual(layout.viewportWidth);
  expect(layout.insightsOpen).toBe(false);
  expect(layout.optionGridBeforeInsights).toBe(true);
  if (test.info().project.name === "desktop-chromium") {
    expect(layout.desktopActionsDisplay).not.toBe("none");
    expect(layout.mobileActionsDisplay).toBe("none");
  } else {
    expect(layout.desktopActionsDisplay).not.toBe("none");
    expect(layout.mobileActionsDisplay).toBe("none");
    await page.locator("#detailOptions").scrollIntoViewIfNeeded();
    await expect(page.locator("body")).toHaveClass(/is-detail-purchase-docked/);
    const docked = await page.locator("#detailMobileActions").evaluate((element) => {
      const bounds = element.getBoundingClientRect();
      return {
        display: getComputedStyle(element).display,
        right: bounds.right,
        bottom: bounds.bottom,
        viewportWidth: document.documentElement.clientWidth,
        viewportHeight: window.innerHeight
      };
    });
    expect(docked.display).toBe("grid");
    expect(docked.right).toBeLessThanOrEqual(docked.viewportWidth);
    expect(docked.bottom).toBeLessThanOrEqual(docked.viewportHeight);
  }
});

test("상품 상세 경로와 Signal은 긴 데이터에서도 반환 제어를 밀어내지 않는다", async ({ page }) => {
  await page.route("**/api/front/products/12", route => route.fulfill({ json: {
    ...productDetail(),
    name: "아주 긴 상품명에서도 상품 목록으로 돌아가는 버튼을 화면 밖으로 밀어내지 않아야 합니다",
    headline: "긴 설명을 가진 오늘의 셀렉션 상품"
  } }));

  await page.goto("/front/products/12");
  await expect(page.locator("#detailTitle")).toHaveText("긴 설명을 가진 오늘의 셀렉션 상품");

  const layout = await page.evaluate(() => {
    const viewportWidth = document.documentElement.clientWidth;
    const breadcrumb = document.querySelector(".detail-breadcrumb").getBoundingClientRect();
    const product = document.querySelector("#detailBreadcrumbProduct");
    const returnLink = document.querySelector("#backToCatalogLink").getBoundingClientRect();
    const signalCards = [...document.querySelectorAll("#detailSignalList .signal-card")];
    return {
      bodyOverflow: document.documentElement.scrollWidth - viewportWidth,
      viewportWidth,
      breadcrumbRight: breadcrumb.right,
      productScrollWidth: product.scrollWidth,
      productClientWidth: product.clientWidth,
      returnLeft: returnLink.left,
      returnRight: returnLink.right,
      signalOverflow: signalCards.some((card) => card.scrollWidth > card.clientWidth)
    };
  });

  expect(layout.bodyOverflow).toBe(0);
  expect(layout.breadcrumbRight).toBeLessThanOrEqual(layout.viewportWidth);
  expect(layout.returnLeft).toBeGreaterThanOrEqual(0);
  expect(layout.returnRight).toBeLessThanOrEqual(layout.viewportWidth);
  expect(layout.productScrollWidth).toBeGreaterThanOrEqual(layout.productClientWidth);
  expect(layout.signalOverflow).toBe(false);
});
