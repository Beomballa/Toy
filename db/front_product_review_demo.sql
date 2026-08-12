-- Local-only visual fixture for product review pagination and summary UI.
-- It deliberately reuses an existing account and active products, and is not part of migrate-db.sh.
START TRANSACTION;

SET @review_demo_member_no = (SELECT MIN(ID) FROM sy_account WHERE DEL_YN <> 'Y');
SET @review_demo_product_one = (SELECT MIN(product_no) FROM product WHERE status = 'ACTIVE');
SET @review_demo_product_two = (
    SELECT MIN(product_no) FROM product WHERE status = 'ACTIVE' AND product_no > @review_demo_product_one
);
SET @review_demo_order_one = (
    SELECT order_no FROM orders WHERE order_num = 'TOY-REVIEW-DEMO-20260812-001' LIMIT 1
);
SET @review_demo_order_two = (
    SELECT order_no FROM orders WHERE order_num = 'TOY-REVIEW-DEMO-20260812-002' LIMIT 1
);

INSERT INTO orders (order_num, buyer_name, buyer_phone, member_no, total_amount, status, crt_dtm, upt_dtm)
SELECT 'TOY-REVIEW-DEMO-20260812-001', '리뷰 데모', '010-0000-0001', @review_demo_member_no,
       139000, 'DELIVERED', DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()
WHERE @review_demo_member_no IS NOT NULL
  AND @review_demo_product_one IS NOT NULL
  AND @review_demo_order_one IS NULL;

SET @review_demo_order_one = (
    SELECT order_no FROM orders WHERE order_num = 'TOY-REVIEW-DEMO-20260812-001' LIMIT 1
);

INSERT INTO orders (order_num, buyer_name, buyer_phone, member_no, total_amount, status, crt_dtm, upt_dtm)
SELECT 'TOY-REVIEW-DEMO-20260812-002', '리뷰 데모', '010-0000-0002', @review_demo_member_no,
       209000, 'DELIVERED', DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()
WHERE @review_demo_member_no IS NOT NULL
  AND @review_demo_product_two IS NOT NULL
  AND @review_demo_order_two IS NULL;

SET @review_demo_order_two = (
    SELECT order_no FROM orders WHERE order_num = 'TOY-REVIEW-DEMO-20260812-002' LIMIT 1
);

INSERT INTO order_item (order_no, product_no, product_name, order_price, count)
SELECT @review_demo_order_one, product_no, name_ko, release_price, 1
FROM product
WHERE product_no = @review_demo_product_one
  AND NOT EXISTS (
      SELECT 1 FROM order_item WHERE order_no = @review_demo_order_one AND product_no = @review_demo_product_one
  );

INSERT INTO order_item (order_no, product_no, product_name, order_price, count)
SELECT @review_demo_order_two, product_no, name_ko, release_price, 1
FROM product
WHERE product_no = @review_demo_product_two
  AND NOT EXISTS (
      SELECT 1 FROM order_item WHERE order_no = @review_demo_order_two AND product_no = @review_demo_product_two
  );

INSERT INTO front_product_review (member_no, product_no, order_no, reviewer_name, rating, content, crt_dtm, upt_dtm)
SELECT @review_demo_member_no, @review_demo_product_one, @review_demo_order_one, '데***', 5,
       '상품 상태가 깔끔하고 배송 안내도 이해하기 쉬웠습니다.', DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()
WHERE @review_demo_member_no IS NOT NULL
  AND @review_demo_product_one IS NOT NULL
  AND @review_demo_order_one IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM front_product_review
      WHERE member_no = @review_demo_member_no AND order_no = @review_demo_order_one AND product_no = @review_demo_product_one
  );

INSERT INTO front_product_review (member_no, product_no, order_no, reviewer_name, rating, content, crt_dtm, upt_dtm)
SELECT @review_demo_member_no, @review_demo_product_two, @review_demo_order_two, '데***', 4,
       '사이즈 옵션과 재고 정보를 주문 전에 확인할 수 있어 선택하기 편했습니다.', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()
WHERE @review_demo_member_no IS NOT NULL
  AND @review_demo_product_two IS NOT NULL
  AND @review_demo_order_two IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM front_product_review
      WHERE member_no = @review_demo_member_no AND order_no = @review_demo_order_two AND product_no = @review_demo_product_two
  );

COMMIT;
