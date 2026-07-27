import { expect, test } from "@playwright/test";

test("저장소가 차단돼도 콘텐츠 조회와 반응은 같은 방문자 키를 사용한다", async ({ page }) => {
  const visitorKeys = [];

  await page.addInitScript(() => {
    Storage.prototype.getItem = () => {
      throw new DOMException("blocked", "SecurityError");
    };
    Storage.prototype.setItem = () => {
      throw new DOMException("blocked", "SecurityError");
    };
  });

  await page.route("**/api/front/content/77**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    if (url.pathname.endsWith("/views")) {
      visitorKeys.push(request.postDataJSON().visitorKey);
      await route.fulfill({ json: { counted: true, viewCount: 11 } });
      return;
    }
    if (url.pathname.endsWith("/reactions")) {
      if (request.method() === "GET") {
        visitorKeys.push(request.headers()["x-content-visitor-key"]);
        await route.fulfill({
          json: { helpfulCount: 1, notHelpfulCount: 0, totalCount: 1, helpfulRate: 100, selectedReaction: null, changed: false }
        });
        return;
      }
      const body = request.postDataJSON();
      visitorKeys.push(body.visitorKey);
      await route.fulfill({
        json: { helpfulCount: 2, notHelpfulCount: 0, totalCount: 2, helpfulRate: 100, selectedReaction: body.reaction, changed: true }
      });
      return;
    }
    await route.fulfill({
      json: {
        id: 77,
        boardType: "NOTICE",
        title: "저장소 제한 환경 안내",
        content: "저장소가 제한되어도 콘텐츠 읽기와 반응 기능은 계속 동작해야 합니다.",
        createdDate: "2026.07.27",
        viewCount: 10,
        estimatedReadMinutes: 1,
        characterCount: 40,
        pinned: false,
        newerContent: null,
        olderContent: null,
        relatedContents: []
      }
    });
  });

  await page.goto("/front/content/77");
  await expect(page.locator("#contentDetailTitle")).toHaveText("저장소 제한 환경 안내");
  await expect(page.locator("#contentDetailReaction")).toBeVisible();
  await page.locator("#contentDetailHelpfulButton").click();
  await expect(page.locator("#contentDetailHelpfulButton")).toHaveAttribute("aria-pressed", "true");

  expect(visitorKeys).toHaveLength(3);
  expect(new Set(visitorKeys).size).toBe(1);
  expect(visitorKeys[0]).toMatch(/^[A-Za-z0-9-]{16,64}$/);
});
