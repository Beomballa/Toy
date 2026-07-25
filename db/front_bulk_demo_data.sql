-- Front catalog/content load data for local performance and UI verification.
-- Re-running this script is safe because every row is anchored to TOY-BULK-20260725-*.

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS tmp_seed_numbers;
CREATE TEMPORARY TABLE tmp_seed_numbers (
    sequence_no INT NOT NULL PRIMARY KEY
);

INSERT INTO tmp_seed_numbers (sequence_no)
SELECT ones.n + tens.n * 10 + hundreds.n * 100 + thousands.n * 1000 + 1
FROM (
    SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) ones
CROSS JOIN (
    SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) tens
CROSS JOIN (
    SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) hundreds
CROSS JOIN (
    SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) thousands;

DROP TEMPORARY TABLE IF EXISTS tmp_active_brands;
CREATE TEMPORARY TABLE tmp_active_brands AS
SELECT brand_no, ROW_NUMBER() OVER (ORDER BY brand_no) AS row_no
FROM brand
WHERE is_active = 'Y';
ALTER TABLE tmp_active_brands ADD PRIMARY KEY (row_no);

DROP TEMPORARY TABLE IF EXISTS tmp_active_categories;
CREATE TEMPORARY TABLE tmp_active_categories AS
SELECT category_no, name, ROW_NUMBER() OVER (ORDER BY category_no) AS row_no
FROM category
WHERE is_active = 'Y'
  AND depth >= 2;
ALTER TABLE tmp_active_categories ADD PRIMARY KEY (row_no);

SET @brand_count = (SELECT COUNT(*) FROM tmp_active_brands);
SET @category_count = (SELECT COUNT(*) FROM tmp_active_categories);
SET @audit_admin_no = COALESCE((SELECT MIN(admin_no) FROM admin_user), 1);

INSERT INTO product (
    category_no,
    brand_no,
    name_ko,
    model_num,
    release_price,
    release_dt,
    thumbnail_url,
    status,
    crt_dtm,
    upt_dtm,
    crt_no,
    upt_no
)
SELECT category.category_no,
       brand.brand_no,
       CONCAT('프론트 데모 ', category.name, ' 셀렉션 ', LPAD(numbers.sequence_no, 5, '0')),
       CONCAT('TOY-BULK-20260725-', LPAD(numbers.sequence_no, 5, '0')),
       39000 + MOD(numbers.sequence_no, 42) * 10000,
       CURRENT_DATE - INTERVAL MOD(numbers.sequence_no, 180) DAY,
       '/images/product-placeholder.svg',
       CASE
           WHEN MOD(numbers.sequence_no, 37) = 0 THEN 'SOLD_OUT'
           WHEN MOD(numbers.sequence_no, 53) = 0 THEN 'HIDDEN'
           ELSE 'ACTIVE'
       END,
       CURRENT_TIMESTAMP - INTERVAL MOD(numbers.sequence_no, 90) DAY,
       CURRENT_TIMESTAMP,
       @audit_admin_no,
       @audit_admin_no
FROM tmp_seed_numbers numbers
JOIN tmp_active_brands brand
  ON brand.row_no = MOD(numbers.sequence_no - 1, @brand_count) + 1
JOIN tmp_active_categories category
  ON category.row_no = MOD(numbers.sequence_no - 1, @category_count) + 1
WHERE NOT EXISTS (
    SELECT 1
    FROM product existing
    WHERE existing.model_num = CONCAT('TOY-BULK-20260725-', LPAD(numbers.sequence_no, 5, '0'))
);

INSERT INTO product_option (
    product_no,
    option_name,
    stock_cnt,
    additional_price
)
SELECT product.product_no,
       option_seed.option_name,
       MOD(product.product_no + option_seed.sort_no * 7, 81),
       option_seed.additional_price
FROM product
CROSS JOIN (
    SELECT 1 sort_no, '250' option_name, 0 additional_price
    UNION ALL SELECT 2, '265', 5000
    UNION ALL SELECT 3, '280', 10000
) option_seed
WHERE product.model_num LIKE 'TOY-BULK-20260725-%'
  AND NOT EXISTS (
      SELECT 1
      FROM product_option existing
      WHERE existing.product_no = product.product_no
        AND existing.option_name = option_seed.option_name
  );

INSERT INTO front_product_display (
    product_no,
    headline,
    description,
    mood,
    featured_yn,
    featured_rank,
    crt_dtm,
    crt_no,
    upt_dtm,
    upt_no
)
SELECT product.product_no,
       CONCAT('오늘의 셀렉션 ', RIGHT(product.model_num, 5)),
       '브랜드, 가격대, 카테고리와 사이즈별 재고 흐름을 확인할 수 있는 프론트 대용량 검증 상품입니다.',
       CASE MOD(product.product_no, 4)
           WHEN 0 THEN 'daily essential'
           WHEN 1 THEN 'new classic'
           WHEN 2 THEN 'street utility'
           ELSE 'seasonal focus'
       END,
       'N',
       999,
       product.crt_dtm,
       @audit_admin_no,
       CURRENT_TIMESTAMP,
       @audit_admin_no
FROM product
WHERE product.model_num LIKE 'TOY-BULK-20260725-%'
  AND NOT EXISTS (
      SELECT 1
      FROM front_product_display existing
      WHERE existing.product_no = product.product_no
  );

INSERT INTO CT_DOCUMENT (
    product_no,
    board_type,
    status,
    public_yn,
    pinned_yn,
    title,
    content,
    view_cnt,
    crt_dtm,
    crt_no,
    upt_dtm,
    upt_no
)
SELECT product.product_no,
       CASE MOD(numbers.sequence_no, 4)
           WHEN 0 THEN 'NOTICE'
           WHEN 1 THEN 'STYLE'
           WHEN 2 THEN 'DISCUSS'
           ELSE 'QNA'
       END,
       'PUBLISHED',
       'Y',
       CASE WHEN MOD(numbers.sequence_no, 500) = 0 THEN 'Y' ELSE 'N' END,
       CONCAT('[TOY-BULK-20260725-', LPAD(numbers.sequence_no, 5, '0'), '] 프론트 콘텐츠'),
       CONCAT('상품 ', product.name_ko, '의 스타일, 사이즈, 재고 흐름을 소개하는 대용량 화면 검증 콘텐츠입니다.'),
       MOD(numbers.sequence_no * 17, 5000),
       CURRENT_TIMESTAMP - INTERVAL MOD(numbers.sequence_no, 120) DAY,
       @audit_admin_no,
       CURRENT_TIMESTAMP,
       @audit_admin_no
FROM tmp_seed_numbers numbers
JOIN product
  ON product.model_num = CONCAT('TOY-BULK-20260725-', LPAD(numbers.sequence_no, 5, '0'))
WHERE NOT EXISTS (
    SELECT 1
    FROM CT_DOCUMENT existing
    WHERE existing.title = CONCAT(
        '[TOY-BULK-20260725-',
        LPAD(numbers.sequence_no, 5, '0'),
        '] 프론트 콘텐츠'
    )
);

INSERT INTO front_content_view_event (
    document_no,
    visitor_key,
    viewed_date,
    viewed_dtm
)
SELECT document.no,
       SHA2(CONCAT('TOY-BULK-VIEW-', document.no), 256),
       CURRENT_DATE - INTERVAL MOD(document.no, 30) DAY,
       CURRENT_TIMESTAMP - INTERVAL MOD(document.no, 30) DAY
FROM CT_DOCUMENT document
WHERE document.title LIKE '[TOY-BULK-20260725-%'
  AND NOT EXISTS (
      SELECT 1
      FROM front_content_view_event existing
      WHERE existing.document_no = document.no
        AND existing.visitor_key = SHA2(CONCAT('TOY-BULK-VIEW-', document.no), 256)
        AND existing.viewed_date = CURRENT_DATE - INTERVAL MOD(document.no, 30) DAY
  );

INSERT INTO front_content_reaction (
    document_no,
    visitor_key,
    reaction_type,
    created_dtm,
    updated_dtm
)
SELECT document.no,
       SHA2(CONCAT('TOY-BULK-REACTION-', document.no), 256),
       CASE WHEN MOD(document.no, 5) = 0 THEN 'NOT_HELPFUL' ELSE 'HELPFUL' END,
       CURRENT_TIMESTAMP - INTERVAL MOD(document.no, 30) DAY,
       CURRENT_TIMESTAMP - INTERVAL MOD(document.no, 30) DAY
FROM CT_DOCUMENT document
WHERE document.title LIKE '[TOY-BULK-20260725-%'
  AND NOT EXISTS (
      SELECT 1
      FROM front_content_reaction existing
      WHERE existing.document_no = document.no
        AND existing.visitor_key = SHA2(CONCAT('TOY-BULK-REACTION-', document.no), 256)
  );

DROP TEMPORARY TABLE IF EXISTS tmp_active_categories;
DROP TEMPORARY TABLE IF EXISTS tmp_active_brands;
DROP TEMPORARY TABLE IF EXISTS tmp_seed_numbers;

COMMIT;
