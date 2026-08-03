import { expect, test } from "@playwright/test";

const highlightPayload = (boardType = "NOTICE") => ({
  notices: [{ id: 501, boardType, title: "서비스 안내", summary: "안내 내용", viewCount: 20, pinned: true, createdDate: "2026.08.03" }],
  styles: [{ id: 502, boardType: "STYLE", title: "스타일 가이드", summary: "스타일 내용", viewCount: 10, pinned: false, createdDate: "2026.08.03" }],
  popular: [{ id: 502, boardType: "STYLE", title: "인기 스타일", summary: "인기 내용", recentViewCount: 8, uniqueVisitors: 5, pinned: false, createdDate: "2026.08.03" }],
  popularStartDate: "2026.07.28",
  popularEndDate: "2026.08.03"
});

test("홈 콘텐츠는 게시판 유형이 잘못된 응답을 거부하고 재시도한다", async ({ page }) => {
  let attempts = 0;
  await page.route("**/api/front/content/highlights?limit=4", async (route) => {
    await route.fulfill({ json: highlightPayload(attempts++ === 0 ? "STYLE" : "NOTICE") });
  });

  await page.goto("/front");
  await expect(page.locator("#contentHighlightRetryButton")).toBeVisible();
  await expect(page.locator("#noticeHighlightList")).not.toContainText("서비스 안내");
  await page.locator("#contentHighlightRetryButton").click();
  await expect(page.locator("#noticeHighlightList")).toContainText("서비스 안내");
  await expect(page.locator("#contentHighlightRetryButton")).toBeHidden();
});
