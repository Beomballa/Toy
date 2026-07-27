import { expect, test } from "@playwright/test";

test("콘텐츠 목록은 유효한 저장 항목만 표시하고 최근 읽기를 초기화한다", async ({ page }) => {
  await page.addInitScript(() => {
    const valid = { id: 77, boardType: "NOTICE", title: "정상 콘텐츠", viewedAt: new Date().toISOString() };
    localStorage.setItem("front-recent-content", JSON.stringify([
      valid,
      valid,
      { id: '78" onclick="window.__contentInjected=true', title: "변조 콘텐츠" }
    ]));
    localStorage.setItem("front-bookmarked-content", JSON.stringify([
      { ...valid, savedAt: new Date().toISOString() },
      { id: -1, title: "잘못된 콘텐츠" }
    ]));
  });
  await page.route("**/api/front/content?**", route => route.fulfill({
    json: {
      items: [],
      page: 0,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
      pageViewCount: 0,
      pagePinnedCount: 0,
      pageNoticeCount: 0,
      pageStyleCount: 0
    }
  }));

  await page.goto("/front/content");

  await expect(page.locator(".content-recent-card")).toHaveCount(1);
  await expect(page.locator(".content-saved-card")).toHaveCount(1);
  await expect(page.locator("body")).not.toContainText("변조 콘텐츠");
  await expect(page.locator("body")).not.toContainText("잘못된 콘텐츠");
  expect(await page.evaluate(() => window.__contentInjected)).toBeUndefined();

  await page.locator("#contentRecentClearButton").click();
  await expect(page.locator("#contentRecentBoard")).toBeHidden();
  await expect(page.locator("#contentListLiveStatus")).toContainText("최근 읽은 콘텐츠를 비웠습니다");
  expect(await page.evaluate(() => localStorage.getItem("front-recent-content"))).toBeNull();
});
