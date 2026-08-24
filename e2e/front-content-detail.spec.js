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
        characterCount: 38,
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

test("콘텐츠 상세는 다른 문서 응답과 불일치한 반응 집계를 거부하고 재시도한다", async ({ page }) => {
  const body = "요청한 콘텐츠만 안전하게 표시합니다.";
  let detailAttempts = 0;
  let reactionAttempts = 0;
  await page.route("**/api/front/content/88**", async (route) => {
    const request = route.request();
    const pathname = new URL(request.url()).pathname;
    if (pathname.endsWith("/views")) {
      await route.fulfill({ json: { counted: true, viewCount: 4 } });
      return;
    }
    if (pathname.endsWith("/reactions")) {
      const valid = reactionAttempts++ > 0;
      await route.fulfill({ json: valid
        ? { helpfulCount: 2, notHelpfulCount: 1, totalCount: 3, helpfulRate: 67, selectedReaction: null, changed: false }
        : { helpfulCount: 2, notHelpfulCount: 1, totalCount: 99, helpfulRate: 100, selectedReaction: null, changed: false } });
      return;
    }
    await route.fulfill({ json: {
      id: detailAttempts++ === 0 ? 999 : 88,
      boardType: "NOTICE",
      title: "응답 계약 안내",
      content: body,
      createdDate: "2026.08.03",
      viewCount: 3,
      estimatedReadMinutes: 1,
      characterCount: Array.from(body).length,
      pinned: false,
      newerContent: null,
      olderContent: null,
      relatedContents: []
    } });
  });

  await page.goto("/front/content/88");
  await expect(page.locator("#contentDetailError")).toBeVisible();
  await page.locator("#contentDetailRetryButton").click();
  await expect(page.locator("#contentDetailTitle")).toHaveText("응답 계약 안내");
  await expect(page.locator("#contentDetailReactionRetryButton")).toBeVisible();
  await page.locator("#contentDetailReactionRetryButton").click();
  await expect(page.locator("#contentDetailReactionSummary")).toContainText("3명 중 67%");
});

test("짧은 콘텐츠 본문은 과도한 빈 영역 없이 읽기 도구와 행동 영역을 정렬한다", async ({ page }) => {
  await page.route("**/api/front/content/99**", async (route) => {
    const pathname = new URL(route.request().url()).pathname;
    if (pathname.endsWith("/views")) {
      await route.fulfill({ json: { counted: true, viewCount: 1 } });
      return;
    }
    if (pathname.endsWith("/reactions")) {
      await route.fulfill({ json: { helpfulCount: 0, notHelpfulCount: 0, totalCount: 0, helpfulRate: 0, selectedReaction: null, changed: false } });
      return;
    }
    await route.fulfill({ json: {
      id: 99,
      boardType: "STYLE",
      title: "짧은 콘텐츠 레이아웃",
      content: "짧은 본문도 과도한 빈 공간 없이 읽을 수 있어야 합니다.",
      createdDate: "2026.08.24",
      viewCount: 0,
      estimatedReadMinutes: 1,
      characterCount: 32,
      pinned: false,
      newerContent: null,
      olderContent: null,
      relatedContents: []
    } });
  });

  await page.goto("/front/content/99");
  await expect(page.locator("#contentDetailTitle")).toHaveText("짧은 콘텐츠 레이아웃");

  const layout = await page.evaluate(() => {
    const viewportWidth = document.documentElement.clientWidth;
    const body = document.querySelector("#contentDetailBody").getBoundingClientRect();
    const toolbar = document.querySelector(".content-detail-reader-toolbar").getBoundingClientRect();
    const actions = [...document.querySelectorAll(".content-detail-article__actions > *")]
      .map((element) => element.getBoundingClientRect());
    return {
      pageOverflow: document.documentElement.scrollWidth - viewportWidth,
      bodyHeight: body.height,
      toolbarRight: toolbar.right,
      viewportWidth,
      actionOverflow: actions.some((action) => action.right > viewportWidth + 1)
    };
  });

  expect(layout.pageOverflow).toBe(0);
  expect(layout.bodyHeight).toBeLessThan(220);
  expect(layout.toolbarRight).toBeLessThanOrEqual(layout.viewportWidth);
  expect(layout.actionOverflow).toBe(false);
});
