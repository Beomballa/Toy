-- Local visual fixture for the public notice/style homepage section.
-- Existing documents are never updated; reruns are safe because titles are unique per fixture.
INSERT INTO CT_DOCUMENT (board_type, status, public_yn, pinned_yn, title, content, view_cnt, crt_dtm, upt_dtm)
SELECT 'NOTICE', 'PUBLISHED', 'Y', 'Y',
       'Grade Stock 서비스 이용 안내',
       '상품 탐색과 재고 비교 기능을 더 편리하게 이용할 수 있도록 주요 사용 방법을 안내합니다.',
       128, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM CT_DOCUMENT WHERE title = 'Grade Stock 서비스 이용 안내'
);

INSERT INTO CT_DOCUMENT (board_type, status, public_yn, pinned_yn, title, content, view_cnt, crt_dtm, upt_dtm)
SELECT 'NOTICE', 'PUBLISHED', 'Y', 'N',
       '상품 정보 업데이트 정책',
       '브랜드와 카테고리, 가격 및 재고 정보는 관리자 검수 이후 고객 화면에 반영됩니다.',
       84, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM CT_DOCUMENT WHERE title = '상품 정보 업데이트 정책'
);

INSERT INTO CT_DOCUMENT (board_type, status, public_yn, pinned_yn, title, content, view_cnt, crt_dtm, upt_dtm)
SELECT 'STYLE', 'PUBLISHED', 'Y', 'N',
       '일상에 자연스럽게 스며드는 스니커즈',
       '낮은 채도의 상의와 클래식 스니커즈를 조합해 오래 입을 수 있는 데일리 룩을 완성해 보세요.',
       236, DATE_SUB(NOW(), INTERVAL 2 HOUR), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM CT_DOCUMENT WHERE title = '일상에 자연스럽게 스며드는 스니커즈'
);

INSERT INTO CT_DOCUMENT (board_type, status, public_yn, pinned_yn, title, content, view_cnt, crt_dtm, upt_dtm)
SELECT 'STYLE', 'PUBLISHED', 'Y', 'N',
       '가벼운 레이어링을 위한 여름 에디트',
       '통기성이 좋은 셔츠와 반팔 티셔츠, 밝은 톤의 슈즈를 중심으로 계절에 맞는 레이어링을 제안합니다.',
       172, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM CT_DOCUMENT WHERE title = '가벼운 레이어링을 위한 여름 에디트'
);
