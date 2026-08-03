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
      size: 8,
      sort: "LATEST",
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

test("콘텐츠 목록은 불일치한 페이지 집계를 거부하고 재시도 후 렌더링한다", async ({ page }) => {
  let attempts = 0;
  await page.route("**/api/front/content?**", async (route) => {
    const item = { id: 91, boardType: "NOTICE", title: "정상 공지", summary: "검증된 공지입니다.", viewCount: 7, pinned: true, createdDate: "2026.08.03" };
    await route.fulfill({ json: {
      items: [item],
      page: 0,
      size: 8,
      sort: "LATEST",
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
      pageViewCount: attempts++ === 0 ? 999 : 7,
      pagePinnedCount: 1,
      pageNoticeCount: 1,
      pageStyleCount: 0
    } });
  });

  await page.goto("/front/content");
  await expect(page.locator("#contentListGrid")).toContainText("콘텐츠를 불러오지 못했습니다.");
  await page.getByRole("button", { name: "다시 불러오기" }).click();
  await expect(page.locator("#contentListGrid")).toContainText("정상 공지");
  await expect(page.locator("#contentListPageViews")).toHaveText("7");
});
