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

test("콘텐츠 목록의 긴 제목과 탐색 제어는 320px 화면에서 넘치지 않는다", async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 844 });
  await page.route("**/api/front/content?**", route => route.fulfill({
    json: {
      items: [{
        id: 101,
        boardType: "STYLE",
        title: "한 단어로 길게 이어지는 콘텐츠제목반응형레이아웃검증용문구",
        summary: "긴 설명이 제어 영역과 카드 본문 폭을 밀어내지 않아야 합니다.",
        viewCount: 7,
        pinned: false,
        createdDate: "2026.08.30"
      }],
      page: 0,
      size: 8,
      sort: "LATEST",
      totalElements: 9,
      totalPages: 2,
      first: true,
      last: false,
      pageViewCount: 7,
      pagePinnedCount: 0,
      pageNoticeCount: 0,
      pageStyleCount: 1
    }
  }));

  await page.goto("/front/content");
  for (const selector of [".content-list-toolbar", ".content-list-settings", ".content-list-insights", ".content-list-card", ".content-list-pagination"]) {
    const box = await page.locator(selector).boundingBox();
    expect(box.x).toBeGreaterThanOrEqual(0);
    expect(box.x + box.width).toBeLessThanOrEqual(320);
  }
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy();
});
