import { expect, test } from "@playwright/test";

test("신규 드롭과 저재고 상품 레일 레이아웃을 유지한다", async ({ page }, testInfo) => {
  await page.goto("/");
  const signalStrip = page.locator("#signalStrip");
  await expect(signalStrip.locator(".signal-feed.swiper-initialized")).toHaveCount(2);
  await expect(signalStrip.locator("#latestDropGrid .swiper-slide")).toHaveCount(4);
  await expect(signalStrip.locator("#lowStockGrid .swiper-slide")).toHaveCount(4);
  await signalStrip.scrollIntoViewIfNeeded();
  await page.waitForTimeout(100);

  const layout = await page.evaluate(() => {
    const rail = document.getElementById("latestDropGrid");
    const firstSlide = rail.querySelector(".swiper-slide");
    const actions = document.querySelector("#signalStrip .section-action-bar");
    const actionMenu = actions.querySelector(".rail-action-menu");
    const navigation = actions.querySelector(".product-rail-navigation");
    const cards = [...rail.querySelectorAll(".rail-product-card")];
    const actionBounds = actions.getBoundingClientRect();
    const menuBounds = actionMenu.getBoundingClientRect();
    const navigationBounds = navigation.getBoundingClientRect();
    const catalogToolbar = document.querySelector(".catalog-toolbar");
    return {
      bodyOverflow: document.body.scrollWidth - document.body.clientWidth,
      railWidth: rail.getBoundingClientRect().width,
      slideWidth: firstSlide.getBoundingClientRect().width,
      actionWidth: actionBounds.width,
      actionRight: actionBounds.right,
      menuWidth: menuBounds.width,
      menuRight: menuBounds.right,
      navigationWidth: navigationBounds.width,
      navigationRight: navigationBounds.right,
      cardDetails: cards.map((card) => {
        const cardBounds = card.getBoundingClientRect();
        const detailBounds = card.querySelector(".rail-product-card__detail").getBoundingClientRect();
        return {
          cardRight: cardBounds.right,
          detailWidth: detailBounds.width,
          detailRight: detailBounds.right
        };
      }),
      catalogToolbarPosition: getComputedStyle(catalogToolbar).position
    };
  });

  expect(layout.bodyOverflow).toBe(0);
  expect(layout.railWidth).toBeGreaterThan(0);
  expect(layout.slideWidth).toBeGreaterThan(0);
  expect(layout.actionWidth).toBeGreaterThan(0);
  expect(layout.actionRight).toBeLessThanOrEqual(page.viewportSize().width);
  expect(layout.menuWidth).toBeGreaterThan(0);
  expect(layout.menuRight).toBeLessThanOrEqual(page.viewportSize().width);
  expect(layout.navigationWidth).toBeGreaterThan(0);
  expect(layout.navigationRight).toBeLessThanOrEqual(page.viewportSize().width);
  for (const card of layout.cardDetails) {
    expect(card.detailWidth).toBeGreaterThan(0);
    expect(card.detailRight).toBeLessThanOrEqual(card.cardRight + 1);
  }
  expect(layout.catalogToolbarPosition).toBe("static");
  if (testInfo.project.name === "desktop-chromium") {
    expect(layout.slideWidth).toBeLessThan(layout.railWidth / 3);
  } else {
    expect(layout.slideWidth).toBeLessThan(layout.railWidth);
    expect(layout.slideWidth).toBeGreaterThan(layout.railWidth / 2);
  }

  await expect(signalStrip).toHaveScreenshot("home-product-rails.png", {
    mask: [signalStrip.locator(".rail-product-card__visual-link")]
  });
});

test("홈 카탈로그 카드와 페이지네이션은 겹치지 않는다", async ({ page }, testInfo) => {
  await page.addInitScript(() => localStorage.removeItem("front-catalog-display-preferences"));
  await page.goto("/");
  await expect(page.locator("#catalogGrid .catalog-card").first()).toBeVisible();

  const layout = await page.evaluate(() => {
    const card = document.querySelector("#catalogGrid .catalog-card");
    const grid = document.querySelector("#catalogGrid");
    const visual = card.querySelector(".catalog-card__visual");
    const select = card.querySelector(".catalog-card__select");
    const wish = card.querySelector(".catalog-card__wish");
    const actions = [...card.querySelector(".catalog-card__action").children];
    const pagination = document.querySelector("#catalogPagination");
    const metrics = pagination.querySelector(".catalog-page-metrics");
    const bounds = (element) => {
      const rect = element.getBoundingClientRect();
      return { x: rect.x, y: rect.y, width: rect.width, height: rect.height, right: rect.right };
    };
    const selectBounds = bounds(select);
    const wishBounds = bounds(wish);
    const actionBounds = actions.map(bounds);
    return {
      bodyOverflow: document.documentElement.scrollWidth - document.documentElement.clientWidth,
      columnCount: getComputedStyle(grid).gridTemplateColumns.split(" ").length,
      card: bounds(card),
      visual: bounds(visual),
      select: selectBounds,
      wish: wishBounds,
      selectionOverlap: Math.max(0, Math.min(selectBounds.right, wishBounds.right) - Math.max(selectBounds.x, wishBounds.x)),
      actions: actionBounds,
      pagination: bounds(pagination),
      metrics: bounds(metrics),
      analyticsDisplay: getComputedStyle(document.querySelector("#catalogInsightGrid")).display
    };
  });

  expect(layout.bodyOverflow).toBe(0);
  expect(layout.columnCount).toBe(testInfo.project.name === "desktop-chromium" ? 3 : 2);
  expect(layout.selectionOverlap).toBe(0);
  expect(layout.select.width).toBeLessThanOrEqual(72);
  expect(layout.wish.width).toBeLessThanOrEqual(44);
  expect(layout.card.height).toBeGreaterThan(layout.visual.height);
  expect(layout.card.height - layout.visual.height).toBeLessThan(260);
  expect(layout.metrics.width).toBeLessThanOrEqual(layout.pagination.width);
  expect(layout.analyticsDisplay).toBe("none");
  if (testInfo.project.name === "desktop-chromium") {
    expect(layout.actions).toHaveLength(2);
    expect(layout.actions[0].y).toBe(layout.actions[1].y);
  } else {
    expect(layout.actions[0].width).toBeGreaterThan(0);
    expect(layout.actions[1].width).toBe(0);
  }
});
