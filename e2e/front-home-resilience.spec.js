import { expect, test } from "@playwright/test";

const highlightPayload = (boardType = "NOTICE") => ({
  notices: [{ id: 501, boardType, title: "서비스 안내", summary: "안내 내용", viewCount: 20, pinned: true, createdDate: "2026.08.03" }],
  styles: [{ id: 502, boardType: "STYLE", title: "스타일 가이드", summary: "스타일 내용", viewCount: 10, pinned: false, createdDate: "2026.08.03" }],
  popular: [{ id: 502, boardType: "STYLE", title: "인기 스타일", summary: "인기 내용", recentViewCount: 8, uniqueVisitors: 5, pinned: false, createdDate: "2026.08.03" }],
  popularStartDate: "2026.07.28",
  popularEndDate: "2026.08.03"
});

const catalogPayload = (totalElements = 1) => ({
  products: [{ id: 101, brand: "나이키", category: "스니커즈", name: "에어포스 1", headline: "화이트 스니커즈", model: "AF1", price: 139000, stock: 12, createdDate: "2026-08-03", description: "상품 설명", mood: "daily", featured: true, featuredRank: 1, stockStatus: "재고 안정", priceLabel: "139,000원", options: [{ id: 1001, name: "260", stock: 12, additionalPrice: 0 }], thumbnailUrl: null }],
  pagination: { page: 0, size: 12, totalElements, totalPages: totalElements ? 1 : 0, first: true, last: true },
  metrics: { totalCount: totalElements, lowStockCount: 0, latestCreatedDate: "2026-08-03", latestDropCount: 1, featuredCount: 1, totalStock: 12, averagePrice: 139000, minimumPrice: 139000, maximumPrice: 139000, brandCount: 1, under200Count: 1, between200And300Count: 0, over300Count: 0 },
  brandFacets: [{ value: "나이키", count: 1 }],
  categoryFacets: [{ value: "스니커즈", count: 1 }]
});

const homeProduct = (overrides = {}) => ({ ...catalogPayload().products[0], ...overrides });

const collectionPayload = () => ({
  recommended: [homeProduct()],
  ranking: [homeProduct()],
  fastDelivery: [],
  latestDrops: [homeProduct()],
  lowStock: [homeProduct({ featured: false })]
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

test("홈 카탈로그는 전체 합계가 다른 응답을 캐시하거나 표시하지 않는다", async ({ page }) => {
  let attempts = 0;
  await page.route("**/api/front/catalog/bootstrap?**", async (route) => {
    const payload = catalogPayload(1);
    if (attempts++ === 0) payload.metrics.totalCount = 2;
    await route.fulfill({ json: payload });
  });

  await page.goto("/front");
  await expect(page.locator("#catalogGrid")).toContainText("카탈로그를 불러오지 못했습니다");
  await page.reload();
  await expect(page.locator("#catalogGrid")).toContainText("에어포스 1");
});

test("홈 컬렉션은 레일 조건이 다른 응답 대신 검증된 카탈로그를 사용한다", async ({ page }) => {
  await page.route("**/api/front/catalog/bootstrap?**", (route) => route.fulfill({ json: catalogPayload() }));
  await page.route("**/api/front/catalog/home-collections", async (route) => {
    const payload = collectionPayload();
    payload.lowStock = [homeProduct({ id: 999, name: "잘못된 저재고 상품", stock: 100, options: [{ id: 9991, name: "260", stock: 100, additionalPrice: 0 }] })];
    await route.fulfill({ json: payload });
  });

  await page.goto("/front");
  await expect(page.locator("#latestDropGrid")).toContainText("에어포스 1");
  await expect(page.locator("#lowStockGrid")).not.toContainText("잘못된 저재고 상품");
});
