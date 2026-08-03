-- Replace only missing or generated placeholder thumbnails while preserving custom product images.
START TRANSACTION;

UPDATE product product
JOIN category category
  ON category.category_no = product.category_no
SET product.thumbnail_url = CASE category.name
    WHEN '스니커즈' THEN '/images/product/category/sneakers.jpg'
    WHEN '러닝화' THEN '/images/product/category/running.jpg'
    WHEN '반팔 티셔츠' THEN '/images/product/category/tshirt.jpg'
    WHEN '모자' THEN '/images/product/category/cap.jpg'
    WHEN '아우터' THEN '/images/product/category/outerwear.jpg'
    WHEN '샌들/슬리퍼' THEN '/images/product/category/sandals.jpg'
    WHEN '후드/맨투맨' THEN '/images/product/category/hoodie.jpg'
    WHEN '셔츠' THEN '/images/product/category/shirt.jpg'
    WHEN '슬리퍼' THEN '/images/product/category/slippers.jpg'
    ELSE product.thumbnail_url
END
WHERE product.thumbnail_url IS NULL
   OR TRIM(product.thumbnail_url) = ''
   OR product.thumbnail_url = '/images/product-placeholder.svg';

COMMIT;
