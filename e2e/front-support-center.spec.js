import { expect, test } from "@playwright/test";

const noticePage = (boardType = "NOTICE") => ({
  items: [{
    id: 501,
    boardType,
    title: boardType === "NOTICE" ? "서비스 점검 안내" : "노출되면 안 되는 스타일",
    summary: "안정적인 서비스 제공을 위한 안내입니다.",
    viewCount: 12,
    pinned: true,
    createdDate: "2026.08.02"
  }],
  page: 0,
  size: 10,
  totalElements: 1,
  totalPages: 1,
  first: true,
  last: true,
  sort: "LATEST",
  pageViewCount: 12,
  pagePinnedCount: 1,
  pageNoticeCount: 1,
  pageStyleCount: boardType === "NOTICE" ? 0 : 1
});

test("고객지원은 공지가 아닌 응답을 거부하고 재시도 후 공지만 표시한다", async ({ page }) => {
  let count = 0;
  await page.route("**/api/front/content?**", async (route) => {
    count += 1;
    await route.fulfill({ json: noticePage(count === 1 ? "STYLE" : "NOTICE") });
  });

  await page.goto("/front/support?view=notice");
  await expect(page.locator("#supportNoticeList")).toContainText("공지사항을 불러오지 못했습니다");
  await expect(page.locator("body")).not.toContainText("노출되면 안 되는 스타일");

  await page.locator("#supportNoticeList").getByRole("button", { name: "다시 불러오기" }).click();
  await expect(page.locator("#supportNoticeList")).toContainText("서비스 점검 안내");
  await expect(page.locator("#supportNoticeList a")).toHaveAttribute("href", "/front/content/501");
});

test("고객지원은 변조된 최근 검색어를 정리해 중복 없이 표시한다", async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem("grade-stock-support-searches", JSON.stringify([
      "  주문   조회  ", "주문 조회", "", 123, "x".repeat(140)
    ]));
  });

  await page.goto("/front/support");
  await expect(page.locator("#supportRecentSearchList button")).toHaveCount(2);
  await expect(page.locator("#supportRecentSearchList button").first()).toHaveText("주문 조회");
  await expect(page.locator("#supportRecentSearchList button").nth(1)).toHaveText("x".repeat(100));
});
