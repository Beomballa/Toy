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
    const actionBounds = actions.getBoundingClientRect();
    const menuBounds = actionMenu.getBoundingClientRect();
    const navigationBounds = navigation.getBoundingClientRect();
    return {
      bodyOverflow: document.body.scrollWidth - document.body.clientWidth,
      railWidth: rail.getBoundingClientRect().width,
      slideWidth: firstSlide.getBoundingClientRect().width,
      actionWidth: actionBounds.width,
      actionRight: actionBounds.right,
      menuWidth: menuBounds.width,
      menuRight: menuBounds.right,
      navigationWidth: navigationBounds.width,
      navigationRight: navigationBounds.right
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
