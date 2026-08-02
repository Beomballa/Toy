import { expect, test } from "@playwright/test";

const productDetail = (id = 12) => ({
  id,
  brand: "반스",
  category: "스니커즈",
  name: "반스 올드스쿨 블랙",
  headline: "오늘의 셀렉션 012",
  model: "VN000D3HY28",
  price: 75000,
  stock: 5,
  createdDate: "2026-04-20",
  description: "상품 상세 테스트",
  mood: "daily essential",
  featured: true,
  featuredRank: 12,
  stockStatus: "재고 안정",
  priceLabel: "75,000원",
  thumbnailUrl: "javascript:alert(1)",
  options: [{ id: 31, name: "260", stock: 5, additionalPrice: 5000 }],
  relatedProducts: [{
    id: 11,
    brand: "컨버스",
    category: "스니커즈",
    name: "척 70",
    reason: "같은 카테고리",
    model: "162050C",
    price: 99000,
    stock: 3,
    stockStatus: "재고 긴장",
    priceLabel: "99,000원",
    thumbnailUrl: null
  }]
});

test("상품 상세는 요청 ID가 다른 응답을 거부하고 재시도 후 안전하게 표시한다", async ({ page }) => {
  let attempts = 0;
  await page.route("**/api/front/products/12", async (route) => {
    await route.fulfill({ json: productDetail(attempts++ === 0 ? 13 : 12) });
  });

  await page.goto("/front/products/12");
  await expect(page.locator("#detailTitle")).toHaveText("상품 상세를 불러오지 못했습니다.");
  await page.locator("#detailRetryButton").click();
  await expect(page.locator("#detailTitle")).toHaveText("오늘의 셀렉션 012");
  await expect(page.locator("#detailProductVisual img")).toHaveAttribute("src", "/images/product-placeholder.svg");
});

test("상품 상세는 옵션 추가금을 계산하고 불일치한 장바구니 합계를 거부한다", async ({ page }) => {
  await page.route("**/api/front/products/12", async (route) => route.fulfill({ json: productDetail() }));
  await page.route("**/api/front/cart/items", async (route) => route.fulfill({ json: {
    items: [{ productId: 12, optionId: 31, quantity: 1, unitPrice: 75000, lineAmount: 75000 }],
    totalQuantity: 1,
    totalAmount: 75000
  } }));

  await page.goto("/front/products/12");
  await page.locator('[data-detail-option="260"]').click();
  await expect(page.locator("#detailUnitPrice")).toHaveText("80,000원");
  await page.locator("#detailAddCartButton").click();
  await expect(page.locator(".toast.is-warning")).toContainText("장바구니 합계가 요청한 상품과 일치하지 않습니다");
});
