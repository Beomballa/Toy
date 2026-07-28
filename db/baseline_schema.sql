SET FOREIGN_KEY_CHECKS = 0;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_activity_log` (
  `log_no` bigint NOT NULL AUTO_INCREMENT COMMENT '로그 번호',
  `admin_no` bigint NOT NULL COMMENT '관리자 번호',
  `action_type` varchar(50) NOT NULL COMMENT '행위 종류 (PRODUCT_CREATE, USER_BAN 등)',
  `target_id` bigint DEFAULT NULL COMMENT '대상 데이터 ID',
  `ip_address` varchar(50) NOT NULL COMMENT '접속 IP',
  `action_dtm` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '행위 발생일시',
  PRIMARY KEY (`log_no`),
  KEY `idx_admin` (`admin_no`),
  KEY `idx_action` (`action_type`),
  KEY `idx_dtm` (`action_dtm`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='관리자 활동 로그 (보안/감사용)';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_operation_notice` (
  `notice_no` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_active` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_pinned` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `start_dtm` datetime DEFAULT NULL,
  `end_dtm` datetime DEFAULT NULL,
  `crt_dtm` datetime DEFAULT NULL,
  `crt_no` bigint DEFAULT NULL,
  `upt_dtm` datetime DEFAULT NULL,
  `upt_no` bigint DEFAULT NULL,
  PRIMARY KEY (`notice_no`),
  KEY `idx_admin_operation_notice_active` (`is_active`,`is_pinned`),
  KEY `idx_admin_operation_notice_period` (`start_dtm`,`end_dtm`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_operation_task` (
  `task_no` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `priority` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `assignee_admin_no` bigint DEFAULT NULL,
  `due_date` date DEFAULT NULL,
  `is_pinned` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N',
  `crt_dtm` datetime DEFAULT NULL,
  `crt_no` bigint DEFAULT NULL,
  `upt_dtm` datetime DEFAULT NULL,
  `upt_no` bigint DEFAULT NULL,
  `source_id` bigint DEFAULT NULL,
  `source_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`task_no`),
  UNIQUE KEY `uk_admin_operation_task_source` (`source_type`,`source_id`),
  KEY `idx_admin_operation_task_status` (`status`),
  KEY `idx_admin_operation_task_priority` (`priority`),
  KEY `idx_admin_operation_task_assignee` (`assignee_admin_no`),
  KEY `idx_admin_operation_task_due_date` (`due_date`),
  KEY `idx_admin_operation_task_pinned` (`is_pinned`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_operation_task_comment` (
  `comment_no` bigint NOT NULL AUTO_INCREMENT,
  `task_no` bigint NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `crt_dtm` datetime(6) DEFAULT NULL,
  `crt_no` bigint DEFAULT NULL,
  `upt_dtm` datetime(6) DEFAULT NULL,
  `upt_no` bigint DEFAULT NULL,
  PRIMARY KEY (`comment_no`),
  KEY `idx_task_comment_task_no` (`task_no`),
  KEY `idx_task_comment_crt_dtm` (`crt_dtm`),
  CONSTRAINT `fk_task_comment_task` FOREIGN KEY (`task_no`) REFERENCES `admin_operation_task` (`task_no`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_system_setting` (
  `setting_no` bigint NOT NULL AUTO_INCREMENT,
  `setting_key` varchar(100) NOT NULL,
  `setting_value` varchar(500) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `crt_dtm` datetime DEFAULT NULL,
  `crt_no` bigint DEFAULT NULL,
  `upt_dtm` datetime DEFAULT NULL,
  `upt_no` bigint DEFAULT NULL,
  PRIMARY KEY (`setting_no`),
  UNIQUE KEY `uk_admin_system_setting_key` (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_system_setting_history` (
  `history_no` bigint NOT NULL AUTO_INCREMENT,
  `setting_key` varchar(100) NOT NULL,
  `setting_name` varchar(100) NOT NULL,
  `before_value` varchar(500) DEFAULT NULL,
  `after_value` varchar(500) NOT NULL,
  `change_summary` varchar(500) NOT NULL,
  `changed_ip_address` varchar(50) NOT NULL,
  `crt_dtm` datetime DEFAULT NULL,
  `crt_no` bigint DEFAULT NULL,
  `upt_dtm` datetime DEFAULT NULL,
  `upt_no` bigint DEFAULT NULL,
  PRIMARY KEY (`history_no`),
  KEY `idx_admin_system_setting_history_key_dtm` (`setting_key`,`crt_dtm`),
  KEY `idx_admin_system_setting_history_admin_dtm` (`crt_no`,`crt_dtm`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_user` (
  `admin_no` bigint NOT NULL AUTO_INCREMENT COMMENT '관리자 번호',
  `login_id` varchar(50) NOT NULL COMMENT '로그인 ID',
  `password` varchar(255) NOT NULL COMMENT '암호화된 비밀번호',
  `name` varchar(50) NOT NULL COMMENT '관리자 이름',
  `role` varchar(20) DEFAULT 'ROLE_ADMIN' COMMENT '권한 (ROLE_SUPER, ROLE_ADMIN)',
  `status` varchar(10) DEFAULT 'ACTIVE' COMMENT '상태 (ACTIVE, SUSPENDED)',
  `last_login_dtm` datetime DEFAULT NULL COMMENT '마지막 로그인 일시',
  `crt_dtm` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '계정 생성일',
  PRIMARY KEY (`admin_no`),
  UNIQUE KEY `login_id` (`login_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='관리자 계정';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `brand` (
  `brand_no` bigint NOT NULL AUTO_INCREMENT COMMENT '브랜드 번호',
  `name_ko` varchar(100) NOT NULL COMMENT '브랜드명 (한글)',
  `name_en` varchar(100) DEFAULT NULL COMMENT '브랜드명 (영문)',
  `logo_url` varchar(500) DEFAULT NULL COMMENT '브랜드 로고 이미지 URL',
  `is_active` varchar(1) NOT NULL DEFAULT 'Y',
  PRIMARY KEY (`brand_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='브랜드 마스터';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `category_no` bigint NOT NULL AUTO_INCREMENT COMMENT '카테고리 번호',
  `parent_no` bigint DEFAULT NULL COMMENT '상위 카테고리 번호 (NULL이면 최상위)',
  `name` varchar(100) NOT NULL COMMENT '카테고리명',
  `depth` int NOT NULL DEFAULT '1' COMMENT '분류 깊이 (1, 2)',
  `is_active` varchar(1) DEFAULT 'Y' COMMENT '활성 여부 (Y/N)',
  PRIMARY KEY (`category_no`),
  KEY `idx_parent` (`parent_no`),
  KEY `idx_depth` (`depth`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='상품 카테고리 (계층 구조)';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ct_document` (
  `no` bigint NOT NULL AUTO_INCREMENT,
  `product_no` bigint DEFAULT NULL,
  `board_type` varchar(20) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `public_yn` varchar(1) NOT NULL DEFAULT 'Y',
  `pinned_yn` varchar(1) NOT NULL DEFAULT 'N',
  `title` varchar(255) DEFAULT NULL,
  `content` varchar(255) DEFAULT NULL,
  `view_cnt` int NOT NULL DEFAULT '0',
  `crt_dtm` datetime DEFAULT CURRENT_TIMESTAMP,
  `crt_no` bigint DEFAULT NULL,
  `upt_dtm` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `upt_no` bigint DEFAULT NULL,
  PRIMARY KEY (`no`),
  KEY `idx_ct_document_board_type` (`board_type`),
  KEY `idx_ct_document_product_no` (`product_no`),
  KEY `idx_ct_document_crt_dtm` (`crt_dtm`),
  KEY `idx_ct_document_status` (`status`),
  KEY `idx_ct_document_public_yn` (`public_yn`),
  KEY `idx_ct_document_pinned_yn` (`pinned_yn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='콘텐츠 게시물';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `display_banner` (
  `banner_no` bigint NOT NULL AUTO_INCREMENT COMMENT '배너 번호',
  `title` varchar(100) NOT NULL COMMENT '배너 제목 (관리용)',
  `image_url` varchar(500) NOT NULL COMMENT '배너 이미지 URL',
  `target_url` varchar(500) DEFAULT NULL COMMENT '클릭 시 이동 URL',
  `start_dtm` datetime NOT NULL COMMENT '노출 시작일시',
  `end_dtm` datetime NOT NULL COMMENT '노출 종료일시',
  `sort_order` int DEFAULT '0' COMMENT '노출 순서 (낮을수록 먼저)',
  `is_active` varchar(1) DEFAULT 'Y' COMMENT '사용 여부 (Y/N)',
  `crt_admin_no` bigint NOT NULL COMMENT '등록 관리자 번호',
  `crt_dtm` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
  `crt_no` bigint DEFAULT NULL COMMENT '등록자',
  `upt_dtm` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  `upt_no` bigint DEFAULT NULL COMMENT '수정자',
  PRIMARY KEY (`banner_no`),
  KEY `idx_display` (`is_active`,`start_dtm`,`end_dtm`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='메인 배너 관리';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `document_daily_stats` (
  `stats_no` bigint NOT NULL AUTO_INCREMENT,
  `aggregated_at` datetime(6) NOT NULL,
  `draft_count` bigint NOT NULL,
  `linked_count` bigint NOT NULL,
  `pinned_count` bigint NOT NULL,
  `private_count` bigint NOT NULL,
  `public_count` bigint NOT NULL,
  `published_count` bigint NOT NULL,
  `scope` enum('DISCUSS','NOTICE','QNA','STYLE','TOTAL') NOT NULL,
  `snapshot_date` date NOT NULL,
  `total_count` bigint NOT NULL,
  `total_view_count` bigint NOT NULL,
  PRIMARY KEY (`stats_no`),
  UNIQUE KEY `uk_document_daily_stats_date_scope` (`snapshot_date`,`scope`),
  KEY `idx_document_daily_stats_date` (`snapshot_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `front_cart` (
  `cart_no` bigint NOT NULL AUTO_INCREMENT,
  `cart_token` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `crt_dtm` datetime(6) DEFAULT NULL,
  `crt_no` bigint DEFAULT NULL,
  `upt_dtm` datetime(6) DEFAULT NULL,
  `upt_no` bigint DEFAULT NULL,
  PRIMARY KEY (`cart_no`),
  UNIQUE KEY `uk_front_cart_token` (`cart_token`),
  KEY `ix_front_cart_status_updated` (`status`,`upt_dtm`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `front_cart_item` (
  `cart_item_no` bigint NOT NULL AUTO_INCREMENT,
  `cart_no` bigint NOT NULL,
  `product_no` bigint NOT NULL,
  `option_no` bigint NOT NULL,
  `quantity` int NOT NULL,
  `crt_dtm` datetime(6) DEFAULT NULL,
  `crt_no` bigint DEFAULT NULL,
  `upt_dtm` datetime(6) DEFAULT NULL,
  `upt_no` bigint DEFAULT NULL,
  PRIMARY KEY (`cart_item_no`),
  UNIQUE KEY `uk_front_cart_item_option` (`cart_no`,`product_no`,`option_no`),
  KEY `ix_front_cart_item_cart` (`cart_no`),
  KEY `fk_front_cart_item_product` (`product_no`),
  KEY `fk_front_cart_item_option` (`option_no`),
  CONSTRAINT `fk_front_cart_item_cart` FOREIGN KEY (`cart_no`) REFERENCES `front_cart` (`cart_no`),
  CONSTRAINT `fk_front_cart_item_option` FOREIGN KEY (`option_no`) REFERENCES `product_option` (`option_no`),
  CONSTRAINT `fk_front_cart_item_product` FOREIGN KEY (`product_no`) REFERENCES `product` (`product_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `front_content_reaction` (
  `reaction_no` bigint NOT NULL AUTO_INCREMENT,
  `document_no` bigint NOT NULL,
  `visitor_key` varchar(64) NOT NULL,
  `reaction_type` varchar(20) NOT NULL,
  `created_dtm` datetime NOT NULL,
  `updated_dtm` datetime NOT NULL,
  PRIMARY KEY (`reaction_no`),
  UNIQUE KEY `uk_front_content_reaction_visitor` (`document_no`,`visitor_key`),
  KEY `idx_front_content_reaction_document_type` (`document_no`,`reaction_type`),
  KEY `idx_front_content_reaction_updated` (`updated_dtm`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `front_content_view_event` (
  `event_no` bigint NOT NULL AUTO_INCREMENT,
  `document_no` bigint NOT NULL,
  `visitor_key` varchar(64) NOT NULL,
  `viewed_date` date NOT NULL,
  `viewed_dtm` datetime NOT NULL,
  PRIMARY KEY (`event_no`),
  UNIQUE KEY `uk_front_content_view_daily` (`document_no`,`visitor_key`,`viewed_date`),
  KEY `idx_front_content_view_document_dtm` (`document_no`,`viewed_dtm`),
  KEY `idx_front_content_view_date` (`viewed_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `front_product_display` (
  `display_no` bigint NOT NULL AUTO_INCREMENT,
  `crt_dtm` datetime(6) DEFAULT NULL,
  `crt_no` bigint DEFAULT NULL,
  `upt_dtm` datetime(6) DEFAULT NULL,
  `upt_no` bigint DEFAULT NULL,
  `description` varchar(1000) NOT NULL,
  `featured_rank` int NOT NULL,
  `featured_yn` varchar(1) NOT NULL,
  `headline` varchar(120) NOT NULL,
  `mood` varchar(120) NOT NULL,
  `product_no` bigint NOT NULL,
  PRIMARY KEY (`display_no`),
  UNIQUE KEY `UKkj3roiunun2vcsff3eabh22tk` (`product_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_delivery` (
  `order_delivery_no` bigint NOT NULL AUTO_INCREMENT,
  `order_no` bigint NOT NULL,
  `recipient_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `recipient_phone` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `postal_code` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `address1` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `address2` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `delivery_request` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `crt_dtm` datetime(6) DEFAULT NULL,
  `crt_no` bigint DEFAULT NULL,
  `upt_dtm` datetime(6) DEFAULT NULL,
  `upt_no` bigint DEFAULT NULL,
  PRIMARY KEY (`order_delivery_no`),
  UNIQUE KEY `uk_order_delivery_order` (`order_no`),
  CONSTRAINT `fk_order_delivery_order` FOREIGN KEY (`order_no`) REFERENCES `orders` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_item` (
  `order_item_no` bigint NOT NULL AUTO_INCREMENT,
  `order_no` bigint NOT NULL,
  `product_no` bigint NOT NULL,
  `product_name` varchar(200) NOT NULL,
  `order_price` int NOT NULL,
  `count` int NOT NULL,
  `option_no` bigint DEFAULT NULL,
  PRIMARY KEY (`order_item_no`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_status_history` (
  `history_no` bigint NOT NULL AUTO_INCREMENT,
  `order_no` bigint NOT NULL,
  `action_type` varchar(30) NOT NULL,
  `before_status` varchar(20) DEFAULT NULL,
  `after_status` varchar(20) DEFAULT NULL,
  `reason` varchar(200) DEFAULT NULL,
  `admin_memo_snapshot` varchar(1000) DEFAULT NULL,
  `delivery_company` varchar(50) DEFAULT NULL,
  `tracking_num` varchar(50) DEFAULT NULL,
  `crt_dtm` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `crt_no` bigint DEFAULT NULL,
  `upt_dtm` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `upt_no` bigint DEFAULT NULL,
  PRIMARY KEY (`history_no`),
  KEY `idx_order_status_history_order_no` (`order_no`),
  KEY `idx_order_status_history_crt_dtm` (`crt_dtm`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `order_no` bigint NOT NULL AUTO_INCREMENT,
  `order_num` varchar(50) NOT NULL,
  `buyer_name` varchar(50) NOT NULL,
  `buyer_phone` varchar(20) NOT NULL,
  `total_amount` int NOT NULL,
  `status` varchar(20) NOT NULL,
  `crt_dtm` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `crt_no` bigint DEFAULT NULL,
  `upt_dtm` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `upt_no` bigint DEFAULT NULL,
  `delivery_company` varchar(50) DEFAULT NULL COMMENT '택배사',
  `tracking_num` varchar(50) DEFAULT NULL COMMENT '운송장 번호',
  `admin_memo` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`order_no`),
  UNIQUE KEY `order_num` (`order_num`),
  KEY `idx_order_num` (`order_num`),
  KEY `idx_crt_dtm` (`crt_dtm`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product` (
  `product_no` bigint NOT NULL AUTO_INCREMENT COMMENT '상품 번호',
  `category_no` bigint NOT NULL COMMENT '카테고리 번호',
  `brand_no` bigint NOT NULL COMMENT '브랜드 번호',
  `name_ko` varchar(200) NOT NULL COMMENT '상품명 (한글)',
  `model_num` varchar(100) DEFAULT NULL COMMENT '모델번호',
  `release_price` int DEFAULT '0' COMMENT '발매가',
  `release_dt` date DEFAULT NULL COMMENT '발매일',
  `thumbnail_url` varchar(500) DEFAULT NULL COMMENT '썸네일 이미지 URL',
  `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '상태 (ACTIVE, HIDDEN, SOLD_OUT)',
  `crt_dtm` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
  `upt_dtm` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  `crt_no` bigint NOT NULL COMMENT '작성자',
  `upt_no` bigint NOT NULL COMMENT '수정자',
  PRIMARY KEY (`product_no`),
  KEY `idx_category` (`category_no`),
  KEY `idx_brand` (`brand_no`),
  KEY `idx_model` (`model_num`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='상품 마스터';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_change_history` (
  `history_no` bigint NOT NULL AUTO_INCREMENT,
  `product_no` bigint NOT NULL,
  `action_type` varchar(20) NOT NULL,
  `summary` varchar(1000) NOT NULL,
  `status_snapshot` varchar(20) DEFAULT NULL,
  `option_count` int NOT NULL DEFAULT '0',
  `total_stock` bigint NOT NULL DEFAULT '0',
  `crt_dtm` datetime DEFAULT NULL,
  `crt_no` bigint DEFAULT NULL,
  `upt_dtm` datetime DEFAULT NULL,
  `upt_no` bigint DEFAULT NULL,
  PRIMARY KEY (`history_no`),
  KEY `idx_product_change_history_product_no_history_no` (`product_no`,`history_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_option` (
  `option_no` bigint NOT NULL AUTO_INCREMENT COMMENT '옵션 번호',
  `product_no` bigint NOT NULL COMMENT '상품 번호',
  `option_name` varchar(255) NOT NULL,
  `stock_cnt` int DEFAULT '0' COMMENT '재고 수량',
  `additional_price` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`option_no`),
  KEY `idx_product` (`product_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='상품 옵션 (사이즈 등)';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sy_account` (
  `ID` bigint NOT NULL AUTO_INCREMENT,
  `EMAIL` varchar(255) DEFAULT NULL,
  `PASSWORD` varchar(255) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `NICKNAME` varchar(255) DEFAULT NULL,
  `MASTER_YN` varchar(1) DEFAULT NULL,
  `INIT_YN` varchar(1) DEFAULT NULL,
  `PROFILE_IMG_PATH` varchar(255) DEFAULT NULL,
  `PROFILE_IMG_NAME` varchar(255) DEFAULT NULL,
  `DEL_YN` varchar(1) DEFAULT NULL,
  `TMP_PW_ISSUE_DT` datetime DEFAULT NULL,
  `crt_dtm` datetime DEFAULT CURRENT_TIMESTAMP,
  `crt_no` bigint DEFAULT NULL,
  `upt_dtm` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `upt_no` bigint DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `idx_sy_account_email` (`EMAIL`),
  KEY `idx_sy_account_name` (`NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='회원 계정';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sy_approval_document` (
  `DOC_NO` bigint NOT NULL AUTO_INCREMENT,
  `TITLE` varchar(255) DEFAULT NULL,
  `CONTENT_TYPE_CODE` varchar(255) DEFAULT NULL,
  `STATUS` varchar(255) DEFAULT NULL,
  `DEPTH` varchar(255) DEFAULT NULL,
  `RENEW_YN` varchar(1) DEFAULT NULL,
  `DEL_YN` varchar(1) DEFAULT NULL,
  `MANAGER_NO` varchar(255) DEFAULT NULL,
  `ALT_MANAGER_NO` varchar(255) DEFAULT NULL,
  `APPROVAL_DTM` datetime DEFAULT NULL,
  `crt_dtm` datetime DEFAULT CURRENT_TIMESTAMP,
  `crt_no` bigint DEFAULT NULL,
  `upt_dtm` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `upt_no` bigint DEFAULT NULL,
  PRIMARY KEY (`DOC_NO`),
  KEY `idx_sy_approval_status` (`STATUS`),
  KEY `idx_sy_approval_manager_no` (`MANAGER_NO`),
  KEY `idx_sy_approval_dtm` (`APPROVAL_DTM`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='결재 문서';
/*!40101 SET character_set_client = @saved_cs_client */;

SET FOREIGN_KEY_CHECKS = 1;
