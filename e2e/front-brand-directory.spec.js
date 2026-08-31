import { expect, test } from "@playwright/test";

const metrics = (totalCount, brandCount = 1) => ({
  totalCount,
  lowStockCount: 0,
  latestCreatedDate: "2026-08-02",
  latestDropCount: totalCount,
  featuredCount: 0,
  totalStock: totalCount * 12,
  averagePrice: totalCount ? 120000 : 0,
  minimumPrice: totalCount ? 120000 : 0,
  maximumPrice: totalCount ? 120000 : 0,
  brandCount,
  under200Count: totalCount,
  between200And300Count: 0,
  over300Count: 0
});

const product = (brand) => ({
  id: brand === "나이키" ? 101 : 202,
  brand,
  category: "스니커즈",
  name: `${brand} 테스트 상품`,
  model: "MODEL-01",
  price: 120000,
  stock: 12,
  stockStatus: "재고 안정",
  thumbnailUrl: "javascript:alert(1)"
});

test("브랜드 디렉터리는 다른 브랜드 응답을 거부하고 재시도 시 안전한 상품만 표시한다", async ({ page }) => {
  let brandRequestCount = 0;
  await page.route("**/api/front/catalog/bootstrap?**", async (route) => {
    const url = new URL(route.request().url());
    const brand = url.searchParams.get("brand");
    if (!brand) {
      await route.fulfill({ json: {
        products: [],
        pagination: { page: 0, size: 1, totalElements: 0, totalPages: 0, first: true, last: true },
        metrics: metrics(2, 2),
        brandFacets: [{ value: "나이키", count: 1 }, { value: "아디다스", count: 1 }],
        categoryFacets: []
      } });
      return;
    }
    brandRequestCount += 1;
    const responseBrand = brandRequestCount === 1 ? "아디다스" : brand;
    await route.fulfill({ json: {
      products: [product(responseBrand)],
      pagination: { page: 0, size: 12, totalElements: 1, totalPages: 1, first: true, last: true },
      metrics: metrics(1),
      brandFacets: [{ value: brand, count: 1 }],
      categoryFacets: [{ value: "스니커즈", count: 1 }]
    } });
  });

  await page.goto("/front/brands");
  await page.getByRole("button", { name: "나이키 브랜드 상품 보기" }).click();
  await expect(page.locator("#brandProductGrid")).toContainText("브랜드 상품을 불러오지 못했습니다");
  await expect(page.locator("body")).not.toContainText("아디다스 테스트 상품");

  await page.locator("#brandProductGrid").getByRole("button", { name: "다시 불러오기" }).click();
  await expect(page.locator("#brandProductGrid")).toContainText("나이키 테스트 상품");
  await expect(page.locator("#brandProductGrid img")).toHaveAttribute("src", "/images/product-placeholder.svg");
});

test("브랜드 디렉터리는 facet 합계가 전체 상품 수와 다른 응답을 거부한다", async ({ page }) => {
  let attempts = 0;
  await page.route("**/api/front/catalog/bootstrap?**", async (route) => {
    const invalid = attempts++ === 0;
    await route.fulfill({ json: {
      products: [],
      pagination: { page: 0, size: 1, totalElements: 0, totalPages: 0, first: true, last: true },
      metrics: metrics(2, 2),
      brandFacets: [
        { value: "나이키", count: invalid ? 99 : 1 },
        { value: "아디다스", count: 1 }
      ],
      categoryFacets: []
    } });
  });

  await page.goto("/front/brands");
  await expect(page.locator("#brandCardGrid")).toContainText("브랜드 데이터를 불러오지 못했습니다");
  await page.getByRole("button", { name: "다시 불러오기" }).click();
  await expect(page.locator(".brand-card")).toHaveCount(2);
  await expect(page.locator("#brandDirectoryProducts")).toHaveText("2");
});

test("브랜드 상품 선택 바와 긴 상품명은 320px 화면에서 겹치지 않는다", async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 844 });
  await page.route("**/api/front/catalog/bootstrap?**", async route => {
    const brand = new URL(route.request().url()).searchParams.get("brand");
    await route.fulfill({ json: {
      products: brand ? [{
        ...product(brand),
        name: "긴 상품명으로 카드 높이와 고정 선택 바 폭을 확인하는 반응형 브랜드 상품"
      }] : [],
      pagination: { page: 0, size: 12, totalElements: brand ? 1 : 0, totalPages: brand ? 1 : 0, first: true, last: true },
      metrics: metrics(1, 1),
      brandFacets: [{ value: "나이키", count: 1 }],
      categoryFacets: [{ value: "스니커즈", count: 1 }]
    } });
  });

  await page.goto("/front/brands");
  await page.getByRole("button", { name: "나이키 브랜드 상품 보기" }).click();
  await page.locator(".brand-product-card__select").click();
  await expect(page.locator("#brandSelectionBar")).toBeVisible();
  for (const selector of [".brand-product-card", "#brandSelectionBar", "#brandSelectionCompareButton", "#brandSelectionCancelButton"]) {
    const box = await page.locator(selector).boundingBox();
    expect(box.x).toBeGreaterThanOrEqual(0);
    expect(box.x + box.width).toBeLessThanOrEqual(320);
  }
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy();
});
